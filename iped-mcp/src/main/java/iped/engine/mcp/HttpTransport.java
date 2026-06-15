package iped.engine.mcp;

import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapperSupplier;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import iped.engine.mcp.tools.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;

/**
 * Embedded Jetty HTTP transport for iped-mcp.
 *
 * <p>Activated when {@code --transport=http}. The MCP endpoint is mounted at
 * {@code /mcp} using the streamable-HTTP protocol (SSE + POST on the same path).
 * Auth and audit guarantees are identical to the stdio transport because both
 * transports share the same {@link ToolRegistry} and {@link McpAuditLog}.
 *
 * <p>Client URL: {@code http://host:<port>/mcp}
 */
@Slf4j
public class HttpTransport {

    static final String MCP_PATH = "/mcp";

    /**
     * Starts the embedded Jetty server and blocks until the JVM shuts down.
     *
     * @param registry tool registry already wired with audit log and session context
     * @param cli      parsed command-line args (only {@link CliArgs#port()} is used here)
     */
    static void run(ToolRegistry registry, CliArgs cli) throws Exception {
        var mapper = new JacksonMcpJsonMapperSupplier().get();

        var transport = HttpServletStreamableServerTransportProvider.builder()
                .jsonMapper(mapper)
                .build();

        McpServer.sync(transport)
                .serverInfo("iped-mcp", "1.0.0")
                .tools(registry.tools())
                .build();

        var context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(new ServletHolder("mcp", transport), MCP_PATH);

        var jetty = new Server(cli.port());
        jetty.setHandler(context);
        jetty.start();

        log.info("iped-mcp HTTP server listening on port {} — endpoint: http://0.0.0.0:{}{} ",
                cli.port(), cli.port(), MCP_PATH);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down iped-mcp HTTP server...");
            try { jetty.stop(); } catch (Exception ignored) {}
        }));

        jetty.join();
    }
}
