/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

    
/**
 *
 * @author skraffmiller
 */
@Stateless
@Named
public class DatasetVersionServiceBean implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DatasetVersionServiceBean.class.getCanonicalName());

    public final String DRAFT_VERSION = "DRAFT";
    
    @EJB
    DatasetServiceBean datasetService;
    
    @EJB
    SettingsServiceBean settingsService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public DatasetVersion find(Object pk) {
        return (DatasetVersion) em.find(DatasetVersion.class, pk);
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

        if (majorVersionNumber != null && minorVersionNumber != null) {
            String queryStr = "SELECT v from DatasetVersion v where v.dataset.id = :datasetId  and v.versionNumber= :majorVersionNumber and v.minorVersionNumber= :minorVersionNumber";
            DatasetVersion foundDatasetVersion = null;
            try {
                Query query = em.createQuery(queryStr);
                query.setParameter("datasetId", datasetId);
                query.setParameter("majorVersionNumber", majorVersionNumber);
                query.setParameter("minorVersionNumber", minorVersionNumber);
                foundDatasetVersion = (DatasetVersion) query.getSingleResult();
            } catch (javax.persistence.NoResultException e) {
                System.out.print("no ds version found: " + datasetId + " " + friendlyVersionNumber);
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
                System.out.print("no ds version found: " + datasetId + " " + friendlyVersionNumber);
                // DO nothing, just return null.
            }

        }

        return null;
    }

    /** 
     *   Parse a Persistent Id and return as 3 strings.        
     * 
     *   Example: doi:10.5072/FK2/BYM3IW
     *       protocol: doi
     *       authority: 10.5072/FK2
     *       identifier: BYM3IW
     * 
     * @param persistentId
     * @return String[] with [ protocol, authority, identifier]
     */
    public String[] parsePersistentId(String persistentId){

        if (persistentId==null){
            return null;
        } 
        
        String doiSeparator = settingsService.getValueForKey(SettingsServiceBean.Key.DoiSeparator, "/");
        
        // Looking for this split  
        //  doi:10.5072/FK2/BYM3IW => (doi) (10.5072/FK2/BYM3IW)
        String[] items = persistentId.split(":");
        if (items.length != 2){
            return null;
        }
        String protocol = items[0];
        
        /* Looking for next split  
          10.5072/FK2/BYM3IW => (10.5072) (FK2) (BYM3IW)
        */
        String[] pieces = items[1].split(doiSeparator);
        if (pieces.length != 3){
            return null;
        }
        
        String authority = pieces[0] + doiSeparator + pieces[1]; // "10.5072/FK2"
        String identifier = pieces[2]; // "BYM3IW"
        
        String persistentIdParts[] = { protocol, authority, identifier };
        
        return persistentIdParts;

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
        //System.out.println(s);
    }
    
    /**
     * Does the version identifier in the URL ask for a "DRAFT"?
     * 
     * @param version
     * @return boolean
     */
    private boolean isVersionAskingForDraft(String version){
        
        if (version == null){
            return false;
        }
    
        return version.toUpperCase().equals(this.DRAFT_VERSION);
    }
    
    private String getDatasetVersionBasicQuery(String identifierClause, String extraClause){
        
        if (identifierClause == null){
            return null;
        }
        
        if (extraClause == null){
            extraClause = "";
        }
                         
        String queryStr = "SELECT * FROM DatasetVersion dv";
        queryStr += " INNER JOIN Dataset ds";
        queryStr += " ON dv.dataset_id=ds.id";
        queryStr += identifierClause;      // either persistentId or id
        queryStr += extraClause;   // may be an empty string
        queryStr += " ORDER BY versionNumber DESC, minorVersionNumber DESC";
        queryStr += " LIMIT 1;";
         
        return queryStr;
        
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
        String releasedClause = " AND dv.versionstate = 'RELEASED'";

        return getDatasetVersionBasicQuery(identifierClause, releasedClause);
        
    }

     /**
     * Query to return a DatasetVersion by Specific Version
     * 
     * @param identifierClause  - query clause to retrieve via DatasetVersion.Id or DatasetVersion.persistentId 
     * @return String fullQuery
     */
    private String getNumericDatasetVersionQueryByPersistentId(String identifierClause, Long majorVersion, Long minorVersion){
        
        if (identifierClause == null){
            return null;
        }
        String extraQueryClause = " AND dv.versionstate = 'RELEASED'";

        if (majorVersion != null){
            extraQueryClause += " AND dv.versionNumber= " + majorVersion;
            
            if (minorVersion != null){
                extraQueryClause += " AND dv.minorVersionNumber= " + minorVersion;
            }
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
        String draftClause = " AND dv.versionstate = 'DRAFT'";

        return getDatasetVersionBasicQuery(identifierClause, draftClause);
        
    }
    
    /**
     * Execute a query to return DatasetVersion
     * 
     * @param queryString
     * @return 
     */
    private DatasetVersion getDatasetVersionByQuery(String queryString){

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
            logger.log(Level.WARNING, "DatasetVersion not found: {0}", queryString);
            return null;
         }catch (EJBException e) {
             logger.log(Level.SEVERE, "EJBException exception: {0}", e.getMessage());
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
        if (this.isVersionAskingForDraft(version)){

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
                - (2a) Look for specified version
                - (2b) Not found: look for latest released version
                - (2c) Not found: look for draft
                - Permissions: check on DatasetPage        
        
            (3) Scenario: No version specified
                - Same as (2b)(2c) above
        -------------------------------------------- */
        Long[] versionNumbers = parseVersionNumber(version);
        if (versionNumbers != null && versionNumbers.length == 2){        // At least a major version found
            
            // (2a) Try to retrieve  specific version.  (..., majorVersion, minorVersion)
            msg("(2a) Try to retrieve  specific version. " + Arrays.toString(versionNumbers));
            String specificVersionQuery = this.getNumericDatasetVersionQueryByPersistentId(identifierClause, versionNumbers[0], versionNumbers[1]);            
            msg("specificVersionQuery: " + specificVersionQuery);
            chosenVersion = this.getDatasetVersionByQuery(specificVersionQuery);
            if (chosenVersion != null){
                return chosenVersion;
            }
        }
        
        // (2b) Look for latest released version
        msg("(2b) Look for latest released version");        
        String latestVersionQuery = this.getLatestReleasedDatasetVersionQuery(identifierClause);
        msg("latestVersionQuery: " + latestVersionQuery);
        chosenVersion = this.getDatasetVersionByQuery(latestVersionQuery);
        if (chosenVersion != null){
            return chosenVersion;
        }

        // (2c) Look for draft
        msg("(2c) Look for draft");        
        String draftQuery = this.getDraftDatasetVersionQuery(identifierClause);            
        msg("draftQuery: " + draftQuery);
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
    public DatasetVersion retrieveDatasetVersionByPersistentId(String persistentId, String version){
        
        msg("retrieveDatasetVersionByPersistentId: " + persistentId + " " + version);
        if (persistentId==null){
            return null;
        }
        
        /*        
            Parse the persistent id
        */
        String[] persistentIdParts = parsePersistentId(persistentId);   // [ protocol, authority, identifier]
        msg("persistentIdParts: " + Arrays.toString(persistentIdParts));
        if ( persistentIdParts == null || persistentIdParts.length != 3){
            logger.log(Level.WARNING, "Failed to parse persistentID: {0}", persistentId);
            return null;
        }
        
        String identifierClause = " AND ds.protocol= '" + persistentIdParts[0] + "'"; 
        identifierClause += " AND ds.authority = '" + persistentIdParts[1] + "'"; 
        identifierClause += " AND ds.identifier = '" + persistentIdParts[2] + "'"; 
        

        return retrieveDatasetVersionByIdentiferClause(identifierClause, version);
    }
    
    
     /**
     * Find a DatasetVersion using the persisentID and version string
     * 
     * @param datasetId
     * @param version  "DRAFT", 1.0, 2, 3.4, null, etc
     * @return 
     */
    public DatasetVersion retrieveDatasetVersionById(Long datasetId, String version){
        msg("retrieveDatasetVersionById: " + datasetId + " " + version);
        if (datasetId==null){
            return null;
        }        
        
        String identifierClause = " AND ds.id = " + datasetId;

        return retrieveDatasetVersionByIdentiferClause(identifierClause, version);
          
    } // end: retrieveDatasetVersionById

} // end class


