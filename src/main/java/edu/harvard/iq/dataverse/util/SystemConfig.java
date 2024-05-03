package edu.harvard.iq.dataverse.util;

import com.ocpsoft.pretty.PrettyContext;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DvObjectContainer;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.validation.PasswordValidatorUtil;
import org.passay.CharacterRule;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Year;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * System-wide configuration
 */
@Stateless
@Named
public class SystemConfig {

    private static final Logger logger = Logger.getLogger(SystemConfig.class.getCanonicalName());

    @EJB
    SettingsServiceBean settingsService;

    @EJB
    DataverseServiceBean dataverseService;

    @EJB
    AuthenticationServiceBean authenticationService;
    
   public static final String DATAVERSE_PATH = "/dataverse/";
   
    /**
     * Some installations may not want download URLs to their files to be
     * available in Schema.org JSON-LD output.
     */
    public static final String FILES_HIDE_SCHEMA_DOT_ORG_DOWNLOAD_URLS = "dataverse.files.hide-schema-dot-org-download-urls";

    /**
     * A JVM option to override the number of minutes for which a password reset
     * token is valid ({@link #getMinutesUntilPasswordResetTokenExpires}).
     */
    private static final String PASSWORD_RESET_TIMEOUT_IN_MINUTES = "dataverse.auth.password-reset-timeout-in-minutes";

    /**
     * The default number of datafiles that we allow to be created through 
     * zip file upload.
     */
    private static final int defaultZipUploadFilesLimit = 1000; 
    public static final long defaultZipDownloadLimit = 104857600L; // 100MB
    private static final int defaultMultipleUploadFilesLimit = 1000;
    private static final int defaultLoginSessionTimeout = 480; // = 8 hours
    
    private String buildNumber = null;
    
    private static final String JVM_TIMER_SERVER_OPTION = "dataverse.timerServer";
    
    private static final long DEFAULT_GUESTBOOK_RESPONSES_DISPLAY_LIMIT = 5000L; 
    private static final long DEFAULT_THUMBNAIL_SIZE_LIMIT_IMAGE = 3000000L; // 3 MB
    private static final long DEFAULT_THUMBNAIL_SIZE_LIMIT_PDF = 1000000L; // 1 MB
    
    public final static String DEFAULTCURATIONLABELSET = "DEFAULT";
    public final static String CURATIONLABELSDISABLED = "DISABLED";
    
    public String getVersion() {
        return getVersion(false);
    }
    
    // The return value is a "prviate static String", that should be initialized
    // once, on the first call (see the code below)... But this is a @Stateless 
    // bean... so that would mean "once per thread"? - this would be a prime 
    // candidate for being moved into some kind of an application-scoped caching
    // service... some CachingService @Singleton - ? (L.A. 5.8)
    public String getVersion(boolean withBuildNumber) {
        // Retrieve the version via MPCONFIG
        // NOTE: You may override the version via all methods of MPCONFIG.
        //       It will default to read from microprofile-config.properties source,
        //       which contains in the source a Maven property reference to ${project.version}.
        //       When packaging the app to deploy it, Maven will replace this, rendering it a static entry.
        String appVersion = JvmSettings.VERSION.lookup();
            
        if (withBuildNumber) {
            if (buildNumber == null) {
                // (build number is still in a .properties file in the source tree; it only
                // contains a real build number if this war file was built by Jenkins)
                // TODO: might be replaced with same trick as for version via Maven property w/ empty default
                try {
                    buildNumber = ResourceBundle.getBundle("BuildNumber").getString("build.number");
                } catch (MissingResourceException ex) {
                    buildNumber = null;
                }
                
                // Also try to read the build number via MicroProfile Config if not already present from the
                // properties file (so can be overridden by env var or other source)
                if (buildNumber == null || buildNumber.isEmpty()) {
                    buildNumber = JvmSettings.BUILD.lookupOptional().orElse("");
                }
            }
            
            if (!buildNumber.equals("")) {
                return appVersion + " build " + buildNumber;
            }
        }
        
        return appVersion;
    }
    
    /**
     * Retrieve the Solr endpoint in "host:port" form, to be used with a Solr client.
     *
     * This will retrieve the setting from either the database ({@link SettingsServiceBean.Key#SolrHostColonPort}) or
     * via Microprofile Config API (properties {@link JvmSettings#SOLR_HOST} and {@link JvmSettings#SOLR_PORT}).
     *
     * A database setting always takes precedence. If not given via other config sources, a default from
     * <code>resources/META-INF/microprofile-config.properties</code> is used. (It's possible to use profiles.)
     *
     * @return Solr endpoint as string "hostname:port"
     */
    public String getSolrHostColonPort() {
        // Get from MPCONFIG. Might be configured by a sysadmin or simply return the default shipped with
        // resources/META-INF/microprofile-config.properties.
        // NOTE: containers should use system property mp.config.profile=ct to use sane container usage default
        String host = JvmSettings.SOLR_HOST.lookup();
        String port = JvmSettings.SOLR_PORT.lookup();
        
        // DB setting takes precedence over all. If not present, will return default from above.
        return Optional.ofNullable(settingsService.getValueForKey(SettingsServiceBean.Key.SolrHostColonPort))
            .orElse(host + ":" + port);
    }

    public boolean isProvCollectionEnabled() {
        String provCollectionEnabled = settingsService.getValueForKey(SettingsServiceBean.Key.ProvCollectionEnabled, null);
        if("true".equalsIgnoreCase(provCollectionEnabled)){         
            return true;
        }
        return false;

    }
    
