package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * System-wide configuration
 */
@Stateless
@Named
public class SystemConfig {

    private static final Logger logger = Logger.getLogger(SystemConfig.class.getCanonicalName());

    @EJB
    SettingsServiceBean settingsService;

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
                    mavenPropertiesFilePath = filePath.concat("../../../META-INF/maven/edu.harvard.iq/dataverse-webapp/pom.properties");

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

    public int getMinutesUntilPasswordResetTokenExpires() {
        return settingsService.getValueForKeyAsInt(SettingsServiceBean.Key.MinutesUntilPasswordResetTokenExpires);
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
        String guidesBaseUrl = settingsService.getValueForKey(SettingsServiceBean.Key.GuidesBaseUrl);
        return guidesBaseUrl + "/en";
    }

    public String getGuidesVersion() {
        String guidesVersion = settingsService.getValueForKey(SettingsServiceBean.Key.GuidesVersion);

        return guidesVersion.equals(StringUtils.EMPTY) ? getVersion() : guidesVersion;
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
        String appTermsOfUse = settingsService.getValueForKey(SettingsServiceBean.Key.ApplicationTermsOfUse);

        return appTermsOfUse.equals(StringUtils.EMPTY) ? BundleUtil.getStringFromBundle("system.app.terms") : appTermsOfUse;
    }

    public String getApiTermsOfUse() {
        String apiTermsOfUse = settingsService.getValueForKey(SettingsServiceBean.Key.ApiTermsOfUse);

        return apiTermsOfUse.equals(StringUtils.EMPTY) ? BundleUtil.getStringFromBundle("system.api.terms") : apiTermsOfUse;
    }

    public long getTabularIngestSizeLimit() {
        // This method will return the blanket ingestable size limit, if 
        // set on the system. I.e., the universal limit that applies to all 
        // tabular ingests, regardless of fromat: 
        return settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.TabularIngestSizeLimit);
    }

    public long getTabularIngestSizeLimit(String formatName) {
        // This method returns the size limit set specifically for this format name,
        // if available, otherwise - the blanket limit that applies to all tabular 
        // ingests regardless of a format. 

        if (StringUtils.isEmpty(formatName)) {
            return getTabularIngestSizeLimit();
        }

        String limitEntry = settingsService.get(SettingsServiceBean.Key.TabularIngestSizeLimit.toString() + ":" + formatName);

        if (StringUtils.isNotEmpty(limitEntry)) {
            try {
                Long sizeOption = new Long(limitEntry);
                return sizeOption;
            } catch (NumberFormatException nfe) {
                logger.warning("Invalid value for TabularIngestSizeLimit:" + formatName + "? - " + limitEntry );
            }
        }

        return getTabularIngestSizeLimit();
    }

    public boolean isTimerServer() {
        return settingsService.isTrueForKey(SettingsServiceBean.Key.TimerServer);
    }

//    public String getNameOfInstallation() {
//        return dataverseService.findRootDataverse().getName();
//    }

    public AbstractOAuth2AuthenticationProvider.DevOAuthAccountType getDevOAuthAccountType() {
        AbstractOAuth2AuthenticationProvider.DevOAuthAccountType saneDefault = AbstractOAuth2AuthenticationProvider.DevOAuthAccountType.PRODUCTION;
        String settingReturned = settingsService.getValueForKey(SettingsServiceBean.Key.DebugOAuthAccountType);
        logger.fine("setting returned: " + settingReturned);
        if (StringUtils.isNotEmpty(settingReturned)) {
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
        if (StringUtils.isNotEmpty(settingReturned)) {
            return settingReturned;
        }
        return saneDefault;
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

    public boolean isRsyncUpload(){
        return getUploadMethodAvailable(SystemConfig.FileUploadMethods.RSYNC.toString());
    }

    // Controls if HTTP upload is enabled for both GUI and API.
    public boolean isHTTPUpload() {
        return getUploadMethodAvailable(SystemConfig.FileUploadMethods.NATIVE.toString());
    }

    public boolean isRsyncOnly() {
        String downloadMethods = settingsService.getValueForKey(SettingsServiceBean.Key.DownloadMethods);
        if(StringUtils.isEmpty(downloadMethods)){
            return false;
        }
        if (!downloadMethods.toLowerCase().equals(SystemConfig.FileDownloadMethods.RSYNC.toString())){
            return false;
        }
        String uploadMethods = settingsService.getValueForKey(SettingsServiceBean.Key.UploadMethods);
        if (StringUtils.isEmpty(uploadMethods)){
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
        if (StringUtils.isEmpty(uploadMethods)){
            return false;
        } else {
           return  Arrays.asList(uploadMethods.toLowerCase().split("\\s*,\\s*")).contains(method);
        }
    }

    public Integer getUploadMethodCount(){
        String uploadMethods = settingsService.getValueForKey(SettingsServiceBean.Key.UploadMethods);
        if (StringUtils.isEmpty(uploadMethods)){
            return 0;
        } else {
           return  Arrays.asList(uploadMethods.toLowerCase().split("\\s*,\\s*")).size();
        }
    }
}
