package edu.harvard.iq.dataverse.i18n;

import java.util.List;
import java.util.Locale;

public class I18nUtil {

    /**
     * A comment from poikilotherm from
     * https://github.com/IQSS/dataverse/pull/11753#discussion_r2787986962
     * 
     * IMHO any parsing of the locale should be done using JAX-RS mechanisms to
     * follow DRY principle.
     * 
     * Solution A) Keep @HeaderParam, but make it a Locale, moving the parsing to a
     * ParamConverter. This is still a lot of repeated boilerplate code.
     * 
     * Solution B) Have a @Context HttpHeaders parameter give you access via
     * headers.getAcceptableLanguages() or a @Context Request give you access via
     * request.getLanguage() to the Locale without manual parsing. Still some
     * boilerplate per method
     * 
     * Solution C) Create a CDI @Producer method that is @RequestScoped, receiving
     * the Locale as a class field @Inject Locale. This would be greatly enhanced by
     * adding an annotation like @ClientLocale to be used as qualifier for both
     * field and producer method. This is the least boilerplate code.
     */

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
