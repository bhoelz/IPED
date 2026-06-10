package iped.engine.mcp.client;

import com.sun.net.httpserver.HttpServer;
import iped.engine.mcp.client.dto.DataListDto;
import iped.engine.mcp.client.dto.SearchResultDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises WebApiClient against a stub HTTP server with exact-path routing.
 */
public class WebApiClientTest {

    private record StubResponse(int status, String body) {
    }

    private static HttpServer server;
    private static WebApiClient client;
    private static final Map<String, StubResponse> routes = new ConcurrentHashMap<>();
    /** method + space + rawQuery of the last request, keyed by raw path */
    private static final Map<String, String> lastRequests = new ConcurrentHashMap<>();

    @BeforeAll
    static void startServer() throws IOException {
        routes.put("/cases", new StubResponse(200, "{\"data\":[\"case1\",\"case2\"]}"));
        routes.put("/cases/my%20case", new StubResponse(200, "{}"));
        routes.put("/search", new StubResponse(200, "{\"data\":[]}"));
        routes.put("/categories", new StubResponse(500, ""));
        routes.put("/bookmarks/evidence", new StubResponse(200, ""));
        routes.put("/sources/src1/docs/7/text", new StubResponse(200, "x".repeat(60000)));
        routes.put("/sources/src1/docs/8/text", new StubResponse(200, "short text"));

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String rawPath = exchange.getRequestURI().getRawPath();
            lastRequests.put(rawPath,
                    exchange.getRequestMethod() + " " + exchange.getRequestURI().getRawQuery());
            StubResponse stub = routes.getOrDefault(rawPath, new StubResponse(404, ""));
            byte[] bytes = stub.body().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(stub.status(), bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            }
            exchange.close();
        });
        server.start();

        // trailing slash in the base URL must be tolerated
        client = new WebApiClient("http://127.0.0.1:" + server.getAddress().getPort() + "/");
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @Test
    public void listCasesParsesJson() throws Exception {
        DataListDto<String> cases = client.listCases();
        assertEquals(List.of("case1", "case2"), cases.getData());
    }

    @Test
    public void pathParametersAreUrlEncoded() throws Exception {
        client.getCaseStatus("my case");
        // path segments must be percent-encoded (space as %20, not '+')
        assertEquals("GET null", lastRequests.get("/cases/my%20case"));
    }

    @Test
    public void searchEncodesQueryParameters() throws Exception {
        SearchResultDto result = client.search("foo bar", "src 1");
        assertTrue(result.getData().isEmpty());
        assertEquals("GET q=foo+bar&sourceID=src+1", lastRequests.get("/search"));
    }

    @Test
    public void searchWithoutSourceOmitsSourceParameter() throws Exception {
        client.search("foo", null);
        assertEquals("GET q=foo", lastRequests.get("/search"));
    }

    @Test
    public void notFoundThrowsWebApiException() {
        WebApiException e = assertThrows(WebApiException.class, () -> client.getCaseStatus("missing"));
        assertTrue(e.getMessage().contains("Not found"), "got: " + e.getMessage());
    }

    @Test
    public void serverErrorThrowsWebApiException() {
        WebApiException e = assertThrows(WebApiException.class, () -> client.listCategories());
        assertTrue(e.getMessage().contains("HTTP 500"), "got: " + e.getMessage());
    }

    @Test
    public void longDocumentTextIsTruncated() throws Exception {
        String text = client.getDocumentText("src1", 7);
        assertTrue(text.length() < 60000);
        assertTrue(text.contains("[TRUNCATED"), "missing truncation marker");
        assertTrue(text.contains("60000"), "should mention original length");
    }

    @Test
    public void shortDocumentTextIsReturnedAsIs() throws Exception {
        assertEquals("short text", client.getDocumentText("src1", 8));
    }

    @Test
    public void createBookmarkUsesPost() throws Exception {
        client.createBookmark("evidence");
        assertEquals("POST null", lastRequests.get("/bookmarks/evidence"));
    }
}
