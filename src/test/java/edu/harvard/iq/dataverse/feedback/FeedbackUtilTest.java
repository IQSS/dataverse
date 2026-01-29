package edu.harvard.iq.dataverse.feedback;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileCategory;
import edu.harvard.iq.dataverse.DataFileTag;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseContact;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.dataset.DatasetTypeServiceBean;
import edu.harvard.iq.dataverse.license.LicenseServiceBean;
import edu.harvard.iq.dataverse.mocks.MockDatasetFieldSvc;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.Mockito;

public class FeedbackUtilTest {

    private static final String installationBrandName = "LibraScholar";
    private static final String supportTeamName = "LibraScholar SWAT Team";
    private static final String baseUrl = "https://dataverse.librascholar.edu";
    private static final String userEmail = "personClickingContactOrSupportButton@example.com";
    private static final DataverseSession dataverseSessionNull = null;
    private static DataverseSession dataverseSessionAuthenticated;
    private static DatasetVersion dsVersion;
    private static DatasetVersion dsVersion2;
    private static DatasetVersion dsVersionNoContacts;
    private static MockDatasetFieldSvc datasetFieldTypeSvc;
    private static InternetAddress systemAddress;
    private static final SettingsServiceBean settingsService = Mockito.mock(SettingsServiceBean.class);
    private static final LicenseServiceBean licenseService = Mockito.mock(LicenseServiceBean.class);
    private static final DatasetTypeServiceBean datasetTypeService = Mockito.mock(DatasetTypeServiceBean.class);
    private static final String systemEmail = "support@librascholar.edu";
    private static final boolean weKnowHowToCreateMockAuthenticatedUsers = false;

    @BeforeAll
    public static void setUpClass() throws IOException, JsonParseException, AddressException {

        if (weKnowHowToCreateMockAuthenticatedUsers) {
            dataverseSessionAuthenticated = new DataverseSession();
            AuthenticatedUser authenticatedUser = MocksFactory.makeAuthenticatedUser("First", "Last");
            dataverseSessionAuthenticated.setUser(authenticatedUser);
        }

        systemAddress = new InternetAddress(systemEmail, supportTeamName);

        datasetFieldTypeSvc = new MockDatasetFieldSvc();
        datasetFieldTypeSvc.setMetadataBlock("citation");
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

        JsonParser jsonParser = new JsonParser(datasetFieldTypeSvc, null, settingsService, licenseService, datasetTypeService);
        dsVersion = jsonParser.parseDatasetVersion(json1.getJsonObject("datasetVersion"));

        File datasetVersionJson2 = new File("tests/data/datasetContacts1.json");
        String datasetVersionAsJson2 = new String(Files.readAllBytes(Paths.get(datasetVersionJson2.getAbsolutePath())));

        JsonReader jsonReader12 = Json.createReader(new StringReader(datasetVersionAsJson2));
        JsonObject json12 = jsonReader12.readObject();

        JsonParser jsonParser2 = new JsonParser(datasetFieldTypeSvc, null, settingsService, licenseService, datasetTypeService);
        dsVersion2 = jsonParser2.parseDatasetVersion(json12.getJsonObject("datasetVersion"));

        File datasetVersionJsonNoContacts = new File("tests/data/datasetNoContacts.json");
        String datasetVersionAsJsonNoContacts = new String(Files.readAllBytes(Paths.get(datasetVersionJsonNoContacts.getAbsolutePath())));
        JsonReader jsonReaderNoContacts = Json.createReader(new StringReader(datasetVersionAsJsonNoContacts));
        JsonObject jsonNoContacts = jsonReaderNoContacts.readObject();
        JsonParser jsonParserNoContacts = new JsonParser(datasetFieldTypeSvc, null, settingsService, licenseService, datasetTypeService);
        dsVersionNoContacts = jsonParserNoContacts.parseDatasetVersion(jsonNoContacts.getJsonObject("datasetVersion"));

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
        System.out.println("first gather feedback");
        Feedback feedback1 = FeedbackUtil.gatherFeedback(dataverse, dataverseSessionNull, messageSubject, userMessage, systemAddress, userEmail, baseUrl, installationBrandName, supportTeamName, true);
        assertEquals(installationBrandName + " contact: " + messageSubject, feedback1.getSubject());
        String expectedBody
                = "You have just been sent the following message from " + userEmail + " "
                + "via the " + installationBrandName + " hosted dataverse named \"dvAlias1\":\n\n"
                + "---\n\n"
                + userMessage + "\n\n"
                + "---\n\n"
                + supportTeamName + "\n"
                + systemEmail + "\n\n"
                + "Go to dataverse https://dataverse.librascholar.edu/dataverse/dvAlias1\n\n"
                + "You received this email because you have been listed as a contact for the dataverse. "
                + "If you believe this was an error, please contact "
                + supportTeamName + " at " + systemEmail + ". "
                + "To respond directly to the individual who sent the message, simply reply to this email.";
        System.out.println("body:\n\n" + feedback1.getBody());
        assertEquals(expectedBody, feedback1.getBody());
        assertEquals("dvContact1@librascholar.edu,dvContact2@librascholar.edu", feedback1.getToEmail());
        assertEquals(systemEmail, feedback1.getCcEmail());
        
        assertEquals("personClickingContactOrSupportButton@example.com", feedback1.getFromEmail());
        JsonObject jsonObject = feedback1.toJsonObjectBuilder().build();
        System.out.println("json: " + jsonObject);
        assertEquals("personClickingContactOrSupportButton@example.com", jsonObject.getString("fromEmail"));
        assertEquals("dvContact1@librascholar.edu,dvContact2@librascholar.edu", jsonObject.getString("toEmail"));
        assertEquals(systemEmail, jsonObject.getString("ccEmail"));
        assertEquals(installationBrandName + " contact: " + "nice dataverse", jsonObject.getString("subject"));
        dataverse.setDataverseContacts(new ArrayList<>());
        System.out.println("second gather feedback");
        Feedback feedback2 = FeedbackUtil.gatherFeedback(dataverse, dataverseSessionNull, messageSubject, userMessage, systemAddress, userEmail, baseUrl, installationBrandName, supportTeamName, false);
        System.out.println("feedbacks2: " + feedback2);
        assertEquals(systemEmail, feedback2.getToEmail());
        assertEquals(null, feedback2.getCcEmail());
        System.out.println("body:\n\n" + feedback2.getBody());
        assertTrue(feedback2.getBody().startsWith("There is no contact address on file for this dataverse so this message is being sent to the system address."));
    }

