package edu.harvard.iq.dataverse.dataset.difference;

import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.UnitTestUtils;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.License;
import edu.harvard.iq.dataverse.persistence.datafile.license.LocaleText;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class LicenseDifferenceFinderTest {

    @Mock
    private DatasetFieldServiceBean datasetFieldService;

    @InjectMocks
    private LicenseDifferenceFinder licenseDifferenceFinder;

    private Dataset dataset;

    @BeforeEach
    public void before() {
        MetadataBlock citationMetadataBlock = new MetadataBlock();
        citationMetadataBlock.setId(1L);
        citationMetadataBlock.setName("citation");
        citationMetadataBlock.setDisplayName("Citation Metadata");


        DatasetFieldType titleFieldType = MocksFactory.makeDatasetFieldType("title",
                FieldType.TEXT,
                false,
                citationMetadataBlock);
        titleFieldType.setDisplayOrder(0);

        DatasetFieldType authorNameFieldType = MocksFactory.makeDatasetFieldType("authorName",
                FieldType.TEXT,
                false,
                citationMetadataBlock);
        DatasetFieldType authorAffiliationFieldType = MocksFactory.makeDatasetFieldType("authorAffiliation",
                FieldType.TEXT,
                false,
                citationMetadataBlock);
        DatasetFieldType authorFieldType = MocksFactory.makeComplexDatasetFieldType("author",
                true,
                citationMetadataBlock,
                authorNameFieldType,
                authorAffiliationFieldType);
        authorFieldType.setDisplayOnCreate(true);
        authorFieldType.setDisplayOrder(1);

        DatasetFieldType datasetContactNameFieldType = MocksFactory.makeDatasetFieldType("datasetContactName",
                FieldType.TEXT,
                false,
                citationMetadataBlock);
        DatasetFieldType datasetContactAffiliationFieldType = MocksFactory.makeDatasetFieldType(
                "datasetContactAffiliation",
                FieldType.TEXT,
                false,
                citationMetadataBlock);
        DatasetFieldType datasetContactEmailFieldType = MocksFactory.makeDatasetFieldType("datasetContactEmail",
                FieldType.TEXT,
                false,
                citationMetadataBlock);
        DatasetFieldType datasetContactFieldType = MocksFactory.makeComplexDatasetFieldType("datasetContact",
                true,
                citationMetadataBlock,
                datasetContactNameFieldType,
                datasetContactAffiliationFieldType,
                datasetContactEmailFieldType);
        datasetContactFieldType.setDisplayOrder(2);


        DatasetFieldType dsDescriptionValueFieldType = MocksFactory.makeDatasetFieldType("dsDescriptionValue",
                FieldType.TEXT,
                false,
                citationMetadataBlock);
        DatasetFieldType dsDescriptionDateFieldType = MocksFactory.makeDatasetFieldType("dsDescriptionDate",
                FieldType.TEXT,
                false,
                citationMetadataBlock);
        DatasetFieldType dsDescriptionFieldType = MocksFactory.makeComplexDatasetFieldType("dsDescription",
                true,
                citationMetadataBlock,
                dsDescriptionValueFieldType,
                dsDescriptionDateFieldType);
        dsDescriptionFieldType.setDisplayOrder(3);

        DatasetFieldType subjectFieldType = MocksFactory.makeControlledVocabDatasetFieldType("subject",
                true,
                citationMetadataBlock,
                "agricultural_sciences",
                "arts_and_humanities",
                "chemistry");
        subjectFieldType.setDisplayOrder(4);

        DatasetFieldType depositorFieldType = MocksFactory.makeDatasetFieldType("depositor",
                FieldType.TEXT,
                false,
                citationMetadataBlock);
        depositorFieldType.setDisplayOrder(5);

        DatasetFieldType dateOfDepositFieldType = MocksFactory.makeDatasetFieldType("dateOfDeposit",
                FieldType.TEXT,
                false,
                citationMetadataBlock);
        dateOfDepositFieldType.setDisplayOrder(6);

        when(datasetFieldService.findByNameOpt(eq("title"))).thenReturn(titleFieldType);
        when(datasetFieldService.findByNameOpt(eq("author"))).thenReturn(authorFieldType);
        when(datasetFieldService.findByNameOpt(eq("authorName"))).thenReturn(authorNameFieldType);
        when(datasetFieldService.findByNameOpt(eq("authorAffiliation"))).thenReturn(authorAffiliationFieldType);
        when(datasetFieldService.findByNameOpt(eq("datasetContact"))).thenReturn(datasetContactFieldType);
        when(datasetFieldService.findByNameOpt(eq("datasetContactName"))).thenReturn(datasetContactNameFieldType);
        when(datasetFieldService.findByNameOpt(eq("datasetContactAffiliation"))).thenReturn(
                datasetContactAffiliationFieldType);
        when(datasetFieldService.findByNameOpt(eq("datasetContactEmail"))).thenReturn(datasetContactEmailFieldType);
        when(datasetFieldService.findByNameOpt(eq("dsDescription"))).thenReturn(dsDescriptionFieldType);
        when(datasetFieldService.findByNameOpt(eq("dsDescriptionValue"))).thenReturn(dsDescriptionValueFieldType);
        when(datasetFieldService.findByNameOpt(eq("dsDescriptionDate"))).thenReturn(dsDescriptionDateFieldType);
        when(datasetFieldService.findByNameOpt(eq("subject"))).thenReturn(subjectFieldType);
        when(datasetFieldService.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(subjectFieldType,
                "Chemistry",
                false))
                .thenReturn(subjectFieldType.getControlledVocabularyValue("chemistry"));
        when(datasetFieldService.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(subjectFieldType,
                "Agricultural Sciences",
                false))
                .thenReturn(subjectFieldType.getControlledVocabularyValue("agricultural_sciences"));
        when(datasetFieldService.findControlledVocabularyValueByDatasetFieldTypeAndStrValue(subjectFieldType,
                "Arts and Humanities",
                false))
                .thenReturn(subjectFieldType.getControlledVocabularyValue("arts_and_humanities"));

        when(datasetFieldService.findByNameOpt(eq("depositor"))).thenReturn(depositorFieldType);
        when(datasetFieldService.findByNameOpt(eq("dateOfDeposit"))).thenReturn(dateOfDepositFieldType);


        dataset = MocksFactory.makeDataset();
    }

    @Test
    public void shouldGetLicenseDifferences_WITH_CHANGED_FILE_TERMS() throws IOException, JsonParseException {

        // given

        DataFile dataFile1 = MocksFactory.makeDataFile();
        dataFile1.getFileMetadatas().clear();
        DataFile dataFile2 = MocksFactory.makeDataFile();
        dataFile2.getFileMetadatas().clear();

        DatasetVersion v1 = parseDatasetVersionFromClasspath();
        v1.setDataset(dataset);

        FileMetadata v1FileMetadata1 = buildFileMetadata(10L, "firstFile.txt", 0, dataFile1);
        FileTermsOfUse v1File1Terms = buildLicenseTermsOfUse(80L, "CCO0", "Public Domain", v1FileMetadata1);
        FileMetadata v1FileMetadata2 = buildFileMetadata(11L, "secondFile.txt", 1, dataFile2);
        FileTermsOfUse v1File2Terms = buildLicenseTermsOfUse(81L, "Apache 2.0", "Apache License", v1FileMetadata2);

        v1.addFileMetadata(v1FileMetadata1);
        v1.addFileMetadata(v1FileMetadata2);


        DatasetVersion v2 = parseDatasetVersionFromClasspath();
        v2.setDataset(dataset);

        FileMetadata v2FileMetadata1 = buildFileMetadata(10L, "firstFile.txt", 0, dataFile1);
        FileTermsOfUse v2File1Terms = buildLicenseTermsOfUse(81L, "Apache 2.0", "Apache License", v2FileMetadata1);
        FileMetadata v2FileMetadata2 = buildFileMetadata(11L, "secondFile.txt", 1, dataFile2);
        FileTermsOfUse v2File2Terms = buildRestrictedTermsOfUse(FileTermsOfUse.RestrictType.NOT_FOR_REDISTRIBUTION, v2FileMetadata2);

        v2.addFileMetadata(v2FileMetadata1);
        v2.addFileMetadata(v2FileMetadata2);


        // when

        List<DatasetFileTermDifferenceItem> termsDiffs = licenseDifferenceFinder.getLicenseDifference(v2.getFileMetadatas(), v1.getFileMetadatas());


        // then

        assertEquals(2, termsDiffs.size());

        assertEquals(String.valueOf(dataFile1.getId()), termsDiffs.get(0).getFileSummary().getFileId());
        assertSame(v1File1Terms, termsDiffs.get(0).getOldTerms());
        assertSame(v2File1Terms, termsDiffs.get(0).getNewTerms());

        assertEquals(String.valueOf(dataFile2.getId()), termsDiffs.get(1).getFileSummary().getFileId());
        assertSame(v1File2Terms, termsDiffs.get(1).getOldTerms());
        assertSame(v2File2Terms, termsDiffs.get(1).getNewTerms());
    }

    private DatasetVersion parseDatasetVersionFromClasspath() throws IOException, JsonParseException {
        try (ByteArrayInputStream is = new ByteArrayInputStream(UnitTestUtils.readFileToByteArray("json/complete-dataset-version.json"))) {
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

    private FileTermsOfUse buildRestrictedTermsOfUse(FileTermsOfUse.RestrictType restrictType, FileMetadata fileMetadata) {
        FileTermsOfUse termsOfUse = new FileTermsOfUse();
        termsOfUse.setRestrictType(restrictType);

        fileMetadata.setTermsOfUse(termsOfUse);
        termsOfUse.setFileMetadata(fileMetadata);

        return termsOfUse;
    }
}
