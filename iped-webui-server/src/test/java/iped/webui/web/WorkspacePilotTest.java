package iped.webui.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Wiring checks for the vertical-slice pilot, using MockMvc (WebEnvironment.MOCK)
 * so no embedded Tomcat is required — tests pass in environments where the JDK
 * NIO selector fails to open a loopback pipe (e.g. sandboxed JVMs on Windows).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WithMockUser(username = "analyst", roles = "ANALYST")
class WorkspacePilotTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void rootRedirectsToCasePicker() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/cases"));
    }

    @Test
    void workspaceWithoutCaseIdRedirectsToCasePicker() throws Exception {
        mvc.perform(get("/workspace"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/cases"));
    }

    @Test
    void workspacePageEmbedsIslandHostAndHtmxRegions() throws Exception {
        mvc.perform(get("/workspace").param("caseId", "demo-case"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<iped-results-grid")))
                .andExpect(content().string(containsString("hx-get=\"/workspace/sidebar")));
    }

    @Test
    void workspacePageEmbedsMapIslandWithTileConfig() throws Exception {
        mvc.perform(get("/workspace").param("caseId", "demo-case"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<iped-map")))
                .andExpect(content().string(containsString("source-id=\"demo-case\"")))
                .andExpect(content().string(containsString("tile-url=")))
                .andExpect(content().string(containsString("openstreetmap.org")));
    }

    @Test
    void workspaceUsesDelegatedDataActionsForUntrustedFragmentValues() throws Exception {
        mvc.perform(get("/workspace").param("caseId", "demo-case"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("document.addEventListener('click'")))
                .andExpect(content().string(containsString("data-action")))
                .andExpect(content().string(not(containsString("onclick=\"ipedSelectItem('"))))
                .andExpect(content().string(not(containsString("onclick=\"ipedSelectFilter('"))))
                .andExpect(content().string(not(containsString("onclick=\"ipedEvidToggle('"))))
                .andExpect(content().string(not(containsString("onclick=\"ipedAiToggle('"))));

        mvc.perform(get("/workspace/sidebar").param("tab", "cat"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-action=\"select-filter\"")))
                .andExpect(content().string(not(containsString("onclick=\"ipedSelectFilter('"))));
    }

    @Test
    void sidebarFragmentReturnsHtmlPartialNotFullPage() throws Exception {
        mvc.perform(get("/workspace/sidebar").param("tab", "meta"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Has hits")))
                .andExpect(content().string(not(containsString("<html"))));
    }

    @Test
    void casePickerRendersOpenCaseForm() throws Exception {
        mvc.perform(get("/cases"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<form")))
                .andExpect(content().string(containsString("action=\"/cases\"")))
                .andExpect(content().string(not(containsString("<iped-results-grid"))));
    }

    @Test
    void casePickerRendersErrorBannerWhenErrorParamPresent() throws Exception {
        mvc.perform(get("/cases").param("error", "Case not found"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Case not found")));
    }

    @Test
    void searchBffCreatesSearchTokenAndReturnsSearchId() throws Exception {
        mvc.perform(post("/api/cases/demo-case/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"x\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"searchId\"")));
    }
}
