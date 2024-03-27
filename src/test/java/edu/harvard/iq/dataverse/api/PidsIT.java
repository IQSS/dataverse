package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.OK;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * In order to execute this test code you must be configured with DataCite
 * credentials.
 */
public class PidsIT {

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Disabled
    @Test
    public void testGetPid() {
        String pid = "";
        pid = "doi:10.70122/FK2/9BXT5O"; // findable
        pid = "doi:10.70122/FK2/DNEUDP"; // draft

        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        UtilIT.makeSuperUser(username).then().assertThat().statusCode(OK.getStatusCode());

        Response getPid = UtilIT.getPid(pid, apiToken);
        getPid.prettyPrint();
    }

    @Test
    public void testGetUnreservedPids() {
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        UtilIT.makeSuperUser(username).then().assertThat().statusCode(OK.getStatusCode());

        Response unreserved = UtilIT.getUnreservedPids(apiToken);
        unreserved.prettyPrint();
    }

//    @Ignore
    @Test
    public void testReservePid() {
        String pid = "";
        pid = "doi:10.70122/FK2/OQ7LN4"; // 2086
        pid = "2086";
        pid = "4"; // doi:10.5072/FK2/HSS61L (id 4) changed to doi:10.70122/FK2/HSS61L
        pid = "6"; // doi:10.5072/FK2/ASDBZL (id 6) changed to doi:10.70122/FK2/ASDBZL
        pid = "19"; // doi:10.5072/FK2/0UQQS1 (id 19) changed to doi:10.70122/FK2/0UQQS1
        pid = "doi:10.70122/FK2/BPJ78V"; // 34
        pid = "doi:10.70122/FK2/EXMBWD"; // 39
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        UtilIT.makeSuperUser(username).then().assertThat().statusCode(OK.getStatusCode());

        Response reservePid = UtilIT.reservePid(pid, apiToken);
        reservePid.prettyPrint();
        // These are some errors seen along the way:
        /**
         * "message": "javax.ejb.EJBTransactionRolledbackException: Exception
         * thrown from bean: javax.ejb.EJBTransactionRolledbackException:
         * Exception thrown from bean: java.lang.RuntimeException: Response
         * code: 400, can't write unknown attribute `repository_id`"
         */
        /**
         * "message": "result: Problem calling createIdentifier:
         * javax.ejb.EJBTransactionRolledbackException: Exception thrown from
         * bean: javax.ejb.EJBTransactionRolledbackException: Exception thrown
         * from bean: java.lang.RuntimeException: Response code: 403, Access is
         * denied"
         */
    }

    @Disabled
    @Test
    public void testDeletePid() {
        String pid = "";
        pid = "doi:10.70122/FK2/UA98UD";
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        UtilIT.makeSuperUser(username).then().assertThat().statusCode(OK.getStatusCode());

        Response deletePid = UtilIT.deletePid(pid, apiToken);
        deletePid.prettyPrint();
    }

    @Disabled
    @Test
    public void testCannotPublishUntilReserved() {
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        UtilIT.makeSuperUser(username).then().assertThat().statusCode(OK.getStatusCode());

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        JsonPath createdDataset = JsonPath.from(createDataset.body().asString());
        String pid = createdDataset.getString("data.persistentId");

        Response deletePid = UtilIT.deletePid(pid, apiToken);
        deletePid.prettyPrint();

        Response publishDataverse = UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken);
        publishDataverse.prettyPrint();
        publishDataverse.then().assertThat()
                .statusCode(OK.getStatusCode());

        // Publishing fails because the PID is not reserved (see deletePid above).
        Response publishDataset = UtilIT.publishDatasetViaNativeApi(pid, "major", apiToken);
        publishDataset.prettyPrint();
        publishDataset.then().assertThat()
                .statusCode(FORBIDDEN.getStatusCode());
    }

    @Disabled
    @Test
    public void testDeleteDraftPidOnDelete() {
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        UtilIT.makeSuperUser(username).then().assertThat().statusCode(OK.getStatusCode());

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        JsonPath createdDataset = JsonPath.from(createDataset.body().asString());
        String pid = createdDataset.getString("data.persistentId");
        int datasetId = createdDataset.getInt("data.id");

        Response getPidBeforeDelete = UtilIT.getPid(pid, apiToken);
        getPidBeforeDelete.prettyPrint(); // draft

        Response deleteDataset = UtilIT.deleteDatasetViaNativeApi(datasetId, apiToken);
        deleteDataset.prettyPrint();
        deleteDataset.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response getPidAfterDelete = UtilIT.getPid(pid, apiToken);
        getPidAfterDelete.prettyPrint(); // 404
    }

}
