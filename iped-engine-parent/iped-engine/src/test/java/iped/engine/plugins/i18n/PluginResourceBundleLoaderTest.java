package iped.engine.plugins.i18n;

import static org.junit.Assert.*;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import org.junit.Before;
import org.junit.Test;

/** Tests for PluginResourceBundleLoader. */
public class PluginResourceBundleLoaderTest {

  private PluginResourceBundleLoader loader;

  @Before
  public void setUp() {
    loader = new PluginResourceBundleLoader();
  }

  @Test
  public void testGetSupportedLocales() {
    String[] locales = loader.getSupportedLocales();
    assertEquals(4, locales.length);
    assertArrayEquals(new String[] {"pt_BR", "en_US", "es_AR", "de_DE"}, locales);
  }

  @Test
  public void testIsSupportedLocale() {
    assertTrue(loader.isSupportedLocale("pt_BR"));
    assertTrue(loader.isSupportedLocale("en_US"));
    assertTrue(loader.isSupportedLocale("es_AR"));
    assertTrue(loader.isSupportedLocale("de_DE"));
    assertFalse(loader.isSupportedLocale("fr_FR"));
    assertFalse(loader.isSupportedLocale("invalid"));
  }

  @Test
  public void testDefaultLocale() {
    assertEquals(new Locale("pt", "BR"), loader.getCurrentLocale());
  }

  @Test
  public void testSetCurrentLocale() {
    loader.setCurrentLocale("en_US");
    assertEquals(new Locale("en", "US"), loader.getCurrentLocale());

    loader.setCurrentLocale("es_AR");
    assertEquals(new Locale("es", "AR"), loader.getCurrentLocale());
  }

  @Test
  public void testSetCurrentLocaleWithLocaleObject() {
    loader.setCurrentLocale(new Locale("de", "DE"));
    assertEquals(new Locale("de", "DE"), loader.getCurrentLocale());
  }

  @Test
  public void testSetInvalidLocale() {
    // Should not change locale
    loader.setCurrentLocale("fr_FR");
    assertEquals(new Locale("pt", "BR"), loader.getCurrentLocale());
  }

  @Test
  public void testSetNullLocale() {
    // Should not change locale
    loader.setCurrentLocale((Locale) null);
    assertEquals(new Locale("pt", "BR"), loader.getCurrentLocale());
  }

  @Test
  public void testGetMessageKeyNotFound() {
    String msg = loader.getMessage("nonexistent-component", "key.does.not.exist");
    assertEquals("key.does.not.exist", msg);
  }

  @Test
  public void testGetMessageWithArguments() {
    // Without actual bundles loaded, should return key
    String msg = loader.getMessage("test", "welcome", "John");
    assertEquals("welcome", msg);
  }

  @Test
  public void testGetComponentBundlesEmpty() {
    Map<String, ResourceBundle> bundles = loader.getComponentBundles("nonexistent");
    assertTrue(bundles.isEmpty());
  }

  @Test
  public void testStatistics() {
    Map<String, Object> stats = loader.getStatistics();
    assertEquals(0, stats.get("cached_bundles"));
    assertEquals(4, stats.get("supported_locales"));
    assertNotNull(stats.get("current_locale"));
  }

  @Test
  public void testClearCache() {
    loader.clearCache();
    Map<String, Object> stats = loader.getStatistics();
    assertEquals(0, stats.get("cached_bundles"));
  }

  @Test
  public void testToString() {
    String str = loader.toString();
    assertNotNull(str);
    assertTrue(str.contains("PluginResourceBundleLoader"));
  }

  @Test
  public void testLocaleDetection() {
    // Test that system locale is handled correctly
    Locale systemLocale = Locale.getDefault();
    // We can't predict what it is, but it should be handled gracefully
    loader.setCurrentLocale(systemLocale);
    // Should not crash
    assertNotNull(loader.getCurrentLocale());
  }

  @Test
  public void testMessageCaching() {
    // First call - not in cache (no bundle loaded)
    String msg1 = loader.getMessage("test", "key1");
    assertEquals("key1", msg1); // Not found

    // Second call - should hit cache
    String msg2 = loader.getMessage("test", "key1");
    assertEquals("key1", msg2); // Still not found

    Map<String, Object> stats = loader.getStatistics();
    assertEquals(0, stats.get("cached_bundles"));
  }

  @Test
  public void testGetFormattedMessageNoParams() {
    // Without actual bundle
    String msg = loader.getFormattedMessage("test", "message");
    assertEquals("message", msg);
  }

  @Test
  public void testLocaleStringConversion() {
    // Test that locale conversion is correct
    loader.setCurrentLocale("en_US");
    Locale locale = loader.getCurrentLocale();
    assertEquals("en", locale.getLanguage());
    assertEquals("US", locale.getCountry());
  }

  @Test
  public void testMultipleComponents() {
    // Just test that loader can handle multiple component IDs
    String msg1 = loader.getMessage("component1", "key");
    String msg2 = loader.getMessage("component2", "key");

    assertEquals("key", msg1);
    assertEquals("key", msg2);
  }

  @Test
  public void testSwitchingLocales() {
    String locale1 = "en_US";
    String locale2 = "es_AR";

    loader.setCurrentLocale(locale1);
    assertEquals(new Locale("en", "US"), loader.getCurrentLocale());

    loader.setCurrentLocale(locale2);
    assertEquals(new Locale("es", "AR"), loader.getCurrentLocale());

    loader.setCurrentLocale(locale1);
    assertEquals(new Locale("en", "US"), loader.getCurrentLocale());
  }

  @Test
  public void testGetMessageWithSpecificLocale() {
    // Request message with specific locale (even without bundle)
    String msg = loader.getMessage("test", "key", new Locale("de", "DE"));
    assertEquals("key", msg);
  }
}
