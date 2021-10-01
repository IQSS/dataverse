package edu.harvard.iq.dataverse.settings;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.persistence.ActionLogRecord;
import edu.harvard.iq.dataverse.persistence.Setting;
import edu.harvard.iq.dataverse.persistence.SettingDao;
import edu.harvard.iq.dataverse.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * Service bean accessing and manipulating application settings.
 * Settings are resolved from database and property files (db ones takes precedence).
 *
 * @author michael
 * @see FileBasedSettingsFetcher
 */
@ApplicationScoped
public class SettingsServiceBean {

    private static final String KEY_AND_POSTFIX_SEPARATOR = ".";

    /**
     * Some convenient keys for the settings. Note that the setting's
     * name is really a {@code String}, but it's good to have the compiler look
     * over your shoulder when typing strings in various places of a large app.
     * So there.
     */
    public enum Key {
        AllowApiTokenLookupViaApi,
        /**
         * Ordered, comma-separated list of custom fields to show above the fold
         * on dataset page such as "data_type,sample,pdb"
         */
        CustomDatasetSummaryFields,
        /**
         * Defines a public installation -- all datafiles are unrestricted
         */
        PublicInstall,
        /**
         * Enables the provenance collection popup.
         * Allows users to store their provenance json and description
         */
        ProvCollectionEnabled,
        /**
         * For example, https://datacapture.example.org
         */
        DataCaptureModuleUrl,
        RepositoryStorageAbstractionLayerUrl,
        UploadMethods,
        DownloadMethods,
        /**
         * If the data replicated around the world using RSAL (Repository
         * Storage Abstraction Layer) is locally available, this is its file
         * path, such as "/programs/datagrid".
         * <p>
         * TODO: Think about if it makes sense to make this a column in the
         * StorageSite database table.
         */
        LocalDataAccessPath,
        IdentifierGenerationStyle,
        OAuth2CallbackUrl,
        DefaultAuthProvider,
        FooterCopyright,
        FileFixityChecksumAlgorithm,
        MinutesUntilConfirmEmailTokenExpires,
        /**
         * Override Solr highlighting "fragsize"
         * https://wiki.apache.org/solr/HighlightingParameters#hl.fragsize
         */
        SearchHighlightFragmentSize,
        /**
         * Experimental: Allow non-public search with a key/token using the
         * Search API. See also https://github.com/IQSS/dataverse/issues/1299
         */
        SearchApiNonPublicAllowed,
        /**
         * In Dataverse 4.7 and earlier, an API token was required to use the
         * Search API. Tokens are no longer required but you can revert to the
         * old behavior by setting this to false.
         */
        SearchApiRequiresToken,

        /**
         * API endpoints that are not accessible. Comma separated list.
         */
        BlockedApiEndpoints,

        /**
         * A key that, with the right {@link ApiBlockingFilter.BlockPolicy},
         * allows calling blocked APIs.
         */
        BlockedApiKey,


        /**
         * How to treat blocked APIs. One of drop, localhost-only, unblock-key
         */
        BlockedApiPolicy,

        /**
         * For development only (see dev guide for details). Backed by an enum
         * of possible account types.
         */
        DebugShibAccountType,
        DebugOAuthAccountType,
        /**
         * Application-wide Terms of Use per installation.
         * Setting can be postfixed with language code to
         * obtain translated versions of terms of use.
         * It is assumed that not postfixed setting is
         * the default one (used in case if language specific
         * version is not present).
         */
        ApplicationTermsOfUse,
        /**
         * Terms of Use specific to API per installation.
         */
        ApiTermsOfUse,
        /**
         * URL for the application-wide Privacy Policy per installation, linked
         * to from the footer.
         */
        ApplicationPrivacyPolicyUrl,
        /**
         * Solr hostname and port, such as "localhost:8983".
         */
        SolrHostColonPort,
        /**
         * Enable full-text indexing in solr
         * Defaults to false
         **/
        SolrFullTextIndexing,
        /**
         * If file size of indexed file is greater than this value
         * then full text indexing will not take place
         * If set to 0 then no limit
         * Defaults to 0
         */
        SolrMaxFileSizeForFullTextIndexing, //
        /**
         * Key for limiting the number of bytes uploaded via the Data Deposit API, UI
         * If not set then not limit
         **/
        MaxFileUploadSizeInBytes,
        /**
         * Key for if ScrubMigrationData is enabled or disabled.
         */
        ScrubMigrationData,
        /**
         * Key for the url to send users who want to sign up to.
         */
        SignUpUrl,
        /**
         * Key for whether we allow users to sign up
         */
        AllowSignUp,
        /**
         * protocol for global id
         */
        Protocol,
        /**
         * authority for global id
         */
        Authority,
        /**
         * DoiProvider for global id
         */
        DoiProvider,
        /**
         * Period in ms after which the service will try to reserve yet unreserved DOI's
         */
        DoiBackgroundReservationInterval,
        /**
         * Shoulder for global id - used to create a common prefix on identifiers
         */
        Shoulder,