    public int getMetricsCacheTimeoutMinutes() {
        int defaultValue = 10080; //one week in minutes
        SettingsServiceBean.Key key = SettingsServiceBean.Key.MetricsCacheTimeoutMinutes;
        String metricsCacheTimeString = settingsService.getValueForKey(key);
        if (metricsCacheTimeString != null) {
            int returnInt = 0;
            try {
                returnInt = Integer.parseInt(metricsCacheTimeString);
                if (returnInt >= 0) {
                    return returnInt;
                } else {
                    logger.info("Returning " + defaultValue + " for " + key + " because value must be greater than zero, not \"" + metricsCacheTimeString + "\".");
                }
            } catch (NumberFormatException ex) {
                logger.info("Returning " + defaultValue + " for " + key + " because value must be an integer greater than zero, not \"" + metricsCacheTimeString + "\".");
            }
        }
        return defaultValue;
    }
    
    public int getMinutesUntilConfirmEmailTokenExpires() {
        final int minutesInOneDay = 1440;
        final int reasonableDefault = minutesInOneDay;
        SettingsServiceBean.Key key = SettingsServiceBean.Key.MinutesUntilConfirmEmailTokenExpires;
        String valueFromDatabase = settingsService.getValueForKey(key);
        if (valueFromDatabase != null) {
            try {
                int intFromDatabase = Integer.parseInt(valueFromDatabase);
                if (intFromDatabase > 0) {
                    return intFromDatabase;
                } else {
                    logger.info("Returning " + reasonableDefault + " for " + key + " because value must be greater than zero, not \"" + intFromDatabase + "\".");
                }
            } catch (NumberFormatException ex) {
                logger.info("Returning " + reasonableDefault + " for " + key + " because value must be an integer greater than zero, not \"" + valueFromDatabase + "\".");
            }
        }
        logger.fine("Returning " + reasonableDefault + " for " + key);
        return reasonableDefault;
    }

    /**
     * The number of minutes for which a password reset token is valid. Can be
     * overridden by {@link #PASSWORD_RESET_TIMEOUT_IN_MINUTES}.
     */
    public static int getMinutesUntilPasswordResetTokenExpires() {
        final int reasonableDefault = 60;
        String configuredValueAsString = System.getProperty(PASSWORD_RESET_TIMEOUT_IN_MINUTES);
        if (configuredValueAsString != null) {
            int configuredValueAsInteger = 0;
            try {
                configuredValueAsInteger = Integer.parseInt(configuredValueAsString);
                if (configuredValueAsInteger > 0) {
                    return configuredValueAsInteger;
                } else {
                    logger.info(PASSWORD_RESET_TIMEOUT_IN_MINUTES + " is configured as a negative number \"" + configuredValueAsInteger + "\". Using default value instead: " + reasonableDefault);
                    return reasonableDefault;
                }
            } catch (NumberFormatException ex) {
                logger.info("Unable to convert " + PASSWORD_RESET_TIMEOUT_IN_MINUTES + " from \"" + configuredValueAsString + "\" into an integer value: " + ex + ". Using default value " + reasonableDefault);
            }
        }
        return reasonableDefault;
    }
    
    /**
     * Lookup (or construct) the designated URL of this instance from configuration.
     *
     * Can be defined as a complete URL via <code>dataverse.siteUrl</code>; or derived from the hostname
     * <code>dataverse.fqdn</code> and HTTPS. If none of these options is set, defaults to the
     * {@link InetAddress#getLocalHost} and HTTPS.
     *
     * NOTE: This method does not provide any validation.
     * TODO: The behaviour of this method is subject to a later change, see
     *       https://github.com/IQSS/dataverse/issues/6636
     *
     * @return The designated URL of this instance as per configuration.
     */
    public String getDataverseSiteUrl() {
        return getDataverseSiteUrlStatic();
    }
    
    /**
     * Lookup (or construct) the designated URL of this instance from configuration.
     *
     * Can be defined as a complete URL via <code>dataverse.siteUrl</code>; or derived from the hostname
     * <code>dataverse.fqdn</code> and HTTPS. If none of these options is set, defaults to the
     * {@link InetAddress#getLocalHost} and HTTPS.
     *
     * NOTE: This method does not provide any validation.
     * TODO: The behaviour of this method is subject to a later change, see
     *       https://github.com/IQSS/dataverse/issues/6636
     *
     * @return The designated URL of this instance as per configuration.
     */
    public static String getDataverseSiteUrlStatic() {
        // If dataverse.siteUrl has been configured, simply return it
        Optional<String> siteUrl = JvmSettings.SITE_URL.lookupOptional();
        if (siteUrl.isPresent()) {
            return siteUrl.get();
        }
        
        // Otherwise try to lookup dataverse.fqdn setting and default to HTTPS
        Optional<String> fqdn = JvmSettings.FQDN.lookupOptional();
        if (fqdn.isPresent()) {
            return "https://" + fqdn.get();
        }
        
        // Last resort - get the servers local name and use it.
        // BEWARE - this is dangerous.
        // 1) A server might have a different name than your repository URL.
        // 2) The underlying reverse DNS lookup might point to a different name than your repository URL.
        // 3) If this server has multiple IPs assigned, which one will it be for the lookup?
        try {
            return "https://" + InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            return null;
        }
    }
    
    /**
     * URL Tracking: 
     */
    public String getPageURLWithQueryString() {
        return PrettyContext.getCurrentInstance().getRequestURL().toURL() + PrettyContext.getCurrentInstance().getRequestQueryString().toQueryString();
    }

