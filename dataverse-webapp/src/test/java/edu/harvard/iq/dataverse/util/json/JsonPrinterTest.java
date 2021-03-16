package edu.harvard.iq.dataverse.util.json;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.citation.CitationFactory;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.common.Util;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileCategory;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileTag;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion.VersionState;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.PrivateUrlUser;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignee;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import org.junit.Before;
import org.junit.Test;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;

public class JsonPrinterTest {

    private MetadataBlock citationBlock;
    private DatasetFieldType contactFieldType;
    private DatasetFieldType contactEmailFieldType;
    private DatasetFieldType contactNameFieldType;
    private DatasetFieldType contactAffiliationFieldType;

    private JsonPrinter jsonPrinter = new JsonPrinter(new CitationFactory());

    @Before
    public void setUp() {

        contactFieldType = new DatasetFieldType("datasetContact", FieldType.TEXT, true);
        contactEmailFieldType = new DatasetFieldType(DatasetFieldConstant.datasetContactEmail, FieldType.EMAIL, false);
        contactNameFieldType = new DatasetFieldType("datasetContactName", FieldType.TEXT, false);
        contactAffiliationFieldType = new DatasetFieldType("datasetContactAffiliation", FieldType.TEXT, false);
        List<DatasetFieldType> contactChildrenFieldTypes = Lists.newArrayList(contactEmailFieldType, contactNameFieldType, contactAffiliationFieldType);
        for (DatasetFieldType t : contactChildrenFieldTypes) {
            t.setParentDatasetFieldType(contactFieldType);
        }
        contactFieldType.setChildDatasetFieldTypes(contactChildrenFieldTypes);

        citationBlock = new MetadataBlock();
        citationBlock.setName("citation");
        citationBlock.setDatasetFieldTypes(Lists.newArrayList(contactFieldType));

        contactFieldType.setMetadataBlock(citationBlock);
    }

    @Test
    public void testJson_RoleAssignment() {
        //given
        DataverseRole aRole = new DataverseRole();
        RoleAssignee anAssignee = new PrivateUrlUser(42);
        Dataset dataset = new Dataset();
        dataset.setId(123L);
        String privateUrlToken = "e1d53cf6-794a-457a-9709-7c07629a8267";
        RoleAssignment ra = new RoleAssignment(aRole, anAssignee, dataset, privateUrlToken);

        //when
        JsonObjectBuilder job = jsonPrinter.json(ra);
        assertNotNull(job);
        JsonObject jsonObject = job.build();

        //then
        assertEquals("#42", jsonObject.getString("assignee"));
        assertEquals(123, jsonObject.getInt("definitionPointId"));
        assertEquals("e1d53cf6-794a-457a-9709-7c07629a8267", jsonObject.getString("privateUrlToken"));
    }

    @Test
    public void testJson_PrivateUrl() {
        //given
        DataverseRole aRole = new DataverseRole();
        RoleAssignee anAssignee = new PrivateUrlUser(42);
        Dataset dataset = new Dataset();
        String privateUrlToken = "e1d53cf6-794a-457a-9709-7c07629a8267";
        RoleAssignment ra = new RoleAssignment(aRole, anAssignee, dataset, privateUrlToken);
        String dataverseSiteUrl = "https://dataverse.example.edu";
        PrivateUrl privateUrl = new PrivateUrl(ra, dataset, dataverseSiteUrl);

        //when
        JsonObjectBuilder job = jsonPrinter.json(privateUrl);
        assertNotNull(job);
        JsonObject jsonObject = job.build();

        //then
        assertEquals("e1d53cf6-794a-457a-9709-7c07629a8267", jsonObject.getString("token"));
        assertEquals("https://dataverse.example.edu/privateurl.xhtml?token=e1d53cf6-794a-457a-9709-7c07629a8267", jsonObject.getString("link"));
        assertEquals("e1d53cf6-794a-457a-9709-7c07629a8267", jsonObject.getJsonObject("roleAssignment").getString("privateUrlToken"));
        assertEquals("#42", jsonObject.getJsonObject("roleAssignment").getString("assignee"));
    }

