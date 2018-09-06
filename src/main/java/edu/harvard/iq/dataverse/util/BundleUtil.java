package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.DataverseLocaleBean;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class BundleUtil {

    private static final Logger logger = Logger.getLogger(BundleUtil.class.getCanonicalName());

    private static final String defaultBundleFile = "Bundle";
    private static Locale bundle_locale;

    public static String getStringFromBundle(String key) {
        return getStringFromBundle(key, null);
    }

    public static String getStringFromBundle(String key, List<String> arguments) {

        DataverseLocaleBean d = new DataverseLocaleBean();
        bundle_locale= new Locale(d.getLocaleCode());

        logger.info("dataverselocalebean   : **** : " + d.getLocaleCode() );
        String filesRootDirectory = System.getProperty("dataverse.lang.directory");
        if (filesRootDirectory == null || filesRootDirectory.isEmpty()) {
            filesRootDirectory = "/tmp/lang";
        }

        File file = new File(filesRootDirectory);
        URL[] urls = null;
        try {
            urls = new URL[]{file.toURI().toURL()};
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        ClassLoader loader = new URLClassLoader(urls);
        ResourceBundle bundle = ResourceBundle.getBundle(defaultBundleFile, bundle_locale, loader);
        return getStringFromBundle(key, arguments, bundle);
    }

    public static String getStringFromBundle(String key, List<String> arguments, ResourceBundle bundle) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        String stringFromBundle = null;
        try {
            stringFromBundle = bundle.getString(key);
            logger.fine("string found: " + stringFromBundle);
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