    public String getGuidesBaseUrl() {
        String saneDefault = "https://guides.dataverse.org";
        String guidesBaseUrl = settingsService.getValueForKey(SettingsServiceBean.Key.GuidesBaseUrl, saneDefault);
        return guidesBaseUrl + "/" + getGuidesLanguage();
    }

    private String getGuidesLanguage() {
        String saneDefault = "en";
        return saneDefault;
    }

    public String getGuidesVersion() {
        String saneDefault = getVersion();
        String guidesVersion = settingsService.getValueForKey(SettingsServiceBean.Key.GuidesVersion, saneDefault);
        if (guidesVersion != null) {
            return guidesVersion;
        }
        return saneDefault;
    }

    public String getMetricsUrl() {
        String saneDefault = null;
        String metricsUrl = settingsService.getValueForKey(SettingsServiceBean.Key.MetricsUrl, saneDefault);
        return metricsUrl;
    }

    public static long getLongLimitFromStringOrDefault(String limitSetting, Long defaultValue) {
        Long limit = null;

        if (limitSetting != null && !limitSetting.equals("")) {
            try {
                limit = Long.valueOf(limitSetting);
            } catch (NumberFormatException nfe) {
                limit = null;
            }
        }

        return limit != null ? limit : defaultValue;
    }

    public static int getIntLimitFromStringOrDefault(String limitSetting, Integer defaultValue) {
        Integer limit = null;

        if (limitSetting != null && !limitSetting.equals("")) {
            try {
                limit = Integer.valueOf(limitSetting);
            } catch (NumberFormatException nfe) {
                limit = null;
            }
        }

        return limit != null ? limit : defaultValue;
    }

    /**
     * Download-as-zip size limit.
     * returns defaultZipDownloadLimit if not specified; 
     * set to -1 to disable zip downloads. 
     */
    public long getZipDownloadLimit() {
        String zipLimitOption = settingsService.getValueForKey(SettingsServiceBean.Key.ZipDownloadLimit);
        return getLongLimitFromStringOrDefault(zipLimitOption, defaultZipDownloadLimit);
    }
    
    public int getZipUploadFilesLimit() {
        String limitOption = settingsService.getValueForKey(SettingsServiceBean.Key.ZipUploadFilesLimit);
        return getIntLimitFromStringOrDefault(limitOption, defaultZipUploadFilesLimit);
    }
    
    /**
     * Session timeout, in minutes. 
     * (default value provided)
     */
    public int getLoginSessionTimeout() {
        return getIntLimitFromStringOrDefault(
                settingsService.getValueForKey(SettingsServiceBean.Key.LoginSessionTimeout), 
                defaultLoginSessionTimeout); 
    }
    
    /*
    `   the number of files the GUI user is allowed to upload in one batch, 
        via drag-and-drop, or through the file select dialog
    */
    public int getMultipleUploadFilesLimit() {
        String limitOption = settingsService.getValueForKey(SettingsServiceBean.Key.MultipleUploadFilesLimit);
        return getIntLimitFromStringOrDefault(limitOption, defaultMultipleUploadFilesLimit);
    }
    
    public long getGuestbookResponsesPageDisplayLimit() {
        String limitSetting = settingsService.getValueForKey(SettingsServiceBean.Key.GuestbookResponsesPageDisplayLimit);
        return getLongLimitFromStringOrDefault(limitSetting, DEFAULT_GUESTBOOK_RESPONSES_DISPLAY_LIMIT);
    }
    
    public long getUploadLogoSizeLimit(){
        return 500000;
    }

    public long getThumbnailSizeLimitImage() {
        return getThumbnailSizeLimit("Image");
    }

    public long getThumbnailSizeLimitPDF() {
        return getThumbnailSizeLimit("PDF");
    }

    public static long getThumbnailSizeLimit(String type) {
        String option = null; 
        
        //get options via jvm options
        
        if ("Image".equals(type)) {
            option = System.getProperty("dataverse.dataAccess.thumbnail.image.limit");
            return getLongLimitFromStringOrDefault(option, DEFAULT_THUMBNAIL_SIZE_LIMIT_IMAGE);
        } else if ("PDF".equals(type)) {
            option = System.getProperty("dataverse.dataAccess.thumbnail.pdf.limit");
            return getLongLimitFromStringOrDefault(option, DEFAULT_THUMBNAIL_SIZE_LIMIT_PDF);
        }

        // Zero (0) means no limit.
        return getLongLimitFromStringOrDefault(option, 0L);
    }
    
    public boolean isThumbnailGenerationDisabledForType(String type) {
        return getThumbnailSizeLimit(type) == -1l;
    }
    
    public boolean isThumbnailGenerationDisabledForImages() {
        return isThumbnailGenerationDisabledForType("Image");
    }
    
    public boolean isThumbnailGenerationDisabledForPDF() {
        return isThumbnailGenerationDisabledForType("PDF");
    }
    
    public String getApplicationTermsOfUse() {
        String language = BundleUtil.getCurrentLocale().getLanguage();
        String saneDefaultForAppTermsOfUse = BundleUtil.getStringFromBundle("system.app.terms");
        // Get the value for the defaultLocale. IT will either be used as the return
        // value, or as a better default than the saneDefaultForAppTermsOfUse if there
        // is no language-specific value
        String appTermsOfUse = settingsService.getValueForKey(SettingsServiceBean.Key.ApplicationTermsOfUse, saneDefaultForAppTermsOfUse);
        //Now get the language-specific value if it exists
        if (language != null && !language.equalsIgnoreCase(BundleUtil.getDefaultLocale().getLanguage())) {
            appTermsOfUse = settingsService.getValueForKey(SettingsServiceBean.Key.ApplicationTermsOfUse, language,	appTermsOfUse);
        }
        return appTermsOfUse;
    }

