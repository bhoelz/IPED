package iped.localization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Messages} class.
 */
class MessagesTest {

    @Test
    @DisplayName("BUNDLES_FOLDER constant should be 'localization'")
    void bundlesFolderConstant() {
        assertEquals("localization", Messages.BUNDLES_FOLDER);
    }

    @Test
    @DisplayName("BUNDLES_FOLDER_PREFIX constant should be 'iped-app/resources/'")
    void bundlesFolderPrefixConstant() {
        assertEquals("iped-app/resources/", Messages.BUNDLES_FOLDER_PREFIX);
    }

    @Test
    @DisplayName("getExternalBundle with known bundle should return non-null ResourceBundle")
    void getExternalBundle_withKnownBundle_shouldReturnBundle() {
        ResourceBundle bundle = Messages.getExternalBundle("iped-properties", Locale.getDefault());
        assertNotNull(bundle, "Expected iped-properties bundle to be loadable");
    }

    @Test
    @DisplayName("getExternalBundle with known bundle should contain expected keys")
    void getExternalBundle_withKnownBundle_shouldContainExpectedKeys() {
        ResourceBundle bundle = Messages.getExternalBundle("iped-properties", Locale.getDefault());
        assertNotNull(bundle);
        assertTrue(bundle.getKeys().hasMoreElements(), "Bundle should have at least one key");
    }

    @Test
    @DisplayName("getExternalBundle with non-existent bundle should throw exception")
    void getExternalBundle_withNonExistentBundle_shouldThrow() {
        assertThrows(Exception.class, () -> Messages.getExternalBundle("non-existent-bundle-xyz", Locale.getDefault()));
    }
}
