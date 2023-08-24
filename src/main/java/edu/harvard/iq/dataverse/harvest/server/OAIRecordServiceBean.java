/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.harvest.server;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.export.ExportService;
import io.gdcc.spi.export.ExportException;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import static jakarta.ejb.TransactionAttributeType.REQUIRES_NEW;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TemporalType;

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
    @EJB 
    SettingsServiceBean settingsService;
    //@EJB
    //ExportService exportService;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    EntityManager em;   
    
    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.harvest.server.OAIRecordServiceBean");

    public void updateOaiRecords(String setName, List<Long> datasetIds, Date updateTime, boolean doExport) {
        updateOaiRecords(setName, datasetIds, updateTime, doExport, logger);
    }
    
    public void updateOaiRecords(String setName, List<Long> datasetIds, Date updateTime, boolean doExport, Logger setUpdateLogger) {

        // create Map of OaiRecords
        List<OAIRecord> oaiRecords = findOaiRecordsBySetName(setName);
        Map<String, OAIRecord> recordMap = new HashMap<>();
        if (oaiRecords != null) {
            for (OAIRecord record : oaiRecords) {
                // look for duplicates here? delete?
                recordMap.put(record.getGlobalId(), record);
            }
        } else {
            setUpdateLogger.fine("Null returned - no records found.");
        }

        if (!recordMap.isEmpty()) {
            setUpdateLogger.fine("Found " + recordMap.size() + " existing records");
        } else {
            setUpdateLogger.fine("No records in the set yet.");
        }

        if (datasetIds != null) {
            for (Long datasetId : datasetIds) {
                setUpdateLogger.fine("processing dataset id=" + datasetId);
                Dataset dataset = datasetService.find(datasetId);
                if (dataset == null) {
                    setUpdateLogger.fine("failed to find dataset!");
                } else if (dataset.isReleased() && !dataset.isDeaccessioned()) {
                    // This means this is a published dataset
                    setUpdateLogger.fine("found published dataset.");

                    // if doExport was requested, we'll check if the 
                    // dataset has been exported since the last time it was 
                    // published, and try to export if not.
                    if (doExport) {
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

                            setUpdateLogger.fine("Attempting to run export on dataset " + dataset.getGlobalId().asString());
                            exportAllFormats(dataset);
                        }
                        
                        // TODO: should probably bail if the export attempt has failed! -- L.A. 4.9.2
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
            OAIRecord record = recordMap.get(dataset.getGlobalId().asString());
            if (record == null) {
                setUpdateLogger.info("creating a new OAI Record for " + dataset.getGlobalId().asString());
                record = new OAIRecord(setName, dataset.getGlobalId().asString(), new Date());
                em.persist(record);
            } else {
                if (record.isRemoved()) {
                    setUpdateLogger.info("\"un-deleting\" an existing OAI Record for " + dataset.getGlobalId().asString());
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

        List<OAIRecord> oaiRecords = findOaiRecordsByGlobalId(dataset.getGlobalId().asString());
        if (oaiRecords != null) {

            DatasetVersion releasedVersion = dataset.getReleasedVersion();

            if (releasedVersion == null || dataset.getLastExportTime() == null) {
                // Datast must have been deaccessioned.
                markOaiRecordsAsRemoved(oaiRecords, new Date(), logger);
                return;

            }
            
            for (OAIRecord record : oaiRecords) {
                if (record.isRemoved()) {
                    logger.fine("\"un-deleting\" an existing OAI Record for " + dataset.getGlobalId().asString());
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
            logger.log(Level.FINE, "Attempting to run export on dataset {0}", dataset.getGlobalId());
            exportServiceInstance.exportAllFormats(dataset);
            dataset = datasetService.merge(dataset);
        } catch (ExportException ee) {logger.fine("Caught export exception while trying to export. (ignoring)");}
        catch (Exception e) {logger.fine("Caught unknown exception while trying to export (ignoring)");}
    }
    
    @TransactionAttribute(REQUIRES_NEW)
    public void exportAllFormatsInNewTransaction(Dataset dataset) throws ExportException {
        try {
            ExportService exportServiceInstance = ExportService.getInstance();
            exportServiceInstance.exportAllFormats(dataset);
            dataset = datasetService.merge(dataset);
        } catch (Exception e) {
            logger.log(Level.FINE, "Caught unknown exception while trying to export", e);
            throw new ExportException(e.getMessage());
        }
    }
    
    
    public OAIRecord findOAIRecordBySetNameandGlobalId(String setName, String globalId) {
        OAIRecord oaiRecord = null;
        
        String queryString = "SELECT object(h) from OAIRecord h where h.globalId = :globalId";
        queryString += setName != null ? " and h.setName = :setName" : ""; // and h.setName is null";
        
        logger.fine("findOAIRecordBySetNameandGlobalId; query: "+queryString+"; globalId: "+globalId+"; setName: "+setName);
                
        
        TypedQuery query = em.createQuery(queryString, OAIRecord.class).setParameter("globalId",globalId);
        if (setName != null) { query.setParameter("setName",setName); }        
        
        try {
           oaiRecord = (OAIRecord) query.setMaxResults(1).getSingleResult();
        } catch (jakarta.persistence.NoResultException e) {
           // Do nothing, just return null. 
        }
        logger.fine("returning oai record.");
        return oaiRecord;       
    }
    
    public List<OAIRecord> findOaiRecordsByGlobalId(String globalId) {
        String query="SELECT object(h) from OAIRecord h where h.globalId = :globalId";
        List<OAIRecord> oaiRecords = null;
        try {
            oaiRecords = em.createQuery(query, OAIRecord.class).setParameter("globalId",globalId).getResultList();
        } catch (Exception ex) {
            // Do nothing, return null. 
        }
        return oaiRecords;     
    }

    public List<OAIRecord> findOaiRecordsBySetName(String setName) {
        return findOaiRecordsBySetName(setName, null, null);
    }    
    
    public List<OAIRecord> findOaiRecordsBySetName(String setName, Instant from, Instant until) {
        return findOaiRecordsBySetName(setName, from, until, false);
    }
    
    public List<OAIRecord> findOaiRecordsNotInThisSet(String setName, Instant from, Instant until) {
        return findOaiRecordsBySetName(setName, from, until, true);
    }
    
    public List<OAIRecord> findOaiRecordsBySetName(String setName, Instant from, Instant until, boolean excludeSet) {
                
        if (setName == null) {
            setName = "";
        }
        
        String queryString = "SELECT object(h) from OAIRecord h where h.id is not null";
        if (excludeSet) {
            queryString += " and h.setName is not null and h.setName != '' and h.setName != :setName";
        } else {
            queryString += " and h.setName = :setName";
        }
        
        queryString += from != null ? " and h.lastUpdateTime >= :from" : "";
        queryString += until != null ? " and h.lastUpdateTime<=:until" : "";
        queryString += " order by h.globalId";

        logger.fine("Query: "+queryString);
        
        TypedQuery<OAIRecord> query = em.createQuery(queryString, OAIRecord.class);
        if (setName != null) { 
            query.setParameter("setName",setName); 
        }
        // TODO: review and phase out the use of java.util.Date throughout this service.
        
        if (from != null) { 
            query.setParameter("from",Date.from(from),TemporalType.TIMESTAMP); 
        }
        
        if (until != null) { 
            Date untilDate = Date.from(until);
            query.setParameter("until",untilDate,TemporalType.TIMESTAMP); 
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
        
        TypedQuery<OAIRecord> query = em.createQuery(queryString, OAIRecord.class);
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
        
        TypedQuery<OAIRecord> query = em.createQuery(queryString, OAIRecord.class);
        if (setName != null) { query.setParameter("setName",setName); }
        
        try {
            return query.getResultList();      
        } catch (Exception ex) {
            logger.fine("Caught exception; returning null.");
            return null;
        }
    }
    
    public Instant getEarliestDate() {
        String queryString = "SELECT min(r.lastUpdateTime) FROM OAIRecord r";
        TypedQuery<Date> query = em.createQuery(queryString, Date.class);
        Date retDate = query.getSingleResult();
        if (retDate != null) {
            return retDate.toInstant();
        }
        
        // if there are no records yet, return the default "now"
        return new Date().toInstant();
    }
    
}
