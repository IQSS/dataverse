package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import static edu.harvard.iq.dataverse.util.SystemConfig.SITE_URL;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
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

public class SchemaDotOrgExporterTest {

    private final SchemaDotOrgExporter schemaDotOrgExporter;
    DDIExporterTest.MockDatasetFieldSvc datasetFieldTypeSvc = null;

    public SchemaDotOrgExporterTest() {
        schemaDotOrgExporter = new SchemaDotOrgExporter();
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
        Set<DatasetFieldType> keywordChildTypes = new HashSet<>();
        keywordChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("keywordValue", DatasetFieldType.FieldType.TEXT, false)));
        keywordChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("keywordVocabulary", DatasetFieldType.FieldType.TEXT, false)));
        keywordChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("keywordVocabularyURI", DatasetFieldType.FieldType.TEXT, false)));
        keywordType.setChildDatasetFieldTypes(keywordChildTypes);

        DatasetFieldType topicClassificationType = datasetFieldTypeSvc.add(new DatasetFieldType("topicClassification", DatasetFieldType.FieldType.TEXT, true));
        Set<DatasetFieldType> topicClassificationTypes = new HashSet<>();
        topicClassificationTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("topicClassValue", DatasetFieldType.FieldType.TEXT, false)));
        topicClassificationTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("topicClassVocab", DatasetFieldType.FieldType.TEXT, false)));
        topicClassificationTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("topicClassVocabURI", DatasetFieldType.FieldType.TEXT, false)));
        topicClassificationType.setChildDatasetFieldTypes(topicClassificationTypes);

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
     * Test of exportDataset method, of class SchemaDotOrgExporter.
     */
    @Test
    public void testExportDataset() throws Exception {
        System.out.println("exportDataset");
        File datasetVersionJson = new File("src/test/resources/json/dataset-finch2.json");
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
        schemaDotOrgExporter.exportDataset(version, json1, byteArrayOutputStream);
        String jsonLd = byteArrayOutputStream.toString();
        System.out.println("schema.org JSON-LD: " + JsonUtil.prettyPrint(jsonLd));
        JsonReader jsonReader2 = Json.createReader(new StringReader(jsonLd));
        JsonObject json2 = jsonReader2.readObject();
        assertEquals("http://schema.org", json2.getString("@context"));
        assertEquals("Dataset", json2.getString("@type"));
        assertEquals("https://doi.org/myAuthority/myIdentifier", json2.getString("identifier"));
        assertEquals("Darwin's Finches", json2.getString("name"));
        assertEquals("Finch, Fiona", json2.getJsonArray("creator").getJsonObject(0).getString("name"));
        assertEquals("Birds Inc.", json2.getJsonArray("creator").getJsonObject(0).getString("affiliation"));
        assertEquals("1955-11-05", json2.getString("dateModified"));
        assertEquals("1", json2.getString("version"));
        assertEquals("Darwin's finches (also known as the Galápagos finches) are a group of about fifteen species of passerine birds.", json2.getString("description"));
        assertEquals("Medicine, Health and Life Sciences", json2.getJsonArray("keywords").getString(0));
        assertEquals("tcTerm1", json2.getJsonArray("keywords").getString(1));
        assertEquals("KeywordTerm1", json2.getJsonArray("keywords").getString(2));
        assertEquals("KeywordTerm2", json2.getJsonArray("keywords").getString(3));
        // This dataset, for example, has multiple keywords separated by commas: https://dataverse.harvard.edu/dataset.xhtml?persistentId=doi:10.7910/DVN/24034&version=2.0
        assertEquals("keywords, with, commas", json2.getJsonArray("keywords").getString(4));
        assertEquals("https://schema.org/version/3.3", json2.getString("schemaVersion"));
        assertEquals("DataCatalog", json2.getJsonObject("includedInDataCatalog").getString("@type"));
        assertEquals("LibraScholar", json2.getJsonObject("includedInDataCatalog").getString("name"));
        assertEquals("https://librascholar.org", json2.getJsonObject("includedInDataCatalog").getString("url"));
        assertEquals("Organization", json2.getJsonObject("provider").getString("@type"));
        assertEquals("LibraScholar", json2.getJsonObject("provider").getString("name"));
    }

    /**
     * Test of getProviderName method, of class SchemaDotOrgExporter.
     */
    @Test
    public void testGetProviderName() {
        System.out.println("getProviderName");
        assertEquals("schema.org", schemaDotOrgExporter.getProviderName());
    }

    /**
     * Test of getDisplayName method, of class SchemaDotOrgExporter.
     */
    @Test
    public void testGetDisplayName() {
        System.out.println("getDisplayName");
        // We capitalize "Schema.org" because it looks better in the dropdown list and it's what DataCite does in their UI.
        assertEquals("Schema.org JSON-LD", schemaDotOrgExporter.getDisplayName());
    }

    /**
     * Test of isXMLFormat method, of class SchemaDotOrgExporter.
     */
    @Test
    public void testIsXMLFormat() {
        System.out.println("isXMLFormat");
        assertEquals(false, schemaDotOrgExporter.isXMLFormat());
    }

    /**
     * Test of isHarvestable method, of class SchemaDotOrgExporter.
     */
    @Test
    public void testIsHarvestable() {
        System.out.println("isHarvestable");
        assertEquals(false, schemaDotOrgExporter.isHarvestable());
    }

    /**
     * Test of isAvailableToUsers method, of class SchemaDotOrgExporter.
     */
    @Test
    public void testIsAvailableToUsers() {
        System.out.println("isAvailableToUsers");
        assertEquals(true, schemaDotOrgExporter.isAvailableToUsers());
    }

    /**
     * Test of getXMLNameSpace method, of class SchemaDotOrgExporter.
     */
    @Test
    public void testGetXMLNameSpace() throws Exception {
        System.out.println("getXMLNameSpace");
        ExportException expectedException = null;
        try {
            String result = schemaDotOrgExporter.getXMLNameSpace();
        } catch (ExportException ex) {
            expectedException = ex;
        }
        assertEquals(SchemaDotOrgExporter.class.getSimpleName() + ": not an XML format.", expectedException.getMessage());
    }

    /**
     * Test of getXMLSchemaLocation method, of class SchemaDotOrgExporter.
     */
    @Test
    public void testGetXMLSchemaLocation() throws Exception {
        System.out.println("getXMLSchemaLocation");
        ExportException expectedException = null;
        try {
            String result = schemaDotOrgExporter.getXMLSchemaLocation();
        } catch (ExportException ex) {
            expectedException = ex;
        }
        assertEquals(SchemaDotOrgExporter.class.getSimpleName() + ": not an XML format.", expectedException.getMessage());
    }

    /**
     * Test of getXMLSchemaVersion method, of class SchemaDotOrgExporter.
     */
    @Test
    public void testGetXMLSchemaVersion() throws Exception {
        System.out.println("getXMLSchemaVersion");
        ExportException expectedException = null;
        try {
            String result = schemaDotOrgExporter.getXMLSchemaVersion();
        } catch (ExportException ex) {
            expectedException = ex;
        }
        assertEquals(SchemaDotOrgExporter.class.getSimpleName() + ": not an XML format.", expectedException.getMessage());
    }

    /**
     * Test of setParam method, of class SchemaDotOrgExporter.
     */
    @Test
    public void testSetParam() {
        System.out.println("setParam");
        String name = "";
        Object value = null;
        schemaDotOrgExporter.setParam(name, value);
    }

}
