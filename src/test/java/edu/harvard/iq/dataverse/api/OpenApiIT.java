package edu.harvard.iq.dataverse.api;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import io.restassured.RestAssured;
import io.restassured.response.Response;

public class OpenApiIT {

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testOpenApi(){

        Response openApi = UtilIT.getOpenAPI("application/json", "json");
        openApi.prettyPrint();
        openApi.then().assertThat()
            .statusCode(200);

        openApi = UtilIT.getOpenAPI("", "json");
        openApi.prettyPrint();
        openApi.then().assertThat()
            .statusCode(200);

        openApi = UtilIT.getOpenAPI("", "yaml");
        openApi.prettyPrint();
        openApi.then().assertThat()
            .statusCode(200);

        openApi = UtilIT.getOpenAPI("application/json", "yaml");
        openApi.prettyPrint();
        openApi.then().assertThat()
            .statusCode(400);
        

    }
}