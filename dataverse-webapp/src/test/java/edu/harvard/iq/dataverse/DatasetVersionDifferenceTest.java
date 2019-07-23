package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.DatasetVersionDifference.DatasetFileDifferenceItem;
import edu.harvard.iq.dataverse.DatasetVersionDifference.DatasetReplaceFileItem;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import io.vavr.Tuple2;
import io.vavr.Tuple4;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DatasetVersionDifferenceTest {

    @Mock
    private DatasetFieldServiceBean datasetFieldService;
    
    private Dataset dataset;
    
    
    @BeforeEach
    public void before() {
        MetadataBlock citationMetadataBlock = new MetadataBlock();
        citationMetadataBlock.setId(1L);
        citationMetadataBlock.setName("citation");
        citationMetadataBlock.setDisplayName("Citation Metadata");
        
        
        DatasetFieldType titleFieldType = MocksFactory.makeDatasetFieldType("title", FieldType.TEXT, false, citationMetadataBlock);
        titleFieldType.setDisplayOrder(0);
        
        DatasetFieldType authorNameFieldType = MocksFactory.makeDatasetFieldType("authorName", FieldType.TEXT, false, citationMetadataBlock);
        DatasetFieldType authorAffiliationFieldType = MocksFactory.makeDatasetFieldType("authorAffiliation", FieldType.TEXT, false, citationMetadataBlock);
        DatasetFieldType authorFieldType = MocksFactory.makeComplexDatasetFieldType("author", true, citationMetadataBlock,
                authorNameFieldType, authorAffiliationFieldType);
        authorFieldType.setDisplayOnCreate(true);
        authorFieldType.setDisplayOrder(1);
        
        DatasetFieldType datasetContactNameFieldType = MocksFactory.makeDatasetFieldType("datasetContactName", FieldType.TEXT, false, citationMetadataBlock);
        DatasetFieldType datasetContactAffiliationFieldType = MocksFactory.makeDatasetFieldType("datasetContactAffiliation", FieldType.TEXT, false, citationMetadataBlock);
        DatasetFieldType datasetContactEmailFieldType = MocksFactory.makeDatasetFieldType("datasetContactEmail", FieldType.TEXT, false, citationMetadataBlock);
        DatasetFieldType datasetContactFieldType = MocksFactory.makeComplexDatasetFieldType("datasetContact", true, citationMetadataBlock,
                datasetContactNameFieldType, datasetContactAffiliationFieldType, datasetContactEmailFieldType);
        datasetContactFieldType.setDisplayOrder(2);
        
        
        DatasetFieldType dsDescriptionValueFieldType = MocksFactory.makeDatasetFieldType("dsDescriptionValue", FieldType.TEXT, false, citationMetadataBlock);
        DatasetFieldType dsDescriptionDateFieldType = MocksFactory.makeDatasetFieldType("dsDescriptionDate", FieldType.TEXT, false, citationMetadataBlock);
        DatasetFieldType dsDescriptionFieldType = MocksFactory.makeComplexDatasetFieldType("dsDescription", true, citationMetadataBlock,
                dsDescriptionValueFieldType, dsDescriptionDateFieldType);
        dsDescriptionFieldType.setDisplayOrder(3);
        
        DatasetFieldType subjectFieldType = MocksFactory.makeControlledVocabDatasetFieldType("subject", true, citationMetadataBlock,
                "agricultural_sciences", "arts_and_humanities", "chemistry");
        subjectFieldType.setDisplayOrder(4);
        
        DatasetFieldType depositorFieldType = MocksFactory.makeDatasetFieldType("depositor", FieldType.TEXT, false, citationMetadataBlock);
        depositorFieldType.setDisplayOrder(5);
        
        DatasetFieldType dateOfDepositFieldType = MocksFactory.makeDatasetFieldType("dateOfDeposit", FieldType.TEXT, false, citationMetadataBlock);
        dateOfDepositFieldType.setDisplayOrder(6);
        
        when(datasetFieldService.findByNameOpt(eq("title"))).thenReturn(titleFieldType);
        when(datasetFieldService.findByNameOpt(eq("author"))).thenReturn(authorFieldType);
        when(datasetFieldService.findByNameOpt(eq("authorName"))).thenReturn(authorNameFieldType);
        when(datasetFieldService.findByNameOpt(eq("authorAffiliation"))).thenReturn(authorAffiliationFieldType);
        when(datasetFieldService.findByNameOpt(eq("datasetContact"))).thenReturn(datasetContactFieldType);
        when(datasetFieldService.findByNameOpt(eq("datasetContactName"))).thenReturn(datasetContactNameFieldType);
        when(datasetFieldService.findByNameOpt(eq("datasetContactAffiliation"))).thenReturn(datasetContactAffiliationFieldType);
        when(datasetFieldService.findByNameOpt(eq("datasetContactEmail"))).thenReturn(datasetContactEmailFieldType);
        when(datasetFieldService.findByNameOpt(eq("dsDescription"))).thenReturn(dsDescriptionFieldType);
        when(datasetFieldService.findByNameOpt(eq("dsDescriptionValue"))).thenReturn(dsDescriptionValueFieldType);
        when(datasetFieldService.findByNameOpt(eq("dsDescriptionDate"))).thenReturn(dsDescriptionDateFieldType);
        when(datasetFieldService.findByNameOpt(eq("subject"))).thenReturn(subjectFieldType);
        when(datasetFieldService.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(subjectFieldType, "Chemistry", false))
            .thenReturn(subjectFieldType.getControlledVocabularyValue("chemistry"));
        when(datasetFieldService.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(subjectFieldType, "Agricultural Sciences", false))
            .thenReturn(subjectFieldType.getControlledVocabularyValue("agricultural_sciences"));
        when(datasetFieldService.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(subjectFieldType, "Arts and Humanities", false))
        .thenReturn(subjectFieldType.getControlledVocabularyValue("arts_and_humanities"));
        
        when(datasetFieldService.findByNameOpt(eq("depositor"))).thenReturn(depositorFieldType);
        when(datasetFieldService.findByNameOpt(eq("dateOfDeposit"))).thenReturn(dateOfDepositFieldType);
        

        dataset = MocksFactory.makeDataset();
    }
    
    
    // -------------------- TESTS --------------------
    
    @Test
    public void create_METADATA_ONLY() throws IOException, JsonParseException {

        // given
        
        DatasetVersion v1 = parseDatasetVersionFromClasspath("/json/complete-dataset-version.json");
        v1.setDataset(dataset);
        
        DatasetVersion v2 = parseDatasetVersionFromClasspath("/json/complete-dataset-version-with-changes.json");
        v2.setDataset(dataset);
        
        
        // when
        
        DatasetVersionDifference diff = new DatasetVersionDifference(v2, v1);
        
        
        // then
        
        List<Tuple4<MetadataBlock, Integer, Integer, Integer>> blockDataForNote = diff.getBlockDataForNote();
        assertEquals(1, blockDataForNote.size());
        
        Tuple4<MetadataBlock, Integer, Integer, Integer> blockChange = blockDataForNote.get(0);
        assertEquals("citation", blockChange._1().getName());
        assertEquals(Integer.valueOf(1), blockChange._2()); // added
        assertEquals(Integer.valueOf(1), blockChange._3()); // removed
        assertEquals(Integer.valueOf(2), blockChange._4()); // changed
        
        List<Tuple4<DatasetFieldType, Integer, Integer, Integer>> summaryDataForNote = diff.getSummaryDataForNote();
        
        assertEquals(1, summaryDataForNote.size());
        Tuple4<DatasetFieldType, Integer, Integer, Integer> summaryForNote = summaryDataForNote.get(0);
        assertEquals("author", summaryForNote._1().getName());
        assertEquals(Integer.valueOf(1), summaryForNote._2());
        assertEquals(Integer.valueOf(0), summaryForNote._3());
        assertEquals(Integer.valueOf(1), summaryForNote._4());
        
        
        List<List<Tuple2<DatasetField, DatasetField>>> detailDataByBlock = diff.getDetailDataByBlock();
        assertEquals(1, detailDataByBlock.size());
        
        List<Tuple2<DatasetField, DatasetField>> detailData = detailDataByBlock.get(0);
        
        assertEquals(4, detailData.size());
        
        assertDatasetPrimitiveFieldChange(detailData.get(0), "title",
                "Sample-published-dataset (updated)",
                "Sample-published-dataset (updated2)");
        
        assertDatasetCompoundFieldChange(detailData.get(1), "author",
                "Kew, Susie; Creedence Clearwater Revival",
                "Kew, Susie (changed); Creedence Clearwater Revival; Doe, Joe");
        
        assertDatasetCompoundFieldChange(detailData.get(2), "datasetContact",
                "Dataverse, Admin; Dataverse; admin@malinator.com", "");
        
        assertDatasetPrimitiveFieldChange(detailData.get(3), "subject",
                "chemistry", "agricultural_sciences; arts_and_humanities");
        

        assertFalse(diff.isEmpty());
        assertEquals(0, diff.getAddedFiles().size());
        assertEquals(0, diff.getChangedFileMetadata().size());
        assertEquals(0, diff.getDatasetFilesDiffList().size());
        assertEquals(0, diff.getDatasetFilesReplacementList().size());
        assertEquals(0, diff.getRemovedFiles().size());
        
        assertSame(v1, diff.getOriginalVersion());
        assertSame(v2, diff.getNewVersion());
    }
    
    @Test
    public void create_WITH_REPLACED_FILE() throws IOException, JsonParseException {

        // given
        
        DataFile dataFileToReplace = MocksFactory.makeDataFile();
        dataFileToReplace.getFileMetadatas().clear();
        DataFile dataFileReplacement = MocksFactory.makeDataFile();
        dataFileReplacement.setPreviousDataFileId(dataFileToReplace.getId());
        dataFileReplacement.getFileMetadatas().clear();
        
        DatasetVersion v1 = parseDatasetVersionFromClasspath("/json/complete-dataset-version.json");
        v1.setDataset(dataset);

        FileMetadata v1FileMetadata = buildFileMetadata(12L, "toreplace.txt", 2, dataFileToReplace);
        
        v1.addFileMetadata(v1FileMetadata);
        
        
        DatasetVersion v2 = parseDatasetVersionFromClasspath("/json/complete-dataset-version.json");
        v2.setDataset(dataset);
        
        FileMetadata v2FileMetadata = buildFileMetadata(23L, "replacementFile.txt", 3, dataFileReplacement);
        
        v2.addFileMetadata(v2FileMetadata);
        
        
        // when
        
        DatasetVersionDifference diff = new DatasetVersionDifference(v2, v1);
        
        
        // then
        assertEquals(1, diff.getDatasetFilesReplacementList().size());
        
        DatasetReplaceFileItem dataFileDiffReplacement = diff.getDatasetFilesReplacementList().get(0);
        assertEquals(String.valueOf(dataFileToReplace.getId()), dataFileDiffReplacement.getFile1Id());
        assertEquals(String.valueOf(dataFileReplacement.getId()), dataFileDiffReplacement.getFile2Id());
        assertEquals("toreplace.txt", dataFileDiffReplacement.getFdi().getFileName1());
        assertEquals("replacementFile.txt", dataFileDiffReplacement.getFdi().getFileName2());
        

        assertFalse(diff.isEmpty());
        assertThat(diff.getBlockDataForNote(), is(empty()));
        assertThat(diff.getSummaryDataForNote(), is(empty()));
        assertThat(diff.getDetailDataByBlock(), is(empty()));
        assertThat(diff.getAddedFiles(), is(empty()));
        assertThat(diff.getChangedFileMetadata(), is(empty()));
        assertThat(diff.getDatasetFilesDiffList(), is(empty()));
        assertThat(diff.getRemovedFiles(), is(empty()));
        assertSame(v1, diff.getOriginalVersion());
        assertSame(v2, diff.getNewVersion());
    }
    
    @Test
    public void create_WITH_REMOVED_FILE() throws IOException, JsonParseException {

        // given
        
        DataFile dataFile1 = MocksFactory.makeDataFile();
        dataFile1.getFileMetadatas().clear();
        DataFile dataFile2 = MocksFactory.makeDataFile();
        dataFile2.getFileMetadatas().clear();
        
        DatasetVersion v1 = parseDatasetVersionFromClasspath("/json/complete-dataset-version.json");
        v1.setDataset(dataset);
        
        FileMetadata v1FileMetadata1 = buildFileMetadata(10L, "firstFile.txt", 0, dataFile1);
        FileMetadata v1FileMetadata2 = buildFileMetadata(11L, "secondFile.txt", 1, dataFile2);
        
        v1.addFileMetadata(v1FileMetadata1);
        v1.addFileMetadata(v1FileMetadata2);
        
        
        DatasetVersion v2 = parseDatasetVersionFromClasspath("/json/complete-dataset-version.json");
        v2.setDataset(dataset);
        
        
        // when
        
        DatasetVersionDifference diff = new DatasetVersionDifference(v2, v1);
        
        
        // then
        assertEquals(2, diff.getDatasetFilesDiffList().size());
        
        DatasetFileDifferenceItem fileDifference1 = diff.getDatasetFilesDiffList().get(0);
        assertEquals("firstFile.txt", fileDifference1.getFileName1());
        assertNull(fileDifference1.getFileName2());
        
        DatasetFileDifferenceItem fileDifference2 = diff.getDatasetFilesDiffList().get(1);
        assertEquals("secondFile.txt", fileDifference2.getFileName1());
        assertNull(fileDifference2.getFileName2());
        
        
        assertEquals(2, diff.getRemovedFiles().size());
        assertSame(v1FileMetadata1, diff.getRemovedFiles().get(0));
        assertSame(v1FileMetadata2, diff.getRemovedFiles().get(1));
        
        assertFalse(diff.isEmpty());
        assertThat(diff.getBlockDataForNote(), is(empty()));
        assertThat(diff.getSummaryDataForNote(), is(empty()));
        assertThat(diff.getDetailDataByBlock(), is(empty()));
        assertThat(diff.getAddedFiles(), is(empty()));
        assertThat(diff.getChangedFileMetadata(), is(empty()));
        assertThat(diff.getDatasetFilesReplacementList(), is(empty()));
        assertSame(v1, diff.getOriginalVersion());
        assertSame(v2, diff.getNewVersion());
    }
    
    @Test
    public void create_WITH_ADDED_FILE() throws IOException, JsonParseException {

        // given
        
        DataFile dataFile1 = MocksFactory.makeDataFile();
        dataFile1.getFileMetadatas().clear();
        DataFile dataFile2 = MocksFactory.makeDataFile();
        dataFile2.getFileMetadatas().clear();
        
        DatasetVersion v1 = parseDatasetVersionFromClasspath("/json/complete-dataset-version.json");
        v1.setDataset(dataset);
        
        
        DatasetVersion v2 = parseDatasetVersionFromClasspath("/json/complete-dataset-version.json");
        v2.setDataset(dataset);
        
        FileMetadata v2FileMetadata1 = buildFileMetadata(10L, "firstFile.txt", 0, dataFile1);
        FileMetadata v2FileMetadata2 = buildFileMetadata(11L, "secondFile.txt", 1, dataFile2);
        
        v2.addFileMetadata(v2FileMetadata1);
        v2.addFileMetadata(v2FileMetadata2);
        
        
        // when
        
        DatasetVersionDifference diff = new DatasetVersionDifference(v2, v1);
        
        
        // then
        assertEquals(2, diff.getDatasetFilesDiffList().size());
        
        DatasetFileDifferenceItem fileDifference1 = diff.getDatasetFilesDiffList().get(0);
        assertNull(fileDifference1.getFileName1());
        assertEquals("firstFile.txt", fileDifference1.getFileName2());
        
        DatasetFileDifferenceItem fileDifference2 = diff.getDatasetFilesDiffList().get(1);
        assertNull(fileDifference2.getFileName1());
        assertEquals("secondFile.txt", fileDifference2.getFileName2());
        
        
        assertEquals(2, diff.getAddedFiles().size());
        assertSame(v2FileMetadata1, diff.getAddedFiles().get(0));
        assertSame(v2FileMetadata2, diff.getAddedFiles().get(1));
        
        assertFalse(diff.isEmpty());
        assertThat(diff.getBlockDataForNote(), is(empty()));
        assertThat(diff.getSummaryDataForNote(), is(empty()));
        assertThat(diff.getDetailDataByBlock(), is(empty()));
        assertThat(diff.getRemovedFiles(), is(empty()));
        assertThat(diff.getChangedFileMetadata(), is(empty()));
        assertThat(diff.getDatasetFilesReplacementList(), is(empty()));
        assertSame(v1, diff.getOriginalVersion());
        assertSame(v2, diff.getNewVersion());
    }
    
    @Test
    public void create__WITH_CHANGED_FILE_METADATA() throws IOException, JsonParseException {

        // given
        
        DataFile dataFile1 = MocksFactory.makeDataFile();
        dataFile1.getFileMetadatas().clear();
        DataFile dataFile2 = MocksFactory.makeDataFile();
        dataFile2.getFileMetadatas().clear();
        
        DatasetVersion v1 = parseDatasetVersionFromClasspath("/json/complete-dataset-version.json");
        v1.setDataset(dataset);
        
        FileMetadata v1FileMetadata1 = buildFileMetadata(10L, "firstFile.txt", 0, dataFile1);
        FileMetadata v1FileMetadata2 = buildFileMetadata(11L, "secondFile.txt", 1, dataFile2);
        
        v1.addFileMetadata(v1FileMetadata1);
        v1.addFileMetadata(v1FileMetadata2);
        
        
        DatasetVersion v2 = parseDatasetVersionFromClasspath("/json/complete-dataset-version.json");
        v2.setDataset(dataset);
        
        FileMetadata v2FileMetadata1 = buildFileMetadata(10L, "firstFile.txt", 0, dataFile1);
        FileMetadata v2FileMetadata2 = buildFileMetadata(11L, "secondFile (changed).txt", 1, dataFile2);
        
        v2.addFileMetadata(v2FileMetadata1);
        v2.addFileMetadata(v2FileMetadata2);
        
        
        // when
        
        DatasetVersionDifference diff = new DatasetVersionDifference(v2, v1);
        
        
        // then
        assertEquals(1, diff.getDatasetFilesDiffList().size());
        
        DatasetFileDifferenceItem fileDifference2 = diff.getDatasetFilesDiffList().get(0);
        assertEquals("secondFile.txt", fileDifference2.getFileName1());
        assertEquals("secondFile (changed).txt", fileDifference2.getFileName2());

        assertEquals(1, diff.getChangedFileMetadata().size());
        assertSame(v1FileMetadata2, diff.getChangedFileMetadata().get(0)._1());
        assertSame(v2FileMetadata2, diff.getChangedFileMetadata().get(0)._2());
        
        assertFalse(diff.isEmpty());
        assertThat(diff.getBlockDataForNote(), is(empty()));
        assertThat(diff.getSummaryDataForNote(), is(empty()));
        assertThat(diff.getDetailDataByBlock(), is(empty()));
        assertThat(diff.getAddedFiles(), is(empty()));
        assertThat(diff.getRemovedFiles(), is(empty()));
        assertThat(diff.getDatasetFilesReplacementList(), is(empty()));
        assertSame(v1, diff.getOriginalVersion());
        assertSame(v2, diff.getNewVersion());
    }

    @Test
    public void create_WITH_NO_DIFFERENCES() throws IOException, JsonParseException {
        // given

        DatasetVersion v1 = parseDatasetVersionFromClasspath("/json/complete-dataset-version.json");
        v1.setDataset(dataset);

        DatasetVersion v2 = parseDatasetVersionFromClasspath("/json/complete-dataset-version.json");
        v2.setDataset(dataset);

        // when

        DatasetVersionDifference diff = new DatasetVersionDifference(v2, v1);

        // then

        assertTrue(diff.isEmpty());
        assertFalse(diff.isNotEmpty());
    }
    
    
    // -------------------- PRIVATE --------------------
    
    private void assertDatasetPrimitiveFieldChange(Tuple2<DatasetField, DatasetField> actualFieldChange,
            String expectedFieldName, String expectedOldValue, String expectedNewValue) {
        
        assertEquals(expectedFieldName, actualFieldChange._1().getDatasetFieldType().getName());
        assertEquals(expectedFieldName, actualFieldChange._2().getDatasetFieldType().getName());
        
        assertEquals(expectedOldValue, actualFieldChange._1().getRawValue());
        assertEquals(expectedNewValue, actualFieldChange._2().getRawValue());
        
    }
    
    private void assertDatasetCompoundFieldChange(Tuple2<DatasetField, DatasetField> actualFieldChange,
            String expectedFieldName, String expectedOldValue, String expectedNewValue) {
        
        assertEquals(expectedFieldName, actualFieldChange._1().getDatasetFieldType().getName());
        assertEquals(expectedFieldName, actualFieldChange._2().getDatasetFieldType().getName());
        
        assertEquals(expectedOldValue, actualFieldChange._1().getCompoundRawValue());
        assertEquals(expectedNewValue, actualFieldChange._2().getCompoundRawValue());
        
    }
    
    private DatasetVersion parseDatasetVersionFromClasspath(String classpath) throws IOException, JsonParseException {
        
        try (ByteArrayInputStream is = new ByteArrayInputStream(IOUtils.resourceToByteArray(classpath))) {
            JsonObject jsonObject = Json.createReader(is).readObject();
            JsonParser jsonParser = new JsonParser(datasetFieldService, null, null);
            
            return jsonParser.parseDatasetVersion(jsonObject);
        }
    }
    
    private FileMetadata buildFileMetadata(long id, String label, int displayOrder, DataFile dataFile) {
        FileMetadata fileMetadata = MocksFactory.makeFileMetadata(id, label, displayOrder);
        fileMetadata.setDataFile(dataFile);
        dataFile.getFileMetadatas().add(fileMetadata);
        
        return fileMetadata;
    }
}
