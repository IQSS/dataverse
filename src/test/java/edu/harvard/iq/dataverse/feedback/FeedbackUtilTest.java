package edu.harvard.iq.dataverse.feedback;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileCategory;
import edu.harvard.iq.dataverse.DataFileTag;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseContact;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;

public class FeedbackUtilTest {

    private static String baseUrl = "https://dataverse.librascholar.edu";
    private static String userEmail = "personClickingContactOrSupportButton@example.com";
    private static DataverseSession dataverseSessionNull = null;
    private static DataverseSession dataverseSessionAuthenticated;
    private static DatasetVersion dsVersion;
    private static MockDatasetFieldSvc datasetFieldTypeSvc;
    private static InternetAddress systemAddress;
    private static boolean weKnowHowToCreateMockAuthenticatedUsers = false;

    @BeforeClass
    public static void setUpClass() throws IOException, JsonParseException, AddressException {

        if (weKnowHowToCreateMockAuthenticatedUsers) {
            dataverseSessionAuthenticated = new DataverseSession();
            AuthenticatedUser authenticatedUser = MocksFactory.makeAuthenticatedUser("First", "Last");
            dataverseSessionAuthenticated.setUser(authenticatedUser);
        }

        String systemEmail = "support@librascholar.edu";
        systemAddress = new InternetAddress(systemEmail);

        datasetFieldTypeSvc = new MockDatasetFieldSvc();
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

        File datasetVersionJson = new File("src/test/resources/json/dataset-finch1.json");
        String datasetVersionAsJson = new String(Files.readAllBytes(Paths.get(datasetVersionJson.getAbsolutePath())));

        JsonReader jsonReader1 = Json.createReader(new StringReader(datasetVersionAsJson));
        JsonObject json1 = jsonReader1.readObject();

        JsonParser jsonParser = new JsonParser(datasetFieldTypeSvc, null, null);
        dsVersion = jsonParser.parseDatasetVersion(json1.getJsonObject("datasetVersion"));

        FeedbackUtil justForCodeCoverage = new FeedbackUtil();
    }

    @Test
    public void testGatherFeedbackOnDataverse() {
        Dataverse dataverse = new Dataverse();
        dataverse.setAlias("dvAlias1");
        List<DataverseContact> dataverseContacts = new ArrayList<>();
        dataverseContacts.add(new DataverseContact(dataverse, "dvContact1@librascholar.edu"));
        dataverseContacts.add(new DataverseContact(dataverse, "dvContact2@librascholar.edu"));
        dataverse.setDataverseContacts(dataverseContacts);
        String messageSubject = "nice dataverse";
        String userMessage = "Let's talk!";
//        String userEmail = "hasQuestion@example.com";
        Feedback feedback = FeedbackUtil.gatherFeedback(dataverse, dataverseSessionNull, messageSubject, userMessage, systemAddress, userEmail, baseUrl);
        assertEquals(messageSubject, feedback.getSubject());
        assertEquals("The message below was sent from the Contact button at https://dataverse.librascholar.edu/dataverse/dvAlias1\n\n" + userMessage, feedback.getBody());
        assertEquals("dvContact1@librascholar.edu,dvContact2@librascholar.edu", feedback.getToEmail());
        assertEquals("personClickingContactOrSupportButton@example.com", feedback.getFromEmail());
        dataverse.setDataverseContacts(new ArrayList<>());
        feedback = FeedbackUtil.gatherFeedback(dataverse, dataverseSessionNull, messageSubject, userMessage, systemAddress, userEmail, baseUrl);
        assertEquals("support@librascholar.edu", feedback.getToEmail());
    }

    @Test
    public void testGatherFeedbackOnDataset() {
        Dataset dataset = new Dataset();

        List<DatasetVersion> versions = new ArrayList<>();
        System.out.println("dsversion: " + dsVersion);
        DatasetVersion datasetVersionIn = dsVersion;
        datasetVersionIn.setVersionState(DatasetVersion.VersionState.RELEASED);
        versions.add(datasetVersionIn);
        dataset.setVersions(versions);

        dataset.setProtocol("doi");
        dataset.setAuthority("10.7910/DVN");
        dataset.setIdentifier("TJCLKP");
        Dataverse dataverse = new Dataverse();
        dataset.setOwner(dataverse);

        DataverseSession dataverseSession = null;
        String messageSubject = "nice file";
        String userMessage = "Let's talk!";
//        String userEmail = "hsimpson@mailinator.com";
        Feedback feedback = FeedbackUtil.gatherFeedback(dataset, dataverseSession, messageSubject, userMessage, systemAddress, userEmail, baseUrl);
        System.out.println("feedback: " + feedback);
        System.out.println("Subject: " + feedback.getSubject());
        System.out.println("Body: " + feedback.getBody());
        System.out.println("From: " + feedback.getFromEmail());
        System.out.println("To: " + feedback.getToEmail());
        assertEquals("finch@mailinator.com", feedback.getToEmail());
        assertEquals(messageSubject, feedback.getSubject());
        assertEquals("The message below was sent from the Contact button at https://dataverse.librascholar.edu/dataset.xhtml?persistentId=doi:10.7910/DVN/TJCLKP\n\n" + userMessage, feedback.getBody());
    }

