package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import edu.harvard.iq.dataverse.Dataverse;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response.Status;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import org.junit.BeforeClass;
import org.junit.Test;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import org.hamcrest.CoreMatchers;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import org.junit.BeforeClass;
import org.junit.Test;

public class DataversesIT {

    private static final Logger logger = Logger.getLogger(DataversesIT.class.getCanonicalName());

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testAttemptToCreateDuplicateAlias() throws Exception {

        Response createUserResponse = UtilIT.createRandomUser();
//        createUserResponse.prettyPrint();
        createUserResponse.then().assertThat().statusCode(OK.getStatusCode());

        String apiToken = UtilIT.getApiTokenFromResponse(createUserResponse);
        String username = UtilIT.getUsernameFromResponse(createUserResponse);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        if (createDataverseResponse.getStatusCode() != 201) {
            // purposefully using println here to the error shows under "Test Results" in Netbeans
            System.out.println("A workspace for testing (a dataverse) couldn't be created in the root dataverse. The output was:\n\n" + createDataverseResponse.body().asString());
            System.out.println("\nPlease ensure that users can created dataverses in the root in order for this test to run.");
        } else {
            createDataverseResponse.prettyPrint();
        }
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias1 = UtilIT.getAliasFromResponse(createDataverseResponse);
        String dataverseAlias2 = dataverseAlias1.toUpperCase();
        logger.info("Attempting to creating dataverse with alias '" + dataverseAlias2 + "' (uppercase version of existing '" + dataverseAlias1 + "' dataverse, should fail)...");
        String category = null;
        Response attemptToCreateDataverseWithDuplicateAlias = UtilIT.createDataverse(dataverseAlias2, category, apiToken);
        attemptToCreateDataverseWithDuplicateAlias.prettyPrint();
        attemptToCreateDataverseWithDuplicateAlias.then().assertThat()
                .statusCode(BAD_REQUEST.getStatusCode());

        logger.info("Deleting dataverse " + dataverseAlias1);
        Response deleteDataverse1Response = UtilIT.deleteDataverse(dataverseAlias1, apiToken);
        deleteDataverse1Response.prettyPrint();
        deleteDataverse1Response.then().assertThat().statusCode(OK.getStatusCode());

        logger.info("Checking response code for attempting to delete a non-existent dataverse.");
        Response attemptToDeleteDataverseThatShouldNotHaveBeenCreated = UtilIT.deleteDataverse(dataverseAlias2, apiToken);
        attemptToDeleteDataverseThatShouldNotHaveBeenCreated.prettyPrint();
        attemptToDeleteDataverseThatShouldNotHaveBeenCreated.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

        Response deleteUser1Response = UtilIT.deleteUser(username);
        deleteUser1Response.prettyPrint();
        deleteUser1Response.then().assertThat().statusCode(OK.getStatusCode());
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

        String alias1 = UtilIT.getRandomIdentifier();
        String category1 = Dataverse.DataverseType.LABORATORY.toString();
        Response createDataverseWithCategory = UtilIT.createDataverse(alias1, category1, apiToken);
        createDataverseWithCategory.prettyPrint();
        createDataverseWithCategory.then().assertThat()
                .body("data.dataverseType", equalTo("LABORATORY"))
                .statusCode(Status.CREATED.getStatusCode());

        String alias2 = UtilIT.getRandomIdentifier();
        String madeUpCategory = "madeUpCategory";
        Response createDataverseWithInvalidCategory = UtilIT.createDataverse(alias2, madeUpCategory, apiToken);
        createDataverseWithInvalidCategory.prettyPrint();
        createDataverseWithInvalidCategory.then().assertThat()
                .body("data.dataverseType", equalTo("UNCATEGORIZED"))
                .statusCode(Status.CREATED.getStatusCode());

        String alias3 = UtilIT.getRandomIdentifier();
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
    public void testCreateDatasets() {

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        // create test dataverse
        Response createDataverse1Response = UtilIT.createRandomDataverse(apiToken);
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverse1Response);

        /**
         * @todo test this: 1. creating a dataset as non super user does not
         * grant dv defined access perms to creator, ie they have no direct
         * perms on the dataset the just created, does not see draft card. This
         * is most obvious in root dv where the user is likely not an admin.
         */
        // this test is for the following bug reports:
        // 2. non super users can specify a doi when creating a dataset, this should be super user only.
        // 3. importType modes, including default, appear to do nothing: can specify doi in all, sets globalidcreatetime in all, not just migration.
//        Response attemptSetDoiAsAsNonSuperuserUsingNew = UtilIT.createDatasetWithGlobalIdViaNativeApi(dataverseAlias, apiToken,
//                UtilIT.getJson("scripts/search/tests/data/dataset-finch2.json"), "NEW");
//        attemptSetDoiAsAsNonSuperuserUsingNew.prettyPrint();
//        attemptSetDoiAsAsNonSuperuserUsingNew.then().assertThat()
//                .body("message", equalTo("Attempting to migrate or harvest datasets via this API endpoint is experimental "
//                        + "and requires a superuser's API token. Migrating datasets using DDI (XML) is better supported "
//                        + "(versions are handled, files are handled, etc.)."))
//                .statusCode(UNAUTHORIZED.getStatusCode());
        // bug 3. importType modes, including default, appear to do nothing: can specify doi in all, sets globalidcreatetime in all, not just migration.
        Response attemptSetDoiAsAsNonSuperuserWithoutSpecifyingImportType = UtilIT.createDatasetWithGlobalIdViaNativeApi(dataverseAlias, apiToken,
                UtilIT.getJson("scripts/search/tests/data/dataset-finch2.json"), "");
        attemptSetDoiAsAsNonSuperuserWithoutSpecifyingImportType.prettyPrint();
        attemptSetDoiAsAsNonSuperuserWithoutSpecifyingImportType.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        int datasetId0 = UtilIT.getDatasetIdFromResponse(attemptSetDoiAsAsNonSuperuserWithoutSpecifyingImportType);
        Response datasetShouldHaveRandomIdentifierRatherThan8888 = UtilIT.nativeGet(datasetId0, apiToken);
        datasetShouldHaveRandomIdentifierRatherThan8888.prettyPrint();
        datasetShouldHaveRandomIdentifierRatherThan8888.then().assertThat()
                .body("data.identifier", not(equalTo("8888")))
                .statusCode(OK.getStatusCode());
        System.out.println("dataverse alias: " + dataverseAlias);

        Response attemptJsonMigrationAsNonSuperuser = UtilIT.createDatasetWithGlobalIdViaNativeApi(dataverseAlias, apiToken,
                UtilIT.getJson("scripts/search/tests/data/dataset-finch2.json"), "MIGRATION");
        attemptJsonMigrationAsNonSuperuser.prettyPrint();
        attemptJsonMigrationAsNonSuperuser.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode());

