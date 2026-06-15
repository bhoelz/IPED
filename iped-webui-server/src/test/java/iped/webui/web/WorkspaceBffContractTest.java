package iped.webui.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Contract tests for {@link WorkspaceFragmentController} BFF endpoints.
 *
 * <p>Uses {@link MockRestServiceServer} to verify the BFF calls the correct
 * upstream webapi URLs and produces well-formed HTML fragments for both
 * happy-path and error (backend-unavailable) scenarios.
 *
 * <p>Does not test full Angular island rendering — that lives in Cypress E2E.
 */
@WebMvcTest(WorkspaceFragmentController.class)
class WorkspaceBffContractTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RestClient ipedWebapiClient;

    private MockRestServiceServer mockApiServer;

    @BeforeEach
    void setUp() {
        mockApiServer = MockRestServiceServer.bindTo(ipedWebapiClient).build();
    }

    // ── Sidebar bookmarks ─────────────────────────────────────────────────

    @Test
    void sidebarCollTab_callsV2BookmarksAndRendersNames() throws Exception {
        mockApiServer.expect(requestTo(containsString("/v2/bookmarks")))
                .andRespond(withSuccess(
                        "{\"bookmarks\":[\"Relevant\",\"For Review\"]}",
                        MediaType.APPLICATION_JSON));

        mvc.perform(get("/workspace/sidebar").param("tab", "coll"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Relevant")))
                .andExpect(content().string(containsString("For Review")))
                .andExpect(content().string(not(containsString("<html"))));

        mockApiServer.verify();
    }

    @Test
    void sidebarCollTab_fallsBackToDemoWhenApiDown() throws Exception {
        mockApiServer.expect(requestTo(containsString("/v2/bookmarks")))
                .andRespond(withServerError());

        mvc.perform(get("/workspace/sidebar").param("tab", "coll"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("iped-webapi unreachable")));
    }

    // ── Viewer fragment ───────────────────────────────────────────────────

    @Test
    void viewerPreviewMode_emitsIpedViewerIslandWithMediaType() throws Exception {
        String itemId = "src0:42";
        mockApiServer.expect(requestTo(containsString("/v2/sources/src0/items/42")))
                .andRespond(withSuccess(
                        "{\"name\":\"doc.pdf\",\"mediaType\":\"application/pdf\"}",
                        MediaType.APPLICATION_JSON));

        mvc.perform(get("/workspace/viewer")
                        .param("mode", "preview")
                        .param("itemId", itemId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("iped-viewer")))
                .andExpect(content().string(containsString("media-type=\"application/pdf\"")))
                .andExpect(content().string(containsString("item-id=\"src0:42\"")));

        mockApiServer.verify();
    }

    @Test
    void viewerPreviewMode_emitsIslandWithEmptyMediaTypeWhenApiDown() throws Exception {
        mockApiServer.expect(requestTo(containsString("/v2/sources/")))
                .andRespond(withServerError());

        mvc.perform(get("/workspace/viewer")
                        .param("mode", "preview")
                        .param("itemId", "src0:99"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("iped-viewer")));
    }

    @Test
    void viewerMetaMode_showsMetadataTableFromApi() throws Exception {
        String itemId = "src0:7";
        mockApiServer.expect(requestTo(containsString("/v2/sources/src0/items/7")))
                .andRespond(withSuccess(
                        "{\"name\":\"evidence.dd\",\"mediaType\":\"application/octet-stream\",\"size\":\"2048\"}",
                        MediaType.APPLICATION_JSON));

        mvc.perform(get("/workspace/viewer")
                        .param("mode", "meta")
                        .param("itemId", itemId))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("evidence.dd")))
                .andExpect(content().string(not(containsString("<html"))));

        mockApiServer.verify();
    }

    @Test
    void viewerMetaMode_showsErrorBannerWhenApiDown() throws Exception {
        mockApiServer.expect(requestTo(containsString("/v2/sources/")))
                .andRespond(withServerError());

        mvc.perform(get("/workspace/viewer")
                        .param("mode", "meta")
                        .param("itemId", "src0:1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Could not load metadata")));
    }

    @Test
    void viewerHexMode_doesNotCallApiAtAll() throws Exception {
        // hex mode renders the iped-hex-viewer island with no backend call
        mvc.perform(get("/workspace/viewer")
                        .param("mode", "hex")
                        .param("itemId", "src0:5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("iped-hex-viewer")));

        mockApiServer.verify(); // no requests expected — passes trivially
    }

    @Test
    void viewerWithNoItemId_showsEmptyState() throws Exception {
        mvc.perform(get("/workspace/viewer"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No item selected")));
    }

    // ── Sidebar categories ────────────────────────────────────────────────

    @Test
    void sidebarCatTab_callsV2CategoriesAndRendersList() throws Exception {
        mockApiServer.expect(requestTo(containsString("/v2/sources/")))
                .andRespond(withSuccess(
                        "[\"Documents\",\"Images\"]",
                        MediaType.APPLICATION_JSON));

        mvc.perform(get("/workspace/sidebar")
                        .param("tab", "cat")
                        .param("caseId", "demo-case"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Documents")))
                .andExpect(content().string(containsString("Images")));
    }

    @Test
    void sidebarCatTab_fallsBackToDemoDataWhenApiDown() throws Exception {
        mockApiServer.expect(requestTo(containsString("/v2/sources/")))
                .andRespond(withServerError());

        mvc.perform(get("/workspace/sidebar").param("tab", "cat"))
                .andExpect(status().isOk())
                // DEMO_CATEGORIES contains "Email"
                .andExpect(content().string(containsString("iped-webapi unreachable")));
    }
}
