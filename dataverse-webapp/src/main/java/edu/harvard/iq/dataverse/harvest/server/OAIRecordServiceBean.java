package edu.harvard.iq.dataverse.harvest.server;

import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.harvest.OAIRecord;
import edu.harvard.iq.dataverse.persistence.harvest.OAIRecordRepository;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toMap;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

/**
 * @author Leonid Andreev
 * based on the implementation of "HarvestStudyServiceBean" from
 * DVN 3*, by Gustavo Durand.
 */

@Stateless
public class OAIRecordServiceBean implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(OAIRecordServiceBean.class.getCanonicalName());


    @EJB
    private DatasetRepository datasetRepository;

    @EJB
    private OAIRecordRepository oaiRecordRepository;

    private Clock systemClock = Clock.systemDefaultZone();

    @TransactionAttribute(REQUIRES_NEW)
    public void updateOaiRecords(String setName, List<Long> datasetIds, Logger setUpdateLogger) {

        // create Map of OaiRecords
        Map<String, OAIRecord> recordMap = oaiRecordRepository.findBySetName(setName)
                                                .stream()
                                                .collect(toMap(OAIRecord::getGlobalId, record -> record));

        if (!recordMap.isEmpty()) {
            setUpdateLogger.fine("Found " + recordMap.size() + " existing records");
        } else {
            setUpdateLogger.fine("No records in the set yet.");
        }

        for (Long datasetId : datasetIds) {
            setUpdateLogger.fine("processing dataset id=" + datasetId);

            Optional<Dataset> datasetWithReleasedVersion = datasetRepository
                                            .findById(datasetId)
                                            .filter(Dataset::containsReleasedVersion);

            datasetWithReleasedVersion.ifPresent(dataset -> {
                    setUpdateLogger.fine("found published dataset.");
                    OAIRecord record = recordMap.remove(dataset.getGlobalIdString());
                    
                    if (record == null) {
                        createOaiRecordForDataset(dataset, setName, setUpdateLogger);
                    } else {
                        updateOaiRecordForDataset(dataset, record, setUpdateLogger);
                    }

                });
        }

        // anything left in the map should be marked as removed!
        markOaiRecordsAsRemoved(recordMap.values(), setUpdateLogger);

    }

    private OAIRecord createOaiRecordForDataset(Dataset dataset, String setName, Logger setUpdateLogger) {
        setUpdateLogger.info("creating a new OAI Record for " + dataset.getGlobalIdString());
        OAIRecord record = new OAIRecord(setName, dataset.getGlobalIdString(), Date.from(Instant.now(systemClock)));
        return oaiRecordRepository.save(record);
    }

    /**
     * This method updates - /refreshes/un-marks-as-deleted - one OAI
     * record at a time.
     */
    private void updateOaiRecordForDataset(Dataset dataset, OAIRecord record, Logger setUpdateLogger) {

        if (record.isRemoved()) {
            setUpdateLogger.info("\"un-deleting\" an existing OAI Record for " + dataset.getGlobalIdString());
            record.setRemoved(false);
            record.setLastUpdateTime(Date.from(Instant.now(systemClock)));
        } else if (isDatasetUpdated(dataset, record)) {
            setUpdateLogger.info("updating the timestamp on an existing record.");

            record.setLastUpdateTime(Date.from(Instant.now(systemClock)));
        }

    }

    private void markOaiRecordsAsRemoved(Collection<OAIRecord> records, Logger setUpdateLogger) {
        Date updateTime = Date.from(Instant.now(systemClock));

        for (OAIRecord oaiRecord : records) {
            if (!oaiRecord.isRemoved()) {
                setUpdateLogger.fine("marking OAI record " + oaiRecord.getGlobalId() + " as removed");
                oaiRecord.setRemoved(true);
                oaiRecord.setLastUpdateTime(updateTime);
            } else {
                setUpdateLogger.fine("OAI record " + oaiRecord.getGlobalId() + " is already marked as removed.");
            }
        }

    }

    public List<OAIRecord> findOaiRecordsByGlobalId(String globalId) {
        return oaiRecordRepository.findByGlobalId(globalId);
    }

    public List<OAIRecord> findOaiRecordsByGlobalIds(List<String> globalIds) {
        return oaiRecordRepository.findByGlobalIds(globalIds);
    }

    public List<OAIRecord> findOaiRecordsBySetName(String setName, Date from, Date until) {

        return oaiRecordRepository.findBySetNameAndLastUpdateBetween(
                StringUtils.trimToEmpty(setName),
                from, modifyUntilDate(until));
    }

    public List<OAIRecord> findActiveOaiRecordsBySetName(String setName) {
        return oaiRecordRepository.findBySetNameAndRemoved(setName, false);
    }

    public List<OAIRecord> findDeletedOaiRecordsBySetName(String setName) {
        return oaiRecordRepository.findBySetNameAndRemoved(setName, true);
    }

    private boolean isDatasetUpdated(Dataset dataset, OAIRecord record) {
        Date publishTime = dataset.getReleasedVersion().getReleaseTime();

        boolean isLastModificationTimeAfterOaiTime = dataset.getLastChangeForExporterTime()
                .map(modificationTime -> modificationTime.after(record.getLastUpdateTime()))
                .getOrElse(false);

        return publishTime.after(record.getLastUpdateTime()) || isLastModificationTimeAfterOaiTime || hasEmbargoExpiredSinceLastOaiTime(dataset, record);
    }

    /**
     * This method makes sure harvesting server notify harvesting clients that embargo expired only the first time after it happened.
     * Without it any time after embargo expired and OAIServer runs its check it would falsely notify harvesting clients
     * that {@value dataset} metadata changed.
     * @param dataset
     * @param record
     * @return true if embargo expired between last time the check was run and this run
     */
    private boolean hasEmbargoExpiredSinceLastOaiTime(Dataset dataset, OAIRecord record) {
        return dataset.getEmbargoDate()
                    .map(embargoDate -> embargoDate.after(record.getLastUpdateTime()) && embargoDate.before(Date.from(Instant.now(systemClock))))
                    .getOrElse(false);
    }

    private Date modifyUntilDate(Date until) {
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
                return new Date(until.getTime() + (24 * 3600 * 1000));
            } else {
                logger.fine("date and time. incrementing by one second");
                return new Date(until.getTime() + 1000);
            }
        }
        return null;
    }

    public void setSystemClock(Clock systemClock) {
        this.systemClock = systemClock;
    }
}
