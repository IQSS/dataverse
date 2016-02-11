package edu.harvard.iq.dataverse.util;

import com.ocpsoft.pretty.PrettyContext;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;

/**
 * System-wide configuration
 */
@Stateless
@Named
public class SystemConfig {

    private static final Logger logger = Logger.getLogger(SystemConfig.class.getCanonicalName());

    @EJB
    SettingsServiceBean settingsService;

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

    /**
     * @todo Reconcile with getApplicationVersion on DataverseServiceBean.java
     * which we'd like to move to this class.
     */
    private static String appVersionString = null; 
    private static String buildNumberString = null; 
    
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
        String solrHostColonPort = settingsService.getValueForKey(SettingsServiceBean.Key.SolrHostColonPort, saneDefaultForSolrHostColonPort);
        return solrHostColonPort;
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
            return limit.intValue();
        }
        
        return defaultZipUploadFilesLimit; 
    }

    // TODO: (?)
    // create sensible defaults for these things? -- 4.2.2
    public long getThumbnailSizeLimitImage() {
        long limit = getThumbnailSizeLimit("Image");
        return limit == 0 ? 5000000 : limit;
    } 
    
    public long getThumbnailSizeLimitPDF() {
        long limit = getThumbnailSizeLimit("PDF");
        return limit == 0 ? 500000 : limit;
    }
    
    public long getThumbnailSizeLimit(String type) {
        String option = null; 
        if ("Image".equals(type)) {
            option = settingsService.getValueForKey(SettingsServiceBean.Key.ThumbnailSizeLimitImage);
            option = System.getProperty("dataverse.dataAccess.thumbnail.image.limit");
        } else if ("PDF".equals(type)) {
            option = settingsService.getValueForKey(SettingsServiceBean.Key.ThumbnailSizeLimitPDF);
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
        String saneDefaultForAppTermsOfUse = "There are no Terms of Use for this Dataverse installation.";
        String appTermsOfUse = settingsService.getValueForKey(SettingsServiceBean.Key.ApplicationTermsOfUse, saneDefaultForAppTermsOfUse);
        return appTermsOfUse;
    }

    public String getApiTermsOfUse() {
        String saneDefaultForApiTermsOfUse = "There are no API Terms of Use for this Dataverse installation.";
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

    public boolean isDdiExportEnabled() {
        boolean safeDefaultIfKeyNotFound = false;
        return settingsService.isTrueForKey(SettingsServiceBean.Key.DdiExportEnabled, safeDefaultIfKeyNotFound);
    }

    public boolean isShibEnabled() {
        boolean safeDefaultIfKeyNotFound = false;
        return settingsService.isTrueForKey(SettingsServiceBean.Key.ShibEnabled, safeDefaultIfKeyNotFound);
    }

    public boolean isShibUseHeaders() {
        boolean safeDefaultIfKeyNotFound = false;
        return settingsService.isTrueForKey(SettingsServiceBean.Key.ShibUseHeaders, safeDefaultIfKeyNotFound);
    }

    // TODO: 
    // remove these method! 
    // pages should be using settingsWrapper.isTrueForKey(":Debug", false) instead. -- 4.2.1
    public boolean isDebugEnabled() {
        boolean safeDefaultIfKeyNotFound = false;
        return settingsService.isTrueForKey(SettingsServiceBean.Key.Debug, safeDefaultIfKeyNotFound);
    }

    public boolean myDataDoesNotUsePermissionDocs() {
        boolean safeDefaultIfKeyNotFound = false;
        return settingsService.isTrueForKey(SettingsServiceBean.Key.MyDataDoesNotUseSolrPermissionDocs, safeDefaultIfKeyNotFound);
    }

    public boolean isFilesOnDatasetPageFromSolr() {
        boolean safeDefaultIfKeyNotFound = false;
        return settingsService.isTrueForKey(SettingsServiceBean.Key.FilesOnDatasetPageFromSolr, safeDefaultIfKeyNotFound);
    }
    
    public boolean isFileLandingPageAvailable() {
        boolean safeDefaultIfKeyNotFound = false;
        return settingsService.isTrueForKey(SettingsServiceBean.Key.ShowFileLandingPage, safeDefaultIfKeyNotFound);
    }

    public Long getMaxFileUploadSize(){

         return settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.MaxFileUploadSizeInBytes);
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

}