    @Test
    public void testGetFileCategories() {
        //given
        FileMetadata fmd = MocksFactory.makeFileMetadata(10L, "", 0);
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

        //when
        JsonObjectBuilder job = jsonPrinter.json(fmd);
        assertNotNull(job);
        JsonObject jsonObject = job.build();

        //then
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
        //given
        List<DatasetField> fields = new ArrayList<>();
        DatasetField datasetContactField = new DatasetField();
        datasetContactField.setDatasetFieldType(contactFieldType);
        datasetContactField.setDatasetFieldsChildren(Arrays.asList(
                constructPrimitive(contactEmailFieldType, "foo@bar.com"),
                constructPrimitive(contactNameFieldType, "Foo Bar"),
                constructPrimitive(contactAffiliationFieldType, "Bar University")
        ));
        fields.add(datasetContactField);

        //when
        JsonObject jsonObject = jsonPrinter.json(citationBlock, fields, false).build();
        assertNotNull(jsonObject);

        //then
        assertEquals("Foo Bar", jsonObject.getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactName").getString("value"));
        assertEquals("Bar University", jsonObject.getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactAffiliation").getString("value"));
        assertEquals("foo@bar.com", jsonObject.getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactEmail").getString("value"));

        JsonObject byBlocks = jsonPrinter.jsonByBlocks(fields, false).build();

        System.out.println("byBlocks: " + JsonUtil.prettyPrint(byBlocks.toString()));
        assertEquals("Foo Bar", byBlocks.getJsonObject("citation").getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactName").getString("value"));
        assertEquals("Bar University", byBlocks.getJsonObject("citation").getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactAffiliation").getString("value"));
        assertEquals("foo@bar.com", byBlocks.getJsonObject("citation").getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactEmail").getString("value"));

    }

    @Test
    public void testDatasetContactWithPrivacy() {
        //given
        List<DatasetField> fields = new ArrayList<>();
        DatasetField datasetContactField = new DatasetField();
        datasetContactField.setDatasetFieldType(contactFieldType);

        datasetContactField.setDatasetFieldsChildren(Arrays.asList(
                constructPrimitive(contactEmailFieldType, "foo@bar.com"),
                constructPrimitive(contactNameFieldType, "Foo Bar"),
                constructPrimitive(contactAffiliationFieldType, "Bar University")
        ));
        fields.add(datasetContactField);

        //when
        JsonObject jsonObject = jsonPrinter.json(citationBlock, fields, true).build();
        assertNotNull(jsonObject);

        //then
        assertEquals("Foo Bar", jsonObject.getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactName").getString("value"));
        assertEquals("Bar University", jsonObject.getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactAffiliation").getString("value"));
        assertNull(jsonObject.getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject(
                "datasetContactEmail"));

        JsonObject byBlocks = jsonPrinter.jsonByBlocks(fields, true).build();

        System.out.println("byBlocks: " + JsonUtil.prettyPrint(byBlocks.toString()));
        assertEquals("Foo Bar", byBlocks.getJsonObject("citation").getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactName").getString("value"));
        assertEquals("Bar University", byBlocks.getJsonObject("citation").getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(0).getJsonObject("datasetContactAffiliation").getString("value"));
        assertNull(byBlocks.getJsonObject("citation").getJsonArray("fields").getJsonObject(0).getJsonArray("value").getJsonObject(
                0).getJsonObject("datasetContactEmail"));

    }

    @Test
    public void shouldIncludeEmbargoDate() throws ParseException {
        // given
        Dataset dataset = createDatasetForTests();
        String expectedDate = "2020-01-17";
        Date embargoDate = Util.getDateFormat().parse(expectedDate);
        dataset.setEmbargoDate(embargoDate);

        // when
        JsonObject jsonObject = jsonPrinter.json(dataset).build();

        // then
        assertThat("Should include embargo date", jsonObject.getString("embargoDate"), is(expectedDate));
    }

    @Test
    public void shouldNotIncludeEmptyEmbargoDate() {
        // given
        Dataset datasetWithNoEmbargoDate = createDatasetForTests();

        // when
        JsonObject jsonObject = jsonPrinter.json(datasetWithNoEmbargoDate).build();

        // then
        assertThat("Should not include embargo date if it's null", jsonObject.get("embargoDate"), nullValue());
    }

    @Test
    public void shouldProperlySetWhetherEmbargoIsActive() {
        // given
        Dataset dsWithActiveEmbargo = createDatasetForTests();
        Date dateInFuture = Date.from(LocalDate.now()
                .plusDays(7L)
                .atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant());
        dsWithActiveEmbargo.setEmbargoDate(dateInFuture);

        Dataset dsWithInactiveEmbargo = createDatasetForTests();
        Date dateInPast = Date.from(LocalDate.now()
                .minusDays(7L)
                .atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant());
        dsWithInactiveEmbargo.setEmbargoDate(dateInPast);

        // when
        JsonObject active = jsonPrinter.json(dsWithActiveEmbargo).build();
        JsonObject inactive = jsonPrinter.json(dsWithInactiveEmbargo).build();

        // then
        assertThat("Object with embargo in future should have embargo flag set to TRUE",
                active.getBoolean("embargoActive"), is(true));
        assertThat("Object with embargo in past should have embargo flag set to FALSE",
                inactive.getBoolean("embargoActive"), is(false));
    }

    @Test
    public void shouldNotIncludeFilesWhenDatasetIsUnderEmbargo() {
        // given
        Dataset datasetWithActiveEmbargo = createDatasetForTests();
        datasetWithActiveEmbargo.setEmbargoDate(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));

        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setLabel("file.txt");

        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setVersionState(VersionState.RELEASED);
        datasetVersion.setDataset(datasetWithActiveEmbargo);
        datasetVersion.addFileMetadata(new FileMetadata());

        // when
        JsonObject jsonObject = jsonPrinter.json(datasetVersion, false).build();

        // then
        assertTrue(jsonObject.getJsonArray("files").isEmpty());
    }

    // -------------------- PRIVATE --------------------

    private Dataset createDatasetForTests() {
        Dataset dataset = new Dataset();
        dataset.setId(234L);
        dataset.setOwner(new Dataverse());
        dataset.setIdentifier("identifier");
        dataset.setProtocol("doi");
        return dataset;
    }

    private DatasetField constructPrimitive(DatasetFieldType datasetFieldType, String value) {
        DatasetField retVal = new DatasetField();
        retVal.setDatasetFieldType(datasetFieldType);
        retVal.setFieldValue(value);
        return retVal;
    }
}
