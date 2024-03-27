package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
                .body("data.fields.subject.controlledVocabularyValues[0]", CoreMatchers.is("Agricultural Sciences"))
                .body("data.fields.title.displayOrder", CoreMatchers.is(0))
                .body("data.fields.title.typeClass", CoreMatchers.is("primitive"))
                .body("data.fields.title.isRequired", CoreMatchers.is(true));
    }
    
    @Test
    void testDatasetWithAllDefaultMetadata() {
        // given
        Response createUser = UtilIT.createRandomUser();
        assumeTrue(createUser.statusCode() < 300,
            "code=" + createUser.statusCode() +
            ", response=" + createUser.prettyPrint());
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);
        assumeFalse(apiToken == null || apiToken.isBlank());

        Response createCollection = UtilIT.createRandomDataverse(apiToken);
        assumeTrue(createCollection.statusCode() < 300,
            "code=" + createCollection.statusCode() +
            ", response=" + createCollection.prettyPrint());
        String dataverseAlias = UtilIT.getAliasFromResponse(createCollection);
        assumeFalse(dataverseAlias == null || dataverseAlias.isBlank());

        // when
        String pathToJsonFile = "scripts/api/data/dataset-create-new-all-default-fields.json";
        Response createDataset = UtilIT.createDatasetViaNativeApi(dataverseAlias, pathToJsonFile, apiToken);

        // then
        assertEquals(CREATED.getStatusCode(), createDataset.statusCode(),
           "code=" + createDataset.statusCode() +
            ", response=" + createDataset.prettyPrint());
        createDataset.then().assertThat()
            .body("status", CoreMatchers.equalTo("OK"));
    }

}
