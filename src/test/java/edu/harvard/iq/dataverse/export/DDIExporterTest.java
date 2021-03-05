package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldType.FieldType;
import edu.harvard.iq.dataverse.mocks.MockDatasetFieldSvc;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import edu.harvard.iq.dataverse.util.xml.XmlPrinter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Year;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class DDIExporterTest {

    MockDatasetFieldSvc datasetFieldTypeSvc = null;

    @Before
    public void setUp() {
        datasetFieldTypeSvc = new MockDatasetFieldSvc();

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
    }

    @Test
    public void testExportDataset() throws Exception {
        System.out.println("exportDataset");

        // FIXME: switch ddi/dataset-finch1.json
        //File datasetVersionJson = new File("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-finch1.json");
        File datasetVersionJson = new File("src/test/resources/json/dataset-finch1.json");
        String datasetVersionAsJson = new String(Files.readAllBytes(Paths.get(datasetVersionJson.getAbsolutePath())));

        JsonReader jsonReader = Json.createReader(new StringReader(datasetVersionAsJson));
        JsonObject json = jsonReader.readObject();
        JsonParser jsonParser = new JsonParser(datasetFieldTypeSvc, null, null);
        DatasetVersion version = jsonParser.parseDatasetVersion(json.getJsonObject("datasetVersion"));

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DDIExporter instance = new DDIExporter();
        boolean nullPointerFixed = false;
        if (nullPointerFixed) {
            instance.exportDataset(version, json, byteArrayOutputStream);
        }

        System.out.println("out: " + XmlPrinter.prettyPrintXml(byteArrayOutputStream.toString()));

    }

    @Test
    public void testCitation() throws Exception {
        System.out.println("testCitation");

        File datasetVersionJson = new File("src/test/resources/json/dataset-finch1.json");
        String datasetVersionAsJson = new String(Files.readAllBytes(Paths.get(datasetVersionJson.getAbsolutePath())));

        JsonReader jsonReader = Json.createReader(new StringReader(datasetVersionAsJson));
        JsonObject json = jsonReader.readObject();
        JsonParser jsonParser = new JsonParser(datasetFieldTypeSvc, null, null);
        DatasetVersion version = jsonParser.parseDatasetVersion(json.getJsonObject("datasetVersion"));
        version.setVersionState(DatasetVersion.VersionState.DRAFT);
        Dataset dataset = new Dataset();
        version.setDataset(dataset);
        dataset.setOwner(new Dataverse());
        String citation = version.getCitation();
        System.out.println("citation: " + citation);
        int currentYear = Year.now().getValue();
        assertEquals("Finch, Fiona, " + currentYear + ", \"Darwin's Finches\", LibraScholar, DRAFT VERSION", citation);
    }

    @Test
    public void testExportDatasetContactEmailPresent() throws Exception {
        File datasetVersionJson = new File("src/test/java/edu/harvard/iq/dataverse/export/ddi/datasetContactEmailPresent.json");
        String datasetVersionAsJson = new String(Files.readAllBytes(Paths.get(datasetVersionJson.getAbsolutePath())));

        JsonReader jsonReader = Json.createReader(new StringReader(datasetVersionAsJson));
        JsonObject json = jsonReader.readObject();
        JsonParser jsonParser = new JsonParser(datasetFieldTypeSvc, null, null);
        DatasetVersion version = jsonParser.parseDatasetVersion(json.getJsonObject("datasetVersion"));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DDIExporter instance = new DDIExporter();
        instance.exportDataset(version, json, byteArrayOutputStream);

        System.out.println(XmlPrinter.prettyPrintXml(byteArrayOutputStream.toString()));
        assertTrue(byteArrayOutputStream.toString().contains("finch@mailinator.com"));

    }

    @Test
    public void testExportDatasetContactEmailAbsent() throws Exception {
        File datasetVersionJson = new File("src/test/java/edu/harvard/iq/dataverse/export/ddi/datasetContactEmailAbsent.json");
        String datasetVersionAsJson = new String(Files.readAllBytes(Paths.get(datasetVersionJson.getAbsolutePath())));

        JsonReader jsonReader = Json.createReader(new StringReader(datasetVersionAsJson));
        JsonObject json = jsonReader.readObject();
//        JsonParser jsonParser = new JsonParser(datasetFieldTypeSvc, null, settingsSvc);
        JsonParser jsonParser = new JsonParser(datasetFieldTypeSvc, null, null);
        DatasetVersion version = jsonParser.parseDatasetVersion(json.getJsonObject("datasetVersion"));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DDIExporter instance = new DDIExporter();
        instance.exportDataset(version, json, byteArrayOutputStream);

        System.out.println(XmlPrinter.prettyPrintXml(byteArrayOutputStream.toString()));
        assertFalse(byteArrayOutputStream.toString().contains("finch@mailinator.com"));

    }

}
