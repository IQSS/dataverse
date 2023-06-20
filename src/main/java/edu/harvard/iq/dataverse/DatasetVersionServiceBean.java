package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.ingest.IngestUtil;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import static edu.harvard.iq.dataverse.batch.jobs.importer.filesystem.FileRecordJobListener.SEP;
import edu.harvard.iq.dataverse.batch.util.LoggingUtil;
import edu.harvard.iq.dataverse.search.SolrSearchResult;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.MarkupChecker;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
    
/**
 *
 * @author skraffmiller
 */
@Stateless
@Named
public class DatasetVersionServiceBean implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DatasetVersionServiceBean.class.getCanonicalName());

    private static final SimpleDateFormat logFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss");
    
    @EJB
    DatasetServiceBean datasetService;
    
    @EJB
    DataFileServiceBean datafileService;
    
    @EJB
    SettingsServiceBean settingsService;
    
    @EJB
    AuthenticationServiceBean authService;
    
    @EJB
    SystemConfig systemConfig;

    @EJB
    IndexServiceBean indexService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    /**
     *  Response to a successful request from the DatasetPage
     * 
     *  Used to help display messages in the cases when:
     *      (1) A specific dataset version (including a DRAFT) is requested
     *      (2) A different dataset is returned b/c the first one doesn't exist
     * 
     *  Simple example:  "DRAFT" requested but no longer exists b/c it
     *      was published and is now version "1.0"
     */
    public class RetrieveDatasetVersionResponse{
    
        private DatasetVersion datasetVersionForResponse;
        private boolean wasSpecificVersionRequested = false;
        private boolean didSpecificVersionMatch = false;
        private String actualVersion = null;  
        private String requestedVersion = null;  
        
        public RetrieveDatasetVersionResponse(DatasetVersion datasetVersion, String requestedVersion){
            if (datasetVersion == null){
                throw new IllegalArgumentException("datasetVersion cannot be null");
            }
            //logger.fine("RetrieveDatasetVersionResponse: datasetVersion: " + datasetVersion.getSemanticVersion() + " requestedVersion: " + requestedVersion);
            //logger.fine("chosenVersion id: " + datasetVersion.getId() + "  getFriendlyVersionNumber: " + datasetVersion.getFriendlyVersionNumber());
            this.datasetVersionForResponse = datasetVersion;
            
            this.actualVersion = datasetVersion.getSemanticVersion();
            this.requestedVersion = requestedVersion;
            this.checkVersion();
        }
        
        public String getDifferentVersionMessage(){
            
            if (this.wasSpecificVersionRequested && !this.didSpecificVersionMatch){
                String userMsg;
                if (DatasetVersionServiceBean.this.isVersionAskingForDraft(this.requestedVersion)){
                    userMsg = BundleUtil.getStringFromBundle("file.viewDiffDialog.msg.draftNotFound");
                }else{
                    userMsg = BundleUtil.getStringFromBundle("file.viewDiffDialog.msg.versionNotFound", Arrays.asList(MarkupChecker.escapeHtml(this.requestedVersion)));
                }
                
                if (DatasetVersionServiceBean.this.isVersionAskingForDraft(this.actualVersion)){
                    userMsg += BundleUtil.getStringFromBundle("file.viewDiffDialog.msg.draftFound");
                }else{
                    userMsg += BundleUtil.getStringFromBundle("file.viewDiffDialog.msg.versionFound", Arrays.asList(this.actualVersion));
                }
                
                return userMsg;
            }
            return null;
        }
        
        private void checkVersion(){
            if (actualVersion==null){   // this shouldn't happen
                return;
            }
            //logger.fine("check version. requested: " + this.requestedVersion + " returned: " + actualVersion);
            // This may often be the case if version is not specified
            //
            if (requestedVersion == null || requestedVersion.isEmpty()){
                this.wasSpecificVersionRequested = false;         
                return;
            }

            this.wasSpecificVersionRequested = true;                

            this.didSpecificVersionMatch = this.requestedVersion.equalsIgnoreCase(actualVersion);
            
        }
        
        public boolean wasRequestedVersionRetrieved(){
            return !(this.wasSpecificVersionRequested && !this.didSpecificVersionMatch);
        }
        
        
        public DatasetVersion getDatasetVersion(){
            return this.datasetVersionForResponse;
        }                
    } // end RetrieveDatasetVersionResponse
    
    public DatasetVersion find(Object pk) {
        return em.find(DatasetVersion.class, pk);
    }

    public DatasetVersion findByFriendlyVersionNumber(Long datasetId, String friendlyVersionNumber) {
        Long majorVersionNumber = null;
        Long minorVersionNumber = null;

        String[] versions = friendlyVersionNumber.split("\\.");
        try {
            if (versions.length == 1) {
                majorVersionNumber = Long.parseLong(versions[0]);
            } else if (versions.length == 2) {
                majorVersionNumber = Long.parseLong(versions[0]);
                minorVersionNumber = Long.parseLong(versions[1]);
            } else {
                return null;
            }
        } catch (NumberFormatException n) {
            return null;
        }
        if (minorVersionNumber != null) {
            String queryStr = "SELECT v from DatasetVersion v where v.dataset.id = :datasetId  and v.versionNumber= :majorVersionNumber and v.minorVersionNumber= :minorVersionNumber";
            DatasetVersion foundDatasetVersion = null;
            try {
                Query query = em.createQuery(queryStr);
                query.setParameter("datasetId", datasetId);
                query.setParameter("majorVersionNumber", majorVersionNumber);
                query.setParameter("minorVersionNumber", minorVersionNumber);
                foundDatasetVersion = (DatasetVersion) query.getSingleResult();
            } catch (javax.persistence.NoResultException e) {
                logger.warning("no ds version found: " + datasetId + " " + friendlyVersionNumber);
                // DO nothing, just return null.
            }
            return foundDatasetVersion;
        }

        if (majorVersionNumber == null && minorVersionNumber == null) {
            return null;
        }

        if (majorVersionNumber != null && minorVersionNumber == null) {
            try {
                TypedQuery<DatasetVersion> typedQuery = em.createQuery("SELECT v from DatasetVersion v where v.dataset.id = :datasetId  and v.versionNumber= :majorVersionNumber", DatasetVersion.class);
                typedQuery.setParameter("datasetId", datasetId);
                typedQuery.setParameter("majorVersionNumber", majorVersionNumber);
                DatasetVersion retVal = null;
                List<DatasetVersion> versionsList = typedQuery.getResultList();
                for (DatasetVersion dsvTest : versionsList) {
                    if (retVal == null) {
                        retVal = dsvTest;
                    } else {
                        if (retVal.getMinorVersionNumber().intValue() < dsvTest.getMinorVersionNumber().intValue()) {
                            retVal = dsvTest;
                        }
                    }
                }
                return retVal;
            } catch (javax.persistence.NoResultException e) {
                logger.warning("no ds version found: " + datasetId + " " + friendlyVersionNumber);
                // DO nothing, just return null.
            }

        }
        return null;
    }

    
    /** 
     *   Parse a Persistent Id and return as 3 strings.        
     * 
     *   Example: 1.0, 1.1. 3.4, etc.
     * 
     * @param version
     * @return Long[] with [ major_version, minor_version ] 
     *                  - either or both may be null
     */
     public Long[] parseVersionNumber(String version){
        if (version == null){
            return null;            
        }
        
        Long majorVersion;
        Long minorVersion = null;
        
        String[] vparts = version.split("\\.");
        if (vparts.length == 1){
            try{
                majorVersion = Long.parseLong(vparts[0]);
            }catch (NumberFormatException n) {
                return null;
            }
        }else if (vparts.length == 2){
            try{
                majorVersion = Long.parseLong(vparts[0]);
                minorVersion = Long.parseLong(vparts[1]);
            }catch (NumberFormatException n) {
                return null;
            }            
        }else{
            return null;
        }
        
        Long versionNumbers[] = { majorVersion, minorVersion };
        
        return versionNumbers;
    }
         

    
    private void msg(String s){
        //logger.fine(s);
    }
    
    /**
     * Does the version identifier in the URL ask for a "DRAFT"?
     * 
     * @param version
     * @return boolean
     */
    public boolean isVersionAskingForDraft(String version){
        
        if (version == null){
            return false;
        }
    
        return version.toUpperCase().equals(VersionState.DRAFT.toString());
    }
    
    private String getDatasetVersionBasicQuery(String identifierClause, String extraClause){
        
        if (identifierClause == null){
            return null;
        }
        
        if (extraClause == null){
            extraClause = "";
        }
                          
        String queryStr = "SELECT dv.* FROM DatasetVersion dv";
        queryStr += " INNER JOIN Dataset ds";
        queryStr += " ON dv.dataset_id=ds.id";
        queryStr += identifierClause;      // either persistentId or id
        queryStr += extraClause;   // may be an empty string
        queryStr += " ORDER BY versionNumber DESC, minorVersionNumber DESC";
        queryStr += " LIMIT 1;";
         
        return queryStr;
        
    }
    
    public String getContributorsNames(DatasetVersion version) {
        String contNames = "";
        for (String id : version.getVersionContributorIdentifiers()) {
            id = id.startsWith("@") ? id.substring(1) : id;
            AuthenticatedUser au = authService.getAuthenticatedUser(id);
            if (au != null) {
                if (contNames.isEmpty()) {
                    contNames = au.getName();
                } else {
                    contNames = contNames + ", " + au.getName();
                }
            }
        }
        return contNames;
    } 
    
    public List<DatasetVersionUser> getDatasetVersionUsersByAuthenticatedUser(AuthenticatedUser user){
        
        TypedQuery<DatasetVersionUser> typedQuery = em.createQuery("SELECT u from DatasetVersionUser u where u.authenticatedUser.id = :authenticatedUserId", DatasetVersionUser.class);
                typedQuery.setParameter("authenticatedUserId", user.getId());
                return typedQuery.getResultList();        

    }

    /**
     * Query to return the last Released DatasetVersion by Persistent ID
     * 
     * @param identifierClause  - query clause to retrieve via DatasetVersion.Id or DatasetVersion.persistentId 
     * @return String fullQuery
     */
    private String getLatestReleasedDatasetVersionQuery(String identifierClause){
        
        if (identifierClause == null){
            return null;
        }
        String releasedClause = " AND dv.versionstate = '" + VersionState.RELEASED.toString() + "'";

        return getDatasetVersionBasicQuery(identifierClause, releasedClause);
        
    }

     /**
     * Query to return a DatasetVersion by Specific Version
     * 
     * @param identifierClause  - query clause to retrieve via DatasetVersion.Id or DatasetVersion.persistentId 
     * @return String fullQuery
     */
    private String getNumericDatasetVersionQueryByIdentifier(String identifierClause, Long majorVersion, Long minorVersion){
        
        if (identifierClause == null){
            return null;
        }
        String extraQueryClause = "";

        
        // For an exact version: retrieve either RELEASED or DEACCESSIONED
        //
        if (majorVersion != null && minorVersion != null){
            extraQueryClause += " AND dv.versionNumber= " + majorVersion;
            extraQueryClause += " AND dv.minorVersionNumber= " + minorVersion;
            extraQueryClause += " AND (dv.versionstate = '" + VersionState.RELEASED.toString() + "' or dv.versionstate = '" + VersionState.DEACCESSIONED.toString() + "')";
        } else {
            // Last released major version
            //
            extraQueryClause += " AND dv.versionNumber= " + majorVersion;
            extraQueryClause += " AND dv.versionstate = '" + VersionState.RELEASED.toString() + "'";
        }
        
        return getDatasetVersionBasicQuery(identifierClause, extraQueryClause);
        
    }
    
    /**
     * Query to return a Draft DatasetVersion by Persistent ID
     * 
     * @param identifierClause  - query clause to retrieve via DatasetVersion.Id or DatasetVersion.persistentId 
     * @return String fullQuery
     */
    private String getDraftDatasetVersionQuery(String identifierClause){
        
        if (identifierClause == null){
            return null;
        }
        String draftClause = " AND dv.versionstate = '" + VersionState.DRAFT.toString() + "'";

        return getDatasetVersionBasicQuery(identifierClause, draftClause);
        
    }
    
        /**
     * Query to return a DEACCESSIONED DatasetVersion by Persistent ID
     * 
     * @param identifierClause  - query clause to retrieve via DatasetVersion.Id or DatasetVersion.persistentId 
     * @return String fullQuery
     */
    private String getDeaccessionedDatasetVersionQuery(String identifierClause){
        
        if (identifierClause == null){
            return null;
        }
        String draftClause = " AND dv.versionstate = '" + VersionState.DEACCESSIONED.toString() + "'";

        return getDatasetVersionBasicQuery(identifierClause, draftClause);
        
    }
    
    
    
    
    /**
     * Execute a query to return DatasetVersion
     * 
     * @param queryString
     * @return 
     */
    private DatasetVersion getDatasetVersionByQuery(String queryString){
        msg("getDatasetVersionByQuery queryString: " + queryString);
        if (queryString == null){
            return null;
        }
        
        // Inexact check to see if one of the selected tables is DATASETVERSION
        //
        if (!queryString.toUpperCase().contains("FROM DATASETVERSION")){
            throw new IllegalArgumentException("This query does not select from the table DATASETVERSION");
        }
        
        try{
            Query query = em.createNativeQuery(queryString, DatasetVersion.class);           
            DatasetVersion ds = (DatasetVersion) query.getSingleResult();
            
            msg("Found: " + ds);
            return ds;
            
        } catch (javax.persistence.NoResultException e) {
            msg("DatasetVersion not found: " + queryString);
            logger.log(Level.FINE, "DatasetVersion not found: {0}", queryString);
            return null;
         } catch (EJBException e) {
             logger.log(Level.WARNING, "EJBException exception: {0}", e.getMessage());
             return null;
         }
    } // end getDatasetVersionByQuery
    
    
    
    
    public DatasetVersion retrieveDatasetVersionByIdentiferClause(String identifierClause, String version){
        
        if (identifierClause == null){
            return null;
        }
        
        DatasetVersion chosenVersion;
        
        /* --------------------------------------------
            (1) Scenario: User asking for a DRAFT?
                - (1a) Look for draft
                - (1b) Not found: Get Latest Release
                - Permissions: check on DatasetPage
        -------------------------------------------- */
        if (DatasetVersionServiceBean.this.isVersionAskingForDraft(version)){

            // (1a) Try to retrieve a draft
            msg("(1a) Try to retrieve a draft");
            String draftQuery = this.getDraftDatasetVersionQuery(identifierClause);            
            chosenVersion = this.getDatasetVersionByQuery(draftQuery);

            // Draft Exists! Return it!
            msg("Draft Exists! Return it!");
            if (chosenVersion != null){
                return chosenVersion;   // let DatasetPage check permissions
            }
            
            // (1b) No draft found - check for last released
            msg("(1b) No draft found - check for last released");
            String lastReleasedQuery = this.getLatestReleasedDatasetVersionQuery(identifierClause);
            chosenVersion = this.getDatasetVersionByQuery(lastReleasedQuery);
            
            return chosenVersion;  // This may be null -- let DatasetPage check
        }
        // END: User asking for a Draft
        
        /* --------------------------------------------
            (2) Scenario: Version specified
                - (2a) Look for major and minor version - RELEASE OR DEACCESSIONED
                    - OR Look for major version - RELEASE
                - (2c) Not found: look for latest released version
                - (2c) Not found: look for DEACCESSIONED
                - (2d) Not found: look for draft
                - Permissions: check on DatasetPage        
        
            (3) Scenario: No version specified
                - Same as (2c)(2d) above
                - Permissions: check on DatasetPage        
        -------------------------------------------- */
        Long[] versionNumbers = parseVersionNumber(version);
        if (versionNumbers != null && versionNumbers.length == 2){        // At least a major version found
            
            // (2a) Look for major and minor version - RELEASE OR DEACCESSIONED
            msg("(2a) Look for major and minor version -" + Arrays.toString(versionNumbers));
            String specificVersionQuery = this.getNumericDatasetVersionQueryByIdentifier(identifierClause, versionNumbers[0], versionNumbers[1]);            
            
            chosenVersion = this.getDatasetVersionByQuery(specificVersionQuery);
            if (chosenVersion != null){
                return chosenVersion;
            }
        }
        
        // (2b) Look for latest released version
        msg("(2b) Look for latest released version");        
        String latestVersionQuery = this.getLatestReleasedDatasetVersionQuery(identifierClause);
        //msg("latestVersionQuery: " + latestVersionQuery);
        chosenVersion = this.getDatasetVersionByQuery(latestVersionQuery);
        if (chosenVersion != null){
            return chosenVersion;
        }

        // (2c) Look for DEACCESSIONED
        msg("(2c) Look for draft");        
        String dQuery = this.getDeaccessionedDatasetVersionQuery(identifierClause);            
        //msg("draftQuery: " + draftQuery);
        chosenVersion = this.getDatasetVersionByQuery(dQuery);
        if (chosenVersion != null){
            return chosenVersion;
        }
        
        // (2d) Look for draft
        msg("(2d) Look for draft");        
        String draftQuery = this.getDraftDatasetVersionQuery(identifierClause);            
        //msg("draftQuery: " + draftQuery);
        chosenVersion = this.getDatasetVersionByQuery(draftQuery);

        return chosenVersion;   // This may be null -- let DatasetPage check

                  
    } // end: retrieveDatasetVersionByPersistentId
    
    
    /**
     * Find a DatasetVersion using the persisentID and version string
     * 
     * @param persistentId doi:10.5072/FK2/BYM3IW
     * @param version  "DRAFT", 1.0, 2, 3.4, null, etc
     * @return 
     */
    public RetrieveDatasetVersionResponse retrieveDatasetVersionByPersistentId(String persistentId, String version){
        
        msg("retrieveDatasetVersionByPersistentId: " + persistentId + " " + version);
        if (persistentId==null){
            return null;
        }
        
        /*        
            Parse the persistent id
        */
        GlobalId parsedId;
        try{
            parsedId = PidUtil.parseAsGlobalID(persistentId);   // [ protocol, authority, identifier]
        } catch (IllegalArgumentException e){
            logger.log(Level.WARNING, "Failed to parse persistentID: {0}", persistentId);
            return null;
        }
        
        String identifierClause = " AND ds.protocol= '" + parsedId.getProtocol() + "'"; 
        identifierClause += " AND ds.authority = '" + parsedId.getAuthority() + "'"; 
        identifierClause += " AND ds.identifier = '" + parsedId.getIdentifier() + "'"; 
        

        DatasetVersion ds = retrieveDatasetVersionByIdentiferClause(identifierClause, version);
        
        if (ds != null){
            msg("retrieved dataset: " + ds.getId() + " semantic: " + ds.getSemanticVersion());
            return new RetrieveDatasetVersionResponse(ds, version);
        }
        
        return null;
        
    }
    
    public RetrieveDatasetVersionResponse selectRequestedVersion(List<DatasetVersion> versions, String versionTag) {
        
        Long[] versionNumbers = parseVersionNumber(versionTag);
        Long majorVersion = null; 
        Long minorVersion = null; 
        
        if (versionNumbers != null && versionNumbers.length == 2) { 
            majorVersion = versionNumbers[0];
            minorVersion = versionNumbers[1];
        }
        
        for (DatasetVersion version : versions) {

            if (this.isVersionAskingForDraft(versionTag)) {

                if (version.isDraft()) {
                    return new RetrieveDatasetVersionResponse(version, versionTag);
                }

            } else if (majorVersion != null && minorVersion != null) {
                if (majorVersion.equals(version.getVersionNumber()) && minorVersion.equals(version.getMinorVersionNumber()) && (version.isReleased() || version.isDeaccessioned())) {
                    return new RetrieveDatasetVersionResponse(version, versionTag);
                }
            } else if (majorVersion != null) {
                if (majorVersion.equals(version.getVersionNumber()) && version.isReleased()) {
                    return new RetrieveDatasetVersionResponse(version, versionTag);
                }
                //
            }
        }
        
        // second pass - grab the first  released version:
        
        for (DatasetVersion version : versions) {
            if (version.isReleased()) {
                return new RetrieveDatasetVersionResponse(version, versionTag);
            }
        }
        
        // third pass - grab the first (latest!) deaccessioned version
        
        for (DatasetVersion version : versions) {
            if (version.isDeaccessioned()) {
                return new RetrieveDatasetVersionResponse(version, versionTag);
            }
        }
            
        // fourth pass -  draft is the last choice:
        
        for (DatasetVersion version : versions) {
            if (version.isDraft()) {
                return new RetrieveDatasetVersionResponse(version, versionTag);
            }
        }    
        
        return null; 
    }
     
     /**
     * Find a DatasetVersion using the persisentID and version string
     * 
     * @param datasetId
     * @param version  "DRAFT", 1.0, 2, 3.4, null, etc
     * @return 
     */
    public RetrieveDatasetVersionResponse retrieveDatasetVersionById(Long datasetId, String version){
        msg("retrieveDatasetVersionById: " + datasetId + " " + version);
        
        DatasetVersion ds = getDatasetVersionById(datasetId, version);
        
        if (ds != null){
            return new RetrieveDatasetVersionResponse(ds, version);
        }
        
        return null;

          
    } // end: retrieveDatasetVersionById
    
    public DatasetVersion getDatasetVersionById(Long datasetId, String version){
        msg("retrieveDatasetVersionById: " + datasetId + " " + version);
        if (datasetId==null){
            return null;
        }        
        
        String identifierClause = this.getIdClause(datasetId);

        DatasetVersion ds = retrieveDatasetVersionByIdentiferClause(identifierClause, version);
        
        return ds;

          
    } // end: getDatasetVersionById
    
    public DatasetVersion getLatestReleasedVersionFast(Long datasetId) {
        String identifierClause = this.getIdClause(datasetId);
        String latestVersionQuery = this.getLatestReleasedDatasetVersionQuery(identifierClause);
        return this.getDatasetVersionByQuery(latestVersionQuery);
    }
    
    private String getIdClause(Long datasetId) {
        return " AND ds.id = " + datasetId;
    }
    
     /**
     * Find a DatasetVersion using the dataset versionId
     * 
     * @param versionId DatasetVersion id
     * @return 
     */
    public RetrieveDatasetVersionResponse retrieveDatasetVersionByVersionId(Long versionId){
        //msg("retrieveDatasetVersionById: " + datasetId + " " + versionId);
        if (versionId==null ){
            return null;
        }        
        
        // Try versionId - release state doesn't matte
        //
        String retrieveSpecifiedDSVQuery = "SELECT dv.* FROM DatasetVersion dv WHERE dv.id = " + versionId;
        
        DatasetVersion chosenVersion = this.getDatasetVersionByQuery(retrieveSpecifiedDSVQuery);
        if (chosenVersion != null) {
            return new RetrieveDatasetVersionResponse(chosenVersion, "");
        }
        return null;          
    } // end: retrieveDatasetVersionByVersionId
    
    // This is an optimized, native query-based method for picking an image 
    // that can be used as the thumbnail for a given dataset/version. 
    // It is primarily designed to be used when thumbnails are requested
    // from the Dataverse page, which is Solr search object based; meaning we 
    // may not have the Dataset, DatasetVersion, etc. entities initialized.
    // And since we may need to look up/generate these thumbnails for a large 
    // number of search results, actually instantiating full entities for each
    // one may be prohibitively expensive. It is also used by the DatasetPage, 
    // when in read-only, optimized mode, when we similarly try to serve the 
    // page while minimizing full lookup of entities via EJB. 
    // (in both cases above the method is called via ThumbnailServiceWrapper)
    
    public Long getThumbnailByVersionId(Long versionId) {
        if (versionId == null) {
            return null;
        }

        Long thumbnailFileId;

        // First, let's see if there are thumbnails that have already been 
        // generated:
        try {
            thumbnailFileId = (Long) em.createNativeQuery("SELECT df.id "
                    + "FROM datafile df, filemetadata fm, datasetversion dv, dvobject o "
                    + "WHERE dv.id = " + versionId + " "
                    + "AND df.id = o.id "
                    + "AND fm.datasetversion_id = dv.id "
                    + "AND fm.datafile_id = df.id "
                    + "AND df.restricted = false "
                    + "AND df.embargo_id is null "
                    + "AND o.previewImageAvailable = true "
                    + "ORDER BY df.id LIMIT 1;").getSingleResult();
        } catch (Exception ex) {
            thumbnailFileId = null;
        }

        if (thumbnailFileId != null) {
            logger.fine("DatasetVersionService,getThumbnailByVersionid(): found already generated thumbnail for version " + versionId + ": " + thumbnailFileId);
            assignDatasetThumbnailByNativeQuery(versionId, thumbnailFileId);
            return thumbnailFileId;
        }

        if (!systemConfig.isThumbnailGenerationDisabledForImages()) {
            // OK, let's try and generate an image thumbnail!
            long imageThumbnailSizeLimit = systemConfig.getThumbnailSizeLimitImage();

            try {
                thumbnailFileId = (Long) em.createNativeQuery("SELECT df.id "
                        + "FROM datafile df, filemetadata fm, datasetversion dv, dvobject o "
                        + "WHERE dv.id = " + versionId + " "
                        + "AND df.id = o.id "
                        + "AND fm.datasetversion_id = dv.id "
                        + "AND fm.datafile_id = df.id "
                        // + "AND o.previewImageAvailable = false "
                        + "AND df.restricted = false "
                        + "AND df.embargo_id is null "
                        + "AND df.contenttype LIKE 'image/%' "
                        + "AND NOT df.contenttype = 'image/fits' "
                        + "AND df.filesize < " + imageThumbnailSizeLimit + " "
                        + "ORDER BY df.filesize ASC LIMIT 1;").getSingleResult();
            } catch (Exception ex) {
                thumbnailFileId = null;
            }

            if (thumbnailFileId != null) {
                logger.fine("obtained file id: " + thumbnailFileId);
                DataFile thumbnailFile = datafileService.find(thumbnailFileId);
                if (thumbnailFile != null) {
                    if (datafileService.isThumbnailAvailable(thumbnailFile)) {
                        assignDatasetThumbnailByNativeQuery(versionId, thumbnailFileId);
                        return thumbnailFileId;
                    }
                }
            }
        }

        // And if that didn't work, try the same thing for PDFs:
        if (!systemConfig.isThumbnailGenerationDisabledForPDF()) {
            // OK, let's try and generate an image thumbnail!
            long imageThumbnailSizeLimit = systemConfig.getThumbnailSizeLimitPDF();
            try {
                thumbnailFileId = (Long) em.createNativeQuery("SELECT df.id "
                        + "FROM datafile df, filemetadata fm, datasetversion dv, dvobject o "
                        + "WHERE dv.id = " + versionId + " "
                        + "AND df.id = o.id "
                        + "AND fm.datasetversion_id = dv.id "
                        + "AND fm.datafile_id = df.id "
                        // + "AND o.previewImageAvailable = false "
                        + "AND df.restricted = false "
                        + "AND df.embargo_id is null "
                        + "AND df.contenttype = 'application/pdf' "
                        + "AND df.filesize < " + imageThumbnailSizeLimit + " "
                        + "ORDER BY df.filesize ASC LIMIT 1;").getSingleResult();
            } catch (Exception ex) {
                thumbnailFileId = null;
            }

            if (thumbnailFileId != null) {
                DataFile thumbnailFile = datafileService.find(thumbnailFileId);
                if (thumbnailFile != null) {
                    if (datafileService.isThumbnailAvailable(thumbnailFile)) {
                        assignDatasetThumbnailByNativeQuery(versionId, thumbnailFileId);
                        return thumbnailFileId;
                    }
                }
            }
        }

        return null;
    }
    
    private void assignDatasetThumbnailByNativeQuery(Long versionId, Long dataFileId) {
        try {
            em.createNativeQuery("UPDATE dataset SET thumbnailfile_id=" + dataFileId + " WHERE id in (SELECT dataset_id FROM datasetversion WHERE id=" + versionId + ")").executeUpdate();
        } catch (Exception ex) {
            // it's ok to just ignore... 
        }
    }
    
    public void writeEditVersionLog(DatasetVersionDifference dvd, AuthenticatedUser au) {

        String logDir = System.getProperty("com.sun.aas.instanceRoot") + SEP + "logs" + SEP + "edit-drafts" + SEP;
        String identifier = dvd.getOriginalVersion().getDataset().getIdentifier();
        identifier = identifier.substring(identifier.indexOf("/") + 1);
        String datasetId = dvd.getOriginalVersion().getDataset().getId().toString();
        String summary = au.getFirstName() + " " + au.getLastName() + " (" + au.getIdentifier() + ") updated " + dvd.getEditSummaryForLog();
        String logTimestamp = logFormatter.format(new Date());
        String fileName = "/edit-draft-" + datasetId + "-" + identifier + "-" + logTimestamp + ".txt";
        LoggingUtil.saveLogFile(summary, logDir, fileName);
        
    }
    
    public void populateDatasetSearchCard(SolrSearchResult solrSearchResult) {
        Long dataverseId = Long.parseLong(solrSearchResult.getParent().get("id"));
        Long datasetVersionId = solrSearchResult.getDatasetVersionId();
        Long datasetId = solrSearchResult.getEntityId();
        
        if (dataverseId == 0) {
            return;
        }
        
        Object[] searchResult;
        
        try {
            if (datasetId != null) {
                searchResult = (Object[]) em.createNativeQuery("SELECT t0.VERSIONSTATE, t1.ALIAS, t2.THUMBNAILFILE_ID, t2.USEGENERICTHUMBNAIL, t3.STORAGEIDENTIFIER FROM DATASETVERSION t0, DATAVERSE t1, DATASET t2, DVOBJECT t3 WHERE t0.ID = " 
                        + datasetVersionId 
                        + " AND t1.ID = " 
                        + dataverseId
                        + " AND t2.ID = "
                        + datasetId
                        + " AND t2.ID = t3.ID").getSingleResult()
                        
                        ;
            } else {
                // Why is this method ever called with dataset_id = null? -- L.A.
                searchResult = (Object[]) em.createNativeQuery("SELECT t0.VERSIONSTATE, t1.ALIAS FROM DATASETVERSION t0, DATAVERSE t1 WHERE t0.ID = " + datasetVersionId + " AND t1.ID = " + dataverseId).getSingleResult();
            }
        } catch (Exception ex) {
            return;
        }

        if (searchResult == null) {
            return;
        }
        
        if (searchResult[0] != null) {
            String versionState = (String)searchResult[0];
            if ("DEACCESSIONED".equals(versionState)) {
                solrSearchResult.setDeaccessionedState(true);
            }
        }
        
        /**
          * @todo (from pdurbin) can a dataverse alias ever be null?
          */
        
        if (searchResult[1] != null) {
            solrSearchResult.setDataverseAlias((String) searchResult[1]);
        }
        
        if (searchResult.length == 5) {
            Dataset datasetEntity = new Dataset();
            String globalIdentifier = solrSearchResult.getIdentifier();
            GlobalId globalId = PidUtil.parseAsGlobalID(globalIdentifier);

            datasetEntity.setProtocol(globalId.getProtocol());
            datasetEntity.setAuthority(globalId.getAuthority());
            datasetEntity.setIdentifier(globalId.getIdentifier());
            if (searchResult[4] != null) {
                datasetEntity.setStorageIdentifier(searchResult[4].toString());
            }
            solrSearchResult.setEntity(datasetEntity);
            if (searchResult[2] != null) {
                // This is the image file specifically assigned as the "icon" for
                // the dataset:
                Long thumbnailFile_id = (Long) searchResult[2];
                if (thumbnailFile_id != null) {
                    DataFile thumbnailFile;
                    try {
                        thumbnailFile = datafileService.findCheapAndEasy(thumbnailFile_id);
                    } catch (Exception ex) {
                        thumbnailFile = null;
                    }

                    if (thumbnailFile != null) {
                        ((Dataset) solrSearchResult.getEntity()).setThumbnailFile(thumbnailFile);
                    }
                }
            }
            if (searchResult[3] != null) {
                ((Dataset)solrSearchResult.getEntity()).setUseGenericThumbnail((Boolean) searchResult[3]);
            } else {
                ((Dataset)solrSearchResult.getEntity()).setUseGenericThumbnail(false);
            }
        }
    }
    
    /**
     * Return a list of the checksum Strings for files in the specified DatasetVersion
     * 
     * This is used to help check for duplicate files within a DatasetVersion
     * 
     * @param datasetVersion
     * @return a list of checksum Strings for files in the specified DatasetVersion
     */
    public List<String> getChecksumListForDatasetVersion(DatasetVersion datasetVersion) {

        if (datasetVersion == null){
            throw new NullPointerException("datasetVersion cannot be null");
        }

        String query = "SELECT df.md5 FROM datafile df, filemetadata fm WHERE fm.datasetversion_id = " + datasetVersion.getId() + " AND fm.datafile_id = df.id;";

        logger.log(Level.FINE, "query: {0}", query);
        Query nativeQuery = em.createNativeQuery(query);

        return nativeQuery.getResultList();
    }
    
        
    /**
     * Check for the existence of a single checksum value within a DatasetVersion's files
     * 
     * @param datasetVersion
     * @param selectedChecksum
     * @return 
     */
    public boolean doesChecksumExistInDatasetVersion(DatasetVersion datasetVersion, String selectedChecksum) {
        if (datasetVersion == null){
            throw new NullPointerException("datasetVersion cannot be null");
        }
        
        String query = "SELECT df.md5 FROM datafile df, filemetadata fm" 
                + " WHERE fm.datasetversion_id = " + datasetVersion.getId() 
                + " AND fm.datafile_id = df.id"
                + " AND df.md5 = '" + selectedChecksum + "';";
        
        Query nativeQuery = em.createNativeQuery(query);
        List<?> checksumList = nativeQuery.getResultList();

        return !checksumList.isEmpty();
    }
        
    
    public List<HashMap<String, Object>> getBasicDatasetVersionInfo(Dataset dataset){
        
        if (dataset == null){
            throw new NullPointerException("dataset cannot be null");
        }
        
        String query = "SELECT id, dataset_id, releasetime, versionnumber,"
                    + " minorversionnumber, versionstate, versionnote" 
                    + " FROM datasetversion"
                    + " WHERE dataset_id = " + dataset.getId()
                    + " ORDER BY versionnumber DESC,"
                    + " minorversionnumber DESC," 
                    + " versionstate;";
        msg("query: " + query);
        Query nativeQuery = em.createNativeQuery(query);
        List<Object[]> datasetVersionInfoList = nativeQuery.getResultList();

        List<HashMap<String, Object>> hashList = new ArrayList<>();
        
        HashMap<String, Object> mMap = new HashMap<>();
        for (Object[] dvInfo : datasetVersionInfoList) {
            mMap = new HashMap<>();
            mMap.put("datasetVersionId", dvInfo[0]);
            mMap.put("datasetId", dvInfo[1]);
            mMap.put("releaseTime", dvInfo[2]);
            mMap.put("versionnumber", dvInfo[3]);
            mMap.put("minorversionnumber", dvInfo[4]);
            mMap.put("versionstate", dvInfo[5]);
            mMap.put("versionnote", dvInfo[6]);
            hashList.add(mMap);
        }
        return hashList;
    } // end getBasicDatasetVersionInfo
    
    
    
    public HashMap getFileMetadataHistory(DataFile df){
        
        if (df == null){
            throw new NullPointerException("DataFile 'df' cannot be null");
        }
        
        String rootFileIdClause = "";
        if (df.getRootDataFileId() != null){
            rootFileIdClause = " OR rootdatafileid = " + df.getRootDataFileId();
        }
        
        List<String> colsToRetrieve = Arrays.asList("df.id", "df.contenttype" 
                 , "df.filesize", "df.checksumtype", "df.checksumvalue"
                 , "fm.label", "fm.description", "fm.version"        
        );
                
        String colsToRetrieveString = StringUtils.join(colsToRetrieve, ",");
                
        
        String query = "SELECT " + colsToRetrieveString
                    + " FROM datafile df, filemetadata fm"
                    + " WHERE (df.id = " + df.getId()
                    + "    OR rootdatafileid = "  + df.getId()
                    + rootFileIdClause + ")"
                    + " AND fm.datafile_id = df.id"
                    + " ORDER BY df.id;";
        
        Query nativeQuery = em.createNativeQuery(query);
        List<Object[]> infoList = nativeQuery.getResultList();

        List<HashMap> hashList = new ArrayList<>();
        
        /*
        HashMap mMap;
        List<String> hashKeys = colsToRetrieve.stream()
                                  .map(String :: trim)  
          */                              

        /*
                                                        .map(x -> x.getTypeLabel())
w
                 return tagsToCheck.stream()
                        .filter(p -> p != null)         // no nulls
                        .map(String :: trim)            // strip strings
                        .filter(p -> p.length() > 0 )   // no empty strings
                        .distinct()                     // distinct
                        .collect(Collectors.toList());
                */        
        return null;/*
        for (Object[] dvInf: infoList) {
                        
            mMap = new HashMap();
            for(int idx=0; idx < colsToRetrieve.size(); idx++){
                String keyName = colsToRetrieve.get(idx);
                if ()
                mMap.put(colsToRetrieve.get(idx), dvInfo[idx]);
            }
            hashList.add(mMap);
        }
        return hashList;
        */
    }

    public JsonObjectBuilder fixMissingUnf(String datasetVersionId, boolean forceRecalculate) {
        JsonObjectBuilder info = Json.createObjectBuilder();
        if (datasetVersionId == null || datasetVersionId.isEmpty()) {
            info.add("message", "datasetVersionId was null or empty!");
            return info;
        }
        long dsvId = Long.parseLong(datasetVersionId);
        DatasetVersion datasetVersion = find(dsvId);
        if (datasetVersion == null) {
            info.add("message", "Could not find a dataset version based on datasetVersionId " + datasetVersionId + ".");
            return info;
        }
        if (!StringUtils.isBlank(datasetVersion.getUNF())) {
            info.add("message", "Dataset version (id=" + datasetVersionId + ") already has a UNF. Blank the UNF value in the database if you must change it.");
            return info;
        }
            
        List<String> fileUnfsInVersion = getFileUnfsInVersion(datasetVersion);
        if (fileUnfsInVersion.isEmpty()) {
            info.add("message", "Dataset version (id=" + datasetVersionId + ") has no tabular data files with UNF signatures. The version UNF will remain blank.");
            return info;
        }
        
        if (!forceRecalculate) {
            DatasetVersion previousDatasetVersion = getPreviousVersionWithUnf(datasetVersion);
            if (previousDatasetVersion != null) {
                List<String> fileUnfsInPreviousVersion = getFileUnfsInVersion(previousDatasetVersion);
                
                if (isFileUnfsIdentical(fileUnfsInVersion, fileUnfsInPreviousVersion)) {
                    datasetVersion.setUNF(previousDatasetVersion.getUNF());
                    DatasetVersion saved = em.merge(datasetVersion);
                    info.add("message", "Dataset version (id=" + datasetVersionId + ") has the same tabular file UNFs as a previous version. Assigned the UNF of the previous version without recalculation (" + previousDatasetVersion.getUNF() + "). Use the --forceRecalculate option if you insist on recalculating this UNF.");
                }
            }
        }
        
        // is the UNF still unset? 
        if (StringUtils.isBlank(datasetVersion.getUNF())) {
            IngestUtil.recalculateDatasetVersionUNF(datasetVersion);
            DatasetVersion saved = em.merge(datasetVersion);
            info.add("message", "New UNF value saved (" + saved.getUNF() + "). Reindexing dataset.");
        }

        // reindexing the dataset, to make sure the new UNF is in SOLR:
        boolean doNormalSolrDocCleanUp = true;
        indexService.asyncIndexDataset(datasetVersion.getDataset(), doNormalSolrDocCleanUp);
        return info;
    }
    
    private boolean isFileUnfsIdentical(List<String> fileUnfs1, List<String> fileUnfs2) {
        if (fileUnfs1.size() != fileUnfs2.size()) {
            return false;
        }
        
        for (int i = 0; i < fileUnfs1.size(); i++) {
            if (!fileUnfs1.get(i).equalsIgnoreCase(fileUnfs2.get(i))) {
                return false;
            }
        }
        
        return true;
    }
    
    private List<String> getFileUnfsInVersion(DatasetVersion datasetVersion) {
        ArrayList<String> fileUnfs = new ArrayList<>();
        
        Iterator<FileMetadata> fileMetadataIterator = datasetVersion.getFileMetadatas().iterator();
        
        while (fileMetadataIterator.hasNext()) {
            FileMetadata fileMetadata = fileMetadataIterator.next();
            
            String fileUnf = fileMetadata.getDataFile().getUnf();
            
            if (fileUnf != null && !StringUtils.isBlank(fileUnf)) {
                fileUnfs.add(fileUnf);
            }
        }
        
        if (fileUnfs.size() > 0) {
            Collections.sort(fileUnfs, String.CASE_INSENSITIVE_ORDER);
        }
        
        return fileUnfs;
    }
    
    private DatasetVersion getPreviousVersionWithUnf(DatasetVersion datasetVersion) {
        if (datasetVersion.getDataset().getVersions().size() < 2) {
            // this is the only version - so there's no previous version.
            return null;
        }
        
        Iterator<DatasetVersion> versionIterator = datasetVersion.getDataset().getVersions().iterator();
        boolean returnNext = false; 
        
        while (versionIterator.hasNext()) {
            DatasetVersion iteratedVersion = versionIterator.next();
            
            if (returnNext) {
                if (!StringUtils.isBlank(iteratedVersion.getUNF())) {
                    return iteratedVersion;
                }
            } else if (DatasetVersion.compareByVersion.compare(datasetVersion, iteratedVersion) == 0) {
                returnNext = true;
            }
        }
        
        return null;
    }
    
    /**
     * Merges the passed datasetversion to the persistence context.
     * @param ver the DatasetVersion whose new state we want to persist.
     * @return The managed entity representing {@code ver}.
     */
    public DatasetVersion merge( DatasetVersion ver ) {
        return em.merge(ver);
    }
    
    /**
     * Execute a query to return DatasetVersion
     * 
     * @param queryString
     * @return 
     */
    public List<DatasetVersion> getUnarchivedDatasetVersions(){
        
        try {
            List<DatasetVersion> dsl = em.createNamedQuery("DatasetVersion.findUnarchivedReleasedVersion", DatasetVersion.class).getResultList();
            return dsl;
        } catch (javax.persistence.NoResultException e) {
            logger.log(Level.FINE, "No unarchived DatasetVersions found: {0}");
            return null;
        } catch (EJBException e) {
            logger.log(Level.WARNING, "EJBException exception: {0}", e.getMessage());
            return null;
        }
    } // end getUnarchivedDatasetVersions
} // end class
