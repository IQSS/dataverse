/*
 *  (C) Michael Bar-Sinai
 */

package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldType.FieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseTheme.Alignment;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.UserNotification.Type;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroupProvider;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddressRange;
import edu.harvard.iq.dataverse.authorization.groups.impl.maildomain.MailDomainGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.maildomain.MailDomainGroupTest;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.dataset.DatasetType;
import edu.harvard.iq.dataverse.dataset.DatasetTypeServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.license.LicenseServiceBean;
import edu.harvard.iq.dataverse.mocks.MockDatasetFieldSvc;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author michael
 */
public class JsonParserTest {
    
    MockDatasetFieldSvc datasetFieldTypeSvc = null;
    MockSettingsSvc settingsSvc = null;
    LicenseServiceBean licenseService = Mockito.mock(LicenseServiceBean.class);
    DatasetTypeServiceBean datasetTypeService = Mockito.mock(DatasetTypeServiceBean.class);
    DatasetFieldType keywordType;
    DatasetFieldType descriptionType;
    DatasetFieldType subjectType;
    DatasetFieldType pubIdType;
    DatasetFieldType compoundSingleType;
    JsonParser sut;
    
    public JsonParserTest() {
    }
    
    @BeforeAll
    public static void setUpClass() {
    }
    
    @AfterAll
    public static void tearDownClass() {
    }
    
    @BeforeEach
    public void setUp() {
        datasetFieldTypeSvc = new MockDatasetFieldSvc();
        datasetFieldTypeSvc.setMetadataBlock("citation");

        keywordType = datasetFieldTypeSvc.add(new DatasetFieldType("keyword", FieldType.TEXT, true));
        descriptionType = datasetFieldTypeSvc.add( new DatasetFieldType("description", FieldType.TEXTBOX, false) );
        
        subjectType = datasetFieldTypeSvc.add(new DatasetFieldType("subject", FieldType.TEXT, true));
        subjectType.setAllowControlledVocabulary(true);
        subjectType.setControlledVocabularyValues( Arrays.asList( 
                new ControlledVocabularyValue(1l, "mgmt", subjectType),
                new ControlledVocabularyValue(2l, "law", subjectType),
                new ControlledVocabularyValue(3l, "cs", subjectType)
        ));
        
        pubIdType = datasetFieldTypeSvc.add(new DatasetFieldType("publicationIdType", FieldType.TEXT, false));
        pubIdType.setAllowControlledVocabulary(true);
        pubIdType.setControlledVocabularyValues( Arrays.asList( 
                new ControlledVocabularyValue(1l, "ark", pubIdType),
                new ControlledVocabularyValue(2l, "doi", pubIdType),
                new ControlledVocabularyValue(3l, "url", pubIdType)
        ));
        
        compoundSingleType = datasetFieldTypeSvc.add(new DatasetFieldType("coordinate", FieldType.TEXT, true));
        Set<DatasetFieldType> childTypes = new HashSet<>();
        childTypes.add( datasetFieldTypeSvc.add(new DatasetFieldType("lat", FieldType.TEXT, false)) );
        childTypes.add( datasetFieldTypeSvc.add(new DatasetFieldType("lon", FieldType.TEXT, false)) );
        
        for ( DatasetFieldType t : childTypes ) {
            t.setParentDatasetFieldType(compoundSingleType);
        }
        compoundSingleType.setChildDatasetFieldTypes(childTypes);
        settingsSvc = new MockSettingsSvc();
        DatasetType datasetType = new DatasetType();
        datasetType.setName(DatasetType.DEFAULT_DATASET_TYPE);
        datasetType.setId(1l);
        Mockito.when(datasetTypeService.getByName(DatasetType.DEFAULT_DATASET_TYPE)).thenReturn(datasetType);
        sut = new JsonParser(datasetFieldTypeSvc, null, settingsSvc, licenseService, datasetTypeService);
    }
    
    @Test 
    public void testCompoundRepeatsRoundtrip() throws JsonParseException {
        DatasetField expected = new DatasetField();
        expected.setDatasetFieldType( datasetFieldTypeSvc.findByName("coordinate") );
        List<DatasetFieldCompoundValue> vals = new LinkedList<>();
        for ( int i=0; i<5; i++ ) {
            DatasetFieldCompoundValue val = new DatasetFieldCompoundValue();
            val.setParentDatasetField(expected);
            val.setChildDatasetFields( Arrays.asList(latLonField("lat", Integer.toString(i*10)), latLonField("lon", Integer.toString(3+i*10))));
            vals.add( val );
        }
        expected.setDatasetFieldCompoundValues(vals);
        
        JsonObject json = JsonPrinter.json(expected);
        
        System.out.println("json = " + json);
        
        DatasetField actual = sut.parseField(json);
        
        assertFieldsEqual(expected, actual);
    }
    
