package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import io.vavr.Value;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class DatasetServiceIT extends WebappArquillianDeployment {

    @Inject
    private DatasetService datasetService;

    @Inject
    private DataverseSession dataverseSession;

    @Inject
    private AuthenticationServiceBean authenticationServiceBean;

    @Inject
    private DatasetDao datasetDao;

    @Before
    public void setUp() {
        dataverseSession.setUser(authenticationServiceBean.getAdminUser());
    }

    @Test
    public void removeDatasetThumbnail() {
        // given
        Dataset datasetWithFiles = datasetDao.find(52L);
        datasetWithFiles.setThumbnailFile(datasetWithFiles.getFiles().get(0));
        Dataset datasetWithThumbnail = datasetDao.merge(datasetWithFiles);

        // when
        datasetService.removeDatasetThumbnail(datasetWithThumbnail);

        // then
        Dataset updatedDataset = datasetDao.find(52L);
        assertThat(updatedDataset.getThumbnailFile()).isNull();
    }

    @Test
    public void changeDatasetThumbnail() {
        // given
        Dataset datasetWithFiles = datasetDao.find(52L);

        // when
        datasetService.changeDatasetThumbnail(datasetWithFiles, datasetWithFiles.getFiles().get(0));
        Dataset updatedDataset = datasetDao.find(52L);

        // then
        assertThat(updatedDataset.getThumbnailFile()).isEqualTo(datasetWithFiles.getFiles().get(0));
    }

    @Test
    public void shouldSetDatasetEmbargoDate() {
        // given
        Dataset draftDataset = datasetDao.find(66L);
        Date embargoDate = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));

        // when
        datasetService.setDatasetEmbargoDate(draftDataset, embargoDate);

        // then
        Dataset dbDataset = datasetDao.find(66L);
        assertThat(dbDataset.getEmbargoDate().isDefined()).isTrue();
        assertThat(dbDataset.getEmbargoDate().get()).isEqualTo(embargoDate);
    }

    @Test
    public void shouldLiftDatasetEmbargoDate() {
        // given
        Dataset draftDataset = datasetDao.find(66L);
        Date embargoDate = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));
        draftDataset.setEmbargoDate(embargoDate);
        datasetDao.merge(draftDataset);

        // when
        datasetService.liftDatasetEmbargoDate(draftDataset);

        // then
        Dataset dbDataset = datasetDao.find(66L);
        assertThat(dbDataset.getEmbargoDate().isEmpty()).isTrue();
    }

    @Test
    public void updateLastChangeForExporterTime() {
        // given
        Dataset dataset = datasetDao.find(52L);
        Date lastChangeForExporterTime = dataset.getLastChangeForExporterTime().getOrNull();

        // when
        datasetService.updateLastChangeForExporterTime(dataset);

        // then
        assertThat(dataset.getLastChangeForExporterTime()).isNotEqualTo(lastChangeForExporterTime);
    }

    @Test
    public void updateAllLastChangeForExporterTime() {
        // when
        datasetService.updateAllLastChangeForExporterTime();

        // then
        List<Date> updatedTimeList = datasetDao.findAll().stream()
                .filter(d -> !d.isHarvested())
                .map(Dataset::getLastChangeForExporterTime)
                .map(Value::getOrNull)
                .collect(Collectors.toList());
        Date first = updatedTimeList.get(0);
        assertThat(first).isNotNull();
        assertThat(updatedTimeList).allMatch(first::equals);
    }
}