        /**
         * Optionally override http://guides.dataverse.org .
         */
        GuidesBaseUrl,

        /**
         * A link to an installation of https://github.com/IQSS/miniverse or
         * some other metrics app.
         */
        MetricsUrl,

        /**
         * Number of minutes before a metrics query can be rerun. Otherwise a cached value is returned.
         * Previous month dates always return cache. Only applies to new internal caching system (not miniverse).
         */
        MetricsCacheTimeoutMinutes,
        /* zip download size limit */
        /**
         * Optionally override version number in guides.
         */
        GuidesVersion,
        /**
         * Download-as-zip size limit.
         * If set to 0 then no limit.
         * If set to -1 then zip downloads are disabled
         */
        ZipDownloadLimit,
        /**
         * Number of datafiles that we allow to be created through
         * zip file upload.
         */
        ZipUploadFilesLimit,
        /**
         * the number of files the GUI user is allowed to upload in one batch,
         * via drag-and-drop, or through the file select dialog
         */
        MultipleUploadFilesLimit,

        /**
         * status message that will appear on the home page
         */
        StatusMessageHeader,
        /**
         * full text of status message, to appear in popup
         */
        StatusMessageText,
        /**
         * return email address for system emails such as notifications
         */
        SystemEmail,
        /**
         * size limit for Tabular data file ingests <br/>
         * (can be set separately for specific ingestable formats; in which
         * case the actual stored option will be TabularIngestSizeLimit:{FORMAT_NAME}
         * where {FORMAT_NAME} is the format identification tag returned by the
         * getFormatName() method in the format-specific plugin; "sav" for the
         * SPSS/sav format, "RData" for R, etc.
         * for example: :TabularIngestSizeLimit:RData <br/>
         * -1 means no limit is set;
         * 0 on the other hand would mean that ingest is fully disabled for tabular data.
         */
        TabularIngestSizeLimit,
        /**
         * Whether to allow user to create GeoConnect Maps
         * This boolean effects whether the user sees the map button on
         * the dataset page and if the ingest will create a shape file
         * Default is false
         */
        GeoconnectCreateEditMaps,
        /**
         * Whether to allow a user to view existing maps
         * This boolean effects whether a user may see the
         * Explore World Map Button
         * Default is false;
         */
        GeoconnectViewMaps,
        /**
         * The message added to a popup upon dataset publish
         */
        DatasetPublishPopupCustomText,
        /**
         * Whether to display the publish text for every published version
         */
        DatasetPublishPopupCustomTextOnAllVersions,
        /**
         * Whether Harvesting (OAI) service is enabled
         */
        OAIServerEnabled,

        /**
         * Whether Shibboleth passive authentication mode is enabled
         */
        ShibPassiveLoginEnabled,
        /**
         * Convert Shibboleth AJP attributes from ISO-8859-1 to UTF-8
         */
        ShibAttributeCharacterSetConversionEnabled,
        /**
         * Whether Export should exclude FieldType.EMAIL
         */
        ExcludeEmailFromExport,
        /**
         * Location and name of HomePage customization file
         */
        HomePageCustomizationFile,
        /**
         * Location and name of Header customization file
         */
        HeaderCustomizationFile,
        /**
         * Location and name of Footer customization file
         */
        FooterCustomizationFile,
        /**
         * Location and name of CSS customization file (it will be used as an inline style)
         */
        StyleCustomizationFile,
        /**
         * Location and name of analytics code file
         */
        WebAnalyticsCode,
        /**
         * Location and name of installation logo customization file
         */
        LogoCustomizationFile,

        // Option to override the navbar url underlying the "About" link
        NavbarAboutUrl,

        // Option to override multiple guides with a single url
        NavbarGuidesUrl,

        // Option to overide the feedback dialog display with a link to an external page via its url
        NavbarSupportUrl,

        /**
         * The theme for the root dataverse can get in the way when you try make
         * use of HeaderCustomizationFile and LogoCustomizationFile so this is a
         * way to disable it.
         */
        DisableRootDataverseTheme,
        // Limit on how many guestbook entries to display on the guestbook-responses page:
        GuestbookResponsesPageDisplayLimit,

