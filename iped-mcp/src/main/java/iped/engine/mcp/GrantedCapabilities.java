package iped.engine.mcp;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Write-side capability flags granted to an MCP session.
 *
 * <p>Parsed from {@code --capabilities=bookmarks,jobs}. The default (no flag) is read-only — no
 * mutating tools are registered.
 *
 * <ul>
 *   <li>{@link #BOOKMARKS} — enables bookmark/tag write tools
 *   <li>{@link #JOBS} — enables async job tools (export, report)
 * </ul>
 */
public enum GrantedCapabilities {
  BOOKMARKS,
  JOBS;

  public static Set<GrantedCapabilities> parse(String csv) {
    if (csv == null || csv.isBlank()) return Collections.emptySet();
    return Arrays.stream(csv.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(
            s -> {
              try {
                return GrantedCapabilities.valueOf(s.toUpperCase());
              } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    "Unknown capability: \"" + s + "\". Valid values: bookmarks, jobs");
              }
            })
        .collect(Collectors.toCollection(() -> EnumSet.noneOf(GrantedCapabilities.class)));
  }
}