    public String getApiTermsOfUse() {
        String saneDefaultForApiTermsOfUse = BundleUtil.getStringFromBundle("system.api.terms");
        String apiTermsOfUse = settingsService.getValueForKey(SettingsServiceBean.Key.ApiTermsOfUse, saneDefaultForApiTermsOfUse);
        return apiTermsOfUse;
    }

    // TODO: 
    // remove this method!
    // pages should be using settingsWrapper.get(":ApplicationPrivacyPolicyUrl") instead. -- 4.2.1
    public String getApplicationPrivacyPolicyUrl() {
        String saneDefaultForPrivacyPolicyUrl = null;
        String appPrivacyPolicyUrl = settingsService.getValueForKey(SettingsServiceBean.Key.ApplicationPrivacyPolicyUrl, saneDefaultForPrivacyPolicyUrl);
        return appPrivacyPolicyUrl;
    }

    public boolean myDataDoesNotUsePermissionDocs() {
        boolean safeDefaultIfKeyNotFound = false;
        return settingsService.isTrueForKey(SettingsServiceBean.Key.MyDataDoesNotUseSolrPermissionDocs, safeDefaultIfKeyNotFound);
    }

    public boolean isFilesOnDatasetPageFromSolr() {
        boolean safeDefaultIfKeyNotFound = false;
        return settingsService.isTrueForKey(SettingsServiceBean.Key.FilesOnDatasetPageFromSolr, safeDefaultIfKeyNotFound);
    }

    public Long getMaxFileUploadSizeForStore(String driverId){
         return settingsService.getValueForCompoundKeyAsLong(SettingsServiceBean.Key.MaxFileUploadSizeInBytes, driverId);
     }
    
    public Integer getSearchHighlightFragmentSize() {
        String fragSize = settingsService.getValueForKey(SettingsServiceBean.Key.SearchHighlightFragmentSize);
        if (fragSize != null) {
            try {
                return new Integer(fragSize);
            } catch (NumberFormatException nfe) {
                logger.info("Could not convert " + SettingsServiceBean.Key.SearchHighlightFragmentSize + " to int: " + nfe);
            }
        }
        return null;
    }

    public long getTabularIngestSizeLimit() {
        // This method will return the blanket ingestable size limit, if 
        // set on the system. I.e., the universal limit that applies to all 
        // tabular ingests, regardless of fromat: 
        
        String limitEntry = settingsService.getValueForKey(SettingsServiceBean.Key.TabularIngestSizeLimit); 
        
        if (limitEntry != null) {
            try {
                Long sizeOption = new Long(limitEntry);
                return sizeOption;
            } catch (NumberFormatException nfe) {
                logger.warning("Invalid value for TabularIngestSizeLimit option? - " + limitEntry);
            }
        }
        // -1 means no limit is set; 
        // 0 on the other hand would mean that ingest is fully disabled for 
        // tabular data. 
        return -1; 
    }
    
    public long getTabularIngestSizeLimit(String formatName) {
        // This method returns the size limit set specifically for this format name,
        // if available, otherwise - the blanket limit that applies to all tabular 
        // ingests regardless of a format. 
        
        if (formatName == null || formatName.equals("")) {
            return getTabularIngestSizeLimit(); 
        }
        
        String limitEntry = settingsService.get(SettingsServiceBean.Key.TabularIngestSizeLimit.toString() + ":" + formatName); 
                
        if (limitEntry != null) {
            try {
                Long sizeOption = new Long(limitEntry);
                return sizeOption;
            } catch (NumberFormatException nfe) {
                logger.warning("Invalid value for TabularIngestSizeLimit:" + formatName + "? - " + limitEntry );
            }
        }
        
        return getTabularIngestSizeLimit();        
    }

    public boolean isOAIServerEnabled() {
        boolean defaultResponse = false;
        return settingsService.isTrueForKey(SettingsServiceBean.Key.OAIServerEnabled, defaultResponse);
    }
    
    public void enableOAIServer() {
        settingsService.setValueForKey(SettingsServiceBean.Key.OAIServerEnabled, "true");
    }
    
    public void disableOAIServer() {
        settingsService.deleteValueForKey(SettingsServiceBean.Key.OAIServerEnabled);
    }   
    
    public boolean isTimerServer() {
        String optionValue = System.getProperty(JVM_TIMER_SERVER_OPTION);
        if ("true".equalsIgnoreCase(optionValue)) {
            return true;
        }
        return false;
    }

    public String getFooterCopyrightAndYear() {
        return BundleUtil.getStringFromBundle("footer.copyright", Arrays.asList(Year.now().getValue() + ""));
    }

    public DataFile.ChecksumType getFileFixityChecksumAlgorithm() {
        DataFile.ChecksumType saneDefault = DataFile.ChecksumType.MD5;
        String checksumStringFromDatabase = settingsService.getValueForKey(SettingsServiceBean.Key.FileFixityChecksumAlgorithm, saneDefault.toString());
        try {
            DataFile.ChecksumType checksumTypeFromDatabase = DataFile.ChecksumType.fromString(checksumStringFromDatabase);
            return checksumTypeFromDatabase;
        } catch (IllegalArgumentException ex) {
            logger.info("The setting " + SettingsServiceBean.Key.FileFixityChecksumAlgorithm + " is misconfigured. " + ex.getMessage() + " Returning sane default: " + saneDefault + ".");
            return saneDefault;
        }
    }

