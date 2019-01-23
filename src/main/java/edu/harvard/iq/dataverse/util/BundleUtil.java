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
import javax.faces.context.FacesContext;

public class BundleUtil {

    private static final Logger logger = Logger.getLogger(BundleUtil.class.getCanonicalName());

    private static final String defaultBundleFile = "Bundle";
    //private static Locale bundle_locale; // static ??

    public static String getStringFromBundle(String key) {
        return getStringFromBundle(key, null);
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

    public static String getStringFromPropertyFile(String key, String propertyFileName  ) throws MissingResourceException {
        ResourceBundle bundle = getResourceBundle(propertyFileName);
        if (bundle == null) {
            return null; 
        }
        return getStringFromBundleNoMissingCheck(key, null, bundle);
    }

    public static ResourceBundle getResourceBundle(String propertyFileName) {
        ResourceBundle bundle;

        String filesRootDirectory = System.getProperty("dataverse.lang.directory");

        Locale currentLocale = getCurrentLocale();
        
        if (filesRootDirectory == null || filesRootDirectory.isEmpty()) {
            bundle = ResourceBundle.getBundle(propertyFileName, currentLocale);
        } else {
            File bundleFileDir  = new File(filesRootDirectory);
            URL[] urls = null;
            try {
                urls = new URL[]{bundleFileDir.toURI().toURL()};
            } catch (Exception e) {
                e.printStackTrace();
                return null; 
            }

            ClassLoader loader = new URLClassLoader(urls);
            bundle = ResourceBundle.getBundle(propertyFileName, currentLocale, loader);
        }

        return bundle ;
    }
    
    public static Locale getCurrentLocale() {
        if (FacesContext.getCurrentInstance() == null) {
            logger.info("BundleUtil: no FacesContext, defaulting to locale en");
            return new Locale("en");
        } else if (FacesContext.getCurrentInstance().getViewRoot() == null) {
            return FacesContext.getCurrentInstance().getExternalContext().getRequestLocale();
        } else if (FacesContext.getCurrentInstance().getViewRoot().getLocale().getLanguage().equals("en_US")) {
            return new Locale("en");
        }

        return FacesContext.getCurrentInstance().getViewRoot().getLocale();

    }
}
