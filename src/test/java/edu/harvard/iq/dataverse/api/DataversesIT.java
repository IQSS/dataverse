package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import io.restassured.RestAssured;

import static io.restassured.RestAssured.given;
import static io.restassured.path.json.JsonPath.with;

import io.restassured.response.Response;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static jakarta.ws.rs.core.Response.Status.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.hasKey;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;

import io.restassured.path.json.JsonPath;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;

public class DataversesIT {

    private static final Logger logger = Logger.getLogger(DataversesIT.class.getCanonicalName());

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @AfterAll
    public static void afterClass() {
        Response removeExcludeEmail = UtilIT.deleteSetting(SettingsServiceBean.Key.ExcludeEmailFromExport);
    }

    @Test
    public void testAttemptToCreateDuplicateAlias() throws Exception {

        Response createUser = UtilIT.createRandomUser();
//        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverse1Response = UtilIT.createRandomDataverse(apiToken);
        if (createDataverse1Response.getStatusCode() != 201) {
            // purposefully using println here to the error shows under "Test Results" in Netbeans
            System.out.println("A workspace for testing (a dataverse) couldn't be created in the root dataverse. The output was:\n\n" + createDataverse1Response.body().asString());
            System.out.println("\nPlease ensure that users can created dataverses in the root in order for this test to run.");
        } else {
            createDataverse1Response.prettyPrint();
        }
        assertEquals(201, createDataverse1Response.getStatusCode());

        String dataverseAlias1 = UtilIT.getAliasFromResponse(createDataverse1Response);
        String dataverseAlias2 = dataverseAlias1.toUpperCase();
        logger.info("Attempting to creating dataverse with alias '" + dataverseAlias2 + "' (uppercase version of existing '" + dataverseAlias1 + "' dataverse, should fail)...");
        String category = null;
        Response attemptToCreateDataverseWithDuplicateAlias = UtilIT.createDataverse(dataverseAlias2, category, apiToken);
        attemptToCreateDataverseWithDuplicateAlias.prettyPrint();
        assertEquals(403, attemptToCreateDataverseWithDuplicateAlias.getStatusCode());

        logger.info("Deleting dataverse " + dataverseAlias1);
        Response deleteDataverse1Response = UtilIT.deleteDataverse(dataverseAlias1, apiToken);
        deleteDataverse1Response.prettyPrint();
        assertEquals(200, deleteDataverse1Response.getStatusCode());

        logger.info("Checking response code for attempting to delete a non-existent dataverse.");
        Response attemptToDeleteDataverseThatShouldNotHaveBeenCreated = UtilIT.deleteDataverse(dataverseAlias2, apiToken);
        attemptToDeleteDataverseThatShouldNotHaveBeenCreated.prettyPrint();
        assertEquals(404, attemptToDeleteDataverseThatShouldNotHaveBeenCreated.getStatusCode());

    }

    @Test
    public void testDataverseCategory() {
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseWithoutCategory = UtilIT.createRandomDataverse(apiToken);
        createDataverseWithoutCategory.prettyPrint();
        createDataverseWithoutCategory.then().assertThat()
                .body("data.dataverseType", equalTo("UNCATEGORIZED"))
                .statusCode(Status.CREATED.getStatusCode());

        String alias1 = UtilIT.getRandomDvAlias();
        String category1 = Dataverse.DataverseType.DEPARTMENT.toString();
        Response createDataverseWithCategory = UtilIT.createDataverse(alias1, category1, apiToken);
        createDataverseWithCategory.prettyPrint();
        createDataverseWithCategory.then().assertThat()
                .body("data.dataverseType", equalTo("DEPARTMENT"))
                .statusCode(Status.CREATED.getStatusCode());

        String alias2 = UtilIT.getRandomDvAlias();
        String madeUpCategory = "madeUpCategory";
        Response createDataverseWithInvalidCategory = UtilIT.createDataverse(alias2, madeUpCategory, apiToken);
        createDataverseWithInvalidCategory.prettyPrint();
        createDataverseWithInvalidCategory.then().assertThat()
                .body("data.dataverseType", equalTo("UNCATEGORIZED"))
                .statusCode(Status.CREATED.getStatusCode());

        String alias3 = UtilIT.getRandomDvAlias();
        String category3 = Dataverse.DataverseType.LABORATORY.toString().toLowerCase();
        Response createDataverseWithLowerCaseCategory = UtilIT.createDataverse(alias3, category3, apiToken);
        createDataverseWithLowerCaseCategory.prettyPrint();
        createDataverseWithLowerCaseCategory.then().assertThat()
                .body("data.dataverseType", equalTo("UNCATEGORIZED"))
                .statusCode(Status.CREATED.getStatusCode());

    }

    @Test
    public void testMinimalDataverse() throws FileNotFoundException {
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        JsonObject dvJson;
        FileReader reader = new FileReader("doc/sphinx-guides/source/_static/api/dataverse-minimal.json");
        dvJson = Json.createReader(reader).readObject();
        Response create = UtilIT.createDataverse(dvJson, apiToken);
        create.prettyPrint();
        create.then().assertThat()
                .body("data.isMetadataBlockRoot", equalTo(false))
                .body("data.isFacetRoot", equalTo(false))
                .statusCode(CREATED.getStatusCode());
        Response deleteDataverse = UtilIT.deleteDataverse("science", apiToken);
        deleteDataverse.prettyPrint();
        deleteDataverse.then().assertThat().statusCode(OK.getStatusCode());
    }

    @Test
    public void testGetDataverseOwners() {
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        Response createDataverse1Response = UtilIT.createRandomDataverse(apiToken);
        
        createDataverse1Response.prettyPrint();
        createDataverse1Response.then().assertThat().statusCode(CREATED.getStatusCode());
        
        String first = UtilIT.getAliasFromResponse(createDataverse1Response);
        
        Response getWithOwnersFirst = UtilIT.getDataverseWithOwners(first, apiToken, true);
        getWithOwnersFirst.prettyPrint();
        
        Response createLevel1a = UtilIT.createSubDataverse(UtilIT.getRandomDvAlias() + "-level1a", null, apiToken, first);
        createLevel1a.prettyPrint();
        String level1a = UtilIT.getAliasFromResponse(createLevel1a);
        
        Response getWithOwners = UtilIT.getDataverseWithOwners(level1a, apiToken, true);
        getWithOwners.prettyPrint();
        
        getWithOwners.then().assertThat().body("data.isPartOf.identifier", equalTo(first));
    }

    @Test
    public void testGetDataverseChildCount() {
        Response createUser = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.then().assertThat().statusCode(CREATED.getStatusCode());

        Response getDataverseWithChildCount = UtilIT.getDataverseWithChildCount(dataverseAlias, apiToken, true);
        getDataverseWithChildCount.then().assertThat().body("data.childCount", equalTo(1));

        Response getDataverseWithoutChildCount = UtilIT.getDataverseWithChildCount(dataverseAlias, apiToken, false);
        getDataverseWithoutChildCount.then().assertThat().body("data.childCount", equalTo(null));
    }

    /**
     * A regular user can create a Dataverse Collection and access its
     * GuestbookResponses by DV alias or ID.
     * A request for a non-existent Dataverse's GuestbookResponses returns
     * Not Found.
     * A regular user cannot access the guestbook responses for a Dataverse
     * that they do not have permissions for, like the root Dataverse.
     */
    @Test
    public void testGetGuestbookResponses() {
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response create = UtilIT.createRandomDataverse(apiToken);
        create.prettyPrint();
        create.then().assertThat().statusCode(CREATED.getStatusCode());
        String alias = UtilIT.getAliasFromResponse(create);
        Integer dvId = UtilIT.getDataverseIdFromResponse(create);

        logger.info("Request guestbook responses for non-existent Dataverse");
        Response getResponsesByBadAlias = UtilIT.getGuestbookResponses("-1", null, apiToken);
        getResponsesByBadAlias.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        logger.info("Request guestbook responses for existent Dataverse by alias");
        Response getResponsesByAlias = UtilIT.getGuestbookResponses(alias, null, apiToken);
        getResponsesByAlias.then().assertThat().statusCode(OK.getStatusCode());

        logger.info("Request guestbook responses for existent Dataverse by ID");
        Response getResponsesById = UtilIT.getGuestbookResponses(dvId.toString(), null, apiToken);
        getResponsesById.then().assertThat().statusCode(OK.getStatusCode());

        logger.info("Request guestbook responses for root Dataverse by alias");
        getResponsesById = UtilIT.getGuestbookResponses("root", null, apiToken);
        getResponsesById.prettyPrint();
        getResponsesById.then().assertThat().statusCode(FORBIDDEN.getStatusCode());
    }

    @Test
    public void testNotEnoughJson() {
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        Response createFail = UtilIT.createDataverse(Json.createObjectBuilder().add("name", "notEnough").add("alias", "notEnough").build(), apiToken);
        createFail.prettyPrint();
        createFail.then().assertThat()
                /**
                 * @todo We really don't want Dataverse to throw a 500 error
                 * when not enough JSON is supplied to create a dataverse.
                 */
                .statusCode(INTERNAL_SERVER_ERROR.getStatusCode());
    }

    //Ensure that email is not returned when the ExcludeEmailFromExport setting is set
    @Test 
    public void testReturnEmail() throws FileNotFoundException {        
        
        Response setToExcludeEmailFromExport = UtilIT.setSetting(SettingsServiceBean.Key.ExcludeEmailFromExport, "true");
        setToExcludeEmailFromExport.then().assertThat()
            .statusCode(OK.getStatusCode());
        
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        String dataverseAlias = UtilIT.getRandomDvAlias();
        String emailAddressOfFirstDataverseContact = dataverseAlias + "@mailinator.com";
        JsonObjectBuilder jsonToCreateDataverse = Json.createObjectBuilder()
                .add("name", dataverseAlias)
                .add("alias", dataverseAlias)
                .add("dataverseContacts", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("contactEmail", emailAddressOfFirstDataverseContact)
                        )
                );
        ;

