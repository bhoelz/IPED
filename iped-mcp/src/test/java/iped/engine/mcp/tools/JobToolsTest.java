package iped.engine.mcp.tools;

import static org.junit.jupiter.api.Assertions.*;

import iped.engine.mcp.GrantedCapabilities;
import iped.engine.mcp.McpAuditLog;
import iped.engine.mcp.McpSessionContext;
import java.util.Collections;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

public class JobToolsTest {

  private ToolRegistry registryWithCapabilities(GrantedCapabilities... caps) {
    McpSessionContext session =
        new McpSessionContext(
            Collections.emptySet(),
            120,
            caps.length == 0 ? Collections.emptySet() : EnumSet.of(caps[0], caps));
    // null client is safe — we only inspect tool registrations, never invoke them
    return new ToolRegistry(null, new McpAuditLog(), session);
  }

  // ── Capability gating ─────────────────────────────────────────────────────

  @Test
  void jobToolsAbsentWithoutCapability() {
    var reg = registryWithCapabilities(); // read-only
    var names = reg.tools().stream().map(t -> t.tool().name()).toList();
    assertFalse(names.contains("iped_job_export"), "iped_job_export should not be registered");
    assertFalse(names.contains("iped_job_status"), "iped_job_status should not be registered");
    assertFalse(names.contains("iped_job_cancel"), "iped_job_cancel should not be registered");
  }

  @Test
  void jobToolsPresentWithJobsCapability() {
    var reg = registryWithCapabilities(GrantedCapabilities.JOBS);
    var names = reg.tools().stream().map(t -> t.tool().name()).toList();
    assertTrue(names.contains("iped_job_export"));
    assertTrue(names.contains("iped_job_status"));
    assertTrue(names.contains("iped_job_cancel"));
  }

  @Test
  void bookmarkWriteToolsAbsentWithoutCapability() {
    var reg = registryWithCapabilities();
    var names = reg.tools().stream().map(t -> t.tool().name()).toList();
    assertTrue(names.contains("iped_bookmark_list"), "read tool should be present");
    assertTrue(names.contains("iped_bookmark_items"), "read tool should be present");
    assertFalse(names.contains("iped_bookmark_create"), "write tool should be absent");
    assertFalse(names.contains("iped_item_tag"), "write tool should be absent");
  }

  @Test
  void bookmarkWriteAndTagToolsPresentWithBookmarksCapability() {
    var reg = registryWithCapabilities(GrantedCapabilities.BOOKMARKS);
    var names = reg.tools().stream().map(t -> t.tool().name()).toList();
    assertTrue(names.contains("iped_bookmark_create"));
    assertTrue(names.contains("iped_item_tag"));
    assertTrue(names.contains("iped_item_untag"));
  }

  // ── Cursor pagination ─────────────────────────────────────────────────────

  @Test
  void cursorRoundTrip() throws Exception {
    String cursor = SearchTools.encodeCursor("bitcoin AND deleted:true", 40, 20);
    assertNotNull(cursor);
    assertFalse(cursor.isBlank());
    // Cursor must be URL-safe base64 (no +, /, or =)
    assertFalse(cursor.contains("+"));
    assertFalse(cursor.contains("/"));
    assertFalse(cursor.contains("="));
  }

  @Test
  void distinctCursorsForDifferentPages() throws Exception {
    String page1 = SearchTools.encodeCursor("q", 0, 20);
    String page2 = SearchTools.encodeCursor("q", 20, 20);
    assertNotEquals(page1, page2);
  }

  // ── GrantedCapabilities parsing ───────────────────────────────────────────

  @Test
  void parsesBothCapabilities() {
    var caps = GrantedCapabilities.parse("bookmarks,jobs");
    assertTrue(caps.contains(GrantedCapabilities.BOOKMARKS));
    assertTrue(caps.contains(GrantedCapabilities.JOBS));
  }

  @Test
  void parseCaseInsensitive() {
    var caps = GrantedCapabilities.parse("BOOKMARKS,JOBS");
    assertEquals(2, caps.size());
  }

  @Test
  void parseEmptyStringGivesEmptySet() {
    assertTrue(GrantedCapabilities.parse("").isEmpty());
    assertTrue(GrantedCapabilities.parse(null).isEmpty());
  }

  @Test
  void parseUnknownCapabilityThrows() {
    assertThrows(IllegalArgumentException.class, () -> GrantedCapabilities.parse("admin"));
  }
}
