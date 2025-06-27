package edu.harvard.iq.dataverse.pidproviders.doi.datacite;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DataCitation;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetAuthor;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.DatasetFieldType.FieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.dataset.DatasetType;
import edu.harvard.iq.dataverse.pidproviders.PidProviderFactoryBean;
import edu.harvard.iq.dataverse.pidproviders.doi.DoiMetadata;
import edu.harvard.iq.dataverse.pidproviders.doi.XmlMetadataTemplate;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.CompoundVocabularyException;
import edu.harvard.iq.dataverse.util.json.ControlledVocabularyException;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import edu.harvard.iq.dataverse.util.xml.XmlValidator;
import io.restassured.path.xml.XmlPath;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xml.sax.SAXException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
@LocalJvmSettings
@JvmSetting(key = JvmSettings.SITE_URL, value = "https://example.com")

public class XmlMetadataTemplateTest {

    static DataverseServiceBean dataverseSvc;
    static SettingsServiceBean settingsSvc;
    static PidProviderFactoryBean pidService;
    static final String DEFAULT_NAME = "LibraScholar";

    @BeforeAll
    public static void setupMocks() {
        dataverseSvc = Mockito.mock(DataverseServiceBean.class);
        settingsSvc = Mockito.mock(SettingsServiceBean.class);
        BrandingUtil.injectServices(dataverseSvc, settingsSvc);

        // initial values (needed here for other tests where this method is reused!)
        Mockito.when(settingsSvc.getValueForKey(SettingsServiceBean.Key.InstallationName)).thenReturn(DEFAULT_NAME);
        Mockito.when(dataverseSvc.getRootDataverseName()).thenReturn(DEFAULT_NAME);

        pidService = Mockito.mock(PidProviderFactoryBean.class);
        Mockito.when(pidService.isGlobalIdLocallyUnique(any(GlobalId.class))).thenReturn(true);
        Mockito.when(pidService.getProducer()).thenReturn("RootDataverse");

    }

