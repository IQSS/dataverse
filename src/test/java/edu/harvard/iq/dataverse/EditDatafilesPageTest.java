package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.storageuse.UploadSessionQuotaLimit;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class EditDatafilesPageTest {
    
    @InjectMocks
    private EditDatafilesPage editDatafilesPage;
    
    @Mock
    private SystemConfig systemConfig;
    
    public EditDatafilesPageTest() {
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
    void testPopulateHumanPerFormatTabularLimits_WithEmptyLimits() {
        Map<String, Long> tabularLimits = new HashMap<>();
        when(systemConfig.getTabularIngestSizeLimits()).thenReturn(tabularLimits);
        
        String result = editDatafilesPage.populateHumanPerFormatTabularLimits();
        
        assertEquals("", result, "Expected no formatted limits when the map is empty");
    }
    
    @Test
    void testPopulateHumanPerFormatTabularLimits_WithNonDefaultLimits() {
        Map<String, Long> tabularLimits = new HashMap<>();
        tabularLimits.put("csv", 10485760L); // 10MB
        tabularLimits.put("tsv", 5242880L);  // 5MB
        when(systemConfig.getTabularIngestSizeLimits()).thenReturn(tabularLimits);
        
        String result = editDatafilesPage.populateHumanPerFormatTabularLimits();
        
        assertTrue(result.contains("csv: 10.0 MB"), "Expected CSV limit in human-readable format, but got: " + result);
        assertTrue(result.contains("tsv: 5.0 MB"), "Expected TSV limit in human-readable format, but got: " + result);
    }
    
    @Test
    void testPopulateHumanPerFormatTabularLimits_WithDefaultKey() {
        Map<String, Long> tabularLimits = new HashMap<>();
        tabularLimits.put(SystemConfig.TABULAR_INGEST_SIZE_LIMITS_DEFAULT_KEY, 2097152L); // 2MB
        tabularLimits.put("csv", 10485760L); // 10MB
        when(systemConfig.getTabularIngestSizeLimits()).thenReturn(tabularLimits);
        
        String result = editDatafilesPage.populateHumanPerFormatTabularLimits();
        
        assertTrue(result.contains("csv: 10.0 MB"), "Expected CSV limit in human-readable format, but got: " + result);
        assertFalse(result.contains("default"), "Default key should be excluded from the output");
    }

    @Test
    void testMaxTotalUploadSizeTracksCurrentUploadSessionQuota() throws Exception {
        UploadSessionQuotaLimit quota = new UploadSessionQuotaLimit(100L, 80L);
        Field quotaField = EditDatafilesPage.class.getDeclaredField("uploadSessionQuota");
        quotaField.setAccessible(true);
        quotaField.set(editDatafilesPage, quota);

        assertEquals(20L, editDatafilesPage.getMaxTotalUploadSizeInBytes());
        assertEquals("20 B", editDatafilesPage.getHumanMaxTotalUploadSizeInBytes());

        quota.setTotalUsageInBytes(90L);

        assertEquals(10L, editDatafilesPage.getMaxTotalUploadSizeInBytes());
        assertEquals("10 B", editDatafilesPage.getHumanMaxTotalUploadSizeInBytes());
    }

    @Test
    void testMaxTotalUploadSizeFallsBackToConfiguredLimitWithoutQuota() throws Exception {
        Field limitField = EditDatafilesPage.class.getDeclaredField("maxTotalUploadSizeInBytes");
        limitField.setAccessible(true);
        limitField.set(editDatafilesPage, 25L);

        assertEquals(25L, editDatafilesPage.getMaxTotalUploadSizeInBytes());
        assertEquals("25 B", editDatafilesPage.getHumanMaxTotalUploadSizeInBytes());
    }

    @Test
    void testDeletingUnsavedFileReleasesUploadQuota() throws Exception {
        UploadSessionQuotaLimit quota = new UploadSessionQuotaLimit(100L, 90L);
        Field quotaField = EditDatafilesPage.class.getDeclaredField("uploadSessionQuota");
        quotaField.setAccessible(true);
        quotaField.set(editDatafilesPage, quota);
        DataFile dataFile = new DataFile();
        dataFile.setFilesize(10L);
        dataFile.setStorageIdentifier("test-storage-id");
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setDataFile(dataFile);
        fileMetadata.setLabel("test.txt");
        Dataset dataset = mock(Dataset.class);
        DatasetVersion version = mock(DatasetVersion.class);
        List<FileMetadata> displayedFiles = new ArrayList<>(List.of(fileMetadata));
        List<FileMetadata> versionFiles = new ArrayList<>(List.of(fileMetadata));
        List<DataFile> datasetFiles = new ArrayList<>(List.of(dataFile));
        List<DataFile> newFiles = new ArrayList<>(List.of(dataFile));
        when(dataset.getOrCreateEditVersion()).thenReturn(version);
        when(version.getFileMetadatas()).thenReturn(versionFiles);
        when(dataset.getFiles()).thenReturn(datasetFiles);
        editDatafilesPage.setDataset(dataset);
        editDatafilesPage.setMode(EditDatafilesPage.FileEditMode.UPLOAD);
        editDatafilesPage.setFileMetadatas(displayedFiles);
        editDatafilesPage.setSelectedFiles(List.of(fileMetadata));
        Field newFilesField = EditDatafilesPage.class.getDeclaredField("newFiles");
        newFilesField.setAccessible(true);
        newFilesField.set(editDatafilesPage, newFiles);

        try (MockedStatic<FileUtil> fileUtil = mockStatic(FileUtil.class);
                MockedStatic<JsfHelper> jsfHelper = mockStatic(JsfHelper.class)) {
            editDatafilesPage.deleteFiles();
            fileUtil.verify(() -> FileUtil.deleteTempFile(dataFile, dataset, null));
        }

        assertEquals(20L, editDatafilesPage.getMaxTotalUploadSizeInBytes());
        assertTrue(displayedFiles.isEmpty());
        assertTrue(versionFiles.isEmpty());
        assertTrue(datasetFiles.isEmpty());
        assertTrue(newFiles.isEmpty());
    }

}
