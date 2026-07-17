package iped.engine.webapi;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves the per-source allow-list from configuration.
 *
 * <p>Configure via system property or environment variable (checked in order):
 *
 * <ol>
 *   <li>{@code iped.webapi.allowed-sources} — comma-separated source IDs
 *   <li>{@code IPED_WEBAPI_ALLOWED_SOURCES} — same format
 * </ol>
 *
 * <p>When neither is set the allow-list is {@code null}, meaning unrestricted access (all sources
 * are allowed). This preserves the open-access default for development.
 *
 * <p>The configured set is resolved once at class-load time and cached for the lifetime of the JVM.
 */
public final class AllowedSources {

  private static volatile Set<String> ALLOWED = resolve();

  // ── Public API ────────────────────────────────────────────────────────────

  /** Returns the configured allow-list, or {@code null} when unrestricted. */
  public static Set<String> get() {
    return ALLOWED;
  }

  /**
   * Returns {@code true} when {@code sourceId} may be accessed by the current session (either no
   * allow-list is configured, or the ID is in the list).
   */
  public static boolean isAllowed(String sourceId) {
    Set<String> allowed = ALLOWED;
    if (allowed == null) return true;
    return allowed.contains(sourceId);
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  /**
   * Parses a comma-separated source-ID string into an immutable set. Blank entries and surrounding
   * whitespace are discarded.
   */
  static Set<String> parse(String csv) {
    return Arrays.stream(csv.split(",", -1))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toUnmodifiableSet());
  }

  private static Set<String> resolve() {
    String prop = System.getProperty("iped.webapi.allowed-sources");
    if (prop != null && !prop.isBlank()) return parse(prop);
    String env = System.getenv("IPED_WEBAPI_ALLOWED_SOURCES");
    if (env != null && !env.isBlank()) return parse(env);
    return null;
  }

  /** Package-private override for unit tests. */
  static void overrideForTest(Set<String> allowed) {
    ALLOWED = allowed;
  }

  /** Restores the configured value after a test override. */
  static void resetToConfigured() {
    ALLOWED = resolve();
  }

  private AllowedSources() {}
}