    /**
     * A minimal example to assure that the XMLMetadataTemplate generates output
     * consistent with the DataCite XML v4.5 schema.
     */
    @Test
    public void testDataCiteXMLCreation() throws IOException {
        DoiMetadata doiMetadata = new DoiMetadata();
        doiMetadata.setTitle("A Title");
        DatasetFieldType dft = new DatasetFieldType(DatasetFieldConstant.authorName, FieldType.TEXT, false);
        dft.setDisplayFormat("#VALUE");
        DatasetFieldType dft2 = new DatasetFieldType(DatasetFieldConstant.authorAffiliation, FieldType.TEXT, false);
        dft2.setDisplayFormat("#VALUE");
        DatasetAuthor alice = new DatasetAuthor();
        DatasetField df1 = new DatasetField();
        df1.setDatasetFieldType(dft);
        df1.setSingleValue("Alice");
        alice.setName(df1);
        DatasetField df2 = new DatasetField();
        df2.setDatasetFieldType(dft2);
        df2.setSingleValue("Harvard University");
        alice.setAffiliation(df2);
        alice.setIdType("ORCID");
        alice.setIdValue("0000-0002-1825-0097");
        DatasetAuthor bob = new DatasetAuthor();
        DatasetField df3 = new DatasetField();
        df3.setDatasetFieldType(dft);
        df3.setSingleValue("Bob");
        bob.setName(df3);
        DatasetField df4 = new DatasetField();
        df4.setDatasetFieldType(dft2);
        df4.setSingleValue("QDR");
        bob.setAffiliation(df4);
        DatasetAuthor harvard = new DatasetAuthor();
        DatasetField df5 = new DatasetField();
        df5.setDatasetFieldType(dft);
        df5.setSingleValue("Harvard University");
        harvard.setName(df5);
        harvard.setIdType("ROR");
        harvard.setIdValue("03vek6s52");
        DatasetAuthor qdr = new DatasetAuthor();
        DatasetField df6 = new DatasetField();
        df6.setDatasetFieldType(dft);
        df6.setSingleValue("Qualitative Data Repository");
        qdr.setName(df6);
        qdr.setIdType("ROR");
        // This value is set improperly as a URL. It should be just
        // the identifier (014trz974) as in the ORCID example above.
        qdr.setIdValue("https://ror.org/014trz974");
        List<DatasetAuthor> authors = new ArrayList<>();
        authors.add(alice);
        authors.add(bob);
        authors.add(harvard);
        authors.add(qdr);
        doiMetadata.setAuthors(authors);
        doiMetadata.setPublisher("Dataverse");
        XmlMetadataTemplate template = new XmlMetadataTemplate(doiMetadata);

        Dataset d = new Dataset();
        GlobalId doi = new GlobalId("doi", "10.5072", "FK2/ABCDEF", null, null, null);
        d.setGlobalId(doi);
        DatasetVersion dv = new DatasetVersion();
        TermsOfUseAndAccess toa = new TermsOfUseAndAccess();
        toa.setTermsOfUse("Some terms");
        dv.setTermsOfUseAndAccess(toa);
        dv.setDataset(d);
        DatasetFieldType primitiveDSFType = new DatasetFieldType(DatasetFieldConstant.title,
                DatasetFieldType.FieldType.TEXT, false);
        DatasetField testDatasetField = new DatasetField();

        dv.setVersionState(VersionState.DRAFT);

        testDatasetField.setDatasetVersion(dv);
        testDatasetField.setDatasetFieldType(primitiveDSFType);
        testDatasetField.setSingleValue("First Title");
        List<DatasetField> fields = new ArrayList<>();
        fields.add(testDatasetField);
        dv.setDatasetFields(fields);
        ArrayList<DatasetVersion> dsvs = new ArrayList<>();
        dsvs.add(0, dv);
        d.setVersions(dsvs);
        DatasetType dType = new DatasetType();
        dType.setName(DatasetType.DATASET_TYPE_DATASET);
        d.setDatasetType(dType);

        String xml = template.generateXML(d);
        System.out.println("Output from minimal example is " + xml);
        try {
            StreamSource source = new StreamSource(new StringReader(xml));
            source.setSystemId("DataCite XML for test dataset");
            assertTrue(XmlValidator.validateXmlSchema(source,
                    new URL("https://schema.datacite.org/meta/kernel-4/metadata.xsd")));
        } catch (SAXException e) {
            System.out.println("Invalid schema: " + e.getMessage());
        }

        assertEquals("Alice", XmlPath.from(xml).getString("resource.creators.creator[0].creatorName"));
        assertEquals("https://orcid.org/0000-0002-1825-0097", XmlPath.from(xml).getString("resource.creators.creator[0].nameIdentifier"));
        assertEquals("ORCID", XmlPath.from(xml).getString("resource.creators.creator[0].nameIdentifier.@nameIdentifierScheme"));
        assertEquals("https://orcid.org", XmlPath.from(xml).getString("resource.creators.creator[0].nameIdentifier.@schemeURI"));
        assertEquals("Bob", XmlPath.from(xml).getString("resource.creators.creator[1].creatorName"));
        assertEquals("Harvard University", XmlPath.from(xml).getString("resource.creators.creator[2].creatorName"));
        assertEquals("https://ror.org/03vek6s52", XmlPath.from(xml).getString("resource.creators.creator[2].nameIdentifier"));
        assertEquals("ROR", XmlPath.from(xml).getString("resource.creators.creator[2].nameIdentifier.@nameIdentifierScheme"));
        assertEquals("https://ror.org", XmlPath.from(xml).getString("resource.creators.creator[2].nameIdentifier.@schemeURI"));
        assertEquals("Qualitative Data Repository", XmlPath.from(xml).getString("resource.creators.creator[3].creatorName"));
        //Test when URL form was used
        assertEquals("https://ror.org/014trz974", XmlPath.from(xml).getString("resource.creators.creator[3].nameIdentifier"));
        assertEquals("ROR", XmlPath.from(xml).getString("resource.creators.creator[3].nameIdentifier.@nameIdentifierScheme"));
        assertEquals("https://ror.org", XmlPath.from(xml).getString("resource.creators.creator[3].nameIdentifier.@schemeURI"));
        assertEquals("Dataverse", XmlPath.from(xml).getString("resource.publisher"));

        dv.setVersionNumber(1L);
        dv.setMinorVersionNumber(0l);
        String xml2 = template.generateXML(d);
        System.out.println("Output from example with v1.0 is " + xml2);
        try {
            StreamSource source = new StreamSource(new StringReader(xml2));
            source.setSystemId("DataCite XML for test dataset");
            assertTrue(XmlValidator.validateXmlSchema(source,
                    new URL("https://schema.datacite.org/meta/kernel-4/metadata.xsd")));
        } catch (SAXException e) {
            System.out.println("Invalid schema: " + e.getMessage());
            fail("Schema validation failed: " + e.getMessage());
        }
    }

