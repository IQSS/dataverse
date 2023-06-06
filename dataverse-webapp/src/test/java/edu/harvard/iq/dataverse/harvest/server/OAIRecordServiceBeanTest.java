package edu.harvard.iq.dataverse.harvest.server;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.persistence.harvest.OAIRecord;
import edu.harvard.iq.dataverse.persistence.harvest.OAIRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class OAIRecordServiceBeanTest {

    private Logger logger = Logger.getLogger(OAIRecordServiceBeanTest.class.getCanonicalName());

    @InjectMocks
    private OAIRecordServiceBean oaiRecordServiceBean;

    @Mock
    private OAIRecordRepository oaiRecordRepository;

    @Mock
    private DatasetRepository datasetRepository;

    @Captor
    private ArgumentCaptor<OAIRecord> oaiRecordCaptor;

    private static final String SET_NAME = "setName";
    private static final long OAI_RECORD_UPDATE_TIME = 123456;
    private static final long DATASET_METADATA_CHANGE_TIME = 1234567;
    private static final long UTC_CLOCK_TIME = 12345678;

    private static final long DATASET_VERSION_OLDER_RELEASE_TIME = 123;
    private static final long DATASET_VERSION_RELEASE_TIME = 1234;
    private static final long DATASET_VERSION_UPDATED_RELEASE_TIME = 123456799;
    private Clock utcClock = Clock.fixed(Instant.ofEpochMilli(UTC_CLOCK_TIME), ZoneId.of("UTC"));

    @BeforeEach
    void beforeEach() {
        oaiRecordServiceBean.setSystemClock(utcClock);
    }

    // -------------------- TESTS --------------------

    @Test
    void updateOaiRecords_ForUpdatedGuestbook() {
        // given
        Dataset dataset = setupDatasetData();
        when(datasetRepository.findById(dataset.getId())).thenReturn(Optional.of(dataset));

        OAIRecord oaiRecord = setupOaiRecord();
        when(oaiRecordRepository.findBySetName(SET_NAME)).thenReturn(Lists.newArrayList(oaiRecord));

        // when
        oaiRecordServiceBean.updateOaiRecords(SET_NAME, Lists.newArrayList(dataset.getId()), logger);

        // then
        assertThat(oaiRecord.getLastUpdateTime().toInstant()).isEqualTo(utcClock.instant());
        assertThat(oaiRecord.isRemoved()).isFalse();
    }

    @Test
    void updateOaiRecords_ForUpdatedRealeseTime() {
        // given
        Dataset dataset = setupDatasetData();
        DatasetVersion releasedVersion = dataset.getReleasedVersion();
        dataset.setLastChangeForExporterTime(null);
        releasedVersion.setReleaseTime(Date.from(Instant.ofEpochMilli(DATASET_VERSION_UPDATED_RELEASE_TIME)));
        when(datasetRepository.findById(dataset.getId())).thenReturn(Optional.of(dataset));

        OAIRecord oaiRecord = setupOaiRecord();
        when(oaiRecordRepository.findBySetName(SET_NAME)).thenReturn(Lists.newArrayList(oaiRecord));

        // when
        oaiRecordServiceBean.updateOaiRecords(SET_NAME, Lists.newArrayList(dataset.getId()), logger);

        // then
        assertThat(oaiRecord.getLastUpdateTime().toInstant()).isEqualTo(utcClock.instant());
        assertThat(oaiRecord.isRemoved()).isFalse();
    }

    @Test
    public void updateOaiRecords_WithoutUpdates() {
        // given
        Dataset dataset = setupDatasetData();
        DatasetVersion releasedVersion = dataset.getReleasedVersion();
        dataset.setLastChangeForExporterTime(null);
        releasedVersion.setReleaseTime(Date.from(Instant.ofEpochMilli(DATASET_VERSION_OLDER_RELEASE_TIME)));
        when(datasetRepository.findById(dataset.getId())).thenReturn(Optional.of(dataset));

        OAIRecord oaiRecord = setupOaiRecord();
        oaiRecord.setRemoved(false);
        when(oaiRecordRepository.findBySetName(SET_NAME)).thenReturn(Lists.newArrayList(oaiRecord));

        // when
        oaiRecordServiceBean.updateOaiRecords(SET_NAME, Lists.newArrayList(dataset.getId()), logger);

        // then
        assertThat(oaiRecord.getLastUpdateTime().toInstant()).isEqualTo(Instant.ofEpochMilli(OAI_RECORD_UPDATE_TIME));
        assertThat(oaiRecord.isRemoved()).isFalse();
    }

    @Test
    public void updateOaiRecords_WithoutUpdates_EmbargoExpired() {
        // given
        Clock presentTime = Clock.fixed(Instant.now(), ZoneId.of("UTC"));
        oaiRecordServiceBean.setSystemClock(presentTime);

        Dataset dataset = setupDatasetData();
        dataset.setEmbargoDate(Date.from(presentTime.instant().minus(12, ChronoUnit.HOURS)));
        when(datasetRepository.findById(dataset.getId())).thenReturn(Optional.of(dataset));

        OAIRecord oaiRecord = setupOaiRecord();
        oaiRecord.setLastUpdateTime(Date.from(presentTime.instant().minus(1, ChronoUnit.DAYS)));
        oaiRecord.setRemoved(false);
        when(oaiRecordRepository.findBySetName(SET_NAME)).thenReturn(Lists.newArrayList(oaiRecord));

        // when
        oaiRecordServiceBean.updateOaiRecords(SET_NAME, Lists.newArrayList(dataset.getId()), logger);

        // then
        assertThat(oaiRecord.getLastUpdateTime().toInstant()).isEqualTo(presentTime.instant());
        assertThat(oaiRecord.isRemoved()).isFalse();
    }

    @Test
    public void updateOaiRecords_ForRemovedOAIdRecord() {
        // given
        Dataset dataset = setupDatasetData();
        when(datasetRepository.findById(dataset.getId())).thenReturn(Optional.of(dataset));

        OAIRecord oaiRecord = setupOaiRecord();
        when(oaiRecordRepository.findBySetName(SET_NAME)).thenReturn(Lists.newArrayList(oaiRecord));

        // when
        oaiRecordServiceBean.updateOaiRecords(SET_NAME, Lists.newArrayList(dataset.getId()), logger);

        // then
        assertThat(oaiRecord.getLastUpdateTime().toInstant()).isEqualTo(utcClock.instant());
        assertThat(oaiRecord.isRemoved()).isFalse();
    }

    @Test
    public void updateOaiRecords_ForDatasetNotPresentInOAIRecords() {
        // given
        Dataset dataset = setupDatasetData();
        when(datasetRepository.findById(dataset.getId())).thenReturn(Optional.of(dataset));

        // when
        oaiRecordServiceBean.updateOaiRecords(SET_NAME, Lists.newArrayList(dataset.getId()), logger);

        // then
        verify(oaiRecordRepository).save(oaiRecordCaptor.capture());

        OAIRecord persistedRecord = oaiRecordCaptor.getValue();
        assertThat(persistedRecord.getLastUpdateTime().toInstant()).isEqualTo(utcClock.instant());
        assertThat(persistedRecord.getSetName()).isEqualTo(SET_NAME);
        assertThat(persistedRecord.getGlobalId()).isEqualTo("doi:nice/ID1");
    }

    @Test
    public void updateOaiRecords_ForDeaccessionedDatasetNotPresentInOAIRecords() {
        // given
        Dataset dataset = setupDatasetData();
        dataset.getLatestVersion().setVersionState(VersionState.DEACCESSIONED);
        when(datasetRepository.findById(dataset.getId())).thenReturn(Optional.of(dataset));

        // when
        oaiRecordServiceBean.updateOaiRecords(SET_NAME, Lists.newArrayList(dataset.getId()), logger);

        // then
        verify(oaiRecordRepository, times(0)).save(any());
    }

    @Test
    public void updateOaiRecords_ForDeaccessionedDatasetPresentInOAIRecords() {
        // given
        Dataset dataset = setupDatasetData();
        dataset.getLatestVersion().setVersionState(VersionState.DEACCESSIONED);
        when(datasetRepository.findById(dataset.getId())).thenReturn(Optional.of(dataset));

        OAIRecord oaiRecord = setupOaiRecord();
        oaiRecord.setRemoved(false);
        when(oaiRecordRepository.findBySetName(SET_NAME)).thenReturn(Lists.newArrayList(oaiRecord));

        // when
        oaiRecordServiceBean.updateOaiRecords(SET_NAME, Lists.newArrayList(dataset.getId()), logger);

        // then
        assertThat(oaiRecord.getLastUpdateTime().toInstant()).isEqualTo(utcClock.instant());
        assertThat(oaiRecord.isRemoved()).isTrue();
    }

    @Test
    void findEarliestDate() {
        // when
        oaiRecordServiceBean.findEarliestDate();

        // then
        verify(oaiRecordRepository, times(1)).findEarliestDate();
    }

    // -------------------- PRIVATE --------------------

    private OAIRecord setupOaiRecord() {
        OAIRecord oaiRecord = new OAIRecord(SET_NAME, "doi:nice/ID1",
                Date.from(Instant.ofEpochMilli(OAI_RECORD_UPDATE_TIME)));
        oaiRecord.setRemoved(true);
        return oaiRecord;
    }

    private Dataset setupDatasetData() {
        Dataset dataset = new Dataset();
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setReleaseTime(Date.from(Instant.ofEpochMilli(DATASET_VERSION_RELEASE_TIME)));
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);
        dataset.setVersions(Lists.newArrayList(datasetVersion));
        dataset.setPublicationDate(Timestamp.from(Instant.ofEpochMilli(DATASET_VERSION_RELEASE_TIME)));
        dataset.setLastChangeForExporterTime(Date.from(Instant.ofEpochMilli(DATASET_METADATA_CHANGE_TIME)));
        dataset.setGlobalId(new GlobalId("doi", "nice", "ID1"));
        return dataset;
    }
}