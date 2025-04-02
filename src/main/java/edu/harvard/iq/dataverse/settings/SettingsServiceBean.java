package edu.harvard.iq.dataverse.settings;

import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.api.ApiBlockingFilter;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service bean accessing a persistent hash map, used as settings in the application.
 * @author michael
 */
@Stateless
@Named
public class SettingsServiceBean {
    
    private static final Logger logger = Logger.getLogger(SettingsServiceBean.class.getCanonicalName());
    
    /**
     * Some convenient keys for the settings. Note that the setting's 
     * name is really a {@code String}, but it's good to have the compiler look
     * over your shoulder when typing strings in various places of a large app. 
     * So there.
     */
    @SuppressWarnings("java:S115")
    public enum Key {
        AllowApiTokenLookupViaApi,
        /**
         * Ordered, comma-separated list of custom fields to show above the fold
         * on dataset page such as "data_type,sample,pdb"
         */
        CustomDatasetSummaryFields,
        /**
         * Defines a public installation -- all datafiles are unrestricted
         *
         * This was added along with CloudEnvironmentName and ComputeBaseUrl.
         * See https://github.com/IQSS/dataverse/issues/3776 and
         * https://github.com/IQSS/dataverse/pull/3967
         */
        PublicInstall,
        /**
         * Sets the name of your cloud computing environment.
         * For example, "Massachusetts Open Cloud"
         */
        CloudEnvironmentName,
        /**
         * Defines the base for a computing environment URL.
         * The container name will be appended to this on the "Compute" button 
         */
        ComputeBaseUrl,
        /**
         * Enables the provenance collection popup.
         * Allows users to store their provenance json and description
         */
        ProvCollectionEnabled,
        /**
         * For example, https://datacapture.example.org
         */
        @Deprecated(forRemoval = true, since = "2024-07-07")
        DataCaptureModuleUrl,
        @Deprecated(forRemoval = true, since = "2024-07-07")
        RepositoryStorageAbstractionLayerUrl,
        UploadMethods,
        @Deprecated(forRemoval = true, since = "2024-07-07")
        DownloadMethods,
        /**
         * If the data replicated around the world using RSAL (Repository
         * Storage Abstraction Layer) is locally available, this is its file
         * path, such as "/programs/datagrid".
         *
         * TODO: Think about if it makes sense to make this a column in the
         * StorageSite database table.
         */
        @Deprecated(forRemoval = true, since = "2024-07-07")
        LocalDataAccessPath,
        /**
         * The algorithm used to generate PIDs, randomString (default) or
         * storedProcedure
         * 
         * @deprecated New installations should not use this database setting, but use
         *             the settings within {@link JvmSettings#SCOPE_PID}.
         * 
         */
        @Deprecated(forRemoval = true, since = "2024-02-13")
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
         * Revert to MyData *not* using the Solr "permission documents" which
         * was the behavior in Dataverse 4.2. Starting to use Solr permission
         * documents in MyData has been introduced in 4.2.1 as a fix for
         * https://github.com/IQSS/dataverse/issues/2649 where the "File
         * Downloader" role was exposing cards for unpublished datasets when it
         * shouldn't.
         */
        MyDataDoesNotUseSolrPermissionDocs,
        /**
         * In Dataverse 4.7 and earlier, an API token was required to use the
         * Search API. Tokens are no longer required but you can revert to the
         * old behavior by setting this to false.
         */
        SearchApiRequiresToken,
        /**
         * Experimental: Use Solr to power the file listing on the dataset page.
         */
        FilesOnDatasetPageFromSolr,

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
        /** Application-wide Terms of Use per installation. */
        ApplicationTermsOfUse,
        /** Terms of Use specific to API per installation. */
        ApiTermsOfUse,
        /**
         * URL for the application-wide Privacy Policy per installation, linked
         * to from the footer.
         */
        ApplicationPrivacyPolicyUrl,
        /**
         * Solr hostname and port, such as "localhost:8983".
         * @deprecated New installations should not use this database setting, but use {@link JvmSettings#SOLR_HOST}
         *             and {@link JvmSettings#SOLR_PORT}.
         */
        @Deprecated(forRemoval = true, since = "2022-12-23")
        SolrHostColonPort,
        /** Enable full-text indexing in solr up to max file size */
        SolrFullTextIndexing, //true or false (default)
        SolrMaxFileSizeForFullTextIndexing, //long - size in bytes (default unset/no limit)
        /** Default Key for limiting the number of bytes uploaded via the Data Deposit API, UI (web site and . */
        MaxFileUploadSizeInBytes,
        /** Key for if ScrubMigrationData is enabled or disabled. */
        ScrubMigrationData,
        /** Key for the url to send users who want to sign up to. */
        SignUpUrl,
        /** Key for whether we allow users to sign up */
        AllowSignUp,
        /**
         * protocol for global id
         * 
         * @deprecated New installations should not use this database setting, but use
         *             the settings within {@link JvmSettings#SCOPE_PID}.
         * 
         */
        @Deprecated(forRemoval = true, since = "2024-02-13")
        Protocol,
        /**
         * authority for global id
         * 
         * @deprecated New installations should not use this database setting, but use
         *             the settings within {@link JvmSettings#SCOPE_PID}.
         * 
         */
        @Deprecated(forRemoval = true, since = "2024-02-13")
        Authority,
        /**
         * DoiProvider for global id
         * 
         * @deprecated New installations should not use this database setting, but use
         *             the settings within {@link JvmSettings#SCOPE_PID}.
         * 
         */
        @Deprecated(forRemoval = true, since = "2024-02-13")
        DoiProvider,
        /**
         * Shoulder for global id - used to create a common prefix on identifiers
         * 
         * @deprecated New installations should not use this database setting, but use
         *             the settings within {@link JvmSettings#SCOPE_PID}.
         * 
         */
        @Deprecated(forRemoval = true, since = "2024-02-13")
        Shoulder,
        /** Optionally override http://guides.dataverse.org . */
        GuidesBaseUrl,

