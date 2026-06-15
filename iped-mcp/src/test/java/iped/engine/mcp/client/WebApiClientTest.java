package iped.engine.mcp.client;

import com.sun.net.httpserver.HttpServer;
import iped.engine.mcp.client.dto.ItemMetadataDto;
import iped.engine.mcp.client.dto.SearchPageDto;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises WebApiClient v2 paths against a stub HTTP server.
 */
public class WebApiClientTest {

    private record StubResponse(int status, String body) {}

    private static HttpServer server;
    private static WebApiClient client;
    private static final Map<String, StubResponse> routes = new ConcurrentHashMap<>();
    private static final Map<String, String> lastRequests = new ConcurrentHashMap<>();

    @BeforeAll
    static void startServer() throws IOException {
        routes.put("/v2/cases",
                new StubResponse(200, "{\"cases\":[{\"id\":\"c1\"},{\"id\":\"c2\"}]}"));
        routes.put("/v2/cases/my%20case",
                new StubResponse(200, "{\"id\":\"my case\"}"));
        routes.put("/v2/search",
                new StubResponse(200, "{\"total\":2,\"offset\":0,\"limit\":20,\"items\":[]}"));
        routes.put("/v2/bookmarks",
                new StubResponse(200, "{\"bookmarks\":[\"tag1\",\"tag2\"]}"));
        routes.put("/v2/sources/src1/items/7/text",
                new StubResponse(200, "hello world"));
        routes.put("/v2/sources/src1/items/8",
                new StubResponse(200,
                        "{\"itemId\":\"src1:8\",\"name\":\"doc.txt\",\"mediaType\":\"text/plain\",\"size\":100}"));
        routes.put("/v2/sources/src1/items/categories",
                new StubResponse(200, "[\"Images\",\"Documents\"]"));
        // 4xx/5xx
        routes.put("/v2/cases/missing",
                new StubResponse(404, ""));

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

        client = new WebApiClient("http://127.0.0.1:" + server.getAddress().getPort() + "/");
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @Test
    void listCasesReturnsBothCases() throws Exception {
        List<Map<String, Object>> cases = client.listCases();
        assertEquals(2, cases.size());
        assertEquals("c1", cases.get(0).get("id"));
    }

    @Test
    void getCaseEncodesPathSegment() throws Exception {
        client.getCase("my case");
        assertEquals("GET null", lastRequests.get("/v2/cases/my%20case"));
    }

    @Test
    void notFoundThrowsWebApiException() {
        WebApiException ex = assertThrows(WebApiException.class, () -> client.getCase("missing"));
        assertTrue(ex.getMessage().contains("Not found"), "got: " + ex.getMessage());
    }

    @Test
    void searchEncodesQueryAndPaginationParams() throws Exception {
        SearchPageDto page = client.search("foo bar", 0, 20);
        assertEquals(0, page.getOffset());
        assertEquals(20, page.getLimit());
        String query = lastRequests.get("/v2/search");
        assertTrue(query.contains("q=foo"), "expected q= in: " + query);
        assertTrue(query.contains("offset=0"),  "expected offset= in: " + query);
        assertTrue(query.contains("limit=20"),  "expected limit= in: " + query);
    }

    @Test
    void listBookmarksParsesV2Shape() throws Exception {
        List<String> names = client.listBookmarks();
        assertEquals(List.of("tag1", "tag2"), names);
    }

    @Test
    void getItemTextReturnsBody() throws Exception {
        String text = client.getItemText("src1", 7, "");
        assertEquals("hello world", text);
    }

    @Test
    void getItemMetadataDeserializesDto() throws Exception {
        ItemMetadataDto dto = client.getItemMetadata("src1", 8);
        assertEquals("doc.txt", dto.getName());
        assertEquals("text/plain", dto.getMediaType());
        assertEquals(100L, dto.getSize());
    }

    @Test
    void listCategoriesReturnsList() throws Exception {
        List<String> cats = client.listCategories("src1");
        assertEquals(List.of("Images", "Documents"), cats);
    }
}