        Response createDataverseResponse = UtilIT.createDataverse(jsonToCreateDataverse.build(), apiToken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());

        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode())
                .body("data.alias", equalTo(dataverseAlias))
                .body("data.name", equalTo(dataverseAlias))
                .body("data.dataverseContacts[0].displayOrder", equalTo(0))
                .body("data.dataverseContacts[0].contactEmail", equalTo(emailAddressOfFirstDataverseContact))
                .body("data.permissionRoot", equalTo(true))
                .body("data.dataverseType", equalTo("UNCATEGORIZED"));
        
        Response exportDataverseAsJson = UtilIT.exportDataverse(dataverseAlias, apiToken);
        exportDataverseAsJson.prettyPrint();
        exportDataverseAsJson.then().assertThat()
                .statusCode(OK.getStatusCode());

        exportDataverseAsJson.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.alias", equalTo(dataverseAlias))
                .body("data.name", equalTo(dataverseAlias))
                .body("data.dataverseContacts", equalTo(null))
                .body("data.permissionRoot", equalTo(true))
                .body("data.dataverseType", equalTo("UNCATEGORIZED"));

        RestAssured.unregisterParser("text/plain");

        List dataverseEmailNotAllowed = with(exportDataverseAsJson.body().asString())
                .getJsonObject("data.dataverseContacts");
        assertNull(dataverseEmailNotAllowed);
        
        Response removeExcludeEmail = UtilIT.deleteSetting(SettingsServiceBean.Key.ExcludeEmailFromExport);
        removeExcludeEmail.then().assertThat()
                .statusCode(200);
        
        Response exportDataverseAsJson2 = UtilIT.exportDataverse(dataverseAlias, apiToken);
        exportDataverseAsJson2.prettyPrint();
        exportDataverseAsJson2.then().assertThat()
                .statusCode(OK.getStatusCode());
        exportDataverseAsJson2.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.alias", equalTo(dataverseAlias))
                .body("data.name", equalTo(dataverseAlias))
                .body("data.dataverseContacts[0].displayOrder", equalTo(0))
                .body("data.dataverseContacts[0].contactEmail", equalTo(emailAddressOfFirstDataverseContact))
                .body("data.permissionRoot", equalTo(true))
                .body("data.dataverseType", equalTo("UNCATEGORIZED"));
        
        RestAssured.unregisterParser("text/plain");
        List dataverseEmailAllowed = with(exportDataverseAsJson2.body().asString())
                .getJsonObject("data.dataverseContacts");
        assertNotNull(dataverseEmailAllowed);
        
        Response deleteDataverse2 = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteDataverse2.prettyPrint();
        deleteDataverse2.then().assertThat().statusCode(OK.getStatusCode());        
        Response deleteUserResponse = UtilIT.deleteUser(username);
        deleteUserResponse.prettyPrint();
        assertEquals(200, deleteUserResponse.getStatusCode());
    }
    
    /**
     * Test the Dataverse page error message and link 
     * when the query string has a malformed url
     */
    @Test
    public void testMalformedFacetQueryString(){
        
        Response createUser = UtilIT.createRandomUser();
        //        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverse1Response = UtilIT.createRandomDataverse(apiToken);
        if (createDataverse1Response.getStatusCode() != 201) {
            // purposefully using println here to the error shows under "Test Results" in Netbeans
            System.out.println("A workspace for testing (a dataverse) couldn't be created in the root dataverse. The output was:\n\n" + createDataverse1Response.body().asString());
            System.out.println("\nPlease ensure that users can created dataverses in the root in order for this test to run.");
        } else {
            createDataverse1Response.prettyPrint();
        }
        assertEquals(201, createDataverse1Response.getStatusCode());

        Integer dvId = UtilIT.getDataverseIdFromResponse(createDataverse1Response);
        String dvAlias = UtilIT.getAliasFromResponse(createDataverse1Response);

        Response publishDataverse = UtilIT.publishDataverseViaSword(dvAlias, apiToken);
        assertEquals(200, publishDataverse.getStatusCode());
      
        
        String expectedErrMsg = BundleUtil.getStringFromBundle("dataverse.search.facet.error", Arrays.asList("root"));

        // ----------------------------------
        // Malformed query string 1 
        //  - From 500 errs in log - No dataverse or dataverse.xhtml
        //  - expect "clear your search" url to link to root
        // ----------------------------------
        String badQuery1 = "/?q=&fq0=authorName_ss%25253A%252522U.S.+Department+of+Commerce%25252C+Bureau+of+the+Census%25252C+Geography+Division%252522&types=dataverses%25253Adatasets&sort=dateSort&order=desc";
        Response resp1 = given()
                        .get(badQuery1);
        
        String htmlStr = resp1.asString();        
        assertTrue(htmlStr.contains(expectedErrMsg));

        // ----------------------------------
        // Malformed query string 2 with Dataverse alias
        // - From https://github.com/IQSS/dataverse/issues/2605
        // - expect "clear your search" url to link to sub dataverse

        // ----------------------------------
        String badQuery2 = "/dataverse/" + dvAlias + "?fq0=authorName_ss:\"Bar,+Foo";
        Response resp2 = given()
                        .get(badQuery2);
        
        expectedErrMsg = BundleUtil.getStringFromBundle("dataverse.search.facet.error", Arrays.asList(dvAlias));

        String htmlStr2 = resp2.asString();        
        assertTrue(htmlStr2.contains(expectedErrMsg));
        
        
        // ----------------------------------
        // Malformed query string 3 with Dataverse alias
        // - expect "clear your search" url to link to sub dataverse
        // ----------------------------------
        String badQuery3 = "/dataverse/" + dvAlias + "?q=&fq0=authorName_ss%3A\"\"Finch%2C+Fiona\"&types=dataverses%3Adatasets&sort=dateSort&order=desc";
        Response resp3 = given()
                        .get(badQuery3);
        
        String htmlStr3 = resp3.asString();        

        expectedErrMsg = BundleUtil.getStringFromBundle("dataverse.search.facet.error", Arrays.asList(dvAlias));
        assertTrue(htmlStr3.contains(expectedErrMsg));

    
        // ----------------------------------
        // Malformed query string 4 with Dataverse id
        //  - expect "clear your search" url to link to root
        // ----------------------------------
        String badQuery4 = "/dataverse.xhtml?id=" + dvId + "&q=&fq0=authorName_ss%3A\"\"Finch%2C+Fiona\"&types=dataverses%3Adatasets&sort=dateSort&order=desc";
        Response resp4 = given()
                        .get(badQuery4);
        
        String htmlStr4 = resp4.asString();        
        System.out.println("------htmlStr4: " + resp4);

        // Solr searches using ?id={id} incorrectly searches the root
        //
        expectedErrMsg = BundleUtil.getStringFromBundle("dataverse.search.facet.error", Arrays.asList("root"));
        assertTrue(htmlStr4.contains(expectedErrMsg));

    }
    
    @Test
    public void testMoveDataverse() {
        Response createUser = UtilIT.createRandomUser();
        
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        
        Response superuserResponse = UtilIT.makeSuperUser(username);
        
        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        assertTrue(createDataverseResponse.prettyPrint().contains("isReleased\": false"));
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        Integer dataverseId = UtilIT.getDataverseIdFromResponse(createDataverseResponse);
        
        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);//.publishDataverseViaSword(dataverseAlias, apiToken);
        assertEquals(200, publishDataverse.getStatusCode());
        assertTrue(publishDataverse.prettyPrint().contains("isReleased\": true"));
        
        Response createDataverseResponse2 = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse2.prettyPrint();
        String dataverseAlias2 = UtilIT.getAliasFromResponse(createDataverseResponse2);
        Response publishDataverse2 = UtilIT.publishDataverseViaNativeApi(dataverseAlias2, apiToken);
        assertEquals(200, publishDataverse2.getStatusCode());
        
        Response moveResponse = UtilIT.moveDataverse(dataverseAlias, dataverseAlias2, true, apiToken);

        moveResponse.prettyPrint();
        moveResponse.then().assertThat().statusCode(OK.getStatusCode());
        
        // because indexing happens asynchronously, we'll wait first, and then retry a few times, before failing
        int numberofAttempts = 0;
        boolean checkIndex = true;
        while (checkIndex) {
            try {   
                    try {
                        Thread.sleep(6000);
                    } catch (InterruptedException ex) {
                    }                
                Response search = UtilIT.search("id:dataverse_" + dataverseId + "&subtree=" + dataverseAlias2, apiToken);
                search.prettyPrint();
                search.then().assertThat()
                        .body("data.total_count", equalTo(1))
                        .statusCode(200);
                checkIndex = false;
            } catch (AssertionError ae) {
                if (numberofAttempts++ > 5) {
                    throw ae;
                }
            }
        }

    }

    // testCreateDeleteDataverseLink was here but is now in LinkIT

    @Test
    public void testUpdateDefaultContributorRole() {
        Response createUser = UtilIT.createRandomUser();

        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response superuserResponse = UtilIT.makeSuperUser(username);

        Response createUserRando = UtilIT.createRandomUser();

        createUserRando.prettyPrint();
        String apiTokenRando = UtilIT.getApiTokenFromResponse(createUserRando);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        //Try no perms user
        Response updateDataverseDefaultRoleNoPerms = UtilIT.updateDefaultContributorsRoleOnDataverse(dataverseAlias, "curator", apiTokenRando);
        updateDataverseDefaultRoleNoPerms.prettyPrint();
        updateDataverseDefaultRoleNoPerms.then().assertThat()
                .statusCode(401);
        
        // try role with no dataset permissions alias
        Response updateDataverseDefaultRoleBadRolePermissions = UtilIT.updateDefaultContributorsRoleOnDataverse(dataverseAlias, "dvContributor", apiToken);
        updateDataverseDefaultRoleBadRolePermissions.prettyPrint();
        updateDataverseDefaultRoleBadRolePermissions.then().assertThat()
                .body("message", equalTo("Role dvContributor does not have dataset permissions."))
                .statusCode(400);

        //for test use an existing role. In practice this likely will be a custom role
        Response updateDataverseDefaultRole = UtilIT.updateDefaultContributorsRoleOnDataverse(dataverseAlias, "curator", apiToken);
        updateDataverseDefaultRole.prettyPrint();
        updateDataverseDefaultRole.then().assertThat()
                .body("data.message", equalTo("Default contributor role for Dataverse " + dataverseAlias + " has been set to Curator."))
                .statusCode(200);
        
        //for test use an existing role. In practice this likely will be a custom role
        Response updateDataverseDefaultRoleNone = UtilIT.updateDefaultContributorsRoleOnDataverse(dataverseAlias, "none", apiToken);
        updateDataverseDefaultRoleNone.prettyPrint();
        updateDataverseDefaultRoleNone.then().assertThat()
                .body("data.message", equalTo("Default contributor role for Dataverse " + dataverseAlias + " has been set to None."))
                .statusCode(200);

        // try bad role alias
        Response updateDataverseDefaultRoleBadRoleAlias = UtilIT.updateDefaultContributorsRoleOnDataverse(dataverseAlias, "colonel", apiToken);
        updateDataverseDefaultRoleBadRoleAlias.prettyPrint();
        updateDataverseDefaultRoleBadRoleAlias.then().assertThat()
                .body("message", equalTo("Role colonel not found."))
                .statusCode(404);

    }
    
    @Test
    public void testDataFileAPIPermissions() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        String pathToJsonFile = "src/test/resources/json/complete-dataset-with-files.json";
        Response createDatasetResponse = UtilIT.createDatasetViaNativeApi(dataverseAlias, pathToJsonFile, apiToken);
        
        //should fail if non-super user and attempting to
        //create a dataset with files
        createDatasetResponse.prettyPrint();
        createDatasetResponse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode());
        
        //should be ok to create a dataset without files...
        pathToJsonFile = "scripts/api/data/dataset-create-new.json";
        createDatasetResponse = UtilIT.createDatasetViaNativeApi(dataverseAlias, pathToJsonFile, apiToken);
        
        createDatasetResponse.prettyPrint();
        createDatasetResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);
        
        //As non-super user should be able to add a real file
        String pathToFile1 = "src/main/webapp/resources/images/cc0.png";
        Response authorAttemptsToAddFileViaNative = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile1, apiToken);

        authorAttemptsToAddFileViaNative.prettyPrint();
        authorAttemptsToAddFileViaNative.then().assertThat()
                .statusCode(OK.getStatusCode());
 
    }

    @Test
    public void testImportDDI() throws IOException, InterruptedException {

        Response createUser = UtilIT.createRandomUser();
        String username = UtilIT.getUsernameFromResponse(createUser);
        Response makeSuperUser = UtilIT.makeSuperUser(username);
        assertEquals(200, makeSuperUser.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        assertEquals(200, publishDataverse.getStatusCode());

        // This XML is a full DDI export without a PID.
        String xml = new String(Files.readAllBytes(Paths.get("doc/sphinx-guides/source/_static/api/ddi_dataset.xml")));

        Response importDDI = UtilIT.importDatasetDDIViaNativeApi(apiToken, dataverseAlias, xml,  null, "no");
        logger.info(importDDI.prettyPrint());
        assertEquals(201, importDDI.getStatusCode());

        // Under normal conditions, you shouldn't need to destroy these datasets.
        // Uncomment if they're still around from a previous failed run.
//        Response destroy1 = UtilIT.destroyDataset("doi:10.5072/FK2/ABCD11", apiToken);
//        destroy1.prettyPrint();
//        Response destroy2 = UtilIT.destroyDataset("doi:10.5072/FK2/ABCD22", apiToken);
//        destroy2.prettyPrint();

        Response importDDIPid = UtilIT.importDatasetDDIViaNativeApi(apiToken, dataverseAlias, xml,  "doi:10.5072/FK2/ABCD11", "no");
        logger.info(importDDIPid.prettyPrint());
        assertEquals(201, importDDIPid.getStatusCode());

        Response importDDIPidRel = UtilIT.importDatasetDDIViaNativeApi(apiToken, dataverseAlias, xml,  "doi:10.5072/FK2/ABCD22", "yes");
        logger.info(importDDIPidRel.prettyPrint());
        assertEquals(201, importDDIPidRel.getStatusCode());


        Response importDDIRelease = UtilIT.importDatasetDDIViaNativeApi(apiToken, dataverseAlias, xml, null, "yes");
        logger.info( importDDIRelease.prettyPrint());
        assertEquals(201, importDDIRelease.getStatusCode());

        Integer datasetIdInt = JsonPath.from(importDDI.body().asString()).getInt("data.id");

        Response search1 = UtilIT.search("id:dataset_" + datasetIdInt + "_draft", apiToken); // santity check, can find it
        search1.prettyPrint();
        search1.then().assertThat()
                .body("data.total_count", CoreMatchers.is(1))
                .body("data.count_in_response", CoreMatchers.is(1))
                .body("data.items[0].name", CoreMatchers.is("Replication Data for: Title"))
                .statusCode(OK.getStatusCode());

        Response search2 = UtilIT.search("id:dataset_" + datasetIdInt + "_draft", apiToken, "&geo_point=35,15&geo_radius=5"); // should find it
        search2.prettyPrint();
        search2.then().assertThat()
                .body("data.total_count", CoreMatchers.is(1))
                .body("data.count_in_response", CoreMatchers.is(1))
                .body("data.items[0].name", CoreMatchers.is("Replication Data for: Title"))
                .statusCode(OK.getStatusCode());

        Response search3 = UtilIT.search("id:dataset_" + datasetIdInt + "_draft", apiToken, "&geo_point=0,0&geo_radius=5"); // should not find it
        search3.prettyPrint();
        search3.then().assertThat()
                .body("data.total_count", CoreMatchers.is(0))
                .body("data.count_in_response", CoreMatchers.is(0))
                .body("data.items", Matchers.empty())
                .statusCode(OK.getStatusCode());

        //cleanup

        Response destroyDatasetResponse = UtilIT.destroyDataset(datasetIdInt, apiToken);
        assertEquals(200, destroyDatasetResponse.getStatusCode());

        Integer datasetIdIntPid = JsonPath.from(importDDIPid.body().asString()).getInt("data.id");
        Response destroyDatasetResponsePid = UtilIT.destroyDataset(datasetIdIntPid, apiToken);
        assertEquals(200, destroyDatasetResponsePid.getStatusCode());

        Integer datasetIdIntPidRel = JsonPath.from(importDDIPidRel.body().asString()).getInt("data.id");
        Response destroyDatasetResponsePidRel = UtilIT.destroyDataset(datasetIdIntPidRel, apiToken);
        assertEquals(200, destroyDatasetResponsePidRel.getStatusCode());
        
        UtilIT.sleepForDeadlock(UtilIT.MAXIMUM_IMPORT_DURATION);

        Integer datasetIdIntRelease = JsonPath.from(importDDIRelease.body().asString()).getInt("data.id");
        Response destroyDatasetResponseRelease = UtilIT.destroyDataset(datasetIdIntRelease, apiToken);
        assertEquals(200, destroyDatasetResponseRelease.getStatusCode());

        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        assertEquals(200, deleteDataverseResponse.getStatusCode());

        Response deleteUserResponse = UtilIT.deleteUser(username);
        assertEquals(200, deleteUserResponse.getStatusCode());
    }

    @Test
    public void testImport() throws IOException, InterruptedException {

        Response createUser = UtilIT.createRandomUser();
        String username = UtilIT.getUsernameFromResponse(createUser);
        Response makeSuperUser = UtilIT.makeSuperUser(username);
        assertEquals(200, makeSuperUser.getStatusCode());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        assertEquals(200, publishDataverse.getStatusCode());

        JsonObjectBuilder datasetJson = Json.createObjectBuilder()
                .add("datasetVersion", Json.createObjectBuilder()
                        .add("license", Json.createObjectBuilder()
                                .add("name", "CC0 1.0")
                        )
                        .add("metadataBlocks", Json.createObjectBuilder()
                                .add("citation", Json.createObjectBuilder()
                                        .add("fields", Json.createArrayBuilder()
                                                .add(Json.createObjectBuilder()
                                                        .add("typeName", "title")
                                                        .add("value", "Test Dataset")
                                                        .add("typeClass", "primitive")
                                                        .add("multiple", false)
                                                )
                                                .add(Json.createObjectBuilder()
                                                        .add("value", Json.createArrayBuilder()
                                                                .add(Json.createObjectBuilder()
                                                                        .add("authorName",
                                                                                Json.createObjectBuilder()
                                                                                        .add("value", "Simpson, Homer")
                                                                                        .add("typeClass", "primitive")
                                                                                        .add("multiple", false)
                                                                                        .add("typeName", "authorName"))
                                                                )
                                                        )
                                                        .add("typeClass", "compound")
                                                        .add("multiple", true)
                                                        .add("typeName", "author")
                                                )
                                                .add(Json.createObjectBuilder()
                                                        .add("value", Json.createArrayBuilder()
                                                                .add(Json.createObjectBuilder()
                                                                        .add("datasetContactEmail",
                                                                                Json.createObjectBuilder()
                                                                                        .add("value", "hsimpson@mailinator.com")
                                                                                        .add("typeClass", "primitive")
                                                                                        .add("multiple", false)
                                                                                        .add("typeName", "datasetContactEmail"))
                                                                )
                                                        )
                                                        .add("typeClass", "compound")
                                                        .add("multiple", true)
                                                        .add("typeName", "datasetContact")
                                                )
                                                .add(Json.createObjectBuilder()
                                                        .add("value", Json.createArrayBuilder()
                                                                .add(Json.createObjectBuilder()
                                                                        .add("dsDescriptionValue",
                                                                                Json.createObjectBuilder()
                                                                                        .add("value", "This a test dataset.")
                                                                                        .add("typeClass", "primitive")
                                                                                        .add("multiple", false)
                                                                                        .add("typeName", "dsDescriptionValue"))
                                                                )
                                                        )
                                                        .add("typeClass", "compound")
                                                        .add("multiple", true)
                                                        .add("typeName", "dsDescription")
                                                )
                                                .add(Json.createObjectBuilder()
                                                        .add("value", Json.createArrayBuilder()
                                                                .add("Other")
                                                        )
                                                        .add("typeClass", "controlledVocabulary")
                                                        .add("multiple", true)
                                                        .add("typeName", "subject")
                                                )
                                        )
                                )
                        ));

        String json = datasetJson.build().toString();

        Response importJSONNoPid = UtilIT.importDatasetViaNativeApi(apiToken, dataverseAlias, json,  null, "no");
        logger.info(importJSONNoPid.prettyPrint());
        assertEquals(400, importJSONNoPid.getStatusCode());

        String body = importJSONNoPid.getBody().asString();
        String status = JsonPath.from(body).getString("status");
        assertEquals("ERROR", status);

        String message = JsonPath.from(body).getString("message");
        assertEquals(
                "Please provide a persistent identifier, either by including it in the JSON, or by using the pid query parameter.",
                message
        );

        Response importJSONNoPidRelease = UtilIT.importDatasetViaNativeApi(apiToken, dataverseAlias, json, null, "yes");
        logger.info( importJSONNoPidRelease.prettyPrint());
        assertEquals(400, importJSONNoPidRelease.getStatusCode());

        body = importJSONNoPidRelease.getBody().asString();
        status = JsonPath.from(body).getString("status");
        assertEquals("ERROR", status);

        message = JsonPath.from(body).getString("message");
        assertEquals(
                "Please provide a persistent identifier, either by including it in the JSON, or by using the pid query parameter.",
                message
        );

        Response importJSONUnmanagedPid = UtilIT.importDatasetViaNativeApi(apiToken, dataverseAlias, json, "doi:10.5073/FK2/ABCD11", "no");
        logger.info(importJSONUnmanagedPid.prettyPrint());
        assertEquals(400, importJSONUnmanagedPid.getStatusCode());

        body = importJSONUnmanagedPid.getBody().asString();
        status = JsonPath.from(body).getString("status");
        assertEquals("ERROR", status);

        message = JsonPath.from(body).getString("message");
        assertEquals(
                "Cannot import a dataset that has a PID that doesn't match the server's settings",
                message
        );

        // Under normal conditions, you shouldn't need to destroy these datasets.
        // Uncomment if they're still around from a previous failed run.
//        Response destroy1 = UtilIT.destroyDataset("doi:10.5072/FK2/ABCD11", apiToken);
//        destroy1.prettyPrint();
//        Response destroy2 = UtilIT.destroyDataset("doi:10.5072/FK2/ABCD22", apiToken);
//        destroy2.prettyPrint();

        Response importJSONPid = UtilIT.importDatasetViaNativeApi(apiToken, dataverseAlias, json,  "doi:10.5072/FK2/ABCD11", "no");
        logger.info(importJSONPid.prettyPrint());
        assertEquals(201, importJSONPid.getStatusCode());

        Response importJSONPidRel = UtilIT.importDatasetViaNativeApi(apiToken, dataverseAlias, json,  "doi:10.5072/FK2/ABCD22", "yes");
        logger.info(importJSONPidRel.prettyPrint());
        assertEquals(201, importJSONPidRel.getStatusCode());

        Integer datasetIdInt = JsonPath.from(importJSONPid.body().asString()).getInt("data.id");

        Response search1 = UtilIT.search("id:dataset_" + datasetIdInt + "_draft", apiToken); // santity check, can find it
        search1.prettyPrint();
        search1.then().assertThat()
                .body("data.total_count", CoreMatchers.is(1))
                .body("data.count_in_response", CoreMatchers.is(1))
                .body("data.items[0].name", CoreMatchers.is("Test Dataset"))
                .statusCode(OK.getStatusCode());

        //cleanup

        Response destroyDatasetResponse = UtilIT.destroyDataset(datasetIdInt, apiToken);
        assertEquals(200, destroyDatasetResponse.getStatusCode());

        Integer datasetIdIntPidRel = JsonPath.from(importJSONPidRel.body().asString()).getInt("data.id");
        Response destroyDatasetResponsePidRel = UtilIT.destroyDataset(datasetIdIntPidRel, apiToken);
        assertEquals(200, destroyDatasetResponsePidRel.getStatusCode());

        UtilIT.sleepForDeadlock(UtilIT.MAXIMUM_IMPORT_DURATION);

        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        assertEquals(200, deleteDataverseResponse.getStatusCode());

        Response deleteUserResponse = UtilIT.deleteUser(username);
        assertEquals(200, deleteUserResponse.getStatusCode());
    }

    @Test
    public void testAttributesApi() {
        Response createUser = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        if (createDataverseResponse.getStatusCode() != 201) {
            System.out.println("A workspace for testing (a dataverse) couldn't be created in the root dataverse. The output was:\n\n" + createDataverseResponse.body().asString());
            System.out.println("\nPlease ensure that users can created dataverses in the root in order for this test to run.");
        } else {
            createDataverseResponse.prettyPrint();
        }
        assertEquals(201, createDataverseResponse.getStatusCode());

        String collectionAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        String newCollectionAlias = collectionAlias + "RENAMED";

        // Change the name of the collection:

        String newCollectionName = "Renamed Name";
        Response changeAttributeResp = UtilIT.setCollectionAttribute(collectionAlias, "name", newCollectionName, apiToken);
        changeAttributeResp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("message.message", equalTo("Update successful"));

        // Change the description of the collection:

        String newDescription = "Renamed Description";
        changeAttributeResp = UtilIT.setCollectionAttribute(collectionAlias, "description", newDescription, apiToken);
        changeAttributeResp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("message.message", equalTo("Update successful"));

        // Change the affiliation of the collection:

        String newAffiliation = "Renamed Affiliation";
        changeAttributeResp = UtilIT.setCollectionAttribute(collectionAlias, "affiliation", newAffiliation, apiToken);
        changeAttributeResp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("message.message", equalTo("Update successful"));

        // Cannot update filePIDsEnabled from a regular user:

        changeAttributeResp = UtilIT.setCollectionAttribute(collectionAlias, "filePIDsEnabled", "true", apiToken);
        changeAttributeResp.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode());

        // Change the alias of the collection:

        changeAttributeResp = UtilIT.setCollectionAttribute(collectionAlias, "alias", newCollectionAlias, apiToken);
        changeAttributeResp.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("message.message", equalTo("Update successful"));

        // Check on the collection, under the new alias:

        Response collectionInfoResponse = UtilIT.exportDataverse(newCollectionAlias, apiToken);
        collectionInfoResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.alias", equalTo(newCollectionAlias))
                .body("data.name", equalTo(newCollectionName))
                .body("data.description", equalTo(newDescription))
                .body("data.affiliation", equalTo(newAffiliation));

        // Delete the collection (again, using its new alias):

        Response deleteCollectionResponse = UtilIT.deleteDataverse(newCollectionAlias, apiToken);
        assertEquals(OK.getStatusCode(), deleteCollectionResponse.getStatusCode());

        // Cannot update root collection from a regular user:

        changeAttributeResp = UtilIT.setCollectionAttribute("root", "name", newCollectionName, apiToken);
        changeAttributeResp.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode());

        collectionInfoResponse = UtilIT.exportDataverse("root", apiToken);

        collectionInfoResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.name", equalTo("Root"));
    }

    @Test
    public void testListMetadataBlocks() {
        Response createUserResponse = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createUserResponse);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        // New Dataverse should return just the citation block and its displayOnCreate fields when onlyDisplayedOnCreate=true and returnDatasetFieldTypes=true
        Response listMetadataBlocks = UtilIT.listMetadataBlocks(dataverseAlias, true, true, apiToken);
        listMetadataBlocks.prettyPrint();
        listMetadataBlocks.then().assertThat().statusCode(OK.getStatusCode());
        listMetadataBlocks.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.size()", equalTo(1))
                .body("data[0].name", is("citation"))
                .body("data[0].fields.title.displayOnCreate", equalTo(true))
                .body("data[0].fields.size()", is(10)) // 28 - 18 child duplicates
                .body("data[0].fields.author.childFields.size()", is(4));

        Response setMetadataBlocksResponse = UtilIT.setMetadataBlocks(dataverseAlias, Json.createArrayBuilder().add("citation").add("astrophysics"), apiToken);
        setMetadataBlocksResponse.prettyPrint();
        setMetadataBlocksResponse.then().assertThat().statusCode(OK.getStatusCode());

        String[] testInputLevelNames = {"geographicCoverage", "country", "city", "notesText"};
        boolean[] testRequiredInputLevels = {false, true, false, false};
        boolean[] testIncludedInputLevels = {false, true, true, false};
        Response updateDataverseInputLevelsResponse = UtilIT.updateDataverseInputLevels(dataverseAlias, testInputLevelNames, testRequiredInputLevels, testIncludedInputLevels, apiToken);
        updateDataverseInputLevelsResponse.prettyPrint();
        updateDataverseInputLevelsResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Dataverse not found
        Response listMetadataBlocksResponse = UtilIT.listMetadataBlocks("-1", false, false, apiToken);
        listMetadataBlocksResponse.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        // Existent dataverse and no optional params
        String[] expectedAllMetadataBlockDisplayNames = {"Astronomy and Astrophysics Metadata", "Citation Metadata", "Geospatial Metadata"};

        listMetadataBlocksResponse = UtilIT.listMetadataBlocks(dataverseAlias, false, false, apiToken);
        listMetadataBlocksResponse.prettyPrint();
        listMetadataBlocksResponse.then().assertThat().statusCode(OK.getStatusCode());
        listMetadataBlocksResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].fields", equalTo(null))
                .body("data[1].fields", equalTo(null))
                .body("data[2].fields", equalTo(null))
                .body("data.size()", equalTo(3));

        String actualMetadataBlockDisplayName1 = listMetadataBlocksResponse.then().extract().path("data[0].displayName");
        String actualMetadataBlockDisplayName2 = listMetadataBlocksResponse.then().extract().path("data[1].displayName");
        String actualMetadataBlockDisplayName3 = listMetadataBlocksResponse.then().extract().path("data[2].displayName");
        assertNotEquals(actualMetadataBlockDisplayName1, actualMetadataBlockDisplayName2);
        assertNotEquals(actualMetadataBlockDisplayName1, actualMetadataBlockDisplayName3);
        assertNotEquals(actualMetadataBlockDisplayName2, actualMetadataBlockDisplayName3);
        assertThat(expectedAllMetadataBlockDisplayNames, hasItemInArray(actualMetadataBlockDisplayName1));
        assertThat(expectedAllMetadataBlockDisplayNames, hasItemInArray(actualMetadataBlockDisplayName2));
        assertThat(expectedAllMetadataBlockDisplayNames, hasItemInArray(actualMetadataBlockDisplayName3));

        // Existent dataverse and onlyDisplayedOnCreate=true
        String[] expectedOnlyDisplayedOnCreateMetadataBlockDisplayNames = {"Citation Metadata", "Geospatial Metadata"};

        listMetadataBlocksResponse = UtilIT.listMetadataBlocks(dataverseAlias, true, false, apiToken);
        listMetadataBlocksResponse.then().assertThat().statusCode(OK.getStatusCode());
        listMetadataBlocksResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].fields", equalTo(null))
                .body("data[1].fields", equalTo(null))
                .body("data.size()", equalTo(2));

        actualMetadataBlockDisplayName1 = listMetadataBlocksResponse.then().extract().path("data[0].displayName");
        actualMetadataBlockDisplayName2 = listMetadataBlocksResponse.then().extract().path("data[1].displayName");
        assertNotEquals(actualMetadataBlockDisplayName1, actualMetadataBlockDisplayName2);
        assertThat(expectedOnlyDisplayedOnCreateMetadataBlockDisplayNames, hasItemInArray(actualMetadataBlockDisplayName1));
        assertThat(expectedOnlyDisplayedOnCreateMetadataBlockDisplayNames, hasItemInArray(actualMetadataBlockDisplayName2));

        // Existent dataverse and returnDatasetFieldTypes=true
        listMetadataBlocksResponse = UtilIT.listMetadataBlocks(dataverseAlias, false, true, apiToken);
        listMetadataBlocksResponse.then().assertThat().statusCode(OK.getStatusCode());
        listMetadataBlocksResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].fields", not(equalTo(null)))
                .body("data[1].fields", not(equalTo(null)))
                .body("data[2].fields", not(equalTo(null)))
                .body("data.size()", equalTo(3));

        actualMetadataBlockDisplayName1 = listMetadataBlocksResponse.then().extract().path("data[0].displayName");
        actualMetadataBlockDisplayName2 = listMetadataBlocksResponse.then().extract().path("data[1].displayName");
        actualMetadataBlockDisplayName3 = listMetadataBlocksResponse.then().extract().path("data[2].displayName");
        assertNotEquals(actualMetadataBlockDisplayName1, actualMetadataBlockDisplayName2);
        assertNotEquals(actualMetadataBlockDisplayName1, actualMetadataBlockDisplayName3);
        assertNotEquals(actualMetadataBlockDisplayName2, actualMetadataBlockDisplayName3);
        assertThat(expectedAllMetadataBlockDisplayNames, hasItemInArray(actualMetadataBlockDisplayName1));
        assertThat(expectedAllMetadataBlockDisplayNames, hasItemInArray(actualMetadataBlockDisplayName2));
        assertThat(expectedAllMetadataBlockDisplayNames, hasItemInArray(actualMetadataBlockDisplayName3));

        // Check dataset fields for the updated input levels are retrieved
        int geospatialMetadataBlockIndex = actualMetadataBlockDisplayName1.equals("Geospatial Metadata") ? 0 : actualMetadataBlockDisplayName2.equals("Geospatial Metadata") ? 1 : 2;

        // Since the included property of notesText is set to false, we should retrieve the total number of fields minus one
        int citationMetadataBlockIndex = geospatialMetadataBlockIndex == 0 ? 1 : 0;
        listMetadataBlocksResponse.then().assertThat()
                .body(String.format("data[%d].fields.size()", citationMetadataBlockIndex), equalTo(34)); // 79 minus 45 child duplicates

        // Since the included property of geographicCoverage is set to false, we should retrieve the total number of fields minus one
        listMetadataBlocksResponse.then().assertThat()
                .body(String.format("data[%d].fields.size()", geospatialMetadataBlockIndex), equalTo(2));

        listMetadataBlocksResponse = UtilIT.getMetadataBlock("geospatial");
        String actualGeospatialMetadataField1 = listMetadataBlocksResponse.then().extract().path(String.format("data.fields['geographicCoverage'].name"));
        String actualGeospatialMetadataField2 = listMetadataBlocksResponse.then().extract().path(String.format("data.fields['geographicCoverage'].childFields['country'].name"));
        String actualGeospatialMetadataField3 = listMetadataBlocksResponse.then().extract().path(String.format("data.fields['geographicCoverage'].childFields['city'].name"));
        
        listMetadataBlocksResponse.then().assertThat().statusCode(OK.getStatusCode())
        .body("data.fields['geographicCoverage'].childFields.size()", equalTo(4))
        .body("data.fields['geographicBoundingBox'].childFields.size()", equalTo(4));

        assertNotNull(actualGeospatialMetadataField1);
        assertNotNull(actualGeospatialMetadataField2);
        assertNotNull(actualGeospatialMetadataField3);

        // Existent dataverse and onlyDisplayedOnCreate=true and returnDatasetFieldTypes=true
        listMetadataBlocksResponse = UtilIT.listMetadataBlocks(dataverseAlias, true, true, apiToken);
        listMetadataBlocksResponse.then().assertThat().statusCode(OK.getStatusCode());
        listMetadataBlocksResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].fields", not(equalTo(null)))
                .body("data[1].fields", not(equalTo(null)))
                .body("data.size()", equalTo(2));

        actualMetadataBlockDisplayName1 = listMetadataBlocksResponse.then().extract().path("data[0].displayName");
        actualMetadataBlockDisplayName2 = listMetadataBlocksResponse.then().extract().path("data[1].displayName");
        assertNotEquals(actualMetadataBlockDisplayName1, actualMetadataBlockDisplayName2);
        assertThat(expectedOnlyDisplayedOnCreateMetadataBlockDisplayNames, hasItemInArray(actualMetadataBlockDisplayName1));
        assertThat(expectedOnlyDisplayedOnCreateMetadataBlockDisplayNames, hasItemInArray(actualMetadataBlockDisplayName2));

        // Check dataset fields for the updated input levels are retrieved
        geospatialMetadataBlockIndex = actualMetadataBlockDisplayName2.equals("Geospatial Metadata") ? 1 : 0;

        listMetadataBlocksResponse.then().assertThat()
                .body(String.format("data[%d].fields.size()", geospatialMetadataBlockIndex), equalTo(0));

