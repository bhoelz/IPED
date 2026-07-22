package iped.engine.mcp;

import iped.engine.mcp.client.WebApiClient;
import iped.engine.mcp.client.dto.ItemMetadataDto;
import iped.engine.mcp.client.dto.JobDto;
import iped.engine.mcp.client.dto.SearchPageDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Hand-coded stub for {@link WebApiClient} used in evaluation tests.
 *
 * <p>No HTTP connections are made. Data is returned in-memory. Method calls are recorded in {@link
 * #calls} for assertion in tests.
 */
class WebApiStub extends WebApiClient {

  final List<String> calls = new ArrayList<>();

  WebApiStub() {
    // The parent constructor only creates an HttpClient — no network connection.
    super("http://stub-unused");
  }

  @Override
  public List<Map<String, Object>> listCases() {
    calls.add("listCases");
    return List.of(Map.of("id", "demo-1", "path", "/cases/demo", "totalItems", 1500));
  }

  @Override
  public SearchPageDto search(String query, int offset, int limit) {
    calls.add("search:" + query);
    var page = new SearchPageDto();
    page.setTotal(45);
    page.setOffset(offset);
    page.setLimit(limit);
    page.setItems(
        IntStream.range(0, Math.min(limit, Math.max(0, 45 - offset)))
            .mapToObj(
                i ->
                    Map.<String, Object>of(
                        "itemId",
                        "demo-1:" + (offset + i),
                        "name",
                        "file-" + (offset + i) + ".txt",
                        "mediaType",
                        "text/plain",
                        "size",
                        1024L))
            .toList());
    return page;
  }

  @Override
  public ItemMetadataDto getItemMetadata(String sourceId, int docId) {
    calls.add("getItemMetadata:" + sourceId + ":" + docId);
    var dto = new ItemMetadataDto();
    dto.setItemId(sourceId + ":" + docId);
    dto.setName("evidence.txt");
    dto.setPath("/images/disk0/files/evidence.txt");
    dto.setMediaType("text/plain");
    dto.setSize(2048L);
    dto.setHash("a1b2c3d4e5f6");
    return dto;
  }

  @Override
  public String getItemText(String sourceId, int docId, String highlight) {
    calls.add("getItemText:" + sourceId + ":" + docId);
    return "Sensitive evidence text content for item " + docId + ". This is forensic data.";
  }

  @Override
  public List<String> listBookmarks() {
    calls.add("listBookmarks");
    return List.of("review", "suspicious");
  }

    @Override
    public List<Map<String, Object>> listOsintPlugins() {
        calls.add("listOsintPlugins");
        return List.of(Map.of(
                "id", "basic-profile-lookup",
                "displayName", "Basic Profile Lookup",
                "supportedIndicatorTypes", List.of("USERNAME", "EMAIL", "DOMAIN", "URL")));
    }

    @Override
    public Map<String, Object> getOsintPlugin(String pluginId) {
        calls.add("getOsintPlugin:" + pluginId);
        return Map.of(
                "id", pluginId,
                "displayName", "Basic Profile Lookup",
                "description", "Stub OSINT plugin");
    }

    @Override
    public Map<String, Object> searchOsint(Map<String, Object> body) {
        calls.add("searchOsint:" + body.get("sourceId"));
        return Map.of(
                "sourceId", body.get("sourceId"),
                "itemId", body.getOrDefault("itemId", 7),
                "results", List.of(Map.of(
                        "executionId", "osint-1",
                        "pluginId", "basic-profile-lookup",
                        "indicatorType", body.getOrDefault("indicatorType", "USERNAME"),
                        "normalizedValue", body.getOrDefault("value", "alice"),
                        "hits", List.of(Map.of("source", "github", "title", "github lookup")))));
    }

    @Override
    public Map<String, Object> listOsintResults(String sourceId, Integer itemId, String pluginId, int limit) {
        calls.add("listOsintResults:" + sourceId);
        return Map.of("results", List.of(Map.of(
                "executionId", "osint-1",
                "pluginId", pluginId == null ? "basic-profile-lookup" : pluginId,
                "itemId", itemId == null ? 7 : itemId,
                "sourceId", sourceId == null ? "demo-1" : sourceId)));
    }

    @Override
    public Map<String, Object> getOsintResult(String sourceId, String executionId) {
        calls.add("getOsintResult:" + executionId);
        return Map.of(
                "executionId", executionId,
                "pluginId", "basic-profile-lookup",
                "sourceId", sourceId == null ? "demo-1" : sourceId,
                "status", "completed");
    }

    @Override
    public void tagItem(String sourceId, int docId, String tag) {
        calls.add("tagItem:" + sourceId + ":" + docId + ":" + tag);
    }

  @Override
  public void untagItem(String sourceId, int docId, String tag) {
    calls.add("untagItem:" + sourceId + ":" + docId + ":" + tag);
  }

  @Override
  public Map<String, Object> submitJob(String type, Map<String, Object> params) {
    calls.add("submitJob:" + type);
    return Map.of("jobId", "job-abc-123", "status", "pending");
  }

  @Override
  public JobDto getJob(String jobId) {
    calls.add("getJob:" + jobId);
    var dto = new JobDto();
    dto.setId(jobId);
    dto.setType("export");
    dto.setStatus("running");
    dto.setProgress(50);
    dto.setMessage("Exporting items…");
    return dto;
  }

  @Override
  public void cancelJob(String jobId) {
    calls.add("cancelJob:" + jobId);
  }
}
