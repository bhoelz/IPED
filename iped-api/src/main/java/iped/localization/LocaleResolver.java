package iped.localization;

import java.util.Locale;

/**
 * Resolves the application locale from the {@value #LOCALE_SYS_PROP} system property, falling back
 * to the JVM default locale.
 */
public class LocaleResolver {

  /** System property that overrides the application locale (language tag). */
  public static final String LOCALE_SYS_PROP = "iped-locale";

  /**
   * @return the configured application locale, or the JVM default if none was set
   */
  public static Locale getLocale() {
    String localeStr = getLocaleString();
    return localeStr != null ? Locale.forLanguageTag(localeStr) : Locale.getDefault();
  }

  /**
   * @return the configured locale language tag, or {@code null} if not set
   */
  public static String getLocaleString() {
    return System.getProperty(LOCALE_SYS_PROP);
  }

  /**
   * Sets the application locale system property.
   *
   * @param locale the locale to use from now on
   */
  public static void setLocale(Locale locale) {
    System.setProperty(LOCALE_SYS_PROP, locale.toLanguageTag());
  }
}