//        actualGeospatialMetadataField1 = listMetadataBlocksResponse.then().extract().path(String.format("data[%d].fields.geographicCoverage.name", geospatialMetadataBlockIndex));
//        actualGeospatialMetadataField2 = listMetadataBlocksResponse.then().extract().path(String.format("data[%d].fields.geographicCoverage.childFields['country'].name", geospatialMetadataBlockIndex));
//        actualGeospatialMetadataField3 = listMetadataBlocksResponse.then().extract().path(String.format("data[%d].fields.geographicCoverage.childFields['city'].name", geospatialMetadataBlockIndex));

//        assertNull(actualGeospatialMetadataField1);
//        assertNotNull(actualGeospatialMetadataField2);
//        assertNull(actualGeospatialMetadataField3);

        citationMetadataBlockIndex = geospatialMetadataBlockIndex == 0 ? 1 : 0;

        // notesText has displayOnCreate=true but has include=false, so should not be retrieved
        String notesTextCitationMetadataField = listMetadataBlocksResponse.then().extract().path(String.format("data[%d].fields.notesText.name", citationMetadataBlockIndex));
        assertNull(notesTextCitationMetadataField);

        // producerName is a conditionally required field, so should not be retrieved
        String producerNameCitationMetadataField = listMetadataBlocksResponse.then().extract().path(String.format("data[%d].fields.producerName.name", citationMetadataBlockIndex));
        assertNull(producerNameCitationMetadataField);

        // author is a required field, so should be retrieved
        String authorCitationMetadataField = listMetadataBlocksResponse.then().extract().path(String.format("data[%d].fields.author.name", citationMetadataBlockIndex));
        assertNotNull(authorCitationMetadataField);

        // User has no permissions on the requested dataverse
        Response createSecondUserResponse = UtilIT.createRandomUser();
        String secondApiToken = UtilIT.getApiTokenFromResponse(createSecondUserResponse);

        createDataverseResponse = UtilIT.createRandomDataverse(secondApiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String secondDataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        listMetadataBlocksResponse = UtilIT.listMetadataBlocks(secondDataverseAlias, true, true, apiToken);
        listMetadataBlocksResponse.then().assertThat().statusCode(UNAUTHORIZED.getStatusCode());

        // List metadata blocks from Root
        listMetadataBlocksResponse = UtilIT.listMetadataBlocks("root", true, true, apiToken);
        listMetadataBlocksResponse.then().assertThat().statusCode(OK.getStatusCode());
        listMetadataBlocksResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].displayName", equalTo("Citation Metadata"))
                .body("data[0].fields", not(equalTo(null)))
                .body("data.size()", equalTo(1));
        
        // Checking child / parent logic
        listMetadataBlocksResponse = UtilIT.getMetadataBlock("citation");
        listMetadataBlocksResponse.then().assertThat().statusCode(OK.getStatusCode());
        listMetadataBlocksResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.displayName", equalTo("Citation Metadata"))
                .body("data.fields", not(equalTo(null)))
                .body("data.fields.otherIdAgency", equalTo(null))
                .body("data.fields.otherId.childFields.size()", equalTo(2));
    }

    @Test
    public void testFeatureDataverse() throws Exception {

        Response createUser = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        assertEquals(200, publishDataverse.getStatusCode());


        Response createSubDVToBeFeatured = UtilIT.createSubDataverse(UtilIT.getRandomDvAlias() + "-feature", null, apiToken, dataverseAlias);
        String subDataverseAlias = UtilIT.getAliasFromResponse(createSubDVToBeFeatured);

        //publish a sub dataverse so that the owner will have something to feature
        Response createSubDVToBePublished = UtilIT.createSubDataverse(UtilIT.getRandomDvAlias() + "-pub", null, apiToken, dataverseAlias);
        assertEquals(201, createSubDVToBePublished.getStatusCode());
        String subDataverseAliasPub = UtilIT.getAliasFromResponse(createSubDVToBePublished);
        publishDataverse = UtilIT.publishDataverseViaNativeApi(subDataverseAliasPub, apiToken);
        assertEquals(200, publishDataverse.getStatusCode());

        //can't feature a dataverse that is unpublished
        Response featureSubDVResponseUnpublished = UtilIT.addFeaturedDataverse(dataverseAlias, subDataverseAlias, apiToken);
        featureSubDVResponseUnpublished.prettyPrint();
        assertEquals(400, featureSubDVResponseUnpublished.getStatusCode());
        featureSubDVResponseUnpublished.then().assertThat()
                .body(containsString("may not be featured"));

        //can't feature a dataverse you don't own
        Response featureSubDVResponseNotOwned = UtilIT.addFeaturedDataverse(dataverseAlias, "root", apiToken);
        featureSubDVResponseNotOwned.prettyPrint();
        assertEquals(400, featureSubDVResponseNotOwned.getStatusCode());
        featureSubDVResponseNotOwned.then().assertThat()
                .body(containsString("may not be featured"));

        //can't feature a dataverse that doesn't exist
        Response featureSubDVResponseNotExist = UtilIT.addFeaturedDataverse(dataverseAlias, "dummy-alias-sek-foobar-333", apiToken);
        featureSubDVResponseNotExist.prettyPrint();
        assertEquals(400, featureSubDVResponseNotExist.getStatusCode());
        featureSubDVResponseNotExist.then().assertThat()
                .body(containsString("Can't find dataverse collection"));

        publishDataverse = UtilIT.publishDataverseViaNativeApi(subDataverseAlias, apiToken);
        assertEquals(200, publishDataverse.getStatusCode());

        //once published it should work
        Response featureSubDVResponse = UtilIT.addFeaturedDataverse(dataverseAlias, subDataverseAlias, apiToken);
        featureSubDVResponse.prettyPrint();
        assertEquals(OK.getStatusCode(), featureSubDVResponse.getStatusCode());


        Response getFeaturedDataverseResponse = UtilIT.getFeaturedDataverses(dataverseAlias, apiToken);
        getFeaturedDataverseResponse.prettyPrint();
        assertEquals(OK.getStatusCode(), getFeaturedDataverseResponse.getStatusCode());
        getFeaturedDataverseResponse.then().assertThat()
                .body("data[0]", equalTo(subDataverseAlias));

        Response deleteFeaturedDataverseResponse = UtilIT.deleteFeaturedDataverses(dataverseAlias, apiToken);
        deleteFeaturedDataverseResponse.prettyPrint();

        assertEquals(OK.getStatusCode(), deleteFeaturedDataverseResponse.getStatusCode());
        deleteFeaturedDataverseResponse.then().assertThat()
                .body(containsString("Featured dataverses have been removed"));

        Response deleteSubCollectionResponse = UtilIT.deleteDataverse(subDataverseAlias, apiToken);
        deleteSubCollectionResponse.prettyPrint();
        assertEquals(OK.getStatusCode(), deleteSubCollectionResponse.getStatusCode());

        Response deleteSubCollectionPubResponse = UtilIT.deleteDataverse(subDataverseAliasPub, apiToken);
        deleteSubCollectionResponse.prettyPrint();
        assertEquals(OK.getStatusCode(), deleteSubCollectionPubResponse.getStatusCode());

        Response deleteCollectionResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteCollectionResponse.prettyPrint();
        assertEquals(OK.getStatusCode(), deleteCollectionResponse.getStatusCode());
    }

    @Test
    public void testUpdateInputLevels() {
        Response createUserResponse = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createUserResponse);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        // Update valid input levels
        String[] testInputLevelNames = {"geographicCoverage", "country"};
        boolean[] testRequiredInputLevels = {true, false};
        boolean[] testIncludedInputLevels = {true, false};
        Response updateDataverseInputLevelsResponse = UtilIT.updateDataverseInputLevels(dataverseAlias, testInputLevelNames, testRequiredInputLevels, testIncludedInputLevels, apiToken);
        String actualInputLevelName = updateDataverseInputLevelsResponse.then().extract().path("data.inputLevels[0].datasetFieldTypeName");
        int geographicCoverageInputLevelIndex = actualInputLevelName.equals("geographicCoverage") ? 0 : 1;
        updateDataverseInputLevelsResponse.then().assertThat()
                .body(String.format("data.inputLevels[%d].include", geographicCoverageInputLevelIndex), equalTo(true))
                .body(String.format("data.inputLevels[%d].required", geographicCoverageInputLevelIndex), equalTo(true))
                .body(String.format("data.inputLevels[%d].include", 1 - geographicCoverageInputLevelIndex), equalTo(false))
                .body(String.format("data.inputLevels[%d].required", 1 - geographicCoverageInputLevelIndex), equalTo(false))
                .statusCode(OK.getStatusCode());
        String actualFieldTypeName1 = updateDataverseInputLevelsResponse.then().extract().path("data.inputLevels[0].datasetFieldTypeName");
        String actualFieldTypeName2 = updateDataverseInputLevelsResponse.then().extract().path("data.inputLevels[1].datasetFieldTypeName");
        assertNotEquals(actualFieldTypeName1, actualFieldTypeName2);
        assertThat(testInputLevelNames, hasItemInArray(actualFieldTypeName1));
        assertThat(testInputLevelNames, hasItemInArray(actualFieldTypeName2));

        // Update input levels with an invalid field type name
        String[] testInvalidInputLevelNames = {"geographicCoverage", "invalid1"};
        updateDataverseInputLevelsResponse = UtilIT.updateDataverseInputLevels(dataverseAlias, testInvalidInputLevelNames, testRequiredInputLevels, testIncludedInputLevels, apiToken);
        updateDataverseInputLevelsResponse.then().assertThat()
                .body("message", equalTo("Invalid dataset field type name: invalid1"))
                .statusCode(BAD_REQUEST.getStatusCode());

        // Update input levels with invalid configuration (field required but not included)
        testIncludedInputLevels = new boolean[]{false, false};
        updateDataverseInputLevelsResponse = UtilIT.updateDataverseInputLevels(dataverseAlias, testInputLevelNames, testRequiredInputLevels, testIncludedInputLevels, apiToken);
        updateDataverseInputLevelsResponse.then().assertThat()
                .body("message", equalTo(BundleUtil.getStringFromBundle("dataverse.inputlevels.error.cannotberequiredifnotincluded", List.of("geographicCoverage"))))
                .statusCode(BAD_REQUEST.getStatusCode());

        // Update invalid empty input levels
        testInputLevelNames = new String[]{};
        updateDataverseInputLevelsResponse = UtilIT.updateDataverseInputLevels(dataverseAlias, testInputLevelNames, testRequiredInputLevels, testIncludedInputLevels, apiToken);
        updateDataverseInputLevelsResponse.then().assertThat()
                .body("message", equalTo("Error while updating dataverse input levels: Input level list cannot be null or empty"))
                .statusCode(INTERNAL_SERVER_ERROR.getStatusCode());
    }

    @Test
    public void testAddDataverse() {
        Response createUser = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        String testAliasSuffix = "-add-dataverse";

        // Without optional input levels and facet ids
        String testDataverseAlias = UtilIT.getRandomDvAlias() + testAliasSuffix;
        Response createSubDataverseResponse = UtilIT.createSubDataverse(testDataverseAlias, null, apiToken, "root");
        createSubDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        Response listMetadataBlocksResponse = UtilIT.listMetadataBlocks(testDataverseAlias, false, false, apiToken);
        listMetadataBlocksResponse.then().assertThat().statusCode(OK.getStatusCode());
        String actualMetadataBlockName = listMetadataBlocksResponse.then().extract().path("data[0].name");
        assertEquals(actualMetadataBlockName, "citation");

        // Assert root facets are configured
        String[] expectedRootFacetIds = {"authorName", "subject", "keywordValue", "dateOfDeposit"};
        Response listDataverseFacetsResponse = UtilIT.listDataverseFacets(testDataverseAlias, apiToken);
        List<String> actualFacetNames = listDataverseFacetsResponse.then().extract().path("data");
        assertThat("Facet names should match expected root facet ids", actualFacetNames, containsInAnyOrder(expectedRootFacetIds));

        // With optional input levels and facet ids
        String[] testInputLevelNames = {"geographicCoverage", "country"};
        String[] testFacetIds = {"language", "contributorName"};
        String[] testMetadataBlockNames = {"citation", "geospatial"};
        testDataverseAlias = UtilIT.getRandomDvAlias() + testAliasSuffix;
        createSubDataverseResponse = UtilIT.createSubDataverse(testDataverseAlias, null, apiToken, "root", testInputLevelNames, testFacetIds, testMetadataBlockNames);
        createSubDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());

        // Assert custom facets are configured
        listDataverseFacetsResponse = UtilIT.listDataverseFacets(testDataverseAlias, apiToken);
        String actualFacetName1 = listDataverseFacetsResponse.then().extract().path("data[0]");
        String actualFacetName2 = listDataverseFacetsResponse.then().extract().path("data[1]");
        assertNotEquals(actualFacetName1, actualFacetName2);
        assertThat(testFacetIds, hasItemInArray(actualFacetName1));
        assertThat(testFacetIds, hasItemInArray(actualFacetName2));

        // Assert input levels are configured
        Response listDataverseInputLevelsResponse = UtilIT.listDataverseInputLevels(testDataverseAlias, apiToken);
        String actualInputLevelName1 = listDataverseInputLevelsResponse.then().extract().path("data[0].datasetFieldTypeName");
        String actualInputLevelName2 = listDataverseInputLevelsResponse.then().extract().path("data[1].datasetFieldTypeName");
        assertNotEquals(actualFacetName1, actualFacetName2);
        assertThat(testInputLevelNames, hasItemInArray(actualInputLevelName1));
        assertThat(testInputLevelNames, hasItemInArray(actualInputLevelName2));

        // Assert metadata blocks are configured
        listMetadataBlocksResponse = UtilIT.listMetadataBlocks(testDataverseAlias, false, false, apiToken);
        listMetadataBlocksResponse.then().assertThat().statusCode(OK.getStatusCode());
        String actualMetadataBlockName1 = listMetadataBlocksResponse.then().extract().path("data[0].name");
        String actualMetadataBlockName2 = listMetadataBlocksResponse.then().extract().path("data[1].name");
        assertNotEquals(actualMetadataBlockName1, actualMetadataBlockName2);
        assertThat(testMetadataBlockNames, hasItemInArray(actualMetadataBlockName1));
        assertThat(testMetadataBlockNames, hasItemInArray(actualMetadataBlockName2));

        // Setting metadata blocks without citation
        testDataverseAlias = UtilIT.getRandomDvAlias() + testAliasSuffix;
        String[] testMetadataBlockNamesWithoutCitation = {"geospatial"};
        createSubDataverseResponse = UtilIT.createSubDataverse(testDataverseAlias, null, apiToken, "root", null, null, testMetadataBlockNamesWithoutCitation);
        createSubDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());

        // Assert metadata blocks including citation are configured
        String[] testExpectedBlockNames = {"citation", "geospatial"};
        actualMetadataBlockName1 = listMetadataBlocksResponse.then().extract().path("data[0].name");
        actualMetadataBlockName2 = listMetadataBlocksResponse.then().extract().path("data[1].name");
        assertNotEquals(actualMetadataBlockName1, actualMetadataBlockName2);
        assertThat(testExpectedBlockNames, hasItemInArray(actualMetadataBlockName1));
        assertThat(testExpectedBlockNames, hasItemInArray(actualMetadataBlockName2));

        // Should return error when an invalid facet id is sent
        String invalidFacetId = "invalidFacetId";
        String[] testInvalidFacetIds = {"authorName", invalidFacetId};
        testDataverseAlias = UtilIT.getRandomDvAlias() + testAliasSuffix;
        createSubDataverseResponse = UtilIT.createSubDataverse(testDataverseAlias, null, apiToken, "root", testInputLevelNames, testInvalidFacetIds, testMetadataBlockNames);
        createSubDataverseResponse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo(BundleUtil.getStringFromBundle("dataverse.facets.error.fieldtypenotfound",  Arrays.asList(invalidFacetId))));

        // Should return error when an invalid input level is sent
        String invalidInputLevelName = "wrongInputLevel";
        String[] testInvalidInputLevelNames = {"geographicCoverage", invalidInputLevelName};
        testDataverseAlias = UtilIT.getRandomDvAlias() + testAliasSuffix;
        createSubDataverseResponse = UtilIT.createSubDataverse(testDataverseAlias, null, apiToken, "root", testInvalidInputLevelNames, testFacetIds, testMetadataBlockNames);
        createSubDataverseResponse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("Invalid dataset field type name: " + invalidInputLevelName));

        // Should return error when an invalid metadata block name is sent
        String invalidMetadataBlockName = "invalidMetadataBlockName";
        String[] testInvalidMetadataBlockNames = {"citation", invalidMetadataBlockName};
        testDataverseAlias = UtilIT.getRandomDvAlias() + testAliasSuffix;
        createSubDataverseResponse = UtilIT.createSubDataverse(testDataverseAlias, null, apiToken, "root", testInputLevelNames, testInvalidFacetIds, testInvalidMetadataBlockNames);
        createSubDataverseResponse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo("Invalid metadata block name: \"" + invalidMetadataBlockName + "\""));
    }

    @Test
    public void testUpdateDataverse() throws JsonParseException {
        Response createUser = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        String testAliasSuffix = "-update-dataverse";

        String testDataverseAlias = UtilIT.getRandomDvAlias() + testAliasSuffix;
        Response createSubDataverseResponse = UtilIT.createSubDataverse(testDataverseAlias, null, apiToken, "root");
        createSubDataverseResponse.prettyPrint();
        createSubDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode())
                .body("data.effectiveDatasetFileCountLimit", equalTo(null))
                .body("data.datasetFileCountLimit", equalTo(null));

        // Update the dataverse with a datasetFileCountLimit of 500
        JsonObject data = JsonUtil.getJsonObject(createSubDataverseResponse.getBody().asString());
        JsonParser parser = new JsonParser();
        Dataverse dv = parser.parseDataverse(data.getJsonObject("data"));
        dv.setDatasetFileCountLimit(500);
        Response updateDataverseResponse = UtilIT.updateDataverse(testDataverseAlias, dv, apiToken);
        updateDataverseResponse.prettyPrint();
        updateDataverseResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.effectiveDatasetFileCountLimit", equalTo(500))
                .body("data.datasetFileCountLimit", equalTo(500));

        String newAlias = UtilIT.getRandomDvAlias() + testAliasSuffix;
        String newName = "New Test Dataverse Name";
        String newAffiliation = "New Test Dataverse Affiliation";
        String newDataverseType = Dataverse.DataverseType.TEACHING_COURSES.toString();
        String[] newContactEmails = new String[]{"new_email@dataverse.com"};
        String[] newInputLevelNames = new String[]{"geographicCoverage"};
        String[] newFacetIds = new String[]{"contributorName"};
        String[] newMetadataBlockNames = new String[]{"citation", "geospatial", "biomedical"};

        // Assert that the error is returned for having both MetadataBlockNames and inheritMetadataBlocksFromParent
        updateDataverseResponse = UtilIT.updateDataverse(
                testDataverseAlias, newAlias, newName, newAffiliation, newDataverseType, newContactEmails, newInputLevelNames,
                null, newMetadataBlockNames, apiToken,
                Boolean.TRUE, Boolean.TRUE, null
        );
        updateDataverseResponse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo(MessageFormat.format(BundleUtil.getStringFromBundle("dataverse.metadatablocks.error.containslistandinheritflag"), "metadataBlockNames", "inheritMetadataBlocksFromParent")));

        // Assert that the error is returned for having both facetIds and inheritFacetsFromParent
        updateDataverseResponse = UtilIT.updateDataverse(
                testDataverseAlias, newAlias, newName, newAffiliation, newDataverseType, newContactEmails, newInputLevelNames,
                newFacetIds, null, apiToken,
                Boolean.TRUE, Boolean.TRUE, null
        );
        updateDataverseResponse.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode())
                .body("message", equalTo(MessageFormat.format(BundleUtil.getStringFromBundle("dataverse.metadatablocks.error.containslistandinheritflag"), "facetIds", "inheritFacetsFromParent")));

        // Assert dataverse properties are updated
        updateDataverseResponse = UtilIT.updateDataverse(
                testDataverseAlias, newAlias, newName, newAffiliation, newDataverseType, newContactEmails, newInputLevelNames,
                newFacetIds, newMetadataBlockNames, apiToken
        );
        updateDataverseResponse.then().assertThat().statusCode(OK.getStatusCode());
        String actualDataverseAlias = updateDataverseResponse.then().extract().path("data.alias");
        assertEquals(newAlias, actualDataverseAlias);
        String actualDataverseName = updateDataverseResponse.then().extract().path("data.name");
        assertEquals(newName, actualDataverseName);
        String actualDataverseAffiliation = updateDataverseResponse.then().extract().path("data.affiliation");
        assertEquals(newAffiliation, actualDataverseAffiliation);
        String actualDataverseType = updateDataverseResponse.then().extract().path("data.dataverseType");
        assertEquals(newDataverseType, actualDataverseType);
        String actualContactEmail = updateDataverseResponse.then().extract().path("data.dataverseContacts[0].contactEmail");
        assertEquals("new_email@dataverse.com", actualContactEmail);

        // Assert metadata blocks are updated
        Response listMetadataBlocksResponse = UtilIT.listMetadataBlocks(newAlias, false, false, apiToken);
        String actualDataverseMetadataBlock1 = listMetadataBlocksResponse.then().extract().path("data[0].name");
        String actualDataverseMetadataBlock2 = listMetadataBlocksResponse.then().extract().path("data[1].name");
        String actualDataverseMetadataBlock3 = listMetadataBlocksResponse.then().extract().path("data[2].name");
        assertThat(newMetadataBlockNames, hasItemInArray(actualDataverseMetadataBlock1));
        assertThat(newMetadataBlockNames, hasItemInArray(actualDataverseMetadataBlock2));
        assertThat(newMetadataBlockNames, hasItemInArray(actualDataverseMetadataBlock3));

        // Assert custom facets are updated
        Response listDataverseFacetsResponse = UtilIT.listDataverseFacets(newAlias, apiToken);
        String actualFacetName = listDataverseFacetsResponse.then().extract().path("data[0]");
        assertThat(newFacetIds, hasItemInArray(actualFacetName));

        // Assert input levels are updated
        Response listDataverseInputLevelsResponse = UtilIT.listDataverseInputLevels(newAlias, apiToken);
        String actualInputLevelName = listDataverseInputLevelsResponse.then().extract().path("data[0].datasetFieldTypeName");
        assertThat(newInputLevelNames, hasItemInArray(actualInputLevelName));

        // The alias has been changed, so we should not be able to do any operation using the old one
        String oldDataverseAlias = testDataverseAlias;
        Response getDataverseResponse = UtilIT.listDataverseFacets(oldDataverseAlias, apiToken);
        getDataverseResponse.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        listMetadataBlocksResponse = UtilIT.listMetadataBlocks(newAlias, false, false, apiToken);
        listMetadataBlocksResponse.prettyPrint();
        updateDataverseResponse = UtilIT.updateDataverse(
                newAlias, newAlias, newName, newAffiliation, newDataverseType, newContactEmails,
                null,
                null,
                null,
                apiToken
        );
        updateDataverseResponse.prettyPrint();
        listMetadataBlocksResponse = UtilIT.listMetadataBlocks(newAlias, false, false, apiToken);
        listMetadataBlocksResponse.prettyPrint();


        // Update the dataverse without including metadata blocks, facets, or input levels
        // ignore the missing data so the metadata blocks, facets, and input levels are NOT deleted and inherited from the parent
        updateDataverseResponse = UtilIT.updateDataverse(
                newAlias, newAlias, newName, newAffiliation, newDataverseType, newContactEmails,
                null,
                null,
                null,
                apiToken
        );
        updateDataverseResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Assert that the metadata blocks are untouched and NOT inherited from the parent
        listMetadataBlocksResponse = UtilIT.listMetadataBlocks(newAlias, false, false, apiToken);
        listMetadataBlocksResponse.prettyPrint();
        listMetadataBlocksResponse
                .then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.size()", equalTo(3))
                .body("data[0].name", equalTo(actualDataverseMetadataBlock1))
                .body("data[1].name", equalTo(actualDataverseMetadataBlock2))
                .body("data[2].name", equalTo(actualDataverseMetadataBlock3));
        // Assert that the dataverse should still have its input level(s)
        listDataverseInputLevelsResponse = UtilIT.listDataverseInputLevels(newAlias, apiToken);
        listDataverseInputLevelsResponse.prettyPrint();
        listDataverseInputLevelsResponse
                .then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.size()", equalTo(1))
                .body("data[0].datasetFieldTypeName", equalTo("geographicCoverage"));
        // Assert that the dataverse should still have its Facets
        listDataverseFacetsResponse = UtilIT.listDataverseFacets(newAlias, apiToken);
        listDataverseFacetsResponse.prettyPrint();
        listDataverseFacetsResponse
                .then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.size()", equalTo(1))
                .body("data", hasItem("contributorName"));

        // Update the dataverse without setting metadata blocks, facets, or input levels
        // Do NOT ignore the missing data so the metadata blocks, facets, and input levels are deleted and inherited from the parent
        updateDataverseResponse = UtilIT.updateDataverse(
                newAlias,
                newAlias,
                newName,
                newAffiliation,
                newDataverseType,
                newContactEmails,
                null,
                null,
                null,
                apiToken,
                Boolean.TRUE, Boolean.TRUE, null
        );
        updateDataverseResponse.then().assertThat().statusCode(OK.getStatusCode());

        // Assert that the metadata blocks are inherited from the parent
        listMetadataBlocksResponse = UtilIT.listMetadataBlocks(newAlias, false, false, apiToken);
        listMetadataBlocksResponse.prettyPrint();
        listMetadataBlocksResponse
                .then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.size()", equalTo(1))
                .body("data[0].name", equalTo("citation"));

        // Assert that the facets are inherited from the parent
        String[] rootFacetIds = new String[]{"authorName", "subject", "keywordValue", "dateOfDeposit"};
        listDataverseFacetsResponse = UtilIT.listDataverseFacets(newAlias, apiToken);
        String actualFacetName1 = listDataverseFacetsResponse.then().extract().path("data[0]");
        String actualFacetName2 = listDataverseFacetsResponse.then().extract().path("data[1]");
        String actualFacetName3 = listDataverseFacetsResponse.then().extract().path("data[2]");
        String actualFacetName4 = listDataverseFacetsResponse.then().extract().path("data[3]");
        assertThat(rootFacetIds, hasItemInArray(actualFacetName1));
        assertThat(rootFacetIds, hasItemInArray(actualFacetName2));
        assertThat(rootFacetIds, hasItemInArray(actualFacetName3));
        assertThat(rootFacetIds, hasItemInArray(actualFacetName4));

        // Assert that the dataverse should not have any input level
        listDataverseInputLevelsResponse = UtilIT.listDataverseInputLevels(newAlias, apiToken);
        listDataverseInputLevelsResponse
                .then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.size()", equalTo(0));

        // Should return error when the dataverse to edit does not exist
        updateDataverseResponse = UtilIT.updateDataverse(
                "unexistingDataverseAlias",
                newAlias,
                newName,
                newAffiliation,
                newDataverseType,
                newContactEmails,
                newInputLevelNames,
                newFacetIds,
                newMetadataBlockNames,
                apiToken
        );
        updateDataverseResponse.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        // User with unprivileged API token cannot update Root dataverse
        updateDataverseResponse = UtilIT.updateDataverse(
                "root",
                newAlias,
                newName,
                newAffiliation,
                newDataverseType,
                newContactEmails,
                newInputLevelNames,
                newFacetIds,
                newMetadataBlockNames,
                apiToken
        );
        updateDataverseResponse.then().assertThat().statusCode(UNAUTHORIZED.getStatusCode());

        Response rootCollectionInfoResponse = UtilIT.exportDataverse("root", apiToken);
        rootCollectionInfoResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.name", equalTo("Root"));


        updateDataverseResponse = UtilIT.updateDataverse(
                testDataverseAlias, newAlias, newName, newAffiliation, newDataverseType, newContactEmails, newInputLevelNames,
                newFacetIds, newMetadataBlockNames, apiToken);
    }

    @Test
    public void testListFacets() {
        Response createUserResponse = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createUserResponse);
        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        String[] expectedFacetNames = {"authorName", "subject", "keywordValue", "dateOfDeposit"};

        // returnDetails is false
        Response listFacetsResponse = UtilIT.listDataverseFacets(dataverseAlias, false, apiToken);
        listFacetsResponse.then().assertThat().statusCode(OK.getStatusCode());
        String actualFacetName = listFacetsResponse.then().extract().path("data[0]");
        assertThat(expectedFacetNames, hasItemInArray(actualFacetName));

        // returnDetails is true
        String[] expectedDisplayNames = {"Author Name", "Subject", "Keyword Term", "Deposit Date"};
        listFacetsResponse = UtilIT.listDataverseFacets(dataverseAlias, true, apiToken);
        listFacetsResponse.then().assertThat().statusCode(OK.getStatusCode());
        actualFacetName = listFacetsResponse.then().extract().path("data[0].name");
        assertThat(expectedFacetNames, hasItemInArray(actualFacetName));
        String actualDisplayName = listFacetsResponse.then().extract().path("data[0].displayName");
        assertThat(expectedDisplayNames, hasItemInArray(actualDisplayName));
        String actualId = listFacetsResponse.then().extract().path("data[0].id");
        assertNotNull(actualId);

        // Dataverse with custom facets
        String dataverseWithCustomFacetsAlias = UtilIT.getRandomDvAlias() + "customFacets";

        String[] testFacetNames = {"authorName", "authorAffiliation"};
        String[] testMetadataBlockNames = {"citation", "geospatial"};

        Response createSubDataverseResponse = UtilIT.createSubDataverse(dataverseWithCustomFacetsAlias, null, apiToken, "root", null, testFacetNames, testMetadataBlockNames);
        createSubDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());

        listFacetsResponse = UtilIT.listDataverseFacets(dataverseWithCustomFacetsAlias, true, apiToken);
        listFacetsResponse.then().assertThat().statusCode(OK.getStatusCode());

        String actualFacetName1 = listFacetsResponse.then().extract().path("data[0].name");
        String actualFacetName2 = listFacetsResponse.then().extract().path("data[1].name");
        assertNotEquals(actualFacetName1, actualFacetName2);
        assertThat(testFacetNames, hasItemInArray(actualFacetName1));
        assertThat(testFacetNames, hasItemInArray(actualFacetName2));

        String[] testFacetExpectedDisplayNames = {"Author Name", "Author Affiliation"};
        String actualFacetDisplayName1 = listFacetsResponse.then().extract().path("data[0].displayName");
        String actualFacetDisplayName2 = listFacetsResponse.then().extract().path("data[1].displayName");
        assertNotEquals(actualFacetDisplayName1, actualFacetDisplayName2);
        assertThat(testFacetExpectedDisplayNames, hasItemInArray(actualFacetDisplayName1));
        assertThat(testFacetExpectedDisplayNames, hasItemInArray(actualFacetDisplayName2));
    }

    @Test
    public void testGetUserPermissionsOnDataverse() {
        Response createUserResponse = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createUserResponse);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        // Call for dataverse created by the user
        Response getUserPermissionsOnDataverseResponse = UtilIT.getUserPermissionsOnDataverse(dataverseAlias, apiToken);
        getUserPermissionsOnDataverseResponse.then().assertThat().statusCode(OK.getStatusCode());
        boolean canAddDataverse = JsonPath.from(getUserPermissionsOnDataverseResponse.body().asString()).getBoolean("data.canAddDataverse");
        assertTrue(canAddDataverse);
        boolean canAddDataset = JsonPath.from(getUserPermissionsOnDataverseResponse.body().asString()).getBoolean("data.canAddDataset");
        assertTrue(canAddDataset);
        boolean canViewUnpublishedDataverse = JsonPath.from(getUserPermissionsOnDataverseResponse.body().asString()).getBoolean("data.canViewUnpublishedDataverse");
        assertTrue(canViewUnpublishedDataverse);
        boolean canEditDataverse = JsonPath.from(getUserPermissionsOnDataverseResponse.body().asString()).getBoolean("data.canEditDataverse");
        assertTrue(canEditDataverse);
        boolean canManageDataversePermissions = JsonPath.from(getUserPermissionsOnDataverseResponse.body().asString()).getBoolean("data.canManageDataversePermissions");
        assertTrue(canManageDataversePermissions);
        boolean canPublishDataverse = JsonPath.from(getUserPermissionsOnDataverseResponse.body().asString()).getBoolean("data.canPublishDataverse");
        assertTrue(canPublishDataverse);
        boolean canDeleteDataverse = JsonPath.from(getUserPermissionsOnDataverseResponse.body().asString()).getBoolean("data.canDeleteDataverse");
        assertTrue(canDeleteDataverse);

        // Call for root dataverse
        getUserPermissionsOnDataverseResponse = UtilIT.getUserPermissionsOnDataverse("root", apiToken);
        getUserPermissionsOnDataverseResponse.then().assertThat().statusCode(OK.getStatusCode());
        canAddDataverse = JsonPath.from(getUserPermissionsOnDataverseResponse.body().asString()).getBoolean("data.canAddDataverse");
        assertTrue(canAddDataverse);
        canAddDataset = JsonPath.from(getUserPermissionsOnDataverseResponse.body().asString()).getBoolean("data.canAddDataset");
        assertTrue(canAddDataset);
        canViewUnpublishedDataverse = JsonPath.from(getUserPermissionsOnDataverseResponse.body().asString()).getBoolean("data.canViewUnpublishedDataverse");
        assertFalse(canViewUnpublishedDataverse);
        canEditDataverse = JsonPath.from(getUserPermissionsOnDataverseResponse.body().asString()).getBoolean("data.canEditDataverse");
        assertFalse(canEditDataverse);
        canManageDataversePermissions = JsonPath.from(getUserPermissionsOnDataverseResponse.body().asString()).getBoolean("data.canManageDataversePermissions");
        assertFalse(canManageDataversePermissions);
        canPublishDataverse = JsonPath.from(getUserPermissionsOnDataverseResponse.body().asString()).getBoolean("data.canPublishDataverse");
        assertFalse(canPublishDataverse);
        canDeleteDataverse = JsonPath.from(getUserPermissionsOnDataverseResponse.body().asString()).getBoolean("data.canDeleteDataverse");
        assertFalse(canDeleteDataverse);

        // Call with invalid dataverse alias
        Response getUserPermissionsOnDataverseInvalidIdResponse = UtilIT.getUserPermissionsOnDataverse("testInvalidAlias", apiToken);
        getUserPermissionsOnDataverseInvalidIdResponse.then().assertThat().statusCode(NOT_FOUND.getStatusCode());
    }

    @Test
    public void testCreateFeaturedItem() {
        Response createUserResponse = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createUserResponse);
        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        String datasetPersistentId = UtilIT.getDatasetPersistentIdFromResponse(createDatasetResponse);
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);
        String pathToFile1 = "src/main/webapp/resources/images/cc0.png";
        Response uploadFileResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile1, apiToken);
        uploadFileResponse.prettyPrint();
        String datafileId = String.valueOf(UtilIT.getDataFileIdFromResponse(uploadFileResponse));
        assertTrue(UtilIT.sleepForLock(datasetId, "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration");

        // Publish Dataverse and Dataset with Datafile
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken).prettyPrint();
        UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken).prettyPrint();

        // Should not return any error when not passing a file

        Response createFeatureItemResponse = UtilIT.createDataverseFeaturedItem(dataverseAlias, apiToken, "test", 0, null);
        createFeatureItemResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.content", equalTo("test"))
                .body("data.type", equalTo("custom"))
                .body("data.imageFileName", equalTo(null))
                .body("data.displayOrder", equalTo(0));

        // Should not return any error when passing correct file and data

        String pathToTestFile = "src/test/resources/images/coffeeshop.png";
        createFeatureItemResponse = UtilIT.createDataverseFeaturedItem(dataverseAlias, apiToken, "test", 1, pathToTestFile);
        createFeatureItemResponse.then().assertThat()
                .body("data.content", equalTo("test"))
                .body("data.imageFileName", equalTo("coffeeshop.png"))
                .body("data.displayOrder", equalTo(1))
                .statusCode(OK.getStatusCode());

        // Should return bad request error when passing incorrect file type

        pathToTestFile = "src/test/resources/tab/test.tab";
        createFeatureItemResponse = UtilIT.createDataverseFeaturedItem(dataverseAlias, apiToken, "test", 0, pathToTestFile);
        createFeatureItemResponse.then().assertThat()
                .body("message", equalTo(BundleUtil.getStringFromBundle("dataverse.create.featuredItem.error.invalidFileType")))
                .statusCode(BAD_REQUEST.getStatusCode());

        // Should return unauthorized error when user has no permissions

        Response createRandomUser = UtilIT.createRandomUser();
        String randomUserApiToken = UtilIT.getApiTokenFromResponse(createRandomUser);
        createFeatureItemResponse = UtilIT.createDataverseFeaturedItem(dataverseAlias, randomUserApiToken, "test", 0, pathToTestFile);
        createFeatureItemResponse.then().assertThat().statusCode(UNAUTHORIZED.getStatusCode());

        // Should return not found error when dataverse does not exist

        createFeatureItemResponse = UtilIT.createDataverseFeaturedItem("thisDataverseDoesNotExist", apiToken, "test", 0, pathToTestFile);
        createFeatureItemResponse.then().assertThat()
                .body("message", equalTo("Can't find dataverse with identifier='thisDataverseDoesNotExist'"))
                .statusCode(NOT_FOUND.getStatusCode());

        // Testing new dvobject-type featured items
        createFeatureItemResponse = UtilIT.createDataverseFeaturedItem(dataverseAlias, apiToken, null, 10, null, "dataset", datasetPersistentId);
        createFeatureItemResponse.prettyPrint();
        createFeatureItemResponse = UtilIT.createDataverseFeaturedItem(dataverseAlias, apiToken, null, 11, null, "datafile", datafileId);
        createFeatureItemResponse.prettyPrint();
        Response listDataverseFeaturedItemsResponse = UtilIT.listDataverseFeaturedItems(dataverseAlias, apiToken);
        listDataverseFeaturedItemsResponse.prettyPrint();
        listDataverseFeaturedItemsResponse.then().assertThat()
                .body("data[2].dvObjectIdentifier", equalTo(datasetPersistentId))
                .body("data[2].type", equalTo("dataset"))
                .body("data[3].dvObjectIdentifier", equalTo(datafileId))
                .body("data[3].type", equalTo("datafile"));
    }

    @Test
    public void testListFeaturedItems() {
        Response createUserResponse = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createUserResponse);
        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        // Create test items

        List<Long> ids = Arrays.asList(0L, 0L, 0L);
        List<String> contents = Arrays.asList("Content 1", "Content 2", "Content 3");
        List<Integer> orders = Arrays.asList(2, 1, 0);
        List<Boolean> keepFiles = Arrays.asList(false, false, false);
        List<String> pathsToFiles = Arrays.asList("src/test/resources/images/coffeeshop.png", null, null);

        Response updateDataverseFeaturedItemsResponse = UtilIT.updateDataverseFeaturedItems(dataverseAlias, ids, contents, orders, keepFiles, pathsToFiles, apiToken);
        updateDataverseFeaturedItemsResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Items should be retrieved with all their properties and sorted by displayOrder

        Response listDataverseFeaturedItemsResponse = UtilIT.listDataverseFeaturedItems(dataverseAlias, apiToken);
        listDataverseFeaturedItemsResponse.prettyPrint();
        listDataverseFeaturedItemsResponse.then().assertThat()
                .body("data.size()", equalTo(3))
                .body("data[0].content", equalTo("Content 3"))
                .body("data[0].imageFileName", equalTo(null))
                .body("data[0].imageFileUrl", equalTo(null))
                .body("data[0].displayOrder", equalTo(0))
                .body("data[0].type", equalTo("custom"))
                .body("data[1].content", equalTo("Content 2"))
                .body("data[1].imageFileName", equalTo(null))
                .body("data[1].imageFileUrl", equalTo(null))
                .body("data[1].displayOrder", equalTo(1))
                .body("data[1].type", equalTo("custom"))
                .body("data[2].content", equalTo("Content 1"))
                .body("data[2].imageFileName", equalTo("coffeeshop.png"))
                .body("data[2].imageFileUrl", containsString("/api/access/dataverseFeaturedItemImage/"))
                .body("data[2].displayOrder", equalTo(2))
                .body("data[2].type", equalTo("custom"))
                .statusCode(OK.getStatusCode());

        // Should return not found error when dataverse does not exist

        listDataverseFeaturedItemsResponse = UtilIT.listDataverseFeaturedItems("thisDataverseDoesNotExist", apiToken);
        listDataverseFeaturedItemsResponse.then().assertThat()
                .body("message", equalTo("Can't find dataverse with identifier='thisDataverseDoesNotExist'"))
                .statusCode(NOT_FOUND.getStatusCode());

    }

    @Test
    public void testUpdateFeaturedItems() {
        Response createUserResponse = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createUserResponse);
        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        String baseUri = UtilIT.getRestAssuredBaseUri();
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken).prettyPrint();
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);
        String datasetPersistentId = UtilIT.getDatasetPersistentIdFromResponse(createDatasetResponse);
        UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken).prettyPrint();

        // Create new items

        List<Long> ids = Arrays.asList(0L, 0L, 0L);
        List<String> contents = Arrays.asList("Content 1", "Content 2", "Content 3");
        List<Integer> orders = Arrays.asList(0, 1, 2);
        List<Boolean> keepFiles = Arrays.asList(false, false, false);
        List<String> pathsToFiles = Arrays.asList("src/test/resources/images/coffeeshop.png", null, null);

        Response updateDataverseFeaturedItemsResponse = UtilIT.updateDataverseFeaturedItems(dataverseAlias, ids, contents, orders, keepFiles, pathsToFiles, apiToken);
        updateDataverseFeaturedItemsResponse.prettyPrint();
        updateDataverseFeaturedItemsResponse.then().assertThat()
                .body("data.size()", equalTo(3))
                .body("data[0].content", equalTo("Content 1"))
                .body("data[0].imageFileName", equalTo("coffeeshop.png"))
                .body("data[0].imageFileUrl", containsString("/api/access/dataverseFeaturedItemImage/"))
                .body("data[0].displayOrder", equalTo(0))
                .body("data[0].type", equalTo("custom"))
                .body("data[1].content", equalTo("Content 2"))
                .body("data[1].imageFileName", equalTo(null))
                .body("data[1].imageFileUrl", equalTo(null))
                .body("data[1].displayOrder", equalTo(1))
                .body("data[1].type", equalTo("custom"))
                .body("data[2].content", equalTo("Content 3"))
                .body("data[2].imageFileName", equalTo(null))
                .body("data[2].imageFileUrl", equalTo(null))
                .body("data[2].displayOrder", equalTo(2))
                .body("data[2].type", equalTo("custom"))
                .statusCode(OK.getStatusCode());

        Long firstItemId = JsonPath.from(updateDataverseFeaturedItemsResponse.body().asString()).getLong("data[0].id");
        Long secondItemId = JsonPath.from(updateDataverseFeaturedItemsResponse.body().asString()).getLong("data[1].id");
        Long thirdItemId = JsonPath.from(updateDataverseFeaturedItemsResponse.body().asString()).getLong("data[2].id");

        // Update first item (content, order, and keeping image), delete the rest and create new items

        ids = Arrays.asList(firstItemId, 0L, 0L);
        contents = Arrays.asList("Content 1 updated", "Content 2", "Content 3");
        orders = Arrays.asList(1, 0, 2);
        keepFiles = Arrays.asList(true, false, false);
        pathsToFiles = Arrays.asList(null, null, null);
        List<String> types = Arrays.asList("custom", "custom", "dataset");
        List<String> dvObjects = Arrays.asList("", "", String.valueOf(datasetId));

        updateDataverseFeaturedItemsResponse = UtilIT.updateDataverseFeaturedItems(dataverseAlias, ids, contents, orders, keepFiles, pathsToFiles, types, dvObjects, apiToken);
        updateDataverseFeaturedItemsResponse.prettyPrint();
        updateDataverseFeaturedItemsResponse.then().assertThat()
                .body("data.size()", equalTo(3))
                .body("data[0].content", equalTo("Content 2"))
                .body("data[0].imageFileName", equalTo(null))
                .body("data[0].imageFileUrl", equalTo(null))
                .body("data[0].displayOrder", equalTo(0))
                .body("data[0].type", equalTo("custom"))
                .body("data[1].content", equalTo("Content 1 updated"))
                .body("data[1].imageFileName", equalTo("coffeeshop.png"))
                .body("data[1].imageFileUrl", containsString("/api/access/dataverseFeaturedItemImage/"))
                .body("data[1].displayOrder", equalTo(1))
                .body("data[1].type", equalTo("custom"))
                .body("data[2].content", equalTo(null))
                .body("data[2].imageFileName", equalTo(null))
                .body("data[2].imageFileUrl", equalTo(null))
                .body("data[2].displayOrder", equalTo(2))
                .body("data[2].type", equalTo("dataset"))
                .body("data[2].dvObjectIdentifier", equalTo(datasetPersistentId))
                .statusCode(OK.getStatusCode());

        Long firstItemIdAfterUpdate = JsonPath.from(updateDataverseFeaturedItemsResponse.body().asString()).getLong("data[1].id");
        Long secondItemIdAfterUpdate = JsonPath.from(updateDataverseFeaturedItemsResponse.body().asString()).getLong("data[0].id");
        Long thirdItemIdAfterUpdate = JsonPath.from(updateDataverseFeaturedItemsResponse.body().asString()).getLong("data[2].id");

        assertEquals(firstItemId, firstItemIdAfterUpdate);
        assertNotEquals(secondItemId, secondItemIdAfterUpdate);
        assertNotEquals(thirdItemId, thirdItemIdAfterUpdate);

        // Update first item (removing image), update second item (adding image), delete the third item and create a new item

        ids = Arrays.asList(firstItemId, secondItemIdAfterUpdate, 0L);
        contents = Arrays.asList("Content 1 updated", "Content 2", "Content 3");
        orders = Arrays.asList(1, 0, 2);
        keepFiles = Arrays.asList(false, false, false);
        pathsToFiles = Arrays.asList(null, "src/test/resources/images/coffeeshop.png", null);

        updateDataverseFeaturedItemsResponse = UtilIT.updateDataverseFeaturedItems(dataverseAlias, ids, contents, orders, keepFiles, pathsToFiles, apiToken);
        updateDataverseFeaturedItemsResponse.then().assertThat()
                .body("data.size()", equalTo(3))
                .body("data[0].content", equalTo("Content 2"))
                .body("data[0].imageFileName", equalTo("coffeeshop.png"))
                .body("data[0].imageFileUrl", containsString("/api/access/dataverseFeaturedItemImage/"))
                .body("data[0].displayOrder", equalTo(0))
                .body("data[0].type", equalTo("custom"))
                .body("data[1].content", equalTo("Content 1 updated"))
                .body("data[1].imageFileName", equalTo(null))
                .body("data[1].imageFileUrl", equalTo(null))
                .body("data[1].displayOrder", equalTo(1))
                .body("data[1].type", equalTo("custom"))
                .body("data[2].content", equalTo("Content 3"))
                .body("data[2].imageFileName", equalTo(null))
                .body("data[2].imageFileUrl", equalTo(null))
                .body("data[2].displayOrder", equalTo(2))
                .body("data[2].type", equalTo("custom"))
                .statusCode(OK.getStatusCode());

        Long firstItemIdAfterSecondUpdate = JsonPath.from(updateDataverseFeaturedItemsResponse.body().asString()).getLong("data[1].id");
        Long secondItemIdAfterSecondUpdate = JsonPath.from(updateDataverseFeaturedItemsResponse.body().asString()).getLong("data[0].id");
        Long thirdItemIdAfterSecondUpdate = JsonPath.from(updateDataverseFeaturedItemsResponse.body().asString()).getLong("data[2].id");

        assertEquals(firstItemId, firstItemIdAfterSecondUpdate);
        assertEquals(secondItemIdAfterUpdate, secondItemIdAfterSecondUpdate);
        assertNotEquals(thirdItemIdAfterUpdate, thirdItemIdAfterSecondUpdate);

        // Only keep first featured item

        ids = List.of(firstItemId);
        contents = List.of("Content 1 updated");
        orders = List.of(0);
        keepFiles = List.of(false);
        pathsToFiles = null;

        updateDataverseFeaturedItemsResponse = UtilIT.updateDataverseFeaturedItems(dataverseAlias, ids, contents, orders, keepFiles, pathsToFiles, apiToken);
        updateDataverseFeaturedItemsResponse.then().assertThat()
                .body("data.size()", equalTo(1))
                .body("data[0].content", equalTo("Content 1 updated"))
                .body("data[0].imageFileName", equalTo(null))
                .body("data[0].imageFileUrl", equalTo(null))
                .body("data[0].displayOrder", equalTo(0))
                .body("data[0].type", equalTo("custom"))
                .statusCode(OK.getStatusCode());

        // Should return unauthorized error when user has no permissions

        Response createRandomUser = UtilIT.createRandomUser();
        String randomUserApiToken = UtilIT.getApiTokenFromResponse(createRandomUser);
        updateDataverseFeaturedItemsResponse = UtilIT.updateDataverseFeaturedItems(dataverseAlias, ids, contents, orders, keepFiles, pathsToFiles, randomUserApiToken);
        updateDataverseFeaturedItemsResponse.then().assertThat().statusCode(UNAUTHORIZED.getStatusCode());

        // Should return not found error when dataverse does not exist

        updateDataverseFeaturedItemsResponse = UtilIT.updateDataverseFeaturedItems("thisDataverseDoesNotExist", ids, contents, orders, keepFiles, pathsToFiles, apiToken);
        updateDataverseFeaturedItemsResponse.then().assertThat()
                .body("message", equalTo("Can't find dataverse with identifier='thisDataverseDoesNotExist'"))
                .statusCode(NOT_FOUND.getStatusCode());
    }

    @Test
    public void testDeleteFeaturedItemWithDvObject() {
        // test when featuring a datafile and the file is either deleted or restricted
        Response createUserResponse = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createUserResponse);
        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        String datasetPersistentId = UtilIT.getDatasetPersistentIdFromResponse(createDatasetResponse);
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);

        // Upload a file
        String pathToFile1 = "src/main/webapp/resources/images/cc0.png";
        Response uploadFileResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile1, apiToken);
        uploadFileResponse.prettyPrint();
        Integer datafileId = UtilIT.getDataFileIdFromResponse(uploadFileResponse);
        assertTrue(UtilIT.sleepForLock(datasetId.longValue(), "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + pathToFile1);

        // Publish the Dataverse and Dataset
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken).prettyPrint();
        UtilIT.publishDatasetViaNativeApi(datasetPersistentId, "major", apiToken).prettyPrint();

        Response createDataverseFeaturedItemResponse = UtilIT.createDataverseFeaturedItem(dataverseAlias, apiToken, null, 0, pathToFile1, "datafile", String.valueOf(datafileId));
        createDataverseFeaturedItemResponse.prettyPrint();
        int featuredItemId = UtilIT.getDatasetIdFromResponse(createDataverseFeaturedItemResponse);

        Response listFeaturedItemsResponse = UtilIT.listDataverseFeaturedItems(dataverseAlias, apiToken);
        listFeaturedItemsResponse.prettyPrint();
        listFeaturedItemsResponse.then()
                .body("data.size()", equalTo(1))
                .assertThat().statusCode(OK.getStatusCode());

        // delete the file creates a new DRAFT version of the Dataset but the File still exists in the latest published version
        UtilIT.deleteFile(datafileId,apiToken).prettyPrint();
        listFeaturedItemsResponse = UtilIT.listDataverseFeaturedItems(dataverseAlias, apiToken);
        listFeaturedItemsResponse.prettyPrint();
        listFeaturedItemsResponse.then()
                .body("data.size()", equalTo(1))
                .assertThat().statusCode(OK.getStatusCode());

        // publish the draft version with the file deleted will cause the featured item to be deleted
        UtilIT.publishDatasetViaNativeApi(datasetPersistentId, "major", apiToken).prettyPrint();
        listFeaturedItemsResponse = UtilIT.listDataverseFeaturedItems(dataverseAlias, apiToken);
        listFeaturedItemsResponse.prettyPrint();
        listFeaturedItemsResponse.then()
                .body("data.size()", equalTo(0))
                .assertThat().statusCode(OK.getStatusCode());

        // try to delete the featured item if it's already deleted should be NOT FOUND
        Response deleteItemResponse = UtilIT.deleteDataverseFeaturedItem(featuredItemId, apiToken);
        deleteItemResponse.prettyPrint();
        deleteItemResponse.then()
                .body("message", equalTo(BundleUtil.getStringFromBundle("dataverseFeaturedItems.errors.notFound", List.of(String.valueOf(featuredItemId)))))
                .assertThat().statusCode(NOT_FOUND.getStatusCode());
    }

    @Test
    public void testRestrictFeaturedItemWithDvObject() {
        // first create a superuser
        Response createResponse = UtilIT.createRandomUser();
        String adminApiToken = UtilIT.getApiTokenFromResponse(createResponse);
        String username = UtilIT.getUsernameFromResponse(createResponse);
        UtilIT.makeSuperUser(username);

        // Create the owner of the dataverse/dataset/datafile
        createResponse = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createResponse);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken).prettyPrint();

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);
        String datasetPersistentId = UtilIT.getDatasetPersistentIdFromResponse(createDatasetResponse);

        // Upload a file
        String pathToFile1 = "src/main/webapp/resources/images/cc0.png";
        Response uploadFileResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile1, apiToken);
        uploadFileResponse.prettyPrint();
        Integer datafileId = UtilIT.getDataFileIdFromResponse(uploadFileResponse);
        assertTrue(UtilIT.sleepForLock(datasetId.longValue(), "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + pathToFile1);

        UtilIT.publishDatasetViaNativeApi(datasetId, "major", apiToken);
        Response createDatafileResponse = UtilIT.createDataverseFeaturedItem(dataverseAlias, apiToken, null, 0, pathToFile1, "datafile", String.valueOf(datafileId));
        createDatafileResponse.prettyPrint();

        // test when featuring a datafile and the file is either deleted or restricted
        Response createUserResponse = UtilIT.createRandomUser();
        createUserResponse.prettyPrint();
        String userToken = UtilIT.getApiTokenFromResponse(createUserResponse);
        username = UtilIT.getUsernameFromResponse(createUserResponse);

        // Test restrict datafile
        UtilIT.restrictFile(String.valueOf(datafileId), true, apiToken);
        UtilIT.publishDatasetViaNativeApi(datasetId, "minor", apiToken);
        Response listFeaturedItemsResponse = UtilIT.listDataverseFeaturedItems(dataverseAlias, userToken);
        listFeaturedItemsResponse.prettyPrint();
        listFeaturedItemsResponse.then()
                .body("data.size()", equalTo(0))
                .assertThat().statusCode(OK.getStatusCode());

        // un-restrict
        UtilIT.restrictFile(String.valueOf(datafileId), false, apiToken);
        UtilIT.publishDatasetViaNativeApi(datasetId, "minor", apiToken);
        listFeaturedItemsResponse = UtilIT.listDataverseFeaturedItems(dataverseAlias, userToken);
        listFeaturedItemsResponse.prettyPrint();
        listFeaturedItemsResponse.then()
                .body("data.size()", equalTo(0))
                .assertThat().statusCode(OK.getStatusCode());

        // Test deaccessioned dataset.
        createDatasetResponse = UtilIT.createDataverseFeaturedItem(dataverseAlias, apiToken, null, 0, pathToFile1, "dataset", String.valueOf(datasetId));
        createDatasetResponse.prettyPrint();
        listFeaturedItemsResponse = UtilIT.listDataverseFeaturedItems(dataverseAlias, userToken);
        listFeaturedItemsResponse.prettyPrint();
        listFeaturedItemsResponse.then()
                .body("data.size()", equalTo(1))
                .assertThat().statusCode(OK.getStatusCode());

        for (int i=0; i < 3; i++) { // deaccession all versions
            Response datasetResponse = UtilIT.deaccessionDataset(datasetId, "1." + i, "Test reason", null, apiToken);
            datasetResponse.prettyPrint();
            datasetResponse.then()
                    .assertThat().statusCode(OK.getStatusCode());
        }

        // All featuredItems are now gone
        listFeaturedItemsResponse = UtilIT.listDataverseFeaturedItems(dataverseAlias, userToken);
        listFeaturedItemsResponse.prettyPrint();
        listFeaturedItemsResponse.then()
                .body("data.size()", equalTo(0))
                .assertThat().statusCode(OK.getStatusCode());
    }

    @Test
    public void testDeleteFeaturedItems() {
        Response createUserResponse = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createUserResponse);
        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        // Create test featured items

        List<Long> ids = Arrays.asList(0L, 0L, 0L);
        List<String> contents = Arrays.asList("Content 1", "Content 2", "Content 3");
        List<Integer> orders = Arrays.asList(0, 1, 2);
        List<Boolean> keepFiles = Arrays.asList(false, false, false);
        List<String> pathsToFiles = Arrays.asList("src/test/resources/images/coffeeshop.png", null, null);

        Response updateDataverseFeaturedItemsResponse = UtilIT.updateDataverseFeaturedItems(dataverseAlias, ids, contents, orders, keepFiles, pathsToFiles, apiToken);
        updateDataverseFeaturedItemsResponse.then().assertThat()
                .body("data.size()", equalTo(3))
                .statusCode(OK.getStatusCode());

        // Check that the featured items are successfully deleted when calling the delete endpoint

        Response deleteDataverseFeaturedItemsResponse = UtilIT.deleteDataverseFeaturedItems(dataverseAlias, apiToken);
        deleteDataverseFeaturedItemsResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response listFeaturedItemsResponse = UtilIT.listDataverseFeaturedItems(dataverseAlias, apiToken);
        listFeaturedItemsResponse.then()
                .body("data.size()", equalTo(0))
                .assertThat().statusCode(OK.getStatusCode());

        // Should return unauthorized error when user has no permissions

        Response createRandomUser = UtilIT.createRandomUser();
        String randomUserApiToken = UtilIT.getApiTokenFromResponse(createRandomUser);
        deleteDataverseFeaturedItemsResponse = UtilIT.deleteDataverseFeaturedItems(dataverseAlias, randomUserApiToken);
        deleteDataverseFeaturedItemsResponse.then().assertThat().statusCode(UNAUTHORIZED.getStatusCode());

        // Should return not found error when dataverse does not exist

        deleteDataverseFeaturedItemsResponse = UtilIT.deleteDataverseFeaturedItems("thisDataverseDoesNotExist", apiToken);
        deleteDataverseFeaturedItemsResponse.then().assertThat()
                .body("message", equalTo("Can't find dataverse with identifier='thisDataverseDoesNotExist'"))
                .statusCode(NOT_FOUND.getStatusCode());
    }

    @Test
    public void testUpdateInputLevelDisplayOnCreate() {
        Response createUserResponse = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createUserResponse);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        // Configure metadata blocks - disable inherit from root and set specific blocks
        Response setMetadataBlocksResponse = UtilIT.setMetadataBlocks(
                dataverseAlias, 
                Json.createArrayBuilder().add("socialscience"), 
                apiToken);
        setMetadataBlocksResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Verify initial state
        Response listMetadataBlocks = UtilIT.listMetadataBlocks(dataverseAlias, false, true, apiToken);
        listMetadataBlocks.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.size()", equalTo(1))
                .body("data[0].name", equalTo("socialscience"));

        // Update displayOnCreate for a field
        Response updateResponse = UtilIT.updateDataverseInputLevelDisplayOnCreate(
            dataverseAlias, "unitOfAnalysis", true, apiToken);
        updateResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.inputLevels[0].displayOnCreate", equalTo(true))
                .body("data.inputLevels[0].datasetFieldTypeName", equalTo("unitOfAnalysis"));
        
        // Update an inputlevel w/o displayOnCreate set
        Response updateResponse2 = UtilIT.updateDataverseInputLevelDisplayOnCreate(
            dataverseAlias, "unitOfAnalysis", null, apiToken);
        updateResponse2.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.inputLevels[0]", not(hasKey("displayOnCreate")))
                .body("data.inputLevels[0].datasetFieldTypeName", equalTo("unitOfAnalysis"));
    }
    
    @Test
    public void testUpdateInputLevelDisplayOnCreateOverride() {
        Response createUserResponse = UtilIT.createRandomUser();
        String apiToken = UtilIT.getApiTokenFromResponse(createUserResponse);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.then().assertThat().statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        // Configure metadata blocks - disable inherit from root and set specific blocks
        Response setMetadataBlocksResponse = UtilIT.setMetadataBlocks(
                dataverseAlias, 
                Json.createArrayBuilder().add("citation"), 
                apiToken);
        setMetadataBlocksResponse.then().assertThat()
                .statusCode(OK.getStatusCode());


        Response listMetadataBlocks = UtilIT.listMetadataBlocks(dataverseAlias, false, true, apiToken);
        listMetadataBlocks.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.size()", equalTo(1))
                .body("data[0].name", equalTo("citation"));


        // Update displayOnCreate for a field
      
               
        Response updateResponse = UtilIT.updateDataverseInputLevelDisplayOnCreate(
            dataverseAlias, "notesText", false, apiToken);
        updateResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.inputLevels[0].displayOnCreate", equalTo(false))
                .body("data.inputLevels[0].datasetFieldTypeName", equalTo("notesText"));  
        
        Response listMetadataBlocksResponse = UtilIT.listMetadataBlocks(dataverseAlias, true, true, apiToken);
        listMetadataBlocksResponse.prettyPrint();
         int expectedNumberOfMetadataFields = 9; // 28 - 18 - notes child duplicates
        int  expectedOnlyDisplayedOnCreateNumberOfMetadataBlocks = 1;
        listMetadataBlocksResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].fields", not(equalTo(null)))
                .body("data[0].fields.size()", equalTo(expectedNumberOfMetadataFields))
                .body("data[0].displayName", equalTo("Citation Metadata"))
                .body("data.size()", equalTo(expectedOnlyDisplayedOnCreateNumberOfMetadataBlocks))
                .body("data[0].fields.author.childFields.size()", is(4));
        //set it back just in case
        updateResponse = UtilIT.updateDataverseInputLevelDisplayOnCreate(
            dataverseAlias, "notesText", true, apiToken);
        updateResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.inputLevels[0].displayOnCreate", equalTo(true))
                .body("data.inputLevels[0].datasetFieldTypeName", equalTo("notesText"));
        
        updateResponse = UtilIT.updateDataverseInputLevelDisplayOnCreate(
            dataverseAlias, "subtitle", true, apiToken);
        updateResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.inputLevels[0].displayOnCreate", equalTo(true))
                .body("data.inputLevels[0].datasetFieldTypeName", equalTo("subtitle"));
        
        listMetadataBlocksResponse = UtilIT.listMetadataBlocks(dataverseAlias, true, true, apiToken);
        listMetadataBlocksResponse.prettyPrint();
        expectedNumberOfMetadataFields = 11; // 28 - 18 + subtitle child duplicates
        expectedOnlyDisplayedOnCreateNumberOfMetadataBlocks = 1;
        listMetadataBlocksResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].fields", not(equalTo(null)))
                .body("data[0].fields.size()", equalTo(expectedNumberOfMetadataFields))
                .body("data[0].displayName", equalTo("Citation Metadata"))
                .body("data.size()", equalTo(expectedOnlyDisplayedOnCreateNumberOfMetadataBlocks))
                .body("data[0].fields.author.childFields.size()", is(4));
        
        updateResponse = UtilIT.updateDataverseInputLevelDisplayOnCreate(
            dataverseAlias, "subtitle", false, apiToken);
        updateResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.inputLevels[0].displayOnCreate", equalTo(false))
                .body("data.inputLevels[0].datasetFieldTypeName", equalTo("subtitle"));
        
    }
}
