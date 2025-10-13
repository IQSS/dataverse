package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.FileVersionDifference;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileVersionDifferenceJsonPrinterTest {
    @Mock
    private FileVersionDifference fileVersionDifference;
    @Mock
    private FileMetadata newFileMetadata;
    @Mock
    private FileMetadata originalFileMetadata;
    @Mock
    private DatasetVersion datasetVersion;
    @Mock
    private DataFile dataFile;

    @BeforeEach
    void setUp() {
        when(fileVersionDifference.getNewFileMetadata()).thenReturn(newFileMetadata);
        when(newFileMetadata.getDatasetVersion()).thenReturn(datasetVersion);
        when(datasetVersion.getVersionNote()).thenReturn("Test Version Note");
    }

    @Test
    @DisplayName("should correctly serialize basic dataset and file details")
    void jsonFileVersionDifference_with_basic_dataset_and_file_details() {
        // Arrange
        when(newFileMetadata.getDataFile()).thenReturn(dataFile);
        when(dataFile.getId()).thenReturn(42L);
        when(datasetVersion.getFriendlyVersionNumber()).thenReturn("V1");
        when(datasetVersion.getVersionState()).thenReturn(DatasetVersion.VersionState.RELEASED);
        when(fileVersionDifference.getDifferenceSummaryGroups()).thenReturn(Collections.emptyList());

        // Act
        JsonObject result = FileVersionDifferenceJsonPrinter.jsonFileVersionDifference(fileVersionDifference).build();

        // Assert
        assertEquals("V1", result.getString("datasetVersion"));
        assertEquals("RELEASED", result.getString("versionState"));
        assertEquals(42, result.getInt("datafileId"));
        assertNotNull(result.getJsonObject("fileDifferenceSummary"));
        assertEquals("Test Version Note", result.getJsonObject("fileDifferenceSummary").getString("versionNote"));
    }

    @Test
    @DisplayName("should report file action as 'Added' for a new file")
    void jsonFileVersionDifference_file_added() {
        // Arrange
        when(fileVersionDifference.getOriginalFileMetadata()).thenReturn(null);
        when(newFileMetadata.getDataFile()).thenReturn(dataFile);

        // Act
        JsonObject result = FileVersionDifferenceJsonPrinter.jsonFileVersionDifference(fileVersionDifference).build();

        // Assert
        assertEquals("Added", result.getJsonObject("fileDifferenceSummary").getString("file"));
    }

    @Test
    @DisplayName("should report file action as 'Deleted' for a removed file")
    void jsonFileVersionDifference_file_deleted() {
        // Arrange
        when(fileVersionDifference.getOriginalFileMetadata()).thenReturn(originalFileMetadata);
        when(newFileMetadata.getDataFile()).thenReturn(null); // Key condition for deleted

        // Act
        JsonObject result = FileVersionDifferenceJsonPrinter.jsonFileVersionDifference(fileVersionDifference).build();

        // Assert
        assertEquals("Deleted", result.getJsonObject("fileDifferenceSummary").getString("file"));
    }

    @Test
    @DisplayName("should report file action as 'Replaced' for a replaced file")
    void jsonFileVersionDifference_file_replaced() {
        // Arrange
        DataFile originalDataFile = new DataFile();
        originalDataFile.setId(100L);
        DataFile newDataFile = new DataFile();
        newDataFile.setId(101L); // Different file object

        when(fileVersionDifference.getOriginalFileMetadata()).thenReturn(originalFileMetadata);
        when(originalFileMetadata.getDataFile()).thenReturn(originalDataFile);
        when(newFileMetadata.getDataFile()).thenReturn(newDataFile);

        // Act
        JsonObject result = FileVersionDifferenceJsonPrinter.jsonFileVersionDifference(fileVersionDifference).build();

        // Assert
        assertEquals("Replaced", result.getJsonObject("fileDifferenceSummary").getString("file"));
    }

    @Test
    @DisplayName("should correctly serialize count-based summary groups")
    void jsonFileVersionDifference_as_count_based_summary() {
        // Arrange
        FileVersionDifference fvd = new FileVersionDifference(newFileMetadata, originalFileMetadata, true);
        FileVersionDifference.FileDifferenceSummaryItem item = fvd.new FileDifferenceSummaryItem("", 2, 1, 0, 0, true);
        FileVersionDifference.FileDifferenceSummaryGroup group = fvd.new FileDifferenceSummaryGroup("Tags");
        group.getFileDifferenceSummaryItems().add(item);

        when(fileVersionDifference.getDifferenceSummaryGroups()).thenReturn(Collections.singletonList(group));

        // Act
        JsonObject result = FileVersionDifferenceJsonPrinter.jsonFileVersionDifference(fileVersionDifference).build();

        // Assert
        JsonObject summary = result.getJsonObject("fileDifferenceSummary");
        assertNotNull(summary);
        JsonObject tags = summary.getJsonObject("Tags");
        assertNotNull(tags);
        assertEquals(2, tags.getInt("Added"));
        assertEquals(1, tags.getInt("Changed"));
        assertFalse(tags.containsKey("Deleted"), "Zero-count fields should not be present");
    }

    @Test
    @DisplayName("should correctly serialize the 'File Access' group as a name-value pair")
    void jsonFileVersionDifference_as_name_value_for_file_access() {
        // Arrange
        FileVersionDifference fvd = new FileVersionDifference(newFileMetadata, originalFileMetadata, true);
        FileVersionDifference.FileDifferenceSummaryItem item = fvd.new FileDifferenceSummaryItem("Public", 0, 1, 0, 0, false);
        FileVersionDifference.FileDifferenceSummaryGroup group = fvd.new FileDifferenceSummaryGroup("File Access");
        group.getFileDifferenceSummaryItems().add(item);
        when(fileVersionDifference.getDifferenceSummaryGroups()).thenReturn(Collections.singletonList(group));

        // Act
        JsonObject result = FileVersionDifferenceJsonPrinter.jsonFileVersionDifference(fileVersionDifference).build();

        // Assert
        JsonObject summary = result.getJsonObject("fileDifferenceSummary");
        assertEquals("Public", summary.getString("FileAccess"));
    }


    @Test
    @DisplayName("should correctly serialize list-item-based summary groups")
    void jsonFileVersionDifference_as_list_item_based_summary() {
        // Arrange
        FileVersionDifference fvd = new FileVersionDifference(newFileMetadata, originalFileMetadata, true);

        FileVersionDifference.FileDifferenceSummaryItem itemA = fvd.new FileDifferenceSummaryItem("Category A", 1, 0, 0, 0, false);
        FileVersionDifference.FileDifferenceSummaryItem itemB = fvd.new FileDifferenceSummaryItem("Category B", 0, 0, 1, 0, false);

        FileVersionDifference.FileDifferenceSummaryGroup group = fvd.new FileDifferenceSummaryGroup("Categories");
        group.getFileDifferenceSummaryItems().addAll(Arrays.asList(itemA, itemB));

        when(fileVersionDifference.getDifferenceSummaryGroups()).thenReturn(Collections.singletonList(group));

        // Act
        JsonObject result = FileVersionDifferenceJsonPrinter.jsonFileVersionDifference(fileVersionDifference).build();

        // Assert
        JsonObject summary = result.getJsonObject("fileDifferenceSummary");
        assertNotNull(summary);
        jakarta.json.JsonArray categories = summary.getJsonArray("Categories");
        assertNotNull(categories);
        assertEquals(2, categories.size());
        assertEquals("Category A", categories.getJsonObject(0).getString("name"));
        assertEquals("Added", categories.getJsonObject(0).getString("action"));
        assertEquals("Category B", categories.getJsonObject(1).getString("name"));
        assertEquals("Deleted", categories.getJsonObject(1).getString("action"));
    }

    @Test
    @DisplayName("should correctly serialize a mix of all summary group types")
    void jsonFileVersionDifference_with_multiple_and_mixed_types() {
        // Arrange
        FileVersionDifference fvd = new FileVersionDifference(newFileMetadata, originalFileMetadata, true);

        List<FileVersionDifference.FileDifferenceSummaryGroup> groups = new ArrayList<>();

        // Group 1: Tags (counts)
        FileVersionDifference.FileDifferenceSummaryItem tagsItem = fvd.new FileDifferenceSummaryItem("", 2, 0, 1, 0, true);
        FileVersionDifference.FileDifferenceSummaryGroup tagsGroup = fvd.new FileDifferenceSummaryGroup("Tags");
        tagsGroup.getFileDifferenceSummaryItems().add(tagsItem);
        groups.add(tagsGroup);

        // Group 2: Categories (list items)
        FileVersionDifference.FileDifferenceSummaryItem categoriesItem = fvd.new FileDifferenceSummaryItem("Science", 1, 0, 0, 0, false);
        FileVersionDifference.FileDifferenceSummaryGroup categoriesGroup = fvd.new FileDifferenceSummaryGroup("Categories");
        categoriesGroup.getFileDifferenceSummaryItems().add(categoriesItem);
        groups.add(categoriesGroup);

        // Group 3: File Access (name/value)
        FileVersionDifference.FileDifferenceSummaryItem accessItem = fvd.new FileDifferenceSummaryItem("Restricted", 0, 1, 0, 0, false);
        FileVersionDifference.FileDifferenceSummaryGroup accessGroup = fvd.new FileDifferenceSummaryGroup("File Access");
        accessGroup.getFileDifferenceSummaryItems().add(accessItem);
        groups.add(accessGroup);

        when(fileVersionDifference.getDifferenceSummaryGroups()).thenReturn(groups);

        // Act
        JsonObject result = FileVersionDifferenceJsonPrinter.jsonFileVersionDifference(fileVersionDifference).build();

        // Assert
        JsonObject summary = result.getJsonObject("fileDifferenceSummary");

        // Check Categories (should be first alphabetically)
        assertEquals(1, summary.getJsonArray("Categories").size());
        assertEquals("Science", summary.getJsonArray("Categories").getJsonObject(0).getString("name"));

        // Check File Access
        assertEquals("Restricted", summary.getString("FileAccess"));

        // Check Tags
        assertEquals(2, summary.getJsonObject("Tags").getInt("Added"));
        assertEquals(1, summary.getJsonObject("Tags").getInt("Deleted"));
    }
}
