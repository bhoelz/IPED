package iped.localization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LocaleResolverTest {

    @AfterEach
    void cleanup() {
        System.clearProperty(LocaleResolver.LOCALE_SYS_PROP);
    }

    @Test
    void getLocale_whenPropertySet_thenUsesConfiguredLanguageTag() {
        System.setProperty(LocaleResolver.LOCALE_SYS_PROP, "pt-BR");

        Locale locale = LocaleResolver.getLocale();

        assertEquals("pt-BR", locale.toLanguageTag());
    }

    @Test
    void getLocale_whenPropertyNotSet_thenUsesDefaultLocale() {
        System.clearProperty(LocaleResolver.LOCALE_SYS_PROP);

        assertNotNull(LocaleResolver.getLocale());
    }

    @Test
    void setLocale_whenCalled_thenStoresLanguageTagInSystemProperty() {
        LocaleResolver.setLocale(Locale.GERMANY);

        assertEquals("de-DE", LocaleResolver.getLocaleString());
    }
}