    /**
     * This tests a more complete example based off of the dataset-all-defaults
     * file, again checking for conformance of the result with the DataCite XML v4.5
     * schema.
     */
    @Test
    public void testDataCiteXMLCreationAllFields() throws IOException {
        Dataverse collection = new Dataverse();
        collection.setCitationDatasetFieldTypes(new ArrayList<>());
        Dataset d = new Dataset();
        d.setOwner(collection);
        DatasetVersion dv = new DatasetVersion();
        TermsOfUseAndAccess toa = new TermsOfUseAndAccess();
        toa.setTermsOfUse("Some terms");
        dv.setTermsOfUseAndAccess(toa);
        dv.setDataset(d);
        DatasetFieldType primitiveDSFType = new DatasetFieldType(DatasetFieldConstant.title,
                DatasetFieldType.FieldType.TEXT, false);
        DatasetField testDatasetField = new DatasetField();

        dv.setVersionState(VersionState.DRAFT);

        testDatasetField.setDatasetVersion(dv);

        File datasetVersionJson = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-all-defaults.txt");
        String datasetVersionAsJson = new String(Files.readAllBytes(Paths.get(datasetVersionJson.getAbsolutePath())));
        JsonObject datasetJson = JsonUtil.getJsonObject(datasetVersionAsJson);

        GlobalId doi = new GlobalId("doi", datasetJson.getString("authority"), datasetJson.getString("identifier"),
                null, null, null);
        d.setGlobalId(doi);

        List<DatasetField> fields = assertDoesNotThrow(() -> XmlMetadataTemplateTest
                .parseMetadataBlocks(datasetJson.getJsonObject("datasetVersion").getJsonObject("metadataBlocks")));
        dv.setDatasetFields(fields);

        ArrayList<DatasetVersion> dsvs = new ArrayList<>();
        dsvs.add(0, dv);
        d.setVersions(dsvs);
        DatasetType dType = new DatasetType();
        dType.setName(DatasetType.DATASET_TYPE_DATASET);
        d.setDatasetType(dType);
        String xml = DOIDataCiteRegisterService.getMetadataFromDvObject(dv.getDataset().getGlobalId().asString(),
                new DataCitation(dv).getDataCiteMetadata(), dv.getDataset());
        System.out.println("Output from dataset-all-defaults is " + xml);
        try {
            StreamSource source = new StreamSource(new StringReader(xml));
            source.setSystemId("DataCite XML for test dataset");
            assertTrue(XmlValidator.validateXmlSchema(source,
                    new URL("https://schema.datacite.org/meta/kernel-4/metadata.xsd")));
        } catch (SAXException e) {
            System.out.println("Invalid schema: " + e.getMessage());
        }

    }
    
