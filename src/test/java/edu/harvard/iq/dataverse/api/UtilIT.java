package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;

import static edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder.jsonObjectBuilder;
import static jakarta.ws.rs.core.Response.Status.CREATED;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.logging.Level;
import edu.harvard.iq.dataverse.api.datadeposit.SwordConfigurationImpl;
import io.restassured.path.xml.XmlPath;
import edu.harvard.iq.dataverse.mydata.MyDataFilterParams;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import io.restassured.specification.RequestSpecification;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import edu.harvard.iq.dataverse.util.FileUtil;
import org.apache.commons.io.IOUtils;
import java.nio.file.Path;

import org.apache.commons.lang3.math.NumberUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import static edu.harvard.iq.dataverse.api.ApiConstants.*;
import static io.restassured.path.xml.XmlPath.from;
import static io.restassured.RestAssured.given;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.settings.FeatureFlags;
import edu.harvard.iq.dataverse.util.StringUtil;

import static org.junit.jupiter.api.Assertions.*;

public class UtilIT {

    private static final Logger logger = Logger.getLogger(UtilIT.class.getCanonicalName());

    public static final String API_TOKEN_HTTP_HEADER = "X-Dataverse-key";
    private static final String USERNAME_KEY = "userName";
    private static final String EMAIL_KEY = "email";
    private static final String API_TOKEN_KEY = "apiToken";
    private static final String BUILTIN_USER_KEY = "burrito";
    private static final String EMPTY_STRING = "";
    public static final int MAXIMUM_INGEST_LOCK_DURATION = 15;
    public static final int MAXIMUM_PUBLISH_LOCK_DURATION = 20;
    public static final int GENERAL_LONG_DURATION = 45; //Useful when multiple adds/publishes, etc/ all get done in sequence
    public static final int MAXIMUM_IMPORT_DURATION = 1;

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

    /**
     * A convenience method for creating a random test user, when all you need
     * is the api token.
     * @return apiToken
     */
    public static String createRandomUserGetToken(){
        Response createUser = createRandomUser();
        return getApiTokenFromResponse(createUser);
    }

    public static Response createUser(String username, String email) {
        logger.info("Creating user " + username);
        String userAsJson = getUserAsJsonString(username, username, username, email);
        String password = getPassword(userAsJson);
        Response response = given()
                .body(userAsJson)
                .contentType(ContentType.JSON)
                .post("/api/builtin-users?key=" + BUILTIN_USER_KEY + "&password=" + password);
        return response;
    }

