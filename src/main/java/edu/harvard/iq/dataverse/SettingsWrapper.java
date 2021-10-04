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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    @EJB
    SettingsServiceBean settingsService;

    @EJB
    DataverseServiceBean dataverseService;

    @EJB
    SystemConfig systemConfig;
    
    @EJB
    DatasetFieldServiceBean fieldService;

    private Map<String, String> settingsMap;
    
    // Related to a specific setting for guide urls
    private String guidesBaseUrl = null; 

    private String siteUrl = null; 
    
    private Dataverse rootDataverse = null; 
    
    private String guidesVersion = null;
    
    private String appVersion = null; 
    
    private String appVersionWithBuildNumber = null; 
    
    private Boolean shibPassiveLoginEnabled = null; 
    
    private String footerCopyrightAndYear = null; 
    
    //External Vocabulary support
    private Map<Long, JsonObject> cachedCvocMap = null;
    
    private Long zipDownloadLimit = null; 
    
    private Boolean publicInstall = null; 
    
    private Integer uploadMethodsCount;
    
    private Boolean rsyncUpload = null; 
    
    private Boolean rsyncDownload = null; 
    
    private Boolean httpUpload = null; 
    
    private Boolean rsyncOnly = null;
    
    private String metricsUrl = null; 
    
    private Boolean dataFilePIDSequentialDependent = null;
    
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
        if (guidesVersion == null) {
            String saneDefault = getAppVersion();
            guidesVersion = getValueForKey(SettingsServiceBean.Key.GuidesVersion, saneDefault);
        }
        return guidesVersion;
    }
    
    // OK to call SystemConfig - the method does not rely on database 
    // settings.
    public String getDataverseSiteUrl() {
        if (siteUrl == null) {
            siteUrl = systemConfig.getDataverseSiteUrl();
        }
        return siteUrl;
    }
    
    // OK to call SystemConfig - the value is based on a JVM option; no 
    // extra database lookups. 
    public Long getZipDownloadLimit(){
        if (zipDownloadLimit == null) {
            String zipLimitOption = getValueForKey(SettingsServiceBean.Key.ZipDownloadLimit);
            zipDownloadLimit = SystemConfig.getLongLimitFromStringOrDefault(zipLimitOption, SystemConfig.defaultZipDownloadLimit);
        }
        return zipDownloadLimit;
    }

    public boolean isPublicInstall(){
        if (publicInstall == null) {
            boolean saneDefault = false;
            publicInstall = isTrueForKey(SettingsServiceBean.Key.PublicInstall, saneDefault);
        }
        return publicInstall; 
    }
    
    public boolean isRsyncUpload() {
        if (rsyncUpload == null) {
            rsyncUpload = getUploadMethodAvailable(SystemConfig.FileUploadMethods.RSYNC.toString());
        }
        return rsyncUpload; 
    }
    
    public boolean isRsyncDownload() {
        if (rsyncDownload == null) {
            String downloadMethods = getValueForKey(SettingsServiceBean.Key.DownloadMethods);
            rsyncDownload = downloadMethods != null && downloadMethods.toLowerCase().contains(SystemConfig.FileDownloadMethods.RSYNC.toString());
        }
        return rsyncDownload;
    }
    
    public boolean isRsyncOnly() {
        if (rsyncOnly == null) {
            String downloadMethods = getValueForKey(SettingsServiceBean.Key.DownloadMethods);
            if(downloadMethods == null){
                rsyncOnly = false;
            } else if (!downloadMethods.toLowerCase().equals(SystemConfig.FileDownloadMethods.RSYNC.toString())){
                rsyncOnly = false;
            } else {
                String uploadMethods = getValueForKey(SettingsServiceBean.Key.UploadMethods);
                if (uploadMethods==null){
                    rsyncOnly = false;
                } else {
                    rsyncOnly = Arrays.asList(uploadMethods.toLowerCase().split("\\s*,\\s*")).size() == 1 && uploadMethods.toLowerCase().equals(SystemConfig.FileUploadMethods.RSYNC.toString());
                }
            }
        }
        return rsyncOnly;
    }
    
    public boolean isHTTPUpload(){
        if (httpUpload == null) {
            httpUpload = getUploadMethodAvailable(SystemConfig.FileUploadMethods.NATIVE.toString());
        }
        return httpUpload;      
    }
    
    public boolean isDataFilePIDSequentialDependent(){
        if (dataFilePIDSequentialDependent == null) {
            dataFilePIDSequentialDependent = false;
            String doiIdentifierType = getValueForKey(SettingsServiceBean.Key.IdentifierGenerationStyle, "randomString");
            String doiDataFileFormat = getValueForKey(SettingsServiceBean.Key.DataFilePIDFormat, "DEPENDENT");
            if (doiIdentifierType.equals("storedProcGenerated") && doiDataFileFormat.equals("DEPENDENT")){
                dataFilePIDSequentialDependent = true;
            }
        }
        return dataFilePIDSequentialDependent;
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
        if (uploadMethodsCount == null) {
            String uploadMethods = getValueForKey(SettingsServiceBean.Key.UploadMethods); 
            if (uploadMethods==null){
                uploadMethodsCount = 0;
            } else {
                uploadMethodsCount = Arrays.asList(uploadMethods.toLowerCase().split("\\s*,\\s*")).size();
            } 
        }
        return uploadMethodsCount;
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
    
    public Dataverse getRootDataverse() {
        if (rootDataverse == null) {
            rootDataverse = dataverseService.findRootDataverse();
        }
        
        return rootDataverse;
    }
    
    // The following 2 methods may be unnecessary *with the current implementation* of 
    // how the application version is retrieved (the values are initialized and caached inside 
    // SystemConfig once per thread - see the code there); 
    // But in case we switch to some other method, that requires a database 
    // lookup like other settings, it should be here. 
    // This would be a prime candidate for moving into some kind of an 
    // APPLICATION-scope caching singleton. -- L.A. 5.7
    public String getAppVersion() {
        if (appVersion == null) {
            appVersion = systemConfig.getVersion();
        }
        return appVersion;
    }
    
    public String getAppVersionWithBuildNumber() {
        if (appVersionWithBuildNumber == null) {
            appVersionWithBuildNumber = systemConfig.getVersion(true);
        }
        return appVersionWithBuildNumber;
    }
    
    public boolean isShibPassiveLoginEnabled() {
        if (shibPassiveLoginEnabled == null) {
            boolean defaultResponse = false;
            shibPassiveLoginEnabled = isTrueForKey(SettingsServiceBean.Key.ShibPassiveLoginEnabled, defaultResponse);
        }
        return shibPassiveLoginEnabled;
    }
    
    // This method may not be necessary *currently* either (the value is 
    // stored in the bundle). -- L.A. 5.7
    public String getFooterCopyrightAndYear() {
        if (footerCopyrightAndYear == null) {
            footerCopyrightAndYear = systemConfig.getFooterCopyrightAndYear();
        }
        return footerCopyrightAndYear; 
    }
    
    public Map<Long, JsonObject> getCVocConf() {
        //Cache this in the view
        if(cachedCvocMap==null) {
        cachedCvocMap = fieldService.getCVocConf(false);
        }
        return cachedCvocMap;
    }
    
    public String getMetricsUrl() {
        if (metricsUrl == null) {
            metricsUrl = getValueForKey(SettingsServiceBean.Key.MetricsUrl);
        }
        return metricsUrl;
    }
    
    private Boolean getUploadMethodAvailable(String method){
        String uploadMethods = getValueForKey(SettingsServiceBean.Key.UploadMethods); 
        if (uploadMethods==null){
            return false;
        } else {
           return  Arrays.asList(uploadMethods.toLowerCase().split("\\s*,\\s*")).contains(method);
        }
    }

}

