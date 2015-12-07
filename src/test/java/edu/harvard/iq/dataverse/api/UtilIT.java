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
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.path.xml.XmlPath.from;

public class UtilIT {

    private static final Logger logger = Logger.getLogger(UtilIT.class.getCanonicalName());

    public static final String API_TOKEN_HTTP_HEADER = "X-Dataverse-key";
    private static final String USERNAME_KEY = "userName";
    private static final String EMAIL_KEY = "email";
    private static final String API_TOKEN_KEY = "apiToken";
    private static final String BUILTIN_USER_KEY = "burrito";
    private static final String EMPTY_STRING = "";

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
        Response createDatasetResponse = given()
                .auth().basic(apiToken, EMPTY_STRING)
                .body(xmlIn)
                .contentType("application/atom+xml")
                .post("/dvn/api/data-deposit/v1.1/swordv2/collection/dataverse/" + dataverseToCreateDatasetIn);
        return createDatasetResponse;
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
                .delete("/dvn/api/data-deposit/v1.1/swordv2/edit/study/" + persistentId);
    }

}
