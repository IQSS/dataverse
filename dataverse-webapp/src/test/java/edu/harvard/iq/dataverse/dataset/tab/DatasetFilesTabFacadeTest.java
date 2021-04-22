package edu.harvard.iq.dataverse.dataset.tab;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.datafile.page.FileDownloadHelper;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileCategory;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.License;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionRepository;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.assertj.core.api.Condition;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatasetFilesTabFacadeTest {

    @Mock
    private FileDownloadHelper fileDownloadHelper;

    @Mock
    private DatasetVersionRepository datasetVersionRepository;

    @Mock
    private DatasetDao datasetDao;

    @InjectMocks
    private DatasetFilesTabFacade datasetFilesTabFacade;

    @Test
    void isVersionContainsDownloadableFiles() {
        //given
        Long id = 1L;
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.addFileMetadata(generateFileMetadata());

        //when
        when(datasetVersionRepository.findById(id)).thenReturn(Optional.of(datasetVersion));
        when(fileDownloadHelper.canUserDownloadFile(any())).thenReturn(true);

        boolean versionContainsDownloadableFiles = datasetFilesTabFacade.isVersionContainsDownloadableFiles(id);

        //then
        assertThat(versionContainsDownloadableFiles).isTrue();
    }

    @Test
    void isVersionContainsNonDownloadableFiles() {
        //given
        Long id = 1L;
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.addFileMetadata(generateFileMetadata());

        //when
        when(datasetVersionRepository.findById(id)).thenReturn(Optional.of(datasetVersion));
        when(fileDownloadHelper.canUserDownloadFile(any())).thenReturn(false);

        boolean versionContainsDownloadableFiles = datasetFilesTabFacade.isVersionContainsDownloadableFiles(id);

        //then
        assertThat(versionContainsDownloadableFiles).isFalse();
    }

    @Test
    void updateFileTagsAndCategories() {
        //given
        Long id = 1L;
        List<FileMetadata> selectedFiles = new ArrayList<>();
        selectedFiles.add(generateFileMetadata());
        selectedFiles.add(generateFileMetadata());
        selectedFiles.add(generateFileMetadata());
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.getFileMetadatas().addAll(selectedFiles);

        List<String> selectedFileMetadataTags = new ArrayList<>();
        String metaTag = "metaTag";
        selectedFileMetadataTags.add(metaTag);
        List<String> selectedDataFileTags = new ArrayList<>();
        selectedDataFileTags.add("dataTag");


        //when
        when(datasetVersionRepository.findById(id)).thenReturn(Optional.of(datasetVersion));
        DatasetVersion updatedDsv = datasetFilesTabFacade.updateFileTagsAndCategories(id, selectedFiles, selectedFileMetadataTags, selectedDataFileTags);

        //then
        List<FileMetadata> updatedFiles = updatedDsv.getFileMetadatas();

        assertThat(updatedFiles.get(0)).extracting(FileMetadata::getCategoriesByName).is(new Condition<>(strings -> strings.contains(metaTag), ""));
        assertThat(updatedFiles.get(1)).extracting(FileMetadata::getCategoriesByName).is(new Condition<>(strings -> strings.contains(metaTag), ""));
        assertThat(updatedFiles.get(2)).extracting(FileMetadata::getCategoriesByName).is(new Condition<>(strings -> strings.contains(metaTag), ""));

    }

    @Test
    void updateFileTagsAndCategories_WithFileMetadataMissmatch() {
        //given
        Long id = 1L;
        List<FileMetadata> selectedFiles = new ArrayList<>();
        selectedFiles.add(generateFileMetadata());
        selectedFiles.add(generateFileMetadata());
        selectedFiles.add(generateFileMetadata());
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.addFileMetadata(generateFileMetadata());

        List<String> selectedFileMetadataTags = new ArrayList<>();
        String metaTag = "metaTag";
        selectedFileMetadataTags.add(metaTag);
        List<String> selectedDataFileTags = new ArrayList<>();
        selectedDataFileTags.add("dataTag");


        //when
        when(datasetVersionRepository.findById(id)).thenReturn(Optional.of(datasetVersion));
        DatasetVersion updatedDsv = datasetFilesTabFacade.updateFileTagsAndCategories(id, selectedFiles, selectedFileMetadataTags, selectedDataFileTags);

        //then
        List<FileMetadata> updatedFiles = updatedDsv.getFileMetadatas();

        assertThat(updatedFiles.get(0)).extracting(FileMetadata::getCategoriesByName).is(new Condition<>(List::isEmpty, ""));

    }

    @Test
    void updateTermsOfUse() {
        //given
        Long id = 1L;
        FileMetadata fileMetadata = generateFileMetadata();
        FileTermsOfUse termsOfUse = new FileTermsOfUse();
        termsOfUse.setId(1L);
        fileMetadata.setTermsOfUse(termsOfUse);
        FileTermsOfUse providedTerms = new FileTermsOfUse();
        providedTerms.setId(2L);
        License license = new License();
        providedTerms.setLicense(license);
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.addFileMetadata(fileMetadata);

        //when
        when(datasetVersionRepository.findById(id)).thenReturn(Optional.of(datasetVersion));

        DatasetVersion updatedDsv = datasetFilesTabFacade.updateTermsOfUse(id, providedTerms, Lists.newArrayList(fileMetadata));

        //then
        assertThat(updatedDsv.getFileMetadatas().get(0).getTermsOfUse().getLicense()).isEqualTo(license);
    }

    @Test
    void updateTermsOfUse_WithFileMissmatch() {
        //given
        Long id = 1L;
        FileMetadata fileMetadata = generateFileMetadata();
        fileMetadata.setId(100L);

        FileTermsOfUse termsOfUse = new FileTermsOfUse();
        termsOfUse.setId(1L);
        fileMetadata.setTermsOfUse(termsOfUse);

        FileTermsOfUse providedTerms = new FileTermsOfUse();
        providedTerms.setId(2L);
        License license = new License();
        providedTerms.setLicense(license);
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.addFileMetadata(generateFileMetadata());

        //when
        when(datasetVersionRepository.findById(id)).thenReturn(Optional.of(datasetVersion));

        DatasetVersion updatedDsv = datasetFilesTabFacade.updateTermsOfUse(id, providedTerms, Lists.newArrayList(fileMetadata));

        //then
        assertThat(updatedDsv.getFileMetadatas().get(0).getTermsOfUse()).isNull();
    }

    @Test
    void fetchCategoriesByName() {
        //given
        Long id = 1L;
        DatasetVersion datasetVersion = new DatasetVersion();
        FileMetadata fileMetadata = new FileMetadata();
        DataFileCategory category = new DataFileCategory();
        String categoryName = "testName";
        category.setName(categoryName);
        fileMetadata.addCategory(category);
        datasetVersion.addFileMetadata(fileMetadata);

        //when
        when(datasetVersionRepository.findById(id)).thenReturn(Optional.of(datasetVersion));

        Set<String> categoriesByName = datasetFilesTabFacade.fetchCategoriesByName(id);

        //then
        assertThat(categoriesByName).allMatch(s -> s.equals(categoryName));

    }

    @Test
    void retrieveDataset() {
        //given
        Long id = 1L;
        Dataset dataset = new Dataset();
        dataset.setId(id);

        //when
        when(datasetDao.find(id)).thenReturn(dataset);

        Dataset foundDataset = datasetFilesTabFacade.retrieveDataset(id);

        //then
        assertThat(foundDataset.getId()).isEqualTo(id);

    }

    @Test
    void addDatasetLock() {
        //given
        DatasetLock datasetLock = new DatasetLock(DatasetLock.Reason.InReview, new AuthenticatedUser());
        datasetLock.setId(1L);

        //when
        when(datasetDao.addDatasetLock(any(), any(), any(), any())).thenReturn(datasetLock);

        DatasetLock addedLock = datasetFilesTabFacade.addDatasetLock(1L, DatasetLock.Reason.InReview, 1L, "");

        //then
        assertThat(addedLock.getId()).isEqualTo(datasetLock.getId());

    }

    @Test
    void fileSize() {
        //given
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.addFileMetadata(new FileMetadata());

        //when
        when(datasetVersionRepository.findById(any())).thenReturn(Optional.of(datasetVersion));

        int fileSize = datasetFilesTabFacade.fileSize(1L);

        //then
        assertThat(fileSize).isEqualTo(1);

    }

    @Test
    void retrieveDatasetFileCategories() {
        //given
        Dataset dataset = new Dataset();
        dataset.addFileCategory(new DataFileCategory());

        //when
        when(datasetDao.find(any())).thenReturn(dataset);

        List<DataFileCategory> dataFileCategories = datasetFilesTabFacade.retrieveDatasetFileCategories(1L);

        //then
        assertThat(dataFileCategories).size().isEqualTo(1);

    }

    @Test
    void removeDatasetFileCategories() {
        //given
        Dataset dataset = new Dataset();
        DataFileCategory category = new DataFileCategory();
        dataset.addFileCategory(category);

        //when
        when(datasetDao.find(any())).thenReturn(dataset);

        datasetFilesTabFacade.removeDatasetFileCategories(1L, Lists.newArrayList(category));

        //then
        assertThat(dataset.getCategories()).size().isEqualTo(0);

    }

    @NotNull
    private FileMetadata generateFileMetadata() {
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setId(new Random().nextLong());
        fileMetadata.setDataFile(new DataFile());
        return fileMetadata;
    }
}