        CVocConf,

        // Default calls per hour for each tier. csv format (30,60,...)
        RateLimitingDefaultCapacityTiers,
        // json defined list of capacities by tier and action list. See RateLimitSetting.java
        RateLimitingCapacityByTierAndAction,
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
        /** Optionally override version number in guides. */
        GuidesVersion,
        ZipDownloadLimit,
        /* zip upload number of files limit */
        ZipUploadFilesLimit,
        /* the number of files the GUI user is allowed to upload in one batch, 
            via drag-and-drop, or through the file select dialog */
        MultipleUploadFilesLimit,
        /**
         * Return email address for system emails such as notifications
         * @deprecated Please replace usages with {@link edu.harvard.iq.dataverse.MailServiceBean#getSystemAddress},
         *             which is backward compatible with this setting.
         */
        @Deprecated(since = "6.2", forRemoval = true)
        SystemEmail, 
        /* size limit for Tabular data file ingests */
        /* (can be set separately for specific ingestable formats; in which 
        case the actual stored option will be TabularIngestSizeLimit:{FORMAT_NAME}
        where {FORMAT_NAME} is the format identification tag returned by the 
        getFormatName() method in the format-specific plugin; "sav" for the 
        SPSS/sav format, "RData" for R, etc.
        for example: :TabularIngestSizeLimit:RData */
        TabularIngestSizeLimit,
        /* Validate physical files in the dataset when publishing, if the dataset size less than the threshold limit */
        DatasetChecksumValidationSizeLimit,
        /* Validate physical files in the dataset when publishing, if the datafile size less than the threshold limit */
        DataFileChecksumValidationSizeLimit,
        /**
         The message added to a popup upon dataset publish
         * 
         */
        DatasetPublishPopupCustomText,
        /*
        Whether to display the publish text for every published version
        */
        DatasetPublishPopupCustomTextOnAllVersions,
        /*
        Whether Harvesting (OAI) service is enabled
        */
        OAIServerEnabled,
        
