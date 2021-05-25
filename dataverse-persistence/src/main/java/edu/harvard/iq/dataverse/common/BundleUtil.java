package edu.harvard.iq.dataverse.common;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;

import javax.faces.context.FacesContext;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class BundleUtil {

    private static final Logger logger = Logger.getLogger(BundleUtil.class.getCanonicalName());

    private static final String DEFAULT_BUNDLE_FILE = "Bundle";

    private static final Set<String> INTERNAL_BUNDLE_NAMES = Sets.newHashSet(
            DEFAULT_BUNDLE_FILE, "BuiltInRoles", "MimeTypeDisplay", "MimeTypeFacets", "ValidationMessages");

    /**
     * This map is CRUCIAL for the performance of all methods from
     * the class, especially in parts for search, as the original
     * {@link ResourceBundle#getBundle(String)} could be very
     * inefficient.
     */
    private static ConcurrentMap<String, ResourceBundle> bundleCache = new ConcurrentHashMap<>();

    private static final ResourceBundle EMPTY_BUNDLE = new ResourceBundle() {
        @Override protected Object handleGetObject(String key) { return null; }
        @Override public Enumeration<String> getKeys() { return null; }
    };

    // -------------------- LOGIC --------------------

    public static String getStringFromBundle(String key, Object ... arguments) {
        return getStringFromBundleWithLocale(key, getCurrentLocale(), arguments);
    }

    public static String getStringFromBundleWithLocale(String key, Locale locale, Object... arguments) {
        String message = getStringFromPropertyFile(key, DEFAULT_BUNDLE_FILE, locale);

        return MessageFormat.format(message, arguments);
    }

    public static String getStringFromNonDefaultBundle(String key, String bundleName, Object... arguments) {
        return getStringFromNonDefaultBundleWithLocale(key, bundleName, getCurrentLocale(), arguments);
    }

    public static String getStringFromNonDefaultBundleWithLocale(String key, String bundleName, Locale locale, Object... arguments) {
        String stringFromPropertyFile = getStringFromPropertyFile(key, bundleName, locale);

        return MessageFormat.format(stringFromPropertyFile, arguments);
    }

    public static String getStringFromClasspathBundle(String key, String bundleName, Object... arguments) {
        String stringFromPropertyFile = getStringFromInternalBundle(key, bundleName, getCurrentLocale());

        return MessageFormat.format(stringFromPropertyFile, arguments);
    }

    public static Locale getCurrentLocale() {
        if (FacesContext.getCurrentInstance() == null) {
            return new Locale("en");
        } else if (FacesContext.getCurrentInstance().getViewRoot() == null) {
            return FacesContext.getCurrentInstance().getExternalContext().getRequestLocale();
        } else if ("en_US".equals(FacesContext.getCurrentInstance().getViewRoot().getLocale().getLanguage())) {
            return new Locale("en");
        }

        return FacesContext.getCurrentInstance().getViewRoot().getLocale();
    }

    // -------------------- PRIVATE --------------------

    private static String getStringFromPropertyFile(String bundleKey, String bundleName) throws MissingResourceException {
        return getStringFromPropertyFile(bundleKey, bundleName, getCurrentLocale());
    }

    /**
     * Gets display name for specified bundle key. If it is external bundle,
     * method tries to access external directory (jvm property - dataverse.lang.directory)
     * where bundles are kept and return the display name.
     * <p>
     * If it is default bundle or default metadata block #{@link DefaultMetadataBlocks#METADATA_BLOCK_NAMES}
     * method tries to get the name from default bundles otherwise it returns empty string.
     */
    private static String getStringFromPropertyFile(String bundleKey, String bundleName, Locale locale) throws MissingResourceException {
        String displayNameFromExternalBundle = StringUtils.EMPTY;

        if ((!DefaultMetadataBlocks.METADATA_BLOCK_NAMES.contains(bundleName) && !INTERNAL_BUNDLE_NAMES.contains(bundleName))
                && System.getProperty("dataverse.lang.directory") != null) {
            displayNameFromExternalBundle = getStringFromExternalBundle(bundleKey, bundleName, locale);
        }

        return StringUtils.isBlank(displayNameFromExternalBundle)
                ? getStringFromInternalBundle(bundleKey, bundleName, locale)
                : displayNameFromExternalBundle;
    }

    private static String getStringFromInternalBundle(String bundleKey, String bundleName, Locale locale) {
        String key = bundleName + "_" + locale.getLanguage();
        ResourceBundle resourceBundle = bundleCache.get(key);
        if (resourceBundle == null) {
            try {
                resourceBundle = ResourceBundle.getBundle(bundleName, locale);
            } catch (MissingResourceException mre) {
                resourceBundle = EMPTY_BUNDLE;
            }
            bundleCache.putIfAbsent(key, resourceBundle);
        }
        try {
            return !EMPTY_BUNDLE.equals(resourceBundle)
                    ? resourceBundle.getString(bundleKey)
                    : StringUtils.EMPTY;
        } catch (Exception ex) {
            logger.finest("Could not find key \"" + bundleKey + "\" in bundle file: " + bundleName);
            return StringUtils.EMPTY;
        }
    }

    // IMPORTANT: this method is nearly exact copy of getStringFromInternalBundle(…), however
    // any attempt in extracting common code from these two and pass differing parts as lambdas
    // would cause great decrease in performance of WHOLE dataverse app.
    private static String getStringFromExternalBundle(String bundleKey, String bundleName, Locale locale) {
        String key = bundleName + "_" + locale.getLanguage();
        ResourceBundle resourceBundle = bundleCache.get(key);
        if (resourceBundle == null) {
            try {
                URL customBundlesDir = Paths.get(System.getProperty("dataverse.lang.directory")).toUri().toURL();
                URLClassLoader externalBundleDirURL = new URLClassLoader(new URL[]{customBundlesDir});
                resourceBundle = ResourceBundle.getBundle(bundleName, locale, externalBundleDirURL);
            } catch (MalformedURLException | MissingResourceException ex) {
                resourceBundle = EMPTY_BUNDLE;
            }
            bundleCache.putIfAbsent(key, resourceBundle);
        }
        try {
            return !EMPTY_BUNDLE.equals(resourceBundle)
                    ? resourceBundle.getString(bundleKey)
                    : StringUtils.EMPTY;
        } catch (Exception ex) {
            logger.finest("Could not find key \"" + bundleKey + "\" in bundle file: " + bundleName);
            return StringUtils.EMPTY;
        }
    }
}