        /**
         * The dictionary filepaths separated by a pipe (|)
         */
        PVDictionaries,

        /**
         * The minimum length of a good, long, strong password.
         * Defaults to 20.
         */
        PVGoodStrength,

        /**
         * A password minimum length
         * Defaults to 6
         */
        PVMinLength,
        /**
         * A password maximum length
         * If set to 0 then maximum length is disabled
         */
        PVMaxLength,

        /**
         * One letter, 2 special characters, etc. (string in form Alphabetical:1,Digit:1)
         * Defaults to (string in form Alphabetical:1,Digit:1):
         * - one alphabetical
         * - one digit
         */
        PVCharacterRules,

        /**
         * The number of M characteristics.
         * Defaults to 2.
         */
        PVNumberOfCharacteristics,

        /**
         * The number of consecutive digits allowed for a password.
         * Defaults to highest int
         */
        PVNumberOfConsecutiveDigitsAllowed,
        /**
         * Configurable text for alert/info message on passwordreset.xhtml when users are required to update their password.
         */
        PVCustomPasswordResetAlertMessage,
        /**
         * String to describe DOI format for data files. Default is DEPENDENT.
         * 'DEPENEDENT' means the DOI will be the Dataset DOI plus a file DOI with a slash in between.
         * 'INDEPENDENT' means a new global id, completely independent from the dataset-level global id.
         */
        DataFilePIDFormat,
        /**
         * Json array of supported languages
         */
        Languages,
        /**
         * Number for the minimum number of files to send PID registration to asynchronous workflow
         */
        PIDAsynchRegFileCount,
        /**
         *
         */
        FilePIDsEnabled,

        /**
         * Indicates if the Handle service is setup to work 'independently' (No communication with the Global Handle Registry)
         */
        IndependentHandleService,

        /**
         * Archiving can be configured by providing an Archiver class name (class must extend AstractSubmitToArchiverCommand)
         * and a list of settings that should be passed to the Archiver.
         * Note:
         * Configuration may also require adding Archiver-specific jvm-options (i.e. for username and password) in glassfish.
         * <p>
         * To automate the submission of an archival copy step as part of publication, a post-publication workflow must also be configured.
         * <p>
         * For example:
         * ArchiverClassName - "edu.harvard.iq.dataverse.engine.command.impl.DPNSubmitToArchiveCommand"
         * ArchiverSettings - "DuraCloudHost, DuraCloudPort, DuraCloudContext"
         * <p>
         * Note: Dataverse must be configured with values for these dynamically defined settings as well, e.g.
         * <p>
         * DuraCloudHost , eg. "qdr.duracloud.org", a non-null value enables submission
         * DuraCloudPort, default is 443
         * DuraCloudContext, default is "durastore"
         */

        ArchiverClassName,
        ArchiverSettings,
        /**
         * A comma-separated list of roles for which new dataverses should inherit the
         * corresponding role assignments from the parent dataverse. Also affects
         * /api/admin/dataverse/{alias}/addRolesToChildren. Default is "", no
         * inheritance. "*" means inherit assignments for all roles
         */
        InheritParentRoleAssignments,

        /**
         * Indicates if other terms of use are active or not.
         */
        AllRightsReservedTermsOfUseActive,
        RestrictedAccessTermsOfUseActive,

        /**
         * Size limits for generating thumbnails on the fly
         * (i.e., we'll attempt to generate a thumbnail on the fly if the
         * size of the file is less than this)
         */
        ThumbnailImageSizeLimit,
        ThumbnailPDFSizeLimit,

        DropboxKey,
        DoiDataCiteCitationsPageUrl,
        DoiDataCiteRestApiUrl,
        DoiBaseUrlString,
        DoiUsername,
        DoiPassword,

        HandleNetAdmCredFile,
        HandleNetAdmPrivPhrase,
        HandleNetIndex,

        TimerServer,

        MinutesUntilPasswordResetTokenExpires,

        /**
         * Indicates if rserve is properly configured in this dataverse installation.
         * If not, then any functionality using rserve should be switched off
         */
        RserveConfigured,
        RserveHost,
        RservePort,
        RserveUser,
        RservePassword,
        RserveTempDir,

        /**
         * Some installations may not want download URLs to their files to be
         * available in Schema.org JSON-LD output.
         */
        HideSchemaDotOrgDownloadUrls,

        /**
         * A JVM option for specifying the "official" URL of the site.
         * Unlike the FQDN option above, this would be a complete URL,
         * with the protocol, port number etc.
         */
        SiteUrl,

