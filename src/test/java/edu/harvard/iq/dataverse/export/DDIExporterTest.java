package edu.harvard.iq.dataverse.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldType.FieldType;
import edu.harvard.iq.dataverse.branding.BrandingUtilTest;
import edu.harvard.iq.dataverse.export.ddi.DdiExportUtil;
import io.gdcc.spi.export.ExportDataProvider;
import io.gdcc.spi.export.ExportException;
import edu.harvard.iq.dataverse.license.LicenseServiceBean;
import edu.harvard.iq.dataverse.mocks.MockDatasetFieldSvc;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.util.xml.XmlPrinter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.xmlunit.assertj3.XmlAssert;
import org.xmlunit.builder.Input;

public class DDIExporterTest {

    private static final Logger logger = Logger.getLogger(DDIExporterTest.class.getCanonicalName());
    private static final SettingsServiceBean settingsService = Mockito.mock(SettingsServiceBean.class);
    private static final LicenseServiceBean licenseService = Mockito.mock(LicenseServiceBean.class);
    private static final MockDatasetFieldSvc datasetFieldTypeSvc = new MockDatasetFieldSvc();
    private static final Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDateTime>) (JsonElement json, Type type, JsonDeserializationContext jsonDeserializationContext) -> {
        Instant instant = Instant.ofEpochMilli(json.getAsJsonPrimitive().getAsLong());
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }).create();
    
    /*
     * Setup and teardown mocks for BrandingUtil for atomicity.
     * Setup the mocked DatasetFieldSvc
     * Setup and teardown the mocked SettingsServiceBean
     */
    @BeforeAll
    private static void setUpAll() {
        BrandingUtilTest.setupMocks();
        mockDatasetFieldSvc();
        DdiExportUtil.injectSettingsService(settingsService);
    }
    @AfterAll
    private static void tearDownAll() {
        BrandingUtilTest.tearDownMocks();
        DdiExportUtil.injectSettingsService(null);
    }

    @Test
    public void testExportDataset() throws JsonParseException, IOException, ExportException {
        //given
        String datasetDtoJsonString = Files.readString(Path.of("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-finch1.json"), StandardCharsets.UTF_8);
        
        JsonObject datasetDtoJson = Json.createReader(new StringReader(datasetDtoJsonString)).readObject();
        
        ExportDataProvider exportDataProviderStub = Mockito.mock(ExportDataProvider.class);
        Mockito.when(exportDataProviderStub.getDatasetJson()).thenReturn(datasetDtoJson);
        Mockito.when(exportDataProviderStub.getDatasetFileDetails()).thenReturn(Json.createArrayBuilder().build());
        
        
        //when
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        new DDIExporter().exportDataset(exportDataProviderStub, byteArrayOutputStream);

        // then
        String xml = XmlPrinter.prettyPrintXml(byteArrayOutputStream.toString(StandardCharsets.UTF_8));
        logger.fine(xml);
        XmlAssert.assertThat(xml).isValidAgainst(Input.fromPath(Path.of("src/test/resources/xml/xsd/ddi-codebook-2.5/ddi_codebook_2_5.xsd")).build());
        logger.severe("DDIExporterTest.testExportDataset() creates XML that should now be valid, since DDIExportUtil has been fixed.");
    }

    @Test
    public void testExportDatasetContactEmailPresent() throws Exception {
        File datasetVersionJson = new File("src/test/java/edu/harvard/iq/dataverse/export/ddi/datasetContactEmailPresent.json");
        String datasetVersionAsJson = new String(Files.readAllBytes(Paths.get(datasetVersionJson.getAbsolutePath())));

        JsonObject json = JsonUtil.getJsonObject(datasetVersionAsJson);
        
        ExportDataProvider exportDataProviderStub = Mockito.mock(ExportDataProvider.class);
        Mockito.when(exportDataProviderStub.getDatasetJson()).thenReturn(json);
        Mockito.when(exportDataProviderStub.getDatasetFileDetails()).thenReturn(Json.createArrayBuilder().build());
        
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DDIExporter instance = new DDIExporter();
        instance.exportDataset(exportDataProviderStub, byteArrayOutputStream);

        logger.fine(XmlPrinter.prettyPrintXml(byteArrayOutputStream.toString()));
        assertTrue(byteArrayOutputStream.toString().contains("finch@mailinator.com"));

    }

    @Test
    public void testExportDatasetContactEmailAbsent() throws Exception {
        File datasetVersionJson = new File("src/test/java/edu/harvard/iq/dataverse/export/ddi/datasetContactEmailAbsent.json");
        String datasetVersionAsJson = new String(Files.readAllBytes(Paths.get(datasetVersionJson.getAbsolutePath())));

        JsonObject json = JsonUtil.getJsonObject(datasetVersionAsJson);
        
        ExportDataProvider exportDataProviderStub = Mockito.mock(ExportDataProvider.class);
        Mockito.when(exportDataProviderStub.getDatasetJson()).thenReturn(json);
        Mockito.when(exportDataProviderStub.getDatasetFileDetails()).thenReturn(Json.createArrayBuilder().build());
        
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DDIExporter instance = new DDIExporter();
        instance.exportDataset(exportDataProviderStub, byteArrayOutputStream);

        logger.fine(XmlPrinter.prettyPrintXml(byteArrayOutputStream.toString()));
        assertFalse(byteArrayOutputStream.toString().contains("finch@mailinator.com"));

    }
    
    private static void mockDatasetFieldSvc() {
        DatasetFieldType titleType = datasetFieldTypeSvc.add(new DatasetFieldType("title", FieldType.TEXTBOX, false));
        DatasetFieldType authorType = datasetFieldTypeSvc.add(new DatasetFieldType("author", FieldType.TEXT, true));
        Set<DatasetFieldType> authorChildTypes = new HashSet<>();
        authorChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("authorName", FieldType.TEXT, false)));
        authorChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("authorAffiliation", FieldType.TEXT, false)));
        authorChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("authorIdentifier", FieldType.TEXT, false)));
        DatasetFieldType authorIdentifierSchemeType = datasetFieldTypeSvc.add(new DatasetFieldType("authorIdentifierScheme", FieldType.TEXT, false));
        authorIdentifierSchemeType.setAllowControlledVocabulary(true);
        authorIdentifierSchemeType.setControlledVocabularyValues(Arrays.asList(
            // Why aren't these enforced? Should be ORCID, etc.
            new ControlledVocabularyValue(1l, "ark", authorIdentifierSchemeType),
            new ControlledVocabularyValue(2l, "doi", authorIdentifierSchemeType),
            new ControlledVocabularyValue(3l, "url", authorIdentifierSchemeType)
        ));
        authorChildTypes.add(datasetFieldTypeSvc.add(authorIdentifierSchemeType));
        for (DatasetFieldType t : authorChildTypes) {
            t.setParentDatasetFieldType(authorType);
        }
        authorType.setChildDatasetFieldTypes(authorChildTypes);
    
        DatasetFieldType datasetContactType = datasetFieldTypeSvc.add(new DatasetFieldType("datasetContact", FieldType.TEXT, true));
        Set<DatasetFieldType> datasetContactTypes = new HashSet<>();
        datasetContactTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("datasetContactEmail", FieldType.TEXT, false)));
        datasetContactTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("datasetContactName", FieldType.TEXT, false)));
        datasetContactTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("datasetContactAffiliation", FieldType.TEXT, false)));
        for (DatasetFieldType t : datasetContactTypes) {
            t.setParentDatasetFieldType(datasetContactType);
        }
        datasetContactType.setChildDatasetFieldTypes(datasetContactTypes);
    
        DatasetFieldType dsDescriptionType = datasetFieldTypeSvc.add(new DatasetFieldType("dsDescription", FieldType.TEXT, true));
        Set<DatasetFieldType> dsDescriptionTypes = new HashSet<>();
        dsDescriptionTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("dsDescriptionValue", FieldType.TEXT, false)));
        for (DatasetFieldType t : dsDescriptionTypes) {
            t.setParentDatasetFieldType(dsDescriptionType);
        }
        dsDescriptionType.setChildDatasetFieldTypes(dsDescriptionTypes);
    
        DatasetFieldType keywordType = datasetFieldTypeSvc.add(new DatasetFieldType("keyword", DatasetFieldType.FieldType.TEXT, true));
        DatasetFieldType descriptionType = datasetFieldTypeSvc.add(new DatasetFieldType("description", DatasetFieldType.FieldType.TEXTBOX, false));
    
        DatasetFieldType subjectType = datasetFieldTypeSvc.add(new DatasetFieldType("subject", DatasetFieldType.FieldType.TEXT, true));
        subjectType.setAllowControlledVocabulary(true);
        subjectType.setControlledVocabularyValues(Arrays.asList(
            new ControlledVocabularyValue(1l, "mgmt", subjectType),
            new ControlledVocabularyValue(2l, "law", subjectType),
            new ControlledVocabularyValue(3l, "cs", subjectType)
        ));
    
        DatasetFieldType pubIdType = datasetFieldTypeSvc.add(new DatasetFieldType("publicationIdType", DatasetFieldType.FieldType.TEXT, false));
        pubIdType.setAllowControlledVocabulary(true);
        pubIdType.setControlledVocabularyValues(Arrays.asList(
            new ControlledVocabularyValue(1l, "ark", pubIdType),
            new ControlledVocabularyValue(2l, "doi", pubIdType),
            new ControlledVocabularyValue(3l, "url", pubIdType)
        ));
    
        DatasetFieldType compoundSingleType = datasetFieldTypeSvc.add(new DatasetFieldType("coordinate", DatasetFieldType.FieldType.TEXT, true));
        Set<DatasetFieldType> childTypes = new HashSet<>();
        childTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("lat", DatasetFieldType.FieldType.TEXT, false)));
        childTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("lon", DatasetFieldType.FieldType.TEXT, false)));
    
        for (DatasetFieldType t : childTypes) {
            t.setParentDatasetFieldType(compoundSingleType);
        }
        compoundSingleType.setChildDatasetFieldTypes(childTypes);
        
        datasetFieldTypeSvc.setMetadataBlock("citation");
    }
}
