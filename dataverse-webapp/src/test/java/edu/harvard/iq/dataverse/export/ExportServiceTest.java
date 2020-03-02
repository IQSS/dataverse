package edu.harvard.iq.dataverse.export;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.error.DataverseError;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import io.vavr.control.Either;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ExportServiceTest {

    //10/07/2019
    private final long DATE = 1562766661000L;

    private ExportService exportService;

    @Mock
    private SettingsServiceBean settingsService;

    @Mock
    private DatasetFieldServiceBean datasetFieldService;

    @Mock
    private SystemConfig systemConfig;

    @BeforeEach
    void prepareData() {
        when(settingsService.isTrueForKey(SettingsServiceBean.Key.ExcludeEmailFromExport)).thenReturn(false);
        when(systemConfig.getDataverseSiteUrl()).thenReturn("https://localhost");
        mockDatasetFields();

        exportService = new ExportService(settingsService, systemConfig);
        exportService.setCurrentDate(LocalDate.of(2019, 7, 11));
        exportService.loadAllExporters();
    }

    @Test
    @DisplayName("export DatasetVersion as string for datacite")
    public void exportDatasetVersionAsString_forDataCite() throws IOException, JsonParseException, URISyntaxException {
        //given
        DatasetVersion datasetVersion = parseDatasetVersionFromClasspath("json/testDataset.json");
        prepareDataForExport(datasetVersion);

        //when
        Either<DataverseError, String> exportedDataset =
                exportService.exportDatasetVersionAsString(datasetVersion, ExporterType.DATACITE);

        //then
        Assert.assertEquals(readFileToString("exportdata/testDatacite.xml"), exportedDataset.get());
    }

    @Test
    @DisplayName("export DatasetVersion as string for DCTerms")
    public void exportDatasetVersionAsString_forDCTerms() throws IOException, JsonParseException, URISyntaxException {
        //given
        DatasetVersion datasetVersion = parseDatasetVersionFromClasspath("json/testDataset.json");
        prepareDataForExport(datasetVersion);

        //when
        Either<DataverseError, String> exportedDataset =
                exportService.exportDatasetVersionAsString(datasetVersion, ExporterType.DCTERMS);

        //then
        Assert.assertEquals(readFileToString("exportdata/dcterms.xml"), exportedDataset.get());
    }

    @Test
    @DisplayName("export DatasetVersion as string for ddi")
    public void exportDatasetVersionAsString_forDdi() throws IOException, JsonParseException, URISyntaxException {
        //given
        DatasetVersion datasetVersion = parseDatasetVersionFromClasspath("json/testDataset.json");
        prepareDataForExport(datasetVersion);

        //when
        Either<DataverseError, String> exportedDataset =
                exportService.exportDatasetVersionAsString(datasetVersion, ExporterType.DDI);

        //then
        Assert.assertEquals(readFileToString("exportdata/ddi.xml"), exportedDataset.get());

        System.out.println(exportedDataset.get());
    }

    @Test
    @DisplayName("export DatasetVersion as string for ddi without email")
    public void exportDatasetVersionAsString_forDdiWithoutEmail() throws IOException, JsonParseException {
        //given
        enableExcludingEmails();

        DatasetVersion datasetVersion = parseDatasetVersionFromClasspath("json/testDataset.json");
        prepareDataForExport(datasetVersion);

        //when
        Either<DataverseError, String> exportedDataset =
                exportService.exportDatasetVersionAsString(datasetVersion, ExporterType.DDI);

        //then
        Assert.assertEquals(readFileToString("exportdata/ddiWithoutEmail.xml"), exportedDataset.get());
    }

    @Test
    @DisplayName("export DatasetVersion as string for json")
    public void exportDatasetVersionAsString_forJson() throws IOException, JsonParseException, URISyntaxException {
        //given
        DatasetVersion datasetVersion = parseDatasetVersionFromClasspath("json/testDataset.json");
        prepareDataForExport(datasetVersion);

        //when
        Either<DataverseError, String> exportedDataset =
                exportService.exportDatasetVersionAsString(datasetVersion, ExporterType.JSON);

        //then
        Assert.assertEquals(readFileToString("exportdata/datasetInJson.json"), exportedDataset.get());
    }

    @Test
    @DisplayName("export DatasetVersion as string for Oai Ore")
    public void exportDatasetVersionAsString_forOaiOre() throws IOException, JsonParseException, URISyntaxException {
        //given
        DatasetVersion datasetVersion = parseDatasetVersionFromClasspath("json/testDataset.json");
        prepareDataForExport(datasetVersion);

        //when
        Either<DataverseError, String> exportedDataset =
                exportService.exportDatasetVersionAsString(datasetVersion, ExporterType.OAIORE);

        //then
        Assert.assertEquals(readFileToString("exportdata/oai_ore.json"), exportedDataset.get());
    }

    @Test
    @DisplayName("export DatasetVersion as string for Schema Org")
    public void exportDatasetVersionAsString_forSchemaOrg() throws IOException, JsonParseException, URISyntaxException {
        //given
        DatasetVersion datasetVersion = parseDatasetVersionFromClasspath("json/testDataset.json");
        prepareDataForExport(datasetVersion);

        //when
        Either<DataverseError, String> exportedDataset =
                exportService.exportDatasetVersionAsString(datasetVersion, ExporterType.SCHEMADOTORG);

        //then
        Assert.assertEquals(readFileToString("exportdata/schemaorg.json"), exportedDataset.get());
    }

    @Test
    @DisplayName("export DatasetVersion as string for OpenAire")
    public void exportDatasetVersionAsString_forOpenAire() throws IOException, JsonParseException, URISyntaxException {
        //given
        DatasetVersion datasetVersion = parseDatasetVersionFromClasspath("json/testDataset.json");
        prepareDataForExport(datasetVersion);

        //when
        Either<DataverseError, String> exportedDataset =
                exportService.exportDatasetVersionAsString(datasetVersion, ExporterType.OPENAIRE);

        //then
        Assert.assertEquals(readFileToString("exportdata/openaire.xml"), exportedDataset.get());
    }

    @Test
    @DisplayName("export DatasetVersion as string for DublinCore")
    public void exportDatasetVersionAsString_forDublinCore() throws IOException, JsonParseException {
        //given
        DatasetVersion datasetVersion = parseDatasetVersionFromClasspath("json/testDataset.json");
        prepareDataForExport(datasetVersion);

        //when
        Either<DataverseError, String> exportedDataset =
                exportService.exportDatasetVersionAsString(datasetVersion, ExporterType.DUBLINCORE);

        //then
        Assert.assertEquals(readFileToString("exportdata/dublincore.xml"), exportedDataset.get());
    }

    @Test
    @DisplayName("Get all exporters")
    public void getAllExporters() {
        //when
        Map<ExporterType, Exporter> allExporters = exportService.getAllExporters();

        //then
        Assert.assertTrue(allExporters.containsKey(ExporterType.JSON));
        Assert.assertTrue(allExporters.containsKey(ExporterType.DDI));
        Assert.assertTrue(allExporters.containsKey(ExporterType.DATACITE));
        Assert.assertTrue(allExporters.containsKey(ExporterType.SCHEMADOTORG));
        Assert.assertTrue(allExporters.containsKey(ExporterType.DCTERMS));
        Assert.assertTrue(allExporters.containsKey(ExporterType.OPENAIRE));
    }

    @Test
    @DisplayName("Get mediaType for dataCite exporter")
    public void getMediaType_forDataCite() {
        //when
        String mediaType = exportService.getMediaType(ExporterType.DATACITE);

        //then
        Assert.assertEquals(MediaType.APPLICATION_XML, mediaType);
    }

    @Test
    @DisplayName("Get mediaType for json exporter")
    public void getMediaType_forJsonExporter() {
        //when
        String mediaType = exportService.getMediaType(ExporterType.JSON);

        //then
        Assert.assertEquals(MediaType.APPLICATION_JSON, mediaType);
    }

    // -------------------- PRIVATE --------------------

    private void enableExcludingEmails() {
        when(settingsService.isTrueForKey(SettingsServiceBean.Key.ExcludeEmailFromExport)).thenReturn(true);
        when(systemConfig.getDataverseSiteUrl()).thenReturn("https://localhost");

        exportService = new ExportService(settingsService, systemConfig);
        exportService.setCurrentDate(LocalDate.of(2019, 7, 11));
        exportService.loadAllExporters();
    }

    private DatasetVersion parseDatasetVersionFromClasspath(String classpath) throws IOException, JsonParseException {

        try (ByteArrayInputStream is = new ByteArrayInputStream(IOUtils.resourceToByteArray(classpath, getClass().getClassLoader()))) {

            JsonObject jsonObject = Json.createReader(is).readObject();
            JsonParser jsonParser = new JsonParser(datasetFieldService, null, null);

            return jsonParser.parseDatasetVersion(jsonObject);
        }
    }

    private DatasetVersion prepareDataForExport(DatasetVersion datasetVersion) {
        Dataset dataset = new Dataset();
        dataset.setId(5L);
        dataset.setIdentifier("FK2/05NAR1");
        dataset.setProtocol("doi");
        dataset.setAuthority("10.5072");

        Dataverse root = new Dataverse();
        root.setName("Root");

        Dataverse owner = new Dataverse();
        owner.setName("Banner test");
        owner.setOwner(root);

        dataset.setOwner(owner);
        dataset.setPublicationDate(new Timestamp(DATE));
        dataset.setStorageIdentifier("file://10.5072/FK2/05NAR1");
        dataset.setVersions(Lists.newArrayList(datasetVersion));

        datasetVersion.setDataset(dataset);
        datasetVersion.setReleaseTime(new Timestamp(DATE));
        datasetVersion.setId(1L);
        datasetVersion.setMinorVersionNumber(0L);

        prepareDatasetFieldValues(datasetVersion);

        return datasetVersion;
    }

    private void prepareDatasetFieldValues(DatasetVersion datasetVersion) {
        List<DatasetField> datasetFields = datasetVersion.getDatasetFields();

        DatasetField titleValue = new DatasetField();
        titleValue.setFieldValue("Export test");
        titleValue.setId(3L);

        datasetFields.stream()
                .filter(datasetField -> datasetField.getDatasetFieldType().getName().equals(DatasetFieldConstant.title))
                .peek(titleValue::setDatasetFieldParent)
                .forEach(datasetField -> {
                    datasetField.getDatasetFieldType().setTitle("Title");
                    datasetField.getDatasetFieldType().setDisplayOrder(1);
                    datasetField.getDatasetFieldType().setUri("http://purl.org/dc/terms/title");
                });

        DatasetField subjectValue = new DatasetField();
        subjectValue.setFieldValue("Agricultural Sciences");
        subjectValue.setId(3L);

        datasetFields.stream()
                .filter(datasetField -> datasetField.getDatasetFieldType().getName().equals(DatasetFieldConstant.subject))
                .peek(subjectValue::setDatasetFieldParent)
                .forEach(datasetField -> {
                    datasetField.getDatasetFieldType().setTitle("Subject");
                    datasetField.getDatasetFieldType().setDisplayOrder(5);
                    datasetField.getDatasetFieldType().setUri("http://purl.org/dc/terms/subject");
                    datasetField.setDatasetFieldsChildren(Lists.newArrayList(subjectValue));
                    datasetField.setSingleControlledVocabularyValue(
                            new ControlledVocabularyValue(13L, subjectValue.getValue(), datasetField.getDatasetFieldType()));
                });


        setupDescriptionData(datasetFields);
        setupAuthorData(datasetFields);
        setupContactData(datasetFields);
    }

    private void setupAuthorData(List<DatasetField> datasetFields) {
        DatasetField authorField = datasetFields.stream()
                .filter(datasetField -> datasetField.getDatasetFieldType().getName().equals(DatasetFieldConstant.author))
                .findFirst().get();

        DatasetFieldType authorFieldType = authorField.getDatasetFieldType();
        authorFieldType.setTitle("Author");
        authorFieldType.setDisplayOrder(2);
        authorFieldType.setUri("http://purl.org/dc/terms/creator");

        authorField.setDatasetFieldsChildren(Lists.newArrayList(setupNameOfAuthor(), setupAffiliationOfAuthor()));

    }

    private DatasetField setupAffiliationOfAuthor() {
        DatasetFieldType authorAffiliation = new DatasetFieldType(DatasetFieldConstant.authorAffiliation, FieldType.TEXT, false);
        authorAffiliation.setTitle("Affiliation");

        DatasetField authorAffiliationDf = new DatasetField();
        authorAffiliationDf.setDatasetFieldType(authorAffiliation);
        authorAffiliationDf.setFieldValue("Dataverse.org");

        return authorAffiliationDf;
    }

    private DatasetField setupNameOfAuthor() {
        DatasetFieldType authorName = new DatasetFieldType(DatasetFieldConstant.authorName, FieldType.TEXT, false);
        authorName.setTitle("Name");

        DatasetField authorNameDF = new DatasetField();
        authorNameDF.setFieldValue("Admin, Dataverse");
        authorNameDF.setDatasetFieldType(authorName);

        return authorNameDF;
    }

    private void setupDescriptionData(List<DatasetField> datasetFields) {

        DatasetField descriptionField = datasetFields.stream()
                .filter(datasetField -> datasetField.getDatasetFieldType().getName().equals(DatasetFieldConstant.description))
                .findFirst().get();

        DatasetFieldType descriptionFieldType = descriptionField.getDatasetFieldType();
        descriptionFieldType.setDisplayOrder(4);
        descriptionFieldType.setTitle("Description");

        DatasetFieldType dsDescription = descriptionFieldType.getChildDatasetFieldTypes().iterator().next();
        dsDescription.setTitle("Text");

        descriptionFieldType.setChildDatasetFieldTypes(Lists.newArrayList(dsDescription));
    }

    private void setupContactData(List<DatasetField> datasetFields) {
        DatasetField datasetContact = datasetFields.stream()
                .filter(datasetField -> datasetField.getDatasetFieldType().getName().equals(DatasetFieldConstant.datasetContact))
                .findFirst().get();

        datasetContact.getDatasetFieldType().setTitle("Contact");
        datasetContact.getDatasetFieldType().setDisplayOrder(3);

        Collection<DatasetFieldType> childDatasetContact = datasetContact.getDatasetFieldType().getChildDatasetFieldTypes();

        for (DatasetFieldType contactChild : childDatasetContact) {
            if (contactChild.getName().equals(DatasetFieldConstant.datasetContactName)) {
                contactChild.setTitle("Name");

                DatasetField dsContactName = new DatasetField();
                dsContactName.setDatasetFieldType(new DatasetFieldType(DatasetFieldConstant.datasetContactName, FieldType.TEXT, true));

                contactChild.setDatasetFields(Lists.newArrayList(dsContactName));

                dsContactName.setDatasetFieldsChildren(Lists.newArrayList(new DatasetField()
                .setDatasetFieldParent(dsContactName).setFieldValue("Admin, Dataverse")));
            }

            if (contactChild.getName().equals(DatasetFieldConstant.datasetContactAffiliation)) {
                contactChild.setTitle("Affiliation");

                DatasetField dsContactAffiliation = new DatasetField();
                dsContactAffiliation.setDatasetFieldType(new DatasetFieldType(DatasetFieldConstant.datasetContactAffiliation, FieldType.TEXT, true));

                contactChild.setDatasetFields(Lists.newArrayList(dsContactAffiliation));

                dsContactAffiliation.setDatasetFieldsChildren(Lists.newArrayList(new DatasetField()
                .setDatasetFieldParent(dsContactAffiliation).setFieldValue("Dataverse.org")));
            }

            if (contactChild.getName().equals(DatasetFieldConstant.datasetContactEmail)) {
                contactChild.setTitle("E-mail");

                DatasetField dsContactEmail = new DatasetField();
                dsContactEmail.setDatasetFieldType(new DatasetFieldType(DatasetFieldConstant.datasetContactEmail, FieldType.EMAIL, true));

                contactChild.setDatasetFields(Lists.newArrayList(dsContactEmail));

                dsContactEmail.setDatasetFieldsChildren(Lists.newArrayList(new DatasetField()
                .setDatasetFieldParent(dsContactEmail).setFieldValue("dataverse@mailinator.com")));
            }
        }
    }

    private void mockDatasetFields() {
        MetadataBlock citationMetadataBlock = new MetadataBlock();
        citationMetadataBlock.setId(1L);
        citationMetadataBlock.setName("citation");
        citationMetadataBlock.setDisplayName("Citation Metadata");
        citationMetadataBlock.setNamespaceUri("https://dataverse.org/schema/citation/");


        DatasetFieldType titleFieldType = MocksFactory.makeDatasetFieldType("title", FieldType.TEXT, false, citationMetadataBlock);

        DatasetFieldType authorNameFieldType = MocksFactory.makeDatasetFieldType("authorName", FieldType.TEXT, false, citationMetadataBlock);
        DatasetFieldType authorAffiliationFieldType = MocksFactory.makeDatasetFieldType("authorAffiliation", FieldType.TEXT, false, citationMetadataBlock);
        DatasetFieldType authorFieldType = MocksFactory.makeComplexDatasetFieldType("author", true, citationMetadataBlock,
                                                                                    authorNameFieldType, authorAffiliationFieldType);
        authorFieldType.setDisplayOnCreate(true);

        DatasetFieldType datasetContactNameFieldType = MocksFactory.makeDatasetFieldType("datasetContactName", FieldType.TEXT, false, citationMetadataBlock);
        DatasetFieldType datasetContactAffiliationFieldType = MocksFactory.makeDatasetFieldType("datasetContactAffiliation", FieldType.TEXT, false, citationMetadataBlock);
        DatasetFieldType datasetContactEmailFieldType = MocksFactory.makeDatasetFieldType("datasetContactEmail", FieldType.EMAIL, false, citationMetadataBlock);
        DatasetFieldType datasetContactFieldType = MocksFactory.makeComplexDatasetFieldType("datasetContact", true, citationMetadataBlock,
                                                                                            datasetContactNameFieldType, datasetContactAffiliationFieldType, datasetContactEmailFieldType);
        datasetContactFieldType.setDisplayOrder(12);

        DatasetFieldType dsDescriptionValueFieldType = MocksFactory.makeDatasetFieldType("dsDescriptionValue", FieldType.TEXT, false, citationMetadataBlock);
        DatasetFieldType dsDescriptionDateFieldType = MocksFactory.makeDatasetFieldType("dsDescriptionDate", FieldType.TEXT, false, citationMetadataBlock);
        DatasetFieldType dsDescriptionFieldType = MocksFactory.makeComplexDatasetFieldType("dsDescription", true, citationMetadataBlock,
                                                                                           dsDescriptionValueFieldType, dsDescriptionDateFieldType);

        DatasetFieldType subjectFieldType = MocksFactory.makeControlledVocabDatasetFieldType("subject", true, citationMetadataBlock,
                                                                                             "agricultural_sciences", "arts_and_humanities", "chemistry");
        DatasetFieldType depositorFieldType = MocksFactory.makeDatasetFieldType("depositor", FieldType.TEXT, false, citationMetadataBlock);
        depositorFieldType.setTitle("Depositor");
        depositorFieldType.setDisplayOrder(6);

        DatasetFieldType dateOfDepositFieldType = MocksFactory.makeDatasetFieldType("dateOfDeposit", FieldType.TEXT, false, citationMetadataBlock);
        dateOfDepositFieldType.setTitle("Deposit Date");
        dateOfDepositFieldType.setDisplayOrder(7);
        dateOfDepositFieldType.setUri("http://purl.org/dc/terms/dateSubmitted");

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
    }

    private String readFileToString(String resourcePath) throws IOException {
        return IOUtils.resourceToString(resourcePath, StandardCharsets.UTF_8, getClass().getClassLoader());
    }
}