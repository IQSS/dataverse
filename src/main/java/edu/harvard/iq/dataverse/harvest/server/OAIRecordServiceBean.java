/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.server;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.export.ExportException;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TemporalType;

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
        updateOaiRecords(setName, datasetIds, updateTime, doExport, logger);
    }
    
    public void updateOaiRecords(String setName, List<Long> datasetIds, Date updateTime, boolean doExport, Logger setUpdateLogger) {
        
        // create Map of OaiRecords
        List<OAIRecord> oaiRecords = findOaiRecordsBySetName( setName );
        Map<String,OAIRecord> recordMap = new HashMap();
        if (oaiRecords != null) {
            for (OAIRecord record : oaiRecords) {
                // look for duplicates here? delete?
                recordMap.put(record.getGlobalId(), record);
            }
        } else {
            setUpdateLogger.fine("Null returned - no records found.");
        } 

        if (!recordMap.isEmpty()) {
            setUpdateLogger.fine("Found "+recordMap.size()+" existing records");
        } else {
            setUpdateLogger.fine("No records in the set yet.");
        }

        if (datasetIds != null) {
            for (Long datasetId : datasetIds) {
                setUpdateLogger.fine("processing dataset id=" + datasetId);
                Dataset dataset = datasetService.find(datasetId);
                if (dataset == null) {
                    setUpdateLogger.fine("failed to find dataset!");
                } else {
                    setUpdateLogger.fine("found dataset.");

                    // TODO: option to *force* export? 
                    if (doExport) {
                        // TODO: 
                        // Review this logic - specifically for handling of 
                        // deaccessioned datasets. -- L.A. 4.5
                        // OK, it looks like we can't rely on .getPublicationDate() - 
                        // as it is essentially the *first publication* date; 
                        // and we are interested in the *last*
                        
                        DatasetVersion releasedVersion = dataset.getReleasedVersion();
                        Date publicationDate = releasedVersion == null ? null : releasedVersion.getReleaseTime();
                        
                        //if (dataset.getPublicationDate() != null
                        //        && (dataset.getLastExportTime() == null
                        //        || dataset.getLastExportTime().before(dataset.getPublicationDate()))) {
                        
                        if (publicationDate != null 
                                && (dataset.getLastExportTime() == null
                                || dataset.getLastExportTime().before(publicationDate))) {
                        
                            setUpdateLogger.fine("Attempting to run export on dataset " + dataset.getGlobalId());
                            exportAllFormats(dataset);
                        }
                    }

                    setUpdateLogger.fine("\"last exported\" timestamp: " + dataset.getLastExportTime());
                    em.refresh(dataset);
                    setUpdateLogger.fine("\"last exported\" timestamp, after db refresh: " + dataset.getLastExportTime());

                    updateOaiRecordForDataset(dataset, setName, recordMap, setUpdateLogger);
                }
            }
        }

        // anything left in the map should be marked as removed!
        markOaiRecordsAsRemoved( recordMap.values(), updateTime, setUpdateLogger);
        
    }
    
    // This method updates -  creates/refreshes/un-marks-as-deleted - one OAI 
    // record at a time. It does so inside its own transaction, to ensure that 
    // the changes take place immediately. (except the method is called from 
    // right here, in this EJB - so the attribute does not do anything! (TODO:!)
    @TransactionAttribute(REQUIRES_NEW)
    public void updateOaiRecordForDataset(Dataset dataset, String setName, Map<String, OAIRecord> recordMap, Logger setUpdateLogger) {
        // TODO: review .isReleased() logic
        // Answer: no, we can't trust isReleased()! It's a dvobject method that
        // simply returns (publicationDate != null). And the publication date 
        // stays in place even if all the released versions have been deaccessioned. 
        boolean isReleased = dataset.getReleasedVersion() != null;
        
        if (isReleased && dataset.getLastExportTime() != null) {
            OAIRecord record = recordMap.get(dataset.getGlobalId());
            if (record == null) {
                setUpdateLogger.info("creating a new OAI Record for " + dataset.getGlobalId());
                record = new OAIRecord(setName, dataset.getGlobalId(), new Date());
                em.persist(record);
            } else {
                if (record.isRemoved()) {
                    setUpdateLogger.info("\"un-deleting\" an existing OAI Record for " + dataset.getGlobalId());
                    record.setRemoved(false);
                    record.setLastUpdateTime(new Date());
                } else if (dataset.getLastExportTime().after(record.getLastUpdateTime())) {
                    setUpdateLogger.info("updating the timestamp on an existing record.");
                    record.setLastUpdateTime(new Date());
                }

                recordMap.remove(record.getGlobalId());
            }
        }
    }
    
    
    // Updates any existing OAI records for this dataset
    // Should be called whenever there's a change in the release status of the Dataset
    // (i.e., when it's published or deaccessioned), so that the timestamps and 
    // on the records could be freshened before the next reexport of the corresponding
    // sets. 
    // *Note* that the method assumes that a full metadata reexport has already 
    // been attempted on the dataset. (Meaning that if getLastExportTime is null, 
    // we'll just assume that the exports failed and the OAI records must be marked
    // as "deleted". 
    @TransactionAttribute(REQUIRES_NEW)
    public void updateOaiRecordsForDataset(Dataset dataset) {
        // create Map of OaiRecords

        List<OAIRecord> oaiRecords = findOaiRecordsByGlobalId(dataset.getGlobalId());
        if (oaiRecords != null) {

            DatasetVersion releasedVersion = dataset.getReleasedVersion();

            if (releasedVersion == null || dataset.getLastExportTime() == null) {
                // Datast must have been deaccessioned.
                markOaiRecordsAsRemoved(oaiRecords, new Date(), logger);
                return;

            }
            
            for (OAIRecord record : oaiRecords) {
                if (record.isRemoved()) {
                    logger.fine("\"un-deleting\" an existing OAI Record for " + dataset.getGlobalId());
                    record.setRemoved(false);
                    record.setLastUpdateTime(new Date());
                } else if (dataset.getLastExportTime().after(record.getLastUpdateTime())) {
                    record.setLastUpdateTime(new Date());
                }
            }
        } else {
            logger.fine("Null returned - no records found.");
        }
    }
    
    public void markOaiRecordsAsRemoved(Collection<OAIRecord> records, Date updateTime, Logger setUpdateLogger) {
        for (OAIRecord oaiRecord : records) {
            if ( !oaiRecord.isRemoved() ) {
                setUpdateLogger.fine("marking OAI record "+oaiRecord.getGlobalId()+" as removed");
                oaiRecord.setRemoved(true);
                oaiRecord.setLastUpdateTime(updateTime);
            } else {
                setUpdateLogger.fine("OAI record "+oaiRecord.getGlobalId()+" is already marked as removed.");
            }
        }
       
    }
    
    // TODO: 
    // Export functionality probably deserves its own EJB ServiceBean - 
    // so maybe create ExportServiceBean, and move these methods there? 
    // (why these need to be in an EJB bean at all, what's wrong with keeping 
    // them in the loadable ExportService? - since we need to modify the 
    // "last export" timestamp on the dataset, being able to do that in the 
    // @EJB context is convenient. 
    
    public void exportAllFormats(Dataset dataset) {
        try {
            ExportService exportServiceInstance = ExportService.getInstance();
            logger.fine("Attempting to run export on dataset "+dataset.getGlobalId());
            exportServiceInstance.exportAllFormats(dataset);
            datasetService.updateLastExportTimeStamp(dataset.getId());
        } catch (ExportException ee) {logger.fine("Caught export exception while trying to export. (ignoring)");}
        catch (Exception e) {logger.fine("Caught unknown exception while trying to export (ignoring)");}
    }
    
    @TransactionAttribute(REQUIRES_NEW)
    public void exportAllFormatsInNewTransaction(Dataset dataset) throws ExportException {
        try {
            ExportService exportServiceInstance = ExportService.getInstance();
            exportServiceInstance.exportAllFormats(dataset);
            datasetService.updateLastExportTimeStamp(dataset.getId());
        } catch (Exception e) {
            logger.fine("Caught unknown exception while trying to export");
            throw new ExportException(e.getMessage());
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
           oaiRecord = (OAIRecord) query.setMaxResults(1).getSingleResult();
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
                
        String queryString ="SELECT object(h) from OAIRecord as h where h.id is not null";
        queryString += setName != null ? " and h.setName = :setName" : ""; // where h.setName is null";
        queryString += from != null ? " and h.lastUpdateTime >= :from" : "";
        queryString += until != null ? " and h.lastUpdateTime<=:until" : "";

        logger.fine("Query: "+queryString);
        
        Query query = em.createQuery(queryString);
        if (setName != null) { query.setParameter("setName",setName); }
        if (from != null) { query.setParameter("from",from,TemporalType.TIMESTAMP); }
        // In order to achieve inclusivity on the "until" matching, we need to do 
        // the following (if the "until" parameter is supplied):
        // 1) if the supplied "until" parameter has the time portion (and is not just
        // a date), we'll increment it by one second. This is because the time stamps we 
        // keep in the database also have fractional thousands of a second. 
        // So, a record may be shown as "T17:35:45", but in the database it is 
        // actually "17:35:45.356", so "<= 17:35:45" isn't going to work on this 
        // time stamp! - So we want to try "<= 17:35:45" instead. 
        // 2) if it's just a date, we'll increment it by a *full day*. Otherwise
        // our database time stamp of 2016-10-23T17:35:45.123Z is NOT going to 
        // match " <= 2016-10-23" - which is really going to be interpreted as 
        // "2016-10-23T00:00:00.000". 
        // -- L.A. 4.6
        
        if (until != null) { 
            // 24 * 3600 * 1000 = number of milliseconds in a day. 
            
            if (until.getTime() % (24 * 3600 * 1000) == 0) {
                // The supplied "until" parameter is a date, with no time
                // portion. 
                logger.fine("plain date. incrementing by one day");
                until.setTime(until.getTime()+(24 * 3600 * 1000));
            } else {
                logger.fine("date and time. incrementing by one second");
                until.setTime(until.getTime()+1000);
            }
            query.setParameter("until",until,TemporalType.TIMESTAMP); 
        }
                
        try {
            return query.getResultList();      
        } catch (Exception ex) {
            logger.fine("Caught exception; returning null.");
            return null;
        }
    }
    
    // This method is to only get the records NOT marked as "deleted":
    public List<OAIRecord> findActiveOaiRecordsBySetName(String setName) {
        
        
        String queryString ="SELECT object(h) from OAIRecord as h WHERE (h.removed != true)";
        queryString += setName != null ? " and (h.setName = :setName)" : "and (h.setName is null)";
        logger.fine("Query: "+queryString);
        
        Query query = em.createQuery(queryString);
        if (setName != null) { query.setParameter("setName",setName); }
        
        try {
            return query.getResultList();      
        } catch (Exception ex) {
            logger.fine("Caught exception; returning null.");
            return null;
        }
    }
    
    // This method is to only get the records marked as "deleted":
    public List<OAIRecord> findDeletedOaiRecordsBySetName(String setName) {
        
        
        String queryString ="SELECT object(h) from OAIRecord as h WHERE (h.removed = true)";
        queryString += setName != null ? " and (h.setName = :setName)" : "and (h.setName is null)";
        logger.fine("Query: "+queryString);
        
        Query query = em.createQuery(queryString);
        if (setName != null) { query.setParameter("setName",setName); }
        
        try {
            return query.getResultList();      
        } catch (Exception ex) {
            logger.fine("Caught exception; returning null.");
            return null;
        }
    }
    
}
