/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.settings.Setting;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import edu.harvard.iq.dataverse.util.MailUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.mail.internet.InternetAddress;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named
public class SettingsWrapper implements java.io.Serializable {

    @EJB
    SettingsServiceBean settingService;

    @EJB
    DataverseServiceBean dataverseService;

    @EJB
    SystemConfig systemConfig;

    private Map<String, String> settingsMap;
    
    // Related to a specific setting for guide urls
    private String guidesBaseUrl = null; 

 
    public String get(String settingKey) {
        if (settingsMap == null) {
            initSettingsMap();
        }
        
        return settingsMap.get(settingKey);
    }
    /**
     * Return value from map, initiating settings map if needed
     * @param settingKey
     * @param defaultValue
     * @return 
     */
    public String get(String settingKey, String defaultValue) {
        if (settingsMap == null) {
            initSettingsMap();
        }
        
        if (!settingsMap.containsKey(settingKey)){
            return defaultValue;
        }
        return settingsMap.get(settingKey);
    }
    
    /**
     * Pass the map key as a "Key" object instead of a string
     * 
     * @param key
     * @return 
     */
    public String getValueForKey(Key key){
        if (key == null){
            return null;
        }
        return get(key.toString());
    }

    /**
     * Pass the map key as a "Key" object instead of a string
     * Allow a default value if null is encountered
     * 
     * @param key
     * @param defaultValue
     * @return 
     */
    public String getValueForKey(Key key, String defaultValue){
        if (key == null){
            return null;
        }
        return get(key.toString(), defaultValue);
    }

    public boolean isTrueForKey(Key key, boolean safeDefaultIfKeyNotFound) {
        
        return isTrueForKey(key.toString(), safeDefaultIfKeyNotFound);
    }

    public boolean isTrueForKey(String settingKey, boolean safeDefaultIfKeyNotFound) {
        if (settingsMap == null) {
            initSettingsMap();
        }
        
        String val = get(settingKey);;
        return ( val==null ) ? safeDefaultIfKeyNotFound : StringUtil.isTrue(val);
    }

    private void initSettingsMap() {
        // initialize settings map
        settingsMap = new HashMap<>();
        for (Setting setting : settingService.listAll()) {
            settingsMap.put(setting.getName(), setting.getContent());
        }
    }

    
    public String getGuidesBaseUrl() {
        if (true)

            if (guidesBaseUrl == null) {
            String saneDefault = "http://guides.dataverse.org";
        
            guidesBaseUrl = getValueForKey(SettingsServiceBean.Key.GuidesBaseUrl);
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

    public boolean isPublicInstall(){
        return systemConfig.isPublicInstall();
    }
    
    public boolean isRsyncUpload() {
        return systemConfig.isRsyncUpload();
    }
    
    public boolean isRsyncDownload() {
        return systemConfig.isRsyncDownload();
    }
    
    public boolean isDataFilePIDSequentialDependent(){
        return systemConfig.isDataFilePIDSequentialDependent();
    }
    
    public String getSupportTeamName() {
        String systemEmail = getValueForKey(SettingsServiceBean.Key.SystemEmail);
        InternetAddress systemAddress = MailUtil.parseSystemAddress(systemEmail);
        return BrandingUtil.getSupportTeamName(systemAddress, dataverseService.findRootDataverse().getName());
    }

    public boolean isRootDataverseThemeDisabled() {
        return isTrueForKey(Key.DisableRootDataverseTheme, false);
    }
}

