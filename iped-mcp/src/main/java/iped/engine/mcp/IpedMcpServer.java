package iped.engine.mcp;

import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapperSupplier;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import iped.engine.mcp.client.WebApiClient;
import iped.engine.mcp.tools.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public class IpedMcpServer {

    private static final Logger LOG = LoggerFactory.getLogger(IpedMcpServer.class);

    public static void main(String[] args) throws Exception {
        // Parse CLI arguments
        CliArgs cli = CliArgs.parse(args);
        if (cli == null) {
            printUsage();
            System.exit(1);
        }

        // Redirect all logging to stderr; stdout is reserved for stdio transport
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.err");

        LOG.info("Starting iped-mcp, connecting to {}", cli.webapiUrl());

        // Create HTTP client
        WebApiClient client = new WebApiClient(cli.webapiUrl());

        // Probe connectivity — fail fast with clear message
        try {
            client.getGlobalStats();
            LOG.info("Connected to iped-webapi successfully.");
        } catch (Exception e) {
            System.err.println("ERROR: Cannot reach iped-webapi at " + cli.webapiUrl()
                + " — " + e.getMessage());
            System.exit(2);
        }

        // Create tool registry
        ToolRegistry registry = new ToolRegistry(client);

        // Build and start MCP server
        buildAndRunServer(registry, cli);
    }

    private static void buildAndRunServer(ToolRegistry registry, CliArgs cli) throws Exception {
        var mapper = new JacksonMcpJsonMapperSupplier().get();
        var transport = new StdioServerTransportProvider(mapper);

        var server = McpServer.sync(transport)
            .serverInfo("iped-mcp", "1.0.0")
            .tools(registry.tools())
            .build();

        LOG.info("iped-mcp ready (transport={}), {} tools registered",
            cli.transport(), registry.tools().size());

        // Keep the JVM alive until terminated; stdio transport runs on background
        // threads. A shutdown hook closes the server gracefully on SIGINT/SIGTERM.
        CountDownLatch shutdownLatch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Shutting down iped-mcp...");
            server.closeGracefully();
            shutdownLatch.countDown();
        }));

        shutdownLatch.await();
    }

    private static void printUsage() {
        System.err.println("Usage: java -jar iped-mcp.jar \\");
        System.err.println("         --webapi-url=http://localhost:8080 \\");
        System.err.println("         [--transport=stdio] \\");
        System.err.println("         [--port=3000]");
    }
}
