package edu.harvard.iq.dataverse.search.index;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

/**
 * @author madryk
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PermissionsSolrDocFactoryTest {

    @InjectMocks
    private PermissionsSolrDocFactory permissionsDocFactory;

    @Mock
    private SolrPermissionsFinder searchPermissionsService;
    @Mock
    private DvObjectServiceBean dvObjectService;
    @Mock
    private DatasetDao datasetDao;

    private static SolrPermission EMPTY_PERMISSION = new SolrPermission(Permission.AddDataset, Collections.emptyList());
    private static String[] PERMS_DOC_ASSERTIONS = new String[] { "dvObjectId", "solrId", "datasetVersionId",
            "searchPermissions.permissions", "searchPermissions.publicFrom", "addDatasetPermissions.permittedEntities" };

    // -------------------- TESTS --------------------

    @Test
    void determinePermissionsDocsOnAll() {
        // given
        Dataverse dataverse1 = constructDataverse(1L);
        Dataverse dataverse2 = constructDataverse(2L);

        when(dvObjectService.findAll())
                .thenReturn(list(dataverse1, dataverse2));
        when(searchPermissionsService.findDataversePerms(dataverse1))
                .thenReturn(new SolrPermissions(createSearchPermissions("perm1","perm2"), EMPTY_PERMISSION));
        when(searchPermissionsService.findDataversePerms(dataverse2))
                .thenReturn(new SolrPermissions(createSearchPermissions("perm3"), createAddDatasetPermissions("add-perm")));

        // when
        List<PermissionsSolrDoc> permissionsDocs = permissionsDocFactory.determinePermissionsDocsOnAll();

        // then
        assertThat(permissionsDocs).extracting(PERMS_DOC_ASSERTIONS)
                .containsExactlyInAnyOrder(
                        tuple(1L, "dataverse_1", null, list("perm1", "perm2"), Instant.EPOCH, list()),
                        tuple(2L, "dataverse_2", null, list("perm3"), Instant.EPOCH, list("add-perm")));
    }

    @Test
    void determinePermissionsDocsForDatasetWithDataFiles() {
        // given
        Dataset dataset = constructDataset(1L);
        DatasetVersion version1 = constructDatasetVersion(11L, VersionState.DRAFT, dataset);

        DataFile datafile1 = constructDataFile(2L, dataset);
        constructFileMetadata(21L, datafile1, version1);

        DataFile datafile2 = constructDataFile(3L, dataset);
        constructFileMetadata(22L, datafile2, version1);

        when(searchPermissionsService.extractVersionsForPermissionIndexing(dataset))
                .thenReturn(Sets.newHashSet(version1));
        when(searchPermissionsService.findDatasetVersionPerms(version1))
                .thenReturn(new SolrPermissions(createSearchPermissions("perm1"), EMPTY_PERMISSION));
        when(searchPermissionsService.findFileMetadataPermsFromDatasetVersion(version1))
                .thenReturn(new SolrPermissions(createSearchPermissions("perm2"), EMPTY_PERMISSION));

        // when
        List<PermissionsSolrDoc> permissionsDocs = permissionsDocFactory.determinePermissionsDocsForDatasetWithDataFiles(dataset);

        // then
        assertThat(permissionsDocs).extracting(PERMS_DOC_ASSERTIONS)
                .containsExactlyInAnyOrder(
                        tuple(1L, "dataset_1_draft", 11L, list("perm1"), Instant.EPOCH, list()),
                        tuple(2L, "datafile_2_draft", 11L, list("perm2"), Instant.EPOCH, list()),
                        tuple(3L, "datafile_3_draft", 11L, list("perm2"), Instant.EPOCH, list()));
    }

    @Test
    void determinePermissionsDocsOnSelfOnly__dataverse() {
        // given
        Dataverse dataverse = constructDataverse(1L);

        when(searchPermissionsService.findDataversePerms(dataverse))
                .thenReturn(new SolrPermissions(createSearchPermissions("perm1"), createAddDatasetPermissions("add-perm")));

        // when
        List<PermissionsSolrDoc> permissionsDocs = permissionsDocFactory.determinePermissionsDocsOnSelfOnly(dataverse);

        // then
        assertThat(permissionsDocs).extracting(PERMS_DOC_ASSERTIONS)
                .containsExactlyInAnyOrder(
                        tuple(1L, "dataverse_1", null, list("perm1"), Instant.EPOCH, list("add-perm")));
    }

    @Test
    void determinePermissionsDocsOnSelfOnly__dataset__with_multiple_versions() {
        // given
        Dataset dataset = constructDataset(1L);
        DatasetVersion version3 = constructDatasetVersion(13L, VersionState.DRAFT, dataset);
        DatasetVersion version2 = constructDatasetVersion(12L, VersionState.RELEASED, dataset);
        constructDatasetVersion(11L, VersionState.RELEASED, dataset);

        when(searchPermissionsService.extractVersionsForPermissionIndexing(dataset))
                .thenReturn(Sets.newHashSet(version3, version2));
        when(searchPermissionsService.findDatasetVersionPerms(version3))
                .thenReturn(new SolrPermissions(createSearchPermissions("perm3"), EMPTY_PERMISSION));
        when(searchPermissionsService.findDatasetVersionPerms(version2))
                .thenReturn(new SolrPermissions(createSearchPermissions("perm2"), EMPTY_PERMISSION));

        // when
        List<PermissionsSolrDoc> permissionsDocs = permissionsDocFactory.determinePermissionsDocsOnSelfOnly(dataset);

        // then
        assertThat(permissionsDocs).extracting(PERMS_DOC_ASSERTIONS)
                .containsExactlyInAnyOrder(
                        tuple(1L, "dataset_1_draft", 13L, list("perm3"), Instant.EPOCH, list()),
                        tuple(1L, "dataset_1", 12L, list("perm2"), Instant.EPOCH, list()));
    }

    @Test
    void determinePermissionsDocsOnSelfOnly__datafile() {
        // given
        Dataset dataset = constructDataset(1L);
        DatasetVersion version2 = constructDatasetVersion(12L, VersionState.DRAFT, dataset);
        DatasetVersion version1 = constructDatasetVersion(11L, VersionState.RELEASED, dataset);
        constructDatasetVersion(11L, VersionState.RELEASED, dataset);

        DataFile datafile = constructDataFile(2L, dataset);
        constructFileMetadata(21L, datafile, version2);
        constructFileMetadata(21L, datafile, version1);

        when(searchPermissionsService.extractVersionsForPermissionIndexing(dataset))
                .thenReturn(Sets.newHashSet(version2, version1));
        when(searchPermissionsService.findFileMetadataPermsFromDatasetVersion(version2))
                .thenReturn(new SolrPermissions(createSearchPermissions("perm2"), EMPTY_PERMISSION));
        when(searchPermissionsService.findFileMetadataPermsFromDatasetVersion(version1))
                .thenReturn(new SolrPermissions(createSearchPermissions("perm1"), EMPTY_PERMISSION));

        // when
        List<PermissionsSolrDoc> permissionsDocs = permissionsDocFactory.determinePermissionsDocsOnSelfOnly(datafile);

        // then
        assertThat(permissionsDocs).extracting(PERMS_DOC_ASSERTIONS)
                .containsExactlyInAnyOrder(
                        tuple(2L, "datafile_2_draft", 12L, list("perm2"), Instant.EPOCH, list()),
                        tuple(2L, "datafile_2", 11L, list("perm1"), Instant.EPOCH, list()));
    }

    // -------------------- PRIVATE --------------------

    private Dataverse constructDataverse(Long dataverseId) {
        Dataverse dataverse = new Dataverse();
        dataverse.setId(dataverseId);
        dataverse.setOwner(new Dataverse());
        return dataverse;
    }

    private Dataset constructDataset(Long datasetId) {
        Dataset dataset = new Dataset();
        dataset.setId(datasetId);
        return dataset;
    }

    private DatasetVersion constructDatasetVersion(Long datasetVersionId, VersionState state, Dataset dataset) {
        DatasetVersion version = new DatasetVersion();
        version.setId(datasetVersionId);
        version.setVersionState(state);
        version.setDataset(dataset);

        dataset.getVersions().add(version);
        return version;
    }

    private DataFile constructDataFile(Long datafileId, Dataset dataset) {
        DataFile dataFile = new DataFile();
        dataFile.setId(datafileId);
        dataFile.setOwner(dataset);

        dataset.getFiles().add(dataFile);
        return dataFile;
    }

    private FileMetadata constructFileMetadata(Long fileMetadataId, DataFile dataFile, DatasetVersion version) {
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setId(fileMetadataId);

        fileMetadata.setDatasetVersion(version);
        fileMetadata.setDataFile(dataFile);

        dataFile.getFileMetadatas().add(fileMetadata);
        version.getFileMetadatas().add(fileMetadata);
        return fileMetadata;
    }

    private SearchPermissions createSearchPermissions(String... permissions) {
        return new SearchPermissions(list(permissions), Instant.EPOCH);
    }

    private SolrPermission createAddDatasetPermissions(String... permissions) {
        return new SolrPermission(Permission.AddDataset, list(permissions));
    }

    @SafeVarargs
    private final <T> List<T> list(T... elements) {
        return elements.length > 0 ? Lists.newArrayList(elements) : Collections.emptyList();
    }
}