    public String getDefaultAuthProvider() {
        String saneDefault = BuiltinAuthenticationProvider.PROVIDER_ID;
        String settingInDatabase = settingsService.getValueForKey(SettingsServiceBean.Key.DefaultAuthProvider, saneDefault);
        if (settingInDatabase != null && !settingInDatabase.isEmpty()) {
            /**
             * @todo Add more sanity checking.
             */
            return settingInDatabase;
        }
        return saneDefault;
    }

    public String getNameOfInstallation() {
        return dataverseService.getRootDataverseName();
    }

    public AbstractOAuth2AuthenticationProvider.DevOAuthAccountType getDevOAuthAccountType() {
        AbstractOAuth2AuthenticationProvider.DevOAuthAccountType saneDefault = AbstractOAuth2AuthenticationProvider.DevOAuthAccountType.PRODUCTION;
        String settingReturned = settingsService.getValueForKey(SettingsServiceBean.Key.DebugOAuthAccountType);
        logger.fine("setting returned: " + settingReturned);
        if (settingReturned != null) {
            try {
                AbstractOAuth2AuthenticationProvider.DevOAuthAccountType parsedValue = AbstractOAuth2AuthenticationProvider.DevOAuthAccountType.valueOf(settingReturned);
                return parsedValue;
            } catch (IllegalArgumentException ex) {
                logger.info("Couldn't parse value: " + ex + " - returning a sane default: " + saneDefault);
                return saneDefault;
            }
        } else {
            logger.fine("OAuth dev mode has not been configured. Returning a sane default: " + saneDefault);
            return saneDefault;
        }
    }

    public String getOAuth2CallbackUrl() {
        String saneDefault = getDataverseSiteUrl() + "/oauth2/callback.xhtml";
        String settingReturned = settingsService.getValueForKey(SettingsServiceBean.Key.OAuth2CallbackUrl);
        logger.fine("getOAuth2CallbackUrl setting returned: " + settingReturned);
        if (settingReturned != null) {
            return settingReturned;
        }
        return saneDefault;
    }
    
    public boolean isShibPassiveLoginEnabled() {
        boolean defaultResponse = false;
        return settingsService.isTrueForKey(SettingsServiceBean.Key.ShibPassiveLoginEnabled, defaultResponse);
    }
    public boolean isShibAttributeCharacterSetConversionEnabled() {
        boolean defaultResponse = true;
        return settingsService.isTrueForKey(SettingsServiceBean.Key.ShibAttributeCharacterSetConversionEnabled, defaultResponse);
    }

    /**
     * getPVDictionaries
     *
     * @return A string of one or more pipe (|) separated file paths.
     */
    public String getPVDictionaries() {
        return settingsService.get(SettingsServiceBean.Key.PVDictionaries.toString());
    }

    /**
     * getPVGoodStrength
     *
     * Get the minimum length of a valid password to apply an expiration rule.
     * Defaults to 20.
     *
     * @return The length.
     */
    public int getPVGoodStrength() {
        // FIXME: Change this to 21 to match Harvard's requirements or implement a way to disable the rule (0 or -1) and have the default be disabled.
        int goodStrengthLength = 20;
        //String _goodStrengthLength = System.getProperty("pv.goodstrength", settingsService.get(SettingsServiceBean.Key.PVGoodStrength.toString()));
        String _goodStrengthLength = settingsService.get(SettingsServiceBean.Key.PVGoodStrength.toString());
        try {
            goodStrengthLength = Integer.parseInt(_goodStrengthLength);
        } catch (NumberFormatException nfe) {
            logger.fine("Invalid value for PVGoodStrength: " + _goodStrengthLength);
        }
        return goodStrengthLength;
    }

    /**
     * getPVMinLength
     *
     * Get the minimum length of a valid password. Defaults to 6.
     *
     * @return The length.
     */
    public int getPVMinLength() {
        int passportValidatorMinLength = 6;
        String _passportValidatorMinLength = settingsService.get(SettingsServiceBean.Key.PVMinLength.toString());
        try {
            passportValidatorMinLength = Integer.parseInt(_passportValidatorMinLength);
        } catch (NumberFormatException nfe) {
            logger.fine("Invalid value for PwMinLength: " + _passportValidatorMinLength);
        }
        return passportValidatorMinLength;
    }

    /**
     * getPVMaxLength
     *
     * Get the maximum length of a valid password. Defaults to 0 (disabled).
     *
     * @return The length.
     */
    public int getPVMaxLength() {
        int passportValidatorMaxLength = 0;
        String _passportValidatorMaxLength = settingsService.get(SettingsServiceBean.Key.PVMaxLength.toString());
        try {
            passportValidatorMaxLength = Integer.parseInt(_passportValidatorMaxLength);
        } catch (NumberFormatException nfe) {
            logger.fine("Invalid value for PwMaxLength: " + _passportValidatorMaxLength);
        }
        return passportValidatorMaxLength;
    }

    /**
     * One letter, 2 special characters, etc. Defaults to:
     *
     * - one uppercase
     *
     * - one lowercase
     *
     * - one digit
     *
     * - one special character
     *
     * TODO: This is more strict than what Dataverse 4.0 shipped with. Consider
     * keeping the default the same.
     */
    public List<CharacterRule> getPVCharacterRules() {
        String characterRulesString = settingsService.get(SettingsServiceBean.Key.PVCharacterRules.toString());
        return PasswordValidatorUtil.getCharacterRules(characterRulesString);
    }

