package edu.harvard.iq.dataverse.export;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.UnitTestUtils;
import edu.harvard.iq.dataverse.citation.CitationDataExtractor;
import edu.harvard.iq.dataverse.citation.CitationFactory;
import edu.harvard.iq.dataverse.citation.StandardCitationFormatsConverter;
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
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import io.vavr.control.Either;
import org.apache.commons.collections4.IteratorUtils;
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

import javax.enterprise.inject.Instance;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ExportServiceTest {

    //10/07/2019
    private final long DATE = 1562766661000L;
    private final Clock clock = Clock.fixed(Instant.ofEpochMilli(DATE), ZoneOffset.UTC);

    private ExportService exportService;

    @Mock
    private SettingsServiceBean settingsService;

    @Mock
    private DatasetFieldServiceBean datasetFieldService;

    @Mock
    private SystemConfig systemConfig;

    @Mock
    private DataFileServiceBean dataFileService;

    @Mock
    private Instance<Exporter> exporters;

    private JsonLdBuilder jsonLdBuilder;

    private JsonPrinter jsonPrinter = new JsonPrinter(
            new CitationFactory(new CitationDataExtractor(), new StandardCitationFormatsConverter()));

    @BeforeEach
    void prepareData() {
        when(settingsService.isTrueForKey(SettingsServiceBean.Key.ExcludeEmailFromExport)).thenReturn(false);
        when(systemConfig.getDataverseSiteUrl()).thenReturn("https://localhost");
        when(dataFileService.isSameTermsOfUse(any(), any())).thenReturn(false);
        jsonLdBuilder = new JsonLdBuilder(dataFileService, settingsService, systemConfig);

        mockDatasetFields();

        when(exporters.iterator()).thenReturn(IteratorUtils.arrayIterator(
                new DataCiteExporter(new CitationFactory(new CitationDataExtractor(), new StandardCitationFormatsConverter())),
                new DCTermsExporter(settingsService, jsonPrinter),
                new DublinCoreExporter(settingsService,jsonPrinter),
                new OAI_OREExporter(settingsService, systemConfig, clock),
                new SchemaDotOrgExporter(jsonLdBuilder),
                new OpenAireExporter(settingsService, jsonPrinter),
                new JSONExporter(settingsService, jsonPrinter)));
        exportService = new ExportService(exporters);
        exportService.loadAllExporters();
    }

    // -------------------- TESTS --------------------

    @Test
    @DisplayName("export DatasetVersion as string for datacite")
    public void exportDatasetVersionAsString_forDataCite() throws IOException, JsonParseException, URISyntaxException {
        // given
        DatasetVersion datasetVersion = parseDatasetVersionFromClasspath("json/testDataset.json");
        prepareDataForExport(datasetVersion);

        // when
        Either<DataverseError, String> exportedDataset =
                exportService.exportDatasetVersionAsString(datasetVersion, ExporterType.DATACITE);

        // then
        assertThat(exportedDataset.get())
                .isEqualToIgnoringWhitespace(UnitTestUtils.readFileToString("exportdata/testDatacite.xml"));
    }

    @Test
    @DisplayName("export DatasetVersion as string for DCTerms")
    public void exportDatasetVersionAsString_forDCTerms() throws IOException, JsonParseException, URISyntaxException {
        // given
        DatasetVersion datasetVersion = parseDatasetVersionFromClasspath("json/testDataset.json");
        prepareDataForExport(datasetVersion);

        // when
        Either<DataverseError, String> exportedDataset =
                exportService.exportDatasetVersionAsString(datasetVersion, ExporterType.DCTERMS);

        // then
        assertThat(exportedDataset.get())
                .isEqualTo(UnitTestUtils.readFileToString("exportdata/dcterms.xml"));
    }

    @Test
    @DisplayName("export DatasetVersion as string for json")
    public void exportDatasetVersionAsString_forJson() throws IOException, JsonParseException, URISyntaxException {
        // given
        DatasetVersion datasetVersion = parseDatasetVersionFromClasspath("json/testDataset.json");
        prepareDataForExport(datasetVersion);

        // when
        Either<DataverseError, String> exportedDataset =
                exportService.exportDatasetVersionAsString(datasetVersion, ExporterType.JSON);

        // then
        assertThat(exportedDataset.get())
                .isEqualTo(UnitTestUtils.readFileToString("exportdata/datasetInJson.json"));
    }

    @Test
    @DisplayName("export DatasetVersion as string for Oai Ore")
    public void exportDatasetVersionAsString_forOaiOre() throws IOException, JsonParseException, URISyntaxException {
        // given
        DatasetVersion datasetVersion = parseDatasetVersionFromClasspath("json/testDatasetMultipleAuthors.json");
        prepareDataForExport(datasetVersion);

        // when
        Either<DataverseError, String> exportedDataset =
                exportService.exportDatasetVersionAsString(datasetVersion, ExporterType.OAIORE);

        // then
        assertThat(exportedDataset.get())
                .isEqualTo(UnitTestUtils.readFileToString("exportdata/oai_ore_authors.json"));
    }

    @Test
    @DisplayName("export DatasetVersion as string for Schema Org")
    public void exportDatasetVersionAsString_forSchemaOrg() throws IOException, JsonParseException, URISyntaxException {
        // given
        DatasetVersion datasetVersion = parseDatasetVersionFromClasspath("json/testDataset.json");
        prepareDataForExport(datasetVersion);

        // when
        Either<DataverseError, String> exportedDataset =
                exportService.exportDatasetVersionAsString(datasetVersion, ExporterType.SCHEMADOTORG);

        // then
        assertThat(exportedDataset.get())
                .isEqualTo(UnitTestUtils.readFileToString("exportdata/schemaorg.json"));
    }

    @Test
    @DisplayName("export DatasetVersion as string for OpenAire")
    public void exportDatasetVersionAsString_forOpenAire() throws IOException, JsonParseException, URISyntaxException {
        // given
        DatasetVersion datasetVersion = parseDatasetVersionFromClasspath("json/testDataset.json");
        prepareDataForExport(datasetVersion);

        // when
        Either<DataverseError, String> exportedDataset =
                exportService.exportDatasetVersionAsString(datasetVersion, ExporterType.OPENAIRE);

        // then
        assertThat(exportedDataset.get())
                .isEqualTo(UnitTestUtils.readFileToString("exportdata/openaire.xml"));
    }

    @Test
    @DisplayName("export DatasetVersion as string for DublinCore")
    public void exportDatasetVersionAsString_forDublinCore() throws IOException, JsonParseException {
        // given
        DatasetVersion datasetVersion = parseDatasetVersionFromClasspath("json/testDataset.json");
        prepareDataForExport(datasetVersion);

        // when
        Either<DataverseError, String> exportedDataset =
                exportService.exportDatasetVersionAsString(datasetVersion, ExporterType.DUBLINCORE);

        // then
        assertThat(exportedDataset.get())
                .isEqualTo(UnitTestUtils.readFileToString("exportdata/dublincore.xml"));
    }

    @Test
    @DisplayName("Get all exporters")
    public void getAllExporters() {
        // when
        Map<ExporterType, Exporter> allExporters = exportService.getAllExporters();

        // then
        assertThat(allExporters).containsKeys(ExporterType.JSON, ExporterType.DATACITE, ExporterType.SCHEMADOTORG,
                ExporterType.DCTERMS, ExporterType.OPENAIRE);
    }

    @Test
    @DisplayName("Get mediaType for dataCite exporter")
    public void getMediaType_forDataCite() {
        // when
        String mediaType = exportService.getMediaType(ExporterType.DATACITE);

        // then
        assertThat(mediaType).isEqualTo(MediaType.APPLICATION_XML);
    }

    @Test
    @DisplayName("Get mediaType for json exporter")
    public void getMediaType_forJsonExporter() {
        // when
        String mediaType = exportService.getMediaType(ExporterType.JSON);

        // then
        assertThat(mediaType).isEqualTo(MediaType.APPLICATION_JSON);
    }

    // -------------------- PRIVATE --------------------

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
        datasetFields.stream()
                .filter(datasetField -> datasetField.getDatasetFieldType().getName().equals(DatasetFieldConstant.author))
                .forEach(authorField -> {
                    DatasetFieldType authorFieldType = authorField.getDatasetFieldType();
                    authorFieldType.setTitle("Author");
                    authorFieldType.setDisplayOrder(2);
                    authorFieldType.setUri("http://purl.org/dc/terms/creator");
                    authorField.getDatasetFieldsChildren().stream().forEach(childField -> setupChildField(childField));
                });
    }

    private void setupChildField(DatasetField datasetField) {
        if (datasetField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorAffiliation)) {
            datasetField.getDatasetFieldType().setTitle("Affiliation");
        } else if (datasetField.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorName)) {
            datasetField.getDatasetFieldType().setTitle("Name");
        }
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

                dsContactName.setDatasetFieldsChildren(Lists.newArrayList(new DatasetField()
                .setDatasetFieldParent(dsContactName).setFieldValue("Admin, Dataverse")));
            }

            if (contactChild.getName().equals(DatasetFieldConstant.datasetContactAffiliation)) {
                contactChild.setTitle("Affiliation");

                DatasetField dsContactAffiliation = new DatasetField();
                dsContactAffiliation.setDatasetFieldType(new DatasetFieldType(DatasetFieldConstant.datasetContactAffiliation, FieldType.TEXT, true));

                dsContactAffiliation.setDatasetFieldsChildren(Lists.newArrayList(new DatasetField()
                .setDatasetFieldParent(dsContactAffiliation).setFieldValue("Dataverse.org")));
            }

            if (contactChild.getName().equals(DatasetFieldConstant.datasetContactEmail)) {
                contactChild.setTitle("E-mail");

                DatasetField dsContactEmail = new DatasetField();
                dsContactEmail.setDatasetFieldType(new DatasetFieldType(DatasetFieldConstant.datasetContactEmail, FieldType.EMAIL, true));

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
        DatasetFieldType authorAffiliationIdentifierFieldType = MocksFactory.makeDatasetFieldType("authorAffiliationIdentifier", FieldType.TEXT, false, citationMetadataBlock);

        DatasetFieldType authorFieldType = MocksFactory.makeComplexDatasetFieldType("author", true, citationMetadataBlock,
                                                                                    authorNameFieldType, authorAffiliationFieldType, authorAffiliationIdentifierFieldType);
        authorFieldType.setDisplayOnCreate(true);

        DatasetFieldType grantNumberAgencyFieldType = MocksFactory.makeDatasetFieldType("grantNumberAgency", FieldType.TEXT, false, citationMetadataBlock);
        DatasetFieldType grantNumberAgencyIdentifierFieldType = MocksFactory.makeDatasetFieldType("grantNumberAgencyIdentifier", FieldType.TEXT, false, citationMetadataBlock);
        DatasetFieldType grantNumberProgramFieldType = MocksFactory.makeDatasetFieldType("grantNumberProgram", FieldType.TEXT, false, citationMetadataBlock);
        DatasetFieldType grantNumberValueFieldType = MocksFactory.makeDatasetFieldType("grantNumberValue", FieldType.TEXT, false, citationMetadataBlock);

        DatasetFieldType grantNumberFieldType = MocksFactory.makeComplexDatasetFieldType("grantNumber", true, citationMetadataBlock,
                grantNumberAgencyFieldType, grantNumberAgencyIdentifierFieldType, grantNumberProgramFieldType, grantNumberValueFieldType);
        grantNumberFieldType.setDisplayOnCreate(true);

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
        when(datasetFieldService.findByNameOpt(eq("authorAffiliationIdentifier"))).thenReturn(authorAffiliationIdentifierFieldType);
        when(datasetFieldService.findByNameOpt(eq("grantNumber"))).thenReturn(grantNumberFieldType);
        when(datasetFieldService.findByNameOpt(eq("grantNumberAgency"))).thenReturn(grantNumberAgencyFieldType);
        when(datasetFieldService.findByNameOpt(eq("grantNumberAgencyIdentifier"))).thenReturn(grantNumberAgencyIdentifierFieldType);
        when(datasetFieldService.findByNameOpt(eq("grantNumberProgram"))).thenReturn(grantNumberProgramFieldType);
        when(datasetFieldService.findByNameOpt(eq("grantNumberValue"))).thenReturn(grantNumberValueFieldType);
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
}
