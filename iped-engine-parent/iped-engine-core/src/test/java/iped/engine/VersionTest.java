package iped.engine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for {@link Version}. */
class VersionTest {

  @Test
  @DisplayName("APP_VERSION should not be null or blank")
  void appVersion_shouldNotBeNullOrBlank() {
    assertNotNull(Version.APP_VERSION);
    assertFalse(Version.APP_VERSION.isBlank());
  }

  @Test
  @DisplayName("APP_NAME_PREFIX should not be null or blank")
  void appNamePrefix_shouldNotBeNullOrBlank() {
    assertNotNull(Version.APP_NAME_PREFIX);
    assertFalse(Version.APP_NAME_PREFIX.isBlank());
  }

  @Test
  @DisplayName("APP_NAME should contain APP_NAME_PREFIX and APP_VERSION")
  void appName_shouldContainPrefixAndVersion() {
    assertNotNull(Version.APP_NAME);
    assertTrue(Version.APP_NAME.contains(Version.APP_NAME_PREFIX));
    assertTrue(Version.APP_NAME.contains(Version.APP_VERSION));
  }

  @Test
  @DisplayName("APP_EXT should be 'IPED'")
  void appExt_shouldBeIPED() {
    assertEquals("IPED", Version.APP_EXT);
  }

  @Test
  @DisplayName("APP_VERSION should follow semver-like pattern")
  void appVersion_shouldFollowPattern() {
    // At minimum should contain digits and dots
    assertTrue(
        Version.APP_VERSION.matches("\\d+\\.\\d+\\.\\d+.*"),
        "Version '" + Version.APP_VERSION + "' should match X.Y.Z pattern");
  }
}
