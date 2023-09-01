package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.io.File;
import java.util.Arrays;
import java.util.logging.Logger;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class TabularIT {

    private static final Logger logger = Logger.getLogger(TabularIT.class.getCanonicalName());

    @BeforeAll
    public static void setUpClass() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Disabled
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
        
        assertTrue(UtilIT.sleepForLock(datasetId.longValue(), "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + pathToFileThatGoesThroughIngest);
      //  Thread.sleep(10000);

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

    @Disabled
    @Test
    public void test50by1000() {
        // cp scripts/search/data/tabular/50by1000.dta /tmp
        String fileName = "/tmp/50by1000.dta";
        String fileType = "application/x-stata";
        Response response = UtilIT.testIngest(fileName, fileType);
        response.prettyPrint();
        assertEquals("NVARS: 50", response.body().asString().split("\n")[0]);
    }

    @Disabled
    @Test
    public void testStata13TinyFile() {
        // cp scripts/search/data/tabular/120745.dta /tmp
        String fileName = "/tmp/120745.dta";
        String fileType = "application/x-stata";
        Response response = UtilIT.testIngest(fileName, fileType);
        response.prettyPrint();
        assertEquals("NVARS: 1", response.body().asString().split("\n")[0]);
    }

    @Disabled
    @Test
    public void testStata13Auto() {
        // curl https://www.stata-press.com/data/r13/auto.dta > /tmp/stata13-auto.dta
        String fileName = "/tmp/stata13-auto.dta";
        String fileType = "application/x-stata-13";
        Response response = UtilIT.testIngest(fileName, fileType);
        response.prettyPrint();
        assertEquals("NVARS: 12", response.body().asString().split("\n")[0]);
    }

    @Disabled
    @Test
    public void testStata14OpenSourceAtHarvard() {
        // https://dataverse.harvard.edu/file.xhtml?fileId=3040230 converted to Stata 14: 2017-07-31.tab
        // cp scripts/search/data/tabular/open-source-at-harvard118.dta /tmp
        String fileName = "/tmp/open-source-at-harvard118.dta";
        // No mention of stata at https://www.iana.org/assignments/media-types/media-types.xhtml
        String fileType = "application/x-stata-14";
        Response response = UtilIT.testIngest(fileName, fileType);
        response.prettyPrint();
        assertEquals("NVARS: 10", response.body().asString().split("\n")[0]);
    }

    @Disabled
    @Test
    public void testStata14Aggregated() {
        // https://dataverse.harvard.edu/file.xhtml?fileId=3140457 Stata 14: 2018_04_06_Aggregated_dataset_v2.dta
        String fileName = "/tmp/2018_04_06_Aggregated_dataset_v2.dta";
        // No mention of stata at https://www.iana.org/assignments/media-types/media-types.xhtml
        String fileType = "application/x-stata-14";
        Response response = UtilIT.testIngest(fileName, fileType);
        response.prettyPrint();
        assertEquals("NVARS: 227", response.body().asString().split("\n")[0]);
    }

    @Disabled
    @Test
    public void testStata14MmPublic() {
        // TODO: This file was downloaded at random. We could keep trying to get it to ingest.
        // https://dataverse.harvard.edu/file.xhtml?fileId=2775556 Stata 14: mm_public_120615_v14.dta
        // For this file "hasSTRLs" is true so it might be nice to get it working.
        String fileName = "/tmp/mm_public_120615_v14.dta";
        // No mention of stata at https://www.iana.org/assignments/media-types/media-types.xhtml
        String fileType = "application/x-stata-14";
        Response response = UtilIT.testIngest(fileName, fileType);
        response.prettyPrint();
        // We don't know how many variables it has. Probably not 12.
        assertEquals("NVARS: 12", response.body().asString().split("\n")[0]);
    }

    @Disabled
    @Test
    public void testStata15() {
        // for i in `echo {0..33000}`; do echo -n "var$i,"; done > 33k.csv
        // Then open Stata 15, run `set maxvar 40000` and import.
        String fileName = "/tmp/33k.dta";
        String fileType = "application/x-stata-15";
        Response response = UtilIT.testIngest(fileName, fileType);
        response.prettyPrint();
        assertEquals("NVARS: 33001", response.body().asString().split("\n")[0]);
    }

    @Disabled
    @Test
    public void testStata13Multiple() {
        String fileType = "application/x-stata-13";
        // From /usr/local/dvn-admin/stata on dvn-build 
        String stata13directory = "/tmp/stata-13";
        File folder = new File(stata13directory);
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            File file = listOfFiles[i];
            String filename = file.getName();
            String filenameFullPath = file.getAbsolutePath();
            Response response = UtilIT.testIngest(filenameFullPath, fileType);
            String firstLine = response.body().asString().split("\n")[0];
            String[] parts = firstLine.split(":");
            String[] justErrors = Arrays.copyOfRange(parts, 1, parts.length);
            System.out.println(i + "\t" + filename + "\t" + Arrays.toString(justErrors) + "\t" + firstLine);
        }
    }
    
    @Disabled
    @Test
    public void testStata14Multiple() {
        String fileType = "application/x-stata-14";
        // From /usr/local/dvn-admin/stata on dvn-build 
        String stata13directory = "/tmp/stata-14";
        File folder = new File(stata13directory);
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            File file = listOfFiles[i];
            String filename = file.getName();
            String filenameFullPath = file.getAbsolutePath();
            Response response = UtilIT.testIngest(filenameFullPath, fileType);
            String firstLine = response.body().asString().split("\n")[0];
            String[] parts = firstLine.split(":");
            String[] justErrors = Arrays.copyOfRange(parts, 1, parts.length);
            System.out.println(i + "\t" + filename + "\t" + Arrays.toString(justErrors) + "\t" + firstLine);
        }
    }

}
