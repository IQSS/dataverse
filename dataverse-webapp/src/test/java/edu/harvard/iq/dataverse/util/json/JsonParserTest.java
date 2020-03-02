/*
 *  (C) Michael Bar-Sinai
 */

package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroupProvider;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseTheme.Alignment;
import edu.harvard.iq.dataverse.persistence.group.IpAddress;
import edu.harvard.iq.dataverse.persistence.group.IpAddressRange;
import edu.harvard.iq.dataverse.persistence.group.IpGroup;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import edu.harvard.iq.dataverse.qualifiers.TestBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import io.vavr.API;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author michael
 */
@RunWith(MockitoJUnitRunner.class)
public class JsonParserTest {


    @Mock
    private SettingsServiceBean settingsService;

    MockDatasetFieldSvc datasetFieldTypeSvc = null;
    DatasetFieldType keywordType;
    DatasetFieldType descriptionType;
    DatasetFieldType subjectType;
    DatasetFieldType pubIdType;
    DatasetFieldType compoundSingleType;
    JsonParser sut;

    public JsonParserTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        datasetFieldTypeSvc = new MockDatasetFieldSvc();

        keywordType = datasetFieldTypeSvc.add(new DatasetFieldType("keyword", FieldType.TEXT, true));
        descriptionType = datasetFieldTypeSvc.add(new DatasetFieldType("description", FieldType.TEXTBOX, false));

        subjectType = datasetFieldTypeSvc.add(new DatasetFieldType("subject", FieldType.TEXT, true));
        subjectType.setAllowControlledVocabulary(true);
        subjectType.setControlledVocabularyValues(Arrays.asList(
                new ControlledVocabularyValue(1l, "mgmt", subjectType),
                new ControlledVocabularyValue(2l, "law", subjectType),
                new ControlledVocabularyValue(3l, "cs", subjectType)
        ));

        pubIdType = datasetFieldTypeSvc.add(new DatasetFieldType("publicationIdType", FieldType.TEXT, false));
        pubIdType.setAllowControlledVocabulary(true);
        pubIdType.setControlledVocabularyValues(Arrays.asList(
                new ControlledVocabularyValue(1l, "ark", pubIdType),
                new ControlledVocabularyValue(2l, "doi", pubIdType),
                new ControlledVocabularyValue(3l, "url", pubIdType)
        ));

