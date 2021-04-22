package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.dataset.difference.DatasetFileTermDifferenceItem;
import edu.harvard.iq.dataverse.dataset.difference.LicenseDifferenceFinder;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.GlobalId;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.TermsOfUseRepository;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionRepository;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.group.IPv4Address;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DatasetPageFacadeTest {

    @Mock
    private DatasetVersionRepository dsvRepository;

    @Mock
    private LicenseDifferenceFinder licenseDifferenceFinder;
    
    @Mock
    private DataFileServiceBean dataFileService;
    
    @Mock
    private TermsOfUseRepository termsOfUseRepository;

    @Mock
    private DatasetDao datasetDao;

    @Mock
    private DataverseDao dataverseDao;

    @InjectMocks
    private DatasetPageFacade datasetPageFacade;

    @Test
    void isLatestDatasetWithAnyFilesIncluded() {
        //given
        Long dsvId = 1L;
        DatasetVersion datasetVersion = new DatasetVersion();


        //when
        Mockito.when(dsvRepository.findById(dsvId)).thenReturn(Optional.of(datasetVersion));

        boolean latestDatasetWithAnyFilesIncluded = datasetPageFacade.isLatestDatasetWithAnyFilesIncluded(dsvId);

        //then
        assertThat(latestDatasetWithAnyFilesIncluded).isFalse();
    }

    @Test
    void isLatestDatasetWithAnyFilesIncluded_WithFiles() {
        //given
        Long dsvId = 1L;
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.addFileMetadata(new FileMetadata());

        //when
        Mockito.when(dsvRepository.findById(dsvId)).thenReturn(Optional.of(datasetVersion));

        boolean latestDatasetWithAnyFilesIncluded = datasetPageFacade.isLatestDatasetWithAnyFilesIncluded(dsvId);

        //then
        assertThat(latestDatasetWithAnyFilesIncluded).isTrue();
    }

    @Test
    void loadFilesTermDiffs() {
        //given
        Long dsvId = 1L;
        Long secondDsvId = 2L;

        //when
        Mockito.when(dsvRepository.findById(dsvId)).thenReturn(Optional.of(new DatasetVersion()));
        Mockito.when(dsvRepository.findById(secondDsvId)).thenReturn(Optional.of(new DatasetVersion()));
        Mockito.when(licenseDifferenceFinder.getLicenseDifference(Mockito.any(), Mockito.any())).thenReturn(new ArrayList<>());

        List<DatasetFileTermDifferenceItem> datasetFileTermDifferenceItems = datasetPageFacade.loadFilesTermDiffs(dsvId, secondDsvId);

        //then
        assertThat(datasetFileTermDifferenceItems).isEmpty();
    }

    @Test
    void isSameTermsOfUseForAllFiles() {
        //given
        Long dsvId = 1L;

        //when
        Mockito.when(dsvRepository.findById(dsvId)).thenReturn(Optional.of(new DatasetVersion()));

        boolean sameTermsOfUseForAllFiles = datasetPageFacade.isSameTermsOfUseForAllFiles(dsvId);

        //then
        assertThat(sameTermsOfUseForAllFiles).isTrue();
    }

    @Test
    void isSameTermsOfUseForAllFiles_WithFiles() {
        //given
        Long dsvId = 1L;
        DatasetVersion dsv = new DatasetVersion();
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setTermsOfUse(new FileTermsOfUse());
        dsv.addFileMetadata(fileMetadata);


        //when
        Mockito.when(dsvRepository.findById(dsvId)).thenReturn(Optional.of(dsv));
        Mockito.when(dataFileService.isSameTermsOfUse(Mockito.any(), Mockito.any())).thenReturn(true);

        boolean sameTermsOfUseForAllFiles = datasetPageFacade.isSameTermsOfUseForAllFiles(dsvId);

        //then
        assertThat(sameTermsOfUseForAllFiles).isTrue();
    }

    @Test
    void getTermsOfUseOfFirstFile() {
        //given
        Long dsvId = 1L;

        //when
        Mockito.when(termsOfUseRepository.retrieveFirstFileTermsOfUse(Mockito.any())).thenReturn(Optional.of(new FileTermsOfUse()));

        Optional<FileTermsOfUse> termsOfUseOfFirstFile = datasetPageFacade.getTermsOfUseOfFirstFile(dsvId);

        //then
        assertThat(termsOfUseOfFirstFile).isPresent();

    }

    @Test
    void retrieveDataset() {
        //given
        Long dsId = 1L;
        Dataset dataset = new Dataset();
        dataset.setId(1L);

        //when
        Mockito.when(datasetDao.find(dsId)).thenReturn(dataset);
        Dataset foundDataset = datasetPageFacade.retrieveDataset(dsId);

        //then
        assertThat(foundDataset.getId()).isEqualTo(dsId);
    }

    @Test
    void findByGlobalId() {
        //given
        String globalId = "doi:10.5072/FK2/BYM3IW";
        Dataset dataset = new Dataset();
        dataset.setGlobalId(new GlobalId(globalId));

        //when
        Mockito.when(datasetDao.findByGlobalId(globalId)).thenReturn(dataset);
        Dataset foundDataset = datasetPageFacade.findByGlobalId(globalId);

        //then
        assertThat(foundDataset.getGlobalIdString()).isEqualTo(globalId);
    }

    @Test
    void filterDataversesForLinking() {
        //given
        String query = "test";
        DataverseRequest dataverseRequest = new DataverseRequest(new AuthenticatedUser(), new IPv4Address(1));
        Dataset dataset = new Dataset();

        //when
        Mockito.when(dataverseDao.filterDataversesForLinking(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new ArrayList<>());
        List<Dataverse> foundDataverses = datasetPageFacade.filterDataversesForLinking(query, dataverseRequest, dataset);

        //then
        assertThat(foundDataverses).isEmpty();
    }

    @Test
    void assignDatasetThumbnailByNativeQuery() {
        //given
        Dataset dataset = new Dataset();
        DataFile file = new DataFile();

        //when
        datasetPageFacade.assignDatasetThumbnailByNativeQuery(dataset, file);

        //then
        Mockito.verify(datasetDao, Mockito.times(1)).assignDatasetThumbnailByNativeQuery(dataset, file);
    }

    @Test
    void isMinorUpdate() {
        //given
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);

        Dataset dataset = new Dataset();
        dataset.getVersions().add(datasetVersion);
        datasetVersion.setDataset(dataset);

        //when
        Mockito.when(dsvRepository.findById(Mockito.any())).thenReturn(Optional.of(datasetVersion));

        boolean isMinorUpdate = datasetPageFacade.isMinorUpdate(1L);

        //then
        assertThat(isMinorUpdate).isTrue();

    }

    @Test
    void getFileSize() {
        //given
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.addFileMetadata(new FileMetadata());

        //when
        when(dsvRepository.findById(any())).thenReturn(Optional.of(datasetVersion));

        int fileSize = datasetPageFacade.getFileSize(1L);

        //then
        assertThat(fileSize).isEqualTo(1);
    }
}