        /**
        * Whether Shibboleth passive authentication mode is enabled
        */
        ShibPassiveLoginEnabled,
        /**
         * Whether Export should exclude FieldType.EMAIL
         */
        ExcludeEmailFromExport,
        /*
         Location and name of HomePage customization file
        */
        HomePageCustomizationFile,
        /*
         Location and name of Header customization file
        */
        HeaderCustomizationFile,
        /*
         Location and name of Footer customization file
        */
        FooterCustomizationFile,
        /*
         Location and name of CSS customization file
        */
        StyleCustomizationFile,
        /*
         Location and name of analytics code file
        */
        WebAnalyticsCode,
        /*
         Location and name of installation logo customization file
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

//        /**
//         * The days and minimum length for when to apply an expiration date.
//         */
//        PVExpirationDays,
//        PVValidatorExpirationMaxLength,

        /**
         * The minimum length of a good, long, strong password.
         */
        PVGoodStrength,

        /**
         * A password minimum and maximum length
         */
        PVMinLength,
        PVMaxLength,

        /**
         * One letter, 2 special characters, etc.
         */
        PVCharacterRules,

        /**
         * The number of M characteristics
         */
        PVNumberOfCharacteristics,
        
        /**
         * The number of consecutive digits allowed for a password
         */
        PVNumberOfConsecutiveDigitsAllowed,
        /**
         * Configurable text for alert/info message on passwordreset.xhtml when users are required to update their password.
         */
        PVCustomPasswordResetAlertMessage,
        /*
         * String to describe DOI format for data files. Default is DEPENDENT.
         * 'DEPENDENT' means the DOI will be the Dataset DOI plus a file DOI with a
         * slash in between. 'INDEPENDENT' means a new global id, completely independent
         * from the dataset-level global id.
         *
         * @deprecated New installations should not use this database setting, but use
         * the settings within {@link JvmSettings#SCOPE_PID}.
         * 
         */
        @Deprecated(forRemoval = true, since = "2024-02-13")
        DataFilePIDFormat, 
        /* Json array of supported languages
        */
        Languages,
        /*
        Number for the minimum number of files to send PID registration to asynchronous workflow
        */
        //PIDAsynchRegFileCount,
        /**
         * 
         */
        FilePIDsEnabled,

        /**
         * Indicates if the Handle service is setup to work 'independently' (No communication with the Global Handle Registry)
         * 
         * @deprecated New installations should not use this database setting, but use
         *             the settings within {@link JvmSettings#SCOPE_PID}.
         * 
         */
        @Deprecated(forRemoval = true, since = "2024-02-13")
        IndependentHandleService,

        /**
        Handle to use for authentication if the default is not being used
         * 
         * @deprecated New installations should not use this database setting, but use
         *             the settings within {@link JvmSettings#SCOPE_PID}.
         * 
         */
        @Deprecated(forRemoval = true, since = "2024-02-13")
        HandleAuthHandle,

        /**
         * Archiving can be configured by providing an Archiver class name (class must extend AstractSubmitToArchiverCommand)
         * and a list of settings that should be passed to the Archiver.
         * Note: 
         * Configuration may also require adding Archiver-specific jvm-options (i.e. for username and password) in glassfish.
         * 
         * To automate the submission of an archival copy step as part of publication, a post-publication workflow must also be configured.
         * 
         * For example:
         * ArchiverClassName - "edu.harvard.iq.dataverse.engine.command.impl.DPNSubmitToArchiveCommand"
         * ArchiverSettings - "DuraCloudHost, DuraCloudPort, DuraCloudContext"
         * 
         * Note: Dataverse must be configured with values for these dynamically defined settings as well, e.g. 
         * 
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
        
        /** Make Data Count Logging, Display, and Start Date */
        MDCLogPath, 
        DisplayMDCMetrics,
        MDCStartDate,

        /**
         * Allow CORS flag (true or false). It is true by default
         *
         */
        AllowCors, 
        
        /**
         * Lifespan, in minutes, of a login user session 
         * (both DataverseSession and the underlying HttpSession)
         */
        LoginSessionTimeout,

