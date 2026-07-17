package iped.parsers.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import iped.data.IItemReader;
import iped.search.IItemSearcher;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class P2PUtilTest {

  /** Stub searcher that always returns a fixed result. */
  private static IItemSearcher stubSearcher(List<IItemReader> result) {
    return new IItemSearcher() {
      @Override
      public List<IItemReader> search(String query) {
        return result;
      }

      @Override
      public Iterable<IItemReader> searchIterable(String query) {
        return result;
      }

      @Override
      public String escapeQuery(String query) {
        return query;
      }

      @Override
      public void close() {}
    };
  }

  @Test
  void searchItemInCase_whenNullSearcher_thenNull() {
    assertNull(P2PUtil.searchItemInCase(null, "MD5", "abc123"));
  }

  @Test
  void searchItemInCase_whenNullHash_thenNull() {
    assertNull(P2PUtil.searchItemInCase(stubSearcher(null), "MD5", null));
  }

  @Test
  void searchItemInCase_whenBlankHash_thenNull() {
    assertNull(P2PUtil.searchItemInCase(stubSearcher(null), "MD5", "   "));
  }

  @Test
  void searchItemInCase_whenEmptyHash_thenNull() {
    assertNull(P2PUtil.searchItemInCase(stubSearcher(null), "MD5", ""));
  }

  @Test
  void searchItemInCase_whenSearcherReturnsNull_thenNull() {
    assertNull(P2PUtil.searchItemInCase(stubSearcher(null), "MD5", "deadbeef"));
  }

  @Test
  void searchItemInCase_whenSearcherReturnsEmptyList_thenNull() {
    assertNull(P2PUtil.searchItemInCase(stubSearcher(Collections.emptyList()), "MD5", "deadbeef"));
  }

  // --- ExportFolder ---

  @Test
  void exportFolder_setAndGet_roundTrip() {
    String original = ExportFolder.getExportPath();
    try {
      ExportFolder.setExportPath("/tmp/exports");
      assertEquals("/tmp/exports", ExportFolder.getExportPath());
    } finally {
      // restore original
      if (original != null && !original.isEmpty()) {
        ExportFolder.setExportPath(original);
      } else {
        System.clearProperty("iped.exportFolder");
      }
    }
  }

  @Test
  void exportFolder_default_thenEmptyString() {
    String prev = System.getProperty("iped.exportFolder");
    try {
      System.clearProperty("iped.exportFolder");
      assertEquals("", ExportFolder.getExportPath());
    } finally {
      if (prev != null) System.setProperty("iped.exportFolder", prev);
    }
  }
}