    /**
     * getPVNumberOfCharacteristics
     *
     * Get the number M characteristics. Defaults to 3.
     *
     * @return The number.
     * 
     * TODO: Consider changing the out-of-the-box rules to be the same as Dataverse 4.0, which was 2 (one letter, one number).
     */
    public int getPVNumberOfCharacteristics() {
        int numberOfCharacteristics = 2;
        String _numberOfCharacteristics = settingsService.get(SettingsServiceBean.Key.PVNumberOfCharacteristics.toString());
        try {
            numberOfCharacteristics = Integer.parseInt(_numberOfCharacteristics);
        } catch (NumberFormatException nfe) {
            logger.fine("Invalid value for PVNumberOfCharacteristics: " + _numberOfCharacteristics);
        }
        return numberOfCharacteristics;
    }

    /**
     * Get the number of consecutive digits allowed. Defaults to highest int
     * possible.
     */
    public int getPVNumberOfConsecutiveDigitsAllowed() {
        int numConsecutiveDigitsAllowed = Integer.MAX_VALUE;
        String _numberOfConsecutiveDigitsAllowed = settingsService.get(SettingsServiceBean.Key.PVNumberOfConsecutiveDigitsAllowed.toString());
        try {
            numConsecutiveDigitsAllowed = Integer.parseInt(_numberOfConsecutiveDigitsAllowed);
        } catch (NumberFormatException nfe) {
            logger.fine("Invalid value for " + SettingsServiceBean.Key.PVNumberOfConsecutiveDigitsAllowed + ": " + _numberOfConsecutiveDigitsAllowed);
        }
        return numConsecutiveDigitsAllowed;
    }

    /**
     * Below are three related enums having to do with big data support:
     *
     * - FileUploadMethods
     *
     * - FileDownloadMethods
     *
     * - TransferProtocols
     *
     * There is a good chance these will be consolidated in the future.
     */
    public enum FileUploadMethods {

        /**
         * DCM stands for Data Capture Module. Right now it supports upload over
         * rsync+ssh but DCM may support additional methods in the future.
         */
        RSYNC("dcm/rsync+ssh"),
        /**
         * Traditional Dataverse file handling, which tends to involve users
         * uploading and downloading files using a browser or APIs.
         */
        NATIVE("native/http"),

        /**
         * Upload through Globus of large files
         */

        GLOBUS("globus"), 
        
        /**
         * Upload folders of files through dvwebloader app
         */

        WEBLOADER("dvwebloader");
        ;


        private final String text;

        private FileUploadMethods(final String text) {
            this.text = text;
        }

        public static FileUploadMethods fromString(String text) {
            if (text != null) {
                for (FileUploadMethods fileUploadMethods : FileUploadMethods.values()) {
                    if (text.equals(fileUploadMethods.text)) {
                        return fileUploadMethods;
                    }
                }
            }
            throw new IllegalArgumentException("FileUploadMethods must be one of these values: " + Arrays.asList(FileUploadMethods.values()) + ".");
        }

        @Override
        public String toString() {
            return text;
        }
        
        
    }

    /**
     * See FileUploadMethods.
     *
     * TODO: Consider if dataverse.files.<id>.download-redirect belongs here since
     * it's a way to bypass Glassfish when downloading.
     */
    public enum FileDownloadMethods {
        /**
         * RSAL stands for Repository Storage Abstraction Layer. Downloads don't
         * go through Glassfish.
         */
        RSYNC("rsal/rsync"),
        NATIVE("native/http"),
        GLOBUS("globus")
        ;
        private final String text;

        private FileDownloadMethods(final String text) {
            this.text = text;
        }

        public static FileUploadMethods fromString(String text) {
            if (text != null) {
                for (FileUploadMethods fileUploadMethods : FileUploadMethods.values()) {
                    if (text.equals(fileUploadMethods.text)) {
                        return fileUploadMethods;
                    }
                }
            }
            throw new IllegalArgumentException("FileDownloadMethods must be one of these values: " + Arrays.asList(FileDownloadMethods.values()) + ".");
        }

        @Override
        public String toString() {
            return text;
        }
        
    }
    
    public enum DataFilePIDFormat {
        DEPENDENT("DEPENDENT"),
        INDEPENDENT("INDEPENDENT");
        private final String text;

        public String getText() {
            return text;
        }
        
        private DataFilePIDFormat(final String text){
            this.text = text;
        }
        
        @Override
        public String toString() {
            return text;
        }
        
    }

    /**
     * See FileUploadMethods.
     */
    public enum TransferProtocols {

        RSYNC("rsync"),
        /**
         * POSIX includes NFS. This is related to Key.LocalDataAccessPath in
         * SettingsServiceBean.
         */
        POSIX("posix"),
        GLOBUS("globus");

        private final String text;

        private TransferProtocols(final String text) {
            this.text = text;
        }

        public static TransferProtocols fromString(String text) {
            if (text != null) {
                for (TransferProtocols transferProtocols : TransferProtocols.values()) {
                    if (text.equals(transferProtocols.text)) {
                        return transferProtocols;
                    }
                }
            }
            throw new IllegalArgumentException("TransferProtocols must be one of these values: " + Arrays.asList(TransferProtocols.values()) + ".");
        }

