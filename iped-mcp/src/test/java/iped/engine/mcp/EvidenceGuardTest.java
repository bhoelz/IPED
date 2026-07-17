package iped.engine.mcp;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EvidenceGuardTest {

  @Test
  void wrapIncludesAllAttributes() {
    String out = EvidenceGuard.wrap("src0", "src0:42", "text/plain", "hello");
    assertTrue(out.contains("source=\"src0\""));
    assertTrue(out.contains("item=\"src0:42\""));
    assertTrue(out.contains("media-type=\"text/plain\""));
    assertTrue(out.contains("hello"));
    assertFalse(out.contains("truncated"));
  }

  @Test
  void wrapTruncatesLongContent() {
    String longText = "x".repeat(EvidenceGuard.MAX_CHARS + 5000);
    String out = EvidenceGuard.wrap("s", "s:1", "text/plain", longText);
    assertTrue(out.contains("truncated=\"true\""), "missing truncated attribute");
    assertTrue(out.contains("[TRUNCATED"), "missing truncation marker");
  }

  @Test
  void wrapDoesNotTruncateShortContent() {
    String short_ = "hello world";
    String out = EvidenceGuard.wrap("s", "s:1", "text/plain", short_);
    assertFalse(out.contains("truncated"), "should not be truncated");
    assertTrue(out.contains("hello world"));
  }

  @Test
  void wrapEscapesDoubleQuotesInAttributes() {
    String out = EvidenceGuard.wrap("src\"x", "id", "text/plain", "body");
    // The &quot; entity must appear, not a raw double-quote that would break XML
    assertTrue(out.contains("&quot;"), "expected &quot; in: " + out);
  }

  @Test
  void wrapNullTextProducesEmptyBody() {
    String out = EvidenceGuard.wrap("s", "s:1", "text/plain", null);
    assertTrue(out.startsWith("<iped-evidence"));
    assertTrue(out.endsWith("</iped-evidence>"));
  }

  @Test
  void binaryPlaceholderContainsBinaryFlagAndMediaType() {
    String out = EvidenceGuard.binaryPlaceholder("src0", "src0:5", "image/jpeg", 204800L);
    assertTrue(out.contains("binary=\"true\""));
    assertTrue(out.contains("image/jpeg"));
    assertTrue(out.contains("204800"));
  }

  @Test
  void binaryPlaceholderHandlesUnknownSize() {
    String out = EvidenceGuard.binaryPlaceholder("s", "s:1", "video/mp4", -1L);
    assertTrue(out.contains("unknown size"));
  }
}
