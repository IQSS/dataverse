package edu.harvard.iq.dataverse.dataset.datasetversion;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.validation.ValidationException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DatasetVersionServiceBeanTest {

    @InjectMocks
    private DatasetVersionServiceBean datasetVersionService;

    @Mock
    private EjbDataverseEngine commandEngine;

    @Mock
    private DataverseRequestServiceBean dvRequestService;

    @Mock
    private EntityManager entityManager;

    private Dataset testDataset =  new Dataset();

    @BeforeEach
    void setUp() {
        testDataset.setId(10L);
        when(commandEngine.submit(any(UpdateDatasetVersionCommand.class))).thenReturn(testDataset);
    }

    // -------------------- TESTS --------------------

    @Test
    public void updateDatasetVersion() {
        //given
        DatasetVersion testDatasetVersion = prepareDataset();
        
        TypedQuery<DatasetVersion> dbQuery = mock(TypedQuery.class);
        when(dbQuery.setParameter("datasetId", 1L)).thenReturn(dbQuery);
        when(dbQuery.setMaxResults(1)).thenReturn(dbQuery);
        when(dbQuery.getSingleResult()).thenReturn(testDatasetVersion);

        //when
        when(entityManager.createQuery(any(), eq(DatasetVersion.class))).thenReturn(dbQuery);
        Dataset updatedDataset = Assertions.assertDoesNotThrow(() -> datasetVersionService.updateDatasetVersion(testDatasetVersion, true));

        //then
        Assert.assertSame(testDataset, updatedDataset);

    }

    @Test
    public void updateDatasetVersion_WithValidationException() {
        //given
        DatasetVersion testDatasetVersion = prepareDataset();

        prepareFileMetadata(testDatasetVersion);

        //when & then
        Assertions.assertThrows(ValidationException.class, () -> datasetVersionService.updateDatasetVersion(testDatasetVersion, true));

    }

    // -------------------- PRIVATE --------------------

    private FileMetadata prepareFileMetadata(DatasetVersion testDatasetVersion) {
        FileMetadata fileToDelete = new FileMetadata();
        fileToDelete.setLabel("");
        fileToDelete.setDatasetVersion(testDatasetVersion);
        fileToDelete.getDatasetVersion().setFileMetadatas(Lists.newArrayList(fileToDelete));

        return fileToDelete;
    }

    private DatasetVersion prepareDataset(){
        DatasetVersion testDatasetVersion = new DatasetVersion();
        testDatasetVersion.setId(1L);
        testDatasetVersion.setVersionState(DatasetVersion.VersionState.DRAFT);
        Dataset testDataset = new Dataset();
        testDataset.setId(1L);
        testDataset.setVersions(Lists.newArrayList(testDatasetVersion));
        testDatasetVersion.setDataset(testDataset);

        return testDatasetVersion;
    }
}