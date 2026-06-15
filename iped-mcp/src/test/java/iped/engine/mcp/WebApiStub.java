package iped.engine.mcp;

import iped.engine.mcp.client.WebApiClient;
import iped.engine.mcp.client.WebApiException;
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
 * <p>No HTTP connections are made. Data is returned in-memory. Method calls are
 * recorded in {@link #calls} for assertion in tests.
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
        page.setItems(IntStream.range(0, Math.min(limit, Math.max(0, 45 - offset)))
                .mapToObj(i -> Map.<String, Object>of(
                        "itemId", "demo-1:" + (offset + i),
                        "name", "file-" + (offset + i) + ".txt",
                        "mediaType", "text/plain",
                        "size", 1024L))
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
        return "Sensitive evidence text content for item " + docId
                + ". This is forensic data.";
    }

    @Override
    public List<String> listBookmarks() {
        calls.add("listBookmarks");
        return List.of("review", "suspicious");
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
