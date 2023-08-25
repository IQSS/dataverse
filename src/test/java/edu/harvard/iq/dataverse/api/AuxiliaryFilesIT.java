package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.Status.CREATED;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AuxiliaryFilesIT {

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();
    }

    @Test
    public void testUploadAuxFiles() throws IOException {
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(OK.getStatusCode());
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
        String datasetPid = UtilIT.getDatasetPersistentIdFromResponse(createDataset);

        Path pathToDataFile = Paths.get(java.nio.file.Files.createTempDirectory(null) + File.separator + "data.csv");
        String contentOfCsv = ""
                + "name,pounds,species\n"
                + "Marshall,40,dog\n"
                + "Tiger,17,cat\n"
                + "Panther,21,cat\n";
        java.nio.file.Files.write(pathToDataFile, contentOfCsv.getBytes());

        Response uploadFile = UtilIT.uploadFileViaNative(datasetId.toString(), pathToDataFile.toString(), apiToken);
        uploadFile.prettyPrint();
        uploadFile.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.files[0].label", equalTo("data.csv"));

        Long fileId = JsonPath.from(uploadFile.body().asString()).getLong("data.files[0].dataFile.id");

        assertTrue(UtilIT.sleepForLock(datasetId.longValue(), "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION), "Failed test if Ingest Lock exceeds max duration " + pathToDataFile);

        Response restrictFile = UtilIT.restrictFile(fileId.toString(), true, apiToken);
        restrictFile.prettyPrint();
        restrictFile.then().assertThat().statusCode(OK.getStatusCode());

        String dpType = "DP";

        // JSON aux file
        Path pathToAuxFileJson = Paths.get(java.nio.file.Files.createTempDirectory(null) + File.separator + "data.json");
        String contentOfJson = "{}";
        java.nio.file.Files.write(pathToAuxFileJson, contentOfJson.getBytes());
        String formatTagJson = "dpJson";
        String formatVersionJson = "0.1";
        String mimeTypeJson = "application/json";
        Response uploadAuxFileJson = UtilIT.uploadAuxFile(fileId, pathToAuxFileJson.toString(), formatTagJson, formatVersionJson, mimeTypeJson, true, dpType, apiToken);
        uploadAuxFileJson.prettyPrint();
        uploadAuxFileJson.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.type", equalTo("DP"))
                .body("data.contentType", equalTo("application/json"));
        Response uploadAuxFileJsonAgain = UtilIT.uploadAuxFile(fileId, pathToAuxFileJson.toString(), formatTagJson, formatVersionJson, mimeTypeJson, true, dpType, apiToken);
        uploadAuxFileJsonAgain.prettyPrint();
        uploadAuxFileJsonAgain.then().assertThat()
                .statusCode(CONFLICT.getStatusCode());

        // XML aux file
        Path pathToAuxFileXml = Paths.get(java.nio.file.Files.createTempDirectory(null) + File.separator + "data.xml");
        String contentOfXml = "<foo></foo>";
        java.nio.file.Files.write(pathToAuxFileXml, contentOfXml.getBytes());
        String formatTagXml = "dpXml";
        String formatVersionXml = "0.1";
        String mimeTypeXml = "application/xml";
        Response uploadAuxFileXml = UtilIT.uploadAuxFile(fileId, pathToAuxFileXml.toString(), formatTagXml, formatVersionXml, mimeTypeXml, true, dpType, apiToken);
        uploadAuxFileXml.prettyPrint();
        uploadAuxFileXml.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.type", equalTo("DP"))
                .body("data.contentType", equalTo("application/xml"));

        // PDF aux file
        Path pathToAuxFilePdf = Paths.get(java.nio.file.Files.createTempDirectory(null) + File.separator + "data.pdf");
        // PDF content from https://stackoverflow.com/questions/17279712/what-is-the-smallest-possible-valid-pdf/32142316#32142316
        String contentOfPdf = "%PDF-1.2 \n"
                + "9 0 obj\n"
                + "<<\n"
                + ">>\n"
                + "stream\n"
                + "BT/ 9 Tf(Test)' ET\n"
                + "endstream\n"
                + "endobj\n"
                + "4 0 obj\n"
                + "<<\n"
                + "/Type /Page\n"
                + "/Parent 5 0 R\n"
                + "/Contents 9 0 R\n"
                + ">>\n"
                + "endobj\n"
                + "5 0 obj\n"
                + "<<\n"
                + "/Kids [4 0 R ]\n"
                + "/Count 1\n"
                + "/Type /Pages\n"
                + "/MediaBox [ 0 0 99 9 ]\n"
                + ">>\n"
                + "endobj\n"
                + "3 0 obj\n"
                + "<<\n"
                + "/Pages 5 0 R\n"
                + "/Type /Catalog\n"
                + ">>\n"
                + "endobj\n"
                + "trailer\n"
                + "<<\n"
                + "/Root 3 0 R\n"
                + ">>\n"
                + "%%EOF";
        java.nio.file.Files.write(pathToAuxFilePdf, contentOfPdf.getBytes());
        String formatTagPdf = "dpPdf";
        String formatVersionPdf = "0.1";
        String mimeTypePdf = "application/pdf";
        Response uploadAuxFilePdf = UtilIT.uploadAuxFile(fileId, pathToAuxFilePdf.toString(), formatTagPdf, formatVersionPdf, mimeTypePdf, true, dpType, apiToken);
        uploadAuxFilePdf.prettyPrint();
        uploadAuxFilePdf.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.type", equalTo("DP"))
                .body("data.contentType", equalTo(mimeTypePdf));

        // Non-DP aux file, no type specified
        Path pathToAuxFileMd = Paths.get(java.nio.file.Files.createTempDirectory(null) + File.separator + "README.md");
        String contentOfMd = "This is my README.";
        java.nio.file.Files.write(pathToAuxFileMd, contentOfMd.getBytes());
        String formatTagMd = "README";
        String formatVersionMd = "0.1";
        String mimeTypeMd = "text/markdown";
        Response uploadAuxFileMd = UtilIT.uploadAuxFile(fileId, pathToAuxFileMd.toString(), formatTagMd, formatVersionMd, mimeTypeMd, true, null, apiToken);
        uploadAuxFileMd.prettyPrint();
        uploadAuxFileMd.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.type", equalTo(null))
                .body("data.contentType", equalTo("text/markdown"));

        // Unknown type
        Path pathToAuxFileTxt = Paths.get(java.nio.file.Files.createTempDirectory(null) + File.separator + "nbt.txt");
        String contentOfTxt = "It's gonna be huge.";
        java.nio.file.Files.write(pathToAuxFileTxt, contentOfTxt.getBytes());
        String formatTagTxt = "gonnaBeHuge";
        String formatVersionTxt = "0.1";
        String mimeTypeTxt = "text/plain";
        String typeTxt = "NEXT_BIG_THING";
        Response uploadAuxFileTxt = UtilIT.uploadAuxFile(fileId, pathToAuxFileTxt.toString(), formatTagTxt, formatVersionTxt, mimeTypeTxt, true, typeTxt, apiToken);
        uploadAuxFileTxt.prettyPrint();
        uploadAuxFileTxt.then().assertThat()
                .statusCode(OK.getStatusCode())
                //                .body("data.type", equalTo("Other Auxiliary Files"));
                .body("data.type", equalTo("NEXT_BIG_THING"));

        // Another Unknown type
        Path pathToAuxFileTxt2 = Paths.get(java.nio.file.Files.createTempDirectory(null) + File.separator + "future.txt");
        String contentOfTxt2 = "The future's so bright I have to wear shades.";
        java.nio.file.Files.write(pathToAuxFileTxt2, contentOfTxt2.getBytes());
        String formatTagTxt2 = "soBright";
        String formatVersionTxt2 = "0.1";
        String mimeTypeTxt2 = "text/plain";
        String typeTxt2 = "FUTURE_FUN";
        Response uploadAuxFileTxt2 = UtilIT.uploadAuxFile(fileId, pathToAuxFileTxt2.toString(), formatTagTxt2, formatVersionTxt2, mimeTypeTxt2, true, typeTxt2, apiToken);
        uploadAuxFileTxt2.prettyPrint();
        uploadAuxFileTxt2.then().assertThat()
                .statusCode(OK.getStatusCode())
                //                .body("data.type", equalTo("Other Auxiliary Files"));
                .body("data.type", equalTo("FUTURE_FUN"));

        // rst aux file with public=false
        Path pathToAuxFileRst = Paths.get(java.nio.file.Files.createTempDirectory(null) + File.separator + "nonpublic.rst");
        String contentOfRst = "Nonpublic stuff in here.";
        java.nio.file.Files.write(pathToAuxFileRst, contentOfRst.getBytes());
        String formatTagRst = "nonPublic";
        String formatVersionRst = "0.1";
        String mimeTypeRst = "text/plain";
        Response uploadAuxFileRst = UtilIT.uploadAuxFile(fileId, pathToAuxFileRst.toString(), formatTagRst, formatVersionRst, mimeTypeRst, false, null, apiToken);
        uploadAuxFileRst.prettyPrint();
        uploadAuxFileRst.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.type", equalTo(null))
                .body("data.contentType", equalTo("text/plain"));

        // file with an origin
        Path pathToAuxFileOrigin1 = Paths.get(java.nio.file.Files.createTempDirectory(null) + File.separator + "file1.txt");
        String contentOfOrigin1 = "This file has an origin.";
        java.nio.file.Files.write(pathToAuxFileOrigin1, contentOfOrigin1.getBytes());
        String formatTagOrigin1 = "origin1";
        String formatVersionOrigin1 = "0.1";
        String mimeTypeOrigin1 = "text/plain";
        String typeOrigin1 = "someType";
        String origin1 = "myApp";
        Response uploadAuxFileOrigin1 = UtilIT.uploadAuxFile(fileId, pathToAuxFileOrigin1.toString(), formatTagOrigin1, formatVersionOrigin1, mimeTypeOrigin1, true, typeOrigin1, origin1, apiToken);
        uploadAuxFileOrigin1.prettyPrint();
        uploadAuxFileOrigin1.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.type", equalTo("someType"))
                .body("data.contentType", equalTo("text/plain"));

        // file with no MIME type
        Path pathToAuxFileNoMimeType1 = Paths.get(java.nio.file.Files.createTempDirectory(null) + File.separator + "file1.md");
        String contentOfNoMimeType1 = "This file has no MIME type (content type).";
        java.nio.file.Files.write(pathToAuxFileNoMimeType1, contentOfNoMimeType1.getBytes());
        String formatTagNoMimeType1 = "noMimeType1";
        String formatVersionNoMimeType1 = "0.1";
        String mimeTypeNoMimeType1 = null;
        String typeNoMimeType1 = "someType";
        Response uploadAuxFileNoMimeType1 = UtilIT.uploadAuxFile(fileId, pathToAuxFileNoMimeType1.toString(), formatTagNoMimeType1, formatVersionNoMimeType1, mimeTypeNoMimeType1, true, typeNoMimeType1, null, apiToken);
        uploadAuxFileNoMimeType1.prettyPrint();
        uploadAuxFileNoMimeType1.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.type", equalTo("someType"))
                // "text/plain" was detected by Tika
                .body("data.contentType", equalTo("text/plain"));

        // Download JSON aux file.
        Response downloadAuxFileJson = UtilIT.downloadAuxFile(fileId, formatTagJson, formatVersionJson, apiToken);
        downloadAuxFileJson.then().assertThat().statusCode(OK.getStatusCode());
        assertEquals("attachment; filename=\"data.tab.dpJson_0.1.json\"", downloadAuxFileJson.header("Content-disposition"));

        // Download XML aux file.
        Response downloadAuxFileXml = UtilIT.downloadAuxFile(fileId, formatTagXml, formatVersionXml, apiToken);
        downloadAuxFileXml.then().assertThat().statusCode(OK.getStatusCode());
        assertEquals("attachment; filename=\"data.tab.dpXml_0.1.xml\"", downloadAuxFileXml.header("Content-disposition"));

        // Download PDF aux file.
        Response downloadAuxFilePdf = UtilIT.downloadAuxFile(fileId, formatTagPdf, formatVersionPdf, apiToken);
        downloadAuxFilePdf.then().assertThat().statusCode(OK.getStatusCode());
        assertEquals("attachment; filename=\"data.tab.dpPdf_0.1.pdf\"", downloadAuxFilePdf.header("Content-disposition"));

        // Download Markdown aux file.
        Response downloadAuxFileMd = UtilIT.downloadAuxFile(fileId, formatTagMd, formatVersionMd, apiToken);
        downloadAuxFileMd.then().assertThat().statusCode(OK.getStatusCode());
        // No file extenstion here because Tika's getDefaultMimeTypes doesn't include "text/markdown".
        // Note: browsers seem to add ".bin" ("myfile.bin") rather than no extension ("myfile").
        assertEquals("attachment; filename=\"data.tab.README_0.1\"", downloadAuxFileMd.header("Content-disposition"));

        // Download Markdown aux file with no MIME type given
        Response downloadAuxFileNoMime1 = UtilIT.downloadAuxFile(fileId, formatTagNoMimeType1, formatVersionNoMimeType1, apiToken);
        downloadAuxFileNoMime1.then().assertThat().statusCode(OK.getStatusCode());
        assertEquals("attachment; filename=\"data.tab.noMimeType1_0.1.txt\"", downloadAuxFileNoMime1.header("Content-disposition"));

        Response createUserNoPrivs = UtilIT.createRandomUser();
        createUserNoPrivs.then().assertThat().statusCode(OK.getStatusCode());
        String apiTokenNoPrivs = UtilIT.getApiTokenFromResponse(createUserNoPrivs);

        // This fails because the dataset hasn't been published.
        Response failToDownloadAuxFileJson = UtilIT.downloadAuxFile(fileId, formatTagJson, formatVersionJson, apiTokenNoPrivs);
        failToDownloadAuxFileJson.then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        Response failToDownloadAuxFileRstBeforePublish = UtilIT.downloadAuxFile(fileId, formatTagRst, formatVersionRst, apiTokenNoPrivs);
        failToDownloadAuxFileRstBeforePublish.then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        UtilIT.publishDataverseViaNativeApi(dataverseAlias, apiToken).then().assertThat().statusCode(OK.getStatusCode());

        UtilIT.publishDatasetViaNativeApi(datasetPid, "major", apiToken).then().assertThat().statusCode(OK.getStatusCode());

        // isPublic=false so publishing the dataset doesn't let a "no privs" account download the file.
        Response failToDownloadAuxFileRstAfterPublish = UtilIT.downloadAuxFile(fileId, formatTagRst, formatVersionRst, apiTokenNoPrivs);
        failToDownloadAuxFileRstAfterPublish.then().assertThat().statusCode(FORBIDDEN.getStatusCode());

        Response creatorCanDownloadNonPublicAuxFile = UtilIT.downloadAuxFile(fileId, formatTagRst, formatVersionRst, apiToken);
        creatorCanDownloadNonPublicAuxFile.then().assertThat().statusCode(OK.getStatusCode());

        // This succeeds now that the dataset has been published.
        Response failToDownloadAuxFileJsonPostPublish = UtilIT.downloadAuxFile(fileId, formatTagJson, formatVersionJson, apiTokenNoPrivs);
        failToDownloadAuxFileJsonPostPublish.then().assertThat().statusCode(OK.getStatusCode());

        Response listAuxFilesOrigin1 = UtilIT.listAuxFilesByOrigin(fileId, origin1, apiToken);
        listAuxFilesOrigin1.prettyPrint();
        listAuxFilesOrigin1.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data[0].formatTag", equalTo("origin1"))
                .body("data[0].formatVersion", equalTo("0.1"))
                .body("data[0].fileSize", equalTo(24))
                .body("data[0].contentType", equalTo("text/plain"))
                .body("data[0].isPublic", equalTo(true))
                .body("data[0].type", equalTo("someType"));
        
        Response listAllAuxFiles = UtilIT.listAllAuxFiles(fileId, apiToken);
        listAllAuxFiles.prettyPrint();
        listAllAuxFiles.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.size()", equalTo(9));


        Response deleteAuxFileOrigin1 = UtilIT.deleteAuxFile(fileId, formatTagOrigin1, formatVersionOrigin1, apiToken);
        deleteAuxFileOrigin1.prettyPrint();
        deleteAuxFileOrigin1.then().assertThat().statusCode(OK.getStatusCode());

        Response listAuxFilesOrigin1NowGone = UtilIT.listAuxFilesByOrigin(fileId, origin1, apiToken);
        listAuxFilesOrigin1NowGone.prettyPrint();
        listAuxFilesOrigin1NowGone.then().assertThat().statusCode(NOT_FOUND.getStatusCode());

    }
}
