package edu.harvard.iq.dataverse.util;

import com.google.common.collect.Sets;
import edu.harvard.iq.dataverse.metadata.DefaultMetadataBlocks;
import org.apache.commons.lang.StringUtils;

import javax.faces.context.FacesContext;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;

public class BundleUtil {

    private static final Logger logger = Logger.getLogger(BundleUtil.class.getCanonicalName());

    private static final String DEFAULT_BUNDLE_FILE = "Bundle";

    private static final Set<String> INTERNAL_BUNDLE_NAMES = Sets.newHashSet(
            DEFAULT_BUNDLE_FILE, "BuiltInRoles", "MimeTypeDisplay", "MimeTypeFacets", "ValidationMessages");

    // -------------------- LOGIC --------------------

    public static String getStringFromBundle(String key) {
        return getStringFromPropertyFile(key, DEFAULT_BUNDLE_FILE, getCurrentLocale());
    }

    public static String getStringFromBundle(String key, Locale locale) {
        return getStringFromPropertyFile(key, DEFAULT_BUNDLE_FILE, locale);
    }

    public static String getStringFromBundle(String key, List<String> arguments) {
        String stringFromPropertyFile = getStringFromPropertyFile(key, DEFAULT_BUNDLE_FILE);

        if (arguments != null) {
            return MessageFormat.format(stringFromPropertyFile, arguments.toArray());
        }

        return stringFromPropertyFile;
    }

    /**
     * Gets display name for specified bundle key. If it is external bundle,
     * method tries to access external directory (jvm property - dataverse.lang.directory)
     * where bundles are kept and return the display name.
     * <p>
     * If it is default bundle or default metadata block #{@link DefaultMetadataBlocks#METADATA_BLOCK_NAMES}
     * method tries to get the name from default bundles otherwise it returns empty string.
     */
    public static String getStringFromPropertyFile(String bundleKey, String bundleName, Locale locale) throws MissingResourceException {
        Optional<String> displayNameFromExternalBundle = Optional.empty();

        if ((!DefaultMetadataBlocks.METADATA_BLOCK_NAMES.contains(bundleName) && !INTERNAL_BUNDLE_NAMES.contains(bundleName))
                && System.getProperty("dataverse.lang.directory") != null) {
            displayNameFromExternalBundle = getStringFromExternalBundle(bundleKey, bundleName, locale);
        }

        return displayNameFromExternalBundle.orElseGet(() -> getStringFromInternalBundle(bundleKey, bundleName, locale));
    }

    public static String getStringFromPropertyFile(String bundleKey, String bundleName) throws MissingResourceException {
        Optional<String> displayNameFromExternalBundle = Optional.empty();

        if ((!DefaultMetadataBlocks.METADATA_BLOCK_NAMES.contains(bundleName) && !INTERNAL_BUNDLE_NAMES.contains(bundleName))
                && System.getProperty("dataverse.lang.directory") != null) {
            displayNameFromExternalBundle = getStringFromExternalBundle(bundleKey, bundleName, getCurrentLocale());
        }

        return displayNameFromExternalBundle.orElseGet(() -> getStringFromInternalBundle(bundleKey, bundleName, getCurrentLocale()));
    }

    public static Locale getCurrentLocale() {
        if (FacesContext.getCurrentInstance() == null) {
            return new Locale("en");
        } else if (FacesContext.getCurrentInstance().getViewRoot() == null) {
            return FacesContext.getCurrentInstance().getExternalContext().getRequestLocale();
        } else if (FacesContext.getCurrentInstance().getViewRoot().getLocale().getLanguage().equals("en_US")) {
            return new Locale("en");
        }

        return FacesContext.getCurrentInstance().getViewRoot().getLocale();

    }

    // -------------------- PRIVATE --------------------

    private static String getStringFromInternalBundle(String bundleKey, String bundleName, Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle(bundleName, locale);
        try {
            return bundle.getString(bundleKey);
        } catch (Exception ex) {
            logger.warning("Could not find key \"" + bundleKey + "\" in bundle file: ");
            return StringUtils.EMPTY;
        }
    }

    private static Optional<String> getStringFromExternalBundle(String bundleKey, String bundleName, Locale locale) {
        try {
            URL customBundlesDir = Paths.get(System.getProperty("dataverse.lang.directory")).toUri().toURL();
            URLClassLoader externalBundleDirURL = new URLClassLoader(new URL[]{customBundlesDir});

            ResourceBundle resourceBundle =
                    ResourceBundle.getBundle(bundleName, locale, externalBundleDirURL);

            return Optional.of(resourceBundle.getString(bundleKey));
        } catch (MalformedURLException ex) {
            logger.warning(ex.getMessage());
            return Optional.empty();
        }
    }
}
