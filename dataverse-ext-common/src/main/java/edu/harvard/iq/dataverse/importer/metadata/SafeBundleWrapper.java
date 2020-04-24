package edu.harvard.iq.dataverse.importer.metadata;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class SafeBundleWrapper {
    private static final Logger logger = Logger.getLogger(SafeBundleWrapper.class.getSimpleName());

    private ResourceBundle bundle;
    private boolean missingBundle;

    // -------------------- CONSTRUCTORS --------------------

    public SafeBundleWrapper(MetadataImporter importer, Locale locale) {
        if (locale == null || importer == null) {
            logger.warning("Null importer or locale received");
            this.missingBundle = true;
            return;
        }
        try {
            this.bundle = importer.getBundle(locale);
            this.missingBundle = false;
        } catch (MissingResourceException | NullPointerException ex) {
            logger.warning("Cannot find bundle for importer [" + importer
                    + "] and locale [" + locale.toLanguageTag() +"]");
            this.missingBundle = true;
        }
    }

    // -------------------- LOGIC --------------------

    public String getString(String key) {
        if (key == null) {
            return StringUtils.EMPTY;
        } else if (missingBundle) {
            return formatKey(key);
        }
        try {
            return bundle.getString(key);
        } catch (MissingResourceException mre) {
            return formatKey(key);
        }
    }

    // -------------------- PRIVATE --------------------

    private String formatKey(String key) {
        return "[" + key + "]";
    }
}
