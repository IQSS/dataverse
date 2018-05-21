package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import java.util.logging.Logger;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import org.junit.BeforeClass;
import org.junit.Test;

public class TabularIT {

    private static final Logger logger = Logger.getLogger(TabularIT.class.getCanonicalName());

    @BeforeClass
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testTabularFile() throws InterruptedException {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat()
                .statusCode(OK.getStatusCode());
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        createDataverseResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        createDatasetResponse.then().assertThat()
                .statusCode(CREATED.getStatusCode());
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        String persistentId = JsonPath.from(createDatasetResponse.body().asString()).getString("data.persistentId");
        logger.info("Dataset created with id " + datasetId + " and persistent id " + persistentId);

        String pathToFileThatGoesThroughIngest = "scripts/search/data/tabular/50by1000.dta";
        Response uploadIngestableFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFileThatGoesThroughIngest, apiToken);
        uploadIngestableFile.prettyPrint();
        uploadIngestableFile.then().assertThat()
                .statusCode(OK.getStatusCode());
        long fileId = JsonPath.from(uploadIngestableFile.body().asString()).getLong("data.files[0].dataFile.id");
        String fileIdAsString = Long.toString(fileId);
        String filePersistentId = JsonPath.from(uploadIngestableFile.body().asString()).getString("data.files[0].dataFile.persistentId");
        System.out.println("fileId: " + fileId);
        System.out.println("filePersistentId: " + filePersistentId);

        // Give file time to ingest
        Thread.sleep(3000);

        Response getMetaUsingPersistentId = UtilIT.getMetaDatafileDeprecated(filePersistentId, apiToken);
        getMetaUsingPersistentId.then().assertThat()
                .statusCode(NOT_FOUND.getStatusCode());

        Response getMetaUsingId = UtilIT.getMetaDatafileDeprecated(fileIdAsString, apiToken);
        getMetaUsingId.prettyPrint();
        getMetaUsingId.then().assertThat()
                .body("codeBook.fileDscr.fileTxt.fileName", equalTo("50by1000.tab"))
                .statusCode(OK.getStatusCode());

        Response fileMetadataNoFormat = UtilIT.getFileMetadata(filePersistentId, null, apiToken);
        fileMetadataNoFormat.prettyPrint();
        fileMetadataNoFormat.then().assertThat()
                .body("codeBook.fileDscr.fileTxt.fileName", equalTo("50by1000.tab"))
                .statusCode(OK.getStatusCode());

        Response fileMetadataNoFormatFileId = UtilIT.getFileMetadata(fileIdAsString, null, apiToken);
        fileMetadataNoFormatFileId.prettyPrint();
        fileMetadataNoFormatFileId.then().assertThat()
                .body("codeBook.fileDscr.fileTxt.fileName", equalTo("50by1000.tab"))
                .statusCode(OK.getStatusCode());

        Response fileMetadataDdi = UtilIT.getFileMetadata(filePersistentId, "ddi", apiToken);
        fileMetadataDdi.prettyPrint();
        fileMetadataDdi.then().assertThat()
                .body("codeBook.fileDscr.fileTxt.fileName", equalTo("50by1000.tab"))
                .statusCode(OK.getStatusCode());

        boolean testPreprocessedMetadataFormat = false;
        if (testPreprocessedMetadataFormat) {
            // If you don't have all the dependencies in place, such as Rserve, you might get a 503 and this error:
            // org.rosuda.REngine.Rserve.RserveException: Cannot connect: Connection refused
            Response fileMetadataPreProcessed = UtilIT.getFileMetadata(filePersistentId, "preprocessed", apiToken);
            fileMetadataPreProcessed.prettyPrint();
            fileMetadataPreProcessed.then().assertThat()
                    .body("codeBook.fileDscr.fileTxt.fileName", equalTo("50by1000.tab"))
                    .statusCode(OK.getStatusCode());
        }

    }

}
