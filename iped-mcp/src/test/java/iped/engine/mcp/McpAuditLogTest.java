package iped.engine.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class McpAuditLogTest {

    @Test
    void recordsSuccessEntry() {
        McpAuditLog log = new McpAuditLog();
        log.success("iped_search", Map.of("query", "bitcoin"));
        List<McpAuditLog.Entry> entries = log.all();
        assertEquals(1, entries.size());
        McpAuditLog.Entry e = entries.get(0);
        assertEquals("iped_search", e.tool());
        assertEquals(McpAuditLog.Entry.Outcome.SUCCESS, e.outcome());
        assertNull(e.errorMessage());
        assertNotNull(e.timestamp());
    }

    @Test
    void recordsErrorEntry() {
        McpAuditLog log = new McpAuditLog();
        log.error("iped_item_get", Map.of("itemId", "src0:1"), "Not found: /v2/sources/src0/items/1");
        McpAuditLog.Entry e = log.all().get(0);
        assertEquals(McpAuditLog.Entry.Outcome.ERROR, e.outcome());
        assertEquals("Not found: /v2/sources/src0/items/1", e.errorMessage());
    }

    @Test
    void recentReturnsLastN() {
        McpAuditLog log = new McpAuditLog();
        for (int i = 0; i < 10; i++) {
            log.success("tool" + i, Map.of());
        }
        List<McpAuditLog.Entry> recent = log.recent(3);
        assertEquals(3, recent.size());
        assertEquals("tool9", recent.get(2).tool());
        assertEquals("tool7", recent.get(0).tool());
    }

    @Test
    void summariseRedactsLongStrings() {
        String longVal = "a".repeat(300);
        String summary = McpAuditLog.summarise(Map.of("query", longVal));
        assertFalse(summary.contains(longVal), "long string should be redacted");
        assertTrue(summary.contains("[string:"), "should contain type placeholder");
    }

    @Test
    void summariseKeepsShortStrings() {
        String summary = McpAuditLog.summarise(Map.of("query", "bitcoin"));
        assertTrue(summary.contains("bitcoin"));
    }

    @Test
    void summariseHandlesNumbers() {
        String summary = McpAuditLog.summarise(Map.of("offset", 0, "limit", 20));
        assertTrue(summary.contains("0") && summary.contains("20"));
    }

    @Test
    void persistsToJsonlFile(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("audit.jsonl");
        McpAuditLog log = new McpAuditLog(file);
        log.success("iped_case_list", Map.of());
        log.error("iped_search", Map.of("query", "q"), "connection refused");

        List<String> lines = Files.readAllLines(file);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("iped_case_list"));
        assertTrue(lines.get(0).contains("SUCCESS"));
        assertTrue(lines.get(1).contains("ERROR"));
        assertTrue(lines.get(1).contains("connection refused"));
    }

    @Test
    void summariseEmptyMapReturnsEmptyObject() {
        assertEquals("{}", McpAuditLog.summarise(Map.of()));
    }
}
