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
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.MailUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;

import java.time.LocalDate;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.mail.internet.InternetAddress;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named
public class SettingsWrapper implements java.io.Serializable {

    static final Logger logger = Logger.getLogger(SettingsWrapper.class.getCanonicalName());
    
    @EJB
    SettingsServiceBean settingsService;

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
        for (Setting setting : settingsService.listAll()) {
            settingsMap.put(setting.getName(), setting.getContent());
        }
    }

    
    public String getGuidesBaseUrl() {
        if (true)

            if (guidesBaseUrl == null) {
            String saneDefault = "https://guides.dataverse.org";
        
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

    public String getGuidesVersion() {
        return systemConfig.getGuidesVersion();
    }
    
    public Long getZipDownloadLimit(){
        return systemConfig.getZipDownloadLimit();
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
    
    public boolean isRsyncOnly() {
        return systemConfig.isRsyncOnly();
    }
    
    public boolean isHTTPUpload(){
        return systemConfig.isHTTPUpload();
    }
    
    public boolean isDataFilePIDSequentialDependent(){
        return systemConfig.isDataFilePIDSequentialDependent();
    }
    
    public String getSupportTeamName() {
        String systemEmail = getValueForKey(SettingsServiceBean.Key.SystemEmail);
        InternetAddress systemAddress = MailUtil.parseSystemAddress(systemEmail);
        return BrandingUtil.getSupportTeamName(systemAddress);
    }
    
    public String getSupportTeamEmail() {
        String systemEmail = getValueForKey(SettingsServiceBean.Key.SystemEmail);
        InternetAddress systemAddress = MailUtil.parseSystemAddress(systemEmail);        
        return BrandingUtil.getSupportTeamEmailAddress(systemAddress) != null ? BrandingUtil.getSupportTeamEmailAddress(systemAddress) : BrandingUtil.getSupportTeamName(systemAddress);
    }
    
    public Integer getUploadMethodsCount() {
        return systemConfig.getUploadMethodCount();
    }

    public boolean isRootDataverseThemeDisabled() {
        return isTrueForKey(Key.DisableRootDataverseTheme, false);
    }
    
    public String getDropBoxKey() {

        String configuredDropBoxKey = System.getProperty("dataverse.dropbox.key");
        if (configuredDropBoxKey != null) {
            return configuredDropBoxKey;
        }
        return "";
    }
    
    public Boolean isHasDropBoxKey() {

        return !getDropBoxKey().isEmpty();
    }
    
    // Language Locales Configuration: 
    
    // Map from locale to display name eg     en -> English
    private Map<String, String> configuredLocales;
    
    public boolean isLocalesConfigured() {
        if (configuredLocales == null) {
            initLocaleSettings();
        }
        return configuredLocales.size() > 1;
    }

    public Map<String, String> getConfiguredLocales() {
        if (configuredLocales == null) {
            initLocaleSettings(); 
        }
        return configuredLocales;
    }
    
    private void initLocaleSettings() {
        
        configuredLocales = new LinkedHashMap<>();
        
        try {
            JSONArray entries = new JSONArray(getValueForKey(SettingsServiceBean.Key.Languages, "[]"));
            for (Object obj : entries) {
                JSONObject entry = (JSONObject) obj;
                String locale = entry.getString("locale");
                String title = entry.getString("title");

                configuredLocales.put(locale, title);
            }
        } catch (JSONException e) {
            //e.printStackTrace();
            // do we want to know? - probably not
        }
    }

    public boolean isDoiInstallation() {
        String protocol = getValueForKey(SettingsServiceBean.Key.Protocol);
        if ("doi".equals(protocol)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isDataCiteInstallation() {
        String protocol = getValueForKey(SettingsServiceBean.Key.DoiProvider);
        if ("DataCite".equals(protocol)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isMakeDataCountDisplayEnabled() {
        boolean safeDefaultIfKeyNotFound = (getValueForKey(SettingsServiceBean.Key.MDCLogPath)!=null); //Backward compatible
        return isTrueForKey(SettingsServiceBean.Key.DisplayMDCMetrics, safeDefaultIfKeyNotFound);
    
    }
    
    public boolean displayChronologicalDateFacets() {
        //Defaults to true
        return isTrueForKey(SettingsServiceBean.Key.ChronologicalDateFacets, true);
    
    }
    
    List<String> anonymizedFieldTypes = null;

    public boolean shouldBeAnonymized(DatasetField df) {
        // Set up once per view
        if (anonymizedFieldTypes == null) {
            anonymizedFieldTypes = new ArrayList<String>();
            String names = get(SettingsServiceBean.Key.AnonymizedFieldTypeNames.toString(), "");
            anonymizedFieldTypes.addAll(Arrays.asList(names.split(",\\s")));
        }
        return anonymizedFieldTypes.contains(df.getDatasetFieldType().getName());
    }
    public LocalDate getMaxEmbargoDate() {
        String months = getValueForKey(SettingsServiceBean.Key.MaxEmbargoDurationInMonths);
        Long maxMonths = null;
        if (months != null) {
            try {
                maxMonths = Long.parseLong(months);
            } catch (NumberFormatException nfe) {
                logger.warning("Cant interpret :MaxEmbargoDurationInMonths as a long");
            }
        }
    
        if (maxMonths != null) {
            if (maxMonths == -1) {
                maxMonths = 12000l; //Arbitrary cutoff at 1000 years - needs to keep maxDate < year 999999999 and somehwere 1K> x >10K years the datepicker widget stops showing a popup calendar
            }
            return LocalDate.now().plusMonths(maxMonths);
        }
        return null;
    }
    
    public LocalDate getMinEmbargoDate() {
            return LocalDate.now().plusDays(1);
    }
    
    
    public boolean isValidEmbargoDate(Embargo e) {
        
        if (e.getDateAvailable()==null || e.getDateAvailable().isAfter(LocalDate.now())
                && e.getDateAvailable().isBefore(getMaxEmbargoDate().plusDays(1))) {
            return true;
        }
        
        return false;
    }
    
    public boolean isEmbargoAllowed() {
        //Need a valid :MaxEmbargoDurationInMonths setting to allow embargoes
        return getMaxEmbargoDate()!=null;
    }

    Map<String,String> languageMap = null;
    
    Map<String, String> getBaseMetadataLanguageMap(boolean refresh) {
        if (languageMap == null || refresh) {
            languageMap = new HashMap<String, String>();

            /* If MetadataLanaguages is set, use it.
             * If not, we can't assume anything and should avoid assuming a metadata language
             */
            String mlString = getValueForKey(SettingsServiceBean.Key.MetadataLanguages,"");
            
            if(mlString.isEmpty()) {
                mlString="[]";
            }
            JsonReader jsonReader = Json.createReader(new StringReader(mlString));
            JsonArray languages = jsonReader.readArray();
            for(JsonValue jv: languages) {
                JsonObject lang = (JsonObject) jv;
                languageMap.put(lang.getString("locale"), lang.getString("title"));
            }
        }
        return languageMap;
    }
    
    public Map<String, String> getMetadataLanguages(DvObjectContainer target) {
        Map<String,String> currentMap = new HashMap<String,String>();
        currentMap.putAll(getBaseMetadataLanguageMap(true));
        languageMap.put(DvObjectContainer.UNDEFINED_METADATA_LANGUAGE_CODE, getDefaultMetadataLanguageLabel(target));
        return languageMap;
    }
    
    private String getDefaultMetadataLanguageLabel(DvObjectContainer target) {
        String mlLabel = Locale.getDefault().getDisplayLanguage();
        Dataverse parent = target.getOwner();
        boolean fromAncestor=false;
        if(parent != null) {
            mlLabel = parent.getEffectiveMetadataLanguage();
            //recurse dataverse chain to root and if any have a metadata language set, fromAncestor is true
            while(parent!=null) {
                if(!parent.getMetadataLanguage().equals(DvObjectContainer.UNDEFINED_METADATA_LANGUAGE_CODE)) {
                    fromAncestor=true;
                    break;
                }
                parent=parent.getOwner();
            }
        }
        if(mlLabel.equals(DvObjectContainer.UNDEFINED_METADATA_LANGUAGE_CODE)) {
            mlLabel = getBaseMetadataLanguageMap(false).get(getDefaultMetadataLanguage());
        }
        if(fromAncestor) {
            mlLabel = mlLabel + " " + BundleUtil.getStringFromBundle("dataverse.inherited");
        } else {
            mlLabel = mlLabel + " " + BundleUtil.getStringFromBundle("dataverse.default");
        }
        return mlLabel;
    }
    
    public String getDefaultMetadataLanguage() {
        Map<String, String> mdMap = getBaseMetadataLanguageMap(false);
        if(mdMap.size()>=1) {
            if(mdMap.size()==1) {
                //One entry - it's the default
            return (String) mdMap.keySet().toArray()[0];
            } else {
                //More than one - :MetadataLanguages is set so we use the default
                return DvObjectContainer.DEFAULT_METADATA_LANGUAGE_CODE;
            }
        } else {
            // None - :MetadataLanguages is not set so return null to turn off the display (backward compatibility)
            return null;
        }
    }

}

