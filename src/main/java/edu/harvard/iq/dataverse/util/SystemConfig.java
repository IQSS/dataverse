package edu.harvard.iq.dataverse.util;

import com.ocpsoft.pretty.PrettyContext;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import static edu.harvard.iq.dataverse.datasetutility.FileSizeChecker.bytesToHumanReadable;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.validation.PasswordValidatorUtil;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Year;
import java.util.Arrays;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import org.passay.CharacterRule;
import org.apache.commons.io.IOUtils;

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
     * A JVM option for the advertised fully qualified domain name (hostname) of
     * the Dataverse installation, such as "dataverse.example.com", which may
     * differ from the hostname that the server knows itself as.
     *
     * The equivalent in DVN 3.x was "dvn.inetAddress".
     */
    public static final String FQDN = "dataverse.fqdn";
    
    /**
     * A JVM option for specifying the "official" URL of the site. 
     * Unlike the FQDN option above, this would be a complete URL, 
     * with the protocol, port number etc. 
     */
    public static final String SITE_URL = "dataverse.siteUrl";

    /**
     * A JVM option for where files are stored on the file system.
     */
    public static final String FILES_DIRECTORY = "dataverse.files.directory";

    /**
     * Some installations may not want download URLs to their files to be
     * available in Schema.org JSON-LD output.
     */
    public static final String FILES_HIDE_SCHEMA_DOT_ORG_DOWNLOAD_URLS = "dataverse.files.hide-schema-dot-org-download-urls";

    /**
     * A JVM option to override the number of minutes for which a password reset
     * token is valid ({@link #minutesUntilPasswordResetTokenExpires}).
     */
    private static final String PASSWORD_RESET_TIMEOUT_IN_MINUTES = "dataverse.auth.password-reset-timeout-in-minutes";

    /**
     * A common place to find the String for a sane Solr hostname:port
     * combination.
     */
    private String saneDefaultForSolrHostColonPort = "localhost:8983";

    /**
     * The default number of datafiles that we allow to be created through 
     * zip file upload.
     */
    private static final int defaultZipUploadFilesLimit = 1000; 
    private static final int defaultMultipleUploadFilesLimit = 1000;

    private static String appVersionString = null; 
    private static String buildNumberString = null; 
    
    private static final String JVM_TIMER_SERVER_OPTION = "dataverse.timerServer";
    
    private static final long DEFAULT_GUESTBOOK_RESPONSES_DISPLAY_LIMIT = 5000L; 
    
    public String getVersion() {
        return getVersion(false);
    }
    
    public String getVersion(boolean withBuildNumber) {
        
        if (appVersionString == null) {

            // The Version Number is no longer supplied in a .properties file - so
            // we can't just do 
            //  return BundleUtil.getStringFromBundle("version.number", null, ResourceBundle.getBundle("VersionNumber", Locale.US));
            //
            // Instead, we'll rely on Maven placing the version number into the
            // Manifest, and getting it from there:
            // (this is considered a better practice, and will also allow us
            // to maintain this number in only one place - the pom.xml file)
            // -- L.A. 4.0.2
            
            // One would assume, that once the version is in the MANIFEST.MF, 
            // as Implementation-Version:, it would be possible to obtain 
            // said version simply as 
            //    appVersionString = getClass().getPackage().getImplementationVersion();
            // alas - that's not working, for whatever reason. (perhaps that's 
            // only how it works with jar-ed packages; not with .war files).
            // People on the interwebs suggest that one should instead 
            // open the Manifest as a resource, then extract its attributes. 
            // There were some complications with that too. Plus, relying solely 
            // on the MANIFEST.MF would NOT work for those of the developers who 
            // are using "in place deployment" (i.e., where 
            // Netbeans runs their builds directly from the local target 
            // directory, bypassing the war file deployment; and the Manifest 
            // is only available in the .war file). For that reason, I am 
            // going to rely on the pom.properties file, and use java.util.Properties 
            // to read it. We have to look for this file in 2 different places
            // depending on whether this is a .war file deployment, or a 
            // developers build. (the app-level META-INF is only populated when
            // a .war file is built; the "maven-archiver" directory, on the other 
            // hand, is only available when it's a local build deployment).
            // So, long story short, I'm resorting to the convoluted steps below. 
            // It may look hacky, but it should actually be pretty solid and 
            // reliable. 
            
            
            // First, find the absolute path url of the application persistence file
            // always supplied with the Dataverse app:
            java.net.URL fileUrl = Thread.currentThread().getContextClassLoader().getResource("META-INF/persistence.xml");
            String filePath = null;


            if (fileUrl != null) {
                filePath = fileUrl.getFile();
                if (filePath != null) {
                    InputStream mavenPropertiesInputStream = null;
                    String mavenPropertiesFilePath; 
                    Properties mavenProperties = new Properties();

                    
                    filePath = filePath.replaceFirst("/[^/]*$", "/");
                    // Using a relative path, find the location of the maven pom.properties file. 
                    // First, try to look for it in the app-level META-INF. This will only be 
                    // available if it's a war file deployment: 
                    mavenPropertiesFilePath = filePath.concat("../../../META-INF/maven/edu.harvard.iq/dataverse/pom.properties");                                     
                    
                    try {
                        mavenPropertiesInputStream = new FileInputStream(mavenPropertiesFilePath);
                    } catch (IOException ioex) {
                        // OK, let's hope this is a local dev. build. 
                        // In that case the properties file should be available in 
                        // the maven-archiver directory: 
                        
                        mavenPropertiesFilePath = filePath.concat("../../../../maven-archiver/pom.properties");
                        
                        // try again: 
                        
                        try {
                            mavenPropertiesInputStream = new FileInputStream(mavenPropertiesFilePath);
                        } catch (IOException ioex2) {
                            logger.warning("Failed to find and/or open for reading the pom.properties file.");
                            mavenPropertiesInputStream = null; 
                        }
                    }
                    
                    if (mavenPropertiesInputStream != null) {
                        try {
                            mavenProperties.load(mavenPropertiesInputStream);
                            appVersionString = mavenProperties.getProperty("version");                        
                        } catch (IOException ioex) {
                            logger.warning("caught IOException trying to read and parse the pom properties file.");
                        } finally {
                            IOUtils.closeQuietly(mavenPropertiesInputStream);
                        }
                    }
                    
                } else {
                    logger.warning("Null file path representation of the location of persistence.xml in the webapp root directory!"); 
                }
            } else {
                logger.warning("Could not find the location of persistence.xml in the webapp root directory!");
            }

            
            if (appVersionString == null) {
                // still null? - defaulting to 4.0:    
                appVersionString = "4.0";
            }
        }
            
        if (withBuildNumber) {
            if (buildNumberString == null) {
                // (build number is still in a .properties file in the source tree; it only 
                // contains a real build number if this war file was built by 
                // Jenkins) 
                        
                try {
                    buildNumberString = ResourceBundle.getBundle("BuildNumber").getString("build.number");
                } catch (MissingResourceException ex) {
                    buildNumberString = null; 
                }
            }
            
            if (buildNumberString != null && !buildNumberString.equals("")) {
                return appVersionString + " build " + buildNumberString; 
            } 
        }        
        
        return appVersionString; 
    }

    public String getSolrHostColonPort() {
        String SolrHost;
        if ( System.getenv("SOLR_SERVICE_HOST") != null && System.getenv("SOLR_SERVICE_HOST") != ""){
            SolrHost = System.getenv("SOLR_SERVICE_HOST");
        }
        else SolrHost = saneDefaultForSolrHostColonPort;
        String solrHostColonPort = settingsService.getValueForKey(SettingsServiceBean.Key.SolrHostColonPort, SolrHost);
        return solrHostColonPort;
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
     * The "official", designated URL of the site;
     * can be defined as a complete URL; or derived from the 
     * "official" hostname. If none of these options is set,
     * defaults to the InetAddress.getLocalHOst() and https;
     * These are legacy JVM options. Will be eventualy replaced
     * by the Settings Service configuration.
     */
    public String getDataverseSiteUrl() {
        return getDataverseSiteUrlStatic();
    }
    
    public static String getDataverseSiteUrlStatic() {
        String hostUrl = System.getProperty(SITE_URL);
        if (hostUrl != null && !"".equals(hostUrl)) {
            return hostUrl;
        }
        String hostName = System.getProperty(FQDN);
        if (hostName == null) {
            try {
                hostName = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (UnknownHostException e) {
                return null;
            }
        }
        hostUrl = "https://" + hostName;
        return hostUrl;
    }
    
    /**
     * URL Tracking: 
     */
    public String getPageURLWithQueryString() {
        return PrettyContext.getCurrentInstance().getRequestURL().toURL() + PrettyContext.getCurrentInstance().getRequestQueryString().toQueryString();
    }

    /**
     * The "official" server's fully-qualified domain name: 
     */
    public String getDataverseServer() {
        // still reliese on a JVM option: 
        String fqdn = System.getProperty(FQDN);
        if (fqdn == null) {
            try {
                fqdn = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (UnknownHostException e) {
                return null;
            }
        }
        return fqdn;
    }

    public String getGuidesBaseUrl() {
        String saneDefault = "http://guides.dataverse.org";
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

    /**
     * Download-as-zip size limit.
     * returns 0 if not specified; 
     * (the file zipper will then use the default value)
     * set to -1 to disable zip downloads. 
     */
    
    public long getZipDownloadLimit() {
        String zipLimitOption = settingsService.getValueForKey(SettingsServiceBean.Key.ZipDownloadLimit);   
        
        Long zipLimit = null; 
        if (zipLimitOption != null && !zipLimitOption.equals("")) {
            try {
                zipLimit = new Long(zipLimitOption);
            } catch (NumberFormatException nfe) {
                zipLimit = null; 
            }
        }
        
        if (zipLimit != null) {
            return zipLimit.longValue();
        }
        
        return 0L; 
    }
    
    public int getZipUploadFilesLimit() {
        String limitOption = settingsService.getValueForKey(SettingsServiceBean.Key.ZipUploadFilesLimit);
        Integer limit = null; 
        
        if (limitOption != null && !limitOption.equals("")) {
            try {
                limit = new Integer(limitOption);
            } catch (NumberFormatException nfe) {
                limit = null; 
            }
        }
        
        if (limit != null) {
            return limit;
        }
        
        return defaultZipUploadFilesLimit; 
    }
    
    /*
    `   the number of files the GUI user is allowed to upload in one batch, 
        via drag-and-drop, or through the file select dialog
    */
    public int getMultipleUploadFilesLimit() {
        String limitOption = settingsService.getValueForKey(SettingsServiceBean.Key.MultipleUploadFilesLimit);
        Integer limit = null; 
        
        if (limitOption != null && !limitOption.equals("")) {
            try {
                limit = new Integer(limitOption);
            } catch (NumberFormatException nfe) {
                limit = null; 
            }
        }
        
        if (limit != null) {
            return limit;
        }
        
        return defaultMultipleUploadFilesLimit; 
    }
    
    public long getGuestbookResponsesPageDisplayLimit() {
        String limitSetting = settingsService.getValueForKey(SettingsServiceBean.Key.GuestbookResponsesPageDisplayLimit);   
        
        Long limit = null; 
        if (limitSetting != null && !limitSetting.equals("")) {
            try {
                limit = new Long(limitSetting);
            } catch (NumberFormatException nfe) {
                limit = null; 
            }
        }
        
        if (limit != null) {
            return limit.longValue();
        }
        
        return DEFAULT_GUESTBOOK_RESPONSES_DISPLAY_LIMIT; 
    }
    
    public long getUploadLogoSizeLimit(){
        return 500000;
    }

    // TODO: (?)
    // create sensible defaults for these things? -- 4.2.2
    public long getThumbnailSizeLimitImage() {
        long limit = getThumbnailSizeLimit("Image");
        return limit == 0 ? 500000 : limit;
    } 
    
    public long getThumbnailSizeLimitPDF() {
        long limit = getThumbnailSizeLimit("PDF");
        return limit == 0 ? 500000 : limit;
    }
    
    public long getThumbnailSizeLimit(String type) {
        String option = null; 
        
        //get options via jvm options
        
        if ("Image".equals(type)) {
            option = System.getProperty("dataverse.dataAccess.thumbnail.image.limit");
        } else if ("PDF".equals(type)) {
            option = System.getProperty("dataverse.dataAccess.thumbnail.pdf.limit");
        }
        Long limit = null; 
        
        if (option != null && !option.equals("")) {
            try {
                limit = new Long(option);
            } catch (NumberFormatException nfe) {
                limit = null; 
            }
        }
        
        if (limit != null) {
            return limit.longValue();
        }
        
        return 0l;
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
        String saneDefaultForAppTermsOfUse = BundleUtil.getStringFromBundle("system.app.terms");
        String appTermsOfUse = settingsService.getValueForKey(SettingsServiceBean.Key.ApplicationTermsOfUse, saneDefaultForAppTermsOfUse);
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

    public Long getMaxFileUploadSize(){
         return settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.MaxFileUploadSizeInBytes);
     }
    
    public String getHumanMaxFileUploadSize(){
         return bytesToHumanReadable(getMaxFileUploadSize());
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
        return dataverseService.findRootDataverse().getName();
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
        NATIVE("native/http");


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
     * TODO: Consider if dataverse.files.s3-download-redirect belongs here since
     * it's a way to bypass Glassfish when downloading.
     */
    public enum FileDownloadMethods {
        /**
         * RSAL stands for Repository Storage Abstraction Layer. Downloads don't
         * go through Glassfish.
         */
        RSYNC("rsal/rsync"),
        NATIVE("native/http");
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
        return getUploadMethodAvailable(SystemConfig.FileUploadMethods.RSYNC.toString());
    }
    
    // Controls if HTTP upload is enabled for both GUI and API.
    public boolean isHTTPUpload(){       
        return getUploadMethodAvailable(SystemConfig.FileUploadMethods.NATIVE.toString());       
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
        String downloadMethods = settingsService.getValueForKey(SettingsServiceBean.Key.DownloadMethods);
        return downloadMethods !=null && downloadMethods.toLowerCase().contains(SystemConfig.FileDownloadMethods.RSYNC.toString());
    }
    
    public boolean isHTTPDownload() {
        String downloadMethods = settingsService.getValueForKey(SettingsServiceBean.Key.DownloadMethods);
        logger.warning("Download Methods:" + downloadMethods);
        return downloadMethods !=null && downloadMethods.toLowerCase().contains(SystemConfig.FileDownloadMethods.NATIVE.toString());
    }
    
    private Boolean getUploadMethodAvailable(String method){
        String uploadMethods = settingsService.getValueForKey(SettingsServiceBean.Key.UploadMethods); 
        if (uploadMethods==null){
            return false;
        } else {
           return  Arrays.asList(uploadMethods.toLowerCase().split("\\s*,\\s*")).contains(method);
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
    public boolean isDataFilePIDSequentialDependent(){
        String doiIdentifierType = settingsService.getValueForKey(SettingsServiceBean.Key.IdentifierGenerationStyle, "randomString");
        String doiDataFileFormat = settingsService.getValueForKey(SettingsServiceBean.Key.DataFilePIDFormat, "DEPENDENT");
        if (doiIdentifierType.equals("sequentialNumber") && doiDataFileFormat.equals("DEPENDENT")){
            return true;
        }
        return false;
    }
    
    public int getPIDAsynchRegFileCount() {
        String fileCount = settingsService.getValueForKey(SettingsServiceBean.Key.PIDAsynchRegFileCount, "10");
        int retVal = 10;
        try {
            retVal = Integer.parseInt(fileCount);
        } catch (NumberFormatException e) {           
            //if no number in the setting we'll return 10
        }
        return retVal;
    }
    
    public boolean isFilePIDsEnabled() {
        boolean safeDefaultIfKeyNotFound = true;
        return settingsService.isTrueForKey(SettingsServiceBean.Key.FilePIDsEnabled, safeDefaultIfKeyNotFound);
    }
    
}