        UtilIT.makeSuperUser(username).then().assertThat().statusCode(OK.getStatusCode());

        // migrate dataset with global ID
        Response createDataset1Response = UtilIT.createDatasetWithGlobalIdViaNativeApi(dataverseAlias, apiToken,
                UtilIT.getJson("scripts/search/tests/data/dataset-finch2.json"), "MIGRATION");
        createDataset1Response.prettyPrint();
        createDataset1Response.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        int datasetId1 = UtilIT.getDatasetIdFromResponse(createDataset1Response);

        Response publishDataverse = UtilIT.publishDataverseViaSword(dataverseAlias, apiToken);
        publishDataverse.prettyPrint();
        publishDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response publishDatasetWithExistingGlobalId = UtilIT.publishDatasetViaNativeApi(datasetId1, "major", apiToken);
        publishDatasetWithExistingGlobalId.prettyPrint();
        publishDatasetWithExistingGlobalId.then().assertThat()
                .statusCode(OK.getStatusCode());

        // attempt to migrate dataset with duplicate global ID
        Response createDataset2Response = UtilIT.createDatasetWithGlobalIdViaNativeApi(dataverseAlias, apiToken,
                UtilIT.getJson("scripts/search/tests/data/dataset-finch2.json"), "MIGRATION");
        createDataset2Response.prettyPrint();
        createDataset2Response.then().assertThat()
                .body("message", equalTo("A dataset with the identifier doi:10.15785/SBGRID/8888 already exists."))
                .statusCode(BAD_REQUEST.getStatusCode());

