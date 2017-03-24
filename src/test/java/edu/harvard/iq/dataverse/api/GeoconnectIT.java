package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import static com.jayway.restassured.RestAssured.given;
import com.jayway.restassured.response.Response;
import static edu.harvard.iq.dataverse.api.UtilIT.API_TOKEN_HTTP_HEADER;
import static javax.ws.rs.core.Response.Status.OK;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class GeoconnectIT {

    @BeforeClass
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

//    @Ignore
    @Test
    public void testMapLayerMetadatas() {
        Response response = given().get("/api/admin/geoconnect/mapLayerMetadatas");
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

//    @Ignore
    @Test
    public void checkMapLayerMetadatas() {
        Response createSuperuser = UtilIT.createRandomUser();
        String superuserUsername = UtilIT.getUsernameFromResponse(createSuperuser);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        Response toggleSuperuser = UtilIT.makeSuperUser(superuserUsername);
        toggleSuperuser.then().assertThat()
                .statusCode(OK.getStatusCode());

        Response response = given()
                .header(API_TOKEN_HTTP_HEADER, superuserApiToken)
                .post("/api/admin/geoconnect/mapLayerMetadatas/check");
        response.prettyPrint();
        response.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

}
