package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import static javax.ws.rs.core.Response.Status.OK;
import org.junit.BeforeClass;
import org.junit.Test;

public class GeoconnectIT {

    @BeforeClass
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

//    @Ignore
    @Test
    public void checkMapLayerMetadatas() {
        Response createUser = UtilIT.createRandomUser();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response checkMapLayerMetadatas = UtilIT.checkMapLayerMetadatas(apiToken);
        checkMapLayerMetadatas.prettyPrint();
        checkMapLayerMetadatas.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

//    @Ignore
    @Test
    public void checkSingleMapLayerMetadata() {
        Response createUser = UtilIT.createRandomUser();
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response checkMapLayerMetadatas = UtilIT.checkMapLayerMetadatas(1l, apiToken);
        checkMapLayerMetadatas.prettyPrint();
        checkMapLayerMetadatas.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

//    @Ignore
    @Test
    public void deleteMapFromFile() {
        String apiToken = "b5a144d8-b3a3-452b-9543-131360884281";
        long fileId = 2661;
        Response deleteMapFromFile = UtilIT.deleteMapFromFile(fileId, apiToken);
        deleteMapFromFile.prettyPrint();
        deleteMapFromFile.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

}
