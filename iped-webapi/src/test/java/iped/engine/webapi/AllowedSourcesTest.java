package iped.engine.webapi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class AllowedSourcesTest {

    @AfterEach
    void resetAllowedSources() {
        AllowedSources.resetToConfigured();
    }

    // ── parse() ───────────────────────────────────────────────────────────────

    @Test
    void parsesCommaSeparatedList() {
        Set<String> result = AllowedSources.parse("case-1,case-2,case-3");
        assertEquals(Set.of("case-1", "case-2", "case-3"), result);
    }

    @Test
    void trimsWhitespaceAroundIds() {
        Set<String> result = AllowedSources.parse("  case-1 , case-2 ");
        assertEquals(Set.of("case-1", "case-2"), result);
    }

    @Test
    void skipsBlankEntries() {
        Set<String> result = AllowedSources.parse("case-1,,case-2");
        assertEquals(Set.of("case-1", "case-2"), result);
    }

    @Test
    void parsesEmptyStringToEmptySet() {
        Set<String> result = AllowedSources.parse("   ");
        assertTrue(result.isEmpty());
    }

    @Test
    void parsesSingleEntry() {
        Set<String> result = AllowedSources.parse("only-case");
        assertEquals(Set.of("only-case"), result);
    }

    // ── isAllowed() ───────────────────────────────────────────────────────────

    @Test
    void allowsAllWhenUnrestricted() {
        AllowedSources.overrideForTest(null);
        assertTrue(AllowedSources.isAllowed("any-case-id"));
        assertTrue(AllowedSources.isAllowed("another-case"));
    }

    @Test
    void allowsListedSourceId() {
        AllowedSources.overrideForTest(Set.of("case-A", "case-B"));
        assertTrue(AllowedSources.isAllowed("case-A"));
        assertTrue(AllowedSources.isAllowed("case-B"));
    }

    @Test
    void deniesUnlistedSourceId() {
        AllowedSources.overrideForTest(Set.of("case-A"));
        assertFalse(AllowedSources.isAllowed("case-B"));
        assertFalse(AllowedSources.isAllowed(""));
    }

    @Test
    void emptyAllowListDenieEverything() {
        AllowedSources.overrideForTest(Set.of());
        assertFalse(AllowedSources.isAllowed("any-source"));
    }
}
