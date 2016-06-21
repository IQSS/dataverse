/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.server;

import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author Leonid Andreev
 * based on the implementation of "HarvestStudyServiceBean" from
 * DVN 3*, by Gustavo Durand. 
 */

@Stateless
@Named
public class OAIRecordServiceBean implements java.io.Serializable {
    @EJB 
    OAISetServiceBean oaiSetService;    
    @EJB 
    IndexServiceBean indexService;
    @EJB 
    DatasetServiceBean datasetService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    EntityManager em;   
    
    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.server.OAIRecordServiceBean");
    
    /*
    public void updateOaiRecords() {
        Date updateTime = new Date();
        List<OAISet> sets = oaiSetService.findAll();
        
        for (OAISet oaiSet : sets) {
            List<Long> studyIds = indexService.query(oaiSet.getDefinition());
            studyIds = studyService.getVisibleStudies(studyIds, null);
            studyIds = studyService.getViewableStudies(studyIds);
            updateOaiRecords( oaiSet.getSpec(), studyIds, updateTime );
        }
        
        // also do noset membet
        List<Long> studyIds = studyService.getAllNonHarvestedStudyIds();
        studyIds = studyService.getVisibleStudies(studyIds, null);
        studyIds = studyService.getViewableStudies(studyIds);        
        updateOaiRecords( null, studyIds, updateTime );
        
    }    

    private void updateOaiRecords(String setName, List<Long> studyIds, Date updateTime) {

        // create Map of OaiRecords
        List<OAIRecord> harvestStudies = findOaiRecordsBySetName( setName );
        Map<String,OAIRecord> hsMap = new HashMap();
        for (OAIRecord hsEntry : harvestStudies) {
            hsMap.put(hsEntry.getGlobalId(), hsEntry);
        }


        for (Long studyId : studyIds) {
            Study study = studyService.getStudy(studyId);
            em.refresh(study); // workaround to get updated lastExportTime (to be investigated)
            
            if ( study.getLastExportTime() != null ) {     
                OAIRecord hs = hsMap.get( study.getGlobalId() );
                if (hs == null) {
                    hs = new OAIRecord( setName, study.getGlobalId(), updateTime );
                    em.persist(hs);                    
                } else {
                    if (hs.isRemoved()) {
                        hs.setRemoved(false);
                        hs.setLastUpdateTime( updateTime );
                    } else if (study.getLastExportTime().after( hs.getLastUpdateTime() ) ) {
                        hs.setLastUpdateTime( updateTime );
                    }

                    hsMap.remove(hs.getGlobalId());
                }
            }
        }

        // anything left in the map should be marked as removed
        markOaiRecordsAsRemoved( hsMap.values(), updateTime);
        
    }
    
    
    public void markOaiRecordsAsRemoved(Collection<OAIRecord> harvestStudies, Date updateTime) {
        for (OAIRecord hs : harvestStudies) {
            if ( !hs.isRemoved() ) {
                hs.setRemoved(true);
                hs.setLastUpdateTime(updateTime);
            }
        }
       
    }
    */
    
    public OAIRecord findOAIRecordBySetNameandGlobalId(String setName, String globalId) {
        OAIRecord oaiRecord = null;
        
        String queryString = "SELECT object(h) from OAIRecord h where h.globalId = :globalId";
        queryString += setName != null ? " and h.setName = :setName" : ""; // and h.setName is null";
        
        logger.info("findOAIRecordBySetNameandGlobalId; query: "+queryString+"; globalId: "+globalId+"; setName: "+setName);
                
        
        Query query = em.createQuery(queryString).setParameter("globalId",globalId);
        if (setName != null) { query.setParameter("setName",setName); }        

        try {
           oaiRecord = (OAIRecord) query.getSingleResult();
        } catch (javax.persistence.NoResultException e) {
           // Do nothing, just return null. 
        }
        logger.info("returning oai record.");
        return oaiRecord;       
    }
    
    public List<OAIRecord> findOaiRecordsByGlobalId(String globalId) {
        String query="SELECT h from OAIRecord as h where h.globalId = :globalId";
        List<OAIRecord> oaiRecords = null;
        try {
            oaiRecords = em.createQuery(query).setParameter("globalId",globalId).getResultList();
        } catch (Exception ex) {
            // Do nothing, return null. 
        }
        return oaiRecords;     
    }

    public List<OAIRecord> findOaiRecordsBySetName(String setName) {
        return findOaiRecordsBySetName(setName, null, null);
    }    
    
    public List<OAIRecord> findOaiRecordsBySetName(String setName, Date from, Date until) {
        
        String queryString ="SELECT object(h) from OAIRecord as h";
        queryString += setName != null ? " where h.setName = :setName" : ""; // where h.setName is null";
        queryString += from != null ? " and h.lastUpdateTime >= :from" : "";
        queryString += until != null ? " and h.lastUpdateTime <= :until" : "";

        logger.info("Query: "+queryString);
        
        Query query = em.createQuery(queryString);
        if (setName != null) { query.setParameter("setName",setName); }
        if (from != null) { query.setParameter("from",from); }
        if (until != null) { query.setParameter("until",until); }
        
        return query.getResultList();        
    }
    
}
