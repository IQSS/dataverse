package edu.harvard.iq.dataverse.datafile;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.arquillian.arquillianexamples.WebappArquillianDeployment;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDataverseCommand;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.transaction.api.annotation.TransactionMode;
import org.jboss.arquillian.transaction.api.annotation.Transactional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

@RunWith(Arquillian.class)
@Transactional(TransactionMode.ROLLBACK)
public class FileServiceIT extends WebappArquillianDeployment {
    private static final long DRAFT_DATASET_WITH_FILES_ID = 52;

    @EJB
    private FileService fileService;

    @EJB
    private EjbDataverseEngine commandEngine;

    @EJB
    private AuthenticationServiceBean authenticationServiceBean;

    @EJB
    private DatasetDao datasetDao;

    @Inject
    private DataverseSession dataverseSession;

    @Inject
    private DataverseRequestServiceBean requestService;

    // -------------------- TESTS --------------------

    @Before
    public void setUp() {
        dataverseSession.setUser(authenticationServiceBean.getAdminUser());
    }

    @Test
    public void deleteFile_forDraftDataset() {
        // given
        Dataset dataset = datasetDao.find(DRAFT_DATASET_WITH_FILES_ID);

        Tuple2<VersionState, Long> versionDataBefore = getLatestVersionData(dataset);

        List<DataFile> files = dataset.getFiles();
        DataFile fileToDelete = files.get(0);

        // when
        Dataset updatedDataset = fileService.deleteFile(fileToDelete.getFileMetadata());

        // then
        List<DataFile> updatedFiles = updatedDataset.getFiles();

        assertThat("State and version after delete should match",
                getLatestVersionData(updatedDataset), equalTo(versionDataBefore));
        assertThat("File list in updated draft should not contain deleted file",
                fileToDelete, not(in(updatedFiles))); // DataFile#equals(…) is based only on file's id
    }

    @Test
    public void deleteFile_forReleasedDataset() {
        // given
        publishDataset(DRAFT_DATASET_WITH_FILES_ID);
        Dataset dataset = datasetDao.find(DRAFT_DATASET_WITH_FILES_ID);

        Tuple2<VersionState, Long> versionDataBefore = getLatestVersionData(dataset);

        DatasetVersion versionBefore = dataset.getLatestVersion();
        List<FileMetadata> filesMetadataBefore = versionBefore.getFileMetadatas();
        FileMetadata fileToDeleteMetadata = filesMetadataBefore.get(0);

        // when
        Dataset updatedDataset = fileService.deleteFile(fileToDeleteMetadata);

        // then
        DatasetVersion versionAfter = updatedDataset.getLatestVersion();
        Tuple2<VersionState, Long> versionDataAfter = getLatestVersionData(updatedDataset);

        assertThat("State before and after delete should be different and state after delete should be DRAFT",
                versionDataAfter._1, allOf(not(equalTo(versionDataBefore._1)), equalTo(VersionState.DRAFT)));
        assertThat("Files' metadata in new version should not contain metadata of deleted file",
                fileToDeleteMetadata, not(in(versionAfter.getFileMetadatas())));
        assertThat(
                "Deleted file should be present in dataset' file collection, as it is used by previously published version(s)",
                fileToDeleteMetadata, in(extractFileListMetadata(updatedDataset))
        );
    }

    @Test
    public void deleteFiles_forDraft() {
        // given
        Dataset dataset = datasetDao.find(DRAFT_DATASET_WITH_FILES_ID);

        Tuple2<VersionState, Long> versionDataBefore = getLatestVersionData(dataset);

        List<DataFile> files = dataset.getFiles();
        Set<FileMetadata> filesToDeleteMetadata = files.stream()
                .map(DataFile::getFileMetadata)
                .collect(Collectors.toSet());

        // when
        Set<Dataset> results = fileService.deleteFiles(filesToDeleteMetadata);

        // then
        Dataset updatedDataset = findNewestResult(results);

        assertThat("State and version after delete should match",
                getLatestVersionData(updatedDataset), equalTo(versionDataBefore));
        assertThat("All files should have been deleted from dataset file list",
                updatedDataset.getFiles(), empty());
    }

    @Test
    public void deleteFiles_forReleasedDataset() {
        // given
        publishDataset(DRAFT_DATASET_WITH_FILES_ID);
        Dataset dataset = datasetDao.find(DRAFT_DATASET_WITH_FILES_ID);

        Tuple2<VersionState, Long> versionDataBefore = getLatestVersionData(dataset);

        DatasetVersion latestVersionBefore = dataset.getLatestVersion();
        List<FileMetadata> filesToDeleteMetadata = latestVersionBefore.getFileMetadatas();

        // when
        Set<Dataset> results = fileService.deleteFiles(filesToDeleteMetadata);

        // then
        Dataset updatedDataset = findNewestResult(results);
        DatasetVersion versionAfter = updatedDataset.getLatestVersion();
        Tuple2<VersionState, Long> versionDataAfter = getLatestVersionData(updatedDataset);

        assertThat("State before and after delete should be different and state after delete should be DRAFT",
                versionDataAfter._1, allOf(not(equalTo(versionDataBefore._1)), equalTo(VersionState.DRAFT)));
        assertThat("All files metadata should have been deleted from latest version metadata",
                versionAfter.getFileMetadatas(), empty());
        assertThat(
                "Deleted file should be present in dataset file collection, as they're used by previously published version(s)",
                filesToDeleteMetadata, everyItem(in(extractFileListMetadata(updatedDataset)))
        );
    }

    // -------------------- PRIVATE --------------------

    private Tuple2<VersionState, Long> getLatestVersionData(Dataset dataset) {
        DatasetVersion latestVersion = dataset.getLatestVersion();
        return Tuple.of(latestVersion.getVersionState(), latestVersion.getVersionNumber());
    }

    private Dataset findNewestResult(Collection<Dataset> datasets) {
        return datasets.stream()
                .reduce((prev, next) -> next.getModificationTime().after(prev.getModificationTime()) ? next : prev)
                .orElseThrow(() -> new IllegalArgumentException("Cannot find newest result: possibly wrong argument"));
    }

    private List<FileMetadata> extractFileListMetadata(Dataset dataset) {
        List<DataFile> files = dataset.getFiles();
        return files.stream()
                .map(DataFile::getFileMetadata)
                .collect(toList());
    }

    private void publishDataset(Long datasetId) {
        Dataset dataset = datasetDao.find(datasetId);
        Dataverse dataverseForDataset = dataset.getDataverseContext();
        publishDataverse(dataverseForDataset);
        commandEngine.submit(new PublishDatasetCommand(dataset, requestService.getDataverseRequest(), false));
    }

    private void publishDataverse(Dataverse dataverse) {
        if (dataverse.isReleased()) {
            return;
        }
        Dataverse owner = dataverse.getOwner();
        if (owner != null) {
            publishDataverse(owner);
        }
        commandEngine.submit(new PublishDataverseCommand(requestService.getDataverseRequest(), dataverse));
    };
}