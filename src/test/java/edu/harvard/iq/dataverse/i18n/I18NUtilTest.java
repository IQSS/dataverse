package edu.harvard.iq.dataverse.i18n;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Locale;

public class I18NUtilTest {

    @Test
    void testParseAcceptLanguageHeader_singleLanguage() {
        Locale locale = I18nUtil.parseAcceptLanguageHeader("en-US");
        assertEquals(Locale.forLanguageTag("en-US"), locale);
    }

    @Test
    void testParseAcceptLanguageHeader_singleLanguageWithQ() {
        Locale locale = I18nUtil.parseAcceptLanguageHeader("en-US,en;q=0.5");
        assertEquals(Locale.forLanguageTag("en-US"), locale);
    }

    @Test
    void testParseAcceptLanguageHeader_multipleLanguages() {
        Locale locale = I18nUtil.parseAcceptLanguageHeader("fr-CA,fr;q=0.8,en-US;q=0.6,en;q=0.4");
        assertEquals(Locale.forLanguageTag("fr-CA"), locale);
    }

    @Test
    void testParseAcceptLanguageHeader_emptyHeader() {
        Locale locale = I18nUtil.parseAcceptLanguageHeader("");
        assertNull(locale);
    }

    @Test
    void testParseAcceptLanguageHeader_nullHeader() {
        Locale locale = I18nUtil.parseAcceptLanguageHeader(null);
        assertNull(locale);
    }

    @Test
    void testParseAcceptLanguageHeader_invalidHeader() {
        Locale locale = I18nUtil.parseAcceptLanguageHeader("invalid-header");
        assertEquals(Locale.forLanguageTag("invalid-header"), locale);
    }
}