        /**
         * Shibboleth affiliation attribute which holds information about the affiliation of the user (e.g. ou)
         */
        ShibAffiliationAttribute,
        /**
         * Convert shibboleth AJP attributes from ISO-8859-1 to UTF-8
         */
        ShibAttributeCharacterSetConversionEnabled,
        /**
         *Return the last or first value of an array of affiliation names
         */
        ShibAffiliationOrder,
         /**
         *Split the affiliation array on given string, default ";"
         */
        ShibAffiliationSeparator,
        /**
         * Validate physical files for all the datafiles in the dataset when publishing
         */
        FileValidationOnPublishEnabled,
        /**
         * If defined, this is the URL of the zipping service outside 
         * the main Application Service where zip downloads should be directed
         * instead of /api/access/datafiles/
         */
        CustomZipDownloadServiceUrl,
        /**
         * Sort Date Facets Chronologically instead or presenting them in order of # of hits as other facets are. Default is true
         */
        ChronologicalDateFacets, 
        /**
         * Used where BrandingUtil.getInstallationBrandName is called, overides the default use of the root Dataverse collection name
         */
        InstallationName,
        /**
         * In metadata exports that set a 'distributor' this flag determines whether the
         * Installation Brand Name is always included (default/false) or is not included
         * when the Distributor field (citation metadatablock) is set (true)
         */
        ExportInstallationAsDistributorOnlyWhenNotSet,

        /** Globus App URL
         * 
         */
        GlobusAppUrl,
        /** Globus Polling Interval how long in seconds Dataverse waits between checks on Globus upload status checks
         * 
         */
        GlobusPollingInterval,
        /**Enable single-file download/transfers for Globus
         *
         */
        GlobusSingleFileTransfer,
        /** Lower limit of the number of files in a Globus upload task where 
         * the batch mode should be utilized in looking up the file information 
         * on the remote end node (file sizes, primarily), instead of individual
         * lookups. 
         */
        GlobusBatchLookupSize,
        /**
         * Optional external executables to run on the metadata for dataverses 
         * and datasets being published; as an extra validation step, to 
         * check for spam, etc. 
         */
        DataverseMetadataValidatorScript,
        DatasetMetadataValidatorScript,
        DataverseMetadataPublishValidationFailureMsg,
        DataverseMetadataUpdateValidationFailureMsg,
        DatasetMetadataValidationFailureMsg,
        ExternalValidationAdminOverride,
        /**
         * A comma-separated list of field type names that should be 'withheld' when
         * dataset access occurs via a Private Url with Anonymized Access (e.g. to
         * support anonymized review). A suggested minimum includes author,
         * datasetContact, and contributor, but additional fields such as depositor, grantNumber, and
         * publication might also need to be included.
         */
        AnonymizedFieldTypeNames,
        /**
         * A Json array containing key/values corresponding the the allowed languages
         * for entering metadata. FOrmat matches that of the Languages setting: e.g.
         * '[{"locale":"en","title":"English"},{"locale":"fr","title":"Français"}]' with
         * the locale being an ISO-639 code for that language (2 and 3 letter codes from
         * the 639-2 and 639-3 standards are allowed. These will be used directly in
         * metadata exports) and the title containing a human readable string. These
         * values are selectable at the Dataverse level and apply to Dataset metadata.
         */
        MetadataLanguages,
        /**
         * A boolean setting that, if true will send an email and notification to users
         * when a Dataset is created. Messages go to those who have the
         * ability/permission necessary to publish the dataset
         */
        SendNotificationOnDatasetCreation,
        /**
         * A JSON Object containing named comma separated sets(s) of allowed labels (up
         * to 32 characters, spaces allowed) that can be set on draft datasets, via API
         * or UI by users with the permission to publish a dataset. (Set names are
         * string keys, labels are a JSON array of strings). These should correspond to
         * the states in an organizations curation process(es) and are intended to help
         * users/curators track the progress of a dataset through an externally defined
         * curation process. Only one set of labels are allowed per dataset (defined via
         * API by a superuser per collection (UI or API) or per dataset (API only)). A
         * dataset may only have one label at a time and if a label is set, it will be
         * removed at publication time. This functionality is disabled when this setting
         * is empty/not set.
         */
        AllowedCurationLabels,
        /** This setting enables Embargo capabilities in Dataverse and sets the maximum Embargo duration allowed.
         * 0 or not set: new embargoes disabled
         * -1: embargo enabled, no time limit
         * n: embargo enabled with n months the maximum allowed duration
         */
        MaxEmbargoDurationInMonths,
        /** This setting enables Retention capabilities in Dataverse and sets the minimum Retention duration allowed.
         * 0 or not set: new retentions disabled
         * -1: retention enabled, no time limit
         * n: retention enabled with n months the minimum allowed duration
         */
        MinRetentionDurationInMonths,
        /*
         * Include "Custom Terms" as an item in the license drop-down or not.
         */
        AllowCustomTermsOfUse,
        /*
         * Allow users to mute notifications or not.
         */
        ShowMuteOptions,
        /*
         * List (comma separated, e.g., "ASSIGNROLE,REVOKEROLE", extra whitespaces are trimmed such that "ASSIGNROLE, REVOKEROLE"
         * would also work) of always muted notifications that cannot be turned on by the users.
         */
        AlwaysMuted,
        /*
         * List (comma separated, e.g., "ASSIGNROLE,REVOKEROLE", extra whitespaces are trimmed such that "ASSIGNROLE, REVOKEROLE"
         * would also work) of never muted notifications that cannot be turned off by the users. AlwaysMuted setting overrides
         * Nevermuted setting warning is logged.
         */
        NeverMuted,
        /**
         * LDN Inbox Allowed Hosts - a comma separated list of IP addresses allowed to submit messages to the inbox
         */
        LDNMessageHosts,