        // attempt to migrate dataset missing global ID param: authority
        Response createDataset3Response = UtilIT.createDatasetWithGlobalIdViaNativeApi(dataverseAlias, apiToken,
                UtilIT.getJson("scripts/search/tests/data/dataset-finch3.json"), "MIGRATION");
        createDataset3Response.prettyPrint();
        createDataset3Response.then().assertThat()
                .body("message", equalTo("Protocol, authority and identifier must all be specified when migrating a dataset with an existing global identifier."))
                .statusCode(BAD_REQUEST.getStatusCode());

        // migrate dataset with no import type specified (should be reset to MIGRATION since all global ID params are present)
        Response createDataset4Response = UtilIT.createDatasetWithGlobalIdViaNativeApi(dataverseAlias, apiToken,
                UtilIT.getJson("scripts/search/tests/data/dataset-finch4.json"), null);
        createDataset4Response.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        int datasetId2 = UtilIT.getDatasetIdFromResponse(createDataset4Response);

        // normal dataset creation (NEW, no existing DOI to migrate)
        Response createDataset5Response = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset5Response.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        int datasetId3 = UtilIT.getDatasetIdFromResponse(createDataset5Response);

        // TODO: test this:  5. the separator query parameter does not seem to set the doiseparator value in the dataset table.
        // revert to non-superuser to make sure a non-superuser can't delete a published dataset
        UtilIT.makeSuperUser(username).then().assertThat().statusCode(OK.getStatusCode());

        Response deleteDataset1Response = UtilIT.deleteDatasetViaNativeApi(datasetId1, apiToken);
        deleteDataset1Response.prettyPrint();
        deleteDataset1Response.then().assertThat()
                .body("message", equalTo("Failed to delete dataset " + datasetId1 + " User @" + username + " is not permitted to perform requested action."))
                .statusCode(UNAUTHORIZED.getStatusCode());

        // only a superuser can destroy a published dataset
        UtilIT.makeSuperUser(username).then().assertThat().statusCode(OK.getStatusCode());

        Response deleteDataset0Response = UtilIT.deleteDatasetViaNativeApi(datasetId0, apiToken);
        deleteDataset0Response.prettyPrint();
        deleteDataset0Response.then().assertThat().statusCode(OK.getStatusCode());

        Response destroyDataset1WhichHasBeenPublished = UtilIT.destroyDataset(datasetId1, apiToken);
        destroyDataset1WhichHasBeenPublished.prettyPrint();
        destroyDataset1WhichHasBeenPublished.then().assertThat().statusCode(OK.getStatusCode());

        Response deleteDataset2Response = UtilIT.deleteDatasetViaNativeApi(datasetId2, apiToken);
        deleteDataset2Response.prettyPrint();
        deleteDataset2Response.then().assertThat().statusCode(OK.getStatusCode());

        Response deleteDataset3Response = UtilIT.deleteDatasetViaNativeApi(datasetId3, apiToken);
        deleteDataset3Response.prettyPrint();
        deleteDataset3Response.then().assertThat().statusCode(OK.getStatusCode());

        Response deleteDataverse1Response = UtilIT.deleteDataverse(dataverseAlias, apiToken);
        deleteDataverse1Response.prettyPrint();
        deleteDataverse1Response.then().assertThat().statusCode(OK.getStatusCode());

        Response deleteUser1Response = UtilIT.deleteUser(username);
        deleteUser1Response.prettyPrint();
        deleteUser1Response.then().assertThat().statusCode(OK.getStatusCode());
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

}