    /**
     * This tests a more complete example based off of the dataset-all-defaults
     * file, again checking for conformance of the result with the DataCite XML v4.5
     * schema.
     */
    @Test
    public void testDataCiteXMLCreationAllFieldsMultipleGeoLocations() throws IOException {
        Dataverse collection = new Dataverse();
        collection.setCitationDatasetFieldTypes(new ArrayList<>());
        Dataset d = new Dataset();
        d.setOwner(collection);
        DatasetVersion dv = new DatasetVersion();
        TermsOfUseAndAccess toa = new TermsOfUseAndAccess();
        toa.setTermsOfUse("Some terms");
        dv.setTermsOfUseAndAccess(toa);
        dv.setDataset(d);
        DatasetFieldType primitiveDSFType = new DatasetFieldType(DatasetFieldConstant.title,
                DatasetFieldType.FieldType.TEXT, false);
        DatasetField testDatasetField = new DatasetField();

        dv.setVersionState(VersionState.DRAFT);

        testDatasetField.setDatasetVersion(dv);

        File datasetVersionJson = new File("src/test/java/edu/harvard/iq/dataverse/export/dataset-all-defaults-multiple-geo.txt");
        String datasetVersionAsJson = new String(Files.readAllBytes(Paths.get(datasetVersionJson.getAbsolutePath())));
        JsonObject datasetJson = JsonUtil.getJsonObject(datasetVersionAsJson);

        GlobalId doi = new GlobalId("doi", datasetJson.getString("authority"), datasetJson.getString("identifier"),
                null, null, null);
        d.setGlobalId(doi);

        List<DatasetField> fields = assertDoesNotThrow(() -> XmlMetadataTemplateTest
                .parseMetadataBlocks(datasetJson.getJsonObject("datasetVersion").getJsonObject("metadataBlocks")));
        dv.setDatasetFields(fields);
        
        JsonValue jsonValueFields = datasetJson.getJsonObject("datasetVersion").getJsonObject("metadataBlocks").getJsonObject("citation").get("fields");
        
        for(JsonValue jsonValue : jsonValueFields.asJsonArray()) {
            JsonObject jsonObject = jsonValue.asJsonObject();
            if (jsonObject.getString("typeName").equals("productionPlace")) {
                assertEquals(jsonObject.get("value").asJsonArray().size(),2);
            }
        }
        
        ArrayList<DatasetVersion> dsvs = new ArrayList<>();
        dsvs.add(0, dv);
        d.setVersions(dsvs);
        DatasetType dType = new DatasetType();
        dType.setName(DatasetType.DATASET_TYPE_DATASET);
        d.setDatasetType(dType);
        String xml = DOIDataCiteRegisterService.getMetadataFromDvObject(dv.getDataset().getGlobalId().asString(),
                new DataCitation(dv).getDataCiteMetadata(), dv.getDataset());
        System.out.println("Output from dataset-all-defaults-multiple-geo is " + xml);
        try {
            StreamSource source = new StreamSource(new StringReader(xml));
            source.setSystemId("DataCite XML for test dataset");
            assertTrue(XmlValidator.validateXmlSchema(source,
                    new URL("https://schema.datacite.org/meta/kernel-4/metadata.xsd")));
        } catch (SAXException e) {
            System.out.println("Invalid schema: " + e.getMessage());
        }

    }

    /**
     * Mock Utility Methods - These methods support importing DatasetFields from the
     * Dataverse JSON export format. They assume that any DatasetFieldType
     * referenced exists, that any Controlled Vocabulary value exists, etc. which
     * avoids having to do database lookups or read metadatablock tsv files. They
     * are derived from the JsonParser methods of the same names with any db
     * references and DatasetFieldType-related error checking removed.
     */
    public static List<DatasetField> parseMetadataBlocks(JsonObject json) throws JsonParseException {

        Map<String, DatasetFieldType> existingTypes = new HashMap<>();

        Set<String> keys = json.keySet();
        List<DatasetField> fields = new LinkedList<>();

        for (String blockName : keys) {
            MetadataBlock block = new MetadataBlock();
            block.setName(blockName);
            JsonObject blockJson = json.getJsonObject(blockName);
            JsonArray fieldsJson = blockJson.getJsonArray("fields");
            fields.addAll(parseFieldsFromArray(fieldsJson, true, block, existingTypes));
        }
        return fields;
    }

