package edu.harvard.iq.dataverse.util;
//import edu.harvard.iq.dataverse.DataverseLocaleBean;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.PropertyResourceBundle;

import javax.faces.context.FacesContext;

public class LocalBundle extends ResourceBundle {

    private static final String defaultBundleFile = "Bundle";
    //private static ResourceBundle bundle;
    //private static Locale bundle_locale;



    public LocalBundle(){
        //DataverseLocaleBean d = new DataverseLocaleBean();
        //bundle_locale = new Locale(d.getLocaleCode());
        Locale currentLocale = BundleUtil.getCurrentLocale();
        ResourceBundle localBundle = BundleUtil.getResourceBundle(defaultBundleFile);

        if (localBundle != null) {
            setParent(localBundle);
        }
    }

    @Override
    public Object handleGetObject(String key) {
        return parent.getObject(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        return parent.getKeys();
    }

}