    @Test
    public void testGatherFeedbackOnFile() {

        FileMetadata fmd = new FileMetadata();
//        DatasetVersion dsVersion = new DatasetVersion();
        DataFile dataFile = new DataFile();

        dataFile.setId(42l);
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
        Dataset dataset = new Dataset();
        dataFile.setOwner(dataset);

        List<DatasetVersion> versions = new ArrayList<>();
        System.out.println("dsversion: " + dsVersion);
        DatasetVersion datasetVersionIn = dsVersion;
        datasetVersionIn.setVersionState(DatasetVersion.VersionState.RELEASED);
        versions.add(datasetVersionIn);
        dataset.setVersions(versions);

        dataset.setProtocol("doi");
        dataset.setAuthority("10.7910/DVN");
        dataset.setIdentifier("TJCLKP");
        Dataverse dataverse = new Dataverse();
        dataset.setOwner(dataverse);

        String messageSubject = "nice file";
        String userMessage = "Let's talk!";
        Feedback feedback = FeedbackUtil.gatherFeedback(dataFile, dataverseSessionNull, messageSubject, userMessage, systemAddress, userEmail, baseUrl);
        System.out.println("feedback: " + feedback);
        System.out.println("Subject: " + feedback.getSubject());
        System.out.println("Body: " + feedback.getBody());
        System.out.println("From: " + feedback.getFromEmail());
        System.out.println("To: " + feedback.getToEmail());
        assertEquals(messageSubject, feedback.getSubject());
        assertEquals("finch@mailinator.com", feedback.getToEmail());
        assertEquals("The message below was sent from the Contact button at https://dataverse.librascholar.edu/file.xhtml?fileId=42\n\n" + userMessage, feedback.getBody());
    }

    @Test
    public void testGatherFeedbackFromSupportButtonNullSession() {
        String messageSubject = "I'm clicking the support button.";
        String userMessage = "Help!";
        DvObject dvObject = null;
        Feedback feedback = FeedbackUtil.gatherFeedback(dvObject, dataverseSessionNull, messageSubject, userMessage, systemAddress, userEmail, baseUrl);
        assertEquals(messageSubject, feedback.getSubject());
        assertEquals("Help!", feedback.getBody());
        assertEquals("support@librascholar.edu", feedback.getToEmail());
        assertEquals("personClickingContactOrSupportButton@example.com", feedback.getFromEmail());
        InternetAddress nullSystemAddress = null;
        feedback = FeedbackUtil.gatherFeedback(dvObject, dataverseSessionNull, messageSubject, userMessage, nullSystemAddress, userEmail, baseUrl);
        assertEquals(null, feedback.getToEmail());
        String nullUserMessage = null;
        feedback = FeedbackUtil.gatherFeedback(dvObject, dataverseSessionNull, messageSubject, nullUserMessage, nullSystemAddress, userEmail, baseUrl);
        assertEquals(null, feedback);
    }

    @Test
    public void testGatherFeedbackFromSupportButtonLoggedIn() {
        if (!weKnowHowToCreateMockAuthenticatedUsers) {
            return;
        }
        String messageSubject = "I'm clicking the support button.";
        String userMessage = "Help!";
        DvObject dvObject = null;
        Feedback feedback = FeedbackUtil.gatherFeedback(dvObject, dataverseSessionAuthenticated, messageSubject, userMessage, systemAddress, userEmail, baseUrl);
        assertEquals(messageSubject, feedback.getSubject());
        assertEquals("Help!", feedback.getBody());
        assertEquals("support@librascholar.edu", feedback.getToEmail());
        assertEquals("First.Last@someU.edu", feedback.getFromEmail());
    }

    // We are starting to accumulate a lot of these. See DDIExporterTest, SchemaDotOrgExporterTest, JsonParserTest, and JsonPrinterTest.
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

}