    @Test
    public void testGatherFeedbackOnDataset() {
        Dataset dataset = new Dataset();

        List<DatasetVersion> versions = new ArrayList<>();
        System.out.println("dsversion: " + dsVersion2);
        DatasetVersion datasetVersionIn = dsVersion2;
        datasetVersionIn.setVersionState(DatasetVersion.VersionState.RELEASED);
        versions.add(datasetVersionIn);
        dataset.setVersions(versions);

        dataset.setProtocol("doi");
        dataset.setAuthority("10.7910/DVN");
        dataset.setIdentifier("TJCLKP");
        Dataverse dataverse = new Dataverse();
        dataset.setOwner(dataverse);

        DataverseSession dataverseSession = null;
        String messageSubject = "nice dataset";
        String userMessage = "Let's talk!";
        Feedback feedback = FeedbackUtil.gatherFeedback(dataset, dataverseSession, messageSubject, userMessage, systemAddress, userEmail, baseUrl, installationBrandName, supportTeamName, true);
        System.out.println("feedbacks: " + feedback);
        System.out.println("Subject: " + feedback.getSubject());
        System.out.println("Body: " + feedback.getBody());
        System.out.println("From: " + feedback.getFromEmail());
        System.out.println("To: " + feedback.getToEmail());
        System.out.println("CC: " + feedback.getCcEmail());

        assertEquals("ContactEmail1@mailinator.com,ContactEmail2@mailinator.com", feedback.getToEmail());
        assertEquals(systemEmail, feedback.getCcEmail());
        assertEquals(installationBrandName + " contact: " + messageSubject, feedback.getSubject());
        String expected = "Hello Tom Brady and Homer Simpson,\n\n"
                // FIXME: change from "personClickingContactOrSupportButton@example.com" to "Homer Simpson" or whatever (add to contact form).
                + "You have just been sent the following message from " + userEmail + " "
                + "via the " + installationBrandName + " hosted dataset "
                + "titled \"Darwin's Finches\" (doi:10.7910/DVN/TJCLKP):\n\n"
                + "---\n\n"
                + userMessage + "\n\n"
                + "---\n\n"
                + supportTeamName + "\n"
                + systemEmail + "\n\n"
                + "Go to dataset https://dataverse.librascholar.edu/dataset.xhtml?persistentId=doi:10.7910/DVN/TJCLKP\n\n"
                + "You received this email because you have been listed as a contact for the dataset. If you believe this was an error, please contact " + supportTeamName + " at " + systemEmail + ". To respond directly to the individual who sent the message, simply reply to this email.";
        System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
        System.out.println("EXPECTED:\n\n" + expected);
        System.out.println("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
        System.out.println("ACTUAL:\n\n" + feedback.getBody());
        assertEquals(expected, feedback.getBody());
    }

    @Test
    public void testGatherFeedbackOnDatasetNoContacts() {
        Dataset dataset = new Dataset();

        List<DatasetVersion> versions = new ArrayList<>();
        DatasetVersion datasetVersionIn = dsVersionNoContacts;
        datasetVersionIn.setVersionState(DatasetVersion.VersionState.RELEASED);
        versions.add(datasetVersionIn);
        dataset.setVersions(versions);

        dataset.setProtocol("doi");
        dataset.setAuthority("10.7910/DVN");
        dataset.setIdentifier("TJCLKP");
        Dataverse dataverse = new Dataverse();
        dataset.setOwner(dataverse);

        DataverseSession dataverseSession = null;
        String messageSubject = "nice dataset";
        String userMessage = "Let's talk!";
        Feedback feedback = FeedbackUtil.gatherFeedback(dataset, dataverseSession, messageSubject, userMessage, systemAddress, userEmail, baseUrl, installationBrandName, supportTeamName, false);
        System.out.println("Subject: " + feedback.getSubject());
        System.out.println("Body: " + feedback.getBody());
        System.out.println("From: " + feedback.getFromEmail());
        System.out.println("To: " + feedback.getToEmail());
        System.out.println("CC: " + feedback.getCcEmail());

        assertEquals(systemEmail, feedback.getToEmail());
        assertEquals(null, feedback.getCcEmail());
        assertEquals(installationBrandName + " contact: " + messageSubject, feedback.getSubject());
        String expected = "There is no contact address on file for this dataset so this message is being sent to the system address.\n\n"
                // FIXME: Add more context for person who receives systemEmail messages.
                // FIXME: change from "personClickingContactOrSupportButton@example.com" to "Homer Simpson" or whatever (add to contact form).
                //                + "You have just been sent the following message from " + feedback.getFromEmail() + " "
                //                + "via the " + installationBrandName + " hosted dataset "
                //                + "titled \"Darwin's Finches\" (doi:10.7910/DVN/TJCLKP):\n\n"
                + "---\n\n"
                + userMessage + "\n\n"
                + "---\n\n"
                + supportTeamName + "\n"
                + systemEmail + "\n\n"
                + "Go to dataset https://dataverse.librascholar.edu/dataset.xhtml?persistentId=doi:10.7910/DVN/TJCLKP\n\n"
                + "You received this email because you have been listed as a contact for the dataset. If you believe this was an error, please contact " + supportTeamName + " at " + systemEmail + ". To respond directly to the individual who sent the message, simply reply to this email.";
        System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
        System.out.println("EXPECTED:\n\n" + expected);
        System.out.println("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
        System.out.println("ACTUAL:\n\n" + feedback.getBody());
        assertEquals(expected, feedback.getBody());
    }

    @Test
    public void testGatherFeedbackOnFile() {

        // TODO: Consider switching to MocksFactory.makeDataFile()
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
        fmd.setLabel("file.txt");
        List<DataFileCategory> fileCategories = new ArrayList<>();
        DataFileCategory dataFileCategory = new DataFileCategory();
        dataFileCategory.setName("Data");
        fileCategories.add(dataFileCategory);
        fmd.setCategories(fileCategories);
        List<FileMetadata> fileMetadatas = new ArrayList<>();
        fileMetadatas.add(fmd);
        dataFile.setFileMetadatas(fileMetadatas);;
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
        Feedback feedback = FeedbackUtil.gatherFeedback(dataFile, dataverseSessionNull, messageSubject, userMessage, systemAddress, userEmail, baseUrl, installationBrandName, supportTeamName, false);
        System.out.println("feedback: " + feedback);
        System.out.println("Subject: " + feedback.getSubject());
        System.out.println("Body: " + feedback.getBody());
        System.out.println("From: " + feedback.getFromEmail());
        System.out.println("To: " + feedback.getToEmail());
        assertEquals(installationBrandName + " contact: " + messageSubject, feedback.getSubject());
        assertEquals("finch@mailinator.com", feedback.getToEmail());
        String expectedBody
                = "Attention Dataset Contact:\n\n"
                + "You have just been sent the following message from " + userEmail + " "
                + "via the LibraScholar hosted file named \"file.txt\" "
                + "from the dataset titled \"Darwin's Finches\" (doi:10.7910/DVN/TJCLKP):\n\n"
                + "---\n\n"
                + userMessage + "\n\n"
                + "---\n\n"
                + supportTeamName + "\n"
                + systemEmail + "\n\n"
                + "Go to file https://dataverse.librascholar.edu/file.xhtml?fileId=42\n\n"
                + "You received this email because you have been listed as a contact for the dataset. If you believe this was an error, please contact " + supportTeamName + " at " + systemEmail + ". To respond directly to the individual who sent the message, simply reply to this email.";;
        System.out.println("body:\n\n" + feedback.getBody());
        assertEquals(expectedBody, feedback.getBody());

    }

    @Test
    public void testGatherFeedbackOnFileNoContacts() {

        // TODO: Consider switching to MocksFactory.makeDataFile()
        FileMetadata fmd = new FileMetadata();
//        DatasetVersion dsVersion = new DatasetVersion();
        DataFile dataFile = new DataFile();

        dataFile.setId(42l);
        List<DataFileTag> dataFileTags = new ArrayList<>();
        DataFileTag tag = new DataFileTag();
        tag.setTypeByLabel("Survey");
        dataFileTags.add(tag);
        dataFile.setTags(dataFileTags);
        fmd.setDatasetVersion(dsVersionNoContacts);
        fmd.setDataFile(dataFile);
        fmd.setLabel("file.txt");
        List<DataFileCategory> fileCategories = new ArrayList<>();
        DataFileCategory dataFileCategory = new DataFileCategory();
        dataFileCategory.setName("Data");
        fileCategories.add(dataFileCategory);
        fmd.setCategories(fileCategories);
        List<FileMetadata> fileMetadatas = new ArrayList<>();
        fileMetadatas.add(fmd);
        dataFile.setFileMetadatas(fileMetadatas);;
        Dataset dataset = new Dataset();
        dataFile.setOwner(dataset);

        List<DatasetVersion> versions = new ArrayList<>();
        DatasetVersion datasetVersionIn = dsVersionNoContacts;
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
        Feedback feedback = FeedbackUtil.gatherFeedback(dataFile, dataverseSessionNull, messageSubject, userMessage, systemAddress, userEmail, baseUrl, installationBrandName, supportTeamName, false);
        System.out.println("feedback: " + feedback);
        System.out.println("Subject: " + feedback.getSubject());
        System.out.println("Body: " + feedback.getBody());
        System.out.println("From: " + feedback.getFromEmail());
        System.out.println("To: " + feedback.getToEmail());
        assertEquals(installationBrandName + " contact: " + messageSubject, feedback.getSubject());
        assertEquals("support@librascholar.edu", feedback.getToEmail());
        // TODO: Consider doing a more thorough test that just "starts with".
        assertTrue(feedback.getBody().startsWith("There is no contact address on file for this dataset so this message is being sent to the system address."));
    }

    @Test
    public void testGatherFeedbackFromSupportButtonNullSession() {
        String messageSubject = "I'm clicking the support button.";
        String userMessage = "Help!";
        DvObject nullDvObject = null;
        Feedback feedback = FeedbackUtil.gatherFeedback(nullDvObject, dataverseSessionNull, messageSubject, userMessage, systemAddress, userEmail, baseUrl, installationBrandName, supportTeamName, false);
        assertEquals(installationBrandName + " support request: " + messageSubject, feedback.getSubject());
        String expectedBody
                = "LibraScholar SWAT Team,\n\n"
                + "The following message was sent from " + userEmail + ".\n\n"
                + "---\n\n"
                + "Help!\n\n"
                + "---\n\n"
                + "Message sent from Support contact form."
                + "";
        System.out.println("body:\n\n" + feedback.getBody());
        assertEquals(expectedBody, feedback.getBody());
        assertEquals("support@librascholar.edu", feedback.getToEmail());
        assertEquals("personClickingContactOrSupportButton@example.com", feedback.getFromEmail());
        InternetAddress nullSystemAddress = null;
        Feedback feedback2 = FeedbackUtil.gatherFeedback(nullDvObject, dataverseSessionNull, messageSubject, userMessage, nullSystemAddress, userEmail, baseUrl, installationBrandName, supportTeamName, false);
        assertEquals(null, feedback2.getToEmail());
        String nullUserMessage = null;
        Feedback feedback3 = FeedbackUtil.gatherFeedback(nullDvObject, dataverseSessionNull, messageSubject, nullUserMessage, nullSystemAddress, userEmail, baseUrl, installationBrandName, supportTeamName, false);
        assertEquals(null, feedback3.getToEmail());
    }

    @Test
    public void testGatherFeedbackFromSupportButtonLoggedIn() {
        if (!weKnowHowToCreateMockAuthenticatedUsers) {
            return;
        }
        String messageSubject = "I'm clicking the support button.";
        String userMessage = "Help!";
        DvObject dvObject = null;
        Feedback feedback = FeedbackUtil.gatherFeedback(dvObject, dataverseSessionAuthenticated, messageSubject, userMessage, systemAddress, userEmail, baseUrl, installationBrandName, supportTeamName, false);
        assertEquals(messageSubject, feedback.getSubject());
        assertEquals("Help!", feedback.getBody());
        assertEquals("support@librascholar.edu", feedback.getToEmail());
        assertEquals("First.Last@someU.edu", feedback.getFromEmail());
    }

}
