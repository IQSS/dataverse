package edu.harvard.iq.dataverse.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests JSON-LD export with incomplete HTML tags in file descriptions.
 *
 * Incomplete HTML tags like "<CSP" or "<img" cause JsonParsingException
 * due to MarkupChecker.stripAllTags() processing JSON-LD as HTML.
 */
public class JsonLDExportIT {

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    /**
     * Tests JSON-LD export with incomplete HTML tags in file descriptions.
     */
    @Test
    public void testJsonLDExportWithIncompleteHtmlTagsInFileDescription() {
        // Create admin user
        Response createUserResponse = UtilIT.createRandomUser();
        createUserResponse.then().assertThat().statusCode(200);
        String apiToken = UtilIT.getApiTokenFromResponse(createUserResponse);
        String username = UtilIT.getUsernameFromResponse(createUserResponse);
        UtilIT.makeSuperUser(username);

        Integer datasetId = null;
        String dataverseAlias = null;

        try {
            // Create test dataverse
            Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
            createDataverseResponse.then().assertThat().statusCode(201);
            dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

            // Publish dataverse (required before publishing datasets)
            Response publishDataverse = UtilIT.publishDataverseViaNativeApi(
                dataverseAlias,
                apiToken
            );
            publishDataverse.then().assertThat().statusCode(200);

            // Create test dataset
            Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(
                dataverseAlias,
                apiToken
            );
            createDatasetResponse.then().assertThat().statusCode(201);
            datasetId = UtilIT.getDatasetIdFromResponse(createDatasetResponse);
            String datasetPersistentId = UtilIT.getDatasetPersistentIdFromResponse(
                createDatasetResponse
            );

            // Upload file with incomplete HTML tags in description
            String problematicDescription =
                "File contains <CSP data, <img tag, <script and text ending with <";

            JsonObjectBuilder fileMetadata = Json.createObjectBuilder()
                .add("description", problematicDescription)
                .add("label", "test-file-with-csp-tag.tab");

            Response uploadResponse = UtilIT.uploadFileViaNative(
                datasetId.toString(),
                "src/test/resources/tab/test.tab",
                fileMetadata.build(),
                apiToken
            );
            uploadResponse.then().assertThat().statusCode(200);

            // Publish dataset
            Response publishResponse = UtilIT.publishDatasetViaNativeApi(
                datasetPersistentId,
                "major",
                apiToken
            );
            publishResponse.then().assertThat().statusCode(200);

            // Test JSON-LD export
            Response jsonLdExportResponse = UtilIT.exportDataset(datasetPersistentId, "schema.org");

            // Verify export does not fail
            jsonLdExportResponse
                .then()
                .assertThat()
                .statusCode(not(equalTo(500)))
                .body(not(containsString("JsonParsingException")))
                .body(not(containsString("Unexpected char -1")));

            // Verify JSON structure if export succeeds
            if (jsonLdExportResponse.getStatusCode() == 200) {
                String responseBody = jsonLdExportResponse.getBody().asString();

                // Verify valid JSON
                assertDoesNotThrow(
                    () -> {
                        jakarta.json.Json.createReader(
                            new java.io.StringReader(responseBody)
                        ).readObject();
                    },
                    "JSON-LD export should produce valid JSON"
                );

                // Verify JSON-LD structure
                assertTrue(
                    responseBody.contains("@context") || responseBody.contains("@type"),
                    "Response should contain JSON-LD structure"
                );
            }
        } finally {
            // Cleanup - delete in reverse order of creation
            if (datasetId != null) {
                UtilIT.destroyDataset(datasetId, apiToken);
            }
            if (dataverseAlias != null) {
                UtilIT.deleteDataverse(dataverseAlias, apiToken);
            }
            UtilIT.deleteUser(username);
        }
    }
}
