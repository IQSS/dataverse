package edu.harvard.iq.dataverse.dataset;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetThumbnailCommand;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.search.index.SolrIndexServiceBean;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DatasetServiceTest {

    @InjectMocks
    private DatasetService datasetService;

    @Mock
    private DataverseRequestServiceBean dvRequestService;

    @Mock
    private EjbDataverseEngine commandEngine;

    @Mock
    private DatasetDao datasetDao;
    
    @Mock
    private SolrIndexServiceBean solrIndexService;

    @Mock
    private PermissionsWrapper permissionsWrapper;

    @Mock
    private DataverseSession session;

    @Test
    public void changeDatasetThumbnail() {
        //given
        Dataset dataset = new Dataset();
        DataFile thumbnailFile = new DataFile();

        when(commandEngine.submit(any(UpdateDatasetThumbnailCommand.class))).thenReturn(new DatasetThumbnail("", thumbnailFile));

        //when
        datasetService.changeDatasetThumbnail(dataset, thumbnailFile);

        //then
        verify(commandEngine, times(1)).submit(any(UpdateDatasetThumbnailCommand.class));

    }

    @Test
    public void changeDatasetThumbnail_WithFileFromDisk() throws IOException {
        //given
        Dataset dataset = new Dataset();

        when(commandEngine.submit(any(UpdateDatasetThumbnailCommand.class))).thenReturn(new DatasetThumbnail("", new DataFile()));

        //when
        datasetService.changeDatasetThumbnail(dataset, IOUtils.toInputStream("", "UTF-8"));

        //then
        verify(commandEngine, times(1)).submit(any(UpdateDatasetThumbnailCommand.class));

    }

    @Test
    public void removeDatasetThumbnail() {
        //given
        Dataset dataset = new Dataset();

        when(commandEngine.submit(any(UpdateDatasetThumbnailCommand.class))).thenReturn(new DatasetThumbnail("", new DataFile()));

        //when
        datasetService.removeDatasetThumbnail(dataset);

        //then
        verify(commandEngine, times(1)).submit(any(UpdateDatasetThumbnailCommand.class));
    }

    @Test
    public void setDatasetEmbargoDate() {
        // given
        Dataset dataset = new Dataset();
        Date embargoDate = Date.from(Instant.now().truncatedTo(ChronoUnit.DAYS).plus(2, ChronoUnit.DAYS));
        when(datasetDao.mergeAndFlush(dataset)).thenReturn(dataset);

        // when
        Dataset result = datasetService.setDatasetEmbargoDate(dataset, embargoDate);

        // then
        Assertions.assertEquals(embargoDate, result.getEmbargoDate().getOrNull());
    }

    @Test
    public void setDatasetEmbargoDate_lockedDataset() {
        // given
        Dataset dataset = new Dataset();
        DatasetLock lock = new DatasetLock(DatasetLock.Reason.InReview, MocksFactory.makeAuthenticatedUser("Jurek", "Kiler"));
        lock.setId(1L);
        dataset.addLock(lock);
        Date embargoDate = Date.from(Instant.now().truncatedTo(ChronoUnit.DAYS).plus(2, ChronoUnit.DAYS));

        // when & then
        Exception exception = Assertions.assertThrows(IllegalStateException.class, () -> {
            datasetService.setDatasetEmbargoDate(dataset, embargoDate);
        });

        String message = datasetService.getDatasetLockedMessage(dataset);
        Assertions.assertEquals(message, exception.getMessage());
        Assertions.assertTrue(dataset.getEmbargoDate().isEmpty());
    }

    @Test
    public void setDatasetEmbargoDate_publishedDataset_notSuperuser() {
        // given
        Dataset dataset = MocksFactory.makeDataset();
        dataset.setVersions(Lists.newArrayList(new DatasetVersion(), new DatasetVersion()));
        AuthenticatedUser user = MocksFactory.makeAuthenticatedUser("Jurek","Kiler");
        user.setSuperuser(false);
        session.setUser(user);
        when(session.getUser()).thenReturn(user);

        Date embargoDate = Date.from(Instant.now().truncatedTo(ChronoUnit.DAYS).plus(2, ChronoUnit.DAYS));

        // when
        Exception exception = Assertions.assertThrows(IllegalStateException.class, () -> {
            datasetService.setDatasetEmbargoDate(dataset, embargoDate);
        });

        String message = datasetService.getDatasetInWrongStateMessage();
        Assertions.assertEquals(message, exception.getMessage());
        Assertions.assertTrue(dataset.getEmbargoDate().isEmpty());
    }
}