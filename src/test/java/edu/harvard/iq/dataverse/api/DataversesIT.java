package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.path.json.JsonPath.with;
import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import javax.ws.rs.core.Response.Status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;
import static junit.framework.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.AfterClass;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.nio.file.Files;
import com.jayway.restassured.path.json.JsonPath;

public class DataversesIT {

    private static final Logger logger = Logger.getLogger(DataversesIT.class.getCanonicalName());

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }
    
    @AfterClass
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
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        JsonObject dvJson;
        FileReader reader = new FileReader("doc/sphinx-guides/source/_static/api/dataverse-minimal.json");
        dvJson = Json.createReader(reader).readObject();
        Response create = UtilIT.createDataverse(dvJson, apiToken);
        create.prettyPrint();
        create.then().assertThat().statusCode(CREATED.getStatusCode());
        Response deleteDataverse = UtilIT.deleteDataverse("science", apiToken);
        deleteDataverse.prettyPrint();
        deleteDataverse.then().assertThat().statusCode(OK.getStatusCode());
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
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        Integer dataverseId = UtilIT.getDataverseIdFromResponse(createDataverseResponse);
        
        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);//.publishDataverseViaSword(dataverseAlias, apiToken);
        assertEquals(200, publishDataverse.getStatusCode());
        
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
                        Thread.sleep(2000);
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

        Response importDDIPid = UtilIT.importDatasetDDIViaNativeApi(apiToken, dataverseAlias, xml,  "doi:10.5072/FK2/ABCD11", "no");
        logger.info(importDDIPid.prettyPrint());
        assertEquals(201, importDDIPid.getStatusCode());

        Response importDDIPidRel = UtilIT.importDatasetDDIViaNativeApi(apiToken, dataverseAlias, xml,  "doi:10.5072/FK2/ABCD22", "yes");
        logger.info(importDDIPidRel.prettyPrint());
        assertEquals(201, importDDIPidRel.getStatusCode());


        Response importDDIRelease = UtilIT.importDatasetDDIViaNativeApi(apiToken, dataverseAlias, xml, null, "yes");
        logger.info( importDDIRelease.prettyPrint());
        assertEquals(201, importDDIRelease.getStatusCode());

        //cleanup

        Integer datasetIdInt = JsonPath.from(importDDI.body().asString()).getInt("data.id");
        Response destroyDatasetResponse = UtilIT.destroyDataset(datasetIdInt, apiToken);
        assertEquals(200, destroyDatasetResponse.getStatusCode());

        Integer datasetIdIntPid = JsonPath.from(importDDIPid.body().asString()).getInt("data.id");
        Response destroyDatasetResponsePid = UtilIT.destroyDataset(datasetIdIntPid, apiToken);
        assertEquals(200, destroyDatasetResponsePid.getStatusCode());

        Integer datasetIdIntPidRel = JsonPath.from(importDDIPidRel.body().asString()).getInt("data.id");
        Response destroyDatasetResponsePidRel = UtilIT.destroyDataset(datasetIdIntPidRel, apiToken);
        assertEquals(200, destroyDatasetResponsePidRel.getStatusCode());

        // This last dataset we have just imported, let's give it a sec. to finish indexing (?)
        // or whatever it is that may still be happening. (Have been seeing intermittent 500 from the next
        // destroyDataset() line lately)
        
        Thread.sleep(1000L); 
        
        Integer datasetIdIntRelease = JsonPath.from(importDDIRelease.body().asString()).getInt("data.id");
        Response destroyDatasetResponseRelease = UtilIT.destroyDataset(datasetIdIntRelease, apiToken);
        assertEquals(200, destroyDatasetResponseRelease.getStatusCode());

        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        assertEquals(200, deleteDataverseResponse.getStatusCode());

        Response deleteUserResponse = UtilIT.deleteUser(username);
        assertEquals(200, deleteUserResponse.getStatusCode());
    }
    
}
