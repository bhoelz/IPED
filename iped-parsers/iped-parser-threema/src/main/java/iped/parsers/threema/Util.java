package iped.parsers.threema;

import iped.data.IItemReader;
import iped.search.IItemSearcher;
import iped.utils.IOUtil;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author André Rodrigues Costa <pfeifer.fmp@pf.gov.br>
 */
public class Util {

  public static String encodeBase64(byte[] data) {
    return Base64.getEncoder().encodeToString(data);
  }

  public static String getUTF8String(ResultSet rs, String field) throws SQLException {
    String result = null;
    byte[] bytes = rs.getBytes(field);
    if (bytes != null) {
      result = new String(bytes, StandardCharsets.UTF_8);
    }
    return result;
  }

  public static String getNameFromId(String id) {
    if (id != null && id.contains("@")) { // $NON-NLS-1$
      id = id.split("@")[0]; // $NON-NLS-1$
    }
    return id;
  }

  public static String readResourceAsString(String resource) {
    byte[] bytes = readResourceAsBytes(resource);
    String result = ""; // $NON-NLS-1$
    if (bytes != null) {
      result = new String(bytes, StandardCharsets.UTF_8);
    }
    return result;
  }

  private static byte[] readResourceAsBytes(String resource) {
    byte[] result = null;
    try {
      result = Util.class.getResourceAsStream(resource).readAllBytes();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return result;
  }

  public static String readResourceAsBase64String(String resource) {
    byte[] bytes = readResourceAsBytes(resource);
    String result = ""; // $NON-NLS-1$
    if (bytes != null) {
      result = encodeBase64(bytes);
    }
    return result;
  }

  public static String getImageResourceAsEmbedded(String resource) {
    String ext = resource.substring(resource.lastIndexOf('.') + 1);
    String type = "jpg"; // $NON-NLS-1$
    if (ext.length() > 0) {
      type = ext;
    }
    return "data:image/"
        + type
        + ";base64,"
        + readResourceAsBase64String(resource); // $NON-NLS-1$ $NON-NLS-2$
  }

  public static String nullToEmpty(String s) {
    if (s == null) {
      return ""; //$NON-NLS-1$
    }
    return s;
  }

  // Report-path helpers — replicate iped.parsers.util.Util to avoid circular dep

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

  public static Optional<String> getSourceFileIfExists(IItemReader item) {
    if (IOUtil.hasFile(item)) {
      File f = IOUtil.getFile(item);
      if (f != null && f.exists()) {
        Path p = f.toPath().toAbsolutePath().normalize();
        return Optional.of(adjustPath(p.toString()));
      }
    }
    return Optional.empty();
  }

  public static String getReportHref(IItemReader item) {
    String exportPath = getExportPath(item);
    String originalPath = getSourceFileIfExists(item).orElse("");
    String type =
        item.getMediaType() != null
            ? ((org.apache.tika.mime.MediaType) item.getMediaType()).getType()
            : "";
    String openMethod;
    if ("image".equals(type)) openMethod = "Image";
    else if ("audio".equals(type)) openMethod = "Audio";
    else if ("video".equals(type)) openMethod = "Video";
    else openMethod = "Other";
    return "javascript:open" + openMethod + "('" + exportPath + "','" + originalPath + "')";
  }

  private static String adjustPath(String path) {
    path = path.replace('\\', '/');
    if (path.startsWith("/")) path = path.substring(1);
    if (path.length() > 2 && path.charAt(1) == ':') path = "file:///" + path;
    return path;
  }
}
