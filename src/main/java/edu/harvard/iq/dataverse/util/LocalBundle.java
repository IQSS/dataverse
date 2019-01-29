package edu.harvard.iq.dataverse.util;

import java.util.Enumeration;
import java.util.ResourceBundle;

public class LocalBundle extends ResourceBundle {

    private static final String defaultBundleFile = "Bundle";



    public LocalBundle(){
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