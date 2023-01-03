package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import static javax.ws.rs.core.Response.Status.OK;
import org.hamcrest.CoreMatchers;
import org.junit.BeforeClass;
import org.junit.Test;

public class MetadataBlocksIT {

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testGetCitationBlock() {
        Response getCitationBlock = UtilIT.getMetadataBlock("citation");
        getCitationBlock.prettyPrint();
        getCitationBlock.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.fields.subject.controlledVocabularyValues[0]", CoreMatchers.is("Agricultural Sciences"));
    }

}
