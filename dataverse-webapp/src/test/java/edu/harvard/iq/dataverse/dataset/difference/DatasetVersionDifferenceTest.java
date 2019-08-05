package edu.harvard.iq.dataverse.dataset.difference;

import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.RestrictType;
import edu.harvard.iq.dataverse.persistence.datafile.license.License;
import edu.harvard.iq.dataverse.persistence.datafile.license.LocaleText;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.util.Locale;

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
        
        List<MetadataBlockChangeCounts> blockDataForNote = diff.getBlockDataForNote();
        assertEquals(1, blockDataForNote.size());
        
        MetadataBlockChangeCounts blockChange = blockDataForNote.get(0);
        assertEquals("citation", blockChange.getItem().getName());
        assertEquals(1, blockChange.getAddedCount());
        assertEquals(1, blockChange.getRemovedCount());
        assertEquals(2, blockChange.getChangedCount());
        
        List<DatasetFieldChangeCounts> summaryDataForNote = diff.getSummaryDataForNote();
        
        assertEquals(1, summaryDataForNote.size());
        DatasetFieldChangeCounts summaryForNote = summaryDataForNote.get(0);
        assertEquals("author", summaryForNote.getItem().getName());
        assertEquals(1, summaryForNote.getAddedCount());
        assertEquals(0, summaryForNote.getRemovedCount());
        assertEquals(1, summaryForNote.getChangedCount());
        
        
        List<List<DatasetFieldDiff>> detailDataByBlock = diff.getDetailDataByBlock();
        assertEquals(1, detailDataByBlock.size());
        
        List<DatasetFieldDiff> detailData = detailDataByBlock.get(0);
        
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
        assertEquals(0, diff.getDatasetFileTermsDiffList().size());
        assertEquals(0, diff.getRemovedFiles().size());
        assertEquals(StringUtils.EMPTY, diff.getFileNote());
        
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
        assertEquals(String.valueOf(dataFileToReplace.getId()), dataFileDiffReplacement.getOldFileSummary().getFileId());
        assertEquals(String.valueOf(dataFileReplacement.getId()), dataFileDiffReplacement.getNewFileSummary().getFileId());
        assertEquals("toreplace.txt", dataFileDiffReplacement.getMetadataDifference().getFileName1());
        assertEquals("replacementFile.txt", dataFileDiffReplacement.getMetadataDifference().getFileName2());

        assertEquals("(Replaced: 1)", diff.getFileNote());
        
        assertFalse(diff.isEmpty());
        assertThat(diff.getBlockDataForNote(), is(empty()));
        assertThat(diff.getSummaryDataForNote(), is(empty()));
        assertThat(diff.getDetailDataByBlock(), is(empty()));
        assertThat(diff.getAddedFiles(), is(empty()));
        assertThat(diff.getChangedFileMetadata(), is(empty()));
        assertThat(diff.getDatasetFilesDiffList(), is(empty()));
        assertThat(diff.getDatasetFileTermsDiffList(), is(empty()));
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
        assertEquals("firstFile.txt", fileDifference1.getDifference().getFileName1());
        assertNull(fileDifference1.getDifference().getFileName2());
        
        DatasetFileDifferenceItem fileDifference2 = diff.getDatasetFilesDiffList().get(1);
        assertEquals("secondFile.txt", fileDifference2.getDifference().getFileName1());
        assertNull(fileDifference2.getDifference().getFileName2());
        
        
        assertEquals(2, diff.getRemovedFiles().size());
        assertSame(v1FileMetadata1, diff.getRemovedFiles().get(0));
        assertSame(v1FileMetadata2, diff.getRemovedFiles().get(1));


        assertEquals("(Removed: 2)", diff.getFileNote());
        
        assertFalse(diff.isEmpty());
        assertThat(diff.getBlockDataForNote(), is(empty()));
        assertThat(diff.getSummaryDataForNote(), is(empty()));
        assertThat(diff.getDetailDataByBlock(), is(empty()));
        assertThat(diff.getAddedFiles(), is(empty()));
        assertThat(diff.getChangedFileMetadata(), is(empty()));
        assertThat(diff.getDatasetFileTermsDiffList(), is(empty()));
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
        assertNull(fileDifference1.getDifference().getFileName1());
        assertEquals("firstFile.txt", fileDifference1.getDifference().getFileName2());
        
        DatasetFileDifferenceItem fileDifference2 = diff.getDatasetFilesDiffList().get(1);
        assertNull(fileDifference2.getDifference().getFileName1());
        assertEquals("secondFile.txt", fileDifference2.getDifference().getFileName2());
        
        
        assertEquals(2, diff.getAddedFiles().size());
        assertSame(v2FileMetadata1, diff.getAddedFiles().get(0));
        assertSame(v2FileMetadata2, diff.getAddedFiles().get(1));


        assertEquals("(Added: 2)", diff.getFileNote());
        
        assertFalse(diff.isEmpty());
        assertThat(diff.getBlockDataForNote(), is(empty()));
        assertThat(diff.getSummaryDataForNote(), is(empty()));
        assertThat(diff.getDetailDataByBlock(), is(empty()));
        assertThat(diff.getRemovedFiles(), is(empty()));
        assertThat(diff.getChangedFileMetadata(), is(empty()));
        assertThat(diff.getDatasetFileTermsDiffList(), is(empty()));
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
        assertEquals("secondFile.txt", fileDifference2.getDifference().getFileName1());
        assertEquals("secondFile (changed).txt", fileDifference2.getDifference().getFileName2());

        assertEquals(1, diff.getChangedFileMetadata().size());
        assertSame(v1FileMetadata2, diff.getChangedFileMetadata().get(0).getOldValue());
        assertSame(v2FileMetadata2, diff.getChangedFileMetadata().get(0).getNewValue());


        assertEquals("(Changed metadata: 1 file)", diff.getFileNote());
        
        assertFalse(diff.isEmpty());
        assertThat(diff.getBlockDataForNote(), is(empty()));
        assertThat(diff.getSummaryDataForNote(), is(empty()));
        assertThat(diff.getDetailDataByBlock(), is(empty()));
        assertThat(diff.getAddedFiles(), is(empty()));
        assertThat(diff.getRemovedFiles(), is(empty()));
        assertThat(diff.getDatasetFileTermsDiffList(), is(empty()));
        assertThat(diff.getDatasetFilesReplacementList(), is(empty()));
        assertSame(v1, diff.getOriginalVersion());
        assertSame(v2, diff.getNewVersion());
    }
    
    @Test
    public void create__WITH_CHANGED_FILE_TERMS() throws IOException, JsonParseException {

        // given
        
        DataFile dataFile1 = MocksFactory.makeDataFile();
        dataFile1.getFileMetadatas().clear();
        DataFile dataFile2 = MocksFactory.makeDataFile();
        dataFile2.getFileMetadatas().clear();
        
        DatasetVersion v1 = parseDatasetVersionFromClasspath("/json/complete-dataset-version.json");
        v1.setDataset(dataset);
        
        FileMetadata v1FileMetadata1 = buildFileMetadata(10L, "firstFile.txt", 0, dataFile1);
        FileTermsOfUse v1File1Terms = buildLicenseTermsOfUse(80L, "CCO0", "Public Domain", v1FileMetadata1);
        FileMetadata v1FileMetadata2 = buildFileMetadata(11L, "secondFile.txt", 1, dataFile2);
        FileTermsOfUse v1File2Terms = buildLicenseTermsOfUse(81L, "Apache 2.0", "Apache License", v1FileMetadata2);
        
        v1.addFileMetadata(v1FileMetadata1);
        v1.addFileMetadata(v1FileMetadata2);
        
        
        DatasetVersion v2 = parseDatasetVersionFromClasspath("/json/complete-dataset-version.json");
        v2.setDataset(dataset);
        
        FileMetadata v2FileMetadata1 = buildFileMetadata(10L, "firstFile.txt", 0, dataFile1);
        FileTermsOfUse v2File1Terms = buildLicenseTermsOfUse(81L, "Apache 2.0", "Apache License", v2FileMetadata1);
        FileMetadata v2FileMetadata2 = buildFileMetadata(11L, "secondFile.txt", 1, dataFile2);
        FileTermsOfUse v2File2Terms = buildRestrictedTermsOfUse(RestrictType.NOT_FOR_REDISTRIBUTION, v2FileMetadata2);
        
        v2.addFileMetadata(v2FileMetadata1);
        v2.addFileMetadata(v2FileMetadata2);
        
        
        // when
        
        DatasetVersionDifference diff = new DatasetVersionDifference(v2, v1);
        
        
        // then
        
        List<DatasetFileTermDifferenceItem> termsDiffs = diff.getDatasetFileTermsDiffList();
        assertEquals(2, termsDiffs.size());
        
        assertEquals(String.valueOf(dataFile1.getId()), termsDiffs.get(0).getFileSummary().getFileId());
        assertSame(v1File1Terms, termsDiffs.get(0).getOldTerms());
        assertSame(v2File1Terms, termsDiffs.get(0).getNewTerms());
        
        assertEquals(String.valueOf(dataFile2.getId()), termsDiffs.get(1).getFileSummary().getFileId());
        assertSame(v1File2Terms, termsDiffs.get(1).getOldTerms());
        assertSame(v2File2Terms, termsDiffs.get(1).getNewTerms());
        

        assertEquals("(Changed licenses/terms of use: 2 files)", diff.getFileNote());
        
        assertFalse(diff.isEmpty());
        assertThat(diff.getBlockDataForNote(), is(empty()));
        assertThat(diff.getSummaryDataForNote(), is(empty()));
        assertThat(diff.getDetailDataByBlock(), is(empty()));
        assertThat(diff.getAddedFiles(), is(empty()));
        assertThat(diff.getRemovedFiles(), is(empty()));
        assertThat(diff.getChangedFileMetadata(), is(empty()));
        assertThat(diff.getDatasetFilesDiffList(), is(empty()));
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
    
    private void assertDatasetPrimitiveFieldChange(DatasetFieldDiff actualFieldChange,
            String expectedFieldName, String expectedOldValue, String expectedNewValue) {
        
        assertEquals(expectedFieldName, actualFieldChange.getOldValue().getDatasetFieldType().getName());
        assertEquals(expectedFieldName, actualFieldChange.getNewValue().getDatasetFieldType().getName());
        
        assertEquals(expectedOldValue, actualFieldChange.getOldValue().getRawValue());
        assertEquals(expectedNewValue, actualFieldChange.getNewValue().getRawValue());
        
    }
    
    private void assertDatasetCompoundFieldChange(DatasetFieldDiff actualFieldChange,
            String expectedFieldName, String expectedOldValue, String expectedNewValue) {
        
        assertEquals(expectedFieldName, actualFieldChange.getOldValue().getDatasetFieldType().getName());
        assertEquals(expectedFieldName, actualFieldChange.getNewValue().getDatasetFieldType().getName());
        
        assertEquals(expectedOldValue, actualFieldChange.getOldValue().getCompoundRawValue());
        assertEquals(expectedNewValue, actualFieldChange.getNewValue().getCompoundRawValue());
        
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
    
    private FileTermsOfUse buildLicenseTermsOfUse(Long licenseId, String licenseName, String licenseEnName, FileMetadata fileMetadata) {
        FileTermsOfUse termsOfUse = new FileTermsOfUse();
        License license = new License();
        license.setId(licenseId);
        license.setName(licenseName);
        license.addLocalizedName(new LocaleText(Locale.ENGLISH, licenseEnName));
        termsOfUse.setLicense(license);
        
        fileMetadata.setTermsOfUse(termsOfUse);
        termsOfUse.setFileMetadata(fileMetadata);
        
        return termsOfUse;
    }
    
    private FileTermsOfUse buildRestrictedTermsOfUse(RestrictType restrictType, FileMetadata fileMetadata) {
        FileTermsOfUse termsOfUse = new FileTermsOfUse();
        termsOfUse.setRestrictType(restrictType);
        
        fileMetadata.setTermsOfUse(termsOfUse);
        termsOfUse.setFileMetadata(fileMetadata);
        
        return termsOfUse;
    }
}
