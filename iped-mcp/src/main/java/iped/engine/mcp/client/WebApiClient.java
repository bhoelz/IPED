package iped.engine.mcp.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import iped.engine.mcp.client.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class WebApiClient {
    private static final Logger LOG = LoggerFactory.getLogger(WebApiClient.class);

    private final String baseUrl;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public WebApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/")
            ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // --- Cases ---

    public DataListDto<String> listCases() throws WebApiException {
        return get("/cases", new com.fasterxml.jackson.core.type.TypeReference<DataListDto<String>>() {});
    }

    public CaseStatusDto getCaseStatus(String caseId) throws WebApiException {
        return get("/cases/" + encode(caseId), CaseStatusDto.class);
    }

    public void pauseCase(String caseId) throws WebApiException {
        post("/cases/" + encode(caseId) + "/pause");
    }

    public void resumeCase(String caseId) throws WebApiException {
        post("/cases/" + encode(caseId) + "/resume");
    }

    // --- Stats ---

    public GlobalStatsDto getGlobalStats() throws WebApiException {
        return get("/stats/global", GlobalStatsDto.class);
    }

    public CaseStatsDto getCaseStats(String caseId) throws WebApiException {
        return get("/stats/case/" + encode(caseId), CaseStatsDto.class);
    }

    // --- Sources ---

    public DataListDto<SourceDto> listSources() throws WebApiException {
        return get("/sources", new com.fasterxml.jackson.core.type.TypeReference<DataListDto<SourceDto>>() {});
    }

    public SourceDto getSource(String sourceId) throws WebApiException {
        return get("/sources/" + encode(sourceId), SourceDto.class);
    }

    public void addSource(String id, String path) throws WebApiException {
        SourceDto dto = new SourceDto(id, path);
        postJson("/sources", dto);
    }

    // --- Search ---

    public SearchResultDto search(String query, String sourceId) throws WebApiException {
        String path = "/search?q=" + encode(query);
        if (sourceId != null && !sourceId.isEmpty()) {
            path += "&sourceID=" + encode(sourceId);
        }
        return get(path, SearchResultDto.class);
    }

    // --- Documents ---

    public DocPropsDto getDocumentMetadata(String sourceId, int docId) throws WebApiException {
        String path = "/sources/" + encode(sourceId) + "/docs/" + docId;
        return get(path, DocPropsDto.class);
    }

    public String getDocumentText(String sourceId, int docId) throws WebApiException {
        String path = "/sources/" + encode(sourceId) + "/docs/" + docId + "/text";
        String text = getText(path);
        if (text.length() > 50000) {
            return text.substring(0, 50000) + "\n[TRUNCATED — full text is " + text.length() + " chars]";
        }
        return text;
    }

    // --- Bookmarks ---

    public DataListDto<String> listBookmarks() throws WebApiException {
        return get("/bookmarks", new com.fasterxml.jackson.core.type.TypeReference<DataListDto<String>>() {});
    }

    public SearchResultDto getBookmarkDocs(String name) throws WebApiException {
        return get("/bookmarks/" + encode(name), SearchResultDto.class);
    }

    public void createBookmark(String name) throws WebApiException {
        post("/bookmarks/" + encode(name));
    }

    public void deleteBookmark(String name) throws WebApiException {
        delete("/bookmarks/" + encode(name));
    }

    public void addDocsToBookmark(String name, DocRefDto[] docs) throws WebApiException {
        putJson("/bookmarks/" + encode(name) + "/add", new DocRefRequest(docs));
    }

    public void removeDocsFromBookmark(String name, DocRefDto[] docs) throws WebApiException {
        putJson("/bookmarks/" + encode(name) + "/remove", new DocRefRequest(docs));
    }

    public void renameBookmark(String oldName, String newName) throws WebApiException {
        put("/bookmarks/" + encode(oldName) + "/rename/" + encode(newName), null);
    }

    // --- Selection ---

    public SearchResultDto getSelection() throws WebApiException {
        return get("/selection", SearchResultDto.class);
    }

    public void addToSelection(DocRefDto[] docs) throws WebApiException {
        putJson("/selection/add", new DocRefRequest(docs));
    }

    public void removeFromSelection(DocRefDto[] docs) throws WebApiException {
        putJson("/selection/remove", new DocRefRequest(docs));
    }

    // --- Categories ---

    public DataListDto<String> listCategories() throws WebApiException {
        return get("/categories", new com.fasterxml.jackson.core.type.TypeReference<DataListDto<String>>() {});
    }

    // --- Private helpers ---

    private <T> T get(String path, Class<T> type) throws WebApiException {
        HttpRequest req = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(baseUrl + path))
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(30))
            .build();
        return send(req, type);
    }

    private <T> T get(String path, com.fasterxml.jackson.core.type.TypeReference<T> typeRef) throws WebApiException {
        HttpRequest req = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(baseUrl + path))
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(30))
            .build();
        try {
            HttpResponse<byte[]> resp = http.send(req, BodyHandlers.ofByteArray());
            requireSuccess(resp.statusCode(), path);
            return mapper.readValue(resp.body(), typeRef);
        } catch (IOException | InterruptedException e) {
            throw new WebApiException("GET " + path + " failed: " + e.getMessage(), e);
        }
    }

    private void post(String path) throws WebApiException {
        HttpRequest req = HttpRequest.newBuilder()
            .POST(BodyPublishers.noBody())
            .uri(URI.create(baseUrl + path))
            .timeout(Duration.ofSeconds(30))
            .build();
        try {
            HttpResponse<Void> resp = http.send(req, BodyHandlers.discarding());
            requireSuccess(resp.statusCode(), path);
        } catch (IOException | InterruptedException e) {
            throw new WebApiException("POST " + path + " failed: " + e.getMessage(), e);
        }
    }

    private <B> void postJson(String path, B body) throws WebApiException {
        try {
            String json = mapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                .POST(BodyPublishers.ofString(json))
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .build();
            HttpResponse<Void> resp = http.send(req, BodyHandlers.discarding());
            requireSuccess(resp.statusCode(), path);
        } catch (IOException | InterruptedException e) {
            throw new WebApiException("POST " + path + " failed: " + e.getMessage(), e);
        }
    }

    private <B> void putJson(String path, B body) throws WebApiException {
        try {
            String json = mapper.writeValueAsString(body);
            HttpRequest req = HttpRequest.newBuilder()
                .PUT(BodyPublishers.ofString(json))
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .build();
            HttpResponse<Void> resp = http.send(req, BodyHandlers.discarding());
            requireSuccess(resp.statusCode(), path);
        } catch (IOException | InterruptedException e) {
            throw new WebApiException("PUT " + path + " failed: " + e.getMessage(), e);
        }
    }

    private void put(String path, Object body) throws WebApiException {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .PUT(BodyPublishers.noBody())
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30));
            HttpRequest req = builder.build();
            HttpResponse<Void> resp = http.send(req, BodyHandlers.discarding());
            requireSuccess(resp.statusCode(), path);
        } catch (IOException | InterruptedException e) {
            throw new WebApiException("PUT " + path + " failed: " + e.getMessage(), e);
        }
    }

    private void delete(String path) throws WebApiException {
        HttpRequest req = HttpRequest.newBuilder()
            .DELETE()
            .uri(URI.create(baseUrl + path))
            .timeout(Duration.ofSeconds(30))
            .build();
        try {
            HttpResponse<Void> resp = http.send(req, BodyHandlers.discarding());
            requireSuccess(resp.statusCode(), path);
        } catch (IOException | InterruptedException e) {
            throw new WebApiException("DELETE " + path + " failed: " + e.getMessage(), e);
        }
    }

    private String getText(String path) throws WebApiException {
        HttpRequest req = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(baseUrl + path))
            .header("Accept", "text/plain")
            .timeout(Duration.ofSeconds(60))
            .build();
        try {
            HttpResponse<String> resp = http.send(req, BodyHandlers.ofString());
            requireSuccess(resp.statusCode(), path);
            return resp.body();
        } catch (IOException | InterruptedException e) {
            throw new WebApiException("GET " + path + " failed: " + e.getMessage(), e);
        }
    }

    private <T> T send(HttpRequest req, Class<T> type) throws WebApiException {
        try {
            HttpResponse<byte[]> resp = http.send(req, BodyHandlers.ofByteArray());
            requireSuccess(resp.statusCode(), req.uri().getPath());
            return mapper.readValue(resp.body(), type);
        } catch (IOException | InterruptedException e) {
            throw new WebApiException("HTTP call failed: " + e.getMessage(), e);
        }
    }

    private void requireSuccess(int status, String path) throws WebApiException {
        if (status == 404) throw new WebApiException("Not found: " + path);
        if (status == 400) throw new WebApiException("Bad request: " + path);
        if (status < 200 || status >= 300)
            throw new WebApiException("HTTP " + status + " from " + path);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    // Helper class for request bodies
    private static class DocRefRequest {
        public DocRefDto[] docs;

        DocRefRequest(DocRefDto[] docs) {
            this.docs = docs;
        }

        public DocRefDto[] getDocs() {
            return docs;
        }

        public void setDocs(DocRefDto[] docs) {
            this.docs = docs;
        }
    }
}
