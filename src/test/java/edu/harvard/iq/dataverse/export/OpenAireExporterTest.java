package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import static edu.harvard.iq.dataverse.util.SystemConfig.SITE_URL;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class OpenAireExporterTest {

    private final OpenAireExporter openAireExporter;
    DDIExporterTest.MockDatasetFieldSvc datasetFieldTypeSvc = null;

    public OpenAireExporterTest() {
        openAireExporter = new OpenAireExporter();
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {

        datasetFieldTypeSvc = new DDIExporterTest.MockDatasetFieldSvc();

        DatasetFieldType titleType = datasetFieldTypeSvc.add(new DatasetFieldType("title", DatasetFieldType.FieldType.TEXTBOX, false));
        DatasetFieldType authorType = datasetFieldTypeSvc.add(new DatasetFieldType("author", DatasetFieldType.FieldType.TEXT, true));
        Set<DatasetFieldType> authorChildTypes = new HashSet<>();
        authorChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("authorName", DatasetFieldType.FieldType.TEXT, false)));
        authorChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("authorAffiliation", DatasetFieldType.FieldType.TEXT, false)));
        authorChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("authorIdentifier", DatasetFieldType.FieldType.TEXT, false)));
        DatasetFieldType authorIdentifierSchemeType = datasetFieldTypeSvc.add(new DatasetFieldType("authorIdentifierScheme", DatasetFieldType.FieldType.TEXT, false));
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

        DatasetFieldType datasetContactType = datasetFieldTypeSvc.add(new DatasetFieldType("datasetContact", DatasetFieldType.FieldType.TEXT, true));
        Set<DatasetFieldType> datasetContactTypes = new HashSet<>();
        datasetContactTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("datasetContactEmail", DatasetFieldType.FieldType.TEXT, false)));
        datasetContactTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("datasetContactName", DatasetFieldType.FieldType.TEXT, false)));
        datasetContactTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("datasetContactAffiliation", DatasetFieldType.FieldType.TEXT, false)));
        for (DatasetFieldType t : datasetContactTypes) {
            t.setParentDatasetFieldType(datasetContactType);
        }
        datasetContactType.setChildDatasetFieldTypes(datasetContactTypes);

        DatasetFieldType dsDescriptionType = datasetFieldTypeSvc.add(new DatasetFieldType("dsDescription", DatasetFieldType.FieldType.TEXT, true));
        Set<DatasetFieldType> dsDescriptionTypes = new HashSet<>();
        dsDescriptionTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("dsDescriptionValue", DatasetFieldType.FieldType.TEXT, false)));
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

    @After
    public void tearDown() {
    }

    /**
     * Test of getProviderName method, of class OpenAireExporter.
     */
    @Test
    public void testGetProviderName() {
        System.out.println("getProviderName");
        OpenAireExporter instance = new OpenAireExporter();
        String expResult = "oai_datacite";
        String result = instance.getProviderName();
        assertEquals(expResult, result);
    }

    /**
     * Test of getDisplayName method, of class OpenAireExporter.
     */
    @Test
    public void testGetDisplayName() {
        System.out.println("getDisplayName");
        OpenAireExporter instance = new OpenAireExporter();
        String expResult = "DataCite OpenAIRE";
        String result = instance.getDisplayName();
        assertEquals(expResult, result);
    }

    /**
     * Test of exportDataset method, of class OpenAireExporter.
     */
    @Test
    public void testExportDataset() throws Exception {
        System.out.println("exportDataset");

        File datasetVersionJson = new File("src/test/resources/json/dataset-finch1.json");
        String datasetVersionAsJson = new String(Files.readAllBytes(Paths.get(datasetVersionJson.getAbsolutePath())));

        JsonReader jsonReader1 = Json.createReader(new StringReader(datasetVersionAsJson));
        JsonObject json1 = jsonReader1.readObject();
        JsonParser jsonParser = new JsonParser(datasetFieldTypeSvc, null, null);
        DatasetVersion version = jsonParser.parseDatasetVersion(json1.getJsonObject("datasetVersion"));
        version.setVersionState(DatasetVersion.VersionState.RELEASED);
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");
        Date publicationDate = dateFmt.parse("19551105");
        version.setReleaseTime(publicationDate);
        version.setVersionNumber(1l);
        // TODO: It might be nice to test TermsOfUseAndAccess some day
        version.setTermsOfUseAndAccess(null);
        Dataset dataset = new Dataset();
        dataset.setProtocol("doi");
        dataset.setAuthority("myAuthority");
        dataset.setIdentifier("myIdentifier");
        version.setDataset(dataset);
        Dataverse dataverse = new Dataverse();
        dataverse.setName("LibraScholar");
        dataset.setOwner(dataverse);
        System.setProperty(SITE_URL, "https://librascholar.org");

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        // TODO: Get this working.
//        openAireExporter.exportDataset(version, json1, byteArrayOutputStream);

    }

    /**
     * Test of isXMLFormat method, of class OpenAireExporter.
     */
    @Test
    public void testIsXMLFormat() {
        System.out.println("isXMLFormat");
        OpenAireExporter instance = new OpenAireExporter();
        Boolean expResult = true;
        Boolean result = instance.isXMLFormat();
        assertEquals(expResult, result);
    }

    /**
     * Test of isHarvestable method, of class OpenAireExporter.
     */
    @Test
    public void testIsHarvestable() {
        System.out.println("isHarvestable");
        OpenAireExporter instance = new OpenAireExporter();
        Boolean expResult = true;
        Boolean result = instance.isHarvestable();
        assertEquals(expResult, result);
    }

    /**
     * Test of isAvailableToUsers method, of class OpenAireExporter.
     */
    @Test
    public void testIsAvailableToUsers() {
        System.out.println("isAvailableToUsers");
        OpenAireExporter instance = new OpenAireExporter();
        Boolean expResult = true;
        Boolean result = instance.isAvailableToUsers();
        assertEquals(expResult, result);
    }

    /**
     * Test of getXMLNameSpace method, of class OpenAireExporter.
     */
    @Test
    public void testGetXMLNameSpace() throws Exception {
        System.out.println("getXMLNameSpace");
        OpenAireExporter instance = new OpenAireExporter();
        String expResult = "http://datacite.org/schema/kernel-4";
        String result = instance.getXMLNameSpace();
        assertEquals(expResult, result);
    }

    /**
     * Test of getXMLSchemaLocation method, of class OpenAireExporter.
     */
    @Test
    public void testGetXMLSchemaLocation() throws Exception {
        System.out.println("getXMLSchemaLocation");
        OpenAireExporter instance = new OpenAireExporter();
        String expResult = "http://schema.datacite.org/meta/kernel-4.1/metadata.xsd";
        String result = instance.getXMLSchemaLocation();
        assertEquals(expResult, result);
    }

    /**
     * Test of getXMLSchemaVersion method, of class OpenAireExporter.
     */
    @Test
    public void testGetXMLSchemaVersion() throws Exception {
        System.out.println("getXMLSchemaVersion");
        OpenAireExporter instance = new OpenAireExporter();
        String expResult = "4.1";
        String result = instance.getXMLSchemaVersion();
        assertEquals(expResult, result);
    }

    /**
     * Test of setParam method, of class OpenAireExporter.
     */
    @Test
    public void testSetParam() {
        System.out.println("setParam");
        String name = "";
        Object value = null;
        OpenAireExporter instance = new OpenAireExporter();
        instance.setParam(name, value);
    }

}
