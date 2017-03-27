package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
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
        Response listMapLayerMetadatas = UtilIT.listMapLayerMetadatas();
        listMapLayerMetadatas.prettyPrint();
        listMapLayerMetadatas.then().assertThat()
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

        Response checkMapLayerMetadatas = UtilIT.checkMapLayerMetadatas(superuserApiToken);

        checkMapLayerMetadatas.prettyPrint();
        checkMapLayerMetadatas.then().assertThat()
                .statusCode(OK.getStatusCode());
    }

//    @Ignore
    @Test
    public void getMapFromFile() {

        Response createUnprivilegedUser = UtilIT.createRandomUser();
        String unprivilegedUserApiToken = UtilIT.getApiTokenFromResponse(createUnprivilegedUser);
        long fileId = 2663;

        Response forbidden = UtilIT.getMapFromFile(fileId, unprivilegedUserApiToken);
        forbidden.prettyPrint();
        forbidden.then().assertThat()
                .body("message", CoreMatchers.equalTo("You are not permitted to check the map for file id " + fileId + "."))
                .statusCode(FORBIDDEN.getStatusCode());

        String apiToken = "b5a144d8-b3a3-452b-9543-131360884281";

        Response fileDoesNotExist = UtilIT.getMapFromFile(Long.MAX_VALUE, apiToken);
        fileDoesNotExist.prettyPrint();
        fileDoesNotExist.then().assertThat()
                .body("message", CoreMatchers.equalTo("File not found based on id " + Long.MAX_VALUE + "."))
                .statusCode(NOT_FOUND.getStatusCode());

        Response getMapFromFile = UtilIT.getMapFromFile(fileId, apiToken);
        getMapFromFile.prettyPrint();

        getMapFromFile.then().assertThat()
                //                .body("data.lastVerifiedTime", Matchers.startsWith("2"))
                .statusCode(OK.getStatusCode());
    }

//    @Ignore
    @Test
    public void checkMapFromFile() {

        Response createUnprivilegedUser = UtilIT.createRandomUser();
        String unprivilegedUserApiToken = UtilIT.getApiTokenFromResponse(createUnprivilegedUser);
        long fileId = 2663;

        Response forbidden = UtilIT.checkMapFromFile(fileId, unprivilegedUserApiToken);
        forbidden.prettyPrint();
        forbidden.then().assertThat()
                .body("message", CoreMatchers.equalTo("You are not permitted to check the map for file id " + fileId + "."))
                .statusCode(FORBIDDEN.getStatusCode());

        String apiToken = "b5a144d8-b3a3-452b-9543-131360884281";

        Response fileDoesNotExist = UtilIT.checkMapFromFile(Long.MAX_VALUE, apiToken);
        fileDoesNotExist.prettyPrint();
        fileDoesNotExist.then().assertThat()
                .body("message", CoreMatchers.equalTo("File not found based on id " + Long.MAX_VALUE + "."))
                .statusCode(NOT_FOUND.getStatusCode());

        Response checkMapFromFile = UtilIT.checkMapFromFile(fileId, apiToken);
        checkMapFromFile.prettyPrint();
        checkMapFromFile.then().assertThat()
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