        /*
         * Allow a custom JavaScript to control values of specific fields.
         */
        ControlledVocabularyCustomJavaScript, 
        /**
         * A compound setting for disabling signup for remote Auth providers:
         */
        AllowRemoteAuthSignUp,
        /**
         * The URL for the DvWebLoader tool (see github.com/gdcc/dvwebloader for details)
         */
        WebloaderUrl, 
        /**
         * Enforce storage quotas:
         */
        UseStorageQuotas, 
        /** 
         * Placeholder storage quota (defines the same quota setting for every user; used to test the concept of a quota.
         */
        StorageQuotaSizeInBytes,

        /**
         * A comma-separated list of CategoryName in the desired order for files to be
         * sorted in the file table display. If not set, files will be sorted
         * alphabetically by default. If set, files will be sorted by these categories
         * and alphabetically within each category.
         */
        CategoryOrder,
        /**
         * True(default)/false option deciding whether ordering by folder should be applied to the 
         * dataset listing of datafiles.
         */
        OrderByFolder,
        /**
         * True/false(default) option deciding whether the dataset file table display should include checkboxes
         * allowing users to dynamically turn folder and category ordering on/off.
         */
        AllowUserManagementOfOrder,
        /*
         * True/false(default) option deciding whether file PIDs can be enabled per collection - using the Dataverse/collection set attribute API call.
         */
        AllowEnablingFilePIDsPerCollection,
        /**
         * Allows an instance admin to disable Solr search facets on the collection
         * and dataset pages instantly
         */
        DisableSolrFacets,
        DisableSolrFacetsForGuestUsers,
        DisableSolrFacetsWithoutJsession,
        DisableUncheckedTypesFacet,
        /**
         * When ingesting tabular data files, store the generated tab-delimited 
         * files *with* the variable names line up top. 
         */
        StoreIngestedTabularFilesWithVarHeaders,

        ContactFeedbackMessageSizeLimit
        ;

        @Override
        public String toString() {
            return ":" + name();
        }
    }
    
    @PersistenceContext
    EntityManager em;
    
    @EJB
    ActionLogServiceBean actionLogSvc;
    
    /**
     * Basic functionality - get the name, return the setting, or {@code null}.
     * @param name of the setting
     * @return the actual setting, or {@code null}.
     */
    public String get( String name ) {
        List<Setting> tokens = em.createNamedQuery("Setting.findByName", Setting.class)
                .setParameter("name", name )
                .getResultList();
        String val = null;
        if(tokens.size() > 0) {
            val = tokens.get(0).getContent();
        }
        return (val!=null) ? val : null;
    }
    
    /**
     * Same as {@link #get(String)}, but with static checking.
     * @param key Enum value of the name.
     * @return The setting, or {@code null}.
     */
    public String getValueForKey( Key key ) {
        return get(key.toString());
    }
    
    
    /**
     * Attempt to convert the value to an integer
     *  - Applicable for keys such as MaxFileUploadSizeInBytes
     * 
     * On failure (key not found or string not convertible to a long), returns null
     * @param key
     * @return 
     */
       public Long getValueForKeyAsLong(Key key){
        
        String val = this.getValueForKey(key);

        if (val == null){
            return null;
        }

        try {
            long valAsInt = Long.parseLong(val);
            return valAsInt;
        } catch (NumberFormatException ex) {
            logger.log(Level.WARNING, "Incorrect setting.  Could not convert \"{0}\" from setting {1} to long.", new Object[]{val, key.toString()});
            return null;
        }
        
    }

