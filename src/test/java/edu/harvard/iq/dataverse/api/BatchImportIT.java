package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import java.io.File;
import io.restassured.response.Response;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static jakarta.ws.rs.core.Response.Status.ACCEPTED;
import static jakarta.ws.rs.core.Response.Status.OK;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import org.hamcrest.CoreMatchers;

public class BatchImportIT {

    private static final Logger logger = Logger.getLogger(BatchImportIT.class.getCanonicalName());

    private static final String importDirectoryAndDataverseAliasMustMatch = "batchImportDv";

    public BatchImportIT() {
    }

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    /**
     * This test is called "roundTripDdi" because the original idea was to start
     * with DDI (XML) as input to create a dataset (via the DVN 3 migration
     * method) and then export the dataset as DDI to see if the DDI matches.
     *
     * In practice, the DDI is slightly different but at least DDI comes out.
     * TODO: Make the DDI going in and coming out the same?
     *
     * Note that to run this test more than once the "destroy" of the dataset
     * must succeed. If you need to destroy the dataset manually, run something
     * like this:
     *
     * curl -H 'X-Dataverse-key: cd883738-34fa-4101-a3ae-47ffbb30d041' -X DELETE
     * http://localhost:8080/api/datasets/:persistentId/destroy?persistentId=hdl:1902.1/00012
     *
     */
    @Test
    public void roundTripDdi() throws Exception {
        Response createUserResponse = UtilIT.createRandomUser();
        createUserResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        JsonPath createdUser1 = JsonPath.from(createUserResponse.body().asString());
        String apiToken1 = createdUser1.getString("data.apiToken");
        String username1 = createdUser1.getString("data.user.userName");

        Response makeSuperuserResponse = UtilIT.makeSuperUser(username1);
        makeSuperuserResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken1);
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        String directoryPath = "scripts/issues/907/" + importDirectoryAndDataverseAliasMustMatch;
        String absoluteDirectoryPath = new File(directoryPath).getAbsolutePath();

        String parentDataverse = dataverseAlias;
        Response migrateResponse = UtilIT.migrateDataset(absoluteDirectoryPath, parentDataverse, apiToken1);
        migrateResponse.prettyPrint();
        migrateResponse.then().assertThat()
                .body("status", CoreMatchers.equalTo("WORKFLOW_IN_PROGRESS"))
                .statusCode(ACCEPTED.getStatusCode());

        Thread.sleep(1500);

        Response dvContents = UtilIT.showDataverseContents(dataverseAlias, apiToken1);
        dvContents.prettyPrint();
        dvContents.then().assertThat()
                .statusCode(OK.getStatusCode());
        int datasetId = JsonPath.from(dvContents.body().asString()).getInt("data[0].id");
        String protocol = JsonPath.from(dvContents.body().asString()).getString("data[0].protocol");
        String authority = JsonPath.from(dvContents.body().asString()).getString("data[0].authority");
        String identifier = JsonPath.from(dvContents.body().asString()).getString("data[0].identifier");
        String datasetPid = protocol + ":" + authority + "/" + identifier;

        Response exportDatasetAsDdi = UtilIT.exportDataset(datasetPid, "ddi", apiToken1);
        exportDatasetAsDdi.prettyPrint();
        exportDatasetAsDdi.then().assertThat()
                .body("codeBook.docDscr.citation.titlStmt.titl", CoreMatchers.equalTo("Black Professional Women, 1969"))
                .body("codeBook.docDscr.citation.titlStmt.IDNo", CoreMatchers.equalTo(datasetPid))
                .statusCode(OK.getStatusCode());

        Response destroyDatasetResponse = UtilIT.destroyDataset(datasetId, apiToken1);
        destroyDatasetResponse.prettyPrint();
        destroyDatasetResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response deleteDataverseResponse = UtilIT.deleteDataverse(dataverseAlias, apiToken1);
        deleteDataverseResponse.prettyPrint();
        deleteDataverseResponse.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response deleteUser1Response = UtilIT.deleteUser(username1);
        deleteUser1Response.prettyPrint();
        deleteUser1Response.then().assertThat()
                .statusCode(OK.getStatusCode());

    }

}
