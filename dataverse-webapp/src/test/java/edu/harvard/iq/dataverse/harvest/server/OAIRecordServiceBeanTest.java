package edu.harvard.iq.dataverse.harvest.server;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.harvest.OAIRecord;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;

@ExtendWith(MockitoExtension.class)
class OAIRecordServiceBeanTest {

    @InjectMocks
    private OAIRecordServiceBean oaiRecordServiceBean;

    @Mock
    private EntityManager entityManager;

    private static final long OAI_RECORD_UPDATE_TIME = 123456;
    private static final long DATASET_METADATA_CHANGE_TIME = 1234567;
    private static final long UTC_CLOCK_TIME = 12345678;

    private static final long DATASET_VERSION_OLDER_RELEASE_TIME = 123;
    private static final long DATASET_VERSION_RELEASE_TIME = 1234;
    private static final long DATASET_VERSION_UPDATED_RELEASE_TIME = 123456799;
    private Clock utcClock = Clock.fixed(Instant.ofEpochMilli(UTC_CLOCK_TIME), ZoneId.of("UTC"));

    @BeforeEach
    private void beforeEach(){
        oaiRecordServiceBean.setSystemClock(utcClock);
    }

    // -------------------- TESTS --------------------

    @Test
    public void updateOaiRecordForDataset_ForUpdatedGuestbook() {
        //given
        Dataset dataset = setupDatasetData();

        HashMap<String, OAIRecord> oaiRecords = new HashMap<>();
        OAIRecord oaiRecord = setupOaiRecord(oaiRecords);

        //when
        oaiRecordServiceBean.updateOaiRecordForDataset(dataset,"setName", oaiRecords, Logger.getGlobal());

        //then
        Assert.assertEquals(utcClock.instant(), oaiRecord.getLastUpdateTime().toInstant());
        Assert.assertEquals(0, oaiRecords.size());

    }

    @Test
    public void updateOaiRecordForDataset_ForUpdatedRealeseTime() {
        //given
        Dataset dataset = setupDatasetData();
        DatasetVersion releasedVersion = dataset.getReleasedVersion();
        dataset.setLastChangeForExporterTime(null);
        releasedVersion.setReleaseTime(Date.from(Instant.ofEpochMilli(DATASET_VERSION_UPDATED_RELEASE_TIME)));

        HashMap<String, OAIRecord> oaiRecords = new HashMap<>();
        OAIRecord oaiRecord = setupOaiRecord(oaiRecords);

        //when
        oaiRecordServiceBean.updateOaiRecordForDataset(dataset,"setName", oaiRecords, Logger.getGlobal());

        //then
        Assert.assertEquals(utcClock.instant(), oaiRecord.getLastUpdateTime().toInstant());
        Assert.assertEquals(0, oaiRecords.size());

    }

    @Test
    public void updateOaiRecordForDataset_WithoutUpdates() {
        //given
        Dataset dataset = setupDatasetData();
        DatasetVersion releasedVersion = dataset.getReleasedVersion();
        dataset.setLastChangeForExporterTime(null);
        releasedVersion.setReleaseTime(Date.from(Instant.ofEpochMilli(DATASET_VERSION_OLDER_RELEASE_TIME)));

        HashMap<String, OAIRecord> oaiRecords = new HashMap<>();
        OAIRecord oaiRecord = setupOaiRecord(oaiRecords);
        oaiRecord.setRemoved(false);

        //when
        oaiRecordServiceBean.updateOaiRecordForDataset(dataset,"setName", oaiRecords, Logger.getGlobal());

        //then
        Assert.assertEquals(Instant.ofEpochMilli(OAI_RECORD_UPDATE_TIME), oaiRecord.getLastUpdateTime().toInstant());
        Assert.assertEquals(0, oaiRecords.size());

    }

    @Test
    public void updateOaiRecordForDataset_WithoutUpdates_EmbargoExpired() {
        //given
        Clock presentTime = Clock.fixed(Instant.now(), ZoneId.of("UTC"));
        oaiRecordServiceBean.setSystemClock(presentTime);

        Dataset dataset = setupDatasetData();
        dataset.setLastChangeForExporterTime(Date.from(presentTime.instant().minus(1, ChronoUnit.DAYS)));
        dataset.setEmbargoDate(Date.from(dataset.getLastChangeForExporterTime().get().toInstant().plus(12, ChronoUnit.HOURS)));

        HashMap<String, OAIRecord> oaiRecords = new HashMap<>();
        OAIRecord oaiRecord = setupOaiRecord(oaiRecords);
        oaiRecord.setRemoved(false);

        //when
        oaiRecordServiceBean.updateOaiRecordForDataset(dataset,"setName", oaiRecords, Logger.getGlobal());

        //then
        Assert.assertEquals(presentTime.instant(), oaiRecord.getLastUpdateTime().toInstant());
        Assert.assertEquals(0, oaiRecords.size());

    }

    @Test
    public void updateOaiRecordForDataset_ForRemovedRecord() {
        //given
        Dataset dataset = setupDatasetData();

        HashMap<String, OAIRecord> oaiRecords = new HashMap<>();
        OAIRecord oaiRecord = setupOaiRecord(oaiRecords);

        //when
        oaiRecordServiceBean.updateOaiRecordForDataset(dataset,"setName", oaiRecords, Logger.getGlobal());

        //then
        Assert.assertEquals(utcClock.instant(), oaiRecord.getLastUpdateTime().toInstant());
        Assert.assertEquals(0, oaiRecords.size());

    }

    @Test
    public void updateOaiRecordForDataset_ForNullRecord() {
        //given
        Dataset dataset = setupDatasetData();

        String setName = "setName";

        //when
        OAIRecord persistedRecord = oaiRecordServiceBean.updateOaiRecordForDataset(dataset,
                                                                              setName,
                                                                              new HashMap<>(),
                                                                              Logger.getGlobal());

        //then
        Assert.assertEquals(utcClock.instant(), persistedRecord.getLastUpdateTime().toInstant());
        Assert.assertEquals(setName, persistedRecord.getSetName());
        Assert.assertEquals("doi:nice/ID1", persistedRecord.getGlobalId());

    }

    // -------------------- PRIVATE --------------------

    private OAIRecord setupOaiRecord(HashMap<String, OAIRecord> oaiRecords) {
        OAIRecord oaiRecord = new OAIRecord();
        oaiRecord.setGlobalId("doi:nice/ID1");
        oaiRecord.setRemoved(true);
        oaiRecord.setLastUpdateTime(Date.from(Instant.ofEpochMilli(OAI_RECORD_UPDATE_TIME)));
        oaiRecords.put("doi:nice/ID1", oaiRecord);
        return oaiRecord;
    }

    private Dataset setupDatasetData() {
        Dataset dataset = new Dataset();
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setReleaseTime(Date.from(Instant.ofEpochMilli(DATASET_VERSION_RELEASE_TIME)));
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);
        dataset.setVersions(Lists.newArrayList(datasetVersion));
        dataset.setLastChangeForExporterTime(Date.from(Instant.ofEpochMilli(DATASET_METADATA_CHANGE_TIME)));
        dataset.setGlobalId(new GlobalId("doi", "nice", "ID1"));
        return dataset;
    }
}