    DatasetField latLonField( String latLon, String value ) {
        DatasetField retVal = new DatasetField();
        retVal.setDatasetFieldType( datasetFieldTypeSvc.findByName(latLon));
        retVal.setDatasetFieldValues( Collections.singletonList( new DatasetFieldValue(retVal, value)));
        return retVal;
    }
    
    @Test 
    public void testControlledVocalNoRepeatsRoundTrip() throws JsonParseException {
        DatasetField expected = new DatasetField();
        DatasetFieldType fieldType = datasetFieldTypeSvc.findByName("publicationIdType");
        expected.setDatasetFieldType( fieldType );
        expected.setControlledVocabularyValues( Collections.singletonList( fieldType.getControlledVocabularyValue("ark")));
        JsonObject json = JsonPrinter.json(expected);
        
        DatasetField actual = sut.parseField(json);
        assertFieldsEqual(expected, actual);
        
    }
    
    @Test 
    public void testControlledVocalRepeatsRoundTrip() throws JsonParseException {
        DatasetField expected = new DatasetField();
        DatasetFieldType fieldType = datasetFieldTypeSvc.findByName("subject");
        expected.setDatasetFieldType( fieldType );
        expected.setControlledVocabularyValues( Arrays.asList( fieldType.getControlledVocabularyValue("mgmt"),
                 fieldType.getControlledVocabularyValue("law"),
                 fieldType.getControlledVocabularyValue("cs")));
        
        JsonObject json = JsonPrinter.json(expected);      
        DatasetField actual = sut.parseField(json);
        assertFieldsEqual(expected, actual);
        
    }
    
    
    @Test
    void testChildValidation() {
        // This Json String is a compound field that contains the wrong
        // fieldType as a child ("description" is not a child of "coordinate").
        // It should throw a JsonParseException when it encounters the invalid child.
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
   
        String text = compoundString;
        JsonReader jsonReader = Json.createReader(new StringReader(text));
        JsonObject obj = jsonReader.readObject();

        assertThrows(JsonParseException.class, () -> sut.parseField(obj));
    }
    
    
    @Test
    public void testPrimitiveNoRepeatesFieldRoundTrip() throws JsonParseException {
        DatasetField expected = new DatasetField();
        expected.setDatasetFieldType( datasetFieldTypeSvc.findByName("description") );
        expected.setDatasetFieldValues( Collections.singletonList(new DatasetFieldValue(expected, "This is a description value")) );
        JsonObject json = JsonPrinter.json(expected);
        
        DatasetField actual = sut.parseField(json);
        
        assertFieldsEqual(actual, expected);
    }
    
    @Test
    public void testPrimitiveRepeatesFieldRoundTrip() throws JsonParseException {
        DatasetField expected = new DatasetField();
        expected.setDatasetFieldType( datasetFieldTypeSvc.findByName("keyword") );
        expected.setDatasetFieldValues( Arrays.asList(new DatasetFieldValue(expected, "kw1"),
                new DatasetFieldValue(expected, "kw2"),
                new DatasetFieldValue(expected, "kw3")) );
        JsonObject json = JsonPrinter.json(expected);
        
        DatasetField actual = sut.parseField(json);
        
        assertFieldsEqual(actual, expected);
    }
    