    /**
     * Attempt to convert the value to an integer
     *  - Applicable for keys such as MaxFileUploadSizeInBytes
     *
     * On failure (key not found or string not convertible to a long), returns defaultValue
     * @param key
     * @param defaultValue
     * @return
     */
    public Long getValueForKeyAsLong(Key key, Long defaultValue) {
           Long val = getValueForKeyAsLong(key);
           if (val == null) {
               return defaultValue;
           }
           return val;
    }
    
       /**
        * Attempt to convert a value in a compound key to a long
        *  - Applicable for keys such as MaxFileUploadSizeInBytes after multistore capabilities were added in ~v4.20
        *  backward compatible with a single value. For multi values, the key's value must be an object with param:value pairs.
        *  A "default":value pair is allowed and will be returned for any param that doesn't have a defined value.   
        * 
        * On failure (key not found or string not convertible to a long), returns null
        * @param key
        * @return 
        */
       public Long getValueForCompoundKeyAsLong(Key key, String param){

    	   String val = this.getValueForKey(key);

    	   if (val == null){
    		   return null;
    	   }

    	   try {
    		   return Long.parseLong(val);
    	   } catch (NumberFormatException ex) {
    		   try {
    			   JsonObject settings = JsonUtil.getJsonObject(val);
    			   if(settings.containsKey(param)) {
    				   return Long.parseLong(settings.getString(param));
    			   } else if(settings.containsKey("default")) {
    				   return Long.parseLong(settings.getString("default"));
    			   } else {
    				   return null;
    			   }

    		   } catch (Exception e) {
    			   logger.log(Level.WARNING, "Incorrect setting.  Could not convert \"{0}\" from setting {1} to long: {2}", new Object[]{val, key.toString(), e.getMessage()});
    			   return null;
    		   }
    	   }

       }
       
    /**
     * Same, but with Booleans 
     * (returns null if not set; up to the calling method to decide what that should
     * default to in each specific case)
     * Example:
     * :AllowRemoteAuthSignUp	{"default":"true","google":"false"}
     */
    
    public Boolean getValueForCompoundKeyAsBoolean(Key key, String param) {

        String val = this.getValueForKey(key);

        if (val == null) {
            return null;
        }

        try {
            JsonObject settings = JsonUtil.getJsonObject(val);
            if (settings.containsKey(param)) {
                return Boolean.parseBoolean(settings.getString(param));
            } else if (settings.containsKey("default")) {
                return Boolean.parseBoolean(settings.getString("default"));
            } else {
                return null;
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Incorrect setting.  Could not convert \"{0}\" from setting {1} to boolean: {2}", new Object[]{val, key.toString(), e.getMessage()});
            return null;
        }

    }
    /**
     * Return the value stored, or the default value, in case no setting by that
     * name exists. The main difference between this method and the other {@code get()}s
     * is that is never returns null (unless {@code defaultValue} is {@code null}.
     * 
     * @param name Name of the setting.
     * @param defaultValue The value to return if no setting is found in the DB.
     * @return Either the stored value, or the default value.
     */
    public String get( String name, String defaultValue ) {
        String val = get(name);
        return (val!=null) ? val : defaultValue;
    }

    public String get(String name, String lang, String defaultValue ) {
        List<Setting> tokens = em.createNamedQuery("Setting.findByNameAndLang", Setting.class)
                .setParameter("name", name )
                .setParameter("lang", lang )
                .getResultList();
        String val = null;
        if(tokens.size() > 0) {
            val = tokens.get(0).getContent();
        }
        return (val!=null) ? val : defaultValue;
    }
    
    public String getValueForKey( Key key, String defaultValue ) {
        return get( key.toString(), defaultValue );
    }

    public String getValueForKey( Key key, String lang, String defaultValue ) {
        return get( key.toString(), lang, defaultValue );
    }
     