    private static List<DatasetField> parseFieldsFromArray(JsonArray fieldsArray, Boolean testType, MetadataBlock block,
            Map<String, DatasetFieldType> existingTypes) throws JsonParseException {
        List<DatasetField> fields = new LinkedList<>();
        for (JsonObject fieldJson : fieldsArray.getValuesAs(JsonObject.class)) {

            DatasetField field = parseField(fieldJson, testType, block, existingTypes);
            if (field != null) {
                fields.add(field);
            }

        }
        return fields;

    }

    public static DatasetField parseField(JsonObject json, Boolean testType, MetadataBlock block,
            Map<String, DatasetFieldType> existingTypes) throws JsonParseException {
        if (json == null) {
            return null;
        }

        DatasetField ret = new DatasetField();
        String fieldName = json.getString("typeName", "");
        String typeClass = json.getString("typeClass", "");
        if (!existingTypes.containsKey(fieldName)) {
            boolean multiple = json.getBoolean("multiple");
            DatasetFieldType fieldType = new DatasetFieldType();
            fieldType.setName(fieldName);
            fieldType.setAllowMultiples(multiple);
            fieldType.setAllowControlledVocabulary(typeClass.equals("controlledVocabulary"));
            fieldType.setFieldType(FieldType.TEXT);
            fieldType.setMetadataBlock(block);
            fieldType.setChildDatasetFieldTypes(new ArrayList<>());
            existingTypes.put(fieldName, fieldType);
        }
        DatasetFieldType type = existingTypes.get(fieldName);
        ret.setDatasetFieldType(type);

        if (typeClass.equals("compound")) {
            parseCompoundValue(ret, type, json, testType, block, existingTypes);
        } else if (type.isControlledVocabulary()) {
            parseControlledVocabularyValue(ret, type, json);
        } else {
            parsePrimitiveValue(ret, type, json);
        }

        return ret;
    }

    public static void parseCompoundValue(DatasetField dsf, DatasetFieldType compoundType, JsonObject json,
            Boolean testType, MetadataBlock block, Map<String, DatasetFieldType> existingTypes)
            throws JsonParseException {
        List<ControlledVocabularyException> vocabExceptions = new ArrayList<>();
        List<DatasetFieldCompoundValue> vals = new LinkedList<>();
        if (compoundType.isAllowMultiples()) {
            int order = 0;
            try {
                json.getJsonArray("value").getValuesAs(JsonObject.class);
            } catch (ClassCastException cce) {
                throw new JsonParseException("Invalid values submitted for " + compoundType.getName()
                        + ". It should be an array of values.");
            }
            for (JsonObject obj : json.getJsonArray("value").getValuesAs(JsonObject.class)) {
                DatasetFieldCompoundValue cv = new DatasetFieldCompoundValue();
                List<DatasetField> fields = new LinkedList<>();
                for (String fieldName : obj.keySet()) {
                    JsonObject childFieldJson = obj.getJsonObject(fieldName);
                    DatasetField f = null;
                    try {
                        f = parseField(childFieldJson, testType, block, existingTypes);
                    } catch (ControlledVocabularyException ex) {
                        vocabExceptions.add(ex);
                    }

                    if (f != null) {
                        f.setParentDatasetFieldCompoundValue(cv);
                        fields.add(f);
                    }
                }
                if (!fields.isEmpty()) {
                    cv.setChildDatasetFields(fields);
                    cv.setDisplayOrder(order);
                    vals.add(cv);
                }
                order++;
            }

        } else {

            DatasetFieldCompoundValue cv = new DatasetFieldCompoundValue();
            List<DatasetField> fields = new LinkedList<>();
            JsonObject value = json.getJsonObject("value");
            for (String key : value.keySet()) {
                JsonObject childFieldJson = value.getJsonObject(key);
                DatasetField f = null;
                try {
                    f = parseField(childFieldJson, testType, block, existingTypes);
                } catch (ControlledVocabularyException ex) {
                    vocabExceptions.add(ex);
                }
                if (f != null) {
                    f.setParentDatasetFieldCompoundValue(cv);
                    fields.add(f);
                }
            }
            if (!fields.isEmpty()) {
                cv.setChildDatasetFields(fields);
                vals.add(cv);
            }

        }
        if (!vocabExceptions.isEmpty()) {
            throw new CompoundVocabularyException("Invalid controlled vocabulary in compound field ", vocabExceptions,
                    vals);
        }

        for (DatasetFieldCompoundValue dsfcv : vals) {
            dsfcv.setParentDatasetField(dsf);
        }
        dsf.setDatasetFieldCompoundValues(vals);
    }