    /**
     * Test that a complete dataverse JSON object is correctly parsed. This
     * checks that required and optional properties are parsed into the correct
     * dataverse properties.
     * @throws JsonParseException when this test is broken.
     */
    @Test
    public void testParseCompleteDataverse() throws JsonParseException {
        
        JsonObject dvJson;
        try (FileReader reader = new FileReader("doc/sphinx-guides/source/_static/api/dataverse-complete.json")) {
            dvJson = Json.createReader(reader).readObject();
            Dataverse actual = sut.parseDataverse(dvJson);
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
        
        JsonObject dvJson;
        try (InputStream jsonFile = ClassLoader.getSystemResourceAsStream("json/dataverse-theme.json")) {
            InputStreamReader reader = new InputStreamReader(jsonFile, "UTF-8");
            dvJson = Json.createReader(reader).readObject();
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
     * @throws JsonParseException when this test is broken.
     */
    @Test
    public void testParseMinimalDataverse() throws JsonParseException {
        
        JsonObject dvJson;
        try (InputStream jsonFile = ClassLoader.getSystemResourceAsStream("json/minimal-dataverse.json")) {
            InputStreamReader reader = new InputStreamReader(jsonFile, "UTF-8");
            dvJson = Json.createReader(reader).readObject();
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
     * @throws JsonParseException if all goes well - this is expected.
     * @throws IOException when test file IO goes wrong - this is bad.
     */
    @Test
    void testParseNoAliasDataverse() throws IOException {
        JsonObject dvJson;
        try (InputStream jsonFile = ClassLoader.getSystemResourceAsStream("json/no-alias-dataverse.json")) {
            dvJson = Json.createReader(jsonFile).readObject();
            assertThrows(JsonParseException.class, () -> sut.parseDataverse(dvJson));
        }
    }
    
    /**
     * Test that a dataverse JSON object without name fails to parse.
     * @throws JsonParseException if all goes well - this is expected.
     * @throws IOException when test file IO goes wrong - this is bad.
     */
    @Test
    void testParseNoNameDataverse() throws IOException {
        JsonObject dvJson;
        try (InputStream jsonFile = ClassLoader.getSystemResourceAsStream("json/no-name-dataverse.json")) {
            dvJson = Json.createReader(jsonFile).readObject();
            assertThrows(JsonParseException.class, () -> sut.parseDataverse(dvJson));
        }
    }
    
    /**
     * Test that a dataverse JSON object with contacts, but without contact
     * email fails to parse.
     * @throws JsonParseException if all goes well - this is expected.
     * @throws IOException when test file IO goes wrong - this is bad.
     */
    @Test
    void testParseNoContactEmailsDataverse() throws IOException {
        JsonObject dvJson;
        try (InputStream jsonFile = ClassLoader.getSystemResourceAsStream("json/no-contacts-dataverse.json")) {
            dvJson = Json.createReader(jsonFile).readObject();
            assertThrows(JsonParseException.class, () -> sut.parseDataverse(dvJson));
        }
    }

    /**
     * Create a date, output it as a JSON string and parse it into the same date.
     * This is a bit tricky, as parsing dates only looks at the first part of
     * the date string, which means time zone indicators are ignored. Only when
     * UTC dates and cleared calendars are used, it is "safe" to perform this
     * round-trip.
     * @throws ParseException if Dataverse outputs date strings that it cannot
     * parse.
     */
    @Test
    public void testDateRoundtrip() throws ParseException {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.clear();
        c.set(2015, 8, 15);
        Date d = c.getTime();
        String generated = JsonPrinter.format(d);
        System.err.println(generated);
        Date parsedDate = sut.parseDate(generated);
        Calendar p = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        p.clear();
        p.setTime(parsedDate);
        assertEquals(c.get(Calendar.YEAR), p.get(Calendar.YEAR));
        assertEquals(c.get(Calendar.MONTH), p.get(Calendar.MONTH));
        assertEquals(c.get(Calendar.DAY_OF_MONTH), p.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * Test that a date-time string that the {@link JsonPrinter} outputs a string
     * that JsonParser can read correctly. This defines a non-UTC date-time that
     * when output as a string and parsed must give the same date-time.
     * @throws ParseException when JsonPrinter outputs a string that JsonParse
     * cannot read.
     */
    @Test
    public void testDateTimeRoundtrip() throws ParseException {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Europe/Amsterdam"));
        c.clear();
        c.set(2015, 8, 15, 13, 37, 56);
        Date d = c.getTime();
        String generated = JsonPrinter.format(d);
        System.err.println(generated);
        Date parsedDate = sut.parseTime(generated);
        assertEquals(d, parsedDate);
    }

    /**
     * Expect an exception when the dataset JSON is empty.
     * @throws JsonParseException when the test is broken
     */
    @Test
    void testParseEmptyDataset() throws JsonParseException {
        JsonObject dsJson;
        try (InputStream jsonFile = ClassLoader.getSystemResourceAsStream("json/empty-dataset.json")) {
            InputStreamReader reader = new InputStreamReader(jsonFile, "UTF-8");
            dsJson = Json.createReader(reader).readObject();
            System.out.println(dsJson != null);
            assertThrows(NullPointerException.class, () -> sut.parseDataset(dsJson));
        } catch (IOException ioe) {
            throw new JsonParseException("Couldn't read test file", ioe);
        }
    }

    /**
     * 
     * Expect no exception when the dataset version JSON contains fields
     * that the {@link DatasetFieldService} doesn't know about.
     * @throws JsonParseException should not happen here
     * @throws IOException when test file IO goes wrong - this is bad.
     */
    @Test
    void testParseOvercompleteDatasetVersion() throws IOException {
        JsonObject dsJson;
        try (InputStream jsonFile = ClassLoader.getSystemResourceAsStream("json/complete-dataset-version.json")) {
            InputStreamReader reader = new InputStreamReader(jsonFile, "UTF-8");
            dsJson = Json.createReader(reader).readObject();
            Assumptions.assumeTrue(dsJson != null);
            assertDoesNotThrow(() -> sut.parseDatasetVersion(dsJson));
        }
    }
    
    @Test
    public void testIpGroupRoundTrip() {
        
        IpGroup original = new IpGroup();
        original.setDescription("Ip group description");
        original.setDisplayName("Test-ip-group");
        original.setId(42l);
        original.setPersistedGroupAlias("test-ip-group");
        original.setGroupProvider( new IpGroupProvider(null) );
        
        original.add( IpAddressRange.make(IpAddress.valueOf("1.2.1.1"), IpAddress.valueOf("1.2.1.10")) );
        original.add( IpAddressRange.make(IpAddress.valueOf("1.1.1.1"), IpAddress.valueOf("1.1.1.1")) );
        original.add( IpAddressRange.make(IpAddress.valueOf("1:2:3::4:5"), IpAddress.valueOf("1:2:3::4:5")) );
        original.add( IpAddressRange.make(IpAddress.valueOf("1:2:3::3:ff"), IpAddress.valueOf("1:2:3::3:5")) );
        
        JsonObject serialized = JsonPrinter.json(original).build();
        
        System.out.println( serialized.toString() );
        
        IpGroup parsed = new JsonParser().parseIpGroup(serialized);
        
        assertEquals( original, parsed );
        
    }
    
    @Test
    public void testIpGroupRoundTrip_singleIpv4Address() {
        
        IpGroup original = new IpGroup();
        original.setDescription("Ip group description");
        original.setDisplayName("Test-ip-group");
        original.setId(42l);
        original.setPersistedGroupAlias("test-ip-group");
        original.setGroupProvider( new IpGroupProvider(null) );
        
        original.add( IpAddressRange.make(IpAddress.valueOf("1.1.1.1"), IpAddress.valueOf("1.1.1.1")) );
        
        JsonObject serialized = JsonPrinter.json(original).build();
        
        System.out.println( serialized.toString() );
        
        IpGroup parsed = new JsonParser().parseIpGroup(serialized);
        
        assertEquals( original, parsed );
        assertTrue( parsed.contains( new DataverseRequest(GuestUser.get(), IpAddress.valueOf("1.1.1.1")) ));
        assertFalse( parsed.contains( new DataverseRequest(GuestUser.get(), IpAddress.valueOf("1.1.1.2")) ));
        assertFalse( parsed.contains( new DataverseRequest(GuestUser.get(), IpAddress.valueOf("1.1.2.1")) ));
        assertFalse( parsed.contains( new DataverseRequest(GuestUser.get(), IpAddress.valueOf("1.1.1.0")) ));
        assertFalse( parsed.contains( new DataverseRequest(GuestUser.get(), IpAddress.valueOf("1.1.1.250")) ));
        assertFalse( parsed.contains( new DataverseRequest(GuestUser.get(), IpAddress.valueOf("1.2.1.1")) ));
        assertFalse( parsed.contains( new DataverseRequest(GuestUser.get(), IpAddress.valueOf("2.1.1.1")) ));
        assertFalse( parsed.contains( new DataverseRequest(GuestUser.get(), IpAddress.valueOf("fe80::22c9:d0ff:fe48:ce61")) ));
        
    }
    
    @Test
    public void testIpGroupRoundTrip_singleIpv6Address() {
        
        IpGroup original = new IpGroup();
        original.setDescription("Ip group description");
        original.setDisplayName("Test-ip-group");
        original.setId(42l);
        original.setPersistedGroupAlias("test-ip-group");
        original.setGroupProvider( new IpGroupProvider(null) );
        
        original.add( IpAddressRange.make(IpAddress.valueOf("fe80::22c9:d0ff:fe48:ce61"),
                                          IpAddress.valueOf("fe80::22c9:d0ff:fe48:ce61")) );
        
        JsonObject serialized = JsonPrinter.json(original).build();
        
        System.out.println( serialized.toString() );
        
        IpGroup parsed = new JsonParser().parseIpGroup(serialized);
        
        assertEquals( original, parsed );
        assertTrue( parsed.contains( new DataverseRequest(GuestUser.get(), IpAddress.valueOf("fe80::22c9:d0ff:fe48:ce61")) ));
        assertFalse( parsed.contains( new DataverseRequest(GuestUser.get(), IpAddress.valueOf("fe80::22c9:d0ff:fe48:ce60")) ));
        assertFalse( parsed.contains( new DataverseRequest(GuestUser.get(), IpAddress.valueOf("fe80::22c9:d0ff:fe48:ce62")) ));
        assertFalse( parsed.contains( new DataverseRequest(GuestUser.get(), IpAddress.valueOf("fe80::22c9:d0ff:fe47:ce61")) ));
        assertFalse( parsed.contains( new DataverseRequest(GuestUser.get(), IpAddress.valueOf("fe80::22c9:d0af:fe48:ce61")) ));
        assertFalse( parsed.contains( new DataverseRequest(GuestUser.get(), IpAddress.valueOf("fe79::22c9:d0ff:fe48:ce61")) ));
        assertFalse( parsed.contains( new DataverseRequest(GuestUser.get(), IpAddress.valueOf("2.1.1.1")) ));
        
    }
    
    @Test
    public void testValidMailDomainGroup() throws JsonParseException {
        // given
        MailDomainGroup test = MailDomainGroupTest.genGroup();
        
        // when
        JsonObject serialized = JsonPrinter.json(test).build();
        MailDomainGroup parsed = new JsonParser().parseMailDomainGroup(serialized);
        
        // then
        assertEquals(test, parsed);
        assertEquals(test.hashCode(), parsed.hashCode());
    }
    
    @Test
    public void testValidRegexMailDomainGroup() throws JsonParseException {
        // given
        MailDomainGroup test = MailDomainGroupTest.genRegexGroup();
        
        // when
        JsonObject serialized = JsonPrinter.json(test).build();
        MailDomainGroup parsed = new JsonParser().parseMailDomainGroup(serialized);
        
        // then
        assertEquals(test, parsed);
        assertEquals(test.hashCode(), parsed.hashCode());
    }
    
    @Test
    void testMailDomainGroupMissingName() {
        // given
        String noname = "{ \"id\": 1, \"alias\": \"test\", \"domains\": [] }";
        JsonObject obj = Json.createReader(new StringReader(noname)).readObject();
        // when && then
        assertThrows(JsonParseException.class, () -> new JsonParser().parseMailDomainGroup(obj));
    }
    
    @Test
    void testMailDomainGroupMissingDomains() {
        // given
        String noname = "{ \"name\": \"test\", \"alias\": \"test\" }";
        JsonObject obj = Json.createReader(new StringReader(noname)).readObject();
        // when && then
        assertThrows(JsonParseException.class, () -> new JsonParser().parseMailDomainGroup(obj));
    }
    
    @Test
    void testMailDomainGroupNotEnabledRegexDomains() {
        // given
        String regexNotEnabled = "{ \"id\": 1, \"alias\": \"test\", \"domains\": [\"^foobar\\\\.com\"] }";
        JsonObject obj = Json.createReader(new StringReader(regexNotEnabled)).readObject();
        // when && then
        assertThrows(JsonParseException.class, () -> new JsonParser().parseMailDomainGroup(obj));
    }

    @Test
    public void testparseFiles() throws JsonParseException {
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
        DatasetVersion dsv = new DatasetVersion();
        Dataset dataset = new Dataset();
        dsv.setDataset(dataset);
        List<FileMetadata> fileMetadatas = new JsonParser().parseFiles(metadatasJson, dsv);
        System.out.println("fileMetadatas: " + fileMetadatas);
        assertEquals("myLabel", fileMetadatas.get(0).getLabel());
        assertEquals("Documentation", fileMetadatas.get(0).getCategories().get(0).getName());
        assertEquals(null, fileMetadatas.get(1).getCategories());
        List<FileMetadata> codeCoverage = new JsonParser().parseFiles(Json.createArrayBuilder().add(Json.createObjectBuilder().add("label", "myLabel").add("dataFile", Json.createObjectBuilder().add("categories", JsonValue.NULL))).build(), dsv);
        assertEquals(null, codeCoverage.get(0).getCategories());
    }

    JsonObject json( String s ) {
        return Json.createReader( new StringReader(s) ).readObject();
    }
    
    public boolean assertFieldsEqual( DatasetField ex, DatasetField act ) {
        if ( ex == act ) return true;
        if ( (ex == null) ^ (act==null) ) return false;
        
        // type
        if ( ! ex.getDatasetFieldType().equals(act.getDatasetFieldType()) ) return false;
        
        if ( ex.getDatasetFieldType().isPrimitive() ) {
            List<DatasetFieldValue> exVals = ex.getDatasetFieldValues();
            List<DatasetFieldValue> actVals = act.getDatasetFieldValues();
            if ( exVals.size() != actVals.size() ) return false;
            Iterator<DatasetFieldValue> exItr = exVals.iterator();
            for ( DatasetFieldValue actVal : actVals ) {
                DatasetFieldValue exVal = exItr.next();
                if ( ! exVal.getValue().equals(actVal.getValue()) ) {
                    return false;
                }
            }
            return true;
            
        } else if ( ex.getDatasetFieldType().isControlledVocabulary() ) {
            List<ControlledVocabularyValue> exVals = ex.getControlledVocabularyValues();
            List<ControlledVocabularyValue> actVals = act.getControlledVocabularyValues();
            if ( exVals.size() != actVals.size() ) return false;
            Iterator<ControlledVocabularyValue> exItr = exVals.iterator();
            for ( ControlledVocabularyValue actVal : actVals ) {
                ControlledVocabularyValue exVal = exItr.next();
                if ( ! exVal.getId().equals(actVal.getId()) ) {
                    return false;
                }
            }
            return true;
            
        } else if ( ex.getDatasetFieldType().isCompound() ) {
            List<DatasetFieldCompoundValue> exVals = ex.getDatasetFieldCompoundValues();
            List<DatasetFieldCompoundValue> actVals = act.getDatasetFieldCompoundValues();
            if ( exVals.size() != actVals.size() ) return false;
            Iterator<DatasetFieldCompoundValue> exItr = exVals.iterator();
            for ( DatasetFieldCompoundValue actVal : actVals ) {
                DatasetFieldCompoundValue exVal = exItr.next();
                Iterator<DatasetField> exChildItr = exVal.getChildDatasetFields().iterator();
                Iterator<DatasetField> actChildItr = actVal.getChildDatasetFields().iterator();
                while( exChildItr.hasNext() ) {
                    assertFieldsEqual(exChildItr.next(), actChildItr.next());
                }
            }
            return true;
            
        }
        
        throw new IllegalArgumentException("Unknown dataset field type '" + ex.getDatasetFieldType() + "'");
    }
    
    private static class MockSettingsSvc extends SettingsServiceBean {
        @Override
        public String getValueForKey( Key key /*, String defaultValue */) {
            switch (key) {
                case Authority:
                    return "10.5072";
                case Protocol:
                    return "doi";
                default:
                    break;
            }
             return null;
        }

        @Override
        public boolean isTrueForKey(Key key, boolean safeDefaultIfKeyNotFound) {
            if (key == Key.AllowCustomTermsOfUse) {
                return false;
            }
            return safeDefaultIfKeyNotFound;
        }
    }

    @Test
    public void testEnum() throws JsonParseException {
        JsonArrayBuilder arr = Json.createArrayBuilder();
        for (Type entry : Arrays.asList(Type.REVOKEROLE, Type.ASSIGNROLE)) {
            arr.add(entry.name());
        }
        Set<Type> typesSet = new HashSet<>(JsonParser.parseEnumsFromArray(arr.build(), Type.class));
        assertEquals(2, typesSet.size(), "Set contains two elements");
        assertTrue(typesSet.contains(Type.REVOKEROLE), "Set contains REVOKEROLE");
        assertTrue(typesSet.contains(Type.ASSIGNROLE), "Set contains ASSIGNROLE");
    }
}
