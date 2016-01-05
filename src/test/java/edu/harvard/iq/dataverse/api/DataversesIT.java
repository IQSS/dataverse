package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import java.util.logging.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static junit.framework.Assert.assertEquals;

public class DataversesIT {

    private static final Logger logger = Logger.getLogger(DataversesIT.class.getCanonicalName());

    private static String username1;
    private static String apiToken1;
    private static String dataverseAlias1;
    private static String dataverseAlias2;

    @BeforeClass
    public static void setUpClass() {

        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

        Response createUserResponse = UtilIT.createRandomUser();
//        createUserResponse.prettyPrint();
        assertEquals(200, createUserResponse.getStatusCode());

        apiToken1 = UtilIT.getApiTokenFromResponse(createUserResponse);
        username1 = UtilIT.getUsernameFromResponse(createUserResponse);

    }

    @Test
    public void testAttemptToCreateDuplicateAlias() throws Exception {
        Response createDataverse1Response = UtilIT.createRandomDataverse(apiToken1);
        createDataverse1Response.prettyPrint();
        dataverseAlias1 = UtilIT.getAliasFromResponse(createDataverse1Response);
        dataverseAlias2 = dataverseAlias1.toUpperCase();
        logger.info("Attempting to creating dataverse with alias '" + dataverseAlias2 + "' (uppercase version of existing '" + dataverseAlias1 + "' dataverse, should fail)...");
        Response createDataverse2Response = UtilIT.createDataverse(dataverseAlias2, apiToken1);
        createDataverse2Response.prettyPrint();
        assertEquals(400, createDataverse2Response.getStatusCode());
    }

    @AfterClass
    public static void tearDownClass() {
        boolean disabled = false;

        if (disabled) {
            return;
        }

        Response deleteDataverse1Response = UtilIT.deleteDataverse(dataverseAlias1, apiToken1);
        deleteDataverse1Response.prettyPrint();
        assertEquals(200, deleteDataverse1Response.getStatusCode());

        Response deleteDataverse2Response = UtilIT.deleteDataverse(dataverseAlias2, apiToken1);
//        deleteDataverse2Response.prettyPrint();
        assertEquals(404, deleteDataverse2Response.getStatusCode());

        Response deleteUser1Response = UtilIT.deleteUser(username1);
        deleteUser1Response.prettyPrint();
        assertEquals(200, deleteUser1Response.getStatusCode());

    }

}