        compoundSingleType = datasetFieldTypeSvc.add(new DatasetFieldType("coordinate", FieldType.TEXT, true));
        Set<DatasetFieldType> childTypes = new HashSet<>();
        childTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("lat", FieldType.TEXT, false)));
        childTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("lon", FieldType.TEXT, false)));

        for (DatasetFieldType t : childTypes) {
            t.setParentDatasetFieldType(compoundSingleType);
        }
        compoundSingleType.setChildDatasetFieldTypes(childTypes);

        Mockito.when(settingsService.getValueForKey(SettingsServiceBean.Key.Authority)).thenReturn("10.5072");
        Mockito.when(settingsService.getValueForKey(SettingsServiceBean.Key.Protocol)).thenReturn("doi");
        sut = new JsonParser(datasetFieldTypeSvc, null, settingsService);
    }

    @Test
    public void testCompoundRepeatsRoundtrip() {
        //given
        ArrayList<DatasetField> expectedFields = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            DatasetField expected = new DatasetField();
            expected.setDatasetFieldType(datasetFieldTypeSvc.findByName("coordinate"));
            expected.setDatasetFieldsChildren(Arrays.asList(latLonField("lat", Integer.toString(i * 10)),
                                                            latLonField("lon", Integer.toString(3 + i * 10))));
            expectedFields.add(expected);
        }

        //when
        List<JsonObject> parsedFields = expectedFields.stream()
                .map(JsonPrinter::json)
                .collect(Collectors.toList());

        List<DatasetField> actualFields = parsedFields.stream()
                .flatMap(jsonObject -> API.unchecked(() -> sut.parseField(jsonObject)).get().stream())
                .collect(Collectors.toList());

        //then
        Assertions.assertAll(() -> assertFieldsEqual(expectedFields.get(0), actualFields.get(0)),
                             () -> assertFieldsEqual(expectedFields.get(1), actualFields.get(1)),
                             () -> assertFieldsEqual(expectedFields.get(2), actualFields.get(2)),
                             () -> assertFieldsEqual(expectedFields.get(3), actualFields.get(3)),
                             () -> assertFieldsEqual(expectedFields.get(4), actualFields.get(4)));
    }

    DatasetField latLonField(String latLon, String value) {
        DatasetField retVal = new DatasetField();
        retVal.setDatasetFieldType(datasetFieldTypeSvc.findByName(latLon));
        retVal.setFieldValue(value);
        return retVal;
    }

    @Test
    public void testControlledVocalNoRepeatsRoundTrip() throws JsonParseException {
        //given
        DatasetField expected = new DatasetField();
        DatasetFieldType fieldType = datasetFieldTypeSvc.findByName("publicationIdType");
        expected.setDatasetFieldType(fieldType);
        expected.setControlledVocabularyValues(Collections.singletonList(fieldType.getControlledVocabularyValue("ark")));

        //when
        JsonObject json = JsonPrinter.json(expected);
        DatasetField actual = sut.parseField(json).get(0);

        //then
        assertFieldsEqual(expected, actual);

    }

    @Test
    public void testControlledVocalRepeatsRoundTrip() throws JsonParseException {
        //given
        DatasetField expected = new DatasetField();
        DatasetFieldType fieldType = datasetFieldTypeSvc.findByName("subject");
        expected.setDatasetFieldType(fieldType);
        expected.setControlledVocabularyValues(Arrays.asList(fieldType.getControlledVocabularyValue("mgmt"),
                                                             fieldType.getControlledVocabularyValue("law"),
                                                             fieldType.getControlledVocabularyValue("cs")));

        //when
        JsonObject json = JsonPrinter.json(expected);
        DatasetField actual = sut.parseField(json).get(0);

        //then
        assertFieldsEqual(expected, actual);

    }


    @Test(expected = JsonParseException.class)
    public void testChildValidation() throws JsonParseException {
        // This Json String is a compound field that contains the wrong
        // fieldType as a child ("description" is not a child of "coordinate").
        // It should throw a JsonParseException when it encounters the invalid child.

        //given
        String compoundString = "{ " +
                "            \"typeClass\": \"compound\"," +
                "            \"multiple\": true," +
                "            \"typeName\": \"coordinate\"," +
                "            \"value\": [" +
                "              {" +
                "                \"description\": {" +
                "                  \"value\": \"0\"," +
                "                  \"typeClass\": \"primitive\"," +
                "                  \"multiple\": false," +
                "                  \"typeName\": \"description\"" +
                "                }" +
                "              }" +
                "            ]" +
                "            " +
                "          }";

        //when
        JsonReader jsonReader = Json.createReader(new StringReader(compoundString));
        JsonObject obj = jsonReader.readObject();

        //then
        sut.parseField(obj);
    }


    @Test
    public void testPrimitiveNoRepeatesFieldRoundTrip() throws JsonParseException {
        //given
        DatasetField expected = new DatasetField();
        expected.setDatasetFieldType(datasetFieldTypeSvc.findByName("description"));
        expected.setFieldValue("This is a description value");

        //when
        JsonObject json = JsonPrinter.json(expected);
        DatasetField actual = sut.parseField(json).get(0);

        //then
        assertFieldsEqual(actual, expected);
    }

    @Test
    public void testPrimitiveRepeatesFieldRoundTrip() {

        //given
        List<DatasetField> expectedFields = Arrays.asList(new DatasetField()
                                                                  .setDatasetFieldType(datasetFieldTypeSvc.findByName(
                                                                          "keyword"))
                                                                  .setFieldValue("kw1"),
                                                          new DatasetField()
                                                                  .setDatasetFieldType(datasetFieldTypeSvc.findByName(
                                                                          "keyword"))
                                                                  .setFieldValue("kw2"),
                                                          new DatasetField()
                                                                  .setDatasetFieldType(datasetFieldTypeSvc.findByName(
                                                                          "keyword"))
                                                                  .setFieldValue("kw3"));

        //when
        List<DatasetField> actualFields = expectedFields.stream()
                .map(JsonPrinter::json)
                .flatMap(jsonObject -> API.unchecked(() -> sut.parseField(jsonObject)).get().stream())
                .collect(Collectors.toList());


        //then
        Assertions.assertAll(() -> assertFieldsEqual(expectedFields.get(0), actualFields.get(0)),
                             () -> assertFieldsEqual(expectedFields.get(1), actualFields.get(1)),
                             () -> assertFieldsEqual(expectedFields.get(2), actualFields.get(2)));
    }

    /**
     * Test that a complete dataverse JSON object is correctly parsed. This
     * checks that required and optional properties are parsed into the correct
     * dataverse properties.
     *
     * @throws JsonParseException when this test is broken.
     */
    @Test
    public void testParseCompleteDataverse() throws JsonParseException {
        //given
        JsonObject dvJson;
        try (InputStreamReader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(
                "json/dataverse-complete.json"), StandardCharsets.UTF_8)) {
            dvJson = Json.createReader(reader).readObject();
            Dataverse actual = sut.parseDataverse(dvJson);

            //when & then
            assertEquals("Scientific Research", actual.getName());
            assertEquals("science", actual.getAlias());
            assertEquals("Scientific Research University", actual.getAffiliation());
            assertEquals("We do all the science.", actual.getDescription());
            assertEquals("LABORATORY", actual.getDataverseType().toString());
            assertEquals(2, actual.getDataverseContacts().size());
            assertEquals("pi@example.edu,student@example.edu", actual.getContactEmails());
            assertEquals(0, actual.getDataverseContacts().get(0).getDisplayOrder());
            assertEquals(1, actual.getDataverseContacts().get(1).getDisplayOrder());
            /**
             * The JSON does not specify "permissionRoot" because it's a no-op
             * so we don't want to document it in the API Guide. It's a no-op
             * because as of fb7e65f (4.0) all dataverses have permissionRoot
             * hard coded to true.
             */
            assertFalse(actual.isPermissionRoot());
        } catch (IOException ioe) {
            throw new JsonParseException("Couldn't read test file", ioe);
        }
    }

    @Test
    public void testParseThemeDataverse() throws JsonParseException {
        //given
        JsonObject dvJson;
        try (InputStream jsonFile = ClassLoader.getSystemResourceAsStream("json/dataverse-theme.json")) {
            InputStreamReader reader = new InputStreamReader(jsonFile, StandardCharsets.UTF_8);
            dvJson = Json.createReader(reader).readObject();

            //when & then
            Dataverse actual = sut.parseDataverse(dvJson);
            assertEquals("testDv", actual.getName());
            assertEquals("testAlias", actual.getAlias());
            assertEquals("Test-Driven University", actual.getAffiliation());
            assertEquals("test Description.", actual.getDescription());
            assertEquals("UNCATEGORIZED", actual.getDataverseType().toString());
            assertEquals("gray", actual.getDataverseTheme().getBackgroundColor());
            assertEquals("red", actual.getDataverseTheme().getLinkColor());
            assertEquals("http://www.cnn.com", actual.getDataverseTheme().getLinkUrl());
            assertEquals("lion", actual.getDataverseTheme().getLogo());
            assertEquals(Alignment.CENTER, actual.getDataverseTheme().getLogoAlignment());
            assertEquals(2, actual.getDataverseContacts().size());
            assertEquals("test@example.com,test@example.org", actual.getContactEmails());
            assertEquals(0, actual.getDataverseContacts().get(0).getDisplayOrder());
            assertEquals(1, actual.getDataverseContacts().get(1).getDisplayOrder());
            assertTrue(actual.isPermissionRoot());
        } catch (IOException ioe) {
            throw new JsonParseException("Couldn't read test file", ioe);
        }
    }

    /**
     * Test that a minimally complete dataverse JSON object is correctly parsed.
     * This checks for required properties and default values for optional
     * values.
     *
     * @throws JsonParseException when this test is broken.
     */
    @Test
    public void testParseMinimalDataverse() throws JsonParseException {
        //given
        JsonObject dvJson;
        try (InputStream jsonFile = ClassLoader.getSystemResourceAsStream("json/minimal-dataverse.json")) {
            InputStreamReader reader = new InputStreamReader(jsonFile, StandardCharsets.UTF_8);
            dvJson = Json.createReader(reader).readObject();

            //when & then
            Dataverse actual = sut.parseDataverse(dvJson);
            assertEquals("testDv", actual.getName());
            assertEquals("testAlias", actual.getAlias());
            assertEquals("UNCATEGORIZED", actual.getDataverseType().toString());
            assertTrue(actual.getDataverseContacts().isEmpty());
            assertEquals("", actual.getContactEmails());
            assertFalse(actual.isPermissionRoot());
            assertFalse(actual.isFacetRoot());
        } catch (IOException ioe) {
            throw new JsonParseException("Couldn't read test file", ioe);
        }
    }

    /**
     * Test that a dataverse JSON object without alias fails to parse.
     *
     * @throws JsonParseException if all goes well - this is expected.
     * @throws IOException        when test file IO goes wrong - this is bad.
     */
    @Test(expected = JsonParseException.class)
    public void testParseNoAliasDataverse() throws JsonParseException, IOException {
        //given
        JsonObject dvJson;
        try (InputStream jsonFile = ClassLoader.getSystemResourceAsStream("json/no-alias-dataverse.json")) {
            dvJson = Json.createReader(jsonFile).readObject();

            //when & then
            Dataverse actual = sut.parseDataverse(dvJson);
        }
    }

    /**
     * Test that a dataverse JSON object without name fails to parse.
     *
     * @throws JsonParseException if all goes well - this is expected.
     * @throws IOException        when test file IO goes wrong - this is bad.
     */
    @Test(expected = JsonParseException.class)
    public void testParseNoNameDataverse() throws JsonParseException, IOException {
        //given
        JsonObject dvJson;
        try (InputStream jsonFile = ClassLoader.getSystemResourceAsStream("json/no-name-dataverse.json")) {
            dvJson = Json.createReader(jsonFile).readObject();

            //when & then
            Dataverse actual = sut.parseDataverse(dvJson);
        }
    }

    /**
     * Test that a dataverse JSON object with contacts, but without contact
     * email fails to parse.
     *
     * @throws JsonParseException if all goes well - this is expected.
     * @throws IOException        when test file IO goes wrong - this is bad.
     */
    @Test(expected = JsonParseException.class)
    public void testParseNoContactEmailsDataverse() throws JsonParseException, IOException {
        //given
        JsonObject dvJson;
        try (InputStream jsonFile = ClassLoader.getSystemResourceAsStream("json/no-contacts-dataverse.json")) {
            dvJson = Json.createReader(jsonFile).readObject();

            //when & then
            Dataverse actual = sut.parseDataverse(dvJson);
        }
    }

    /**
     * Create a date, output it as a JSON string and parse it into the same date.
     * This is a bit tricky, as parsing dates only looks at the first part of
     * the date string, which means time zone indicators are ignored. Only when
     * UTC dates and cleared calendars are used, it is "safe" to perform this
     * round-trip.
     *
     * @throws ParseException if Dataverse outputs date strings that it cannot
     *                        parse.
     */
    @Test
    public void testDateRoundtrip() throws ParseException {
        //given
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.clear();
        c.set(2015, 8, 15);
        Date d = c.getTime();
        String generated = JsonPrinter.format(d);
        System.err.println(generated);

        //when
        Date parsedDate = sut.parseDate(generated);
        Calendar p = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        p.clear();
        p.setTime(parsedDate);

        //then
        assertEquals(c.get(Calendar.YEAR), p.get(Calendar.YEAR));
        assertEquals(c.get(Calendar.MONTH), p.get(Calendar.MONTH));
        assertEquals(c.get(Calendar.DAY_OF_MONTH), p.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * Test that a date-time string that the {@link JsonPrinter} outputs a string
     * that JsonParser can read correctly. This defines a non-UTC date-time that
     * when output as a string and parsed must give the same date-time.
     *
     * @throws ParseException when JsonPrinter outputs a string that JsonParse
     *                        cannot read.
     */
    @Test
    public void testDateTimeRoundtrip() throws ParseException {
        //given
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Europe/Amsterdam"));
        c.clear();
        c.set(2015, 8, 15, 13, 37, 56);
        Date d = c.getTime();
        String generated = JsonPrinter.format(d);
        System.err.println(generated);

        //when
        Date parsedDate = sut.parseTime(generated);

        //then
        assertEquals(d, parsedDate);
    }

    /**
     * Expect an exception when the dataset JSON is empty.
     *
     * @throws JsonParseException when the test is broken
     */
    @Test(expected = NullPointerException.class)
    public void testParseEmptyDataset() throws JsonParseException {
        //given
        JsonObject dsJson;
        try (InputStream jsonFile = ClassLoader.getSystemResourceAsStream("json/empty-dataset.json")) {
            InputStreamReader reader = new InputStreamReader(jsonFile, StandardCharsets.UTF_8);
            dsJson = Json.createReader(reader).readObject();
            System.out.println(dsJson != null);

            //when
            Dataset actual = sut.parseDataset(dsJson);

            //then
            assertEquals("10.5072", actual.getAuthority());
            assertEquals("doi", actual.getProtocol());
        } catch (IOException ioe) {
            throw new JsonParseException("Couldn't read test file", ioe);
        }
    }

    /**
     * Expect an exception when the dataset version JSON contains fields
     * that the {@link DatasetFieldService} doesn't know about.
     *
     * @throws JsonParseException as expected
     * @throws IOException        when test file IO goes wrong - this is bad.
     */
    @Test(expected = JsonParseException.class)
    public void testParseOvercompleteDatasetVersion() throws JsonParseException, IOException {
        //given
        JsonObject dsJson;
        try (InputStream jsonFile = ClassLoader.getSystemResourceAsStream("json/complete-dataset-version.json")) {
            InputStreamReader reader = new InputStreamReader(jsonFile, StandardCharsets.UTF_8);
            dsJson = Json.createReader(reader).readObject();
            System.out.println(dsJson != null);

            //when & then
            DatasetVersion actual = sut.parseDatasetVersion(dsJson);
        }
    }

    @Test
    public void testIpGroupRoundTrip() {
        //given
        IpGroup original = new IpGroup();
        original.setDescription("Ip group description");
        original.setDisplayName("Test-ip-group");
        original.setId(42l);
        original.setPersistedGroupAlias("test-ip-group");

        original.add(IpAddressRange.make(IpAddress.valueOf("1.2.1.1"), IpAddress.valueOf("1.2.1.10")));
        original.add(IpAddressRange.make(IpAddress.valueOf("1.1.1.1"), IpAddress.valueOf("1.1.1.1")));
        original.add(IpAddressRange.make(IpAddress.valueOf("1:2:3::4:5"), IpAddress.valueOf("1:2:3::4:5")));
        original.add(IpAddressRange.make(IpAddress.valueOf("1:2:3::3:ff"), IpAddress.valueOf("1:2:3::3:5")));

        JsonObject serialized = JsonPrinter.json(original).build();

        System.out.println(serialized.toString());

        //when
        IpGroup parsed = new JsonParser().parseIpGroup(serialized);

        //then
        assertEquals(original, parsed);

    }

    @Test
    public void testIpGroupRoundTrip_singleIpv4Address() {
        //given
        IpGroupProvider ipGroupProvider = new IpGroupProvider(null);
        IpGroup original = new IpGroup();
        original.setDescription("Ip group description");
        original.setDisplayName("Test-ip-group");
        original.setId(42l);
        original.setPersistedGroupAlias("test-ip-group");

        original.add(IpAddressRange.make(IpAddress.valueOf("1.1.1.1"), IpAddress.valueOf("1.1.1.1")));

        JsonObject serialized = JsonPrinter.json(original).build();

        System.out.println(serialized.toString());

        //when
        IpGroup parsed = new JsonParser().parseIpGroup(serialized);

        //then
        assertEquals(original, parsed);
        assertTrue(ipGroupProvider.contains(new DataverseRequest(GuestUser.get(), IpAddress.valueOf("1.1.1.1")),
                                            parsed));
        assertFalse(ipGroupProvider.contains(new DataverseRequest(GuestUser.get(), IpAddress.valueOf("1.1.1.2")),
                                             parsed));
        assertFalse(ipGroupProvider.contains(new DataverseRequest(GuestUser.get(), IpAddress.valueOf("1.1.2.1")),
                                             parsed));
        assertFalse(ipGroupProvider.contains(new DataverseRequest(GuestUser.get(), IpAddress.valueOf("1.1.1.0")),
                                             parsed));
        assertFalse(ipGroupProvider.contains(new DataverseRequest(GuestUser.get(), IpAddress.valueOf("1.1.1.250")),
                                             parsed));
        assertFalse(ipGroupProvider.contains(new DataverseRequest(GuestUser.get(), IpAddress.valueOf("1.2.1.1")),
                                             parsed));
        assertFalse(ipGroupProvider.contains(new DataverseRequest(GuestUser.get(), IpAddress.valueOf("2.1.1.1")),
                                             parsed));
        assertFalse(ipGroupProvider.contains(new DataverseRequest(GuestUser.get(),
                                                                  IpAddress.valueOf("fe80::22c9:d0ff:fe48:ce61")),
                                             parsed));

    }

    @Test
    public void testIpGroupRoundTrip_singleIpv6Address() {
        //given
        IpGroupProvider ipGroupProvider = new IpGroupProvider(null);
        IpGroup original = new IpGroup();
        original.setDescription("Ip group description");
        original.setDisplayName("Test-ip-group");
        original.setId(42l);
        original.setPersistedGroupAlias("test-ip-group");

        original.add(IpAddressRange.make(IpAddress.valueOf("fe80::22c9:d0ff:fe48:ce61"),
                                         IpAddress.valueOf("fe80::22c9:d0ff:fe48:ce61")));

        JsonObject serialized = JsonPrinter.json(original).build();

        System.out.println(serialized.toString());

        //when
        IpGroup parsed = new JsonParser().parseIpGroup(serialized);

        //then
        assertEquals(original, parsed);
        assertTrue(ipGroupProvider.contains(new DataverseRequest(GuestUser.get(),
                                                                 IpAddress.valueOf("fe80::22c9:d0ff:fe48:ce61")),
                                            parsed));
        assertFalse(ipGroupProvider.contains(new DataverseRequest(GuestUser.get(),
                                                                  IpAddress.valueOf("fe80::22c9:d0ff:fe48:ce60")),
                                             parsed));
        assertFalse(ipGroupProvider.contains(new DataverseRequest(GuestUser.get(),
                                                                  IpAddress.valueOf("fe80::22c9:d0ff:fe48:ce62")),
                                             parsed));
        assertFalse(ipGroupProvider.contains(new DataverseRequest(GuestUser.get(),
                                                                  IpAddress.valueOf("fe80::22c9:d0ff:fe47:ce61")),
                                             parsed));
        assertFalse(ipGroupProvider.contains(new DataverseRequest(GuestUser.get(),
                                                                  IpAddress.valueOf("fe80::22c9:d0af:fe48:ce61")),
                                             parsed));
        assertFalse(ipGroupProvider.contains(new DataverseRequest(GuestUser.get(),
                                                                  IpAddress.valueOf("fe79::22c9:d0ff:fe48:ce61")),
                                             parsed));
        assertFalse(ipGroupProvider.contains(new DataverseRequest(GuestUser.get(), IpAddress.valueOf("2.1.1.1")),
                                             parsed));

    }

    @Test
    public void testparseFiles() throws JsonParseException {
        //given
        JsonArrayBuilder metadatasJsonBuilder = Json.createArrayBuilder();
        JsonObjectBuilder fileMetadataGood = Json.createObjectBuilder();
        fileMetadataGood.add("label", "myLabel");
        JsonObjectBuilder fileGood = Json.createObjectBuilder();
        fileMetadataGood.add("dataFile", fileGood);
        fileMetadataGood.add("categories", Json.createArrayBuilder()
                .add("Documentation")
        );
        JsonObjectBuilder fileMetadataBad = Json.createObjectBuilder();
        fileMetadataBad.add("label", "bad");
        JsonObjectBuilder fileBad = Json.createObjectBuilder();
        fileMetadataBad.add("dataFile", fileBad);
        fileMetadataBad.add("categories", Json.createArrayBuilder()
                .add(BigDecimal.ONE)
        );
        metadatasJsonBuilder.add(fileMetadataGood);
        metadatasJsonBuilder.add(fileMetadataBad);
        JsonArray metadatasJson = metadatasJsonBuilder.build();
        DatasetVersion dsv = createEmptyDatasetVersion();
        //when
        List<FileMetadata> fileMetadatas = new JsonParser().parseFiles(metadatasJson, dsv);

        //then
        assertEquals("myLabel", fileMetadatas.get(0).getLabel());
        assertEquals("Documentation", fileMetadatas.get(0).getCategories().get(0).getName());
        assertNull(fileMetadatas.get(1).getCategories());
        List<FileMetadata> codeCoverage = new JsonParser().parseFiles(Json.createArrayBuilder().add(Json.createObjectBuilder().add(
                "label",
                "myLabel").add("dataFile", Json.createObjectBuilder().add("categories", JsonValue.NULL))).build(), dsv);
        assertNull(codeCoverage.get(0).getCategories());
    }

    @Test
    public void shouldParseDatasetVersionFilesWithProperDisplayOrder() throws JsonParseException {
        // given
        JsonObject versionJson = emptyDatasetVersionWithFilesJson();
        DatasetVersion datasetVersion = createEmptyDatasetVersion();

        // when
        datasetVersion = new JsonParser().parseDatasetVersion(versionJson, datasetVersion);

        // then
        verifyDisplayOrderAtIndex(datasetVersion, 0, 0);
        verifyDisplayOrderAtIndex(datasetVersion, 1, 1);
    }

    JsonObject json(String s) {
        return Json.createReader(new StringReader(s)).readObject();
    }

    public boolean assertFieldsEqual(DatasetField ex, DatasetField act) {
        if (ex == act) {
            return true;
        }
        if ((ex == null) ^ (act == null)) {
            return false;
        }

        // type
        if (!ex.getDatasetFieldType().equals(act.getDatasetFieldType())) {
            return false;
        }

        if (ex.getDatasetFieldType().isControlledVocabulary()) {
            List<ControlledVocabularyValue> exVals = ex.getControlledVocabularyValues();
            List<ControlledVocabularyValue> actVals = act.getControlledVocabularyValues();
            if (exVals.size() != actVals.size()) {
                return false;
            }
            Iterator<ControlledVocabularyValue> exItr = exVals.iterator();
            for (ControlledVocabularyValue actVal : actVals) {
                ControlledVocabularyValue exVal = exItr.next();
                if (!exVal.getId().equals(actVal.getId())) {
                    return false;
                }
            }
            return true;

        } else {

            List<DatasetField> exVals = ex.getDatasetFieldsChildren();
            List<DatasetField> actVals = act.getDatasetFieldsChildren();

            if (ex.getFieldValue().isDefined() && act.getFieldValue().isDefined()) {
                return ex.getFieldValue().get().equals(act.getFieldValue().get());
            }
            if (exVals.size() != actVals.size()) {
                return false;
            }
            Iterator<DatasetField> exItr = exVals.iterator();
            for (DatasetField actVal : actVals) {
                DatasetField exVal = exItr.next();
                if (!exVal.getValue().equals(actVal.getValue())) {
                    return false;
                }
            }
            return true;
        }
    }


    @TestBean
    static class MockDatasetFieldSvc extends DatasetFieldServiceBean {

        Map<String, DatasetFieldType> fieldTypes = new HashMap<>();
        long nextId = 1;

        public DatasetFieldType add(DatasetFieldType t) {
            if (t.getId() == null) {
                t.setId(nextId++);
            }
            fieldTypes.put(t.getName(), t);
            return t;
        }

        @Override
        public DatasetFieldType findByName(String name) {
            return fieldTypes.get(name);
        }

        @Override
        public DatasetFieldType findByNameOpt(String name) {
            return findByName(name);
        }

        @Override
        public ControlledVocabularyValue findControlledVocabularyValueByDatasetFieldTypeAndStrValue(DatasetFieldType dsft, String strValue, boolean lenient) {
            ControlledVocabularyValue cvv = new ControlledVocabularyValue();
            cvv.setDatasetFieldType(dsft);
            cvv.setStrValue(strValue);
            return cvv;
        }

    }

    private DatasetVersion createEmptyDatasetVersion() {
        DatasetVersion datasetVersion = new DatasetVersion();
        Dataset dataset = new Dataset();
        datasetVersion.setDataset(dataset);
        return datasetVersion;
    }

    private JsonObject emptyDatasetVersionWithFilesJson() {
        JsonArrayBuilder jsonFilesBuilder = Json.createArrayBuilder();
        jsonFilesBuilder.add(fileMetadatasAsJson("file1.png", "Documentation"));
        jsonFilesBuilder.add(fileMetadatasAsJson("file2.png", "Documentation"));
        JsonObjectBuilder versionJson = Json.createObjectBuilder();
        versionJson.add("files", jsonFilesBuilder);
        versionJson.add("metadataBlocks", Json.createObjectBuilder());
        return versionJson.build();
    }

    private JsonObjectBuilder fileMetadatasAsJson(String label, String category) {
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
        jsonBuilder.add("label", label);
        JsonObjectBuilder file = Json.createObjectBuilder();
        jsonBuilder.add("dataFile", file);
        jsonBuilder.add("categories", Json.createArrayBuilder().add(category));
        return jsonBuilder;
    }

    private void verifyDisplayOrderAtIndex(DatasetVersion datasetVersion, int displayOrder, int index) {
        assertEquals(displayOrder, datasetVersion.getFileMetadatas().get(index).getDisplayOrder());
    }

}
