package iped.webui.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Wiring checks for the vertical-slice pilot, over the real HTTP stack: the SSR
 * page composes the island host + HTMX regions, fragments render HTML partials,
 * and the search stub returns contract-shaped JSON.
 *
 * <p>Uses the JDK HTTP client + the {@code local.server.port} environment
 * property so it stays robust across Spring Boot test-module restructuring.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkspacePilotTest {

    private final HttpClient http = HttpClient.newHttpClient();

    @Autowired
    private Environment env;

    private URI url(String path) {
        return URI.create("http://localhost:" + env.getProperty("local.server.port") + path);
    }

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(url(path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void workspacePageEmbedsIslandHostAndHtmxRegions() throws Exception {
        HttpResponse<String> res = get("/workspace");
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.body())
                .contains("<iped-results-grid")
                .contains("hx-get=\"/workspace/sidebar");
    }

    @Test
    void sidebarFragmentReturnsHtmlPartialNotFullPage() throws Exception {
        HttpResponse<String> res = get("/workspace/sidebar?tab=meta");
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.body()).contains("Has hits").doesNotContain("<html");
    }

    @Test
    void searchStubReturnsContractShapedResults() throws Exception {
        HttpResponse<String> created = http.send(
                HttpRequest.newBuilder(url("/api/cases/demo-case/search"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"query\":\"x\"}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(created.statusCode()).isEqualTo(200);
        assertThat(created.body()).contains("\"searchId\"");

        HttpResponse<String> page = get("/api/cases/demo-case/search/s1/results?limit=3");
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("\"total\"").contains("\"items\"");
        // limit=3 → exactly three contract-shaped items in the page
        assertThat(countOccurrences(page.body(), "\"itemId\"")).isEqualTo(3);
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length())) {
            count++;
        }
        return count;
    }
}
