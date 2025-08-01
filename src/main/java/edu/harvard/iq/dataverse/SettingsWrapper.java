/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.GlobusAccessibleStore;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.Setting;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.MailUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.UserNotification.Type;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.Set;

import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIInput;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.ValidatorException;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.json.JsonObject;
import jakarta.mail.internet.InternetAddress;

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
    
    @EJB
    DatasetFieldServiceBean fieldService;
    
    @EJB
    MetadataBlockServiceBean mdbService;
    
    @EJB
    MailServiceBean mailServiceBean;

    private Map<String, String> settingsMap;
    
    // Related to a specific setting for guide urls
    private String guidesBaseUrl = null;
    
    private boolean embargoDateChecked = false;
    private LocalDate maxEmbargoDate = null;

    private boolean retentionDateChecked = false;
    private LocalDate minRetentionDate = null;

    private String siteUrl = null; 
    
    private Dataverse rootDataverse = null; 
    
    private String guidesVersion = null;
    
    private String appVersion = null; 
    
    private String appVersionWithBuildNumber = null; 
    
    private Boolean shibPassiveLoginEnabled = null; 
    
    private String footerCopyrightAndYear = null; 
    
    //External Vocabulary support
    private Map<Long, JsonObject> cachedCvocMap = null;
    private Map<Long, JsonObject> cachedCvocByTermFieldMap = null;
    private Set<Long> cvocFieldSet;
    
    private Long zipDownloadLimit = null; 
    
    private Boolean publicInstall = null; 
    
    private Integer uploadMethodsCount;
    
    private Boolean rsyncUpload = null; 
    
    private Boolean rsyncDownload = null;
    
    private Boolean globusUpload = null;
    private Boolean globusDownload = null;
    private Boolean globusFileDownload = null;
    
    private String globusAppUrl = null;
    
    private List<String> globusStoreList = null;
    
    private Boolean httpUpload = null; 
    
    private Boolean rsyncOnly = null;
    
    private Boolean webloaderUpload = null;
    
    private String metricsUrl = null; 
    
    private Boolean dataFilePIDSequentialDependent = null;
    
    private Boolean customLicenseAllowed = null;
    
    private List<MetadataBlock> systemMetadataBlocks;
    
    private Set<Type> alwaysMuted = null;

    private Set<Type> neverMuted = null;
    
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

    public Integer getInteger(String settingKey, Integer defaultValue) {
        String settingValue = get(settingKey);
        if(settingValue != null) {
            try {
                return Integer.valueOf(settingValue);
            } catch (Exception e) {
                logger.warning(String.format("action=getInteger result=invalid-integer settingKey=%s settingValue=%s", settingKey, settingValue));
            }
        }

        return defaultValue;
    }

    private void initSettingsMap() {
        // initialize settings map
        settingsMap = new HashMap<>();
        for (Setting setting : settingsService.listAll()) {
            settingsMap.put(setting.getName(), setting.getContent());
        }
    }

    private void initAlwaysMuted() {
        alwaysMuted = UserNotification.Type.tokenizeToSet(getValueForKey(Key.AlwaysMuted));
    }

    private void initNeverMuted() {
        neverMuted = UserNotification.Type.tokenizeToSet(getValueForKey(Key.NeverMuted));
    }

    public Set<Type> getAlwaysMutedSet() {
        if (alwaysMuted == null) {
            initAlwaysMuted();
        }
        return alwaysMuted;
    }

    public Set<Type> getNeverMutedSet() {
        if (neverMuted == null) {
            initNeverMuted();
        }
        return neverMuted;
    }

    public boolean isAlwaysMuted(Type type) {
        return getAlwaysMutedSet().contains(type);
    }

    public boolean isNeverMuted(Type type) {
        return getNeverMutedSet().contains(type);
    }

    public boolean isShowMuteOptions() {
        return isTrueForKey(Key.ShowMuteOptions, false);
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
    
    public String getDataverseSiteUrl() {
        if (siteUrl == null) {
            siteUrl = systemConfig.getDataverseSiteUrl();
        }
        return siteUrl;
    }
    
    public Long getZipDownloadLimit(){
        if (zipDownloadLimit == null) {
            String zipLimitOption = getValueForKey(SettingsServiceBean.Key.ZipDownloadLimit);
            zipDownloadLimit = SystemConfig.getLongLimitFromStringOrDefault(zipLimitOption, SystemConfig.defaultZipDownloadLimit);
        }
        return zipDownloadLimit;
    }

    public boolean isPublicInstall(){
        if (publicInstall == null) {
            publicInstall = systemConfig.isPublicInstall();
        }
        return publicInstall; 
    }

    @Deprecated(forRemoval = true, since = "2024-07-07")
    public boolean isRsyncUpload() {
        if (rsyncUpload == null) {
            rsyncUpload = getUploadMethodAvailable(SystemConfig.FileUploadMethods.RSYNC.toString());
        }
        return rsyncUpload; 
    }

    @Deprecated(forRemoval = true, since = "2024-07-07")
    public boolean isRsyncDownload() {
        if (rsyncDownload == null) {
            rsyncDownload = systemConfig.isRsyncDownload();
        }
        return rsyncDownload;
    }

    public boolean isGlobusUpload() {
        if (globusUpload == null) {
            globusUpload = systemConfig.isGlobusUpload();
        }
        return globusUpload;
    }
    
    public boolean isGlobusDownload() {
        if (globusDownload == null) {
            globusDownload = systemConfig.isGlobusDownload();
        }
        return globusDownload;
    }
    
    public boolean isGlobusFileDownload() {
        if (globusFileDownload == null) {
            globusFileDownload = systemConfig.isGlobusFileDownload();
        }
        return globusFileDownload;
    }
    
    public boolean isGlobusEnabledStorageDriver(String driverId) {
        return (GlobusAccessibleStore.acceptsGlobusTransfers(driverId) || GlobusAccessibleStore.allowsGlobusReferences(driverId));
    }
    
    public boolean isDownloadable(FileMetadata fmd) {
        boolean downloadable=true;
        if(isGlobusFileDownload()) {
            String driverId = DataAccess.getStorageDriverFromIdentifier(fmd.getDataFile().getStorageIdentifier());
            
            downloadable = downloadable && StorageIO.isDataverseAccessible(driverId); 
        }
        return downloadable;
    }
    
    public boolean isGlobusTransferable(FileMetadata fmd) {
        boolean globusTransferable=true;
        if(isGlobusFileDownload()) {
            String driverId = DataAccess.getStorageDriverFromIdentifier(fmd.getDataFile().getStorageIdentifier());
            globusTransferable = GlobusAccessibleStore.isGlobusAccessible(driverId);
        }
        return globusTransferable;
    }
    
    
    public String getGlobusAppUrl() {
        if (globusAppUrl == null) {
            globusAppUrl = settingsService.getValueForKey(SettingsServiceBean.Key.GlobusAppUrl, "http://localhost");
        }
        return globusAppUrl;
        
    }
    
    public boolean isWebloaderUpload() {
        if (webloaderUpload == null) {
            webloaderUpload = systemConfig.isWebloaderUpload();
        }
        return webloaderUpload;
    }

    @Deprecated(forRemoval = true, since = "2024-07-07")
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
    
    public String getSupportTeamName() {
        // TODO: should this be replaced with mailServiceBean.getSupportAddress() to expose a configured support team?
        InternetAddress systemAddress = mailServiceBean.getSystemAddress().orElse(null);
        return BrandingUtil.getSupportTeamName(systemAddress);
    }
    
    public String getSupportTeamEmail() {
        // TODO: should this be replaced with mailServiceBean.getSupportAddress() to expose a configured support team?
        InternetAddress systemAddress = mailServiceBean.getSystemAddress().orElse(null);
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
            configuredLocales = new LinkedHashMap<>();
            settingsService.initLocaleSettings(configuredLocales);
        }
        return configuredLocales.size() > 1;
    }

    public Map<String, String> getConfiguredLocales() {
        if (configuredLocales == null) {
            configuredLocales = new LinkedHashMap<>();
            settingsService.initLocaleSettings(configuredLocales); 
        }
        return configuredLocales;
    }
    

    public boolean isMakeDataCountDisplayEnabled() {
        boolean safeDefaultIfKeyNotFound = (getValueForKey(SettingsServiceBean.Key.MDCLogPath)!=null); //Backward compatible
        return isTrueForKey(SettingsServiceBean.Key.DisplayMDCMetrics, safeDefaultIfKeyNotFound);
    }
    
    public LocalDate getMDCStartDate() {
        String date = getValueForKey(SettingsServiceBean.Key.MDCStartDate);
        LocalDate ld=null;
        if(date!=null) {
          ld = LocalDate.parse(date);
        }
        return ld;
        
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
        if (!embargoDateChecked) {
            String months = getValueForKey(SettingsServiceBean.Key.MaxEmbargoDurationInMonths);
            Long maxMonths = null;
            if (months != null) {
                try {
                    maxMonths = Long.parseLong(months);
                } catch (NumberFormatException nfe) {
                    logger.warning("Cant interpret :MaxEmbargoDurationInMonths as a long");
                }
            }

            if (maxMonths != null && maxMonths != 0) {
                if (maxMonths == -1) {
                    maxMonths = 12000l; // Arbitrary cutoff at 1000 years - needs to keep maxDate < year 999999999 and
                                        // somehwere 1K> x >10K years the datepicker widget stops showing a popup
                                        // calendar
                }
                maxEmbargoDate = LocalDate.now().plusMonths(maxMonths);
            }
            embargoDateChecked = true;
        }
        return maxEmbargoDate;
    }
    
    public LocalDate getMinEmbargoDate() {
            return LocalDate.now().plusDays(1);
    }
    
    public boolean isValidEmbargoDate(Embargo e) {
        
        if (e.getDateAvailable()==null || (isEmbargoAllowed() && e.getDateAvailable().isAfter(LocalDate.now())
                && e.getDateAvailable().isBefore(getMaxEmbargoDate().plusDays(1)))) {
            return true;
        }
        
        return false;
    }
    
    public boolean isEmbargoAllowed() {
        //Need a valid :MaxEmbargoDurationInMonths setting to allow embargoes
        return getMaxEmbargoDate()!=null;
    }
    
    public void validateEmbargoDate(FacesContext context, UIComponent component, Object value)
            throws ValidatorException {
        if (isEmbargoAllowed()) {
            UIComponent cb = component.findComponent("embargoCheckbox");
            UIInput endComponent = (UIInput) cb;
            boolean removedState = false;
            if (endComponent != null) {
                try {
                    removedState = (Boolean) endComponent.getSubmittedValue();
                } catch (NullPointerException npe) {
                    // Do nothing - checkbox is not being shown (and is therefore not checked)
                }
            }
            if (!removedState && value == null) {
                String msgString = BundleUtil.getStringFromBundle("embargo.date.required");
                FacesMessage msg = new FacesMessage(msgString);
                msg.setSeverity(FacesMessage.SEVERITY_ERROR);
                throw new ValidatorException(msg);
            }
            Embargo newE = new Embargo(((LocalDate) value), null);
            if (!isValidEmbargoDate(newE)) {
                String minDate = getMinEmbargoDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                String maxDate = getMaxEmbargoDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                String msgString = BundleUtil.getStringFromBundle("embargo.date.invalid",
                        Arrays.asList(minDate, maxDate));
                // If we don't throw an exception here, the datePicker will use it's own
                // vaidator and display a default message. The value for that can be set by
                // adding validatorMessage="#{bundle['embargo.date.invalid']}" (a version with
                // no params) to the datepicker
                // element in file-edit-popup-fragment.html, but it would be better to catch all
                // problems here (so we can show a message with the min/max dates).
                FacesMessage msg = new FacesMessage(msgString);
                msg.setSeverity(FacesMessage.SEVERITY_ERROR);
                throw new ValidatorException(msg);
            }
        }
    }

    public LocalDate getMinRetentionDate() {
        if (!retentionDateChecked) {
            String months = getValueForKey(Key.MinRetentionDurationInMonths);
            Long minMonths = null;
            if (months != null) {
                try {
                    minMonths = Long.parseLong(months);
                } catch (NumberFormatException nfe) {
                    logger.warning("Cant interpret :MinRetentionDurationInMonths as a long");
                }
            }

            if (minMonths != null && minMonths != 0) {
                if (minMonths == -1) {
                    minMonths = 0l; // Absolute minimum is 0
                }
                minRetentionDate = LocalDate.now().plusMonths(minMonths);
            }
            retentionDateChecked = true;
        }
        return minRetentionDate;
    }

    public LocalDate getMaxRetentionDate() {
        Long maxMonths = 12000l; // Arbitrary cutoff at 1000 years - needs to keep maxDate < year 999999999 and
        // somehwere 1K> x >10K years the datepicker widget stops showing a popup
        // calendar
        return LocalDate.now().plusMonths(maxMonths);
    }

    public boolean isValidRetentionDate(Retention r) {

        if (r.getDateUnavailable()==null ||
            isRetentionAllowed() && r.getDateUnavailable().isAfter(getMinRetentionDate())) {
            return true;
        }

        return false;
    }

    public boolean isRetentionAllowed() {
        //Need a valid :MinRetentionDurationInMonths setting to allow retentions
        return getMinRetentionDate()!=null;
    }

    public void validateRetentionDate(FacesContext context, UIComponent component, Object value)
            throws ValidatorException {
        if (isRetentionAllowed()) {
            UIComponent cb = component.findComponent("retentionCheckbox");
            UIInput endComponent = (UIInput) cb;
            boolean removedState = false;
            if (endComponent != null) {
                try {
                    removedState = (Boolean) endComponent.getSubmittedValue();
                } catch (NullPointerException npe) {
                    // Do nothing - checkbox is not being shown (and is therefore not checked)
                }
            }
            if (!removedState && value == null) {
                String msgString = BundleUtil.getStringFromBundle("retention.date.required");
                FacesMessage msg = new FacesMessage(msgString);
                msg.setSeverity(FacesMessage.SEVERITY_ERROR);
                throw new ValidatorException(msg);
            }
            Retention newR = new Retention(((LocalDate) value), null);
            if (!isValidRetentionDate(newR)) {
                String minDate = getMinRetentionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                String maxDate = getMaxRetentionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                String msgString = BundleUtil.getStringFromBundle("retention.date.invalid",
                        Arrays.asList(minDate, maxDate));
                // If we don't throw an exception here, the datePicker will use it's own
                // vaidator and display a default message. The value for that can be set by
                // adding validatorMessage="#{bundle['retention.date.invalid']}" (a version with
                // no params) to the datepicker
                // element in file-edit-popup-fragment.html, but it would be better to catch all
                // problems here (so we can show a message with the min/max dates).
                FacesMessage msg = new FacesMessage(msgString);
                msg.setSeverity(FacesMessage.SEVERITY_ERROR);
                throw new ValidatorException(msg);
            }
        }
    }

    Map<String,String> languageMap = null;
    
    public Map<String, String> getBaseMetadataLanguageMap(boolean refresh) {
        if (languageMap == null || refresh) {
           languageMap = settingsService.getBaseMetadataLanguageMap(languageMap, true);
        }
        return languageMap;
    }
    
    public Map<String, String> getMetadataLanguages(DvObjectContainer target) {
        Map<String,String> currentMap = new HashMap<String,String>();
        currentMap.putAll(getBaseMetadataLanguageMap(false));
        currentMap.put(DvObjectContainer.UNDEFINED_CODE, getDefaultMetadataLanguageLabel(target));
        return currentMap;
    }

    private String getDefaultMetadataLanguageLabel(DvObjectContainer target) {
        String mlLabel = BundleUtil.getStringFromBundle("dataverse.metadatalanguage.setatdatasetcreation");

        if(target.getOwner() != null) { // Root collection is excluded from inherit metadata language research
            String mlCode = target.getOwner().getEffectiveMetadataLanguage();

            // If it's undefined, no parent has a metadata language defined, and the global default should be used.
            if (!mlCode.equals(DvObjectContainer.UNDEFINED_CODE)) {
                // Get the label for the language code found
                mlLabel = getBaseMetadataLanguageMap(false).get(mlCode);
                mlLabel = mlLabel + " " + BundleUtil.getStringFromBundle("dataverse.inherited");
            }
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
                //More than one - :MetadataLanguages is set and the default is undefined (users must choose if the collection doesn't override the default)
                return DvObjectContainer.UNDEFINED_CODE;
            }
        } else {
            // None - :MetadataLanguages is not set so return null to turn off the display (backward compatibility)
            return null;
        }
    }
    
    public Map<String, String> getGuestbookEntryOptions(DvObjectContainer target) {
        Map<String, String> currentMap = new HashMap<String, String>();
        String atDownload = BundleUtil.getStringFromBundle("dataverse.guestbookentry.atdownload");
        String atRequest = BundleUtil.getStringFromBundle("dataverse.guestbookentry.atrequest");
        Optional<Boolean> gbDefault = JvmSettings.GUESTBOOK_AT_REQUEST.lookupOptional(Boolean.class);
        if (gbDefault.isPresent()) {
            // Three options - inherited/default option, at Download, at Request
            String useDefault = null;
            if (target.getOwner() == null) {
                boolean defaultOption = gbDefault.get();
                useDefault = (defaultOption ? atRequest : atDownload)
                        + " " + BundleUtil.getStringFromBundle("dataverse.default");
            } else {
                boolean defaultOption = target.getOwner().getEffectiveGuestbookEntryAtRequest();
                useDefault = (defaultOption ? atRequest : atDownload)
                        + " " + BundleUtil.getStringFromBundle("dataverse.inherited");
            }
            currentMap.put(DvObjectContainer.UNDEFINED_CODE, useDefault);
            currentMap.put(Boolean.toString(true), atRequest);
            currentMap.put(Boolean.toString(false), atDownload);
        } else {
            // Setting not defined - leave empty
        }
        return currentMap;
    }
    
    public Dataverse getRootDataverse() {
        if (rootDataverse == null) {
            rootDataverse = dataverseService.findRootDataverse();
        }
        
        return rootDataverse;
    }
    
    // The following 2 methods may be unnecessary *with the current implementation* of 
    // how the application version is retrieved (the values are initialized and cached inside 
    // SystemConfig once per thread - see the code there); 
    // But in case we switch to some other method, that requires a database 
    // lookup like other settings, it should be here. 
    // This would be a prime candidate for moving into some kind of an 
    // APPLICATION-scope caching singleton. -- L.A. 5.8
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
            shibPassiveLoginEnabled = systemConfig.isShibPassiveLoginEnabled();
        }
        return shibPassiveLoginEnabled;
    }
    
    // Caching this result may not be saving much, *currently* (since the value is 
    // stored in the bundle). -- L.A. 5.8
    public String getFooterCopyrightAndYear() {
        if (footerCopyrightAndYear == null) {
            footerCopyrightAndYear = systemConfig.getFooterCopyrightAndYear();
        }
        return footerCopyrightAndYear; 
    }
    
    public Map<Long, JsonObject> getCVocConf(boolean byTermField) {
        if (byTermField) {
            if (cachedCvocByTermFieldMap == null) {
                cachedCvocByTermFieldMap = fieldService.getCVocConf(true);
            }
            return cachedCvocByTermFieldMap;
        } else {
            // Cache this in the view
            if (cachedCvocMap == null) {
                cachedCvocMap = fieldService.getCVocConf(false);
            }
            return cachedCvocMap;
        }
    }
    
    public boolean isCvocField(Long fieldId) {

        if(cvocFieldSet == null) {
            cvocFieldSet = fieldService.getCvocFieldSet();
        }
        if(cvocFieldSet == null) {
         return false;
        }
        return cvocFieldSet.contains(fieldId);
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

    List<String> allowedExternalStatuses = null;

    public List<String> getAllowedCurationStatuses(Dataset d) {
        String setName = d.getEffectiveCurationLabelSetName();
        if(setName.equals(SystemConfig.CURATIONLABELSDISABLED)) {
            return new ArrayList<String>();
        }
        String[] labelArray = systemConfig.getCurationLabels().get(setName);
        if(labelArray==null) {
            return new ArrayList<String>();
        }
        return Arrays.asList(labelArray);
    }

    public boolean isCustomLicenseAllowed() {
        if (customLicenseAllowed == null) {
            customLicenseAllowed = systemConfig.isAllowCustomTerms();
        }
        return customLicenseAllowed;
    }

    public List<MetadataBlock> getSystemMetadataBlocks() {

        if (systemMetadataBlocks == null) {
            systemMetadataBlocks = new ArrayList<MetadataBlock>();
        }
        List<MetadataBlock> blocks = mdbService.listMetadataBlocks();
        for (MetadataBlock mdb : blocks) {
            String smdbString = JvmSettings.MDB_SYSTEM_KEY_FOR.lookupOptional(mdb.getName()).orElse(null);
            if (smdbString != null) {
                systemMetadataBlocks.add(mdb);
            }
        }

        return systemMetadataBlocks;
    }
}