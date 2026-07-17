package iped.engine.mcp;

import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapperSupplier;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import iped.engine.mcp.client.WebApiClient;
import iped.engine.mcp.tools.ToolRegistry;
import java.util.concurrent.CountDownLatch;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IpedMcpServer {

  public static void main(String[] args) throws Exception {
    CliArgs cli = CliArgs.parse(args);
    if (cli == null) {
      printUsage();
      System.exit(1);
    }

    // Redirect all logging to stderr; stdout is reserved for stdio transport
    System.setProperty("org.slf4j.simpleLogger.logFile", "System.err");

    log.info("Starting iped-mcp, connecting to {}", cli.webapiUrl());

    McpSessionContext session =
        new McpSessionContext(cli.allowedCases(), cli.rateLimit(), cli.capabilities());
    WebApiClient client = new WebApiClient(cli.webapiUrl(), session.sessionId, cli.apiKey());

    // Probe connectivity — fail fast with clear message
    try {
      client.listCases();
      log.info("Connected to iped-webapi successfully. session={}", session.sessionId);
    } catch (Exception e) {
      System.err.println(
          "ERROR: Cannot reach iped-webapi at " + cli.webapiUrl() + " — " + e.getMessage());
      System.exit(2);
    }

    if (!session.allowedCases.isEmpty()) {
      log.info("Case allowlist active: {}", session.allowedCases);
    }
    if (!session.capabilities.isEmpty()) {
      log.info("Capabilities granted: {}", session.capabilities);
    }

    McpAuditLog audit = new McpAuditLog();
    ToolRegistry registry = new ToolRegistry(client, audit, session);

    int toolCount = registry.tools().size();
    log.info("iped-mcp ready (transport={}, tools={})", cli.transport(), toolCount);

    if ("http".equalsIgnoreCase(cli.transport())) {
      HttpTransport.run(registry, cli);
    } else {
      runStdio(registry, cli);
    }
  }

  private static void runStdio(ToolRegistry registry, CliArgs cli) throws Exception {
    var mapper = new JacksonMcpJsonMapperSupplier().get();
    var transport = new StdioServerTransportProvider(mapper);

    var server =
        McpServer.sync(transport).serverInfo("iped-mcp", "1.0.0").tools(registry.tools()).build();

    CountDownLatch shutdownLatch = new CountDownLatch(1);
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  log.info("Shutting down iped-mcp...");
                  server.closeGracefully();
                  shutdownLatch.countDown();
                }));

    shutdownLatch.await();
  }

  private static void printUsage() {
    System.err.println("Usage: java -jar iped-mcp.jar \\");
    System.err.println("         --webapi-url=http://localhost:8080 \\");
    System.err.println("         [--transport=stdio] \\");
    System.err.println("         [--port=3000] \\");
    System.err.println("         [--api-key=TOKEN] \\");
    System.err.println("         [--allowed-cases=id1,id2] \\");
    System.err.println("         [--rate-limit=120] \\");
    System.err.println("         [--capabilities=bookmarks,jobs]");
  }
}
