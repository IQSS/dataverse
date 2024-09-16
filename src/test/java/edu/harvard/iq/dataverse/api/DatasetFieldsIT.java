package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;

public class DatasetFieldsIT {

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    void testListAllFacetableDatasetFields() {
        Response listAllFacetableDatasetFieldsResponse = UtilIT.listAllFacetableDatasetFields();
        listAllFacetableDatasetFieldsResponse.then().assertThat().statusCode(OK.getStatusCode());
        int expectedNumberOfFacetableDatasetFields = 59;
        listAllFacetableDatasetFieldsResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].name", equalTo("authorName"))
                .body("data[0].displayName", equalTo("Author Name"))
                .body("data.size()", equalTo(expectedNumberOfFacetableDatasetFields));
    }
}
