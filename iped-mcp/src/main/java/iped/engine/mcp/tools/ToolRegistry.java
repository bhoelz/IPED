package iped.engine.mcp.tools;

import io.modelcontextprotocol.server.McpServerFeatures;
import iped.engine.mcp.client.WebApiClient;

import java.util.ArrayList;
import java.util.List;

public class ToolRegistry {
    private final WebApiClient client;

    public ToolRegistry(WebApiClient client) {
        this.client = client;
    }

    public List<McpServerFeatures.SyncToolSpecification> tools() {
        List<McpServerFeatures.SyncToolSpecification> allTools = new ArrayList<>();

        allTools.addAll(new CaseTools(client).specifications());
        allTools.addAll(new StatsTools(client).specifications());
        allTools.addAll(new SourceTools(client).specifications());
        allTools.addAll(new SearchTools(client).specifications());
        allTools.addAll(new DocumentTools(client).specifications());
        allTools.addAll(new BookmarkTools(client).specifications());
        allTools.addAll(new SelectionTools(client).specifications());

        return allTools;
    }
}
