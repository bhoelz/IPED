package iped.parsers.skype;

import iped.data.IItemReader;
import iped.search.IItemSearcher;
import java.util.Collections;
import java.util.List;

// Report-path helpers — replicate iped.parsers.util.Util to avoid a dependency
// back on iped-parsers-impl (which depends on this module via the parser SPI).
public class Util {

  public static List<IItemReader> getItems(String query, IItemSearcher searcher) {
    if (searcher == null) return Collections.emptyList();
    return searcher.search(query);
  }

  public static String getExportPath(IItemReader item) {
    return getExportPath(item.getHash(), item.getType());
  }

  public static String getExportPath(String hash, String ext) {
    if (hash == null || hash.length() < 2) return "";
    StringBuilder sb = new StringBuilder();
    sb.append("../../")
        .append(hash.charAt(0))
        .append("/")
        .append(hash.charAt(1))
        .append("/")
        .append(hash);
    if (ext != null && !ext.isEmpty()) sb.append(".").append(ext);
    return sb.toString();
  }
}
