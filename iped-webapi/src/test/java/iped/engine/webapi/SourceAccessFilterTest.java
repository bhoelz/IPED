package iped.engine.webapi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SourceAccessFilterTest {

    // ── extractSourceId — v2/sources ─────────────────────────────────────────

    @Test
    void extractsFromV2SourceItemPath() {
        assertEquals("case-1", SourceAccessFilter.extractSourceId("/v2/sources/case-1/items/42"));
    }

    @Test
    void extractsFromV2SourceCategoriesPath() {
        assertEquals("demo", SourceAccessFilter.extractSourceId("/v2/sources/demo/items/categories"));
    }

    @Test
    void extractsFromV2SourceContentPath() {
        assertEquals("src-X", SourceAccessFilter.extractSourceId("/v2/sources/src-X/items/7/content"));
    }

    @Test
    void extractsFromV2SourceTextPath() {
        assertEquals("src-X", SourceAccessFilter.extractSourceId("/v2/sources/src-X/items/7/text"));
    }

    @Test
    void extractsFromV2SourceTagsPath() {
        assertEquals("case-1", SourceAccessFilter.extractSourceId("/v2/sources/case-1/items/5/tags/relevant"));
    }

    @Test
    void percentDecodesSourceId() {
        assertEquals("case with spaces", SourceAccessFilter.extractSourceId("/v2/sources/case%20with%20spaces/items/1"));
    }

    // ── extractSourceId — v1/sources ─────────────────────────────────────────

    @Test
    void extractsFromV1SourcePath() {
        assertEquals("legacy-src", SourceAccessFilter.extractSourceId("/sources/legacy-src/docs/99"));
    }

    @Test
    void extractsFromV1SourceWithoutLeadingSlash() {
        assertEquals("legacy-src", SourceAccessFilter.extractSourceId("sources/legacy-src/docs/99"));
    }

    // ── extractSourceId — v2/cases ────────────────────────────────────────────

    @Test
    void extractsFromV2CasePath() {
        assertEquals("case-42", SourceAccessFilter.extractSourceId("/v2/cases/case-42"));
    }

    @Test
    void extractsFromV2CaseSubPath() {
        assertEquals("case-42", SourceAccessFilter.extractSourceId("/v2/cases/case-42/something"));
    }

    // ── extractSourceId — non-source paths ────────────────────────────────────

    @Test
    void returnsNullForRootPath() {
        assertNull(SourceAccessFilter.extractSourceId("/"));
    }

    @Test
    void returnsNullForV2BookmarksPath() {
        assertNull(SourceAccessFilter.extractSourceId("/v2/bookmarks"));
    }

    @Test
    void returnsNullForV2SearchPath() {
        assertNull(SourceAccessFilter.extractSourceId("/v2/search"));
    }

    @Test
    void returnsNullForV2CasesListPath() {
        // /v2/cases (no ID segment) — listing all cases is handled by CasesV2 filtering
        assertNull(SourceAccessFilter.extractSourceId("/v2/cases"));
    }

    @Test
    void returnsNullForNullInput() {
        assertNull(SourceAccessFilter.extractSourceId(null));
    }

    @Test
    void returnsNullForEmptyInput() {
        assertNull(SourceAccessFilter.extractSourceId(""));
    }
}
