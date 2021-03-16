package edu.harvard.iq.dataverse.persistence.datafile;

import edu.harvard.iq.dataverse.persistence.PersistenceArquillianDeployment;
import org.assertj.core.util.Lists;
import org.junit.Test;

import javax.inject.Inject;
import java.util.ArrayList;
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
        assertEquals(foundFiles.size(), 3);
        assertEquals(110, foundFiles.get(0).getId().longValue());
        assertEquals(112, foundFiles.get(1).getId().longValue());
        assertEquals(113, foundFiles.get(2).getId().longValue());
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
        assertEquals(foundFiles.size(), 3);
        assertEquals(110, foundFiles.get(0).longValue());
        assertEquals(112, foundFiles.get(1).longValue());
        assertEquals(113, foundFiles.get(2).longValue());
    }

    @Test
    public void findFileMetadata() {
        //given
        ArrayList<Long> fileIds = Lists.newArrayList(112L, 110L);

        //when
        List<FileMetadata> foundFiles = fileMetadataRepository.findFileMetadata(fileIds);

        //then
        assertEquals(foundFiles.size(), 2);
        assertEquals(110, foundFiles.get(0).getId().longValue());
        assertEquals(112, foundFiles.get(1).getId().longValue());
    }

    @Test
    public void findRestrictedFileMetadata() {
        //given
        ArrayList<Long> fileIds = Lists.newArrayList(113L, 112L, 110L);

        //when
        List<FileMetadata> foundRestrictedFiles = fileMetadataRepository.findRestrictedFileMetadata(fileIds);

        //then
        assertEquals(foundRestrictedFiles.size(), 1);
        assertEquals(113, foundRestrictedFiles.get(0).getId().longValue());

    }
}
