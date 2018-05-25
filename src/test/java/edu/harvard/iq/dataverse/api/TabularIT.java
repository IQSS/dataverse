package edu.harvard.iq.dataverse.api;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import java.util.logging.Logger;
import static javax.ws.rs.core.Response.Status.CREATED;
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
//        String filePersistentId = JsonPath.from(uploadIngestableFile.body().asString()).getString("data.files[0].dataFile.persistentId");
        System.out.println("fileId: " + fileId);
//        System.out.println("filePersistentId: " + filePersistentId);

        // Give file time to ingest
        Thread.sleep(10000);

        Response fileMetadataNoFormat = UtilIT.getFileMetadata(fileIdAsString, null, apiToken);
        fileMetadataNoFormat.prettyPrint();
        fileMetadataNoFormat.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("codeBook.fileDscr.fileTxt.fileName", equalTo("50by1000.tab"));

        Response fileMetadataNoFormatFileId = UtilIT.getFileMetadata(fileIdAsString, null, apiToken);
        fileMetadataNoFormatFileId.prettyPrint();
        fileMetadataNoFormatFileId.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("codeBook.fileDscr.fileTxt.fileName", equalTo("50by1000.tab"));

        Response fileMetadataDdi = UtilIT.getFileMetadata(fileIdAsString, "ddi", apiToken);
        fileMetadataDdi.prettyPrint();
        fileMetadataDdi.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("codeBook.fileDscr.fileTxt.fileName", equalTo("50by1000.tab"))
                .body("codeBook.dataDscr.var[0].@name", equalTo("var1"))
                // Yes, it's odd that we go from "var1" to "var3" to "var2" to "var5"
                .body("codeBook.dataDscr.var[1].@name", equalTo("var3"))
                .body("codeBook.dataDscr.var[2].@name", equalTo("var2"))
                .body("codeBook.dataDscr.var[3].@name", equalTo("var5"));

        boolean testPreprocessedMetadataFormat = false;
        if (testPreprocessedMetadataFormat) {
            // If you don't have all the dependencies in place, such as Rserve, you might get a 503 and this error:
            // org.rosuda.REngine.Rserve.RserveException: Cannot connect: Connection refused
            Response fileMetadataPreProcessed = UtilIT.getFileMetadata(fileIdAsString, "preprocessed", apiToken);
            fileMetadataPreProcessed.prettyPrint();
            fileMetadataPreProcessed.then().assertThat()
                    .statusCode(OK.getStatusCode())
                    .body("codeBook.fileDscr.fileTxt.fileName", equalTo("50by1000.tab"));
        }

    }

}
