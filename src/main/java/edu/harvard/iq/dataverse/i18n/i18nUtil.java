package edu.harvard.iq.dataverse.i18n;

import java.util.List;
import java.util.Locale;

public class i18nUtil {

    /**
     * @param acceptLanguageHeader The Accept-Language header value such as
     *                             "Accept-Language: en-US,en;q=0.5"
     * @return The first Locale or null.
     */
    public static Locale parseAcceptLanguageHeader(String acceptLanguageHeader) {
        if (acceptLanguageHeader == null || acceptLanguageHeader.isEmpty()) {
            return null;
        }
        List<Locale.LanguageRange> list = Locale.LanguageRange.parse(acceptLanguageHeader);
        if (list.isEmpty()) {
            return null;
        }
        Locale.LanguageRange languageRange = list.get(0);
        return Locale.forLanguageTag(languageRange.getRange());
    }

}
