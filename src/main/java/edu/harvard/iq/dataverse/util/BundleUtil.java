package edu.harvard.iq.dataverse.util;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class BundleUtil {

    private static final Logger logger = Logger.getLogger(BundleUtil.class.getCanonicalName());

    private static final String defaultBundleFile = "Bundle";
    private static final Locale defaultLocale = Locale.US;

    public static String getStringFromBundle(String key) {
        return getStringFromBundle(key, null);
    }

    public static String getStringFromBundle(String key, List<String> arguments) {
        ResourceBundle bundle = ResourceBundle.getBundle(defaultBundleFile, defaultLocale);
        return getStringFromBundle(key, arguments, bundle);

    }

    public static String getStringFromBundle(String key, List<String> arguments, ResourceBundle bundle) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        String stringFromBundle = null;
        try {
            stringFromBundle = bundle.getString(key);
            logger.info("string found: " + stringFromBundle);
        } catch (MissingResourceException ex) {
            logger.warning("Could not find key \"" + key + "\" in bundle file.");
            return null;
        }
        if (arguments != null) {
            Object[] argArray = new String[arguments.size()];
            argArray = arguments.toArray(argArray);
            return MessageFormat.format(stringFromBundle, argArray);
        } else {
            return stringFromBundle;
        }
    }

}