        @Override
        public String toString() {
            return text;
        }

    }
    
    public boolean isPublicInstall(){
        boolean saneDefault = false;
        return settingsService.isTrueForKey(SettingsServiceBean.Key.PublicInstall, saneDefault);
    }
    
    public boolean isRsyncUpload(){
        return getMethodAvailable(SystemConfig.FileUploadMethods.RSYNC.toString(), true);
    }

    public boolean isGlobusUpload(){
        return getMethodAvailable(FileUploadMethods.GLOBUS.toString(), true);
    }
    
    public boolean isWebloaderUpload(){
        return getMethodAvailable(FileUploadMethods.WEBLOADER.toString(), true);
    }

    // Controls if HTTP upload is enabled for both GUI and API.
    public boolean isHTTPUpload(){       
        return getMethodAvailable(SystemConfig.FileUploadMethods.NATIVE.toString(), true);
    }
    
    public boolean isRsyncOnly(){
        String downloadMethods = settingsService.getValueForKey(SettingsServiceBean.Key.DownloadMethods);
        if(downloadMethods == null){
            return false;
        }
        if (!downloadMethods.toLowerCase().equals(SystemConfig.FileDownloadMethods.RSYNC.toString())){
            return false;
        }
        String uploadMethods = settingsService.getValueForKey(SettingsServiceBean.Key.UploadMethods);
        if (uploadMethods==null){
            return false;
        } else {
           return  Arrays.asList(uploadMethods.toLowerCase().split("\\s*,\\s*")).size() == 1 && uploadMethods.toLowerCase().equals(SystemConfig.FileUploadMethods.RSYNC.toString());
        }
    }
    
    public boolean isRsyncDownload() {
        return getMethodAvailable(SystemConfig.FileUploadMethods.RSYNC.toString(), false);
    }
    
    public boolean isHTTPDownload() {
        return getMethodAvailable(SystemConfig.FileUploadMethods.NATIVE.toString(), false);
    }

    public boolean isGlobusDownload() {
        return getMethodAvailable(FileDownloadMethods.GLOBUS.toString(), false);
    }
    
    public boolean isGlobusFileDownload() {
        return (isGlobusDownload() && settingsService.isTrueForKey(SettingsServiceBean.Key.GlobusSingleFileTransfer, false));
    }

    private Boolean getMethodAvailable(String method, boolean upload) {
        String methods = settingsService.getValueForKey(
                upload ? SettingsServiceBean.Key.UploadMethods : SettingsServiceBean.Key.DownloadMethods);
        if (methods == null) {
            return false;
        } else {
            return Arrays.asList(methods.toLowerCase().split("\\s*,\\s*")).contains(method);
        }
    }
    
    public Integer getUploadMethodCount(){
        String uploadMethods = settingsService.getValueForKey(SettingsServiceBean.Key.UploadMethods); 
        if (uploadMethods==null){
            return 0;
        } else {
           return  Arrays.asList(uploadMethods.toLowerCase().split("\\s*,\\s*")).size();
        }       
    }

    public boolean isAllowCustomTerms() {
        boolean safeDefaultIfKeyNotFound = true;
        return settingsService.isTrueForKey(SettingsServiceBean.Key.AllowCustomTermsOfUse, safeDefaultIfKeyNotFound);
    }

    public boolean isFilePIDsEnabledForCollection(Dataverse collection) {
        if (collection == null) {
            return false;
        }
        
        Dataverse thisCollection = collection; 
        
        // If neither enabled nor disabled specifically for this collection,
        // the parent collection setting is inhereted (recursively): 
        while (thisCollection.getFilePIDsEnabled() == null) {
            if (thisCollection.getOwner() == null) {
                // We've reached the root collection, and file PIDs registration
                // hasn't been explicitly enabled, therefore we presume that it is
                // subject to how the registration is configured for the 
                // entire instance:
                return settingsService.isTrueForKey(SettingsServiceBean.Key.FilePIDsEnabled, false); 
            }
            thisCollection = thisCollection.getOwner();
        }
        
        // If present, the setting of the first direct ancestor collection 
        // takes precedent:
        return thisCollection.getFilePIDsEnabled();
    }
    


    public String getMDCLogPath() {
        String mDCLogPath = settingsService.getValueForKey(SettingsServiceBean.Key.MDCLogPath, null);
        return mDCLogPath;
    }
    
    public boolean isDatafileValidationOnPublishEnabled() {
        boolean safeDefaultIfKeyNotFound = true;
        return settingsService.isTrueForKey(SettingsServiceBean.Key.FileValidationOnPublishEnabled, safeDefaultIfKeyNotFound);
    }

	public boolean directUploadEnabled(DvObjectContainer container) {
    	return Boolean.getBoolean("dataverse.files." + container.getEffectiveStorageDriverId() + ".upload-redirect");
	}
        
    public boolean isExternalDataverseValidationEnabled() {
        return settingsService.getValueForKey(SettingsServiceBean.Key.DataverseMetadataValidatorScript) != null;
        // alternatively, we can also check if the script specified exists, 
        // and is executable. -- ?
    }
    
    public boolean isExternalDatasetValidationEnabled() {
        return settingsService.getValueForKey(SettingsServiceBean.Key.DatasetMetadataValidatorScript) != null;
        // alternatively, we can also check if the script specified exists, 
        // and is executable. -- ?
    }
    
    public String getDataverseValidationExecutable() {
        return settingsService.getValueForKey(SettingsServiceBean.Key.DataverseMetadataValidatorScript);
    }
    
