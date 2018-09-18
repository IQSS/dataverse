package edu.harvard.iq.dataverse.settings;

import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.api.ApiBlockingFilter;
import edu.harvard.iq.dataverse.util.StringUtil;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
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
        * Domain name specific code for Google Analytics
        *//**
        * Domain name specific code for Google Analytics
        */
        GoogleAnalyticsCode,

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
        /** Key for limiting the number of bytes uploaded via the Data Deposit API, UI (web site and . */
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
        /* status message that will appear on the home page */
        StatusMessageHeader,
        /* full text of status message, to appear in popup */
        StatusMessageText,
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
        Whether to allow user to create GeoConnect Maps
        This boolean effects whether the user sees the map button on 
        the dataset page and if the ingest will create a shape file
        Default is false
        */
        GeoconnectCreateEditMaps,
        /**
        Whether to allow a user to view existing maps
        This boolean effects whether a user may see the 
        Explore World Map Button
        Default is false;
        */
        GeoconnectViewMaps,
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
        FilePIDsEnabled
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
        Setting s = em.find( Setting.class, name );
        return (s!=null) ? s.getContent() : null;
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
    
    public String getValueForKey( Key key, String defaultValue ) {
        return get( key.toString(), defaultValue );
    }
     
    public Setting set( String name, String content ) {
        Setting s = new Setting( name, content );
        s = em.merge(s);
        actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.Setting, "set")
                            .setInfo(name + ": " + content));
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
    
    public Set<Setting> listAll() {
        return new HashSet<>(em.createNamedQuery("Setting.findAll", Setting.class).getResultList());
    }
    
    
}
