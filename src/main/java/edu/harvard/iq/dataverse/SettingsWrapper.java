/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.settings.Setting;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named
public class SettingsWrapper implements java.io.Serializable {

    @EJB
    SettingsServiceBean settingService;

    private Map<String, String> settingsMap;
    
    /**
     * Values that are considered as "true".
     * @see #isTrue(java.lang.String, boolean) 
     */
    private static final Set<String> TRUE_VALUES = Collections.unmodifiableSet(
            new TreeSet<>( Arrays.asList("1","yes", "true","allow")));

    /**
     *
     * @return setting value as string, or null if no setting is specified (JSF test `empty` 
     * works for null values`)
     */
    public String get(String settingKey) {
        if (settingsMap == null) {
            initSettingsMap();
        }
        
        return settingsMap.get(settingKey);
    }
    
    public boolean isTrueForKey(String settingKey, boolean safeDefaultIfKeyNotFound) {
        if (settingsMap == null) {
            initSettingsMap();
        }
        
        String val = settingsMap.get(settingKey);;
        return ( val==null ) ? safeDefaultIfKeyNotFound : TRUE_VALUES.contains(val.trim().toLowerCase() );
    }

    private void initSettingsMap() {
        // initialize settings map
        settingsMap = new HashMap<>();
        for (Setting setting : settingService.listAll()) {
            settingsMap.put(setting.getName(), setting.getContent());
        }
    }

    /**
     * default separator "," for settings list
     */
    public static String defaultListSeparator = ",";

    /**
     * check if a given value is present in a setting list using the default separator.
     * @param settingKey setting list to search
     * @param queryValue value to search for
     * @return true if a given value is present in a setting list, false if the value or key is 
     * absent
     */
    public boolean valueInSettingList(String settingKey, String queryValue )
    {
	    return valueInSettingList( settingKey, queryValue, defaultListSeparator);
    }
   
    /**
     * check if a given value is present in a setting list.
     * @param settingKey setting list to search
     * @param queryValue value to search for
     * @param sep list separator
     * @return true if a given value is present in a setting list, false if the value or key is 
     * absent
     */
    public boolean valueInSettingList(String settingKey, String queryValue, String sep)
    {
	    if ( null == settingsMap )
	    {
		    initSettingsMap();
	    }
	    String xs = settingsMap.get( settingKey );
	    if ( null == xs )
	    {
		    return false;
	    }
	    String[] ys = xs.split( sep );
	    if ( 0 == ys.length )
	    {
		    return false;
	    }
	    for( String y : ys )
	    {
		    if ( queryValue == y )
		    {
			    return true;
		    }
	    }
	    return false;
    }

    /**
     * if this key is present in the downloadMethods or uploadMethods list, allow native
     * (http) uploads / downloads (if either list has been set).
     */
    public static String nativeProtocol = "native/http";
    
    /**
     * default string for uploadMethods; may belong elsewhere.
     */
    private static String uploadMethodsList = "uploadMethods";
    /**
     * default string for downloadMethods; may belong elsewhere.
     */
    private static String downloadMethodsList = "downloadMethods";

    /**
     * wrapper to see if the native file upload options should be shown.
     * @return true if `nativeProtocol` is in the `uploadMethods` list, or if `uploadMethods`
     * has not been set; false otherwise.
     */
    public boolean allowNativeUploads()
    {
	    return nativeTransferCheck( uploadMethodsList );
    }

    /**
     * wrapper to see if the native file download options should be shown.
     * @return true if `nativeProtocol` is in the `downloadMethods` list, or if `downloadMethods`
     * has not been set; false otherwise.
     */
    public boolean allowNativeDownloads()
    {
	    return nativeTransferCheck( downloadMethodsList );
    }

    /**
     * centralize logic for check if setting absent, or value present in setting list
     */
    private boolean nativeTransferCheck( String transferDirection )
    {
	    if ( null == settingsMap )
	    {
		    initSettingsMap();
	    }
	    Set keys = settingsMap.keySet();
	    if ( ! keys.contains( (Object) transferDirection ) )
	    {
		    return true;
	    }
	    return valueInSettingList( transferDirection, nativeProtocol );
    }

    private String guidesBaseUrl = null; 
    
    public String getGuidesBaseUrl() {
        if (guidesBaseUrl == null) {
            String saneDefault = "http://guides.dataverse.org";
        
            guidesBaseUrl = get(":GuidesBaseUrl");
            if (guidesBaseUrl == null) {
                guidesBaseUrl = saneDefault + "/en"; 
            } else {
                guidesBaseUrl = guidesBaseUrl + "/en";
            }
            // TODO: 
            // hard-coded "en"; will need to be configuratble once 
            // we have support for other languages. 
            // TODO: 
            // remove a duplicate of this method from SystemConfig
        }
        return guidesBaseUrl;
    }

}