    public static void parsePrimitiveValue(DatasetField dsf, DatasetFieldType dft, JsonObject json)
            throws JsonParseException {
        List<DatasetFieldValue> vals = new LinkedList<>();
        if (dft.isAllowMultiples()) {
            try {
                json.getJsonArray("value").getValuesAs(JsonObject.class);
            } catch (ClassCastException cce) {
                throw new JsonParseException(
                        "Invalid values submitted for " + dft.getName() + ". It should be an array of values.");
            }
            for (JsonString val : json.getJsonArray("value").getValuesAs(JsonString.class)) {
                DatasetFieldValue datasetFieldValue = new DatasetFieldValue(dsf);
                datasetFieldValue.setDisplayOrder(vals.size() - 1);
                datasetFieldValue.setValue(val.getString().trim());
                vals.add(datasetFieldValue);
            }

        } else {
            try {
                json.getString("value");
            } catch (ClassCastException cce) {
                throw new JsonParseException(
                        "Invalid value submitted for " + dft.getName() + ". It should be a single value.");
            }
            DatasetFieldValue datasetFieldValue = new DatasetFieldValue();
            datasetFieldValue.setValue(json.getString("value", "").trim());
            datasetFieldValue.setDatasetField(dsf);
            vals.add(datasetFieldValue);
        }

        dsf.setDatasetFieldValues(vals);
    }

    public static void parseControlledVocabularyValue(DatasetField dsf, DatasetFieldType cvvType, JsonObject json)
            throws JsonParseException {
        List<ControlledVocabularyValue> vals = new LinkedList<>();
        try {
            if (cvvType.isAllowMultiples()) {
                try {
                    json.getJsonArray("value").getValuesAs(JsonObject.class);
                } catch (ClassCastException cce) {
                    throw new JsonParseException(
                            "Invalid values submitted for " + cvvType.getName() + ". It should be an array of values.");
                }
                for (JsonString strVal : json.getJsonArray("value").getValuesAs(JsonString.class)) {
                    String strValue = strVal.getString();
                    ControlledVocabularyValue cvv = new ControlledVocabularyValue();
                    cvv.setDatasetFieldType(cvvType);
                    cvv.setStrValue(strVal.getString());
                    vals.add(cvv);
                }

            } else {
                try {
                    json.getString("value");
                } catch (ClassCastException cce) {
                    throw new JsonParseException(
                            "Invalid value submitted for " + cvvType.getName() + ". It should be a single value.");
                }
                String strValue = json.getString("value", "");
                ControlledVocabularyValue cvv = new ControlledVocabularyValue();
                cvv.setDatasetFieldType(cvvType);
                cvv.setStrValue(strValue);
                vals.add(cvv);
            }
        } catch (ClassCastException cce) {
            throw new JsonParseException("Invalid values submitted for " + cvvType.getName());
        }

        dsf.setControlledVocabularyValues(vals);
    }

}
