package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.OK;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class FileTypeDetectionIT {

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testOverrideMimeType() {
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);

        String readmeFile = "README.md";

        JsonObjectBuilder readmeFileMetadata = Json.createObjectBuilder()
                .add("description", "How to run the code on the data.")
                .add("categories", Json.createArrayBuilder()
                        .add("Documentation")
                );

        // Markdown media type:  https://tools.ietf.org/html/rfc7763
        String overrideMimeType = "text/markdown";
        Response addReadme = UtilIT.uploadFileViaNative(datasetId.toString(), readmeFile, readmeFileMetadata.build().toString(), overrideMimeType, apiToken);
        addReadme.prettyPrint();
        addReadme.then().assertThat()
                .body("data.files[0].categories[0]", equalTo("Documentation"))
                .body("data.files[0].dataFile.contentType", equalTo("text/markdown"))
                .body("data.files[0].dataFile.description", equalTo("How to run the code on the data."))
                .body("data.files[0].directoryLabel", nullValue())
                .body("data.files[0].dataFile.tags", nullValue())
                .body("data.files[0].dataFile.tabularTags", nullValue())
                .body("data.files[0].label", equalTo("README.md"))
                // not sure why description appears in two places
                .body("data.files[0].description", equalTo("How to run the code on the data."))
                .statusCode(OK.getStatusCode());

        String jupyterNotebook = "src/test/java/edu/harvard/iq/dataverse/util/irc-metrics.ipynb";

        JsonObjectBuilder jupyterNotebookMetadata = Json.createObjectBuilder()
                .add("description", "Jupyter Notebook showing IRC metrics.")
                .add("directoryLabel", "code")
                .add("categories", Json.createArrayBuilder()
                        .add("Code")
                );

        Response addCode = UtilIT.uploadFileViaNative(datasetId.toString(), jupyterNotebook, jupyterNotebookMetadata.build(), apiToken);
        addCode.prettyPrint();
        addCode.then().assertThat()
                .body("data.files[0].categories[0]", equalTo("Code"))
                .body("data.files[0].dataFile.contentType", equalTo("application/x-ipynb+json"))
                .body("data.files[0].dataFile.description", equalTo("Jupyter Notebook showing IRC metrics."))
                .body("data.files[0].directoryLabel", equalTo("code"))
                .body("data.files[0].dataFile.tags", nullValue())
                .body("data.files[0].dataFile.tabularTags", nullValue())
                .body("data.files[0].label", equalTo("irc-metrics.ipynb"))
                // not sure why description appears in two places
                .body("data.files[0].description", equalTo("Jupyter Notebook showing IRC metrics."))
                .statusCode(OK.getStatusCode());

        String tsvFile = "src/test/java/edu/harvard/iq/dataverse/util/irclog.tsv";

        JsonObjectBuilder tsvFileMetadata = Json.createObjectBuilder()
                .add("description", "TSV file of Dataverse IRC logs.")
                .add("directoryLabel", "data")
                .add("categories", Json.createArrayBuilder()
                        .add("Data")
                );

        Response addData = UtilIT.uploadFileViaNative(datasetId.toString(), tsvFile, tsvFileMetadata.build(), apiToken);
        addData.prettyPrint();
        addData.then().assertThat()
                .body("data.files[0].categories[0]", equalTo("Data"))
                .body("data.files[0].dataFile.contentType", equalTo("text/tsv"))
                .body("data.files[0].dataFile.description", equalTo("TSV file of Dataverse IRC logs."))
                .body("data.files[0].directoryLabel", equalTo("data"))
                .body("data.files[0].dataFile.tags", nullValue())
                .body("data.files[0].dataFile.tabularTags", nullValue())
                .body("data.files[0].label", equalTo("irclog.tsv"))
                // not sure why description appears in two places
                .body("data.files[0].description", equalTo("TSV file of Dataverse IRC logs."))
                .statusCode(OK.getStatusCode());

    }

    @Test
    public void testRedetectMimeType() {
        Response createUser = UtilIT.createRandomUser();
        createUser.prettyPrint();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDataset = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDataset.prettyPrint();
        createDataset.then().assertThat()
                .statusCode(CREATED.getStatusCode());

        Integer datasetId = UtilIT.getDatasetIdFromResponse(createDataset);

        String filePath = "scripts/issues/1380/dvs.pdf";

        JsonObjectBuilder readmeFileMetadata = Json.createObjectBuilder()
                .add("description", "This is a PDF.")
                .add("categories", Json.createArrayBuilder()
                        .add("Documentation")
                );

        /**
         * We are overriding the MIME type here because even though Dataverse
         * knows how to figure out what a PDF is we want to pretend it doesn't
         * so that we can later try the "redetect file type" API.
         */
        String overrideMimeType = "foo/bar";
        Response addFileUnknownType = UtilIT.uploadFileViaNative(datasetId.toString(), filePath, readmeFileMetadata.build().toString(), overrideMimeType, apiToken);
        addFileUnknownType.prettyPrint();
        addFileUnknownType.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].categories[0]", equalTo("Documentation"))
                .body("data.files[0].dataFile.contentType", equalTo("foo/bar"))
                .body("data.files[0].dataFile.description", equalTo("This is a PDF."))
                .body("data.files[0].directoryLabel", nullValue())
                .body("data.files[0].dataFile.tags", nullValue())
                .body("data.files[0].dataFile.tabularTags", nullValue())
                .body("data.files[0].label", equalTo("dvs.pdf"))
                // not sure why description appears in two places
                .body("data.files[0].description", equalTo("This is a PDF."));

        Long fileId = JsonPath.from(addFileUnknownType.asString()).getLong("data.files[0].dataFile.id");
        System.out.println("file id: " + fileId);
        boolean dryRunTrue = true;
        Response redetectDryRun = UtilIT.redetectFileType(fileId.toString(), dryRunTrue, apiToken);
        redetectDryRun.prettyPrint();
        redetectDryRun.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.dryRun", equalTo(true))
                .body("data.oldContentType", equalTo("foo/bar"))
                .body("data.newContentType", equalTo("application/pdf"));

        Response createNoPrivsUser = UtilIT.createRandomUser();
        createNoPrivsUser.prettyPrint();
        createNoPrivsUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String noPrivsUsername = UtilIT.getUsernameFromResponse(createNoPrivsUser);
        String noPrivsApiToken = UtilIT.getApiTokenFromResponse(createNoPrivsUser);

        Response forbidden = UtilIT.redetectFileType(fileId.toString(), true, noPrivsApiToken);
        forbidden.then().assertThat()
                .statusCode(UNAUTHORIZED.getStatusCode());

        Response noChange = UtilIT.nativeGet(datasetId, apiToken);
        noChange.prettyPrint();
        noChange.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.latestVersion.files[0].dataFile.contentType", equalTo("foo/bar"));

        boolean dryRunFalse = false;
        Response redetectAndChange = UtilIT.redetectFileType(fileId.toString(), dryRunFalse, apiToken);
        redetectAndChange.prettyPrint();
        redetectAndChange.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.dryRun", equalTo(false))
                .body("data.oldContentType", equalTo("foo/bar"))
                .body("data.newContentType", equalTo("application/pdf"));

        Response databaseChanged = UtilIT.nativeGet(datasetId, apiToken);
        databaseChanged.prettyPrint();
        databaseChanged.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.latestVersion.files[0].dataFile.contentType", equalTo("application/pdf"));

    }

}