    public Setting set( String name, String content ) {
        Setting s = null; 
        
        List<Setting> tokens = em.createNamedQuery("Setting.findByName", Setting.class)
                .setParameter("name", name )
                .getResultList();
        
        if(tokens.size() > 0) {
            s = tokens.get(0);
        }
        
        if (s == null) {
            s = new Setting( name, content );
        } else {
            s.setContent(content);
        }
        
        s = em.merge(s);
        actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.Setting, "set")
                            .setInfo(name + ": " + content));
        return s;
    }

    public Setting set( String name, String lang, String content ) {
        Setting s = null; 
        
        List<Setting> tokens = em.createNamedQuery("Setting.findByNameAndLang", Setting.class)
                .setParameter("name", name )
                .setParameter("lang", lang )
                .getResultList();
        
        if(tokens.size() > 0) {
            s = tokens.get(0);
        }
        
        if (s == null) {
            s = new Setting( name, lang, content );
        } else {
            s.setContent(content);
        }
        
        em.merge(s);
        actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.Setting, "set")
                .setInfo(name + ": " +lang + ": " + content));
        return s;
    }
    
    public Setting setValueForKey( Key key, String content ) {
        return set( key.toString(), content );
    }
    
    /**
     * The correct way to decide whether a string value in the
     * settings table should be considered as {@code true}.
     * @param name name of the setting.
     * @param defaultValue logical value of {@code null}.
     * @return boolean value of the setting.
     */
    public boolean isTrue( String name, boolean defaultValue ) {
        String val = get(name);
        return ( val==null ) ? defaultValue : StringUtil.isTrue(val);
    }
    
    public boolean isTrueForKey( Key key, boolean defaultValue ) {
        return isTrue( key.toString(), defaultValue );
    }

    public boolean isFalseForKey( Key key, boolean defaultValue ) {
        return ! isTrue( key.toString(), defaultValue );
    }

    public boolean containsCommaSeparatedValueForKey(Key key, String value) {
        final String tokens = getValueForKey(key);
        if (tokens == null || tokens.isEmpty()) {
            return false;
        }
        return Collections.list(new StringTokenizer(tokens, ",")).stream()
            .anyMatch(token -> ((String) token).trim().equals(value));
    }
            
    public void deleteValueForKey( Key name ) {
        delete( name.toString() );
    }
    
    public void delete( String name ) {
        actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.Setting, "delete")
                            .setInfo(name));
        em.createNamedQuery("Setting.deleteByName")
                .setParameter("name", name)
                .executeUpdate();
    }

    public void delete( String name, String lang ) {
        actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.Setting, "delete")
                .setInfo(name));
        em.createNamedQuery("Setting.deleteByNameAndLang")
                .setParameter("name", name)
                .setParameter("lang", lang)
                .executeUpdate();
    }
    
    public Set<Setting> listAll() {
        return new HashSet<>(em.createNamedQuery("Setting.findAll", Setting.class).getResultList());
    }
    
    public Map<String, String> getBaseMetadataLanguageMap(Map<String,String> languageMap, boolean refresh) {
        if (languageMap == null || refresh) {
            languageMap = new HashMap<String, String>();

            /* If MetadataLanaguages is set, use it.
             * If not, we can't assume anything and should avoid assuming a metadata language
             */
            String mlString = getValueForKey(SettingsServiceBean.Key.MetadataLanguages,"");
            
            if(mlString.isEmpty()) {
                mlString="[]";
            }
            JsonArray languages = JsonUtil.getJsonArray(mlString);
            for(JsonValue jv: languages) {
                JsonObject lang = (JsonObject) jv;
                languageMap.put(lang.getString("locale"), lang.getString("title"));
            }
        }
        return languageMap;
    }
    
    public void initLocaleSettings(Map<String, String> configuredLocales) {
        
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
    

    public Set<String> getConfiguredLanguages() {
        Set<String> langs = new HashSet<String>();
        langs.addAll(getBaseMetadataLanguageMap(new HashMap<String, String>(), true).keySet());
        Map<String, String> configuredLocales = new LinkedHashMap<>();
        initLocaleSettings(configuredLocales);
        langs.addAll(configuredLocales.keySet());
        return langs;
    }

}