    private static String getUserAsJsonString(String username, String firstName, String lastName, String email) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(USERNAME_KEY, username);
        builder.add("firstName", firstName);
        builder.add("lastName", lastName);
        builder.add(EMAIL_KEY, email);
        String userAsJson = builder.build().toString();
        logger.fine("User to create: " + userAsJson);
        return userAsJson;
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
        Response response = given()
                .body(datasetIdentifier)
                .contentType(ContentType.JSON)
                .post("/api/admin/" + datasetIdentifier + "/reregisterHDLToPID?key=" + apiToken);
        return response;
    }

    public static Response getPid(String persistentId, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/pids?persistentId=" + persistentId);
        return response;
    }

    public static Response getUnreservedPids(String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/pids/unreserved");
        return response;
    }

    public static Response reservePid(String persistentId, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .post("/api/pids/:persistentId/reserve?persistentId=" + persistentId);
        return response;
    }

    public static Response deletePid(String persistentId, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("/api/pids/:persistentId/delete?persistentId=" + persistentId);
        return response;
    }

    public static Response computeDataFileHashValue(String fileId, String alg, String apiToken) {
        Response response = given()
                .body(fileId)
                .contentType(ContentType.JSON)
                .post("/api/admin/computeDataFileHashValue/" + fileId + "/algorithm/" + alg + "?key=" + apiToken);
        return response;
    }
    
    public static Response validateDataFileHashValue(String fileId,  String apiToken) {
        Response response = given()
                .body(fileId)
                .contentType(ContentType.JSON)
                .post("/api/admin/validateDataFileHashValue/" + fileId + "?key=" + apiToken);
        return response;
    }

    public static Response clearThumbnailFailureFlags() {
        Response response = given()
                .delete("/api/admin/clearThumbnailFailureFlag");
        return response;
    }

    public static Response clearThumbnailFailureFlag(long fileId) {
        Response response = given()
                .delete("/api/admin/clearThumbnailFailureFlag/" + fileId);
        return response;
    }

    public static Response auditFiles(String apiToken, Long firstId, Long lastId, String csvList) {
        String params = "";
        if (firstId != null) {
            params = "?firstId="+ firstId;
        }
        if (lastId != null) {
            params = params + (params.isEmpty() ? "?" : "&") + "lastId="+ lastId;
        }
        if (csvList != null) {
            params = params + (params.isEmpty() ? "?" : "&") + "datasetIdentifierList="+ csvList;
        }
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/admin/datafiles/auditFiles" + params);
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
    static String getEmailFromResponse(Response createUserResponse) {
        JsonPath createdUser = JsonPath.from(createUserResponse.body().asString());
        String email = createdUser.getString("data.authenticatedUser.email");
        logger.info("Email found in create user response: " + email);
        return email;
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
        logger.info("Id found in create dataverse response: " + dataverseId);
        return dataverseId;
    }

    static Integer getDatasetIdFromResponse(Response createDatasetResponse) {
        JsonPath createdDataset = JsonPath.from(createDatasetResponse.body().asString());
        int datasetId = createdDataset.getInt("data.id");
        logger.info("Id found in create dataset response: " + datasetId);
        return datasetId;
    }

    static Integer getDataFileIdFromResponse(Response uploadDataFileResponse) {
        JsonPath dataFile = JsonPath.from(uploadDataFileResponse.body().asString());
        int dataFileId = dataFile.getInt("data.files[0].dataFile.id");
        logger.info("Id found in upload DataFile response: " + dataFileId);
        return dataFileId;
    }

    static Integer getSearchCountFromResponse(Response searchResponse) {
        JsonPath createdDataset = JsonPath.from(searchResponse.body().asString());
        int searchCount = createdDataset.getInt("data.total_count");
        logger.info("Search Count found: " + searchCount);
        return searchCount;
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
        int doiPos = datasetSwordIdUrl.indexOf("doi");
        return datasetSwordIdUrl.substring(doiPos, datasetSwordIdUrl.length());
    }

    public static Response getServiceDocument(String apiToken) {
        Response response = given()
                .auth().basic(apiToken, EMPTY_STRING)
                .get(swordConfiguration.getBaseUrlPathCurrent() + "/service-document");
        return response;
    }

    //This method creates a dataverse off root, for more nesting use createSubDataverse
    static Response createDataverse(String alias, String category, String apiToken) {
        return createSubDataverse(alias, category, apiToken, ":root");
    }

    static Response createDataverse(String alias, String category, String affiliation, String apiToken) {
        return createSubDataverse(alias, category, apiToken, ":root", null, null, null, affiliation);
    }

    static Response createSubDataverse(String alias, String category, String apiToken, String parentDV) {
        return createSubDataverse(alias, category, apiToken, parentDV, null, null, null, null);
    }

    static Response createSubDataverse(String alias, String category, String apiToken, String parentDV, String[] inputLevelNames, String[] facetIds, String[] metadataBlockNames) {
        return createSubDataverse(alias, category, apiToken, parentDV, inputLevelNames, facetIds, metadataBlockNames, null);
    }

    static Response createSubDataverse(String alias, String category, String apiToken, String parentDV, String[] inputLevelNames, String[] facetIds, String[] metadataBlockNames, String affiliation) {
        JsonArrayBuilder contactArrayBuilder = Json.createArrayBuilder();
        contactArrayBuilder.add(Json.createObjectBuilder().add("contactEmail", getEmailFromUserName(getRandomIdentifier())));
        JsonArrayBuilder subjectArrayBuilder = Json.createArrayBuilder();
        subjectArrayBuilder.add("Other");
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder()
                .add("alias", alias)
                .add("name", alias)
                .add("dataverseContacts", contactArrayBuilder)
                .add("dataverseSubjects", subjectArrayBuilder)
                // don't send "dataverseType" if category is null, must be a better way
                .add(category != null ? "dataverseType" : "notTheKeyDataverseType", category != null ? category : "whatever");

        if (affiliation != null) {
            objectBuilder.add("affiliation", affiliation);
        }

        updateDataverseRequestJsonWithMetadataBlocksConfiguration(inputLevelNames, facetIds, metadataBlockNames, null, null, objectBuilder);

        JsonObject dvData = objectBuilder.build();
        return given()
                .body(dvData.toString()).contentType(ContentType.JSON)
                .when().post("/api/dataverses/" + parentDV + "?key=" + apiToken);
    }
    static Response updateDataverse(String alias,
                                    String newAlias,
                                    String newName,
                                    String newAffiliation,
                                    String newDataverseType,
                                    String[] newContactEmails,
                                    String[] newInputLevelNames,
                                    String[] newFacetIds,
                                    String[] newMetadataBlockNames,
                                    String apiToken) {

        return updateDataverse(alias, newAlias, newName, newAffiliation, newDataverseType, newContactEmails,
                newInputLevelNames, newFacetIds, newMetadataBlockNames, apiToken, null, null);
    }

    static Response updateDataverse(String alias,
                                    String newAlias,
                                    String newName,
                                    String newAffiliation,
                                    String newDataverseType,
                                    String[] newContactEmails,
                                    String[] newInputLevelNames,
                                    String[] newFacetIds,
                                    String[] newMetadataBlockNames,
                                    String apiToken,
                                    Boolean inheritMetadataBlocksFromParent,
                                    Boolean inheritFacetsFromParent) {
        JsonArrayBuilder contactArrayBuilder = Json.createArrayBuilder();
        for(String contactEmail : newContactEmails) {
            contactArrayBuilder.add(Json.createObjectBuilder().add("contactEmail", contactEmail));
        }
        NullSafeJsonBuilder jsonBuilder = jsonObjectBuilder()
                .add("alias", newAlias)
                .add("name", newName)
                .add("affiliation", newAffiliation)
                .add("dataverseContacts", contactArrayBuilder)
                .add("dataverseType", newDataverseType)
                .add("affiliation", newAffiliation);

        updateDataverseRequestJsonWithMetadataBlocksConfiguration(newInputLevelNames, newFacetIds, newMetadataBlockNames,
                inheritMetadataBlocksFromParent, inheritFacetsFromParent, jsonBuilder);

        JsonObject dvData = jsonBuilder.build();
        return given()
                .body(dvData.toString()).contentType(ContentType.JSON)
                .when().put("/api/dataverses/" + alias + "?key=" + apiToken);
    }

    private static void updateDataverseRequestJsonWithMetadataBlocksConfiguration(String[] inputLevelNames,
                                                                                  String[] facetIds,
                                                                                  String[] metadataBlockNames,
                                                                                  Boolean inheritFacetsFromParent,
                                                                                  Boolean inheritMetadataBlocksFromParent,
                                                                                  JsonObjectBuilder objectBuilder) {
        JsonObjectBuilder metadataBlocksObjectBuilder = Json.createObjectBuilder();

        if (inputLevelNames != null) {
            JsonArrayBuilder inputLevelsArrayBuilder = Json.createArrayBuilder();
            for(String inputLevelName : inputLevelNames) {
                inputLevelsArrayBuilder.add(Json.createObjectBuilder()
                        .add("datasetFieldTypeName", inputLevelName)
                        .add("required", true)
                        .add("include", true)
                );
            }
            metadataBlocksObjectBuilder.add("inputLevels", inputLevelsArrayBuilder);
        }

        if (metadataBlockNames != null) {
            JsonArrayBuilder metadataBlockNamesArrayBuilder = Json.createArrayBuilder();
            for(String metadataBlockName : metadataBlockNames) {
                metadataBlockNamesArrayBuilder.add(metadataBlockName);
            }
            metadataBlocksObjectBuilder.add("metadataBlockNames", metadataBlockNamesArrayBuilder);
        }
        if (inheritMetadataBlocksFromParent != null) {
            metadataBlocksObjectBuilder.add("inheritMetadataBlocksFromParent", inheritMetadataBlocksFromParent);
        }

        if (facetIds != null) {
            JsonArrayBuilder facetIdsArrayBuilder = Json.createArrayBuilder();
            for(String facetId : facetIds) {
                facetIdsArrayBuilder.add(facetId);
            }
            metadataBlocksObjectBuilder.add("facetIds", facetIdsArrayBuilder);
        }
        if (inheritFacetsFromParent != null) {
            metadataBlocksObjectBuilder.add("inheritFacetsFromParent", inheritFacetsFromParent);
        }

        objectBuilder.add("metadataBlocks", metadataBlocksObjectBuilder);
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

    static Response createRandomDataverse(String apiToken, String affiliation) {
        String alias = "dv" + getRandomIdentifier();
        String category = null;
        return createDataverse(alias, category, affiliation, apiToken);
    }

    /**
     * A convenience method for creating a random collection and getting its
     * alias in one step.
     * @param apiToken
     * @return alias
     */
    static String createRandomCollectionGetAlias(String apiToken){

        Response createCollectionResponse = createRandomDataverse(apiToken);
        //createDataverseResponse.prettyPrint();
        createCollectionResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        return UtilIT.getAliasFromResponse(createCollectionResponse);
    }

    static Response showDataverseContents(String alias, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .when().get("/api/dataverses/" + alias + "/contents");
    }

    static Response getGuestbookResponses(String dataverseAlias, Long guestbookId, String apiToken) {
        RequestSpecification requestSpec = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken);
        if (guestbookId != null) {
            requestSpec.queryParam("guestbookId", guestbookId);
        }
        return requestSpec.get("/api/dataverses/" + dataverseAlias + "/guestbookResponses/");
    }

    static Response getCollectionSchema(String dataverseAlias, String apiToken) {
        Response getCollectionSchemaResponse = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .contentType("application/json")
                .get("/api/dataverses/" + dataverseAlias + "/datasetSchema");
        return getCollectionSchemaResponse;
    }

    static Response validateDatasetJson(String dataverseAlias, String datasetJson, String apiToken) {
        Response getValidateDatasetJsonResponse = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(datasetJson)
                .contentType("application/json")
                .post("/api/dataverses/" + dataverseAlias + "/validateDatasetJson");
        return getValidateDatasetJsonResponse;
    }

    static Response createRandomDatasetViaNativeApi(String dataverseAlias, String apiToken) {
        return createRandomDatasetViaNativeApi(dataverseAlias, apiToken, false);
    }

    static Response createRandomDatasetViaNativeApi(String dataverseAlias, String apiToken, boolean withNoLicense) {
        String jsonIn = getDatasetJson(withNoLicense);
        Response createDatasetResponse = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(jsonIn)
                .contentType("application/json")
                .post("/api/dataverses/" + dataverseAlias + "/datasets");
        return createDatasetResponse;
    }

    private static String getDatasetJson() {
        return getDatasetJson(false); 
    }
     
    private static String getDatasetJson(boolean nolicense) {
        File datasetVersionJson; 
        if (nolicense) {
            datasetVersionJson = new File("scripts/search/tests/data/dataset-finch1-nolicense.json");
        } else {
            datasetVersionJson = new File("scripts/search/tests/data/dataset-finch1.json");
        }
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

    static Response createDataset(String dataverseAlias, JsonObjectBuilder datasetJson, String apiToken) {
        return createDataset(dataverseAlias, datasetJson.build().toString(), apiToken);
    }

    static Response createDataset(String dataverseAlias, String datasetJson, String apiToken) {
        System.out.println("creating with " + datasetJson);
        Response createDatasetResponse = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(datasetJson)
                .contentType("application/json")
                .post("/api/dataverses/" + dataverseAlias + "/datasets");
        return createDatasetResponse;
    }

    static Response createDatasetSemantic(String dataverseAlias, String datasetJson, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(datasetJson)
                .contentType("application/ld+json")
                .post("/api/dataverses/" + dataverseAlias + "/datasets");
        return response;
    }

    static String getDatasetJson(String pathToJsonFile) {
        File datasetVersionJson = new File(pathToJsonFile);
        try {
            String datasetVersionAsJson = new String(Files.readAllBytes(Paths.get(datasetVersionJson.getAbsolutePath())));
            return datasetVersionAsJson;
        } catch (IOException ex) {
            Logger.getLogger(UtilIT.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (Exception e){
            Logger.getLogger(UtilIT.class.getName()).log(Level.SEVERE, null, e);
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

    static Response createDatasetViaSwordApi(String dataverseToCreateDatasetIn, String title, String description, String license, String apiToken) {
        String nullRights = null;
        String xmlIn = getDatasetXml(title, "Lastname, Firstname", description, license, nullRights);
        return createDatasetViaSwordApiFromXML(dataverseToCreateDatasetIn, xmlIn, apiToken);
    }

    static Response createDatasetViaSwordApi(String dataverseToCreateDatasetIn, String title, String description, String license, String rights, String apiToken) {
        String xmlIn = getDatasetXml(title, "Lastname, Firstname", description, license, rights);
        return createDatasetViaSwordApiFromXML(dataverseToCreateDatasetIn, xmlIn, apiToken);
    }

    public static Response createDatasetViaSwordApiFromXML(String dataverseToCreateDatasetIn, String xmlIn, String apiToken) {
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
                .put("/api/datasets/:persistentId/versions/" + DS_VERSION_DRAFT + "?persistentId=" + persistentId);
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
        return editVersionMetadataFromJsonStr(persistentId, jsonIn, apiToken, null);
    }

    static Response editVersionMetadataFromJsonStr(String persistentId, String jsonString, String apiToken) {
        return editVersionMetadataFromJsonStr(persistentId, jsonString, apiToken, null);
    }

    static Response editVersionMetadataFromJsonStr(String persistentId, String jsonString, String apiToken, Integer sourceInternalVersionNumber) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(jsonString)
                .contentType("application/json")
                .put("/api/datasets/:persistentId/editMetadata/?persistentId="
                        + persistentId
                        + "&replace=true"
                        + (sourceInternalVersionNumber != null ? "&sourceInternalVersionNumber=" + sourceInternalVersionNumber : ""));
    }

    static Response updateDatasetPIDMetadata(String persistentId,  String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .contentType("application/json")
                .post("/api/datasets/:persistentId/modifyRegistrationMetadata/?persistentId=" + persistentId);
        return response;
    }

    /**
     * Deprecated because once there are new fields in the database that Solr
     * doesn't know about, dataset creation could be prevented, or at least
     * subsequent search operations could fail because the dataset can't be
     * indexed.
     */
    @Deprecated    
    static Response loadMetadataBlock(String apiToken, byte[] body) {
        return given()
          .header(API_TOKEN_HTTP_HEADER, apiToken)
          .contentType("text/tab-separated-values; charset=utf-8")
          .body(body)
          .post("/api/admin/datasetfield/load");
    }

    static Response setMetadataBlocks(String dataverseAlias, JsonArrayBuilder blocks, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .contentType("application/json")
                .body(blocks.build().toString())
                .post("/api/dataverses/" + dataverseAlias + "/metadatablocks");
    }

    static Response listMetadataBlocks(String dataverseAlias, boolean onlyDisplayedOnCreate, boolean returnDatasetFieldTypes, String apiToken) {
        return listMetadataBlocks(dataverseAlias, onlyDisplayedOnCreate, returnDatasetFieldTypes, null, apiToken);
    }

    static Response listMetadataBlocks(String dataverseAlias, boolean onlyDisplayedOnCreate, boolean returnDatasetFieldTypes, String datasetType, String apiToken) {
        RequestSpecification requestSpecification = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .queryParam("onlyDisplayedOnCreate", onlyDisplayedOnCreate)
                .queryParam("returnDatasetFieldTypes", returnDatasetFieldTypes);
        if (datasetType != null) {
            requestSpecification.queryParam("datasetType", datasetType);
        }
        return requestSpecification.get("/api/dataverses/" + dataverseAlias + "/metadatablocks");
    }

    static Response listMetadataBlocks(boolean onlyDisplayedOnCreate, boolean returnDatasetFieldTypes) {
        return given()
                .queryParam("onlyDisplayedOnCreate", onlyDisplayedOnCreate)
                .queryParam("returnDatasetFieldTypes", returnDatasetFieldTypes)
                .get("/api/metadatablocks");
    }

    static Response getMetadataBlock(String block) {
        return given()
                .get("/api/metadatablocks/" + block);
    }

    static Response setDisplayOnCreate(String datasetFieldType, boolean setDisplayOnCreate) {
        return given()
                .queryParam("datasetFieldType", datasetFieldType)
                .queryParam("setDisplayOnCreate", setDisplayOnCreate)
                .post("/api/admin/datasetfield/setDisplayOnCreate");
    }

    static private String getDatasetXml(String title, String author, String description) {
        String nullLicense = null;
        String nullRights = null;
        return getDatasetXml(title, author, description, nullLicense, nullRights);
    }

    static private String getDatasetXml(String title, String author, String description, String license, String rights) {
        String optionalLicense = "";
        if (license != null) {
            optionalLicense = "   <dcterms:license>" + license + "</dcterms:license>\n";
        }
        String optionalRights = "";
        if (rights != null) {
            optionalRights = "   <dcterms:rights>" + rights + "</dcterms:rights>\n";
        }
        String xmlIn = "<?xml version=\"1.0\"?>\n"
                + "<entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:dcterms=\"http://purl.org/dc/terms/\">\n"
                + "   <dcterms:title>" + title + "</dcterms:title>\n"
                + "   <dcterms:creator>" + author + "</dcterms:creator>\n"
                + "   <dcterms:description>" + description + "</dcterms:description>\n"
                + optionalLicense
                + optionalRights
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
        String nullMimeType = null;
        return uploadFileViaNative(datasetId, pathToFile, jsonAsString, nullMimeType, apiToken);
    }

    static Response uploadFileViaNative(String datasetId, String pathToFile, String jsonAsString, String mimeType, String apiToken) {
        RequestSpecification requestSpecification = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .multiPart("datasetId", datasetId)
                .multiPart("file", new File(pathToFile), mimeType);
        if (jsonAsString != null) {
            requestSpecification.multiPart("jsonData", jsonAsString);
        }
        return requestSpecification.post("/api/datasets/" + datasetId + "/add");
    }

    static Response addRemoteFile(String datasetId, String jsonAsString, String apiToken) {
        RequestSpecification requestSpecification = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .multiPart("datasetId", datasetId);
        if (jsonAsString != null) {
            requestSpecification.multiPart("jsonData", jsonAsString);
        }
        return requestSpecification.post("/api/datasets/" + datasetId + "/add");
    }

    static Response uploadAuxFile(Long fileId, String pathToFile, String formatTag, String formatVersion, String mimeType, boolean isPublic, String type, String apiToken) {
        String nullOrigin = null;
        return uploadAuxFile(fileId, pathToFile, formatTag, formatVersion, mimeType, isPublic, type, nullOrigin, apiToken);
    }

    static Response uploadAuxFile(Long fileId, String pathToFile, String formatTag, String formatVersion, String mimeType, boolean isPublic, String type, String origin, String apiToken) {
        RequestSpecification requestSpecification = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .multiPart("file", new File(pathToFile), mimeType)
                .multiPart("isPublic", isPublic);
        if (mimeType != null) {
            requestSpecification.multiPart("file", new File(pathToFile), mimeType);
        } else {
            requestSpecification.multiPart("file", new File(pathToFile));
        }
        if (type != null) {
            requestSpecification.multiPart("type", type);
        }
        if (origin != null) {
            requestSpecification.multiPart("origin", origin);
        }
        return requestSpecification.post("/api/access/datafile/" + fileId + "/auxiliary/" + formatTag + "/" + formatVersion);
    }

    static Response downloadAuxFile(Long fileId, String formatTag, String formatVersion, String apiToken) {
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification.header(API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.get("/api/access/datafile/" + fileId + "/auxiliary/" + formatTag + "/" + formatVersion);
    }

    static Response listAuxFilesByOrigin(Long fileId, String origin, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/access/datafile/" + fileId + "/auxiliary/" + origin);
        return response;
    }

    static Response listAllAuxFiles(Long fileId, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/access/datafile/" + fileId + "/auxiliary");
        return response;
    }

    
    static Response deleteAuxFile(Long fileId, String formatTag, String formatVersion, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("/api/access/datafile/" + fileId + "/auxiliary/" + formatTag + "/" + formatVersion);
        return response;
    }

    static Response getCrawlableFileAccess(String datasetId, String folderName, String apiToken) {
        RequestSpecification requestSpecification = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken);
        String apiPath = "/api/datasets/" + datasetId + "/dirindex?version=" + DS_VERSION_DRAFT;
        if (StringUtil.nonEmpty(folderName)) {
            apiPath = apiPath.concat("&folder="+folderName);
        }
        return requestSpecification.get(apiPath);
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
        if (!NumberUtils.isCreatable(fileIdOrPersistentId)) {
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

    static Response deleteFileApi(Integer fileId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("/api/files/" + fileId);
    }
    
    static Response updateFileMetadata(String fileIdOrPersistentId, String jsonAsString, String apiToken) {
        String idInPath = fileIdOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(fileIdOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + fileIdOrPersistentId;
        }
        RequestSpecification requestSpecification = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken);
        if (jsonAsString != null) {
            requestSpecification.multiPart("jsonData", jsonAsString);
        }
        return requestSpecification
                .post("/api/files/" + idInPath + "/metadata" + optionalQueryParam);
    }

    static Response downloadFile(Integer fileId) {
        return given()
                //                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/access/datafile/" + fileId);
    }

    static Response downloadFile(Integer fileId, String apiToken) {
        String nullByteRange = null;
        String nullFormat = null;
        String nullImageThumb = null;
        return downloadFile(fileId, nullByteRange, nullFormat, nullImageThumb, apiToken);
    }

    static Response downloadFile(Integer fileId, String byteRange, String format, String imageThumb, String apiToken) {
        RequestSpecification requestSpecification = given();
        if (byteRange != null) {
            requestSpecification.header("Range", "bytes=" + byteRange);
        }
        String optionalFormat = "";
        if (format != null) {
            optionalFormat = "&format=" + format;
        }
        String optionalImageThumb = "";
        if (format != null) {
            optionalImageThumb = "&imageThumb=" + imageThumb;
        }
        /**
         * Data Access API does not support X-Dataverse-key header -
         * https://github.com/IQSS/dataverse/issues/2662
         *
         * Actually, these days it does. We could switch.
         */
        //.header(API_TOKEN_HTTP_HEADER, apiToken)
        return requestSpecification.get("/api/access/datafile/" + fileId + "?key=" + apiToken + optionalFormat + optionalImageThumb);
    }
    
    static Response downloadTabularFile(Integer fileId) {
        return given()
                // this downloads a tabular file, explicitly requesting the format by name
                .get("/api/access/datafile/" + fileId + "?format=tab");
    }

    static Response downloadFileOriginal(Integer fileId) {
        return given()
                .get("/api/access/datafile/" + fileId + "?format=original");
    }
    
    static Response downloadTabularFileNoVarHeader(Integer fileId) {
        return given()
                .get("/api/access/datafile/" + fileId + "?noVarHeader=true");
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

    public enum DownloadFormat {
        original
    }

    static Response downloadFiles(String datasetIdOrPersistentId, String apiToken) {
        String datasetVersion = null;
        DownloadFormat format = null;
        return downloadFiles(datasetIdOrPersistentId, datasetVersion, format, apiToken);
    }

    static Response downloadFiles(String datasetIdOrPersistentId, DownloadFormat format, String apiToken) {
        String datasetVersion = null;
        return downloadFiles(datasetIdOrPersistentId, datasetVersion, format, apiToken);
    }

    static Response downloadFiles(String datasetIdOrPersistentId, String datasetVersion, String apiToken) {
        DownloadFormat format = null;
        return downloadFiles(datasetIdOrPersistentId, datasetVersion, format, apiToken);
    }

    static Response downloadFiles(String datasetIdOrPersistentId, String datasetVersion, DownloadFormat format, String apiToken) {
        String idInPath = datasetIdOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(datasetIdOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + datasetIdOrPersistentId;
        }
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        String optionalVersion = "";
        if (datasetVersion != null) {
            optionalVersion = "/versions/" + datasetVersion;
        }
        String optionalFormat = "";
        if (format != null) {
            if (!"".equals(optionalQueryParam)) {
                optionalFormat = "&format=" + format;
            } else {
                optionalFormat = "?format=" + format;
            }
        }
        return requestSpecification.get("/api/access/dataset/" + idInPath + optionalVersion + optionalQueryParam + optionalFormat);
    }

    static Response subset(String fileId, String variables, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/access/datafile/" + fileId + "?format=subset&variables=" + variables);
    }

    static Response getFileMetadata(String fileIdOrPersistentId, String optionalFormat, String apiToken) {
        String idInPath = fileIdOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(fileIdOrPersistentId)) {
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

    static Response getFileMetadata(String fileIdOrPersistentId, String optionalFormat) {
        String idInPath = fileIdOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(fileIdOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + fileIdOrPersistentId;
        }
        String optionalFormatInPath = "";
        if (optionalFormat != null) {
            optionalFormatInPath = "/" + optionalFormat;
        }
        return given()
                .urlEncodingEnabled(false)
                .get("/api/access/datafile/" + idInPath + "/metadata" + optionalFormatInPath + optionalQueryParam);
    }

    static Response getFileData(String fileId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/files/" + fileId);
    }

    static Response getFileData(String fileId, String apiToken, String datasetVersionId) {
        return getFileData(fileId, apiToken, datasetVersionId, false, false);
    }

    static Response getFileData(String fileId, String apiToken, String datasetVersionId, boolean includeDeaccessioned, boolean returnDatasetVersion) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .queryParam("includeDeaccessioned", includeDeaccessioned)
                .queryParam("returnDatasetVersion", returnDatasetVersion)
                .get("/api/files/" + fileId + "/versions/" + datasetVersionId);
    }

    static Response getFileVersionDifferences(String fileId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/files/" + fileId + "/versionDifferences");
    }

    static Response testIngest(String fileName, String fileType) {
        return given()
                .get("/api/ingest/test/file?fileName=" + fileName + "&fileType=" + fileType);
    }

    static Response redetectFileType(String fileId, boolean dryRun, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .post("/api/files/" + fileId + "/redetect?dryRun=" + dryRun);
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

    public static Response deactivateUser(String username) {
        Response deactivateUserResponse = given()
                .post("/api/admin/authenticatedUsers/" + username + "/deactivate");
        return deactivateUserResponse;
    }

    public static Response deactivateUser(Long userId) {
        Response deactivateUserResponse = given()
                .post("/api/admin/authenticatedUsers/id/" + userId + "/deactivate");
        return deactivateUserResponse;
    }

    public static Response deleteUser(String username) {
        Response deleteUserResponse = given()
                .delete("/api/admin/authenticatedUsers/" + username + "/");
        return deleteUserResponse;
    }

    public static Response deleteUserRoles(String username, String apiToken) {
        Response deleteUserResponse = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .post("/api/users/" + username + "/removeRoles");
        return deleteUserResponse;
    }

    public static Response getUserTraces(String username, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/users/" + username + "/traces");
        return response;
    }

    public static Response getUserPermittedCollections(String username, String apiToken, String permission) {
        RequestSpecification requestSpecification = given();
        if (!StringUtil.isEmpty(apiToken)) {
            requestSpecification.header(API_TOKEN_HTTP_HEADER, apiToken);
        }
        Response response = requestSpecification.get("/api/users/" + username + "/allowedCollections/" + permission);
        return response;
    }

    public static Response reingestFile(Long fileId, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .post("/api/files/" + fileId + "/reingest");
        return response;
    }

    public static Response uningestFile(Long fileId, String apiToken) {
        
        Response uningestFileResponse = given()
                .post("/api/files/" + fileId + "/uningest/?key=" + apiToken);
        return uningestFileResponse;
    }

    public static Response extractNcml(Long fileId, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .post("/api/files/" + fileId + "/extractNcml");
        return response;
    }

    //I don't understand why this blows up when I remove the key
    public static Response getDataFileMetadata(Long fileId, String apiToken) {
        Response fileResponse = given()
                .get("api/files/" + fileId + "/metadata/?key=" + apiToken);
        return fileResponse;
    }
    
    public static Response getDataFileMetadataDraft(Long fileId, String apiToken) {
        Response fileResponse = given()
                .get("api/files/" + fileId + "/metadata/draft?key=" + apiToken);
        return fileResponse;
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

    static Response destroyDataset(String pid, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("/api/datasets/:persistentId/destroy?persistentId=" + pid);
    }

    static Response deleteFile(Integer fileId, String apiToken) {
        return given()
                .auth().basic(apiToken, EMPTY_STRING)
                .relaxedHTTPSValidation()
                .delete(swordConfiguration.getBaseUrlPathCurrent() + "/edit-media/file/" + fileId);
    }

    static Response publishDatasetViaSword(String persistentId, String apiToken) {
        Response publishResponse =  given()
                .auth().basic(apiToken, EMPTY_STRING)
                .header("In-Progress", "false")
                .post(swordConfiguration.getBaseUrlPathCurrent() + "/edit/study/" + persistentId);
        
        // Wait for the dataset to get unlocked, if/as needed:
        sleepForLock(persistentId, "finalizePublication", apiToken, UtilIT.MAXIMUM_PUBLISH_LOCK_DURATION);
        return publishResponse;
    }

    static Response publishDatasetViaNativeApi(String idOrPersistentId, String majorOrMinor, String apiToken) {
        return publishDatasetViaNativeApi(idOrPersistentId, majorOrMinor, apiToken, false);
    }
    
    static Response publishDatasetViaNativeApi(String idOrPersistentId, String majorOrMinor, String apiToken, boolean mustBeIndexed) {

        String idInPath = idOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(idOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "&persistentId=" + idOrPersistentId;
        }
        if(mustBeIndexed) {
            optionalQueryParam = optionalQueryParam+"&assureIsIndexed=true";
        }
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .urlEncodingEnabled(false)
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        Response publishResponse = requestSpecification.post("/api/datasets/" + idInPath + "/actions/:publish?type=" + majorOrMinor + optionalQueryParam);
        
        // Wait for the dataset to get unlocked, if/as needed:
        sleepForLock(idOrPersistentId, "finalizePublication", apiToken, UtilIT.MAXIMUM_PUBLISH_LOCK_DURATION);
        
        return publishResponse;
    }

    static Response publishDatasetViaNativeApiDeprecated(String persistentId, String majorOrMinor, String apiToken) {
        /**
         * @todo This should be a POST rather than a GET:
         * https://github.com/IQSS/dataverse/issues/2431
         */
        Response publishResponse = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .urlEncodingEnabled(false)
                .get("/api/datasets/:persistentId/actions/:publish?type=" + majorOrMinor + "&persistentId=" + persistentId);
        
        // Wait for the dataset to get unlocked, if/as needed:
        sleepForLock(persistentId, "finalizePublication", apiToken, UtilIT.MAXIMUM_PUBLISH_LOCK_DURATION);
        
        return publishResponse;
    }
   
    // Compatibility method wrapper (takes Integer for the dataset id) 
    // It used to use the GET version of the publish API; I'm switching it to 
    // use the new POST variant. The explicitly marked @deprecated method above
    // should be sufficient for testing the deprecated API call. 
    static Response publishDatasetViaNativeApi(Integer datasetId, String majorOrMinor, String apiToken) {
        return publishDatasetViaNativeApi(datasetId.toString(), majorOrMinor, apiToken);
    }
    
    static Response modifyDatasetPIDMetadataViaApi(String persistentId,  String apiToken) {
        /**
         * @todo This should be a POST rather than a GET:
         * https://github.com/IQSS/dataverse/issues/2431
         */
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .urlEncodingEnabled(false)
                .get("/api/datasets/:persistentId/&persistentId=" + persistentId);
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

    static Response deleteNotification(long id, String apiToken) {
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.delete("/api/notifications/" + id);
    }

    static Response nativeGetUsingPersistentId(String persistentId, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/datasets/:persistentId/?persistentId=" + persistentId);
        return response;
    }

    static Response getDatasetVersion(String persistentId, String versionNumber, String apiToken) {
        return getDatasetVersion(persistentId, versionNumber, apiToken, false, false);
    }

    static Response getDatasetVersion(String persistentId, String versionNumber, String apiToken, boolean excludeFiles, boolean includeDeaccessioned) {
        return getDatasetVersion(persistentId,versionNumber,apiToken,excludeFiles,false,includeDeaccessioned);
    }
    static Response getDatasetVersion(String persistentId, String versionNumber, String apiToken, boolean excludeFiles,boolean excludeMetadataBlocks, boolean includeDeaccessioned) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .queryParam("includeDeaccessioned", includeDeaccessioned)
                .get("/api/datasets/:persistentId/versions/"
                        + versionNumber
                        + "?persistentId="
                        + persistentId
                        + (excludeFiles ? "&excludeFiles=true" : "")
                        + (excludeMetadataBlocks ? "&excludeMetadataBlocks=true" : ""));
    }
    static Response compareDatasetVersions(String persistentId, String versionNumber1, String versionNumber2, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/datasets/:persistentId/versions/"
                        + versionNumber1
                        + "/compare/"
                        + versionNumber2
                        + "?persistentId="
                        + persistentId);
    }
    
    static Response compareDatasetVersions(String persistentId, String versionNumber1, String versionNumber2, String apiToken, boolean includeDeaccessioned) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/datasets/:persistentId/versions/"
                        + versionNumber1
                        + "/compare/"
                        + versionNumber2
                        + "?persistentId="
                        + persistentId
                        + "&includeDeaccessioned="
                        + includeDeaccessioned);
    }
    
    static Response summaryDatasetVersionDifferences(String persistentId,  String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/datasets/:persistentId/versions/compareSummary"
                        + "?persistentId="
                        + persistentId);
    }
    static Response getDatasetWithOwners(String persistentId,  String apiToken, boolean returnOwners) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/datasets/:persistentId/"
                        + "?persistentId="
                        + persistentId
                        + (returnOwners ? "&returnOwners=true" : ""));
    }
    
    static Response getFileWithOwners(String datafileId,  String apiToken, boolean returnOwners) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/files/"
                        + datafileId
                        + (returnOwners ? "/?returnOwners=true" : ""));
    }
    
    static Response getDataverseWithOwners(String alias,  String apiToken, boolean returnOwners) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/dataverses/"
                        + alias
                        + (returnOwners ? "/?returnOwners=true" : ""));
    }

    static Response getDataverseWithChildCount(String alias,  String apiToken, boolean returnChildCount) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/dataverses/"
                        + alias
                        + (returnChildCount ? "/?returnChildCount=true" : ""));
    }

    static Response getMetadataBlockFromDatasetVersion(String persistentId, String versionNumber, String metadataBlock, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/datasets/:persistentId/versions/" + DS_VERSION_LATEST_PUBLISHED + "/metadata/citation?persistentId=" + persistentId);
    }

    @Deprecated
    static Response makeSuperUser(String username) {
        Response response = given().post("/api/admin/superuser/" + username);
        return response;
    }

    static Response setSuperuserStatus(String username, Boolean isSuperUser) {
        Response response = given().body(isSuperUser).put("/api/admin/superuser/" + username);
        return response;
    }

    static Response reindexDataset(String persistentId) {
        Response response = given()
                .get("/api/admin/index/dataset?persistentId=" + persistentId);
        return response;
    }
    
    static Response indexClearDataset(Integer datasetId) {
        return given()
                .delete("/api/admin/index/datasets/"+datasetId);
    }
    
    static Response reindexDataverse(String dvId) {
        Response response = given()
                .get("/api/admin/index/dataverses/" + dvId);
        return response;
    }

    static Response listAuthenticatedUsers(String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/admin/authenticatedUsers");
        return response;
    }

    // TODO: Consider removing apiToken since it isn't used by the API itself.
    static Response getAuthenticatedUser(String userIdentifier, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/admin/authenticatedUsers/" + userIdentifier);
        return response;
    }
    
    static Response getAuthenticatedUserByToken(String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .urlEncodingEnabled(false)
                .get("/api/users/:me");
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
            Integer itemsPerPage,
            String sortKey
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
        if (StringUtils.isNotBlank(sortKey)) {
            queryParams.add("sortKey=" + sortKey);
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

    static Response restrictFile(String fileIdOrPersistentId, String restrict, String apiToken) {
        String idInPath = fileIdOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(fileIdOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + fileIdOrPersistentId;
        }
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(restrict)
                .put("/api/files/" + idInPath + "/restrict" + optionalQueryParam);
        return response;
    }
    static Response restrictFile(String fileIdOrPersistentId, boolean restrict, String apiToken) {
        String idInPath = fileIdOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(fileIdOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + fileIdOrPersistentId;
        }
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(restrict)
                .put("/api/files/" + idInPath + "/restrict" + optionalQueryParam);
        return response;
    }
    
    static Response allowAccessRequests(String datasetIdOrPersistentId, boolean allowRequests, String apiToken) {
        String idInPath = datasetIdOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(datasetIdOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + datasetIdOrPersistentId;
        }
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(allowRequests)
                .put("/api/access/" + idInPath + "/allowAccessRequest" + optionalQueryParam);
        return response;
    }

    static Response requestFileAccess(String fileIdOrPersistentId, String apiToken) {
        System.out.print ("Reuest file acceess + fileIdOrPersistentId: " + fileIdOrPersistentId);
        System.out.print ("Reuest file acceess + apiToken: " + apiToken);
        String idInPath = fileIdOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(fileIdOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + fileIdOrPersistentId;
        }

        String keySeparator = "&";
        if (optionalQueryParam.isEmpty()) {
            keySeparator = "?";
        }
        System.out.print ("URL:  " + "/api/access/datafile/" + idInPath + "/requestAccess" + optionalQueryParam + keySeparator + "key=" + apiToken);
        Response response = given()
                .put("/api/access/datafile/" + idInPath + "/requestAccess" + optionalQueryParam + keySeparator + "key=" + apiToken);
        return response;
    }

    static Response grantFileAccess(String fileIdOrPersistentId, String identifier, String apiToken) {
        String idInPath = fileIdOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(fileIdOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + fileIdOrPersistentId;
        }
        String keySeparator = "&";
        if (optionalQueryParam.isEmpty()) {
            keySeparator = "?";
        }
        Response response = given()
                .put("/api/access/datafile/" + idInPath + "/grantAccess/" + identifier + "/" + optionalQueryParam + keySeparator + "key=" + apiToken);
        return response;
    }

    static Response getAccessRequestList(String fileIdOrPersistentId, String apiToken) {
        String idInPath = fileIdOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(fileIdOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + fileIdOrPersistentId;
        }
        String keySeparator = "&";
        if (optionalQueryParam.isEmpty()) {
            keySeparator = "?";
        }
        Response response = given()
                .get("/api/access/datafile/" + idInPath + "/listRequests/" + optionalQueryParam + keySeparator + "key=" + apiToken);
        return response;
    }

    static Response rejectFileAccessRequest(String fileIdOrPersistentId, String identifier, String apiToken) {
        String idInPath = fileIdOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(fileIdOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + fileIdOrPersistentId;
        }
        String keySeparator = "&";
        if (optionalQueryParam.isEmpty()) {
            keySeparator = "?";
        }
        Response response = given()
                .put("/api/access/datafile/" + idInPath + "/rejectAccess/" + identifier + "/" + optionalQueryParam + keySeparator + "key=" + apiToken);
        return response;
    }

    static Response moveDataverse(String movedDataverseAlias, String targetDataverseAlias, Boolean force, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .post("api/dataverses/" + movedDataverseAlias + "/move/" + targetDataverseAlias + "?forceMove=" + force + "&key=" + apiToken);
        return response;
    }

    static Response moveDataset(String idOrPersistentIdOfDatasetToMove, String destinationDataverseAlias, String apiToken) {
        return moveDataset(idOrPersistentIdOfDatasetToMove, destinationDataverseAlias, false, apiToken);
    }

    static Response moveDataset(String idOrPersistentIdOfDatasetToMove, String destinationDataverseAlias, boolean force, String apiToken) {
        String forceMoveQueryParam = "";
        if (force) {
            // FIXME: Make "&" version work (instead of "?").
            forceMoveQueryParam = "?forceMove=true";
        }
        String idInPath = idOrPersistentIdOfDatasetToMove; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(idOrPersistentIdOfDatasetToMove)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + idOrPersistentIdOfDatasetToMove;
        }
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.post("/api/datasets/" + idInPath + "/move/" + destinationDataverseAlias + optionalQueryParam + forceMoveQueryParam);
    }

    static Response linkDataset(String datasetToLinkPid, String dataverseAlias, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .put("api/datasets/:persistentId/link/" + dataverseAlias + "?persistentId=" + datasetToLinkPid);
        return response;
    }

    static Response getDatasetLinks(String datasetToLinkPid, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("api/datasets/:persistentId/links" + "?persistentId=" + datasetToLinkPid);
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

    static Response privateUrlCreate(Integer datasetId, String apiToken, boolean anonymizedAccess) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .queryParam("anonymizedAccess", anonymizedAccess)
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

    /**
     * @param trueOrWidthInPixels Passing "true" will result in the default width in pixels (64).
     */
    static Response getFileThumbnail(String fileDatabaseId, String trueOrWidthInPixels, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/access/datafile/" + fileDatabaseId + "?imageThumb=" + trueOrWidthInPixels);
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
        return getDatasetVersions(idOrPersistentId, apiToken, false);
    }

    static Response getDatasetVersions(String idOrPersistentId, String apiToken, boolean excludeFiles) {
        return getDatasetVersions(idOrPersistentId, apiToken, null, null, excludeFiles);
    }

    static Response getDatasetVersions(String idOrPersistentId, String apiToken, Integer offset, Integer limit) {
        return getDatasetVersions(idOrPersistentId, apiToken, offset, limit, false);
    }

    static Response getDatasetVersions(String idOrPersistentId, String apiToken, Integer offset, Integer limit, boolean excludeFiles) {
        logger.info("Getting Dataset Versions");
        String idInPath = idOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(idOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + idOrPersistentId;
        }
        if (excludeFiles) {
            if ("".equals(optionalQueryParam)) {
                optionalQueryParam = "?excludeFiles=true";
            } else {
                optionalQueryParam = optionalQueryParam.concat("&excludeFiles=true");
            }
        }
        if (offset != null) {
            if ("".equals(optionalQueryParam)) {
                optionalQueryParam = "?offset="+offset;
            } else {
                optionalQueryParam = optionalQueryParam.concat("&offset="+offset);
            }
        }
        if (limit != null) {
            if ("".equals(optionalQueryParam)) {
                optionalQueryParam = "?limit="+limit;
            } else {
                optionalQueryParam = optionalQueryParam.concat("&limit="+limit);
            }
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
        if (!NumberUtils.isCreatable(idOrPersistentId)) {
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
        if (!NumberUtils.isCreatable(idOrPersistentId)) {
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
        if (!NumberUtils.isCreatable(idOrPersistentId)) {
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
        if (!NumberUtils.isCreatable(idOrPersistentId)) {
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
        if (!NumberUtils.isCreatable(idOrPersistentId)) {
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
//        if (!NumberUtils.isCreatable(idOrPersistentId)) {
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
    static Response exportDataset(String datasetPersistentId, String exporter) {
        return exportDataset(datasetPersistentId, exporter, null, false);
    }
    static Response exportDataset(String datasetPersistentId, String exporter, String apiToken) {
        return exportDataset(datasetPersistentId, exporter, apiToken, false);
    }
    static Response exportDataset(String datasetPersistentId, String exporter, String apiToken, boolean wait) {
        // Wait for the Async call to finish to get the updated data
        if (wait) {
            sleepForReexport(datasetPersistentId, apiToken, 10);
        }
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification
                .get("/api/datasets/export" + "?persistentId=" + datasetPersistentId + "&exporter=" + exporter);
    }

    static Response reexportDatasetAllFormats(String idOrPersistentId) {
        String idInPath = idOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isDigits(idOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + idOrPersistentId;
        }
        return given()
                .get("/api/admin/metadata/" + idInPath + "/reExportDataset" + optionalQueryParam);
    }

    static Response exportDataverse(String identifier, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/dataverses/" + identifier );
    }
    
    static Response search(String query, String apiToken, String parameterString) {
        sleepForDatasetIndex(query, apiToken);
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.get("/api/search?q=" + query + parameterString);
    }

    private static void sleepForDatasetIndex(String query, String apiToken) {
        if (query.contains("id:dataset") || query.contains("id:datafile")) {
            String[] splitted = query.split("_");
            if (splitted.length >= 2) {
                boolean ok = UtilIT.sleepForReindex(String.valueOf(splitted[1]), apiToken, 5);
                if (!ok) {
                    logger.info("Still indexing after 5 seconds");
                }
            }
        }
    }

    static Response search(String query, String apiToken) {
        return search(query, apiToken, "");
    }

    static Response searchAndShowFacets(String query, String apiToken) {
        sleepForDatasetIndex(query, apiToken);
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

    static Response deleteSetting(SettingsServiceBean.Key settingKey, String language) {
        Response response = given().when().delete("/api/admin/settings/" + settingKey + "/lang/" + language);
        return response;
    }

    /**
     * @param settingKey Include the colon like :BagItLocalPath
     */
    static Response deleteSetting(String settingKey) {
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

    static Response setSetting(SettingsServiceBean.Key settingKey, String value, String language) {
        Response response = given().body(value).when().put("/api/admin/settings/" + settingKey + "/lang/" + language);
        return response;
    }

    /**
     * @param settingKey Include the colon like :BagItLocalPath
     */
    public static Response setSetting(String settingKey, String value) {
        Response response = given().body(value).when().put("/api/admin/settings/" + settingKey);
        return response;
    }

    static Response getFeatureFlags() {
        Response response = given().when().get("/api/admin/featureFlags");
        return response;
    }

    static Response getFeatureFlag(FeatureFlags featureFlag) {
        Response response = given().when().get("/api/admin/featureFlags/" + featureFlag);
        return response;
    }

    static Response getRoleAssignmentsOnDataverse(String dataverseAliasOrId, String apiToken) {
        String url = "/api/dataverses/" + dataverseAliasOrId + "/assignments";
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get(url);
    }
    
    static Response updateDefaultContributorsRoleOnDataverse(String dataverseAliasOrId,String roleAlias, String apiToken) {
        String url = "/api/dataverses/" + dataverseAliasOrId + "/defaultContributorRole/" + roleAlias;
        System.out.println("URL: " + url);
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .put(url);
    }

    static Response getRoleAssignmentsOnDataset(String datasetId, String persistentId, String apiToken) {
        String url = "/api/datasets/" + datasetId + "/assignments";
        System.out.println("URL: " + url);
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get(url);
    }

    static Response grantRoleOnDataset(String definitionPoint, String role, String roleAssignee, String apiToken) {

        JsonObjectBuilder roleBuilder = Json.createObjectBuilder();
        roleBuilder.add("assignee", roleAssignee);
        roleBuilder.add("role", role);
        
        JsonObject roleObject = roleBuilder.build();
        logger.info("Granting role on dataset \"" + definitionPoint + "\": " + role);
        return given()
                .body(roleObject.toString())
                .contentType(ContentType.JSON)
                .post("api/datasets/:persistentId/assignments?key=" + apiToken + "&persistentId=" + definitionPoint);
    }

    static Response revokeRole(String definitionPoint, long doomed, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("api/dataverses/" + definitionPoint + "/assignments/" + doomed);
    }
    
    static Response revokeRoleOnDataset(String definitionPoint, long doomed, String apiToken) {
        
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("api/datasets/:persistentId/assignments/" + doomed + "?persistentId=" + definitionPoint);
    }
    
    static Response revokeFileAccess(String definitionPoint, String doomed, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("api/access/datafile/" + definitionPoint + "/revokeAccess/" + doomed);
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
    
    static Response setCollectionAttribute(String dataverseAlias, String attribute, String value, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .put("/api/dataverses/" + dataverseAlias + "/attribute/" + attribute + "?value=" + value);
    }

    /**
     * Deprecated as the apiToken is not used by the call.
     */
    @Deprecated
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
        if (!NumberUtils.isCreatable(datasetPersistentId)) {
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

    static Response getExternalTool(long id) {
        return given()
                .get("/api/admin/externalTools/" + id);
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

// ExternalTools with token
    static Response getExternalTools(String apiToken) {
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification.header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.get("/api/externalTools");
    }

    static Response getExternalTool(long id, String apiToken) {
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification.header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.get("/api/externalTools/" + id);
    }

    static Response addExternalTool(JsonObject jsonObject, String apiToken) {
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification.header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification
                .body(jsonObject.toString())
                .contentType(ContentType.JSON)
                .post("/api/externalTools");
    }

    static Response deleteExternalTool(long externalToolid, String apiToken) {
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification.header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.delete("/api/externalTools/" + externalToolid);
    }

    static Response getExternalToolsForDataset(String idOrPersistentIdOfDataset, String type, String apiToken) {
        String idInPath = idOrPersistentIdOfDataset; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(idOrPersistentIdOfDataset)) {
            idInPath = ":persistentId";
            optionalQueryParam = "&persistentId=" + idOrPersistentIdOfDataset;
        }
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.get("/api/admin/test/datasets/" + idInPath + "/externalTools?type=" + type + optionalQueryParam);
    }
    
    static Response getExternalToolForDatasetById(String idOrPersistentIdOfDataset, String type, String apiToken, String toolId) {
        String idInPath = idOrPersistentIdOfDataset; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(idOrPersistentIdOfDataset)) {
            idInPath = ":persistentId";
            optionalQueryParam = "&persistentId=" + idOrPersistentIdOfDataset;
        }
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.get("/api/admin/test/datasets/" + idInPath + "/externalTool/" + toolId + "?type=" + type + optionalQueryParam);
    }

    static Response getExternalToolsForFile(String idOrPersistentIdOfFile, String type, String apiToken) {
        String idInPath = idOrPersistentIdOfFile; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(idOrPersistentIdOfFile)) {
            idInPath = ":persistentId";
            optionalQueryParam = "&persistentId=" + idOrPersistentIdOfFile;
        }
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.get("/api/admin/test/files/" + idInPath + "/externalTools?type=" + type + optionalQueryParam);
    }
    
    static Response getExternalToolForFileById(String idOrPersistentIdOfFile, String type, String apiToken, String toolId) {
        String idInPath = idOrPersistentIdOfFile; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(idOrPersistentIdOfFile)) {
            idInPath = ":persistentId";
            optionalQueryParam = "&persistentId=" + idOrPersistentIdOfFile;
        }
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.get("/api/admin/test/files/" + idInPath + "/externalTool/" + toolId + "?type=" + type + optionalQueryParam);
    }

    static Response submitFeedback(JsonObjectBuilder job) {
        return given()
                .body(job.build().toString())
                .contentType("application/json")
                .post("/api/admin/feedback");
    }
    static Response sendFeedback(String json, String apiToken) {
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification
                .body(json)
                .post("/api/sendfeedback");
    }
    static Response sendFeedback(JsonObjectBuilder job, String apiToken) {
        return sendFeedback(job.build().toString(), apiToken);
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

    static Response listStorageDrivers(String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/admin/dataverse/storageDrivers");
    }

    static Response getStorageDriver(String dvAlias, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/admin/dataverse/" + dvAlias + "/storageDriver");
    }

    static Response setStorageDriver(String dvAlias, String label, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(label)
                .put("/api/admin/dataverse/" + dvAlias + "/storageDriver");
    }

    static Response getUploadUrls(String idOrPersistentIdOfDataset, long sizeInBytes, String apiToken) {
        String idInPath = idOrPersistentIdOfDataset; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(idOrPersistentIdOfDataset)) {
            idInPath = ":persistentId";
            optionalQueryParam = "&persistentId=" + idOrPersistentIdOfDataset;
        }
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.get("/api/datasets/" + idInPath + "/uploadurls?size=" + sizeInBytes + optionalQueryParam);
    }

    /**
     * If you set dataverse.files.localstack1.disable-tagging=true you will see
     * an error like below.
     *
     * To avoid it, don't send the x-amz-tagging header.
     */
    /*
    <Error>
      <Code>AccessDenied</Code>
      <Message>There were headers present in the request which were not signed</Message>
      <RequestId>25ff2bb0-13c7-420e-8ae6-3d92677e4bd9</RequestId>
      <HostId>9Gjjt1m+cjU4OPvX9O9/8RuvnG41MRb/18Oux2o5H5MY7ISNTlXN+Dz9IG62/ILVxhAGI0qyPfg=</HostId>
      <HeadersNotSigned>x-amz-tagging</HeadersNotSigned>
    </Error>
     */
    static Response uploadFileDirect(String url, InputStream inputStream) {
        return given()
                .header("x-amz-tagging", "dv-state=temp")
                .body(inputStream)
                .put(url);
    }

    static Response downloadFileNoRedirect(Integer fileId, String apiToken) {
        return given().when().redirects().follow(false)
                .get("/api/access/datafile/" + fileId + "?key=" + apiToken);
    }

    static Response downloadFromUrl(String url) {
        return given().get(url);
    }

    static Response getAppTermsOfUse() {
        return getAppTermsOfUse(null);
    }

    static Response getAppTermsOfUse(String lang) {
        String optionalLang = "";
        if (lang != null) {
            optionalLang = "?lang=" + lang;
        }
        return given().get("/api/info/applicationTermsOfUse" + optionalLang);
    }

    static Response metricsDataversesToMonth(String yyyymm, String queryParams) {
        String optionalYyyyMm = "";
        if (yyyymm != null) {
            optionalYyyyMm = "/" + yyyymm;
        }
        String optionalQueryParams = "";
        if (queryParams != null) {
            optionalQueryParams = "?" + queryParams;
        }
        RequestSpecification requestSpecification = given();
        return requestSpecification.get("/api/info/metrics/dataverses/toMonth" + optionalYyyyMm + optionalQueryParams);
    }

    static Response metricsDatasetsToMonth(String yyyymm, String queryParams) {
        String optionalYyyyMm = "";
        if (yyyymm != null) {
            optionalYyyyMm = "/" + yyyymm;
        }
        String optionalQueryParams = "";
        if (queryParams != null) {
            optionalQueryParams = "?" + queryParams;
        }
        RequestSpecification requestSpecification = given();
        return requestSpecification.get("/api/info/metrics/datasets/toMonth" + optionalYyyyMm + optionalQueryParams);
    }

    static Response metricsFilesToMonth(String yyyymm, String queryParams) {
        String optionalYyyyMm = "";
        if (yyyymm != null) {
            optionalYyyyMm = "/" + yyyymm;
        }
        String optionalQueryParams = "";
        if (queryParams != null) {
            optionalQueryParams = "?" + queryParams;
        }
        RequestSpecification requestSpecification = given();
        return requestSpecification.get("/api/info/metrics/files/toMonth" + optionalYyyyMm + optionalQueryParams);
    }

    static Response metricsDownloadsToMonth(String yyyymm, String queryParams) {
        String optionalYyyyMm = "";
        if (yyyymm != null) {
            optionalYyyyMm = "/" + yyyymm;
        }
        String optionalQueryParams = "";
        if (queryParams != null) {
            optionalQueryParams = "?" + queryParams;
        }
        RequestSpecification requestSpecification = given();
        return requestSpecification.get("/api/info/metrics/downloads/toMonth" + optionalYyyyMm + optionalQueryParams);
    }

    static Response metricsAccountsToMonth(String yyyymm, String queryParams) {
        String optionalQueryParams = "";
        if (queryParams != null) {
            optionalQueryParams = "?" + queryParams;
        }
        RequestSpecification requestSpecification = given();
        return requestSpecification.get("/api/info/metrics/accounts/toMonth/" + yyyymm + optionalQueryParams);
    }

    static Response metricsAccountsTimeSeries(String mediaType, String queryParams) {
        String optionalQueryParams = "";
        if (queryParams != null) {
            optionalQueryParams = "?" + queryParams;
        }
        RequestSpecification requestSpecification = given();
        requestSpecification.contentType(mediaType);
        return requestSpecification.get("/api/info/metrics/accounts/monthly" + optionalQueryParams);
    }
    
    static Response metricsDataversesPastDays(String days, String queryParams) {
        String optionalQueryParams = "";
        if (queryParams != null) {
            optionalQueryParams = "?" + queryParams;
        }
        RequestSpecification requestSpecification = given();
        return requestSpecification.get("/api/info/metrics/dataverses/pastDays/" + days + optionalQueryParams);
    }
    
    static Response metricsDatasetsPastDays(String days, String queryParams) {
        String optionalQueryParams = "";
        if (queryParams != null) {
            optionalQueryParams = "?" + queryParams;
        }
        RequestSpecification requestSpecification = given();
        return requestSpecification.get("/api/info/metrics/datasets/pastDays/" + days + optionalQueryParams);
    }
        
    static Response metricsFilesPastDays(String days, String queryParams) {
        String optionalQueryParams = "";
        if (queryParams != null) {
            optionalQueryParams = "?" + queryParams;
        }
        RequestSpecification requestSpecification = given();
        return requestSpecification.get("/api/info/metrics/files/pastDays/" + days + optionalQueryParams);
    }
            
    static Response metricsDownloadsPastDays(String days, String queryParams) {
        String optionalQueryParams = "";
        if (queryParams != null) {
            optionalQueryParams = "?" + queryParams;
        }
        RequestSpecification requestSpecification = given();
        return requestSpecification.get("/api/info/metrics/downloads/pastDays/" + days + optionalQueryParams);
    }

    static Response metricsAccountsPastDays(String days, String queryParams) {
        String optionalQueryParams = "";
        if (queryParams != null) {
            optionalQueryParams = "?" + queryParams;
        }
        RequestSpecification requestSpecification = given();
        return requestSpecification.get("/api/info/metrics/accounts/pastDays/" + days + optionalQueryParams);
    }

    static Response metricsDataversesByCategory(String queryParams) {
        String optionalQueryParams = "";
        if (queryParams != null) {
            optionalQueryParams = "?" + queryParams;
        }
        RequestSpecification requestSpecification = given();
        return requestSpecification.get("/api/info/metrics/dataverses/byCategory" + optionalQueryParams);
    }
    
    static Response metricsDataversesBySubject(String queryParams) {
        String optionalQueryParams = "";
        if (queryParams != null) {
            optionalQueryParams = "?" + queryParams;
        }
        RequestSpecification requestSpecification = given();
        return requestSpecification.get("/api/info/metrics/dataverses/bySubject" + optionalQueryParams);
    }

    static Response metricsDatasetsBySubject(String queryParams) {
        String optionalQueryParams = "";
        if (queryParams != null) {
            optionalQueryParams = "?" + queryParams;
        }
        RequestSpecification requestSpecification = given();
        return requestSpecification.get("/api/info/metrics/datasets/bySubject" + optionalQueryParams);
    }
    
    static Response metricsDatasetsBySubjectToMonth(String month, String queryParams) {
        String optionalQueryParams = "";
        if (queryParams != null) {
            optionalQueryParams = "?" + queryParams;
        }
        RequestSpecification requestSpecification = given();
        return requestSpecification.get("/api/info/metrics/datasets/bySubject/toMonth/" + month + optionalQueryParams);
    }
    
    public static Response makeDataCountMetricTimeSeries(String metricType, String queryParams) {
        String apiPath = "/api/v1/metrics/makeDataCount/" + metricType + "/monthly";
        
        Response response = given()
                .get(apiPath + (queryParams != null && !queryParams.isEmpty() ? "?" + queryParams : ""));
        
        return response;
    }
    
    static Response clearMetricCache() {
        RequestSpecification requestSpecification = given();
        return requestSpecification.delete("/api/admin/clearMetricsCache");
    }
    
    static Response clearMetricCache(String name) {
        RequestSpecification requestSpecification = given();
        return requestSpecification.delete("/api/admin/clearMetricsCache/" + name);
    }

    static Response rateLimitStats(String apiToken, String deltaMinutesFilter) {
        String queryParams = deltaMinutesFilter != null ? "?deltaMinutesFilter=" + deltaMinutesFilter : "";
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/admin/rateLimitStats" + queryParams);
    }

    static Response sitemapUpdate() {
        return given()
                .post("/api/admin/sitemap");
    }

    static Response sitemapDownload() {
        return given()
                .get("/sitemap.xml");
    }
    
    static Response deleteToken( String apiToken) {
        Response response = given()
            .header(API_TOKEN_HTTP_HEADER, apiToken)
            .delete("api/users/token");
        return response;
    }
    
    static Response getTokenExpiration( String apiToken) {
        Response response = given()
            .header(API_TOKEN_HTTP_HEADER, apiToken)
            .get("api/users/token");
        return response;
    }
    
    static Response recreateToken(String apiToken) {
        return recreateToken(apiToken, false);
    }

    static Response recreateToken(String apiToken, boolean returnExpiration) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .queryParam("returnExpiration", returnExpiration)
                .post("api/users/token/recreate");
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
    
    //Helper function that returns true if a given dataset locked for a  given reason is unlocked within
    // a given duration returns false if still locked after given duration
    // (the version of the method that takes a long for the dataset id is 
    // for backward compatibility with how the method is called for the 
    // Ingest lock throughout the test code)
    static Boolean sleepForLock(long datasetId, String lockType, String apiToken, int duration) {
        String datasetIdAsString = String.valueOf(datasetId);
        return sleepForLock(datasetIdAsString, lockType, apiToken, duration);
    }

    static Boolean sleepForLock(String idOrPersistentId, String lockType, String apiToken, int duration) {
        Response lockedForIngest = UtilIT.checkDatasetLocks(idOrPersistentId, lockType, apiToken);
        int i = 0;
        do {
            try {
                lockedForIngest = UtilIT.checkDatasetLocks(idOrPersistentId, lockType, apiToken);
                Thread.sleep(1000);
                i++;
                if (i > duration) {
                    break; 
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(UtilIT.class.getName()).log(Level.SEVERE, null, ex);
            }
        } while (lockedForIngest.body().jsonPath().getList("data").size() >0 && (lockType==null || lockedForIngest.body().prettyPrint().contains(lockType)));

        return i <= duration;

    }
    
    static boolean sleepForReindex(String idOrPersistentId, String apiToken, int durationInSeconds) {
        int i = 0;
        Response timestampResponse;
        int sleepStep = 500;
        int repeats = durationInSeconds * (1000 / sleepStep);
        boolean stale=true;
        do {
            timestampResponse = UtilIT.getDatasetTimestamps(idOrPersistentId, apiToken);
            System.out.println(timestampResponse.body().asString());
            try {
                String hasStaleIndex = timestampResponse.body().jsonPath().getString("data.hasStaleIndex");
                System.out.println(hasStaleIndex);
                stale = Boolean.parseBoolean(hasStaleIndex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(UtilIT.class.getName()).log(Level.INFO, "no stale index property found", ex);
                stale = false;
            }
            try {
                Thread.sleep(sleepStep);
                i++;
            } catch (InterruptedException ex) {
                Logger.getLogger(UtilIT.class.getName()).log(Level.SEVERE, null, ex);
                i = repeats + 1;
            }
        } while ((i <= repeats) && stale);
        try {
            Thread.sleep(1000);  //Current autoSoftIndexTime - which adds a delay to when the new docs are visible 
            i++;
        } catch (InterruptedException ex) {
            Logger.getLogger(UtilIT.class.getName()).log(Level.SEVERE, null, ex);
            i = repeats + 1;
        }
        System.out.println("Waited " + (i * (sleepStep / 1000.0)) + " seconds");
        return i <= repeats;

    }
    static boolean sleepForReexport(String idOrPersistentId, String apiToken, int durationInSeconds) {
        int i = 0;
        Response timestampResponse;
        int sleepStep = 500;
        int repeats = durationInSeconds * (1000 / sleepStep);
        boolean staleExport=true;
        do {
            timestampResponse = UtilIT.getDatasetTimestamps(idOrPersistentId, apiToken);
            //logger.fine(timestampResponse.body().asString());
            String updateTimeString = timestampResponse.body().jsonPath().getString("data.lastUpdateTime");
            String exportTimeString = timestampResponse.body().jsonPath().getString("data.lastMetadataExportTime");
            if (updateTimeString != null && exportTimeString != null) {
                LocalDateTime updateTime = LocalDateTime.parse(updateTimeString);
                LocalDateTime exportTime = LocalDateTime.parse(exportTimeString);
                if (exportTime.isAfter(updateTime)) {
                    staleExport = false;
                }
            }
            try {
                Thread.sleep(sleepStep);
                i++;
            } catch (InterruptedException ex) {
                Logger.getLogger(UtilIT.class.getName()).log(Level.SEVERE, null, ex);
                i = repeats + 1;
            }
        } while ((i <= repeats) && staleExport);
        System.out.println("Waited " + (i * (sleepStep / 1000.0)) + " seconds for export");
        return i <= repeats;

    }

    // Modeled after sleepForLock but the dataset isn't locked.
    // We have to sleep or we can't perform the next operation.
    static Boolean sleepForDeadlock(int duration) {
        int i = 0;
        do {
            try {
                Thread.sleep(1000);
                i++;
                if (i > duration) {
                    break;
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(UtilIT.class.getName()).log(Level.SEVERE, null, ex);
            }
        } while (true);
        return i <= duration;
    }

    //Helper function that returns true if a given search returns a non-zero response within a fixed time limit
    // a given duration returns false if still zero results after given duration
    static Boolean sleepForSearch(String searchPart, String apiToken,  String subTree, int count, int duration) {
        

        Response searchResponse = UtilIT.search(searchPart, apiToken, subTree);
        //Leave early if search isn't working
        if(searchResponse.statusCode()!=200) {
            logger.warning("Non-200 status in sleepForSearch: " + searchResponse.statusCode());
            return false;
        }
        int i = 0;
        do {
            try {
                searchResponse = UtilIT.search(searchPart, apiToken, subTree);
                Thread.sleep(1000);
                i++;
                if (i > duration) {
                    break; 
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(UtilIT.class.getName()).log(Level.SEVERE, null, ex);
            }
        } while (UtilIT.getSearchCountFromResponse(searchResponse) != count);
        logger.info("Waited " + i + " seconds in sleepForSearch");
        return i <= duration;

    }
    
    // backward compatibility version of the method that takes long for the id:
    static Response checkDatasetLocks(long datasetId, String lockType, String apiToken) {
        String datasetIdAsString = String.valueOf(datasetId);
        return checkDatasetLocks(datasetIdAsString, lockType, apiToken);
    }
    
    static Response checkDatasetLocks(String idOrPersistentId, String lockType, String apiToken) {
        String idInPath = idOrPersistentId; // Assume it's a number.
        String queryParams = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(idOrPersistentId)) {
            idInPath = ":persistentId";
            queryParams = "?persistentId=" + idOrPersistentId;
        }
        
        if (lockType != null) {
            queryParams = "".equals(queryParams) ? "?type="+lockType : queryParams+"&type="+lockType;
        }
        
        Response response = given()
            .header(API_TOKEN_HTTP_HEADER, apiToken)
            .get("api/datasets/" + idInPath + "/locks" + queryParams);
        return response;       
    }
    
    static Response listAllLocks(String apiToken) {        
        Response response = given()
            .header(API_TOKEN_HTTP_HEADER, apiToken)
            .get("api/datasets/locks");
        return response;       
    }
    
    static Response listLocksByType(String lockType, String apiToken) {        
        Response response = given()
            .header(API_TOKEN_HTTP_HEADER, apiToken)
            .get("api/datasets/locks?type="+lockType);
        return response;       
    }
    
    static Response listLocksByUser(String userIdentifier, String apiToken) {        
        Response response = given()
            .header(API_TOKEN_HTTP_HEADER, apiToken)
            .get("api/datasets/locks?userIdentifier="+userIdentifier);
        return response;       
    }
    
    static Response listLocksByTypeAndUser(String lockType, String userIdentifier, String apiToken) {        
        Response response = given()
            .header(API_TOKEN_HTTP_HEADER, apiToken)
            .get("api/datasets/locks?type="+lockType+"&userIdentifier="+userIdentifier);
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
    
    static Response getDatasetTimestamps(String idOrPersistentId, String apiToken) {
        String idInPath = idOrPersistentId; // Assume it's a number.
        String queryParams = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(idOrPersistentId)) {
            idInPath = ":persistentId";
            queryParams = "?persistentId=" + idOrPersistentId;
        }
        
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.get("api/datasets/" + idInPath + "/timestamps" + queryParams);
    }
    
    static Response exportOaiSet(String setName) {
        String apiPath = String.format("/api/admin/metadata/exportOAI/%s", setName);
        return given().put(apiPath);
    }
    
    static Response getOaiIdentify() {
        String oaiVerbPath = "/oai?verb=Identify";
        return given().get(oaiVerbPath);
    }
    
    static Response getOaiListMetadataFormats() {
        String oaiVerbPath = "/oai?verb=ListMetadataFormats";
        return given().get(oaiVerbPath);
    }
    
    static Response getOaiListSets() {
        String oaiVerbPath = "/oai?verb=ListSets";
        return given().get(oaiVerbPath);
    }
    
    static Response getOaiRecord(String datasetPersistentId, String metadataFormat) {
        String apiPath = String.format("/oai?verb=GetRecord&identifier=%s&metadataPrefix=%s", datasetPersistentId, metadataFormat);
        return given().get(apiPath);
    }
    
    static Response getOaiListIdentifiers(String setName, String metadataFormat) {
        
        String apiPath;
        if (StringUtil.nonEmpty(setName)) {
            apiPath = String.format("/oai?verb=ListIdentifiers&set=%s&metadataPrefix=%s", setName, metadataFormat);
        } else {
            apiPath = String.format("/oai?verb=ListIdentifiers&metadataPrefix=%s", metadataFormat);
        }
        return given().get(apiPath);
    }
    
    static Response getOaiListIdentifiersWithResumptionToken(String resumptionToken) {
        String apiPath = String.format("/oai?verb=ListIdentifiers&resumptionToken=%s", resumptionToken);
        return given().get(apiPath);
    }

    static Response getOaiListRecords(String setName, String metadataFormat) {
        String apiPath = String.format("/oai?verb=ListRecords&set=%s&metadataPrefix=%s", setName, metadataFormat);
        return given().get(apiPath);
    }
    
    static Response getOaiListRecordsWithResumptionToken(String resumptionToken) {
        String apiPath = String.format("/oai?verb=ListRecords&resumptionToken=%s", resumptionToken);
        return given().get(apiPath);
    }

    static Response changeAuthenticatedUserIdentifier(String oldIdentifier, String newIdentifier, String apiToken) {
        Response response;
        String path = String.format("/api/users/%s/changeIdentifier/%s", oldIdentifier, newIdentifier );
        
        if(null == apiToken) {
            response = given()
                .post(path);
        } else {
            response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .post(path);
        }
        
        return response;
    }
    
    static Response mergeAccounts(String baseId, String consumedId, String apiToken) {
        Response response;
        String path = String.format("/api/users/%s/mergeIntoUser/%s", consumedId, baseId );
        
        if(null == apiToken) {
            response = given()
                .post(path);
        } else {
            response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .post(path);
        }
        
        return response;
    }
    
    

    static Response makeDataCountSendDataToHub() {
        return given().post("/api/admin/makeDataCount/sendToHub");
    }

    // TODO: Think more about if we really need this makeDataCountDownloadFromHub endpoint since we're parsing our own SUSHI reports and inserting rows into our datasetmetrics table.
    static Response makeDataCountDownloadFromHub(String metric) {
        return given().post("/api/admin/makeDataCount/downloadFromHub/" + metric);
    }

    static Response makeDataCountGetMetricForDataset(String idOrPersistentIdOfDataset, String metric, String apiToken) {
        System.out.println("metric: " + metric);
        String idInPath = idOrPersistentIdOfDataset; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(idOrPersistentIdOfDataset)) {
            idInPath = ":persistentId";
            optionalQueryParam = "&persistentId=" + idOrPersistentIdOfDataset;
        }
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.get("/api/datasets/" + idInPath + "/makeDataCount/" + metric + optionalQueryParam);
    }

    static Response makeDataCountGetMetricForDataset(String idOrPersistentIdOfDataset, String metric, String country, String apiToken) {
        System.out.println("metric: " + metric);
        String idInPath = idOrPersistentIdOfDataset; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(idOrPersistentIdOfDataset)) {
            idInPath = ":persistentId";
            optionalQueryParam = "&persistentId=" + idOrPersistentIdOfDataset;
        }
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.get("/api/datasets/" + idInPath + "/makeDataCount/" + metric + "?country=" + country + optionalQueryParam);
    }

    static Response makeDataCountGetMetricForDataset(String idOrPersistentIdOfDataset, String metric, String yyyymm, String country, String apiToken) {
        System.out.println("metric: " + metric);
        String idInPath = idOrPersistentIdOfDataset; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(idOrPersistentIdOfDataset)) {
            idInPath = ":persistentId";
            optionalQueryParam = "&persistentId=" + idOrPersistentIdOfDataset;
        }
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.get("/api/datasets/" + idInPath + "/makeDataCount/" + metric + "/" + yyyymm + "?country=" + country + optionalQueryParam);
    }
    
    static Response makeDataCountAddUsageMetricsFromSushiReport(String idOrPersistentIdOfDataset, String reportOnDisk) {

        String idInPath = idOrPersistentIdOfDataset; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(idOrPersistentIdOfDataset)) {
            idInPath = ":persistentId";
            optionalQueryParam = "&persistentId=" + idOrPersistentIdOfDataset;
        }
        RequestSpecification requestSpecification = given();
        System.out.print("/api/admin/makeDataCount/" + idInPath + "/addUsageMetricsFromSushiReport?reportOnDisk=" + reportOnDisk + optionalQueryParam);
        return requestSpecification.post("/api/admin/makeDataCount/" + idInPath + "/addUsageMetricsFromSushiReport?reportOnDisk=" + reportOnDisk + optionalQueryParam);
    }

    static Response makeDataCountUpdateCitationsForDataset(String idOrPersistentIdOfDataset) {

        String idInPath = idOrPersistentIdOfDataset; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(idOrPersistentIdOfDataset)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + idOrPersistentIdOfDataset;
        }
        RequestSpecification requestSpecification = given();

        return requestSpecification.post("/api/admin/makeDataCount/" + idInPath + "/updateCitationsForDataset"+ optionalQueryParam);
    }

    static Response makeDataCountGetProcessingState(String yearMonth) {
        RequestSpecification requestSpecification = given();
        return requestSpecification.get("/api/admin/makeDataCount/" + yearMonth + "/processingState");
    }
    static Response makeDataCountUpdateProcessingState(String yearMonth, String state) {
        return makeDataCountUpdateProcessingState(yearMonth, state, null);
    }
    static Response makeDataCountUpdateProcessingState(String yearMonth, String state, String server) {
        RequestSpecification requestSpecification = given();
        return requestSpecification.post("/api/admin/makeDataCount/" + yearMonth + "/processingState?state=" + state + (server != null ? "&server=" + server : ""));
    }
    static Response makeDataCountDeleteProcessingState(String yearMonth) {
        RequestSpecification requestSpecification = given();
        return requestSpecification.delete("/api/admin/makeDataCount/" + yearMonth + "/processingState");
    }

    static Response editDDI(String body, String fileId, String apiToken) {
        if (apiToken == null) {
            apiToken = "";
        }
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .contentType(ContentType.XML)
                .accept(ContentType.XML)
                .body(body)
                .when()
                .put("/api/edit/" + fileId);
        return response;

    }

    /**
     * Determine the "payload" storage size of a dataverse
     *
     * @param dataverseId
     * @param apiToken
     * @return response
     */
    static Response findDataverseStorageSize(String dataverseId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/dataverses/" + dataverseId + "/storagesize");
    }
    
    static Response checkCollectionQuota(String collectionId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/dataverses/" + collectionId + "/storage/quota");
    }
    
    static Response setCollectionQuota(String collectionId, long allocatedSize, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .post("/api/dataverses/" + collectionId + "/storage/quota/" + allocatedSize);
        return response;
    }
    
    static Response disableCollectionQuota(String collectionId, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("/api/dataverses/" + collectionId + "/storage/quota");
        return response;
    }
    
    static Response checkCollectionStorageUse(String collectionId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/dataverses/" + collectionId + "/storage/use");
    }
    
    /**
     * Determine the "payload" storage size of a dataverse
     *
     * @param dataverseId
     * @param apiToken
     * @return response
     */
    static Response findDatasetStorageSize(String datasetId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/datasets/" + datasetId + "/storagesize");
    }
    
    static Response findDatasetDownloadSize(String datasetId) {
        return given()
                .get("/api/datasets/" + datasetId + "/versions/" + DS_VERSION_LATEST + "/downloadsize");
    }
    
    static Response findDatasetDownloadSize(String datasetId, String version,  String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/datasets/" + datasetId + "/versions/" + version + "/downloadsize");
    }
    
    static Response addBannerMessage(String pathToJsonFile) {
        String jsonIn = getDatasetJson(pathToJsonFile);
        
        Response addBannerMessageResponse = given()               
                .body(jsonIn)
                .contentType("application/json")
                .post("/api/admin/bannerMessage");
        return addBannerMessageResponse;
    }
    
    static Response addBuiltInRole(String pathToJsonFile) {
        String jsonIn = getDatasetJson(pathToJsonFile);

        Response addBannerMessageResponse = given()
                .body(jsonIn)
                .contentType("application/json")
                .post("/api/admin/roles");
        return addBannerMessageResponse;
    }

    static Response deleteBuiltInRole(String roleAlias) {

        Response addBannerMessageResponse = given()
                .delete("/api/admin/roles/:alias?alias=" +roleAlias);
        return addBannerMessageResponse;
    }

    static Response addDataverseRole(String pathToJsonFile, String dvAlias, String apiToken) {
        String jsonIn = getDatasetJson(pathToJsonFile);

        Response addBannerMessageResponse = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(jsonIn)
                .contentType("application/json")
                .post("/api/roles?dvo="+dvAlias);
        return addBannerMessageResponse;
    }
    
    static Response addFeaturedDataverse (String dvAlias, String featuredDvAlias, String apiToken) {
        
        String jsonString = "[\"" + featuredDvAlias + "\"]";

        Response addFeaturedDataverseResponse = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(jsonString)
                .post("/api/dataverses/"+dvAlias+"/featured/");
        return addFeaturedDataverseResponse;
    }
    
    static Response deleteFeaturedDataverses (String dvAlias, String apiToken) {

        Response deleteFeaturedDataversesResponse = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("/api/dataverses/"+dvAlias+"/featured/");
        return deleteFeaturedDataversesResponse;
    }
    
    static Response getFeaturedDataverses (String dvAlias, String apiToken) {

        Response deleteFeaturedDataversesResponse = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/dataverses/"+dvAlias+"/featured/");
        return deleteFeaturedDataversesResponse;
    }

    static Response deleteDataverseRole( String roleAlias, String apiToken) {

        Response addBannerMessageResponse = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("/api/roles/:alias?alias="+roleAlias);
        return addBannerMessageResponse;
    }
    
    static Response deleteDataverseRoleById( String id, String apiToken) {

        Response addBannerMessageResponse = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("/api/roles/"+id);
        return addBannerMessageResponse;
    }
    
    static Response viewDataverseRole( String roleAlias, String apiToken) {

        Response addBannerMessageResponse = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/roles/:alias?alias="+roleAlias);
        return addBannerMessageResponse;
    }
    
    static Response viewDataverseRoleById( String id, String apiToken) {

        Response addBannerMessageResponse = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/roles/"+id);
        return addBannerMessageResponse;
    }
    
    static Response getBannerMessages() {
        
        Response getBannerMessagesResponse = given()               
                .get("/api/admin/bannerMessage");
        return getBannerMessagesResponse;
    }
    
    static Response deleteBannerMessage(Long id) {
        
        Response deleteBannerMessageResponse = given()               
                .delete("/api/admin/bannerMessage/"+id.toString());
        return deleteBannerMessageResponse;
    }
    
    static Response getDatasetJsonLDMetadata(Integer datasetId, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .accept("application/ld+json")
                .get("/api/datasets/" + datasetId + "/metadata");
        return response;
    }
    
    static Response addLicense(String pathToJsonFile, String apiToken) {
        String jsonIn = getDatasetJson(pathToJsonFile);

        Response addLicenseResponse = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(jsonIn)
                .contentType("application/json")
                .post("/api/licenses");
        return addLicenseResponse;
    }

    static Response getLicenses() {

        Response getLicensesResponse = given()
                .get("/api/licenses");
        return getLicensesResponse;
    }

    static Response getLicenseById(Long id) {

        Response getLicenseResponse = given()
                .get("/api/licenses/"+id.toString());
        return getLicenseResponse;
    }

    static Response deleteLicenseById(Long id, String apiToken) {

        Response deleteLicenseResponse = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("/api/licenses/"+id.toString());
        return deleteLicenseResponse;
    }

    static Response setDefaultLicenseById(Long id, String apiToken) {
        Response defaultLicenseResponse = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .put("/api/licenses/default/"+id.toString());
        return defaultLicenseResponse;
    }
    
    static Response setLicenseActiveById(Long id, boolean state, String apiToken) {
        Response activateLicenseResponse = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .urlEncodingEnabled(false)
                .put("/api/licenses/"+id.toString() + "/:active/" + state);
        return activateLicenseResponse;
    }
    
    static Response setLicenseSortOrderById(Long id, Long sortOrder, String apiToken) {
        Response setSortOrderLicenseResponse = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .urlEncodingEnabled(false)
                .put("/api/licenses/"+id.toString() + "/:sortOrder/" + sortOrder);
        return setSortOrderLicenseResponse;
    }

    static Response updateDatasetJsonLDMetadata(Integer datasetId, String apiToken, String jsonLDBody, boolean replace) {
        Response response = given()
            .header(API_TOKEN_HTTP_HEADER, apiToken)
            .contentType("application/ld+json")
            .body(jsonLDBody.getBytes(StandardCharsets.UTF_8))
            .put("/api/datasets/" + datasetId + "/metadata?replace=" + replace);
        return response;
    }

    static Response deleteDatasetJsonLDMetadata(Integer datasetId, String apiToken, String jsonLDBody) {
        Response response = given()
            .header(API_TOKEN_HTTP_HEADER, apiToken)
            .contentType("application/ld+json")
            .body(jsonLDBody.getBytes(StandardCharsets.UTF_8))
            .put("/api/datasets/" + datasetId + "/metadata/delete");
        return response;
    }

    public static Response recreateDatasetJsonLD(String apiToken, String dataverseAlias, String jsonLDBody) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .contentType("application/ld+json; charset=utf-8")
                .body(jsonLDBody.getBytes(StandardCharsets.UTF_8))
                .post("/api/dataverses/" + dataverseAlias +"/datasets");
        return response;
    }
    
    static Response setDataverseCurationLabelSet(String alias, String apiToken, String labelSetName) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .put("/api/admin/dataverse/" + alias + "/curationLabelSet?name=" + labelSetName);
        return response;
    }
    
    static Response getDatasetCurationLabelSet(Integer datasetId, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/datasets/" + datasetId + "/curationLabelSet");
        return response;
    }
    
    static Response setDatasetCurationLabel(Integer datasetId, String apiToken, String label) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .put("/api/datasets/" + datasetId + "/curationStatus?label=" + label);
        return response;
    }
    
    static Response getDatasetVersionArchivalStatus(Integer datasetId, String version, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/datasets/" + datasetId + "/" + version + "/archivalStatus");
        return response;
    }

    static Response archiveDataset(String idOrPersistentIdOfDataset, String version, String apiToken) {
        String idInPath = idOrPersistentIdOfDataset;
        String optionalQueryParam = "";
        if (!NumberUtils.isCreatable(idOrPersistentIdOfDataset)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + idOrPersistentIdOfDataset;
        }
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .post("/api/admin/submitDatasetVersionToArchive/" + idInPath + "/" + version + optionalQueryParam);
        return response;
    }

    static Response setDatasetVersionArchivalStatus(Integer datasetId, String version, String apiToken, String status, String message) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken).contentType("application/json; charset=utf-8").body("{\"status\":\"" + status + "\", \"message\":\"" + message + "\"}")
                .put("/api/datasets/" + datasetId + "/" + version + "/archivalStatus");
        return response;
    }
    static Response deleteDatasetVersionArchivalStatus(Integer datasetId, String version, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("/api/datasets/" + datasetId + "/" + version + "/archivalStatus");
        return response;
    }

    private static DatasetField constructPrimitive(String fieldName, String value) {
        DatasetField field = new DatasetField();
        field.setDatasetFieldType(
                new DatasetFieldType(fieldName, DatasetFieldType.FieldType.TEXT, false));
        field.setDatasetFieldValues(
                Collections.singletonList(
                        new DatasetFieldValue(field, value)));
        return field;
    }

    static Response importDatasetNativeJson(String apiToken, String dataverseAlias, String jsonString, String pid, String release) {

        String postString = "/api/dataverses/" + dataverseAlias + "/datasets/:import";
        if (pid != null || release != null) {
            //postString = postString + "?";
            if (pid != null) {
                postString = postString + "?pid=" + pid;
                if (release != null && release.compareTo("yes") == 0) {
                    postString = postString + "&release=" + release.toString();
                }
            } else {
                if (release != null && release.compareTo("yes") == 0) {
                    postString = postString + "?release=" + release.toString();
                }
            }
        }

        RequestSpecification importJson = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .urlEncodingEnabled(false)
                .body(jsonString)
                .contentType("application/json");

        return importJson.post(postString);
    }

    static Response importDatasetDDIViaNativeApi(String apiToken, String dataverseAlias, String xml, String pid, String release) {

        String postString = "/api/dataverses/" + dataverseAlias + "/datasets/:importddi";
        if (pid != null || release != null  ) {
            //postString = postString + "?";
            if (pid != null) {
                postString = postString + "?pid=" + pid;
                if (release != null && release.compareTo("yes") == 0) {
                    postString = postString + "&release=" + release.toString();
                }
            } else {
                if (release != null && release.compareTo("yes") == 0) {
                    postString = postString + "?release=" + release.toString();
                }
            }
        }
        logger.info("Here importDatasetDDIViaNativeApi");
        logger.info(postString);

        RequestSpecification importDDI = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .urlEncodingEnabled(false)
                .body(xml)
                .contentType("application/xml");


        return importDDI.post(postString);
    }


    static Response importDatasetViaNativeApi(String apiToken, String dataverseAlias, String json, String pid, String release) {
        String postString = "/api/dataverses/" + dataverseAlias + "/datasets/:import";
        if (pid != null || release != null  ) {
            //postString = postString + "?";
            if (pid != null) {
                postString = postString + "?pid=" + pid;
                if (release != null && release.compareTo("yes") == 0) {
                    postString = postString + "&release=" + release;
                }
            } else {
                if (release != null && release.compareTo("yes") == 0) {
                    postString = postString + "?release=" + release;
                }
            }
        }
        logger.info("Here importDatasetViaNativeApi");
        logger.info(postString);

        RequestSpecification importJSON = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .urlEncodingEnabled(false)
                .body(json)
                .contentType("application/json");

        return importJSON.post(postString);
    }


    static Response retrieveMyDataAsJsonString(String apiToken, String userIdentifier, ArrayList<Long> roleIds) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .contentType("application/json; charset=utf-8")
                .queryParam("role_ids", roleIds)
                .queryParam("dvobject_types", MyDataFilterParams.defaultDvObjectTypes)
                .queryParam("published_states", MyDataFilterParams.defaultPublishedStates)
                .get("/api/mydata/retrieve?userIdentifier=" + userIdentifier);
        return response;
    }

    static Response createSignedUrl(String apiToken, String apiPath, String username) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(String.format("{\"url\":\"%s\",\"timeOut\":35,\"user\":\"%s\"}", getRestAssuredBaseUri() + apiPath, username))
                .contentType("application/json")
                .post("/api/admin/requestSignedUrl");
        return response;
    }

    static String getSignedUrlFromResponse(Response createSignedUrlResponse) {
        JsonPath jsonPath = JsonPath.from(createSignedUrlResponse.body().asString());
        String signedUrl = jsonPath.getString("data.signedUrl");
        return signedUrl;
    }

    static Response logout() {
        Response response = given()
                .contentType("application/json")
                .post("/api/logout");
        return response;
    }

    static Response getDatasetSummaryFieldNames() {
        Response response = given()
                .contentType("application/json")
                .get("/api/datasets/summaryFieldNames");
        return response;
    }

    static Response getPrivateUrlDatasetVersion(String privateUrlToken) {
        Response response = given()
                .contentType("application/json")
                .get("/api/datasets/privateUrlDatasetVersion/" + privateUrlToken);
        return response;
    }

    static Response getPrivateUrlDatasetVersionCitation(String privateUrlToken) {
        Response response = given()
                .contentType("application/json")
                .get("/api/datasets/privateUrlDatasetVersion/" + privateUrlToken + "/citation");
        return response;
    }

    static Response getDatasetVersionCitation(Integer datasetId, String version, boolean includeDeaccessioned, String apiToken) {
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .contentType("application/json")
                .queryParam("includeDeaccessioned", includeDeaccessioned)
                .get("/api/datasets/" + datasetId + "/versions/" + version + "/citation");
        return response;
    }

    static Response setDatasetCitationDateField(String datasetIdOrPersistentId, String dateField, String apiToken) {
        String idInPath = datasetIdOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(datasetIdOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + datasetIdOrPersistentId;
        }
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(dateField)
                .put("/api/datasets/" + idInPath + "/citationdate" + optionalQueryParam);
        return response;
    }

    static Response clearDatasetCitationDateField(String datasetIdOrPersistentId, String apiToken) {
        String idInPath = datasetIdOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(datasetIdOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + datasetIdOrPersistentId;
        }
        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("/api/datasets/" + idInPath + "/citationdate" + optionalQueryParam);
        return response;
    }

    static Response getFileCitation(Integer fileId, String datasetVersion, String apiToken) {
        Boolean includeDeaccessioned = null;
        return getFileCitation(fileId, datasetVersion, includeDeaccessioned, apiToken);
    }

    static Response getFileCitation(Integer fileId, String datasetVersion, Boolean includeDeaccessioned, String apiToken) {
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification.header(API_TOKEN_HTTP_HEADER, apiToken);
        }
        if (includeDeaccessioned != null) {
            requestSpecification.queryParam("includeDeaccessioned", includeDeaccessioned);
        }
        return requestSpecification.get("/api/files/" + fileId + "/versions/" + datasetVersion + "/citation");
    }

    static Response getVersionFiles(Integer datasetId,
                                    String version,
                                    Integer limit,
                                    Integer offset,
                                    String contentType,
                                    String accessStatus,
                                    String categoryName,
                                    String tabularTagName,
                                    String searchText,
                                    String orderCriteria,
                                    boolean includeDeaccessioned,
                                    String apiToken) {
        RequestSpecification requestSpecification = given()
                .contentType("application/json")
                .queryParam("includeDeaccessioned", includeDeaccessioned);
        if (apiToken != null) {
            requestSpecification.header(API_TOKEN_HTTP_HEADER, apiToken);
        }
        if (limit != null) {
            requestSpecification = requestSpecification.queryParam("limit", limit);
        }
        if (offset != null) {
            requestSpecification = requestSpecification.queryParam("offset", offset);
        }
        if (contentType != null) {
            requestSpecification = requestSpecification.queryParam("contentType", contentType);
        }
        if (accessStatus != null) {
            requestSpecification = requestSpecification.queryParam("accessStatus", accessStatus);
        }
        if (categoryName != null) {
            requestSpecification = requestSpecification.queryParam("categoryName", categoryName);
        }
        if (tabularTagName != null) {
            requestSpecification = requestSpecification.queryParam("tabularTagName", tabularTagName);
        }
        if (searchText != null) {
            requestSpecification = requestSpecification.queryParam("searchText", searchText);
        }
        if (orderCriteria != null) {
            requestSpecification = requestSpecification.queryParam("orderCriteria", orderCriteria);
        }
        return requestSpecification.get("/api/datasets/" + datasetId + "/versions/" + version + "/files");
    }

    static Response createAndUploadTestFile(String persistentId, String testFileName, byte[] testFileContentInBytes, String apiToken) throws IOException {
        Path pathToTempDir = Paths.get(Files.createTempDirectory(null).toString());
        String pathToTestFile = pathToTempDir + File.separator + testFileName;
        File testFile = new File(pathToTestFile);
        FileOutputStream fileOutputStream = new FileOutputStream(testFile);

        fileOutputStream.write(testFileContentInBytes);
        fileOutputStream.flush();
        fileOutputStream.close();

        return uploadZipFileViaSword(persistentId, pathToTestFile, apiToken);
    }

    static Response getFileDownloadCount(String dataFileId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/files/" + dataFileId + "/downloadCount");
    }

    static Response getFileDataTables(String dataFileId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/files/" + dataFileId + "/dataTables");
    }

    static Response getUserFileAccessRequested(String dataFileId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/access/datafile/" + dataFileId + "/userFileAccessRequested");
    }

    static Response getUserPermissionsOnFile(String dataFileId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/access/datafile/" + dataFileId + "/userPermissions");
    }

    static Response getUserPermissionsOnDataset(String datasetId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/datasets/" + datasetId + "/userPermissions");
    }

    static Response getUserPermissionsOnDataverse(String dataverseAlias, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/dataverses/" + dataverseAlias + "/userPermissions");
    }

    static Response getCanDownloadAtLeastOneFile(String datasetId, String versionId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/datasets/" + datasetId + "/versions/" + versionId + "/canDownloadAtLeastOneFile");
    }

    static Response createFileEmbargo(Integer datasetId, Integer fileId, String dateAvailable, String apiToken) {
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
        jsonBuilder.add("dateAvailable", dateAvailable);
        jsonBuilder.add("reason", "This is a test embargo");
        jsonBuilder.add("fileIds", Json.createArrayBuilder().add(fileId));
        String jsonString = jsonBuilder.build().toString();
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(jsonString)
                .contentType("application/json")
                .urlEncodingEnabled(false)
                .post("/api/datasets/" + datasetId + "/files/actions/:set-embargo");
    }

    static Response createFileRetention(Integer datasetId, Integer fileId, String dateUnavailable, String apiToken) {
        JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
        jsonBuilder.add("dateUnavailable", dateUnavailable);
        jsonBuilder.add("reason", "This is a test retention");
        jsonBuilder.add("fileIds", Json.createArrayBuilder().add(fileId));
        String jsonString = jsonBuilder.build().toString();
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(jsonString)
                .contentType("application/json")
                .urlEncodingEnabled(false)
                .post("/api/datasets/" + datasetId + "/files/actions/:set-retention");
    }

    static Response getVersionFileCounts(Integer datasetId,
                                         String version,
                                         String contentType,
                                         String accessStatus,
                                         String categoryName,
                                         String tabularTagName,
                                         String searchText,
                                         boolean includeDeaccessioned,
                                         String apiToken) {
        RequestSpecification requestSpecification = given()
                .queryParam("includeDeaccessioned", includeDeaccessioned);
        if (apiToken != null) {
            requestSpecification.header(API_TOKEN_HTTP_HEADER, apiToken);
        }
        if (contentType != null) {
            requestSpecification = requestSpecification.queryParam("contentType", contentType);
        }
        if (accessStatus != null) {
            requestSpecification = requestSpecification.queryParam("accessStatus", accessStatus);
        }
        if (categoryName != null) {
            requestSpecification = requestSpecification.queryParam("categoryName", categoryName);
        }
        if (tabularTagName != null) {
            requestSpecification = requestSpecification.queryParam("tabularTagName", tabularTagName);
        }
        if (searchText != null) {
            requestSpecification = requestSpecification.queryParam("searchText", searchText);
        }
        return requestSpecification.get("/api/datasets/" + datasetId + "/versions/" + version + "/files/counts");
    }

    static Response setFileCategories(String dataFileId, String apiToken, List<String> categories) {
        return setFileCategories(dataFileId, apiToken, categories, null);
    }
    static Response setFileCategories(String dataFileId, String apiToken, List<String> categories, Boolean replaceData) {
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        for (String category : categories) {
            jsonArrayBuilder.add(category);
        }
        String replace = replaceData != null ? "?replace=" + replaceData : "";
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add("categories", jsonArrayBuilder);
        String jsonString = jsonObjectBuilder.build().toString();
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(jsonString)
                .post("/api/files/" + dataFileId + "/metadata/categories" + replace);
    }

    static Response setFileTabularTags(String dataFileId, String apiToken, List<String> tabularTags) {
        return setFileTabularTags(dataFileId, apiToken, tabularTags, null);
    }
    static Response setFileTabularTags(String dataFileId, String apiToken, List<String> tabularTags, Boolean replaceData) {
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        for (String tabularTag : tabularTags) {
            jsonArrayBuilder.add(tabularTag);
        }
        String replace = replaceData != null ? "?replace=" + replaceData : "";
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add("tabularTags", jsonArrayBuilder);
        String jsonString = jsonObjectBuilder.build().toString();
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(jsonString)
                .post("/api/files/" + dataFileId + "/metadata/tabularTags" + replace);
    }

    static Response deleteFileInDataset(Integer fileId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("/api/files/" + fileId);
    }

    static Response getHasBeenDeleted(String dataFileId, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/files/" + dataFileId + "/hasBeenDeleted");
    }

    static Response deaccessionDataset(int datasetId, String version, String deaccessionReason, String deaccessionForwardURL, String apiToken) {
        return deaccessionDataset(String.valueOf(datasetId), version, deaccessionReason, deaccessionForwardURL, apiToken);
    }

    static Response deaccessionDataset(String datasetIdOrPersistentId, String versionId, String deaccessionReason, String deaccessionForwardURL, String apiToken) {
        
        String idInPath = datasetIdOrPersistentId; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(datasetIdOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + datasetIdOrPersistentId;
        }
    
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add("deaccessionReason", deaccessionReason);
        if (deaccessionForwardURL != null) {
            jsonObjectBuilder.add("deaccessionForwardURL", deaccessionForwardURL);
        }

        String jsonString = jsonObjectBuilder.build().toString();
        StringBuilder query = new StringBuilder()
            .append("/api/datasets/")
            .append(idInPath)
            .append("/versions/")
            .append(versionId)
            .append("/deaccession")
            .append(optionalQueryParam);   
                 
        return given()
            .header(API_TOKEN_HTTP_HEADER, apiToken)
            .body(jsonString)
            .post(query.toString());
    }

    static Response getDownloadSize(Integer datasetId,
                                    String version,
                                    String contentType,
                                    String accessStatus,
                                    String categoryName,
                                    String tabularTagName,
                                    String searchText,
                                    String mode,
                                    boolean includeDeaccessioned,
                                    String apiToken) {
        RequestSpecification requestSpecification = given()
                .queryParam("includeDeaccessioned", includeDeaccessioned)
                .queryParam("mode", mode);
        if (apiToken != null) {
            requestSpecification.header(API_TOKEN_HTTP_HEADER, apiToken);
        }
        if (contentType != null) {
            requestSpecification = requestSpecification.queryParam("contentType", contentType);
        }
        if (accessStatus != null) {
            requestSpecification = requestSpecification.queryParam("accessStatus", accessStatus);
        }
        if (categoryName != null) {
            requestSpecification = requestSpecification.queryParam("categoryName", categoryName);
        }
        if (tabularTagName != null) {
            requestSpecification = requestSpecification.queryParam("tabularTagName", tabularTagName);
        }
        if (searchText != null) {
            requestSpecification = requestSpecification.queryParam("searchText", searchText);
        }
        return requestSpecification
                .get("/api/datasets/" + datasetId + "/versions/" + version + "/downloadsize");
    }

    static Response getDownloadCountByDatasetId(Integer datasetId, String apiToken, Boolean includeMDC) {
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification.header(API_TOKEN_HTTP_HEADER, apiToken);
        }
        if (includeMDC != null) {
            requestSpecification = requestSpecification.queryParam("includeMDC", includeMDC);
        }
        return requestSpecification
                .get("/api/datasets/" + datasetId + "/download/count");
    }

    static Response downloadTmpFile(String fullyQualifiedPathToFile, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/admin/downloadTmpFile?fullyQualifiedPathToFile=" + fullyQualifiedPathToFile);
    }

    static Response setDatasetStorageDriver(Integer datasetId, String driverLabel, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(driverLabel)
                .put("/api/datasets/" + datasetId + "/storageDriver");
    }

    /** GET on /api/admin/savedsearches/list */
    static Response getSavedSearchList() {
        return given().get("/api/admin/savedsearches/list");
    }

    /** POST on /api/admin/savedsearches without body */
    static Response setSavedSearch() {
        return given()
                .contentType("application/json")
                .post("/api/admin/savedsearches");
    }

    /** POST on /api/admin/savedsearches with body */
    static Response setSavedSearch(String body) {
        return given()
                .body(body)
                .contentType("application/json")
                .post("/api/admin/savedsearches");
    }

    /** PUT on /api/admin/savedsearches/makelinks/all */
    static Response setSavedSearchMakelinksAll() {
        return given().put("/api/admin/savedsearches/makelinks/all");
    }

    /** DELETE on /api/admin/savedsearches/{id} with identifier */
    static Response deleteSavedSearchById(Integer id) {
        return given().delete("/api/admin/savedsearches/" + id);
    }

    //Globus Store related - not currently used
    
    static Response getDatasetGlobusUploadParameters(Integer datasetId, String locale, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .contentType("application/json")
                .get("/api/datasets/" + datasetId + "/globusUploadParameters?locale=" + locale);
    }
    
    static Response getDatasetGlobusDownloadParameters(Integer datasetId, String locale, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .contentType("application/json")
                .get("/api/datasets/" + datasetId + "/globusDownloadParameters?locale=" + locale);
    }
    
    static Response requestGlobusDownload(Integer datasetId, JsonObject body, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(body)
                .contentType("application/json")
                .post("/api/datasets/" + datasetId + "/requestGlobusDownload");
    }

    static Response requestGlobusUploadPaths(Integer datasetId, JsonObject body, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(body.toString())
                .contentType("application/json")
                .post("/api/datasets/" + datasetId + "/requestGlobusUploadPaths");
    }

    public static Response updateDataverseInputLevels(String dataverseAlias, String[] inputLevelNames, boolean[] requiredInputLevels, boolean[] includedInputLevels, String apiToken) {
        JsonArrayBuilder inputLevelsArrayBuilder = Json.createArrayBuilder();
        for (int i = 0; i < inputLevelNames.length; i++) {
            inputLevelsArrayBuilder.add(createInputLevelObject(inputLevelNames[i], requiredInputLevels[i], includedInputLevels[i]));
        }
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(inputLevelsArrayBuilder.build().toString())
                .contentType(ContentType.JSON)
                .put("/api/dataverses/" + dataverseAlias + "/inputLevels");
    }

    private static JsonObjectBuilder createInputLevelObject(String name, boolean required, boolean include) {
        return Json.createObjectBuilder()
                .add("datasetFieldTypeName", name)
                .add("required", required)
                .add("include", include);
    }

    public static Response getOpenAPI(String accept, String format) {
        Response response = given()
                .header("Accept", accept)
                .queryParam("format", format)
                .get("/openapi");
        return response;
    }

    static Response listDataverseFacets(String dataverseAlias, String apiToken) {
        return listDataverseFacets(dataverseAlias, false, apiToken);
    }

    static Response listDataverseFacets(String dataverseAlias, boolean returnDetails, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .queryParam("returnDetails", returnDetails)
                .get("/api/dataverses/" + dataverseAlias + "/facets");
    }

    static Response listAllFacetableDatasetFields() {
        return given()
                .get("/api/datasetfields/facetables");
    }

    static Response listDataverseInputLevels(String dataverseAlias, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .contentType("application/json")
                .get("/api/dataverses/" + dataverseAlias + "/inputLevels");
    }

    public static Response getDatasetTypes() {
        Response response = given()
                .get("/api/datasets/datasetTypes");
        return response;
    }

    static Response getDatasetType(String idOrName) {
        return given()
                .get("/api/datasets/datasetTypes/" + idOrName);
    }

    static Response addDatasetType(String jsonIn, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(jsonIn)
                .contentType(ContentType.JSON)
                .post("/api/datasets/datasetTypes");
    }
    
    static Response addDatasetType(JsonObject jsonObject, String apiToken) {
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification.header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification
                .body(jsonObject.toString())
                .contentType(ContentType.JSON)
                .post("/api/datasets/datasetTypes");
    }

    static Response deleteDatasetTypes(long doomed, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("/api/datasets/datasetTypes/" + doomed);
    }

    static Response updateDatasetTypeLinksWithMetadataBlocks(String idOrName, String jsonArrayOfMetadataBlocks, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(jsonArrayOfMetadataBlocks)
                .put("/api/datasets/datasetTypes/" + idOrName);
    }
    
    static Response updateDatasetTypeAvailableLicense(String idOrName, String jsonArrayOfLicenses, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(jsonArrayOfLicenses)
                .put("/api/datasets/datasetTypes/" + idOrName + "/licenses");
    }

    static Response registerOidcUser(String jsonIn, String bearerToken) {
        return given()
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .body(jsonIn)
                .contentType(ContentType.JSON)
                .post("/api/users/register");
    }

    /**
     * Creates a new user in the development Keycloak instance.
     * <p>This method is specifically designed for use in the containerized Keycloak development
     * environment. The configured Keycloak instance must be accessible at the specified URL.
     * The method sends a request to the Keycloak Admin API to create a new user in the given realm.
     *
     * <p>Refer to the {@code testRegisterOidc()} method in the {@code UsersIT} class for an example
     * of this method in action.
     *
     * @param bearerToken The Bearer token used for authenticating the request to the Keycloak Admin API.
     * @param userJson    The JSON representation of the user to be created.
     * @return A {@link Response} containing the result of the user creation request.
     */
    static Response createKeycloakUser(String bearerToken, String userJson) {
        return given()
                .contentType(ContentType.JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .body(userJson)
                .post("http://keycloak.mydomain.com:8090/admin/realms/test/users");
    }

    /**
     * Performs an OIDC login in the development Keycloak instance using the Resource Owner Password Credentials (ROPC)
     * grant type to retrieve authentication tokens from a Keycloak instance.
     *
     * <p>This method is specifically designed for use in the containerized Keycloak development
     * environment. The configured Keycloak instance must be accessible at the specified URL.
     *
     * <p>Refer to the {@code testRegisterOidc()} method in the {@code UsersIT} class for an example
     * of this method in action.
     *
     * @return A {@link Response} containing authentication tokens, including access and refresh tokens,
     *         if the login is successful.
     */
    static Response performKeycloakROPCLogin(String username, String password) {
        return given()
                .contentType(ContentType.URLENC)
                .formParam("client_id", "test")
                .formParam("client_secret", "94XHrfNRwXsjqTqApRrwWmhDLDHpIYV8")
                .formParam("username", username)
                .formParam("password", password)
                .formParam("grant_type", "password")
                .formParam("scope", "openid")
                .post("http://keycloak.mydomain.com:8090/realms/test/protocol/openid-connect/token");
    }

    static Response createDataverseFeaturedItem(String dataverseAlias,
                                                String apiToken,
                                                String content,
                                                int displayOrder,
                                                String pathToFile) {
        RequestSpecification requestSpecification = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .contentType(ContentType.MULTIPART)
                .multiPart("content", content)
                .multiPart("displayOrder", displayOrder);

        if (pathToFile != null) {
            requestSpecification.multiPart("file", new File(pathToFile));
        }

        return requestSpecification
                .when()
                .post("/api/dataverses/" + dataverseAlias + "/featuredItems");
    }

    static Response deleteDataverseFeaturedItem(long id, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("/api/dataverseFeaturedItems/" + id);
    }

    static Response updateDataverseFeaturedItem(long featuredItemId,
                                                String content,
                                                int displayOrder,
                                                boolean keepFile,
                                                String pathToFile,
                                                String apiToken) {
        RequestSpecification requestSpecification = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .contentType(ContentType.MULTIPART)
                .multiPart("content", content)
                .multiPart("displayOrder", displayOrder)
                .multiPart("keepFile", keepFile);

        if (pathToFile != null) {
            requestSpecification.multiPart("file", new File(pathToFile));
        }

        return requestSpecification
                .when()
                .put("/api/dataverseFeaturedItems/" + featuredItemId);
    }

    static Response listDataverseFeaturedItems(String dataverseAlias, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .contentType("application/json")
                .get("/api/dataverses/" + dataverseAlias + "/featuredItems");
    }

    static Response updateDataverseFeaturedItems(
            String dataverseAlias,
            List<Long> ids,
            List<String> contents,
            List<Integer> orders,
            List<Boolean> keepFiles,
            List<String> pathsToFiles,
            String apiToken) {

        RequestSpecification requestSpec = given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .contentType(ContentType.MULTIPART);

        for (int i = 0; i < contents.size(); i++) {
            requestSpec.multiPart("content", contents.get(i))
                    .multiPart("displayOrder", orders.get(i))
                    .multiPart("keepFile", keepFiles.get(i))
                    .multiPart("id", ids.get(i));

            String pathToFile = pathsToFiles != null ? pathsToFiles.get(i) : null;
            if (pathToFile != null && !pathToFile.isEmpty()) {
                requestSpec.multiPart("fileName", Paths.get(pathToFile).getFileName().toString())
                        .multiPart("file", new File(pathToFile));
            } else {
                requestSpec.multiPart("fileName", "");
            }
        }

        return requestSpec.when().put("/api/dataverses/" + dataverseAlias + "/featuredItems");
    }

    static Response deleteDataverseFeaturedItems(String dataverseAlias, String apiToken) {
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .delete("/api/dataverses/" + dataverseAlias + "/featuredItems");
    }

    public static Response deleteDatasetFiles(String datasetId, JsonArray fileIds, String apiToken) {
        String path = String.format("/api/datasets/%s/deleteFiles", datasetId);
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .contentType(ContentType.JSON)
                .body(fileIds.toString())
                .put(path);
    }
  
    public static Response updateDataverseInputLevelDisplayOnCreate(String dataverseAlias, String fieldTypeName, Boolean displayOnCreate, String apiToken) {
        JsonArrayBuilder inputLevelsArrayBuilder = Json.createArrayBuilder();
        JsonObjectBuilder inputLevel = Json.createObjectBuilder()
                .add("datasetFieldTypeName", fieldTypeName)
                .add("required", false)
                .add("include", true);
        if(displayOnCreate != null) {
            inputLevel.add("displayOnCreate", displayOnCreate);
        }
        
        inputLevelsArrayBuilder.add(inputLevel);
        
        return given()
                .header(API_TOKEN_HTTP_HEADER, apiToken)
                .body(inputLevelsArrayBuilder.build().toString())
                .contentType(ContentType.JSON)
                .put("/api/dataverses/" + dataverseAlias + "/inputLevels");
     }
    
    public static Response updateDatasetFilesMetadata(String datasetIdOrPersistentId, JsonArray jsonArray,
            String apiToken) {
        String idInPath = datasetIdOrPersistentId; // Assume it's a number to start.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(datasetIdOrPersistentId)) {
            idInPath = ":persistentId";
            optionalQueryParam = "?persistentId=" + datasetIdOrPersistentId;
        }

        return given().header(API_TOKEN_HTTP_HEADER, apiToken).contentType(ContentType.JSON).body(jsonArray.toString())
                .post("/api/datasets/" + idInPath + "/files/metadata" + optionalQueryParam);
    }
}
