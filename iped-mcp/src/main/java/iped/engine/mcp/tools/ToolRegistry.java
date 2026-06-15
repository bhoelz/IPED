package iped.engine.mcp.tools;

import io.modelcontextprotocol.server.McpServerFeatures;
import iped.engine.mcp.GrantedCapabilities;
import iped.engine.mcp.McpAuditLog;
import iped.engine.mcp.McpSessionContext;
import iped.engine.mcp.ToolRateLimiter;
import iped.engine.mcp.client.WebApiClient;

import java.util.ArrayList;
import java.util.List;

public class ToolRegistry {
    private final WebApiClient client;
    private final McpAuditLog audit;
    private final McpSessionContext session;

    public ToolRegistry(WebApiClient client, McpAuditLog audit, McpSessionContext session) {
        this.client  = client;
        this.audit   = audit;
        this.session = session;
    }

    public List<McpServerFeatures.SyncToolSpecification> tools() {
        List<McpServerFeatures.SyncToolSpecification> all = new ArrayList<>();
        // Read-only tools — always registered
        all.addAll(new CaseTools(client, audit, session).specifications());
        all.addAll(new SearchTools(client, audit, session).specifications());
        all.addAll(new DocumentTools(client, audit, session).specifications());
        // BookmarkTools registers read specs always; write specs only when BOOKMARKS is granted
        all.addAll(new BookmarkTools(client, audit, session).specifications());
        // Write tools — gated by capability
        if (session.can(GrantedCapabilities.BOOKMARKS)) {
            all.addAll(new TagTools(client, audit, session).specifications());
        }
        if (session.can(GrantedCapabilities.JOBS)) {
            all.addAll(new JobTools(client, audit, session).specifications());
        }
        return rateLimit(all);
    }

    /**
     * Wraps every tool's handler with a rate-limit check as a cross-cutting concern
     * so individual tool classes don't need to repeat the call.
     */
    private List<McpServerFeatures.SyncToolSpecification> rateLimit(
            List<McpServerFeatures.SyncToolSpecification> specs) {
        ToolRateLimiter limiter = session.rateLimiter;
        List<McpServerFeatures.SyncToolSpecification> wrapped = new ArrayList<>(specs.size());
        for (var spec : specs) {
            var tool    = spec.tool();
            var handler = spec.callHandler();
            wrapped.add(McpServerFeatures.SyncToolSpecification.builder()
                    .tool(tool)
                    .callHandler((exchange, request) -> {
                        try {
                            limiter.acquire();
                        } catch (ToolRateLimiter.RateLimitException e) {
                            return CaseTools.err(tool.name(), e);
                        }
                        return handler.apply(exchange, request);
                    })
                    .build());
        }
        return wrapped;
    }
}
