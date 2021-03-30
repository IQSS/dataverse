package edu.harvard.iq.dataverse.settings;

import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.api.ApiBlockingFilter;
import edu.harvard.iq.dataverse.util.StringUtil;

import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonObject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.constraints.NotNull;

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
    public enum Key {
        AllowApiTokenLookupViaApi(ConfigScope.AUTH_BUILTIN_API, "allow-token-lookup"),
        /**
         * Ordered, comma-separated list of custom fields to show above the fold
         * on dataset page such as "data_type,sample,pdb"
         */
        CustomDatasetSummaryFields(ConfigScope.UI_DATASET, "custom-summary-fields"),
        /**
         * Defines a public installation -- all datafiles are unrestricted
         */
        PublicInstall(ConfigScope.DOWNLOAD, "public-install"),
        /**
         * Sets the name of your cloud computing environment.
         * For example, "Massachusetts Open Cloud"
         */
        CloudEnvironmentName(ConfigScope.UI_DATASET, "cloud-environment-name"),
        /**
         * Defines the base for a computing environment URL.
         * The container name will be appended to this on the "Compute" button 
         */
        ComputeBaseUrl(ConfigScope.UI_DATASET, "compute-base-url"),
        /**
         * Enables the provenance collection popup.
         * Allows users to store their provenance json and description
         */
        ProvCollectionEnabled(ConfigScope.UI_DATASET, "provenance-collection"),
        /**
         * For example, https://datacapture.example.org
         */
        DataCaptureModuleUrl(ConfigScope.UPLOAD, "dcm-url"),
        RepositoryStorageAbstractionLayerUrl(ConfigScope.UPLOAD, "rsal-url"),
        UploadMethods(ConfigScope.UPLOAD, "methods"),
        DownloadMethods(ConfigScope.DOWNLOAD, "methods"),
        /**
         * If the data replicated around the world using RSAL (Repository
         * Storage Abstraction Layer) is locally available, this is its file
         * path, such as "/programs/datagrid".
         *
         * TODO: Think about if it makes sense to make this a column in the
         * StorageSite database table.
         */
        LocalDataAccessPath(ConfigScope.UPLOAD, "rsal-local-path"),
        IdentifierGenerationStyle(ConfigScope.PID, "generation-style"),
        OAuth2CallbackUrl(ConfigScope.AUTH_OAUTH, "callback-url"),
        DefaultAuthProvider(ConfigScope.AUTH, "default-provider"),
        FooterCopyright(ConfigScope.UI_FOOTER, "copyright-organization"),
        FileFixityChecksumAlgorithm(ConfigScope.UPLOAD, "file-checksum-algorithm"),
        MinutesUntilConfirmEmailTokenExpires(ConfigScope.AUTH, "confirm-mail-expiry-minutes"),
        /**
         * Override Solr highlighting "fragsize"
         * https://wiki.apache.org/solr/HighlightingParameters#hl.fragsize
         */
        SearchHighlightFragmentSize(ConfigScope.INDEX, "highlight-fragment-size"),
        /**
         * Revert to MyData *not* using the Solr "permission documents" which
         * was the behavior in Dataverse 4.2. Starting to use Solr permission
         * documents in MyData has been introduced in 4.2.1 as a fix for
         * https://github.com/IQSS/dataverse/issues/2649 where the "File
         * Downloader" role was exposing cards for unpublished datasets when it
         * shouldn't.
         */
        MyDataDoesNotUseSolrPermissionDocs(ConfigScope.INDEX, "skip-my-data-permission"),
        /**
         * In Dataverse 4.7 and earlier, an API token was required to use the
         * Search API. Tokens are no longer required but you can revert to the
         * old behavior by setting this to false.
         */
        SearchApiRequiresToken(ConfigScope.API_ACCESS, "search-tokenized"),

        /**
         * API endpoints that are not accessible. Comma separated list.
         */
        BlockedApiEndpoints(ConfigScope.API_ACCESS, "blocked"),
        
        /**
         * A key that, with the right {@link ApiBlockingFilter.BlockPolicy},
         * allows calling blocked APIs.
         */
        BlockedApiKey(ConfigScope.API_ACCESS, "unblock-key"),
        
        
        /**
         * How to treat blocked APIs. One of drop, localhost-only, unblock-key
         */
        BlockedApiPolicy(ConfigScope.API_ACCESS, "blocked-policy"),
        
        /**
         * For development only (see dev guide for details). Backed by an enum
         * of possible account types.
         */
        DebugShibAccountType(ConfigScope.AUTH_SHIB, "debug-account-type"),
        DebugOAuthAccountType(ConfigScope.AUTH_OAUTH, "debug-account-type"),
        /** Application-wide Terms of Use per installation. */
        ApplicationTermsOfUse(ConfigScope.UI, "terms-of-use"),
        /** Terms of Use specific to API per installation. */
        ApiTermsOfUse(ConfigScope.API, "terms-of-use"),
        /**
         * URL for the application-wide Privacy Policy per installation, linked
         * to from the footer.
         */
        ApplicationPrivacyPolicyUrl(ConfigScope.UI_FOOTER, "privacy-policy-url"),
        /** Solr hostname and port, such as "localhost:8983". */
        SolrHostColonPort(ConfigScope.INDEX, "host-colon-port"),
        /** Enable full-text indexing in solr up to max file size */
        SolrFullTextIndexing(ConfigScope.INDEX_FULLTEXT, "enable"), //true or false (default)
        SolrMaxFileSizeForFullTextIndexing(ConfigScope.INDEX_FULLTEXT, "max-file-size"), //long - size in bytes (default unset/no limit)
        /** Default Key for limiting the number of bytes uploaded via the Data Deposit API, UI (web site and . */
        MaxFileUploadSizeInBytes(ConfigScope.UPLOAD, "max-upload-size"),
        /** Key for if ScrubMigrationData is enabled or disabled. */
        ScrubMigrationData(ConfigScope.HARVEST_IMPORT, "scrub-data"),
        /** Key for the url to send users who want to sign up to. */
        SignUpUrl(ConfigScope.UI, "signup-url"),
        /** Key for whether we allow users to sign up */
        AllowSignUp(ConfigScope.UI, "signup-enable"),
        /** protocol for global id */
        Protocol(ConfigScope.PID, "protocol"),
        /** authority for global id */
        Authority(ConfigScope.PID, "authority"),
        /** DoiProvider for global id */
        DoiProvider(ConfigScope.PID, "provider"),
        /** Shoulder for global id - used to create a common prefix on identifiers */
        Shoulder(ConfigScope.PID, "shoulder"),
        /* Removed for now - tried to add here but DOI Service Bean didn't like it at start-up
        DoiUsername,
        DoiPassword,
        DoiBaseurlstring,
        */
        /** Optionally override http://guides.dataverse.org . */
        GuidesBaseUrl(ConfigScope.UI, "guides-base-url"),

        /**
         * A link to an installation of https://github.com/IQSS/miniverse or
         * some other metrics app.
         */
        MetricsUrl(ConfigScope.UI, "metrics-url"),
        
        /**
         * Number of minutes before a metrics query can be rerun. Otherwise a cached value is returned.
         * Previous month dates always return cache. Only applies to new internal caching system (not miniverse).
         */
        MetricsCacheTimeoutMinutes(ConfigScope.METRICS, "cache-timout-minutes"),
        /* zip download size limit */
        /** Optionally override version number in guides. */
        GuidesVersion(ConfigScope.UI, "guides-version"),
        ZipDownloadLimit(ConfigScope.DOWNLOAD, "zip-size-limit"),
        /* zip upload number of files limit */
        ZipUploadFilesLimit(ConfigScope.UPLOAD_LIMITS, "max-files-in-zip"),
        /* the number of files the GUI user is allowed to upload in one batch, 
            via drag-and-drop, or through the file select dialog */
        MultipleUploadFilesLimit(ConfigScope.UPLOAD_LIMITS, "max-files-in-batch"),
        /* Size limits for generating thumbnails on the fly */
        /* (i.e., we'll attempt to generate a thumbnail on the fly if the 
         * size of the file is less than this)
        */
        ThumbnailSizeLimitImage(ConfigScope.UPLOAD_LIMITS, "max-size-image-thumbnail"),
        ThumbnailSizeLimitPDF(ConfigScope.UPLOAD_LIMITS, "max-size-pdf-thumbnail"),
        /* return email address for system emails such as notifications */
        SystemEmail(ConfigScope.MAIL_SYSTEM, "fromaddress"),
        /* size limit for Tabular data file ingests */
        /* (can be set separately for specific ingestable formats; in which 
        case the actual stored option will be TabularIngestSizeLimit:{FORMAT_NAME}
        where {FORMAT_NAME} is the format identification tag returned by the 
        getFormatName() method in the format-specific plugin; "sav" for the 
        SPSS/sav format, "RData" for R, etc.
        for example: :TabularIngestSizeLimit:RData */
        TabularIngestSizeLimit(ConfigScope.INGEST, "max-size-tabular"),
        /**
         The message added to a popup upon dataset publish
         * 
         */
        DatasetPublishPopupCustomText(ConfigScope.UI_DATASET, "publish-popup-text"),
        /*
        Whether to display the publish text for every published version
        */
        DatasetPublishPopupCustomTextOnAllVersions(ConfigScope.UI_DATASET, "publish-popup-on-every-release"),
        /*
        Whether Harvesting (OAI) service is enabled
        */
        OAIServerEnabled(ConfigScope.HARVEST, "oai-server-enable"),
        
        /**
        * Whether Shibboleth passive authentication mode is enabled
        */
        ShibPassiveLoginEnabled(ConfigScope.AUTH_SHIB, "passive-login"),
        /**
         * Whether Export should exclude FieldType.EMAIL
         */
        ExcludeEmailFromExport(ConfigScope.EXPORT, "metadata-exclude-email"),
        /*
         Location and name of HomePage customization file
        */
        HomePageCustomizationFile(ConfigScope.UI, "homepage-file"),
        /*
         Location and name of Header customization file
        */
        HeaderCustomizationFile(ConfigScope.UI_HEADER, "include-file"),
        /*
         Location and name of Footer customization file
        */
        FooterCustomizationFile(ConfigScope.UI_FOOTER, "include-file"),
        /*
         Location and name of CSS customization file
        */
        StyleCustomizationFile(ConfigScope.UI, "css-file"),
        /*
         Location and name of analytics code file
        */
        WebAnalyticsCode(ConfigScope.UI, "analytics-snippet"),
        /*
         Location and name of installation logo customization file
        */
        LogoCustomizationFile(ConfigScope.UI_HEADER, "logo-file"),
        
        // Option to override the navbar url underlying the "About" link
        NavbarAboutUrl(ConfigScope.UI_HEADER, "about-url"),
        
        // Option to override multiple guides with a single url
        NavbarGuidesUrl(ConfigScope.UI_HEADER, "guides-url"),

        // Option to overide the feedback dialog display with a link to an external page via its url
        NavbarSupportUrl(ConfigScope.UI_HEADER, "support-url"),

        /**
         * The theme for the root dataverse can get in the way when you try make
         * use of HeaderCustomizationFile and LogoCustomizationFile so this is a
         * way to disable it.
         */
        DisableRootDataverseTheme(ConfigScope.UI, "root-collection-disable-theme"),
        // Limit on how many guestbook entries to display on the guestbook-responses page:
        GuestbookResponsesPageDisplayLimit(ConfigScope.UI, "max-display-guestbook-responses"),

        /**
         * The dictionary filepaths separated by a pipe (|)
         */
        PVDictionaries(ConfigScope.AUTH_BUILTIN_PV, "dictionary-path"),

//        /**
//         * The days and minimum length for when to apply an expiration date.
//         */
//        PVExpirationDays,
//        PVValidatorExpirationMaxLength,

        /**
         * The minimum length of a good, long, strong password.
         */
        PVGoodStrength(ConfigScope.AUTH_BUILTIN_PV, "min-strength"),

        /**
         * A password minimum and maximum length
         */
        PVMinLength(ConfigScope.AUTH_BUILTIN_PV, "min-length"),
        PVMaxLength(ConfigScope.AUTH_BUILTIN_PV, "max-length"),

        /**
         * One letter, 2 special characters, etc.
         */
        PVCharacterRules(ConfigScope.AUTH_BUILTIN_PV, "char-rules"),

        /**
         * The number of M characteristics
         */
        PVNumberOfCharacteristics(ConfigScope.AUTH_BUILTIN_PV, "min-match-char-rules"),
        
        /**
         * The number of consecutive digits allowed for a password
         */
        PVNumberOfConsecutiveDigitsAllowed(ConfigScope.AUTH_BUILTIN_PV, "max-consecutive-digits"),
        /**
         * Configurable text for alert/info message on passwordreset.xhtml when users are required to update their password.
         */
        PVCustomPasswordResetAlertMessage(ConfigScope.AUTH_BUILTIN_PV, "custom-change-alert-text"),
        /*
        String to describe DOI format for data files. Default is DEPENDENT. 
        'DEPENEDENT' means the DOI will be the Dataset DOI plus a file DOI with a slash in between.
        'INDEPENDENT' means a new global id, completely independent from the dataset-level global id.
        */
        DataFilePIDFormat(ConfigScope.PID_FILES, "generation-style"),
        /* Json array of supported languages
        */
        Languages(ConfigScope.UI, "languages"),
        /*
        Number for the minimum number of files to send PID registration to asynchronous workflow
        */
        PIDAsynchRegFileCount(ConfigScope.PID_FILES, "registration-batchsize"),
        /**
         * 
         */
        FilePIDsEnabled(ConfigScope.PID_FILES, "enable"),

        /**
         * Indicates if the Handle service is setup to work 'independently' (No communication with the Global Handle Registry)
         */
        IndependentHandleService(ConfigScope.PID_HDL, "independent"),

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
        
        ArchiverClassName(ConfigScope.EXPORT_BAGIT, "archiver-class"),
        ArchiverSettings(ConfigScope.EXPORT_BAGIT, "archiver-setting-ref"),
        /**
         * A comma-separated list of roles for which new dataverses should inherit the
         * corresponding role assignments from the parent dataverse. Also affects
         * /api/admin/dataverse/{alias}/addRolesToChildren. Default is "", no
         * inheritance. "*" means inherit assignments for all roles
         */
        InheritParentRoleAssignments(ConfigScope.AUTH, "inherit-parent-roles"),
        
        /** Make Data Count Logging and Display */
        MDCLogPath(ConfigScope.METRICS, "mdc-log-path"),
        DisplayMDCMetrics(ConfigScope.METRICS, "mdc-display"),

        /**
         * Allow CORS flag (true or false). It is true by default
         *
         */
        AllowCors(ConfigScope.AUTH, "allow-cors"),
        
        /**
         * Lifespan, in minutes, of a login user session 
         * (both DataverseSession and the underlying HttpSession)
         */
        LoginSessionTimeout(ConfigScope.AUTH, "session-timeout"),

        /**
         * Shibboleth affiliation attribute which holds information about the affiliation of the user (e.g. ou)
         */
        ShibAffiliationAttribute(ConfigScope.AUTH_SHIB, "affiliation-attribute"),
        /**
         * Convert shibboleth AJP attributes from ISO-8859-1 to UTF-8
         */
        ShibAttributeCharacterSetConversionEnabled(ConfigScope.AUTH_SHIB, "convert-attributes-charset"),
        /**
         * Validate physical files for all the datafiles in the dataset when publishing
         */
        FileValidationOnPublishEnabled(ConfigScope.UPLOAD, "validate-on-publish"),
        /**
         * If defined, this is the URL of the zipping service outside 
         * the main Application Service where zip downloads should be directed
         * instead of /api/access/datafiles/
         */
        CustomZipDownloadServiceUrl(ConfigScope.DOWNLOAD, "zip-download-url"),
        /**
         * Sort Date Facets Chronologically instead or presenting them in order of # of hits as other facets are. Default is true
         */
        ChronologicalDateFacets(ConfigScope.UI, "date-facets-chronological")
        ;
        
        private final ConfigScope scope;
        private final String key;
        
        // Initialize unmodifiable map of all enum entries to make lookup from name less error prone
        private static final Map<String,Key> nameIndex = Map.copyOf(Arrays.stream(Key.values()).collect(Collectors.toMap(Key::name,k -> k)));
        
        Key(@NotNull ConfigScope scope, @NotNull String key) {
           this.scope = scope;
           this.key = key;
        }
    
        /**
         * Nullsafe lookup of a key from its String name. May contain the leading colon (will be stripped of)
         * @param name The key String name to lookup. May contain the leading colon (will be stripped of), must be at least 2 chars long.
         * @return The corresponding key or null if not found
         */
        public static Key lookupKey(String name) {
            if (name != null && name.length() > 1)
                return nameIndex.getOrDefault(name, nameIndex.get(name.substring(1)));
            else
                return null;
        }
    
        public ConfigScope getScope() { return scope; }
        public String getKey() { return key; }
        public String getScopedKey() { return scope.getScopedKey(getKey()); }
    
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
     * Same as {@link #get(java.lang.String)}, but with static checking.
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
    		   try ( StringReader rdr = new StringReader(val) ) {
    			   JsonObject settings = Json.createReader(rdr).readObject();
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
    
    
}
