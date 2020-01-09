package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
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

    private static final String VERSION_PROPERTIES_CLASSPATH = "/config/version.properties";
    private static final String VERSION_PROPERTIES_KEY = "dataverse.version";
    private static final String VERSION_PLACEHOLDER = "${project.version}";
    private static final String VERSION_FALLBACK = "4.0";

    public static final String DATAVERSE_PATH = "/dataverse/";

    /**
     * A JVM option for where files are stored on the file system.
     */
    public static final String FILES_DIRECTORY = "dataverse.files.directory";


    @EJB
    private SettingsServiceBean settingsService;


    private static String appVersionString = null;
    private static String buildNumberString = null;

    public String getVersion() {
        return getVersion(false);
    }

    public String getVersion(boolean withBuildNumber) {

        if (appVersionString == null) {

            // We'll rely on Maven placing the version number into the
            // version.properties file using resource filtering
            
            Try<String> appVersionTry = Try.withResources(() -> getClass().getResourceAsStream(VERSION_PROPERTIES_CLASSPATH))
                    .of(is -> {
                        Properties properties = new Properties();
                        properties.load(is);
                        return properties;
                    })
                    .map(p -> p.getProperty(VERSION_PROPERTIES_KEY));
            
            if (appVersionTry.isFailure()) {
                appVersionString = VERSION_FALLBACK;
                logger.warning("Failed to read the " + VERSION_PROPERTIES_CLASSPATH + " file");
                
            } else if (StringUtils.equals(appVersionTry.get(), VERSION_PLACEHOLDER)) {
                appVersionString = VERSION_FALLBACK;
                logger.warning(VERSION_PROPERTIES_CLASSPATH + " was not filtered by maven (check your pom.xml configuration)");
                
            } else {
                appVersionString = appVersionTry.get();
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

    public String getDataverseSiteUrl() {
        return settingsService.getValueForKey(SettingsServiceBean.Key.SiteUrl);
    }

    public String getFilesDirectory() {
        String filesDirectory = System.getProperty(SystemConfig.FILES_DIRECTORY);
        if(StringUtils.isEmpty(filesDirectory)) {
            filesDirectory = "/tmp/files";
        }
        return filesDirectory;
    }

    public static String getFilesDirectoryStatic() {
        String filesDirectory = System.getProperty(SystemConfig.FILES_DIRECTORY);
        if(StringUtils.isEmpty(filesDirectory)) {
            filesDirectory = "/tmp/files";
        }
        return filesDirectory;
    }
    /**
     * The "official" server's fully-qualified domain name:
     */
    public String getDataverseServer() {
        try {
            return new URL(settingsService.getValueForKey(SettingsServiceBean.Key.SiteUrl)).getHost();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return "localhost";
        }
    }

    public String getGuidesBaseUrl() {
        String guidesBaseUrl = settingsService.getValueForKey(SettingsServiceBean.Key.GuidesBaseUrl);
        return guidesBaseUrl + "/en";
    }

    public String getGuidesVersion() {
        String guidesVersion = settingsService.getValueForKey(SettingsServiceBean.Key.GuidesVersion);

        return guidesVersion.equals(StringUtils.EMPTY) ? getVersion() : guidesVersion;
    }
    
    public boolean isRserveConfigured() {
        return settingsService.isTrueForKey(SettingsServiceBean.Key.RserveConfigured);
    }

    public long getUploadLogoSizeLimit() {
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
                logger.warning("Invalid value for TabularIngestSizeLimit:" + formatName + "? - " + limitEntry);
            }
        }

        return getTabularIngestSizeLimit();
    }

    public boolean isTimerServer() {
        return settingsService.isTrueForKey(SettingsServiceBean.Key.TimerServer);
    }

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
     * <p>
     * - FileUploadMethods
     * <p>
     * - FileDownloadMethods
     * <p>
     * - TransferProtocols
     * <p>
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

        FileUploadMethods(final String text) {
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
     * <p>
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

        FileDownloadMethods(final String text) {
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

        DataFilePIDFormat(final String text) {
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

        TransferProtocols(final String text) {
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

    public boolean isRsyncUpload() {
        return getUploadMethodAvailable(SystemConfig.FileUploadMethods.RSYNC.toString());
    }

    // Controls if HTTP upload is enabled for both GUI and API.
    public boolean isHTTPUpload() {
        return getUploadMethodAvailable(SystemConfig.FileUploadMethods.NATIVE.toString());
    }

    public boolean isRsyncOnly() {
        String downloadMethods = settingsService.getValueForKey(SettingsServiceBean.Key.DownloadMethods);
        if (StringUtils.isEmpty(downloadMethods)) {
            return false;
        }
        if (!downloadMethods.toLowerCase().equals(SystemConfig.FileDownloadMethods.RSYNC.toString())) {
            return false;
        }
        String uploadMethods = settingsService.getValueForKey(SettingsServiceBean.Key.UploadMethods);
        if (StringUtils.isEmpty(uploadMethods)) {
            return false;
        } else {
            return Arrays.asList(uploadMethods.toLowerCase().split("\\s*,\\s*")).size() == 1 && uploadMethods.toLowerCase().equals(SystemConfig.FileUploadMethods.RSYNC.toString());
        }
    }

    public boolean isRsyncDownload() {
        String downloadMethods = settingsService.getValueForKey(SettingsServiceBean.Key.DownloadMethods);
        return downloadMethods != null && downloadMethods.toLowerCase().contains(SystemConfig.FileDownloadMethods.RSYNC.toString());
    }

    public boolean isHTTPDownload() {
        String downloadMethods = settingsService.getValueForKey(SettingsServiceBean.Key.DownloadMethods);
        logger.fine("Download Methods:" + downloadMethods);
        return downloadMethods != null && downloadMethods.toLowerCase().contains(SystemConfig.FileDownloadMethods.NATIVE.toString());
    }

    private Boolean getUploadMethodAvailable(String method) {
        String uploadMethods = settingsService.getValueForKey(SettingsServiceBean.Key.UploadMethods);
        if (StringUtils.isEmpty(uploadMethods)) {
            return false;
        } else {
            return Arrays.asList(uploadMethods.toLowerCase().split("\\s*,\\s*")).contains(method);
        }
    }

    public Integer getUploadMethodCount() {
        String uploadMethods = settingsService.getValueForKey(SettingsServiceBean.Key.UploadMethods);
        if (StringUtils.isEmpty(uploadMethods)) {
            return 0;
        } else {
            return Arrays.asList(uploadMethods.toLowerCase().split("\\s*,\\s*")).size();
        }
    }

    public Map<String, String> getConfiguredLocales() {
        Map<String, String> configuredLocales = new LinkedHashMap<>();

        JSONArray entries = new JSONArray(settingsService.getValueForKey(SettingsServiceBean.Key.Languages));
        for (Object obj : entries) {
            JSONObject entry = (JSONObject) obj;
            String locale = entry.getString("locale");
            String title = entry.getString("title");

            configuredLocales.put(locale, title);
        }

        return configuredLocales;
    }


}
