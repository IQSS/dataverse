/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.server;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.export.ExportException;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
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
    //@EJB
    //ExportService exportService;

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
        
    }   */ 

    public void updateOaiRecords(String setName, List<Long> datasetIds, Date updateTime, boolean doExport) {

        // create Map of OaiRecords
        List<OAIRecord> oaiRecords = findOaiRecordsBySetName( setName );
        Map<String,OAIRecord> recordMap = new HashMap();
        if (oaiRecords != null) {
            for (OAIRecord record : oaiRecords) {
                recordMap.put(record.getGlobalId(), record);
            }
        } else {
            logger.fine("Null returned - no records found.");
        } 

        if (!recordMap.isEmpty()) {
            logger.fine("Found "+recordMap.size()+" existing records");
        } else {
            logger.fine("No records in the set yet.");
        }

        for (Long datasetId : datasetIds) {
            logger.fine("processing dataset id="+datasetId);
            Dataset dataset = datasetService.find(datasetId);
            if (dataset != null) {
                logger.fine("found dataset.");
            } else {
                logger.fine("failed to find dataset!");
            }
         
            
            // TODO: option to *force* export? 
            
            if (doExport) {
                if (dataset.getPublicationDate() != null 
                        && (dataset.getLastExportTime() == null 
                            || dataset.getLastExportTime().before(dataset.getPublicationDate()))) {
                    //ExportService exportServiceInstance = ExportService.getInstance();
                    //logger.fine("Attempting to run export on dataset "+dataset.getGlobalId());
                    //exportServiceInstance.exportAllFormatsInNewTransaction(dataset);
                    exportAllFormatsInNewTransaction(dataset);
                }
            }
            
            logger.fine("\"last exported\" timestamp: "+dataset.getLastExportTime());
            //em.refresh(dataset); - not needed?
            
            updateOaiRecordForDataset(dataset, setName, recordMap);
        }

        // anything left in the map should be marked as removed!
        markOaiRecordsAsRemoved( recordMap.values(), updateTime);
        
    }
    
    // This method updates -  creates/refreshes/marks-as-deleted - one OAI 
    // record at a time. It does so inside its own transaction, to ensure that 
    // the changes take place immediately. 
    @TransactionAttribute(REQUIRES_NEW)
    public void updateOaiRecordForDataset(Dataset dataset, String setName, Map<String, OAIRecord> recordMap) {
        if (dataset.isReleased() && dataset.getLastExportTime() != null) {
            OAIRecord record = recordMap.get(dataset.getGlobalId());
            if (record == null) {
                logger.fine("creating a new OAI Record for " + dataset.getGlobalId());
                record = new OAIRecord(setName, dataset.getGlobalId(), new Date());
                em.persist(record);
            } else {
                if (record.isRemoved()) {
                    logger.fine("\"un-deleting\" an existing OAI Record for " + dataset.getGlobalId());
                    record.setRemoved(false);
                    record.setLastUpdateTime(new Date());
                } else if (dataset.getLastExportTime().after(record.getLastUpdateTime())) {
                    logger.fine("updating the timestamp on an existing record.");
                    record.setLastUpdateTime(new Date());
                }

                recordMap.remove(record.getGlobalId());
            }
        }
    }
    
    @TransactionAttribute(REQUIRES_NEW)
    public void exportAllFormatsInNewTransaction(Dataset dataset) {
        try {
            ExportService exportServiceInstance = ExportService.getInstance();
            logger.fine("Attempting to run export on dataset "+dataset.getGlobalId());
            exportServiceInstance.exportAllFormats(dataset);
        } catch (ExportException ee) {logger.fine("Caught exception while trying to export. (ignoring)");}
    }
    
    public void markOaiRecordsAsRemoved(Collection<OAIRecord> records, Date updateTime) {
        for (OAIRecord oaiRecord : records) {
            if ( !oaiRecord.isRemoved() ) {
                logger.fine("marking OAI record "+oaiRecord.getGlobalId()+" as removed");
                oaiRecord.setRemoved(true);
                oaiRecord.setLastUpdateTime(updateTime);
            } else {
                logger.fine("OAI record "+oaiRecord.getGlobalId()+" is already marked as removed.");
            }
        }
       
    }
    
    
    public OAIRecord findOAIRecordBySetNameandGlobalId(String setName, String globalId) {
        OAIRecord oaiRecord = null;
        
        String queryString = "SELECT object(h) from OAIRecord h where h.globalId = :globalId";
        queryString += setName != null ? " and h.setName = :setName" : ""; // and h.setName is null";
        
        logger.fine("findOAIRecordBySetNameandGlobalId; query: "+queryString+"; globalId: "+globalId+"; setName: "+setName);
                
        
        Query query = em.createQuery(queryString).setParameter("globalId",globalId);
        if (setName != null) { query.setParameter("setName",setName); }        

        try {
           oaiRecord = (OAIRecord) query.getSingleResult();
        } catch (javax.persistence.NoResultException e) {
           // Do nothing, just return null. 
        }
        logger.fine("returning oai record.");
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

        logger.fine("Query: "+queryString);
        
        Query query = em.createQuery(queryString);
        if (setName != null) { query.setParameter("setName",setName); }
        if (from != null) { query.setParameter("from",from); }
        if (until != null) { query.setParameter("until",until); }
        
        try {
            return query.getResultList();      
        } catch (Exception ex) {
            logger.fine("Caught exception; returning null.");
            return null;
        }
    }
    
}
