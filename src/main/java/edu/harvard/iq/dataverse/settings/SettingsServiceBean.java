package edu.harvard.iq.dataverse.settings;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.api.ApiBlockingFilter;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
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
        DataCaptureModuleUrl,
        RepositoryStorageAbstractionLayerUrl,
        UploadMethods,
        DownloadMethods,
        /**
         * If the data replicated around the world using RSAL (Repository
         * Storage Abstraction Layer) is locally available, this is its file
         * path, such as "/programs/datagrid".
         *
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
         * A boolean defining if indexing and search should respect the concept
         * of "permission root".
         *
         * <p>
         *
         * If we ignore permissionRoot at index time, we should blindly give
         * search ("discoverability") access to people and group who have access
         * defined in a parent dataverse, all the way back to the root.
         *
         * <p>
         *
         * If we respect permissionRoot, this means that the dataverse being
         * indexed is an island of permissions all by itself. We should not look
         * to its parent to see if more people and groups might be able to
         * search the DvObjects within it. We would assume no implicit
         * inheritance of permissions. In this mode, all permissions must be
         * explicitly defined on DvObjects. No implied inheritance.
         *
         */
        SearchRespectPermissionRoot,
        /** Solr hostname and port, such as "localhost:8983". */
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
        /** protocol for global id */
        Protocol,
        /** authority for global id */
        Authority,
        /** DoiProvider for global id */
        DoiProvider,
        /** Shoulder for global id - used to create a common prefix on identifiers */
        Shoulder,
        /* Removed for now - tried to add here but DOI Service Bean didn't like it at start-up
        DoiUsername,
        DoiPassword,
        DoiBaseurlstring,
        */
        /** Optionally override http://guides.dataverse.org . */
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
        /** Optionally override version number in guides. */
        GuidesVersion,
        ZipDownloadLimit,
        /* zip upload number of files limit */
        ZipUploadFilesLimit,
        /* the number of files the GUI user is allowed to upload in one batch, 
            via drag-and-drop, or through the file select dialog */
        MultipleUploadFilesLimit,
        /* Size limits for generating thumbnails on the fly */
        /* (i.e., we'll attempt to generate a thumbnail on the fly if the 
         * size of the file is less than this)
        */
        ThumbnailSizeLimitImage,
        ThumbnailSizeLimitPDF,
        /* return email address for system emails such as notifications */
        SystemEmail, 
        /* size limit for Tabular data file ingests */
        /* (can be set separately for specific ingestable formats; in which 
        case the actual stored option will be TabularIngestSizeLimit:{FORMAT_NAME}
        where {FORMAT_NAME} is the format identification tag returned by the 
        getFormatName() method in the format-specific plugin; "sav" for the 
        SPSS/sav format, "RData" for R, etc.
        for example: :TabularIngestSizeLimit:RData */
        TabularIngestSizeLimit,
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
        String to describe DOI format for data files. Default is DEPENDENT. 
        'DEPENEDENT' means the DOI will be the Dataset DOI plus a file DOI with a slash in between.
        'INDEPENDENT' means a new global id, completely independent from the dataset-level global id.
        */
        DataFilePIDFormat, 
        /* Json array of supported languages
        */
        Languages,
        /*
        Number for the minimum number of files to send PID registration to asynchronous workflow
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
        
        /** Make Data Count Logging and Display */
        MDCLogPath, 
        DisplayMDCMetrics,

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
        ExportInstallationAsDistributorOnlyWhenNotSet
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