        /**
         * Text with privacy policy content.
         * Setting can be postfixed with language code to
         * obtain translated versions of terms of use.
         * It is assumed that not postfixed setting is
         * the default one (used in case if language specific
         * version is not present).
         */
        PrivacyPolicy,

        /**
         * Text with accessibility statement.
         * Setting can be postfixed with language code to
         * obtain translated versions of the statement.
         * It is assumed that not postfixed setting is
         * the default one (used in case if language specific
         * version is not present).
         */
        AccessibilityStatement,

        /**
         * Dataverse admin can configure application-wide maximum length for embargo.
         * @value number of months
         * For any given date, maximum embargo for that date is: [date] + [MaximumEmbargoLength]
         * Example: for setting value = 3 and date 12DEC2019, effective maximum embargo date for user
         * trying to set embargo for his dataset is 12MAR2020 (3 months from [date])
         */
        MaximumEmbargoLength,

        /**
         * Application wide format for dates.
         * Use this whenever you want to print date on GUI.
         */
        DefaultDateFormat,

        /**
         * Email of mail overseer: if present, a copy of every mail sent by the application
         * will be sent to that address.
         *
         * By default it is not set, so the feature is turned off.
         */
        MailOverseerAddress,

        /**
         * Show link to Privacy Policy page in the footer (if set to 'true').
         * By default is set to 'false', so link won't be shown.
         */
        ShowPrivacyPolicyFooterLink,

        /**
         * Show link to Terms of Use page in the footer (if set to 'true').
         * By default is set to 'false', so link won't be shown.
         */
        ShowTermsOfUseFooterLink,

        /**
         * Show link to Accesibility Statement page in the footer (if set to 'true').
         * By default is set to 'false', so link won't be shown.
         */
        ShowAccessibilityStatementFooterLink,

        /**
         * Name of the site that will be presented in the header.
         * Setting can be postfixed with language code to
         * obtain translated versions.
         */
        SiteName,

        /**
         * Full name of the site that will be presented in the header
         * below {@link Key#SiteName}.
         * Setting can be postfixed with language code to
         * obtain translated versions.
         */
        SiteFullName,

        /**
         * Indicates if antivirus scanner is enabled
         */
        AntivirusScannerEnabled,
        /**
         * Maximum size of files that will be scanned
         * by antivirus
         */
        AntivirusScannerMaxFileSize,
        AntivirusScannerMaxFileSizeForExecutables,
        /**
         * Antivirus scanner engine settings
         */
        AntivirusScannerSocketAddress,
        AntivirusScannerSocketPort,
        AntivirusScannerSocketTimeout,

        FilesIntegrityCheckTimerExpression,

        /**
         * If set to true, the set of actions that could be performed by the
         * users with unconfirmed emails will be severely limited.
         */
        UnconfirmedMailRestrictionModeEnabled,

        /**
         * Command or path for external utility for RAR5 size checking
         */
        RarDataUtilCommand,
        /**
         * Additional options for rar utility
         */
        RarDataUtilOpts,
        /**
         * Character starting the line before the result (total size). The
         * line must be the last in the output that starts with the chosen
         * character. The next line has to start with result (with optional
         * spaces before).
         */
        RarDataLineBeforeResultDelimiter,

        /**
         * Sets the maximal size of gzip file (in bytes) that would be
         * subject to checking of uncompressed content size.
         */
        GzipMaxInputFileSizeInBytes,

        /**
         * Sets the maximal size of output file (in bytes) that could be
         * unpacked from gzip file in order to check its uncompressed size.
         */
        GzipMaxOutputFileSizeInBytes,

        /**
         * Cron expression that indicates how often
         * updating of citation counts should take place.
         * Note that current implementation is heavily based
         * on DataCite api. If your installation uses some
         * other global id provider then this process will
         * not work as expected.
         * If empty then updating of citation counts will be skipped.
         * Default value: empty.
         */
        CitationCountUpdateTimerExpression,

        /**
         * If true then instance will run in readonly mode.
         * Installation in readonly mode will have functionality
         * limited to operations that will not modify database and
         * storage state.
         */
        ReadonlyMode,

        /**
         * Maximal size (in bytes) of files that can be uploaded in a single
         * batch. If set to 0 then there's no limit.
         */
        SingleUploadBatchMaxSize
        ;


        @Override
        public String toString() {
            return ":" + name();
        }
    }

    private static final Logger log = LoggerFactory.getLogger(SettingsServiceBean.class);

    @EJB
    private SettingDao settingDao;

    @EJB
    private ActionLogServiceBean actionLogSvc;

    @EJB
    private FileBasedSettingsFetcher fileBasedSettingsFetcher;

