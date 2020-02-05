package edu.harvard.iq.dataverse.search.index;

import com.amazonaws.thirdparty.apache.codec.binary.StringUtils;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private SearchPermissionsFinder searchPermissionsService;
    @Mock
    private DvObjectServiceBean dvObjectService;
    @Mock
    private DatasetDao datasetDao;

    // -------------------- TESTS --------------------
    
    @Test
    public void determinePermissionsDocsOnAll() {
        // given
        Dataverse dataverse1 = constructDataverse(1L);
        Dataverse dataverse2 = constructDataverse(2L);
        
        when(dvObjectService.findAll()).thenReturn(Lists.newArrayList(dataverse1, dataverse2));
        
        when(searchPermissionsService.findDataversePerms(dataverse1)).thenReturn(
                new SearchPermissions(Lists.newArrayList("perm1", "perm2"), Instant.EPOCH));
        
        when(searchPermissionsService.findDataversePerms(dataverse2)).thenReturn(
                new SearchPermissions(Lists.newArrayList("perm3"), Instant.EPOCH));
        
        // when
        List<PermissionsSolrDoc> permissionsDocs = permissionsDocFactory.determinePermissionsDocsOnAll();
        
        // then
        assertEquals(2, permissionsDocs.size());
        
        PermissionsSolrDoc permDoc1 = extractPermissionsSolrDoc(permissionsDocs, "dataverse_1");
        assertPermissionsSolrDoc(permDoc1, 1L, "dataverse_1", null, Lists.newArrayList("perm1", "perm2"), Instant.EPOCH);

        PermissionsSolrDoc permDoc2 = extractPermissionsSolrDoc(permissionsDocs, "dataverse_2");
        assertPermissionsSolrDoc(permDoc2, 2L, "dataverse_2", null, Lists.newArrayList("perm3"), Instant.EPOCH);
    }
    
    @Test
    public void determinePermissionsDocsOnSelfAndChildren__dataverse() {
        // given
        Dataverse dataverse = constructDataverse(1L);
        
        Dataset dataset1 = constructDataset(2L);
        DatasetVersion version11 = constructDatasetVersion(21L, VersionState.DRAFT, dataset1);
        
        Dataset dataset2 = constructDataset(3L);
        DatasetVersion version21 = constructDatasetVersion(31L, VersionState.DRAFT, dataset2);
        
        when(datasetDao.findByOwnerId(1L)).thenReturn(Lists.newArrayList(dataset1, dataset2));
        
        when(searchPermissionsService.extractVersionsForPermissionIndexing(dataset1)).thenReturn(Sets.newHashSet(version11));
        when(searchPermissionsService.extractVersionsForPermissionIndexing(dataset2)).thenReturn(Sets.newHashSet(version21));
        
        when(searchPermissionsService.findDataversePerms(dataverse)).thenReturn(
                new SearchPermissions(Lists.newArrayList("perm1"), Instant.EPOCH));
        
        when(searchPermissionsService.findDatasetVersionPerms(version11)).thenReturn(
                new SearchPermissions(Lists.newArrayList("perm21"), Instant.EPOCH));
        
        when(searchPermissionsService.findDatasetVersionPerms(version21)).thenReturn(
                new SearchPermissions(Lists.newArrayList("perm31"), Instant.EPOCH));
        
        // when
        List<PermissionsSolrDoc> permissionsDocs = permissionsDocFactory.determinePermissionsDocsOnSelfAndChildren(dataverse);
        
        // then
        assertEquals(3, permissionsDocs.size());
        
        PermissionsSolrDoc permDoc1 = extractPermissionsSolrDoc(permissionsDocs, "dataverse_1");
        assertPermissionsSolrDoc(permDoc1, 1L, "dataverse_1", null, Lists.newArrayList("perm1"), Instant.EPOCH);
        
        PermissionsSolrDoc permDoc2 = extractPermissionsSolrDoc(permissionsDocs, "dataset_2_draft");
        assertPermissionsSolrDoc(permDoc2, 2L, "dataset_2_draft", 21L, Lists.newArrayList("perm21"), Instant.EPOCH);

        PermissionsSolrDoc permDoc3 = extractPermissionsSolrDoc(permissionsDocs, "dataset_3_draft");
        assertPermissionsSolrDoc(permDoc3, 3L, "dataset_3_draft", 31L, Lists.newArrayList("perm31"), Instant.EPOCH);
    }
    
    @Test
    public void determinePermissionsDocsOnSelfAndChildren__dataset_with_files() {
        // given
        Dataset dataset = constructDataset(1L);
        DatasetVersion version1 = constructDatasetVersion(11L, VersionState.DRAFT, dataset);
        
        DataFile datafile1 = constructDataFile(2L, dataset);
        constructFileMetadata(21L, datafile1, version1);
        
        DataFile datafile2 = constructDataFile(3L, dataset);
        constructFileMetadata(22L, datafile2, version1);
        
        when(searchPermissionsService.extractVersionsForPermissionIndexing(dataset)).thenReturn(
                Sets.newHashSet(version1));
        
        when(searchPermissionsService.findDatasetVersionPerms(version1)).thenReturn(
                new SearchPermissions(Lists.newArrayList("perm1"), Instant.EPOCH));
        
        when(searchPermissionsService.findFileMetadataPermsFromDatasetVersion(version1)).thenReturn(
                new SearchPermissions(Lists.newArrayList("perm2"), Instant.EPOCH));
        
        // when
        List<PermissionsSolrDoc> permissionsDocs = permissionsDocFactory.determinePermissionsDocsOnSelfAndChildren(dataset);
        
        // then
        assertEquals(3, permissionsDocs.size());
        
        PermissionsSolrDoc permDoc1 = extractPermissionsSolrDoc(permissionsDocs, "dataset_1_draft");
        assertPermissionsSolrDoc(permDoc1, 1L, "dataset_1_draft", 11L, Lists.newArrayList("perm1"), Instant.EPOCH);
        
        PermissionsSolrDoc permDoc2 = extractPermissionsSolrDoc(permissionsDocs, "datafile_2_draft");
        assertPermissionsSolrDoc(permDoc2, 2L, "datafile_2_draft", 11L, Lists.newArrayList("perm2"), Instant.EPOCH);
        
        PermissionsSolrDoc permDoc3 = extractPermissionsSolrDoc(permissionsDocs, "datafile_3_draft");
        assertPermissionsSolrDoc(permDoc3, 3L, "datafile_3_draft", 11L, Lists.newArrayList("perm2"), Instant.EPOCH);
    }
    
    @Test
    public void determinePermissionsDocsOnSelfOnly__dataverse() {
        // given
        Dataverse dataverse = constructDataverse(1L);
        
        when(searchPermissionsService.findDataversePerms(dataverse)).thenReturn(
                new SearchPermissions(Lists.newArrayList("perm1"), Instant.EPOCH));
        
        // when
        List<PermissionsSolrDoc> permissionsDocs = permissionsDocFactory.determinePermissionsDocsOnSelfOnly(dataverse);
        
        // then
        assertEquals(1, permissionsDocs.size());
        
        PermissionsSolrDoc permDoc1 = extractPermissionsSolrDoc(permissionsDocs, "dataverse_1");
        assertPermissionsSolrDoc(permDoc1, 1L, "dataverse_1", null, Lists.newArrayList("perm1"), Instant.EPOCH);
    }
    
    @Test
    public void determinePermissionsDocsOnSelfOnly__dataset__with_multiple_versions() {
        // given
        Dataset dataset = constructDataset(1L);
        DatasetVersion version3 = constructDatasetVersion(13L, VersionState.DRAFT, dataset);
        DatasetVersion version2 = constructDatasetVersion(12L, VersionState.RELEASED, dataset);
        constructDatasetVersion(11L, VersionState.RELEASED, dataset);
        
        when(searchPermissionsService.extractVersionsForPermissionIndexing(dataset)).thenReturn(
                Sets.newHashSet(version3, version2));
        
        when(searchPermissionsService.findDatasetVersionPerms(version3)).thenReturn(
                new SearchPermissions(Lists.newArrayList("perm3"), Instant.EPOCH));
        
        when(searchPermissionsService.findDatasetVersionPerms(version2)).thenReturn(
                new SearchPermissions(Lists.newArrayList("perm2"), Instant.EPOCH));
        
        // when
        List<PermissionsSolrDoc> permissionsDocs = permissionsDocFactory.determinePermissionsDocsOnSelfOnly(dataset);
        
        // then
        assertEquals(2, permissionsDocs.size());
        
        PermissionsSolrDoc permDoc2 = extractPermissionsSolrDoc(permissionsDocs, "dataset_1_draft");
        assertPermissionsSolrDoc(permDoc2, 1L, "dataset_1_draft", 13L, Lists.newArrayList("perm3"), Instant.EPOCH);

        PermissionsSolrDoc permDoc3 = extractPermissionsSolrDoc(permissionsDocs, "dataset_1");
        assertPermissionsSolrDoc(permDoc3, 1L, "dataset_1", 12L, Lists.newArrayList("perm2"), Instant.EPOCH);
        
    }
    
    @Test
    public void determinePermissionsDocsOnSelfOnly__datafile() {
        // given
        Dataset dataset = constructDataset(1L);
        DatasetVersion version2 = constructDatasetVersion(12L, VersionState.DRAFT, dataset);
        DatasetVersion version1 = constructDatasetVersion(11L, VersionState.RELEASED, dataset);
        constructDatasetVersion(11L, VersionState.RELEASED, dataset);
        
        DataFile datafile = constructDataFile(2L, dataset);
        constructFileMetadata(21L, datafile, version2);
        constructFileMetadata(21L, datafile, version1);
        
        when(searchPermissionsService.extractVersionsForPermissionIndexing(dataset)).thenReturn(
                Sets.newHashSet(version2, version1));
        
        when(searchPermissionsService.findFileMetadataPermsFromDatasetVersion(version2)).thenReturn(
                new SearchPermissions(Lists.newArrayList("perm2"), Instant.EPOCH));
        
        when(searchPermissionsService.findFileMetadataPermsFromDatasetVersion(version1)).thenReturn(
                new SearchPermissions(Lists.newArrayList("perm1"), Instant.EPOCH));
        
        // when
        List<PermissionsSolrDoc> permissionsDocs = permissionsDocFactory.determinePermissionsDocsOnSelfOnly(datafile);
        
        // then
        assertEquals(2, permissionsDocs.size());
        
        PermissionsSolrDoc permDoc2 = extractPermissionsSolrDoc(permissionsDocs, "datafile_2_draft");
        assertPermissionsSolrDoc(permDoc2, 2L, "datafile_2_draft", 12L, Lists.newArrayList("perm2"), Instant.EPOCH);

        PermissionsSolrDoc permDoc3 = extractPermissionsSolrDoc(permissionsDocs, "datafile_2");
        assertPermissionsSolrDoc(permDoc3, 2L, "datafile_2", 11L, Lists.newArrayList("perm1"), Instant.EPOCH);
        
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
    
    private PermissionsSolrDoc extractPermissionsSolrDoc(List<PermissionsSolrDoc> permissionsDoc, String solrId) {
        return permissionsDoc.stream().filter(x -> StringUtils.equals(x.getSolrId(), solrId))
            .findFirst()
            .get();
    }
    
    private void assertPermissionsSolrDoc(PermissionsSolrDoc actualPermissionsDoc,
            long expectedDvObjectId, String expectedSolrId, Long expectedDatasetVersionId,
            List<String> expectedStrings, Instant expectedPublicFrom) {
        
        assertEquals(expectedDvObjectId, actualPermissionsDoc.getDvObjectId());
        assertEquals(expectedSolrId, actualPermissionsDoc.getSolrId());
        assertEquals(expectedDatasetVersionId, actualPermissionsDoc.getDatasetVersionId());
        
        SearchPermissions actualSearchPermissions = actualPermissionsDoc.getPermissions();
        
        assertThat(actualSearchPermissions.getPermissions(), contains(expectedStrings.toArray()));
        assertEquals(expectedPublicFrom, actualSearchPermissions.getPublicFrom());
    }
    
}
