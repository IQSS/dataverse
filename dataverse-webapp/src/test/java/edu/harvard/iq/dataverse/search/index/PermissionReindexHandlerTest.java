package edu.harvard.iq.dataverse.search.index;

import com.google.common.collect.ImmutableList;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PermissionReindexHandlerTest {

    @InjectMocks
    private PermissionReindexHandler permissionReindexHandler;

    @Mock
    private SolrIndexServiceBean solrIndexService;
    @Mock
    private DatasetRepository datasetRepository;

    // -------------------- TESTS --------------------

    @Test
    public void reindexPermission__dataverse() {
        // given
        Dataverse dataverse = new Dataverse();
        dataverse.setId(1L);
        Dataset dataset1 = new Dataset();
        dataset1.setId(2L);
        Dataset dataset2 = new Dataset();
        dataset2.setId(3L);

        when(datasetRepository.findByOwnerId(1L)).thenReturn(ImmutableList.of(dataset1, dataset2));

        // when
        permissionReindexHandler.reindexPermission(new PermissionReindexEvent(dataverse));
        // then
        verify(solrIndexService).indexPermissionsForOneDvObject(dataverse);
        verify(solrIndexService).indexPermissionsForDatasetWithDataFiles(dataset1);
        verify(solrIndexService).indexPermissionsForDatasetWithDataFiles(dataset2);
        verifyNoMoreInteractions(solrIndexService);
    }

    @Test
    public void reindexPermission__dataset() {
        // given
        Dataset dataset = new Dataset();
        dataset.setId(1L);
        // when
        permissionReindexHandler.reindexPermission(new PermissionReindexEvent(dataset));
        // then
        verify(solrIndexService).indexPermissionsForDatasetWithDataFiles(dataset);
        verifyNoMoreInteractions(solrIndexService);
    }

    @Test
    public void reindexPermission__datafile() {
        // given
        DataFile datafile = new DataFile();
        datafile.setId(1L);
        // when
        permissionReindexHandler.reindexPermission(new PermissionReindexEvent(datafile));
        // then
        verify(solrIndexService).indexPermissionsForOneDvObject(datafile);
        verifyNoMoreInteractions(solrIndexService);
    }

    @Test
    public void reindexPermission__multiple() {
        // given
        Dataverse dataverse = new Dataverse();
        dataverse.setId(1L);
        Dataset dataset1 = new Dataset();
        dataset1.setId(2L);
        Dataset dataset2 = new Dataset();
        dataset2.setId(3L);

        Dataset dataset3 = new Dataset();
        dataset3.setId(4L);

        DataFile datafile = new DataFile();
        datafile.setId(5L);

        when(datasetRepository.findByOwnerId(1L)).thenReturn(ImmutableList.of(dataset1, dataset2));

        // when
        permissionReindexHandler.reindexPermission(new PermissionReindexEvent(dataverse, dataset3, datafile));
        // then
        verify(solrIndexService).indexPermissionsForOneDvObject(dataverse);
        verify(solrIndexService).indexPermissionsForDatasetWithDataFiles(dataset1);
        verify(solrIndexService).indexPermissionsForDatasetWithDataFiles(dataset2);
        verify(solrIndexService).indexPermissionsForDatasetWithDataFiles(dataset3);
        verify(solrIndexService).indexPermissionsForOneDvObject(datafile);
        verifyNoMoreInteractions(solrIndexService);
    }

}
