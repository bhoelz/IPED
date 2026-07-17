package iped.webui.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration-level fallback tests for the workspace BFF fragment endpoints.
 *
 * <p>These tests verify behavior when iped-webapi is NOT reachable (no webapi process running).
 * Controllers must return 200 with well-formed HTML, fall back to demo data or show an error state,
 * and never surface a 5xx.
 *
 * <p>Uses {@code WebEnvironment.MOCK} so no embedded Tomcat starts — tests pass in environments
 * where the JDK NIO selector is restricted.
 *
 * <p>Happy-path (live backend) coverage belongs in E2E tests (EPIC-WEB-12 / WEB-113).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WithMockUser(username = "analyst", roles = "ANALYST")
class WorkspaceBffContractTest {

  @Autowired private WebApplicationContext context;

  private MockMvc mvc;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  // ── Sidebar fallbacks ─────────────────────────────────────────────────────

  @Test
  void sidebarCatTab_fallsBackToDemoDataWhenApiDown() throws Exception {
    mvc.perform(get("/workspace/sidebar").param("tab", "cat"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
        .andExpect(content().string(containsString("iped-webapi unreachable")))
        .andExpect(content().string(not(containsString("<html"))));
  }

  @Test
  void sidebarCollTab_fallsBackToDemoWhenApiDown() throws Exception {
    mvc.perform(get("/workspace/sidebar").param("tab", "coll"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("iped-webapi unreachable")))
        .andExpect(content().string(not(containsString("<html"))));
  }

  @Test
  void sidebarEvidTab_rendersEvidenceTreeFromDemoData() throws Exception {
    mvc.perform(get("/workspace/sidebar").param("tab", "evid"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("evidence")))
        .andExpect(content().string(not(containsString("<html"))));
  }

  // ── Info panel fallbacks ───────────────────────────────────────────────────

  @Test
  void infoPanelHitsTab_returnsFragmentWhenNoItemSelected() throws Exception {
    mvc.perform(get("/workspace/info").param("tab", "hits"))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("<html"))));
  }

  @Test
  void infoPanelHitsTab_returnsFragmentForUnknownItem() throws Exception {
    mvc.perform(get("/workspace/info").param("tab", "hits").param("itemId", "src0:1"))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("<html"))));
  }

  // ── Viewer fragment fallbacks ─────────────────────────────────────────────

  @Test
  void viewerWithNoItemId_showsEmptyState() throws Exception {
    mvc.perform(get("/workspace/viewer"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No item selected")));
  }

  @Test
  void viewerHexMode_emitsIslandWithoutApiCall() throws Exception {
    mvc.perform(get("/workspace/viewer").param("mode", "hex").param("itemId", "src0:5"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("iped-hex-viewer")));
  }

  @Test
  void viewerPreviewMode_emitsIslandEvenWhenApiDown() throws Exception {
    mvc.perform(get("/workspace/viewer").param("mode", "preview").param("itemId", "src0:99"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("iped-viewer")));
  }

  @Test
  void viewerMetaMode_showsErrorBannerWhenApiDown() throws Exception {
    mvc.perform(get("/workspace/viewer").param("mode", "meta").param("itemId", "src0:1"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Could not load metadata")));
  }

  // ── Case picker ────────────────────────────────────────────────────────────

  @Test
  void casePicker_rendersOpenCaseFormEvenWhenApiDown() throws Exception {
    mvc.perform(get("/cases"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<form")))
        .andExpect(content().string(containsString("action=\"/cases\"")))
        .andExpect(content().string(containsString("iped-webapi is not reachable")));
  }

  // ── Filter state ───────────────────────────────────────────────────────────

  @Test
  void filterChipsFragment_returnsEmptyHtmlWhenNoFiltersActive() throws Exception {
    mvc.perform(get("/workspace/filter-chips"))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("<html"))));
  }
}
