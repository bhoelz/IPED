package iped.tasks.cli;

import java.util.Map;

/**
 * Outcome of running one task against one item.
 *
 * @param status OK, UNSUPPORTED (structurally incompatible with standalone execution) or ERROR (a
 *     normal processing failure)
 * @param reason human-readable explanation; {@code null} when status is OK
 * @param metadata Tika metadata name -> values, captured by {@link CollectingSinkTask}; {@code
 *     null} unless status is OK
 * @param extraAttributes item.getExtraAttributeMap(), captured the same way; {@code null} unless
 *     status is OK
 */
public record ItemResult(
    String path,
    String status,
    String reason,
    long elapsedMs,
    Map<String, String[]> metadata,
    Map<String, Object> extraAttributes) {

  public static final String OK = "OK";
  public static final String UNSUPPORTED = "UNSUPPORTED";
  public static final String ERROR = "ERROR";

  public static ItemResult ok(
      String path,
      long elapsedMs,
      Map<String, String[]> metadata,
      Map<String, Object> extraAttributes) {
    return new ItemResult(path, OK, null, elapsedMs, metadata, extraAttributes);
  }

  public static ItemResult unsupported(String path, String reason) {
    return new ItemResult(path, UNSUPPORTED, reason, 0, null, null);
  }

  public static ItemResult error(String path, String reason, long elapsedMs) {
    return new ItemResult(path, ERROR, reason, elapsedMs, null, null);
  }
}
