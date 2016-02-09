package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import java.util.UUID;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import edu.harvard.iq.dataverse.api.datadeposit.SwordConfigurationImpl;
import com.jayway.restassured.path.xml.XmlPath;
import org.junit.Test;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.path.xml.XmlPath.from;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class UtilIT {

    private static final Logger logger = Logger.getLogger(UtilIT.class.getCanonicalName());

    public static final String API_TOKEN_HTTP_HEADER = "X-Dataverse-key";
    private static final String USERNAME_KEY = "userName";
    private static final String EMAIL_KEY = "email";
    private static final String API_TOKEN_KEY = "apiToken";
    private static final String BUILTIN_USER_KEY = "burrito";
    private static final String EMPTY_STRING = "";

    private static SwordConfigurationImpl swordConfiguration = new SwordConfigurationImpl();

    static String getRestAssuredBaseUri() {
        String saneDefaultInDev = "http://localhost:8080";
        String restAssuredBaseUri = saneDefaultInDev;
        String specifiedUri = System.getProperty("dataverse.test.baseurl");
        if (specifiedUri != null) {
            restAssuredBaseUri = specifiedUri;
        }
        logger.info("Base URL for tests: " + restAssuredBaseUri);
        return restAssuredBaseUri;
    }

    public static Response createRandomUser() {
        String randomString = getRandomUsername();
        logger.info("Creating random test user " + randomString);
        String userAsJson = getUserAsJsonString(randomString, randomString, randomString);
        String password = getPassword(userAsJson);
        Response response = given()
                .body(userAsJson)
                .contentType(ContentType.JSON)
                .post("/api/builtin-users?key=" + BUILTIN_USER_KEY + "&password=" + password);
        return response;
    }

    private static String getUserAsJsonString(String username, String firstName, String lastName) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(USERNAME_KEY, username);
        builder.add("firstName", firstName);
        builder.add("lastName", lastName);
        builder.add(EMAIL_KEY, getEmailFromUserName(username));
        String userAsJson = builder.build().toString();
        logger.fine("User to create: " + userAsJson);
        return userAsJson;
    }

    private static String getEmailFromUserName(String username) {
        return username + "@mailinator.com";
    }

    private static String getPassword(String jsonStr) {
        String password = JsonPath.from(jsonStr).get(USERNAME_KEY);
        return password;
    }

    private static String getRandomUsername() {
        return "user" + getRandomIdentifier().substring(0, 8);
    }

    private static String getRandomIdentifier() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    static String getUsernameFromResponse(Response createUserResponse) {
        JsonPath createdUser = JsonPath.from(createUserResponse.body().asString());
        String username = createdUser.getString("data.user." + USERNAME_KEY);
        logger.info("Username found in create user response: " + username);
        return username;
    }

    static String getApiTokenFromResponse(Response createUserResponse) {
        JsonPath createdUser = JsonPath.from(createUserResponse.body().asString());
        String apiToken = createdUser.getString("data." + API_TOKEN_KEY);
        logger.info("API token found in create user response: " + apiToken);
        return apiToken;
    }

    static String getAliasFromResponse(Response createDataverseResponse) {
        JsonPath createdDataverse = JsonPath.from(createDataverseResponse.body().asString());
        String alias = createdDataverse.getString("data.alias");
        logger.info("Alias found in create dataverse response: " + alias);
        return alias;
    }

    static Integer getDatasetIdFromResponse(Response createDatasetResponse) {
        JsonPath createdDataset = JsonPath.from(createDatasetResponse.body().asString());
        int datasetId = createdDataset.getInt("data.id");
        logger.info("Id found in create dataset response: " + datasetId);
        return datasetId;
    }

    static String getDatasetPersistentIdFromResponse(Response createDatasetResponse) {
        String xml = createDatasetResponse.body().asString();
        String datasetSwordIdUrl = from(xml).get("entry.id");
        /**
         * @todo stop assuming the last 22 characters are the doi/globalId
         */
        return datasetSwordIdUrl.substring(datasetSwordIdUrl.length() - 22);
    }

    public static Response getServiceDocument(String apiToken) {
        Response response = given()
                .auth().basic(apiToken, EMPTY_STRING)
                .get(swordConfiguration.getBaseUrlPathCurrent() + "/service-document");
        return response;
    }

    static Response createDataverse(String alias, String apiToken) {
        JsonArrayBuilder contactArrayBuilder = Json.createArrayBuilder();
        contactArrayBuilder.add(Json.createObjectBuilder().add("contactEmail", getEmailFromUserName(getRandomIdentifier())));
        JsonArrayBuilder subjectArrayBuilder = Json.createArrayBuilder();
        subjectArrayBuilder.add("Other");
        JsonObject dvData = Json.createObjectBuilder()
                .add("alias", alias)
                .add("name", alias)
                .add("dataverseContacts", contactArrayBuilder)
                .add("dataverseSubjects", subjectArrayBuilder)
                .build();
        Response createDataverseResponse = given()
                .body(dvData.toString()).contentType(ContentType.JSON)
                .when().post("/api/dataverses/:root?key=" + apiToken);
        return createDataverseResponse;
    }

    static Response createRandomDataverse(String apiToken) {
        String alias = getRandomIdentifier();
        return createDataverse(alias, apiToken);
    }

    static Response createRandomDatasetViaNativeApi(String dataverseAlias, String apiToken) {
        String jsonIn = getDatasetJson();
        Response createDatasetResponse = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(jsonIn)
                .contentType("application/json")
                .post("/api/dataverses/" + dataverseAlias + "/datasets");
        return createDatasetResponse;
    }

    private static String getDatasetJson() {
        File datasetVersionJson = new File("scripts/search/tests/data/dataset-finch1.json");
        try {
            String datasetVersionAsJson = new String(Files.readAllBytes(Paths.get(datasetVersionJson.getAbsolutePath())));
            return datasetVersionAsJson;
        } catch (IOException ex) {
            Logger.getLogger(UtilIT.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    static Response createRandomDatasetViaSwordApi(String dataverseToCreateDatasetIn, String apiToken) {
        String xmlIn = getDatasetXml(getRandomIdentifier(), getRandomIdentifier(), getRandomIdentifier());
        return createDatasetViaSwordApiFromXML(dataverseToCreateDatasetIn, xmlIn, apiToken);
    }

    static Response createDatasetViaSwordApi(String dataverseToCreateDatasetIn, String title, String apiToken) {
        String xmlIn = getDatasetXml(title, getRandomIdentifier(), getRandomIdentifier());
        return createDatasetViaSwordApiFromXML(dataverseToCreateDatasetIn, xmlIn, apiToken);
    }

    private static Response createDatasetViaSwordApiFromXML(String dataverseToCreateDatasetIn, String xmlIn, String apiToken) {
        Response createDatasetResponse = given()
                .auth().basic(apiToken, EMPTY_STRING)
                .body(xmlIn)
                .contentType("application/atom+xml")
                .post(swordConfiguration.getBaseUrlPathCurrent() + "/collection/dataverse/" + dataverseToCreateDatasetIn);
        return createDatasetResponse;
    }

    static Response listDatasetsViaSword(String dataverseAlias, String apiToken) {
        Response response = given()
                .auth().basic(apiToken, EMPTY_STRING)
                .get(swordConfiguration.getBaseUrlPathCurrent() + "/collection/dataverse/" + dataverseAlias);
        return response;
    }

    static Response updateDatasetTitleViaSword(String persistentId, String newTitle, String apiToken) {
        String xmlIn = getDatasetXml(newTitle, getRandomIdentifier(), getRandomIdentifier());
        Response updateDatasetResponse = given()
                .auth().basic(apiToken, EMPTY_STRING)
                .body(xmlIn)
                .contentType("application/atom+xml")
                .put(swordConfiguration.getBaseUrlPathCurrent() + "/edit/study/" + persistentId);
        return updateDatasetResponse;
    }

    static private String getDatasetXml(String title, String author, String description) {
        String xmlIn = "<?xml version=\"1.0\"?>\n"
                + "<entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:dcterms=\"http://purl.org/dc/terms/\">\n"
                + "   <dcterms:title>" + title + "</dcterms:title>\n"
                + "   <dcterms:creator>" + author + "</dcterms:creator>\n"
                + "   <dcterms:description>" + description + "</dcterms:description>\n"
                + "</entry>\n"
                + "";
        return xmlIn;
    }

    public static Response uploadRandomFile(String persistentId, String apiToken) {
        String zipfilename = "trees.zip";
        String pathToFileName = "scripts/search/data/binary/" + zipfilename;
        byte[] bytes = null;
        try {
            bytes = Files.readAllBytes(Paths.get(pathToFileName));
            logger.info("Number of bytes to upload: " + bytes.length);
        } catch (IOException ex) {
            throw new RuntimeException("Problem getting bytes from " + pathToFileName + ": " + ex);
        }
        Response swordStatementResponse = given()
                .body(bytes)
                .header("Packaging", "http://purl.org/net/sword/package/SimpleZip")
                .header("Content-Disposition", "filename=" + zipfilename)
                /**
                 * It's unclear why we need to add "preemptive" to auth but
                 * without it we can't use send bytes using the body/content
                 * method. See
                 * https://github.com/jayway/rest-assured/issues/507#issuecomment-162963787
                 */
                .auth().preemptive().basic(apiToken, EMPTY_STRING)
                .post(swordConfiguration.getBaseUrlPathCurrent() + "/edit-media/study/" + persistentId);
        return swordStatementResponse;

    }

    static Response downloadFile(Integer fileId) {
        return given()
                //                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/access/datafile/" + fileId);
    }

    static Response downloadFile(Integer fileId, String apiToken) {
        return given()
                /**
                 * Data Access API does not support X-Dataverse-key header -
                 * https://github.com/IQSS/dataverse/issues/2662
                 */
                //.header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/access/datafile/" + fileId + "?key=" + apiToken);
    }

    static Response getSwordAtomEntry(String persistentId, String apiToken) {
        Response response = given()
                .auth().basic(apiToken, EMPTY_STRING)
                .get(swordConfiguration.getBaseUrlPathCurrent() + "/edit/study/" + persistentId);
        return response;
    }

    static Response getSwordStatement(String persistentId, String apiToken) {
        Response swordStatementResponse = given()
                .auth().basic(apiToken, EMPTY_STRING)
                .get(swordConfiguration.getBaseUrlPathCurrent() + "/statement/study/" + persistentId);
        return swordStatementResponse;
    }

    static Integer getFileIdFromSwordStatementResponse(Response swordStatement) {
        Integer fileId = getFileIdFromSwordStatementBody(swordStatement.body().asString());
        return fileId;
    }

    private static Integer getFileIdFromSwordStatementBody(String swordStatement) {
        XmlPath xmlPath = new XmlPath(swordStatement);
        try {
            String fileIdAsString = xmlPath.get("feed.entry[0].id").toString().split("/")[10];
            Integer fileIdAsInt = Integer.parseInt(fileIdAsString);
            return fileIdAsInt;
        } catch (IndexOutOfBoundsException ex) {
            return null;
        }
    }

    static String getFilenameFromSwordStatementResponse(Response swordStatement) {
        String filename = getFilenameFromSwordStatementResponse(swordStatement.body().asString());
        return filename;
    }

    private static String getFilenameFromSwordStatementResponse(String swordStatement) {
        XmlPath xmlPath = new XmlPath(swordStatement);
        try {
            String filename = xmlPath.get("feed.entry[0].id").toString().split("/")[11];
            return filename;
        } catch (IndexOutOfBoundsException ex) {
            return null;
        }
    }

    static String getTitleFromSwordStatementResponse(Response swordStatement) {
        return getTitleFromSwordStatement(swordStatement.getBody().asString());
    }

    private static String getTitleFromSwordStatement(String swordStatement) {
        return new XmlPath(swordStatement).getString("feed.title");
    }

    public static Response deleteUser(String username) {
        Response deleteUserResponse = given()
                .delete("/api/admin/authenticatedUsers/" + username + "/");
        return deleteUserResponse;
    }

    public static Response deleteDataverse(String doomed, String apiToken) {
        return given().delete("/api/dataverses/" + doomed + "?key=" + apiToken);
    }

    public static Response deleteDatasetViaNativeApi(Integer datasetId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("/api/datasets/" + datasetId);
    }

    static Response deleteDatasetViaSwordApi(String persistentId, String apiToken) {
        return given()
                .auth().basic(apiToken, EMPTY_STRING)
                .relaxedHTTPSValidation()
                .delete(swordConfiguration.getBaseUrlPathCurrent() + "/edit/study/" + persistentId);
    }

    static Response destroyDataset(Integer datasetId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("/api/datasets/" + datasetId);
    }

    static Response deleteFile(Integer fileId, String apiToken) {
        return given()
                .auth().basic(apiToken, EMPTY_STRING)
                .relaxedHTTPSValidation()
                .delete(swordConfiguration.getBaseUrlPathCurrent() + "/edit-media/file/" + fileId);
    }

    static Response publishDatasetViaSword(String persistentId, String apiToken) {
        return given()
                .auth().basic(apiToken, EMPTY_STRING)
                .header("In-Progress", "false")
                .post(swordConfiguration.getBaseUrlPathCurrent() + "/edit/study/" + persistentId);
    }

    static Response publishDataverseViaSword(String alias, String apiToken) {
        return given()
                .auth().basic(apiToken, EMPTY_STRING)
                .header("In-Progress", "false")
                .post(swordConfiguration.getBaseUrlPathCurrent() + "/edit/dataverse/" + alias);
    }

    static Response makeSuperUser(String username) {
        Response response = given().post("/api/admin/superuser/" + username);
        return response;
    }

    static Response reindexDataset(String persistentId) {
        Response response = given()
                .get("/api/admin/index/dataset?persistentId=" + persistentId);
        return response;
    }

    @Test
    public void testGetFileIdFromSwordStatementWithNoFiles() {
        String swordStatementWithNoFiles = "<feed xmlns=\"http://www.w3.org/2005/Atom\">\n"
                + "  <id>https://localhost:8080/dvn/api/data-deposit/v1.1/swordv2/edit/study/doi:10.5072/FK2/0TLRLH</id>\n"
                + "  <link href=\"https://localhost:8080/dvn/api/data-deposit/v1.1/swordv2/edit/study/doi:10.5072/FK2/0TLRLH\" rel=\"self\"/>\n"
                + "  <title type=\"text\">A Dataset Without Any Files</title>\n"
                + "  <author>\n"
                + "    <name>Fileless, Joe</name>\n"
                + "  </author>\n"
                + "  <updated>2015-12-08T15:30:50.865Z</updated>\n"
                + "  <category term=\"latestVersionState\" scheme=\"http://purl.org/net/sword/terms/state\" label=\"State\">DRAFT</category>\n"
                + "  <category term=\"locked\" scheme=\"http://purl.org/net/sword/terms/state\" label=\"State\">false</category>\n"
                + "  <category term=\"isMinorUpdate\" scheme=\"http://purl.org/net/sword/terms/state\" label=\"State\">true</category>\n"
                + "</feed>";
        Integer fileId = getFileIdFromSwordStatementBody(swordStatementWithNoFiles);
        assertNull(fileId);
    }

    @Test
    public void testSwordStatementWithFiles() {
        String swordStatementWithNoFiles = "<feed xmlns=\"http://www.w3.org/2005/Atom\">\n"
                + "  <id>https://localhost:8080/dvn/api/data-deposit/v1.1/swordv2/edit/study/doi:10.5072/FK2/EUEW70</id>\n"
                + "  <link href=\"https://localhost:8080/dvn/api/data-deposit/v1.1/swordv2/edit/study/doi:10.5072/FK2/EUEW70\" rel=\"self\"/>\n"
                + "  <title type=\"text\">A Dataset with a File</title>\n"
                + "  <author>\n"
                + "    <name>Files, John</name>\n"
                + "  </author>\n"
                + "  <updated>2015-12-08T15:38:29.900Z</updated>\n"
                + "  <entry>\n"
                + "    <content type=\"application/zip\" src=\"https://localhost:8080/dvn/api/data-deposit/v1.1/swordv2/edit-media/file/174/trees.zip\"/>\n"
                + "    <id>https://localhost:8080/dvn/api/data-deposit/v1.1/swordv2/edit-media/file/174/trees.zip</id>\n"
                + "    <title type=\"text\">Resource https://localhost:8080/dvn/api/data-deposit/v1.1/swordv2/edit-media/file/174/trees.zip</title>\n"
                + "    <summary type=\"text\">Resource Part</summary>\n"
                + "    <updated>2015-12-08T15:38:30.089Z</updated>\n"
                + "  </entry>\n"
                + "  <category term=\"latestVersionState\" scheme=\"http://purl.org/net/sword/terms/state\" label=\"State\">DRAFT</category>\n"
                + "  <category term=\"locked\" scheme=\"http://purl.org/net/sword/terms/state\" label=\"State\">false</category>\n"
                + "  <category term=\"isMinorUpdate\" scheme=\"http://purl.org/net/sword/terms/state\" label=\"State\">true</category>\n"
                + "</feed>";
        Integer fileId = getFileIdFromSwordStatementBody(swordStatementWithNoFiles);
        assertNotNull(fileId);
        assertEquals(Integer.class, fileId.getClass());
        String title = getTitleFromSwordStatement(swordStatementWithNoFiles);
        assertEquals("A Dataset with a File", title);
    }

}
