package edu.harvard.iq.dataverse.persistence.datafile;

import edu.harvard.iq.dataverse.persistence.PersistenceArquillianDeployment;
import org.junit.Test;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class FileMetadataRepositoryIT extends PersistenceArquillianDeployment {

    @Inject
    private FileMetadataRepository fileMetadataRepository;

    @Test
    public void findFileMetadataByDatasetVersionIdWithPagination() {
        //given
        int datasetVersionId = 36;

        //when
        List<FileMetadata> foundFiles = fileMetadataRepository.findFileMetadataByDatasetVersionIdWithPagination(datasetVersionId, 0, 10);

        //then
        assertEquals(foundFiles.size(), 2);
        assertEquals(110, foundFiles.get(0).getId().longValue());
        assertEquals(112, foundFiles.get(1).getId().longValue());
    }

    @Test
    public void findSearchedFileMetadataByDatasetVersionIdWithPagination() {
        //given
        String searchTerm = "testfile6";
        int datasetVersionId = 36;

        //when
        List<FileMetadata> foundFiles = fileMetadataRepository.findSearchedFileMetadataByDatasetVersionIdWithPagination(datasetVersionId, 0, 10, searchTerm);

        //then
        assertEquals(foundFiles.size(), 1);
        assertEquals(110, foundFiles.get(0).getId().longValue());
    }

    @Test
    public void findSearchedFileMetadataByDatasetVersionIdWithPagination_allFiles() {
        //given
        String searchTerm = "testfile";
        int datasetVersionId = 36;

        //when
        List<FileMetadata> foundFiles = fileMetadataRepository.findSearchedFileMetadataByDatasetVersionIdWithPagination(datasetVersionId, 0, 10, searchTerm);

        //then
        assertEquals(foundFiles.size(), 2);
        assertEquals(110, foundFiles.get(0).getId().longValue());
        assertEquals(112, foundFiles.get(1).getId().longValue());
    }

    @Test
    public void findFileMetadataIdsByDatasetVersionId() {
        //given
        int datasetVersionId = 36;

        //when
        List<Long> foundFiles = fileMetadataRepository.findFileMetadataIdsByDatasetVersionId(datasetVersionId);
        Collections.sort(foundFiles);

        //then
        assertEquals(foundFiles.size(), 2);
        assertEquals(110, foundFiles.get(0).longValue());
        assertEquals(112, foundFiles.get(1).longValue());
    }


}
