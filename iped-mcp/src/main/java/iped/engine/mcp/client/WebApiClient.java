package iped.engine.mcp.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import iped.engine.mcp.client.dto.ItemMetadataDto;
import iped.engine.mcp.client.dto.JobDto;
import iped.engine.mcp.client.dto.SearchPageDto;
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
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Thin HTTP client over the iped-webapi v2 REST contract.
 *
 * <p>All paths begin with {@code /v2/}. The client performs no business logic; it only serialises
 * requests and deserialises responses.
 */
@Slf4j
public class WebApiClient {

  private final String baseUrl;
  private volatile HttpClient http; // lazy — created on first actual HTTP call
  private final ObjectMapper mapper;
  private final String sessionId;
  private final String apiKey;

  public WebApiClient(String baseUrl) {
    this(baseUrl, null, null);
  }

  /**
   * @param baseUrl iped-webapi base URL
   * @param sessionId MCP session UUID forwarded as {@code X-MCP-Session-Id}
   * @param apiKey optional Bearer token forwarded as {@code Authorization}
   */
  public WebApiClient(String baseUrl, String sessionId, String apiKey) {
    this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    this.sessionId = sessionId;
    this.apiKey = apiKey;
    this.mapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  /** Returns the shared HTTP client, creating it on first use. */
  private HttpClient http() {
    if (http == null) {
      synchronized (this) {
        if (http == null) {
          http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        }
      }
    }
    return http;
  }

  // ── Cases ─────────────────────────────────────────────────────────────────

  /** {@code GET /v2/cases} → {@code {cases:[…]}} */
  @SuppressWarnings("unchecked")
  public List<Map<String, Object>> listCases() throws WebApiException {
    Map<?, ?> body = get("/v2/cases", Map.class);
    Object cases = body.get("cases");
    return cases instanceof List<?> l ? (List<Map<String, Object>>) l : List.of();
  }

  /** {@code POST /v2/cases} — open a case by file-system path. */
  public Map<String, Object> openCase(String id, String path) throws WebApiException {
    Map<String, String> req =
        id != null && !id.isBlank() ? Map.of("id", id, "path", path) : Map.of("path", path);
    return postJson("/v2/cases", req, new TypeReference<>() {});
  }

  /** {@code GET /v2/cases/{id}} */
  public Map<String, Object> getCase(String caseId) throws WebApiException {
    return get("/v2/cases/" + encodePath(caseId), new TypeReference<>() {});
  }

  /** {@code DELETE /v2/cases/{id}} */
  public void closeCase(String caseId) throws WebApiException {
    delete("/v2/cases/" + encodePath(caseId));
  }

  // ── Search ────────────────────────────────────────────────────────────────

  /**
   * {@code GET /v2/search?q=…&offset=…&limit=…}
   *
   * @param query Lucene query string
   * @param offset zero-based first result index
   * @param limit max results (1–1000, default 20)
   */
  public SearchPageDto search(String query, int offset, int limit) throws WebApiException {
    String path = "/v2/search?q=" + encode(query) + "&offset=" + offset + "&limit=" + limit;
    return get(path, SearchPageDto.class);
  }

  // ── Items ─────────────────────────────────────────────────────────────────

  /** {@code GET /v2/sources/{src}/items/{id}} */
  public ItemMetadataDto getItemMetadata(String sourceId, int docId) throws WebApiException {
    return get("/v2/sources/" + encodePath(sourceId) + "/items/" + docId, ItemMetadataDto.class);
  }

  /**
   * {@code GET /v2/sources/{src}/items/{id}/text}
   *
   * @param highlight optional highlight terms (space-separated), or blank
   */
  public String getItemText(String sourceId, int docId, String highlight) throws WebApiException {
    String path = "/v2/sources/" + encodePath(sourceId) + "/items/" + docId + "/text";
    if (highlight != null && !highlight.isBlank()) {
      path += "?highlight=" + encode(highlight);
    }
    return getText(path);
  }

  /** {@code GET /v2/sources/{src}/items/categories} */
  @SuppressWarnings("unchecked")
  public List<String> listCategories(String sourceId) throws WebApiException {
    return (List<String>)
        get("/v2/sources/" + encodePath(sourceId) + "/items/categories", List.class);
  }

  // ── Relational (v1 endpoints — no v2 equivalent yet) ─────────────────────

  /**
   * Gets related items for one item through a named relation.
   *
   * @param relation one of {@code subitems}, {@code parent}, {@code duplicates}, {@code
   *     references}, {@code referencedby}
   */
  public Map<String, Object> getRelatedItems(
      String sourceId, int docId, String relation, int offset, int limit) throws WebApiException {
    String path =
        "/sources/"
            + encodePath(sourceId)
            + "/docs/"
            + docId
            + "/"
            + relation
            + "?offset="
            + offset
            + "&limit="
            + limit;
    return get(path, new TypeReference<>() {});
  }

  // ── Tags (bookmark-backed) ────────────────────────────────────────────────

  /**
   * Adds a single item to a named tag (bookmark), creating the bookmark first if it does not
   * already exist.
   *
   * @param sourceId source the item belongs to
   * @param docId Lucene document ID
   * @param tag tag name
   */
  public void tagItem(String sourceId, int docId, String tag) throws WebApiException {
    ensureBookmarkExists(tag);
    addBookmarkItems(tag, List.of(Map.of("sourceId", sourceId, "docId", docId)));
  }

  /**
   * Removes a single item from a named tag (bookmark). Silently succeeds when the bookmark does not
   * exist.
   */
  public void untagItem(String sourceId, int docId, String tag) throws WebApiException {
    try {
      removeBookmarkItems(tag, List.of(Map.of("sourceId", sourceId, "docId", docId)));
    } catch (WebApiException e) {
      if (e.getMessage() != null && e.getMessage().startsWith("Not found:")) return;
      throw e;
    }
  }

  private void ensureBookmarkExists(String name) throws WebApiException {
    try {
      createBookmark(name);
    } catch (WebApiException e) {
      // 409 conflict = bookmark already exists — that is fine
      if (e.getMessage() != null && e.getMessage().startsWith("Conflict")) return;
      throw e;
    }
  }

  // ── Jobs ──────────────────────────────────────────────────────────────────

  /**
   * {@code POST /v2/jobs} — submit an async job.
   *
   * @param type job type, e.g. {@code "export"} or {@code "report"}
   * @param params type-specific parameters map
   * @return server response with {@code jobId} and initial {@code status}
   */
  public Map<String, Object> submitJob(String type, Map<String, Object> params)
      throws WebApiException {
    return postJson("/v2/jobs", Map.of("type", type, "params", params), new TypeReference<>() {});
  }

  /** {@code GET /v2/jobs/{id}} */
  public JobDto getJob(String jobId) throws WebApiException {
    return get("/v2/jobs/" + encodePath(jobId), JobDto.class);
  }

  /** {@code DELETE /v2/jobs/{id}} — request cancellation of a pending or running job. */
  public void cancelJob(String jobId) throws WebApiException {
    delete("/v2/jobs/" + encodePath(jobId));
  }

  // ── Bookmarks ─────────────────────────────────────────────────────────────

  /** {@code GET /v2/bookmarks} → {@code {bookmarks:[…]}} */
  @SuppressWarnings("unchecked")
  public List<String> listBookmarks() throws WebApiException {
    Map<?, ?> body = get("/v2/bookmarks", Map.class);
    Object bms = body.get("bookmarks");
    return bms instanceof List<?> l ? (List<String>) l : List.of();
  }

  /** {@code POST /v2/bookmarks} — create a bookmark. Returns 409 on duplicate. */
  public Map<String, Object> createBookmark(String name) throws WebApiException {
    return postJson("/v2/bookmarks", Map.of("name", name), new TypeReference<>() {});
  }

  /** {@code DELETE /v2/bookmarks/{name}} */
  public void deleteBookmark(String name) throws WebApiException {
    delete("/v2/bookmarks/" + encodePath(name));
  }

  /** {@code PATCH /v2/bookmarks/{name}} — rename. */
  public void renameBookmark(String oldName, String newName) throws WebApiException {
    patchJson("/v2/bookmarks/" + encodePath(oldName), Map.of("name", newName));
  }

  /** {@code GET /v2/bookmarks/{name}/items} */
  public Map<String, Object> getBookmarkItems(String name) throws WebApiException {
    return get("/v2/bookmarks/" + encodePath(name) + "/items", new TypeReference<>() {});
  }

  /** {@code PUT /v2/bookmarks/{name}/items} — add items. */
  public void addBookmarkItems(String name, List<Map<String, Object>> items)
      throws WebApiException {
    putJson("/v2/bookmarks/" + encodePath(name) + "/items", items);
  }

  /** {@code DELETE /v2/bookmarks/{name}/items} — remove items. */
  public void removeBookmarkItems(String name, List<Map<String, Object>> items)
      throws WebApiException {
    deleteWithBody("/v2/bookmarks/" + encodePath(name) + "/items", items);
  }

  // ── HTTP primitives ───────────────────────────────────────────────────────

  /** Applies session-id and api-key headers to every outbound request builder. */
  private HttpRequest.Builder base(String path, Duration timeout) {
    HttpRequest.Builder b =
        HttpRequest.newBuilder().uri(URI.create(baseUrl + path)).timeout(timeout);
    if (sessionId != null) b.header("X-MCP-Session-Id", sessionId);
    if (apiKey != null) b.header("Authorization", "Bearer " + apiKey);
    return b;
  }

  private <T> T get(String path, Class<T> type) throws WebApiException {
    HttpRequest req =
        base(path, Duration.ofSeconds(30)).GET().header("Accept", "application/json").build();
    try {
      HttpResponse<byte[]> resp = http().send(req, BodyHandlers.ofByteArray());
      requireSuccess(resp.statusCode(), path);
      return mapper.readValue(resp.body(), type);
    } catch (IOException | InterruptedException e) {
      throw new WebApiException("GET " + path + " failed: " + e.getMessage(), e);
    }
  }

  private <T> T get(String path, TypeReference<T> typeRef) throws WebApiException {
    HttpRequest req =
        base(path, Duration.ofSeconds(30)).GET().header("Accept", "application/json").build();
    try {
      HttpResponse<byte[]> resp = http().send(req, BodyHandlers.ofByteArray());
      requireSuccess(resp.statusCode(), path);
      return mapper.readValue(resp.body(), typeRef);
    } catch (IOException | InterruptedException e) {
      throw new WebApiException("GET " + path + " failed: " + e.getMessage(), e);
    }
  }

  private String getText(String path) throws WebApiException {
    HttpRequest req =
        base(path, Duration.ofSeconds(60)).GET().header("Accept", "text/plain").build();
    try {
      HttpResponse<String> resp = http().send(req, BodyHandlers.ofString());
      requireSuccess(resp.statusCode(), path);
      return resp.body();
    } catch (IOException | InterruptedException e) {
      throw new WebApiException("GET " + path + " failed: " + e.getMessage(), e);
    }
  }

  private <B, T> T postJson(String path, B body, TypeReference<T> typeRef) throws WebApiException {
    try {
      String json = mapper.writeValueAsString(body);
      HttpRequest req =
          base(path, Duration.ofSeconds(30))
              .POST(BodyPublishers.ofString(json))
              .header("Content-Type", "application/json")
              .header("Accept", "application/json")
              .build();
      HttpResponse<byte[]> resp = http().send(req, BodyHandlers.ofByteArray());
      requireSuccess(resp.statusCode(), path);
      if (resp.body() == null || resp.body().length == 0) return null;
      return mapper.readValue(resp.body(), typeRef);
    } catch (IOException | InterruptedException e) {
      throw new WebApiException("POST " + path + " failed: " + e.getMessage(), e);
    }
  }

  private <B> void putJson(String path, B body) throws WebApiException {
    try {
      String json = mapper.writeValueAsString(body);
      HttpRequest req =
          base(path, Duration.ofSeconds(30))
              .PUT(BodyPublishers.ofString(json))
              .header("Content-Type", "application/json")
              .build();
      HttpResponse<Void> resp = http().send(req, BodyHandlers.discarding());
      requireSuccess(resp.statusCode(), path);
    } catch (IOException | InterruptedException e) {
      throw new WebApiException("PUT " + path + " failed: " + e.getMessage(), e);
    }
  }

  private <B> void patchJson(String path, B body) throws WebApiException {
    try {
      String json = mapper.writeValueAsString(body);
      HttpRequest req =
          base(path, Duration.ofSeconds(30))
              .method("PATCH", BodyPublishers.ofString(json))
              .header("Content-Type", "application/json")
              .build();
      HttpResponse<Void> resp = http().send(req, BodyHandlers.discarding());
      requireSuccess(resp.statusCode(), path);
    } catch (IOException | InterruptedException e) {
      throw new WebApiException("PATCH " + path + " failed: " + e.getMessage(), e);
    }
  }

  private void delete(String path) throws WebApiException {
    HttpRequest req = base(path, Duration.ofSeconds(30)).DELETE().build();
    try {
      HttpResponse<Void> resp = http().send(req, BodyHandlers.discarding());
      requireSuccess(resp.statusCode(), path);
    } catch (IOException | InterruptedException e) {
      throw new WebApiException("DELETE " + path + " failed: " + e.getMessage(), e);
    }
  }

  private <B> void deleteWithBody(String path, B body) throws WebApiException {
    try {
      String json = mapper.writeValueAsString(body);
      HttpRequest req =
          base(path, Duration.ofSeconds(30))
              .method("DELETE", BodyPublishers.ofString(json))
              .header("Content-Type", "application/json")
              .build();
      HttpResponse<Void> resp = http().send(req, BodyHandlers.discarding());
      requireSuccess(resp.statusCode(), path);
    } catch (IOException | InterruptedException e) {
      throw new WebApiException("DELETE " + path + " failed: " + e.getMessage(), e);
    }
  }

  private void requireSuccess(int status, String path) throws WebApiException {
    if (status == 404) throw new WebApiException("Not found: " + path);
    if (status == 409) throw new WebApiException("Conflict (duplicate): " + path);
    if (status == 400) throw new WebApiException("Bad request: " + path);
    if (status == 503) throw new WebApiException("Service unavailable (no cases open?): " + path);
    if (status < 200 || status >= 300)
      throw new WebApiException("HTTP " + status + " from " + path);
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private String encodePath(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }
}
