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
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import com.jayway.restassured.specification.RequestSpecification;
import java.util.List;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import java.io.InputStream;
import edu.harvard.iq.dataverse.util.FileUtil;
import java.util.Base64;
import org.apache.commons.io.IOUtils;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.path.xml.XmlPath.from;
import java.nio.file.Path;
import java.util.ArrayList;
import org.apache.commons.lang3.math.NumberUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
    
    static Matcher<String> equalToCI( String value ) {
        String valueLc = value.toLowerCase();
        
        return new BaseMatcher<String>(){
            @Override
            public boolean matches(Object o) {
                if ( ! (o instanceof String) ) return false;
                return ((String)o).toLowerCase().equals(valueLc);
            }

            @Override
            public void describeTo(Description d) {
                d.appendText("Should be equal to (case insensitive): '"+value+"'");
            }
        };
    }
    
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

    /**
     * Begin each username with a prefix
     *
     * @param usernamePrefix
     * @return
     */
    public static Response createRandomUser(String usernamePrefix) {
        String randomString = getRandomUsername(usernamePrefix);
        logger.info("Creating random test user " + randomString);
        String userAsJson = getUserAsJsonString(randomString, randomString, randomString);
        String password = getPassword(userAsJson);
        Response response = given()
                .body(userAsJson)
                .contentType(ContentType.JSON)
                .post("/api/builtin-users?key=" + BUILTIN_USER_KEY + "&password=" + password);
        return response;
    }

    public static Response createRandomUser() {

        return createRandomUser("user");
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

    public static Response createRandomAuthenticatedUser(String authenticationProviderId) {
        String randomString = getRandomUsername();
        String userAsJson = getAuthenticatedUserAsJsonString(randomString, randomString, randomString, authenticationProviderId, "@" + randomString);
        logger.fine("createRandomAuthenticatedUser: " + userAsJson);
        Response response = given()
                .body(userAsJson)
                .contentType(ContentType.JSON)
                .post("/api/admin/authenticatedUsers");
        return response;
    }
    
    public static Response migrateDatasetIdentifierFromHDLToPId(String datasetIdentifier, String apiToken) {
        System.out.print(datasetIdentifier);
        Response response = given()
                .body(datasetIdentifier)
                .contentType(ContentType.JSON)
                .post("/api/admin/" + datasetIdentifier + "/reregisterHDLToPID?key=" + apiToken);
        return response;
    }

    private static String getAuthenticatedUserAsJsonString(String persistentUserId, String firstName, String lastName, String authenticationProviderId, String identifier) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("authenticationProviderId", authenticationProviderId);
        builder.add("persistentUserId", persistentUserId);
        builder.add("identifier", identifier);
        builder.add("firstName", firstName);
        builder.add("lastName", lastName);
        builder.add(EMAIL_KEY, getEmailFromUserName(persistentUserId));
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

    private static String getRandomUsername(String usernamePrefix) {

        if (usernamePrefix == null) {
            return getRandomUsername();
        }
        return usernamePrefix + getRandomIdentifier().substring(0, 8);
    }

    public static String getRandomString(int length) {
//is it worth replacing with something that doesn't error out on getRandomString(8)
        if (length < 0) {
            length = 3;
        }
        return getRandomIdentifier().substring(0, length + 1);
    }

    private static String getRandomUsername() {
        return "user" + getRandomIdentifier().substring(0, 8);
    }

    public static String getRandomIdentifier() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public static String getRandomDvAlias() {
        return "dv" + getRandomIdentifier();
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

    static Integer getDataverseIdFromResponse(Response createDataverseResponse) {
        JsonPath createdDataverse = JsonPath.from(createDataverseResponse.body().asString());
        int dataverseId = createdDataverse.getInt("data.id");
        logger.info("Id found in create dataverse response: " + createdDataverse);
        return dataverseId;
    }

    static Integer getDatasetIdFromResponse(Response createDatasetResponse) {
        JsonPath createdDataset = JsonPath.from(createDatasetResponse.body().asString());
        int datasetId = createdDataset.getInt("data.id");
        logger.info("Id found in create dataset response: " + datasetId);
        return datasetId;
    }

    static String getDatasetPersistentIdFromResponse(Response createDatasetResponse) {
        JsonPath createdDataset = JsonPath.from(createDatasetResponse.body().asString());
        String persistentDatasetId = createdDataset.getString("data.persistentId");
        logger.info("Persistent id found in create dataset response: " + persistentDatasetId);
        return persistentDatasetId;
    }
    
    static String getDatasetPersistentIdFromSwordResponse(Response createDatasetResponse) {
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

    static Response createDataverse(String alias, String category, String apiToken) {
        JsonArrayBuilder contactArrayBuilder = Json.createArrayBuilder();
        contactArrayBuilder.add(Json.createObjectBuilder().add("contactEmail", getEmailFromUserName(getRandomIdentifier())));
        JsonArrayBuilder subjectArrayBuilder = Json.createArrayBuilder();
        subjectArrayBuilder.add("Other");
        JsonObject dvData = Json.createObjectBuilder()
                .add("alias", alias)
                .add("name", alias)
                .add("dataverseContacts", contactArrayBuilder)
                .add("dataverseSubjects", subjectArrayBuilder)
                // don't send "dataverseType" if category is null, must be a better way
                .add(category != null ? "dataverseType" : "notTheKeyDataverseType", category != null ? category : "whatever")
                .build();
        Response createDataverseResponse = given()
                .body(dvData.toString()).contentType(ContentType.JSON)
                .when().post("/api/dataverses/:root?key=" + apiToken);
        return createDataverseResponse;
    }

    static Response createDataverse(JsonObject dvData, String apiToken) {
        Response createDataverseResponse = given()
                .body(dvData.toString()).contentType(ContentType.JSON)
                .when().post("/api/dataverses/:root?key=" + apiToken);
        return createDataverseResponse;
    }

    static Response createRandomDataverse(String apiToken) {
        // Add any string (i.e. "dv") to avoid possibility of "Alias should not be a number" https://github.com/IQSS/dataverse/issues/211
        String alias = "dv" + getRandomIdentifier();
        String category = null;
        return createDataverse(alias, category, apiToken);
    }

    static Response showDataverseContents(String alias, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .when().get("/api/dataverses/" + alias + "/contents");
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

    static Response createDatasetViaNativeApi(String dataverseAlias, String pathToJsonFile, String apiToken) {
        String jsonIn = getDatasetJson(pathToJsonFile);
        Response createDatasetResponse = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(jsonIn)
                .contentType("application/json")
                .post("/api/dataverses/" + dataverseAlias + "/datasets");
        return createDatasetResponse;
    }

    private static String getDatasetJson(String pathToJsonFile) {
        File datasetVersionJson = new File(pathToJsonFile);
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
        String xmlIn = getDatasetXml(title, "Lastname, Firstname", getRandomIdentifier());
        return createDatasetViaSwordApiFromXML(dataverseToCreateDatasetIn, xmlIn, apiToken);
    }

    static Response createDatasetViaSwordApi(String dataverseToCreateDatasetIn, String title, String description, String apiToken) {
        String xmlIn = getDatasetXml(title, "Lastname, Firstname", description);
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

    static Response migrateDataset(String filename, String parentDataverse, String apiToken) throws IOException {
        if (filename == null || filename.isEmpty()) {
            throw new IOException("null or empty filename");
        }
        logger.info(parentDataverse + "dataverse targeted for import of " + filename);
        Response response = given()
                .contentType("application/atom+xml")
                .get("/api/batch/migrate/?dv=" + parentDataverse + "&key=" + apiToken + "&path=" + filename + "&createDV=true");
        return response;
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

    // https://github.com/IQSS/dataverse/issues/3777
    static Response updateDatasetMetadataViaNative(String persistentId, String pathToJsonFile, String apiToken) {
        String jsonIn = getDatasetJson(pathToJsonFile);
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(jsonIn)
                .contentType("application/json")
                .put("/api/datasets/:persistentId/versions/:draft?persistentId=" + persistentId);
        return response;
    }
    
    // https://github.com/IQSS/dataverse/issues/3777
    static Response addDatasetMetadataViaNative(String persistentId, String pathToJsonFile, String apiToken) {
        String jsonIn = getDatasetJson(pathToJsonFile);
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(jsonIn)
                .contentType("application/json")
                .put("/api/datasets/:persistentId/editMetadata/?persistentId=" + persistentId);
        return response;
    }
    
    static Response deleteDatasetMetadataViaNative(String persistentId, String pathToJsonFile, String apiToken) {
        String jsonIn = getDatasetJson(pathToJsonFile);

        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(jsonIn)
                .contentType("application/json")
                .put("/api/datasets/:persistentId/deleteMetadata/?persistentId=" + persistentId);
        return response;
    }
    
    static Response updateFieldLevelDatasetMetadataViaNative(String persistentId, String pathToJsonFile, String apiToken) {
        String jsonIn = getDatasetJson(pathToJsonFile);
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(jsonIn)
                .contentType("application/json")
                .put("/api/datasets/:persistentId/editMetadata/?persistentId=" + persistentId + "&replace=true");
        return response;
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
    
    static Response createDatasetLink(Long linkedDatasetId, String linkingDataverseAlias, String apiToken) {
        Response response = given()
            .header(API_TOKEN_HTTP_HEADER, apiToken)
            .put("api/datasets/" + linkedDatasetId + "/link/" + linkingDataverseAlias);
        return response;
    } 
    
    static Response deleteDatasetLink(Long linkedDatasetId, String linkingDataverseAlias, String apiToken) {
        Response response = given()
            .header(API_TOKEN_HTTP_HEADER, apiToken)
            .delete("api/datasets/" + linkedDatasetId + "/deleteLink/" + linkingDataverseAlias);
        return response;
    } 

    public static Response uploadRandomFile(String persistentId, String apiToken) {
        String zipfilename = "trees.zip";
        return uploadFile(persistentId, zipfilename, apiToken);
    }

    /**
     * @deprecated switch to uploadZipFileViaSword where the path isn't hard
     * coded.
     */
    @Deprecated
    public static Response uploadFile(String persistentId, String zipfilename, String apiToken) {
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

    public static Response uploadZipFileViaSword(String persistentId, String pathToZipFile, String apiToken) {
        File file = new File(pathToZipFile);
        String zipfilename = file.getName();
        byte[] bytes = null;
        try {
            Path path = Paths.get(pathToZipFile);
            bytes = Files.readAllBytes(path);
            logger.info("Number of bytes to upload: " + bytes.length);
        } catch (IOException ex) {
            throw new RuntimeException("Problem getting bytes from " + pathToZipFile + ": " + ex);
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

    /**
     * For test purposes, datasetId can be non-numeric
     *
     * @param datasetId
     * @param pathToFile
     * @param apiToken
     * @return
     */
    static Response uploadFileViaNative(String datasetId, String pathToFile, String apiToken) {
        String jsonAsString = null;
        return uploadFileViaNative(datasetId, pathToFile, jsonAsString, apiToken);
    }

    static Response uploadFileViaNative(String datasetId, String pathToFile, JsonObject jsonObject, String apiToken) {
        return uploadFileViaNative(datasetId, pathToFile, jsonObject.toString(), apiToken);
    }

    static Response uploadFileViaNative(String datasetId, String pathToFile, String jsonAsString, String apiToken) {
        RequestSpecification requestSpecification = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .multiPart("datasetId", datasetId)
                .multiPart("file", new File(pathToFile));
        if (jsonAsString != null) {
            requestSpecification.multiPart("jsonData", jsonAsString);
        }
        return requestSpecification.post("/api/datasets/" + datasetId + "/add");
    }

    static Response replaceFile(String fileIdOrPersistentId, String pathToFile, String apiToken) {
        String jsonAsString = null;
        return replaceFile(fileIdOrPersistentId, pathToFile, jsonAsString, apiToken);
    }

    static Response replaceFile(String fileIdOrPersistentId, String pathToFile, JsonObject jsonObject, String apiToken) {
        return replaceFile(fileIdOrPersistentId, pathToFile, jsonObject.toString(), apiToken);
    }

    static Response replaceFile(String fileIdOrPersistentId, String pathToFile, String jsonAsString, String apiToken) {
        String idInPath = fileIdOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isNumber(fileIdOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + fileIdOrPersistentId;
        }
        RequestSpecification requestSpecification = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .multiPart("file", new File(pathToFile));
        if (jsonAsString != null) {
            requestSpecification.multiPart("jsonData", jsonAsString);
        }
        return requestSpecification
                .post("/api/files/" + idInPath + "/replace" + optionalQueryParam);
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

    static Response downloadFileOriginal(Integer fileId) {
        return given()
                .get("/api/access/datafile/" + fileId + "?format=original");
    }
    
    static Response downloadFileOriginal(Integer fileId, String apiToken) {
        return given()
                .get("/api/access/datafile/" + fileId + "?format=original&key=" + apiToken);
    }
    
    static Response downloadFiles(Integer[] fileIds) {
        String getString = "/api/access/datafiles/";
        for(Integer fileId : fileIds) {
            getString += fileId + ",";
        }
        return given().get(getString);
    }
    
    static Response downloadFiles(Integer[] fileIds, String apiToken) {
        String getString = "/api/access/datafiles/";
        for(Integer fileId : fileIds) {
            getString += fileId + ",";
        }
        return given().get(getString + "?key=" + apiToken);
    }
    
    static Response downloadFilesOriginal(Integer[] fileIds) {
        String getString = "/api/access/datafiles/";
        for(Integer fileId : fileIds) {
            getString += fileId + ",";
        }
        return given().get(getString + "?format=original");
    }
    
    static Response downloadFilesOriginal(Integer[] fileIds, String apiToken) {
        String getString = "/api/access/datafiles/";
        for(Integer fileId : fileIds) {
            getString += fileId + ",";
        }
        return given().get(getString + "?format=original&key=" + apiToken);
    }

    static Response subset(String fileId, String variables, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/access/datafile/" + fileId + "?format=subset&variables=" + variables);
    }

    static Response getFileMetadata(String fileIdOrPersistentId, String optionalFormat, String apiToken) {
        String idInPath = fileIdOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isNumber(fileIdOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "&persistentId=" + fileIdOrPersistentId;
        }
        String optionalFormatInPath = "";
        if (optionalFormat != null) {
            optionalFormatInPath = "/" + optionalFormat;
        }
        System.out.println("/api/access/datafile/" + idInPath + "/metadata" + optionalFormatInPath + "?key=" + apiToken + optionalQueryParam);
        return given()
                .urlEncodingEnabled(false)
                .get("/api/access/datafile/" + idInPath + "/metadata" + optionalFormatInPath + "?key=" + apiToken + optionalQueryParam);
    }

    static Response testIngest(String fileName, String fileType) {
        return given()
                .get("/api/ingest/test/file?fileName=" + fileName + "&fileType=" + fileType);
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
        String filename = getFirstFilenameFromSwordStatementResponse(swordStatement.body().asString());
        return filename;
    }

    private static String getFirstFilenameFromSwordStatementResponse(String swordStatement) {
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

    static Response createGroup(String dataverseToCreateGroupIn, String aliasInOwner, String displayName, String apiToken) {
        JsonObjectBuilder groupBuilder = Json.createObjectBuilder();
        groupBuilder.add("aliasInOwner", aliasInOwner);
        groupBuilder.add("displayName", displayName);
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(groupBuilder.build().toString())
                .contentType(ContentType.JSON)
                .post("/api/dataverses/" + dataverseToCreateGroupIn + "/groups");
        return response;
    }

    static Response getIpGroups() {
        Response response = given()
                .get("api/admin/groups/ip");
        return response;
    }

    static Response getIpGroup(String ipGroupIdentifier) {
        Response response = given()
                .get("api/admin/groups/ip/" + ipGroupIdentifier);
        return response;
    }

    static Response createIpGroup(JsonObject jsonObject) {
        Response response = given()
                .body(jsonObject.toString())
                .contentType(ContentType.JSON)
                .post("api/admin/groups/ip");
        return response;
    }

    static Response deleteIpGroup(String ipGroupIdentifier) {
        Response response = given()
                .delete("api/admin/groups/ip/" + ipGroupIdentifier);
        return response;
    }

    static Response addToGroup(String dataverseThatGroupBelongsIn, String groupIdentifier, List<String> roleAssigneesToAdd, String apiToken) {
        JsonArrayBuilder groupBuilder = Json.createArrayBuilder();
        roleAssigneesToAdd.stream().forEach((string) -> {
            groupBuilder.add(string);
        });
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(groupBuilder.build().toString())
                .contentType(ContentType.JSON)
                .post("/api/dataverses/" + dataverseThatGroupBelongsIn + "/groups/" + groupIdentifier + "/roleAssignees");
        return response;
    }

    static public Response grantRoleOnDataverse(String definitionPoint, String role, String roleAssignee, String apiToken) {
        JsonObjectBuilder roleBuilder = Json.createObjectBuilder();
        roleBuilder.add("assignee", roleAssignee);
        roleBuilder.add("role", role);
        JsonObject roleObject = roleBuilder.build();
        logger.info("Granting role on dataverse alias \"" + definitionPoint + "\": " + roleObject);
        return given()
                .body(roleObject.toString())
                .contentType(ContentType.JSON)
                .post("api/dataverses/" + definitionPoint + "/assignments?key=" + apiToken);
    }

    public static Response deleteUser(String username) {
        Response deleteUserResponse = given()
                .delete("/api/admin/authenticatedUsers/" + username + "/");
        return deleteUserResponse;
    }
    
    public static Response uningestFile(Long fileId, String apiToken) {
        
        Response uningestFileResponse = given()
                .post("/api/files/" + fileId + "/uningest/?key=" + apiToken);
        return uningestFileResponse;
    }

    public static Response deleteDataverse(String doomed, String apiToken) {
        return given().delete("/api/dataverses/" + doomed + "?key=" + apiToken);
    }

    public static Response deleteDatasetViaNativeApi(Integer datasetId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("/api/datasets/" + datasetId);
    }

    public static Response deleteDatasetVersionViaNativeApi(Integer datasetId, String versionId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("/api/datasets/" + datasetId + "/versions/" + versionId);
    }

    static Response deleteLatestDatasetVersionViaSwordApi(String persistentId, String apiToken) {
        return given()
                .auth().basic(apiToken, EMPTY_STRING)
                .relaxedHTTPSValidation()
                .delete(swordConfiguration.getBaseUrlPathCurrent() + "/edit/study/" + persistentId);
    }

    static Response destroyDataset(Integer datasetId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("/api/datasets/" + datasetId + "/destroy");
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

    static Response publishDatasetViaNativeApi(String idOrPersistentId, String majorOrMinor, String apiToken) {
        String idInPath = idOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isNumber(idOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "&persistentId=" + idOrPersistentId;
        }
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .urlEncodingEnabled(false)
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.post("/api/datasets/" + idInPath + "/actions/:publish?type=" + majorOrMinor + optionalQueryParam);
    }

    static Response publishDatasetViaNativeApiDeprecated(String persistentId, String majorOrMinor, String apiToken) {
        /**
         * @todo This should be a POST rather than a GET:
         * https://github.com/IQSS/dataverse/issues/2431
         */
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .urlEncodingEnabled(false)
                .get("/api/datasets/:persistentId/actions/:publish?type=" + majorOrMinor + "&persistentId=" + persistentId);
    }

    static Response publishDatasetViaNativeApi(Integer datasetId, String majorOrMinor, String apiToken) {
        /**
         * @todo This should be a POST rather than a GET:
         * https://github.com/IQSS/dataverse/issues/2431
         */
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .urlEncodingEnabled(false)
                .get("/api/datasets/" + datasetId + "/actions/:publish?type=" + majorOrMinor);
    }

    static Response publishDataverseViaSword(String alias, String apiToken) {
        return given()
                .auth().basic(apiToken, EMPTY_STRING)
                .header("In-Progress", "false")
                .post(swordConfiguration.getBaseUrlPathCurrent() + "/edit/dataverse/" + alias);
    }

    static Response publishDataverseViaNativeApi(String dataverseAlias, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .urlEncodingEnabled(false)
                .post("/api/dataverses/" + dataverseAlias + "/actions/:publish");
    }

    static Response submitDatasetForReview(String datasetPersistentId, String apiToken) {
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.post("/api/datasets/:persistentId/submitForReview?persistentId=" + datasetPersistentId);
    }

    static Response returnDatasetToAuthor(String datasetPersistentId, JsonObject jsonObject, String apiToken) {
        String jsonIn = jsonObject.toString();
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken)
                    .body(jsonIn)
                    .contentType("application/json");
        }
        return requestSpecification
                .post("/api/datasets/:persistentId/returnToAuthor?persistentId=" + datasetPersistentId);
    }

    static Response getNotifications(String apiToken) {
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.get("/api/notifications/all");
    }

    static Response nativeGetUsingPersistentId(String persistentId, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/datasets/:persistentId/?persistentId=" + persistentId);
        return response;
    }

    static Response getMetadataBlockFromDatasetVersion(String persistentId, String versionNumber, String metadataBlock, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/datasets/:persistentId/versions/:latest-published/metadata/citation?persistentId=" + persistentId);
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

    static Response listAuthenticatedUsers(String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/admin/authenticatedUsers");
        return response;
    }

    static Response getAuthenticatedUser(String userIdentifier, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/admin/authenticatedUsers/" + userIdentifier);
        return response;
    }

    /**
     * Used to the test the filter Authenticated Users API endpoint
     *
     * Note 1 : All params are optional for endpoint to work EXCEPT
     * superUserApiToken Note 2 : sortKey exists in API call but not currently
     * used
     *
     * @param apiToken
     * @return
     */
    static Response filterAuthenticatedUsers(String superUserApiToken,
            String searchTerm,
            Integer selectedPage,
            Integer itemsPerPage
    //                                         String sortKey
    ) {

        List<String> queryParams = new ArrayList<String>();
        if (searchTerm != null) {
            queryParams.add("searchTerm=" + searchTerm);
        }
        if (selectedPage != null) {
            queryParams.add("selectedPage=" + selectedPage.toString());
        }
        if (itemsPerPage != null) {
            queryParams.add("itemsPerPage=" + itemsPerPage.toString());
        }

        String queryString = "";
        if (queryParams.size() > 0) {
            queryString = "?" + String.join("&", queryParams);
        }

        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, superUserApiToken)
                .get("/api/admin/list-users" + queryString);

        return response;
    }

    static Response getAuthProviders(String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/admin/authenticationProviders");
        return response;
    }

    static Response migrateShibToBuiltin(Long userIdToConvert, String newEmailAddress, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(newEmailAddress)
                .put("/api/admin/authenticatedUsers/id/" + userIdToConvert + "/convertShibToBuiltIn");
        return response;
    }

    static Response migrateOAuthToBuiltin(Long userIdToConvert, String newEmailAddress, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(newEmailAddress)
                .put("/api/admin/authenticatedUsers/id/" + userIdToConvert + "/convertRemoteToBuiltIn");
        return response;
    }

    static Response migrateBuiltinToShib(String data, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(data)
                .put("/api/admin/authenticatedUsers/convert/builtin2shib");
        return response;
    }

    static Response migrateBuiltinToOAuth(String data, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(data)
                .put("/api/admin/authenticatedUsers/convert/builtin2oauth");
        return response;
    }

    static Response restrictFile(String fileIdOrPersistentId, boolean restrict, String apiToken) {
        String idInPath = fileIdOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isNumber(fileIdOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + fileIdOrPersistentId;
        }
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(restrict)
                .put("/api/files/" + idInPath + "/restrict" + optionalQueryParam);
        return response;
    }

    static Response moveDataverse(String movedDataverseAlias, String targetDataverseAlias, Boolean force, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .post("api/dataverses/" + movedDataverseAlias + "/move/" + targetDataverseAlias + "?forceMove=" + force + "&key=" + apiToken);
        return response;
    }
    
    static Response createDataverseLink(String linkedDataverseAlias, String linkingDataverseAlias, String apiToken) {
        Response response = given()
            .header(API_TOKEN_HTTP_HEADER, apiToken)
            .put("api/dataverses/" + linkedDataverseAlias + "/link/" + linkingDataverseAlias);
        return response;
    }
    
    static Response deleteDataverseLink(String linkedDataverseAlias, String linkingDataverseAlias, String apiToken) {
        Response response = given()
            .header(API_TOKEN_HTTP_HEADER, apiToken)
            .delete("api/dataverses/" + linkedDataverseAlias + "/deleteLink/" + linkingDataverseAlias);
        return response;
    }

    static Response nativeGet(Integer datasetId, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/datasets/" + datasetId);
        return response;
    }

    static Response privateUrlGet(Integer datasetId, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/datasets/" + datasetId + "/privateUrl");
        return response;
    }

    static Response privateUrlCreate(Integer datasetId, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .post("/api/datasets/" + datasetId + "/privateUrl");
        return response;
    }

    static Response privateUrlDelete(Integer datasetId, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("/api/datasets/" + datasetId + "/privateUrl");
        return response;
    }

    static Response showDatasetThumbnailCandidates(String datasetPersistentId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/datasets/:persistentId/thumbnail/candidates" + "?persistentId=" + datasetPersistentId);
    }

    static Response getDatasetThumbnail(String datasetPersistentId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/datasets/:persistentId/thumbnail" + "?persistentId=" + datasetPersistentId);
    }

    static Response getDatasetThumbnailMetadata(Integer datasetId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/admin/datasets/thumbnailMetadata/" + datasetId);
    }

    static Response useThumbnailFromDataFile(String datasetPersistentId, long dataFileId1, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .post("/api/datasets/:persistentId/thumbnail/" + dataFileId1 + "?persistentId=" + datasetPersistentId);
    }

    static Response uploadDatasetLogo(String datasetPersistentId, String pathToImageFile, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .multiPart("file", new File(pathToImageFile))
                .post("/api/datasets/:persistentId/thumbnail" + "?persistentId=" + datasetPersistentId);
    }

    static Response removeDatasetThumbnail(String datasetPersistentId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("/api/datasets/:persistentId/thumbnail" + "?persistentId=" + datasetPersistentId);
    }
    
    static Response getDatasetVersions(String idOrPersistentId, String apiToken) {
        logger.info("Getting Dataset Versions");
        String idInPath = idOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isNumber(idOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + idOrPersistentId;
        }
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.get("/api/datasets/" + idInPath + "/versions" + optionalQueryParam);
    }

    //TODO: this probably should return a file not a string, but I've mainly implemented it for testing purposes
    static Response getProvJson(String idOrPersistentId, String apiToken) {
        logger.info("Getting Provenance JSON");
        String idInPath = idOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isNumber(idOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + idOrPersistentId;
        }
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.get("/api/files/" + idInPath + "/prov-json" + optionalQueryParam);
    }
    
    static Response getProvFreeForm(String idOrPersistentId, String apiToken) {
         logger.info("Getting Provenance Free Form");
        String idInPath = idOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isNumber(idOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + idOrPersistentId;
        }
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.get("/api/files/" + idInPath + "/prov-freeform" + optionalQueryParam);
    }
    
    static Response uploadProvJson(String idOrPersistentId, JsonObject jsonObject, String apiToken, String entityName) {
        logger.info("Uploading Provenance JSON");
        String idInPath = idOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isNumber(idOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + idOrPersistentId;
        }
        optionalQueryParam += "?entityName="+entityName;
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken)
                    // This "[]" is here to quickly test that a JSON array is considered invalid.
                    //.body("[]")
                    .body(jsonObject.toString())
                    .contentType("application/json");
        }
        return requestSpecification.post("/api/files/" + idInPath + "/prov-json" + optionalQueryParam);
    }
    
    static Response deleteProvJson(String idOrPersistentId, String apiToken) {
        logger.info("Deleting Provenance JSON");
        //TODO: Repeated code, refactor
        String idInPath = idOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isNumber(idOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + idOrPersistentId;
        }
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.delete("/api/files/" + idInPath + "/prov-json" + optionalQueryParam);
    }

    static Response uploadProvFreeForm(String idOrPersistentId, JsonObject jsonObject, String apiToken) {
        logger.info("Uploading Provenance Free Form");
        String idInPath = idOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isNumber(idOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + idOrPersistentId;
        }
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken)
                    // This "[]" is here to quickly test that a JSON array is considered invalid.
                    //.body("[]")
                    .body(jsonObject.toString())
                    .contentType("application/json");
        }
        return requestSpecification.post("/api/files/" + idInPath + "/prov-freeform" + optionalQueryParam);
    }
    
//    static Response deleteProvFreeForm(String idOrPersistentId, String apiToken) {
//         logger.info("Deleting Provenance Free Form");
//        //TODO: Repeated code, refactor
//        String idInPath = idOrPersistentId; // Assume it's a number.
//        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
//        if (!NumberUtils.isNumber(idOrPersistentId)) {
//            idInPath = ":persistentId";
//            optionalQueryParam = "?persistentId=" + idOrPersistentId;
//        }
//        RequestSpecification requestSpecification = given();
//        if (apiToken != null) {
//            requestSpecification = given()
//                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
//        }
//        return requestSpecification.delete("/api/files/" + idInPath + "/prov-freeform" + optionalQueryParam);
//    }

    static Response exportDataset(String datasetPersistentId, String exporter, String apiToken) {
//        http://localhost:8080/api/datasets/export?exporter=dataverse_json&persistentId=doi%3A10.5072/FK2/W6WIMQ
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                //                .get("/api/datasets/:persistentId/export" + "?persistentId=" + datasetPersistentId + "&exporter=" + exporter);
                .get("/api/datasets/export" + "?persistentId=" + datasetPersistentId + "&exporter=" + exporter);
    }

    static Response search(String query, String apiToken) {
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.get("/api/search?q=" + query);
    }

    static Response searchAndShowFacets(String query, String apiToken) {
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.get("/api/search?q=" + query + "&show_facets=true");
    }

    static Response indexClear() {
        return given()
                .get("/api/admin/index/clear");
    }

    static Response index() {
        return given()
                .get("/api/admin/index");
    }

    static Response enableSetting(SettingsServiceBean.Key settingKey) {
        Response response = given().body("true").when().put("/api/admin/settings/" + settingKey);
        return response;
    }

    static Response deleteSetting(SettingsServiceBean.Key settingKey) {
        Response response = given().when().delete("/api/admin/settings/" + settingKey);
        return response;
    }

    static Response getSetting(SettingsServiceBean.Key settingKey) {
        Response response = given().when().get("/api/admin/settings/" + settingKey);
        return response;
    }

    static Response setSetting(SettingsServiceBean.Key settingKey, String value) {
        Response response = given().body(value).when().put("/api/admin/settings/" + settingKey);
        return response;
    }

    static Response getRoleAssignmentsOnDataverse(String dataverseAliasOrId, String apiToken) {
        String url = "/api/dataverses/" + dataverseAliasOrId + "/assignments";
        System.out.println("URL: " + url);
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get(url);
    }

    static Response getRoleAssignmentsOnDataset(String datasetId, String persistentId, String apiToken) {
        String url = "/api/datasets/" + datasetId + "/assignments";
        System.out.println("URL: " + url);
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get(url);
    }

    static Response grantRoleOnDataset(String definitionPoint, String role, String roleAssignee, String apiToken) {
        logger.info("Granting role on dataset \"" + definitionPoint + "\": " + role);
        return given()
                .body("@" + roleAssignee)
                .post("api/datasets/" + definitionPoint + "/assignments?key=" + apiToken);
    }

    static Response revokeRole(String definitionPoint, long doomed, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("api/dataverses/" + definitionPoint + "/assignments/" + doomed);
    }

    static Response findPermissionsOn(String dvObject, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("api/admin/permissions/" + dvObject);
    }

    static Response findRoleAssignee(String roleAssignee, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("api/admin/assignee/" + roleAssignee);
    }

    /**
     * Used to test Dataverse page query parameters
     *
     * @param alias
     * @param queryParamString - do not include the "?"
     * @return
     */
    static Response testDataverseQueryParamWithAlias(String alias, String queryParamString) {

        System.out.println("testDataverseQueryParamWithAlias");
        String testUrl;
        if (alias.isEmpty()) {
            testUrl = " ?" + queryParamString;
        } else {
            testUrl = "/dataverse/" + alias + "?" + queryParamString;
        }
        System.out.println("testUrl: " + testUrl);

        return given()
                .urlEncodingEnabled(false)
                .get(testUrl);
    }

    /**
     * Used to test Dataverse page query parameters
     *
     * The parameters may include "id={dataverse id}
     *
     * @param queryParamString
     * @return
     */
    static Response testDataverseXhtmlQueryParam(String queryParamString) {

        return given()
                //.urlEncodingEnabled(false)
                .get("dataverse.xhtml?" + queryParamString);
    }

    static Response setDataverseLogo(String dataverseAlias, String pathToImageFile, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .multiPart("file", new File(pathToImageFile))
                .put("/api/dataverses/" + dataverseAlias + "/logo");
    }

    /**
     * @todo figure out how to get an InputStream from REST Assured instead.
     */
    static InputStream getInputStreamFromUnirest(String url, String apiToken) {
        GetRequest unirestOut = Unirest.get(url);
        try {
            InputStream unirestInputStream = unirestOut.asBinary().getBody();
            return unirestInputStream;
        } catch (UnirestException ex) {
            return null;
        }
    }

    public static String inputStreamToDataUrlSchemeBase64Png(InputStream inputStream) {
        if (inputStream == null) {
            logger.fine("In inputStreamToDataUrlSchemeBase64Png but InputStream was null. Returning null.");
            return null;
        }
        byte[] bytes = inputStreamToBytes(inputStream);
        if (bytes == null) {
            logger.fine("In inputStreamToDataUrlSchemeBase64Png but bytes was null. Returning null.");
            return null;
        }
        String base64image = Base64.getEncoder().encodeToString(bytes);
        return FileUtil.DATA_URI_SCHEME + base64image;
    }

    /**
     * @todo When Java 9 comes out, switch to
     * http://download.java.net/java/jdk9/docs/api/java/io/InputStream.html#readAllBytes--
     */
    private static byte[] inputStreamToBytes(InputStream inputStream) {
        try {
            return IOUtils.toByteArray(inputStream);
        } catch (IOException ex) {
            logger.fine("In inputStreamToBytes but caught an IOUtils.toByteArray Returning null.");
            return null;
        }
    }

    static Response listMapLayerMetadatas() {
        return given().get("/api/admin/geoconnect/mapLayerMetadatas");
    }

    static Response checkMapLayerMetadatas(String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .post("/api/admin/geoconnect/mapLayerMetadatas/check");
    }

    static Response checkMapLayerMetadatas(Long mapLayerMetadataId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .post("/api/admin/geoconnect/mapLayerMetadatas/check/" + mapLayerMetadataId);
    }

    static Response getMapFromFile(long fileId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/files/" + fileId + "/map");
    }

    static Response checkMapFromFile(long fileId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/files/" + fileId + "/map/check");
    }

    static Response deleteMapFromFile(long fileId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("/api/files/" + fileId + "/map?key=" + apiToken);
    }

    static Response getRsyncScript(String datasetPersistentId, String apiToken) {
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.get("/api/datasets/:persistentId/dataCaptureModule/rsync?persistentId=" + datasetPersistentId);
    }

    static Response dataCaptureModuleChecksumValidation(String datasetPersistentId, JsonObject jsonObject, String apiToken) {
        String persistentIdInPath = datasetPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If datasetPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isNumber(datasetPersistentId)) {
            persistentIdInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + datasetPersistentId;
        }
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .body(jsonObject.toString())
                    .contentType(ContentType.JSON)
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.post("/api/datasets/" + persistentIdInPath + "/dataCaptureModule/checksumValidation" + optionalQueryParam);
    }

    static Response getApiTokenUsingUsername(String username, String password) {
        Response response = given()
                .contentType(ContentType.JSON)
                .get("/api/builtin-users/" + username + "/api-token?username=" + username + "&password=" + password);
        return response;
    }

    static Response getExternalTools() {
        return given()
                .get("/api/admin/externalTools");
    }

    static Response getExternalToolsByFileId(long fileId) {
        return given()
                .get("/api/admin/externalTools/file/" + fileId);
    }

    static Response addExternalTool(JsonObject jsonObject) {
        RequestSpecification requestSpecification = given();
        requestSpecification = given()
                .body(jsonObject.toString())
                .contentType(ContentType.JSON);
        return requestSpecification.post("/api/admin/externalTools");
    }

    static Response deleteExternalTool(long externalToolid) {
        return given()
                .delete("/api/admin/externalTools/" + externalToolid);
    }

    static Response submitFeedback(JsonObjectBuilder job) {
        return given()
                .body(job.build().toString())
                .contentType("application/json")
                .post("/api/admin/feedback");
    }

    static Response listStorageSites() {
        return given()
                .get("/api/admin/storageSites");
    }

    static Response getStorageSitesById(long id) {
        return given()
                .get("/api/admin/storageSites/" + id);
    }

    static Response setPrimaryLocationBoolean(long id, String input) {
        return given()
                .body(input)
                .put("/api/admin/storageSites/" + id + "/primaryStorage");
    }

    static Response addStorageSite(JsonObject jsonObject) {
        RequestSpecification requestSpecification = given();
        requestSpecification = given()
                .body(jsonObject.toString())
                .contentType(ContentType.JSON);
        return requestSpecification.post("/api/admin/storageSites");
    }

    static Response deleteStorageSite(long storageSiteId) {
        return given()
                .delete("/api/admin/storageSites/" + storageSiteId);
    }

    static Response metricsDataversesToMonth(String yyyymm) {
        String optionalYyyyMm = "";
        if (yyyymm != null) {
            optionalYyyyMm = "/" + yyyymm;
        }
        RequestSpecification requestSpecification = given();
        requestSpecification = given();
        return requestSpecification.get("/api/info/metrics/dataverses/toMonth" + optionalYyyyMm);
    }

    static Response metricsDatasetsToMonth(String yyyymm) {
        String optionalYyyyMm = "";
        if (yyyymm != null) {
            optionalYyyyMm = "/" + yyyymm;
        }
        RequestSpecification requestSpecification = given();
        requestSpecification = given();
        return requestSpecification.get("/api/info/metrics/datasets/toMonth" + optionalYyyyMm);
    }

    static Response metricsFilesToMonth(String yyyymm) {
        String optionalYyyyMm = "";
        if (yyyymm != null) {
            optionalYyyyMm = "/" + yyyymm;
        }
        RequestSpecification requestSpecification = given();
        requestSpecification = given();
        return requestSpecification.get("/api/info/metrics/files/toMonth" + optionalYyyyMm);
    }

    static Response metricsDownloadsToMonth(String yyyymm) {
        String optionalYyyyMm = "";
        if (yyyymm != null) {
            optionalYyyyMm = "/" + yyyymm;
        }
        RequestSpecification requestSpecification = given();
        requestSpecification = given();
        return requestSpecification.get("/api/info/metrics/downloads/toMonth" + optionalYyyyMm);
    }

    static Response metricsDataverseByCategory() {
        RequestSpecification requestSpecification = given();
        requestSpecification = given();
        return requestSpecification.get("/api/info/metrics/dataverses/byCategory");
    }

    static Response metricsDatasetsBySubject() {
        RequestSpecification requestSpecification = given();
        requestSpecification = given();
        return requestSpecification.get("/api/info/metrics/datasets/bySubject");
    }
    
    static Response clearMetricCache() {
        RequestSpecification requestSpecification = given();
        requestSpecification = given();
        return requestSpecification.delete("/api/admin/clearMetricsCache");
    }

    static Response sitemapUpdate() {
        return given()
                .post("/api/admin/sitemap");
    }

    static Response sitemapDownload() {
        return given()
                .get("/sitemap.xml");
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
    
    static Response checkDatasetLocks(long datasetId, String lockType, String apiToken) {
        Response response = given()
            .header(API_TOKEN_HTTP_HEADER, apiToken)
            .get("api/datasets/" + datasetId + "/locks" + (lockType == null ? "" : "?type="+lockType));
        return response;       
    }
    
    static Response lockDataset(long datasetId, String lockType, String apiToken) {
        Response response = given()
            .header(API_TOKEN_HTTP_HEADER, apiToken)
            .post("api/datasets/" + datasetId + "/lock/" + lockType);
        return response;       
    }
    
    static Response unlockDataset(long datasetId, String lockType, String apiToken) {
        Response response = given()
            .header(API_TOKEN_HTTP_HEADER, apiToken)
            .delete("api/datasets/" + datasetId + "/locks" + (lockType == null ? "" : "?type="+lockType));
        return response;       
    }
    
    static Response exportOaiSet(String setName) {
        String apiPath = String.format("/api/admin/metadata/exportOAI/%s", setName);
        return given().put(apiPath);
    }
    
    static Response getOaiRecord(String datasetPersistentId, String metadataFormat) {
        String apiPath = String.format("/oai?verb=GetRecord&identifier=%s&metadataPrefix=%s", datasetPersistentId, metadataFormat);
        return given().get(apiPath);
    }
    
    static Response getOaiListIdentifiers(String setName, String metadataFormat) {
        String apiPath = String.format("/oai?verb=ListIdentifiers&set=%s&metadataPrefix=%s", setName, metadataFormat);
        return given().get(apiPath);
    }
}
