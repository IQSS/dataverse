package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import static javax.ws.rs.core.Response.Status.OK;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MetadataBlocksIT {

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    void testGetCitationBlock() {
        Response getCitationBlock = UtilIT.getMetadataBlock("citation");
        getCitationBlock.prettyPrint();
        getCitationBlock.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.fields.subject.controlledVocabularyValues[0]", CoreMatchers.is("Agricultural Sciences"));
    }

}