    public String getDatasetValidationExecutable() {
        return settingsService.getValueForKey(SettingsServiceBean.Key.DatasetMetadataValidatorScript);
    }
    
    public String getDataverseValidationFailureMsg() {
        String defaultMessage = "This dataverse collection cannot be published because it has failed an external metadata validation test.";
        return settingsService.getValueForKey(SettingsServiceBean.Key.DataverseMetadataPublishValidationFailureMsg, defaultMessage);
    }
    
    public String getDataverseUpdateValidationFailureMsg() {
        String defaultMessage = "This dataverse collection cannot be updated because it has failed an external metadata validation test.";
        return settingsService.getValueForKey(SettingsServiceBean.Key.DataverseMetadataUpdateValidationFailureMsg, defaultMessage);
    }
    
    public String getDatasetValidationFailureMsg() {
        String defaultMessage = "This dataset cannot be published because it has failed an external metadata validation test.";
        return settingsService.getValueForKey(SettingsServiceBean.Key.DatasetMetadataValidationFailureMsg, defaultMessage);
    }
    
    public boolean isExternalValidationAdminOverrideEnabled() {
        return "true".equalsIgnoreCase(settingsService.getValueForKey(SettingsServiceBean.Key.ExternalValidationAdminOverride));
    }
    
    public long getDatasetValidationSizeLimit() {
        String limitEntry = settingsService.getValueForKey(SettingsServiceBean.Key.DatasetChecksumValidationSizeLimit);

        if (limitEntry != null) {
            try {
                Long sizeOption = new Long(limitEntry);
                return sizeOption;
            } catch (NumberFormatException nfe) {
                logger.warning("Invalid value for DatasetValidationSizeLimit option? - " + limitEntry);
            }
        }
        // -1 means no limit is set;
        return -1;
    }

    public long getFileValidationSizeLimit() {
        String limitEntry = settingsService.getValueForKey(SettingsServiceBean.Key.DataFileChecksumValidationSizeLimit);

        if (limitEntry != null) {
            try {
                Long sizeOption = new Long(limitEntry);
                return sizeOption;
            } catch (NumberFormatException nfe) {
                logger.warning("Invalid value for FileValidationSizeLimit option? - " + limitEntry);
            }
        }
        // -1 means no limit is set;
        return -1;
    }
    public Map<String, String[]> getCurationLabels() {
        Map<String, String[]> labelMap = new HashMap<String, String[]>();
        String setting = settingsService.getValueForKey(SettingsServiceBean.Key.AllowedCurationLabels, "");
        if (!setting.isEmpty()) {
            try (JsonReader jsonReader = Json.createReader(new StringReader(setting))){
                
                Pattern pattern = Pattern.compile("(^[\\w ]+$)"); // alphanumeric, underscore and whitespace allowed

                JsonObject labelSets = jsonReader.readObject();
                for (String key : labelSets.keySet()) {
                    JsonArray labels = (JsonArray) labelSets.getJsonArray(key);
                    String[] labelArray = new String[labels.size()];

                    boolean allLabelsOK = true;
                    Iterator<JsonValue> iter = labels.iterator();
                    int i = 0;
                    while (iter.hasNext()) {
                        String label = ((JsonString) iter.next()).getString();
                        Matcher matcher = pattern.matcher(label);
                        if (!matcher.matches()) {
                            logger.warning("Label rejected: " + label + ", Label set " + key + " ignored.");
                            allLabelsOK = false;
                            break;
                        }
                        labelArray[i] = label;
                        i++;
                    }
                    if (allLabelsOK) {
                        labelMap.put(key, labelArray);
                    }
                }
            } catch (Exception e) {
                logger.warning("Unable to parse " + SettingsServiceBean.Key.AllowedCurationLabels.name() + ": "
                        + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
        return labelMap;
    }
    
    public boolean isSignupDisabledForRemoteAuthProvider(String providerId) {
        Boolean ret =  settingsService.getValueForCompoundKeyAsBoolean(SettingsServiceBean.Key.AllowRemoteAuthSignUp, providerId);
        
        // we default to false - i.e., "not disabled" if the setting is not present: 
        if (ret == null) {
            return false; 
        }
        
        return !ret; 
    }
    
    public boolean isStorageQuotasEnforced() {
        return settingsService.isTrueForKey(SettingsServiceBean.Key.UseStorageQuotas, false);
    }
    
    /**
     * This method should only be used for testing of the new storage quota 
     * mechanism, temporarily. (it uses the same value as the quota for 
     * *everybody* regardless of the circumstances, defined as a database 
     * setting)
     */
    public Long getTestStorageQuotaLimit() {
        return settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.StorageQuotaSizeInBytes);
    }
    /**
     * Should we store tab-delimited files produced during ingest *with* the
     * variable name header line included?
     * @return boolean - defaults to false.
     */
    public boolean isStoringIngestedFilesWithHeaders() {
        return settingsService.isTrueForKey(SettingsServiceBean.Key.StoreIngestedTabularFilesWithVarHeaders, false);
    }

    /**
     * RateLimitUtil will parse the json to create a List<RateLimitSetting>
     */
    public String getRateLimitsJson() {
        return settingsService.getValueForKey(SettingsServiceBean.Key.RateLimitingCapacityByTierAndAction, "");
    }
    public String getRateLimitingDefaultCapacityTiers() {
        return settingsService.getValueForKey(SettingsServiceBean.Key.RateLimitingDefaultCapacityTiers, "");
    }
}
