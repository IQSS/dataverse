package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileCategory;
import edu.harvard.iq.dataverse.DataFileTag;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldConstant;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldType.FieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.MetadataBlock;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JsonPrinterTest {

    // Centralize JsonParserTest.MockDatasetFieldSvc? See also https://github.com/IQSS/dataverse/issues/3413 and https://github.com/IQSS/dataverse/issues/3777
    JsonParserTest.MockDatasetFieldSvc datasetFieldTypeSvc = null;

    @Before
    public void setUp() {
        datasetFieldTypeSvc = new JsonParserTest.MockDatasetFieldSvc();

        DatasetFieldType titleType = datasetFieldTypeSvc.add(new DatasetFieldType("title", FieldType.TEXTBOX, false));
        DatasetFieldType authorType = datasetFieldTypeSvc.add(new DatasetFieldType("author", FieldType.TEXT, true));
        Set<DatasetFieldType> authorChildTypes = new HashSet<>();
        authorChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("authorName", FieldType.TEXT, false)));
        authorChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("authorAffiliation", FieldType.TEXT, false)));
        authorChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("authorIdentifier", FieldType.TEXT, false)));
        DatasetFieldType authorIdentifierSchemeType = datasetFieldTypeSvc.add(new DatasetFieldType("authorIdentifierScheme", FieldType.TEXT, false));
        authorIdentifierSchemeType.setAllowControlledVocabulary(true);
        authorIdentifierSchemeType.setControlledVocabularyValues(Arrays.asList(
                // FIXME: Why aren't these enforced? Should be ORCID, etc.
                new ControlledVocabularyValue(1l, "foo", authorIdentifierSchemeType),
                new ControlledVocabularyValue(2l, "bar", authorIdentifierSchemeType),
                new ControlledVocabularyValue(3l, "baz", authorIdentifierSchemeType)
        ));
        authorChildTypes.add(datasetFieldTypeSvc.add(authorIdentifierSchemeType));
        for (DatasetFieldType t : authorChildTypes) {
            t.setParentDatasetFieldType(authorType);
        }
        authorType.setChildDatasetFieldTypes(authorChildTypes);

        DatasetFieldType datasetContactType = datasetFieldTypeSvc.add(new DatasetFieldType("datasetContact", FieldType.TEXT, true));
        Set<DatasetFieldType> datasetContactTypes = new HashSet<>();
        datasetContactTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType(DatasetFieldConstant.datasetContactEmail, FieldType.EMAIL, false)));
        datasetContactTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("datasetContactName", FieldType.TEXT, false)));
        datasetContactTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("datasetContactAffiliation", FieldType.TEXT, false)));
        for (DatasetFieldType t : datasetContactTypes) {
            t.setParentDatasetFieldType(datasetContactType);
        }
        datasetContactType.setChildDatasetFieldTypes(datasetContactTypes);

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
    public void testJson_RoleAssignment() {
        DataverseRole aRole = new DataverseRole();
        PrivateUrlUser privateUrlUserIn = new PrivateUrlUser(42);
        RoleAssignee anAssignee = privateUrlUserIn;
        Dataset dataset = new Dataset();
        dataset.setId(123l);
        String privateUrlToken = "e1d53cf6-794a-457a-9709-7c07629a8267";
        RoleAssignment ra = new RoleAssignment(aRole, anAssignee, dataset, privateUrlToken);
        JsonObjectBuilder job = JsonPrinter.json(ra);
        assertNotNull(job);
        JsonObject jsonObject = job.build();
        assertEquals("#42", jsonObject.getString("assignee"));
        assertEquals(123, jsonObject.getInt("definitionPointId"));
        assertEquals("e1d53cf6-794a-457a-9709-7c07629a8267", jsonObject.getString("privateUrlToken"));
    }

    @Test
    public void testJson_PrivateUrl() {
        DataverseRole aRole = new DataverseRole();
        PrivateUrlUser privateUrlUserIn = new PrivateUrlUser(42);
        RoleAssignee anAssignee = privateUrlUserIn;
        Dataset dataset = new Dataset();
        String privateUrlToken = "e1d53cf6-794a-457a-9709-7c07629a8267";
        RoleAssignment ra = new RoleAssignment(aRole, anAssignee, dataset, privateUrlToken);
        String dataverseSiteUrl = "https://dataverse.example.edu";
        PrivateUrl privateUrl = new PrivateUrl(ra, dataset, dataverseSiteUrl);
        JsonObjectBuilder job = JsonPrinter.json(privateUrl);
        assertNotNull(job);
        JsonObject jsonObject = job.build();
        assertEquals("e1d53cf6-794a-457a-9709-7c07629a8267", jsonObject.getString("token"));
        assertEquals("https://dataverse.example.edu/privateurl.xhtml?token=e1d53cf6-794a-457a-9709-7c07629a8267", jsonObject.getString("link"));
        assertEquals("e1d53cf6-794a-457a-9709-7c07629a8267", jsonObject.getJsonObject("roleAssignment").getString("privateUrlToken"));
        assertEquals("#42", jsonObject.getJsonObject("roleAssignment").getString("assignee"));
    }

    @Test
    public void testGetFileCategories() {
        FileMetadata fmd = new FileMetadata();
        DatasetVersion dsVersion = new DatasetVersion();
        DataFile dataFile = new DataFile();
        dataFile.setProtocol("doi");
        dataFile.setIdentifier("ABC123");
        dataFile.setAuthority("10.5072/FK2");
        List<DataFileTag> dataFileTags = new ArrayList<>();
        DataFileTag tag = new DataFileTag();
        tag.setTypeByLabel("Survey");
        dataFileTags.add(tag);
        dataFile.setTags(dataFileTags);
        fmd.setDatasetVersion(dsVersion);
        fmd.setDataFile(dataFile);
        List<DataFileCategory> fileCategories = new ArrayList<>();
        DataFileCategory dataFileCategory = new DataFileCategory();
        dataFileCategory.setName("Data");
        fileCategories.add(dataFileCategory);
        fmd.setCategories(fileCategories);
        JsonObjectBuilder job = JsonPrinter.json(fmd);
        assertNotNull(job);
        JsonObject jsonObject = job.build();
        System.out.println("json: " + jsonObject);
        assertEquals("", jsonObject.getString("description"));
        assertEquals("", jsonObject.getString("label"));
        assertEquals("Data", jsonObject.getJsonArray("categories").getString(0));
        assertEquals("", jsonObject.getJsonObject("dataFile").getString("filename"));
        assertEquals(-1, jsonObject.getJsonObject("dataFile").getInt("filesize"));
        assertEquals(-1, jsonObject.getJsonObject("dataFile").getInt("rootDataFileId"));
        assertEquals("Survey", jsonObject.getJsonObject("dataFile").getJsonArray("tabularTags").getString(0));
    }

    @Test
    public void testDatasetContactOutOfBoxNoPrivacy() {
        MetadataBlock block = new MetadataBlock();
        block.setName("citation");
        List<DatasetField> fields = new ArrayList<>();
        DatasetField datasetContactField = new DatasetField();
        DatasetFieldType datasetContactDatasetFieldType = datasetFieldTypeSvc.findByName("datasetContact");
        datasetContactDatasetFieldType.setMetadataBlock(block);
        datasetContactField.setDatasetFieldType(datasetContactDatasetFieldType);
        List<DatasetFieldCompoundValue> vals = new LinkedList<>();
        DatasetFieldCompoundValue val = new DatasetFieldCompoundValue();
        val.setParentDatasetField(datasetContactField);
        val.setChildDatasetFields(Arrays.asList(
                constructPrimitive("datasetContactEmail", "foo@bar.com"),
                constructPrimitive("datasetContactName", "Foo Bar"),
                constructPrimitive("datasetContactAffiliation", "Bar University")
        ));
        vals.add(val);
        datasetContactField.setDatasetFieldCompoundValues(vals);
        fields.add(datasetContactField);

        SettingsServiceBean nullServiceBean = null;
        JsonPrinter.setSettingsService(nullServiceBean);
        
        JsonObject jsonObject = JsonPrinter.json(block, fields).build();
        assertNotNull(jsonObject);

        System.out.println("json: " + JsonUtil.prettyPrint(jsonObject.toString()));

        assertEquals("Foo Bar", jsonObject.getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactName").getString("value"));
        assertEquals("Bar University", jsonObject.getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactAffiliation").getString("value"));
        assertEquals("foo@bar.com", jsonObject.getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactEmail").getString("value"));

        JsonObject byBlocks = JsonPrinter.jsonByBlocks(fields).build();

        System.out.println("byBlocks: " + JsonUtil.prettyPrint(byBlocks.toString()));
        assertEquals("Foo Bar", byBlocks.getJsonObject("citation").getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactName").getString("value"));
        assertEquals("Bar University", byBlocks.getJsonObject("citation").getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactAffiliation").getString("value"));
        assertEquals("foo@bar.com", byBlocks.getJsonObject("citation").getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactEmail").getString("value"));

    }

    @Test
    public void testDatasetContactWithPrivacy() {
        MetadataBlock block = new MetadataBlock();
        block.setName("citation");
        List<DatasetField> fields = new ArrayList<>();
        DatasetField datasetContactField = new DatasetField();
        DatasetFieldType datasetContactDatasetFieldType = datasetFieldTypeSvc.findByName("datasetContact");
        datasetContactDatasetFieldType.setMetadataBlock(block);
        datasetContactField.setDatasetFieldType(datasetContactDatasetFieldType);
        List<DatasetFieldCompoundValue> vals = new LinkedList<>();
        DatasetFieldCompoundValue val = new DatasetFieldCompoundValue();
        val.setParentDatasetField(datasetContactField);
        val.setChildDatasetFields(Arrays.asList(
                constructPrimitive("datasetContactEmail", "foo@bar.com"),
                constructPrimitive("datasetContactName", "Foo Bar"),
                constructPrimitive("datasetContactAffiliation", "Bar University")
        ));
        vals.add(val);
        datasetContactField.setDatasetFieldCompoundValues(vals);
        fields.add(datasetContactField);

        JsonPrinter.setSettingsService(new MockSettingsSvc());

        JsonObject jsonObject = JsonPrinter.json(block, fields).build();
        assertNotNull(jsonObject);

        System.out.println("json: " + JsonUtil.prettyPrint(jsonObject.toString()));

        assertEquals("Foo Bar", jsonObject.getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactName").getString("value"));
        assertEquals("Bar University", jsonObject.getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactAffiliation").getString("value"));
        assertEquals(null, jsonObject.getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactEmail"));

        JsonObject byBlocks = JsonPrinter.jsonByBlocks(fields).build();

        System.out.println("byBlocks: " + JsonUtil.prettyPrint(byBlocks.toString()));
        assertEquals("Foo Bar", byBlocks.getJsonObject("citation").getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactName").getString("value"));
        assertEquals("Bar University", byBlocks.getJsonObject("citation").getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactAffiliation").getString("value"));
        assertEquals(null, byBlocks.getJsonObject("citation").getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactEmail"));

    }

    DatasetField constructPrimitive(String datasetFieldTypeName, String value) {
        DatasetField retVal = new DatasetField();
        retVal.setDatasetFieldType(datasetFieldTypeSvc.findByName(datasetFieldTypeName));
        retVal.setDatasetFieldValues(Collections.singletonList(new DatasetFieldValue(retVal, value)));
        return retVal;
    }

    private static class MockSettingsSvc extends SettingsServiceBean {

        @Override
        public boolean isTrueForKey(SettingsServiceBean.Key key, boolean defaultValue) {
            switch (key) {
                case ExcludeEmailFromExport:
                    return true;
                default:
                    return false;
            }
        }

    }

}
