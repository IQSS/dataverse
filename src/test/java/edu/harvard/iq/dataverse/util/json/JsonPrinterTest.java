package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.DatasetFieldType.FieldType;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.dataset.DatasetType;
import edu.harvard.iq.dataverse.mocks.MockDatasetFieldSvc;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.UserNotification.Type;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;

import edu.harvard.iq.dataverse.util.BundleUtil;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class JsonPrinterTest {

    MockDatasetFieldSvc datasetFieldTypeSvc = null;

    @BeforeEach
    public void setUp() {
        datasetFieldTypeSvc = new MockDatasetFieldSvc();
        datasetFieldTypeSvc.setMetadataBlock("citation");

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
        assertEquals(PrivateUrlUser.PREFIX + "42", jsonObject.getString("assignee"));
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
        assertEquals("https://dataverse.example.edu/previewurl.xhtml?token=e1d53cf6-794a-457a-9709-7c07629a8267", jsonObject.getString("link"));
        assertEquals("e1d53cf6-794a-457a-9709-7c07629a8267", jsonObject.getJsonObject("roleAssignment").getString("privateUrlToken"));
        assertEquals(PrivateUrlUser.PREFIX + "42", jsonObject.getJsonObject("roleAssignment").getString("assignee"));
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
        DataTable dt = new DataTable();
        dataFile.setDataTable(dt);
        dataFile.getDataTable().setOriginalFileName("50by1000.dta");
        Embargo emb = new Embargo();
        emb.setDateAvailable(LocalDate.parse("2021-12-03"));
        emb.setReason("Some reason");
        dataFile.setEmbargo(emb);
        dsVersion.setId(Long.MIN_VALUE);
        fmd.setDatasetVersion(dsVersion);
        fmd.setDataFile(dataFile);
        fmd.setVersion(Long.MIN_VALUE);
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
        assertEquals("50by1000.dta", jsonObject.getJsonObject("dataFile").getString("originalFileName"));
        assertEquals("Survey", jsonObject.getJsonObject("dataFile").getJsonArray("tabularTags").getString(0));
        assertEquals("2021-12-03", jsonObject.getJsonObject("dataFile").getJsonObject("embargo").getString("dateAvailable"));
        assertEquals("Some reason", jsonObject.getJsonObject("dataFile").getJsonObject("embargo").getString("reason"));
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
        DatasetFieldServiceBean nullDFServiceBean = null;
        DataverseFieldTypeInputLevelServiceBean nullDFILServiceBean = null;
        JsonPrinter.injectSettingsService(nullServiceBean, nullDFServiceBean, nullDFILServiceBean);

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

        DatasetFieldServiceBean nullDFServiceBean = null;
        DataverseFieldTypeInputLevelServiceBean nullDFILServiceBean = null;
        JsonPrinter.injectSettingsService(new MockSettingsSvc(), nullDFServiceBean, nullDFILServiceBean);

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

    @Test
    public void testDatasetFieldTypesWithChildren() {
        MetadataBlock block = new MetadataBlock();
        block.setId(0L);
        block.setName("citation");
        long id = 0L;
        // create datasetFieldTypes
        List<DatasetFieldType> datasetFieldTypes = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            DatasetFieldType dft = new DatasetFieldType();
            dft.setId(id++);
            dft.setDisplayOrder(i);
            dft.setMetadataBlock(block);
            dft.setFieldType(FieldType.TEXT);
            dft.setName("subType" + dft.getId());
            dft.setTitle(dft.getName());
            dft.setChildDatasetFieldTypes(Lists.emptyList());
            datasetFieldTypes.add(dft);
        }
        // add DatasetFieldType as children to another DatasetFieldType to test the suppression of duplicate data
        // adding 3 and 4 as children of 2
        datasetFieldTypes.get(3).setParentDatasetFieldType(datasetFieldTypes.get(2));
        datasetFieldTypes.get(4).setParentDatasetFieldType(datasetFieldTypes.get(2));
        datasetFieldTypes.get(2).setChildDatasetFieldTypes(List.of(datasetFieldTypes.get(3), datasetFieldTypes.get(4)));
        // adding 6 as child of 9
        datasetFieldTypes.get(6).setParentDatasetFieldType(datasetFieldTypes.get(9));
        datasetFieldTypes.get(9).setChildDatasetFieldTypes(List.of(datasetFieldTypes.get(6)));

        block.setDatasetFieldTypes(datasetFieldTypes);

        DatasetFieldServiceBean nullDFServiceBean = null;
        DataverseFieldTypeInputLevelServiceBean nullDFILServiceBean = null;
        JsonPrinter.injectSettingsService(new MockSettingsSvc(), nullDFServiceBean, nullDFILServiceBean);

        JsonObject jsonObject = JsonPrinter.json(block).build();
        assertNotNull(jsonObject);

        System.out.println("json: " + JsonUtil.prettyPrint(jsonObject.toString()));
        assertEquals("subType2 subType3", jsonObject.getJsonObject("fields").getJsonObject("subType2")
                .getJsonObject("childFields").getJsonObject("subType3").getString("displayName"));
        assertEquals("subType2 subType4", jsonObject.getJsonObject("fields").getJsonObject("subType2")
                .getJsonObject("childFields").getJsonObject("subType4").getString("displayName"));
        assertEquals("subType9 subType6", jsonObject.getJsonObject("fields").getJsonObject("subType9")
                .getJsonObject("childFields").getJsonObject("subType6").getString("displayName"));
        assertNull(jsonObject.getJsonObject("fields").getJsonObject("subType3"));
        assertNull(jsonObject.getJsonObject("fields").getJsonObject("subType4"));
        assertNull(jsonObject.getJsonObject("fields").getJsonObject("subType6"));
    }

    @Test
    public void testDataversePrinter() {
        Dataverse dataverse = new Dataverse();
        dataverse.setId(42l);
        dataverse.setAlias("dv42");
        dataverse.setName("Dataverse 42");
        dataverse.setAffiliation("42 Inc.");
        dataverse.setDescription("Description for Dataverse 42.");
        dataverse.setDataverseType(Dataverse.DataverseType.UNCATEGORIZED);
        List<DataverseContact> dataverseContacts = new ArrayList<>();
        dataverseContacts.add(new DataverseContact(dataverse, "dv42@mailinator.com"));
        dataverse.setDataverseContacts(dataverseContacts);
        JsonObjectBuilder job = JsonPrinter.json(dataverse);
        JsonObject jsonObject = job.build();
        assertNotNull(jsonObject);
        System.out.println("json: " + JsonUtil.prettyPrint(jsonObject.toString()));
        assertEquals(42, jsonObject.getInt("id"));
        assertEquals("dv42", jsonObject.getString("alias"));
        assertEquals("Dataverse 42", jsonObject.getString("name"));
        assertEquals("42 Inc.", jsonObject.getString("affiliation"));
        assertEquals(0, jsonObject.getJsonArray("dataverseContacts").getJsonObject(0).getInt("displayOrder"));
        assertEquals("dv42@mailinator.com", jsonObject.getJsonArray("dataverseContacts").getJsonObject(0).getString("contactEmail"));
        assertFalse(jsonObject.getBoolean("permissionRoot"));
        assertEquals("Description for Dataverse 42.", jsonObject.getString("description"));
        assertEquals("UNCATEGORIZED", jsonObject.getString("dataverseType"));
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

    @Test
    public void testEnum() throws JsonParseException {
        JsonArrayBuilder arr = JsonPrinter.enumsToJson(Arrays.asList(Type.REVOKEROLE, Type.ASSIGNROLE));
        Set<String> typesSet = new HashSet<>(arr.build().getValuesAs(JsonString::getString));
        assertTrue(typesSet.size() == 2);
        assertTrue(typesSet.contains("REVOKEROLE"));
        assertTrue(typesSet.contains("ASSIGNROLE"));
    }

    @Test
    public void testMetadataBlockAnonymized() {
        MetadataBlock block = new MetadataBlock();
        block.setName("citation");
        List<DatasetField> fields = new ArrayList<>();
        DatasetField datasetAuthorField = new DatasetField();
        DatasetFieldType datasetAuthorFieldType = datasetFieldTypeSvc.findByName("author");
        datasetAuthorFieldType.setMetadataBlock(block);
        datasetAuthorField.setDatasetFieldType(datasetAuthorFieldType);
        List<DatasetFieldCompoundValue> compoundValues = new LinkedList<>();
        DatasetFieldCompoundValue compoundValue = new DatasetFieldCompoundValue();
        compoundValue.setParentDatasetField(datasetAuthorField);
        compoundValue.setChildDatasetFields(Arrays.asList(
                constructPrimitive("authorName", "Test Author"),
                constructPrimitive("authorAffiliation", "Test Affiliation")
        ));
        compoundValues.add(compoundValue);
        datasetAuthorField.setDatasetFieldCompoundValues(compoundValues);
        fields.add(datasetAuthorField);

        JsonObject actualJsonObject = JsonPrinter.json(block, fields, List.of("author")).build();

        assertNotNull(actualJsonObject);
        JsonObject actualAuthorJsonObject = actualJsonObject.getJsonArray("fields").getJsonObject(0);
        assertEquals(BundleUtil.getStringFromBundle("dataset.anonymized.withheld"), actualAuthorJsonObject.getString("value"));
        assertEquals("primitive", actualAuthorJsonObject.getString("typeClass"));
        assertFalse(actualAuthorJsonObject.getBoolean("multiple"));
    }
    
    @Test
    public void testDatasetWithNondefaultType() {
        String sut = "foobar";
        DatasetType foobar = new DatasetType();
        foobar.setName(sut);
        
        Dataset dataset = new Dataset();
        dataset.setDatasetType(foobar);
        var gid = new GlobalId("doi", "10.5072/FK2", "ABC123", "/", "https://doi.org/", "DataCite");
        dataset.setGlobalId(gid);
        
        DatasetVersion dsv = new DatasetVersion();
        dsv.setDataset(dataset);
        dsv.setVersionState(DatasetVersion.VersionState.DRAFT);
        dsv.setTermsOfUseAndAccess(new TermsOfUseAndAccess());
        
        var jsob = JsonPrinter.json(dsv, false).build();
        String result = jsob.getString("datasetType");
        
        assertNotNull(result);
        assertEquals(sut, result);
    }
}