    private final CacheLoader<String, String> settingCacheLoader = new CacheLoader<String, String>() {
        @Override
        public String load(String key) {
            Setting s = settingDao.find(key);
            return (s != null) ? s.getContent() : fileBasedSettingsFetcher.getSetting(key);
        }
    };

    private final LoadingCache<String, String> settingCache = CacheBuilder.newBuilder()
            .build(settingCacheLoader);

    // -------------------- LOGIC --------------------

    /**
     * Basic functionality - get the name, return the setting from db if present or from properties file if not.
     *
     * @param name of the setting
     * @return the actual setting or empty string.
     */
    public String get(String name) {
        return settingCache.getUnchecked(name);
    }

    /**
     * Same as {@link #get(java.lang.String)}, but with static checking.
     *
     * @param key Enum value of the name.
     * @return The setting, or  empty string.
     */
    public String getValueForKey(Key key) {
        return get(key.toString());
    }

    /**
     * Returns value of setting with key that is postfixed.
     * It can be used if setting has static base key part and dynamic
     * postfix.
     * <p>
     * Example of such setting is {@link Key#ApplicationTermsOfUse}.
     * It can be posfixed with language code to obtain translated
     * values:
     * <code>
     * getValueForKeyWithPostfix(Key.ApplicationTermsOfUse, "de")
     * getValueForKeyWithPostfix(Key.ApplicationTermsOfUse, "fr")
     * </code>
     *
     * @param key
     * @param postfix
     * @return setting value or empty string if setting not present
     */
    public String getValueForKeyWithPostfix(Key key, String postfix) {
        return get(key.toString() + KEY_AND_POSTFIX_SEPARATOR + postfix);
    }

    /**
     * Attempt to convert the value to an integer
     * - Applicable for keys such as MaxFileUploadSizeInBytes
     * <p>
     * On failure (key not found or string not convertible to a long), returns null
     *
     * @param key
     * @return
     */
    public Long getValueForKeyAsLong(Key key) {

        String val = this.getValueForKey(key);

        if (StringUtils.isEmpty(val)) {
            return null;
        }

        try {
            return Long.parseLong(val);
        } catch (NumberFormatException ex) {
            log.warn("Incorrect setting. Could not convert \"{}\" from setting {} to long.", val, key.toString());
            return null;
        }

    }

    public Integer getValueForKeyAsInt(Key key) {
        Long value = getValueForKeyAsLong(key);
        if (value == null) {
            return null;
        }
        return value.intValue();
    }

    public List<String> getValueForKeyAsList(Key key) {
        return Arrays.asList(StringUtils.split(getValueForKey(key), ","));
    }

    public List<Map<String, String>> getValueForKeyAsListOfMaps(Key key) {
        List<Map<String, String>> list = new ArrayList<>();
        try {
            JSONArray entries = new JSONArray(getValueForKey(key));
            for (Object obj : entries) {
                JSONObject entry = (JSONObject) obj;
                list.add(entry.keySet().stream()
                        .collect(toMap(identity(), entry::getString)));
            }
        } catch (JSONException e) {
            log.warn("Error parsing setting " + key + " as JSON", e);
        }
        return list;
    }

    public Setting set(String name, String content) {
        settingCache.invalidate(name);
        Setting s = settingDao.save(new Setting(name, content));
        actionLogSvc.log(new ActionLogRecord(ActionLogRecord.ActionType.Setting, "set")
                                 .setInfo(name + ": " + content));
        return s;
    }

    public Setting setValueForKey(Key key, String content) {
        return set(key.toString(), content);
    }

    /**
     * The correct way to decide whether a string value in the
     * settings table should be considered as {@code true}.
     *
     * @param name name of the setting.
     * @return boolean value of the setting.
     */
    public boolean isTrue(String name) {
        String val = get(name);
        return StringUtil.isTrue(val);
    }

    public boolean isTrueForKey(Key key) {
        return isTrue(key.toString());
    }

    public void deleteValueForKey(Key name) {
        delete(name.toString());
    }

    public void delete(String name) {
        settingCache.invalidate(name);
        actionLogSvc.log(new ActionLogRecord(ActionLogRecord.ActionType.Setting, "delete")
                                 .setInfo(name));
        settingDao.delete(name);
    }

    public Map<String, String> listAll() {
        Map<String, String> mergedSettings = new HashMap<>();

        Map<String, String> fileSettings = fileBasedSettingsFetcher.getAllSettings();
        mergedSettings.putAll(fileSettings);

        List<Setting> dbSettings = settingDao.findAll();
        dbSettings.forEach(s -> mergedSettings.put(s.getName(), s.getContent()));

        return mergedSettings;
    }
}
