package edu.harvard.iq.dataverse.util;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.HashMap;
import javax.faces.context.FacesContext;

public class BundleUtil {

    private static final Logger logger = Logger.getLogger(BundleUtil.class.getCanonicalName());

    private static final String defaultBundleFile = "Bundle";

    private static final Map<String, ClassLoader> classLoaderCache = new HashMap<String, ClassLoader>();

    public static String getStringFromBundle(String key) {
        return getStringFromBundle(key, (List<String>)null);
    }
    
    public static String getStringFromBundle(String key, Locale locale) {
        return getStringFromBundle(key, null, locale);
    }

    private static String getStringFromBundle(String key, List<String> arguments, Locale locale) {
        ResourceBundle bundle = getResourceBundle(defaultBundleFile, locale);
        if (bundle == null) {
            return null;
        }
        return getStringFromBundle(key, arguments, bundle);
    }

    public static String getStringFromBundle(String key, List<String> arguments) {
        ResourceBundle bundle = getResourceBundle(defaultBundleFile );
        if (bundle == null) {
            return null;
        }
        return getStringFromBundle(key, arguments, bundle);
    }

    public static String getStringFromBundle(String key, List<String> arguments, ResourceBundle bundle) {
        try {
            return getStringFromBundleNoMissingCheck(key, arguments, bundle);
        } catch (MissingResourceException ex) {
            logger.warning("Could not find key \"" + key + "\" in bundle file: ");
            logger.log(Level.CONFIG, ex.getMessage(), ex);
            return null;
        }
    }


    /**
     * This call was added to allow bypassing the exception catch, for filetype indexing needs the exception to bubble up
     * --MAD 4.9.4
     */
    private static String getStringFromBundleNoMissingCheck(String key, List<String> arguments, ResourceBundle bundle) throws MissingResourceException {
        if (key == null || key.isEmpty()) {
            return null;
        }
        String stringFromBundle = null;

        stringFromBundle = bundle.getString(key);
        logger.fine("string found: " + stringFromBundle);

        if (arguments != null) {
            Object[] argArray = new String[arguments.size()];
            argArray = arguments.toArray(argArray);
            return MessageFormat.format(stringFromBundle, argArray);
        } else {
            return stringFromBundle;
        }
    }

    public static String getStringFromPropertyFile(String key, String propertyFileName) throws MissingResourceException {
        return getStringFromPropertyFile(key, propertyFileName, null);
    }
    
    public static String getStringFromPropertyFile(String key, String propertyFileName, Locale locale) throws MissingResourceException {
        ResourceBundle bundle = getResourceBundle(propertyFileName,locale);
        if (bundle == null) {
            return null;
        }
        return getStringFromBundleNoMissingCheck(key, null, bundle);
    }

    public static ResourceBundle getResourceBundle(String propertyFileName ) {
        return getResourceBundle(propertyFileName, null);
    }

    public static ResourceBundle getResourceBundle(String propertyFileName, Locale currentLocale) {
        ResourceBundle bundle;

        String filesRootDirectory = System.getProperty("dataverse.lang.directory");

        if(currentLocale == null) {
            currentLocale = getCurrentLocale();
        }

        if (filesRootDirectory == null || filesRootDirectory.isEmpty()) {
            bundle = ResourceBundle.getBundle("propertyFiles/" + propertyFileName, currentLocale);
        } else {
            try {
                ClassLoader loader = getClassLoader(filesRootDirectory);
                bundle = ResourceBundle.getBundle(propertyFileName, currentLocale, loader);
            } catch (MissingResourceException mre) {
                logger.warning("No property file named " + propertyFileName + "_" + currentLocale.getLanguage()
                        + " found in " + filesRootDirectory + ", using untranslated values");
                bundle = ResourceBundle.getBundle("propertyFiles/" + propertyFileName, currentLocale);
            }
        }

        return bundle ;
    }

    private static ClassLoader getClassLoader(String filesRootDirectory) {
        if (classLoaderCache.containsKey(filesRootDirectory)){
            return classLoaderCache.get(filesRootDirectory);
        }

        File bundleFileDir  = new File(filesRootDirectory);
        URL[] urls = null;
        try {
            urls = new URL[]{bundleFileDir.toURI().toURL()};
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        ClassLoader loader = new URLClassLoader(urls);
        classLoaderCache.put(filesRootDirectory, loader);
        return loader;
    }

    public static Locale getCurrentLocale() {
        if (FacesContext.getCurrentInstance() == null) {
            String localeEnvVar = System.getenv().get("LANG");
            if (localeEnvVar != null) {
                if (localeEnvVar.indexOf('.') > 0) {
                    localeEnvVar = localeEnvVar.substring(0, localeEnvVar.indexOf('.'));
                }
                if (!"en_US".equals(localeEnvVar)) {
                    logger.fine("BundleUtil: LOCALE code from the environmental variable is "+localeEnvVar);
                    return new Locale(localeEnvVar);
                }
            }

            return new Locale("en");
        } else if (FacesContext.getCurrentInstance().getViewRoot() == null) {
            return FacesContext.getCurrentInstance().getExternalContext().getRequestLocale();
        } else if (FacesContext.getCurrentInstance().getViewRoot().getLocale().getLanguage().equals("en_US")) {
            return new Locale("en");
        }

        return FacesContext.getCurrentInstance().getViewRoot().getLocale();

    }


    public static String getStringFromDefaultBundle(String key) {
        try {
            return getStringFromBundleNoMissingCheck(key, null, getResourceBundle(defaultBundleFile , getDefaultLocale() ));
        } catch (MissingResourceException ex) {
            logger.warning("Could not find key \"" + key + "\" in bundle file: ");
            logger.log(Level.CONFIG, ex.getMessage(), ex);
            return null;
        }
    }

    public static String getStringFromDefaultPropertyFile(String key, String propertyFileName  ) throws MissingResourceException {
        ResourceBundle bundle = getResourceBundle(propertyFileName, getDefaultLocale());
        if (bundle == null) {
            return null;
        }
        return getStringFromBundleNoMissingCheck(key, null, bundle);
    }
    
    /**
     * Return JVM default locale.
     *
     * For now, this simply forwards default system behaviour.
     * That means on JDK8 the system property user.language will be set on startup
     * from environment variables like LANG or via Maven arguments (which is important for testing).
     * (See also pom.xml for an example how we pinpoint this for reproducible tests!)
     * (You should also be aware that good IDEs are honoring settings from pom.xml.)
     *
     * Nonetheless, someday we might want to have more influence on how this is determined, thus this wrapper.
     * @return Dataverse default locale
     */
    public static Locale getDefaultLocale() {
        return Locale.getDefault();
    }

}
