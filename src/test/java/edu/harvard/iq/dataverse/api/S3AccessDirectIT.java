package edu.harvard.iq.dataverse.api;

import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.math.NumberUtils;
import org.junit.jupiter.api.Test;

public class S3AccessDirectIT {

    @Test
    public void testS3DirectUpload() {
        // TODO: remove all these constants
        RestAssured.baseURI = "https://demo.dataverse.org";
        String apiToken = "";
        String datasetPid = "doi:10.70122/FK2/UBWSJU";
        String datasetId = "2106131";
        long size = 1000000000l;

        Response getUploadUrls = getUploadUrls(datasetPid, size, apiToken);
        getUploadUrls.prettyPrint();
        getUploadUrls.then().assertThat().statusCode(200);

        String url = JsonPath.from(getUploadUrls.asString()).getString("data.url");
        String partSize = JsonPath.from(getUploadUrls.asString()).getString("data.partSize");
        String storageIdentifier = JsonPath.from(getUploadUrls.asString()).getString("data.storageIdentifier");
        System.out.println("url: " + url);
        System.out.println("partSize: " + partSize);
        System.out.println("storageIdentifier: " + storageIdentifier);

        System.out.println("uploading file via direct upload");
        String decodedUrl = null;
        try {
            decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ex) {
        }

        InputStream inputStream = new ByteArrayInputStream("bumble".getBytes(StandardCharsets.UTF_8));
        Response uploadFileDirect = uploadFileDirect(decodedUrl, inputStream);
        uploadFileDirect.prettyPrint();
        uploadFileDirect.then().assertThat().statusCode(200);

        // TODO: Use MD5 or whatever Dataverse is configured for and
        // actually calculate it.
        String jsonData = """
{
    "description": "My description.",
    "directoryLabel": "data/subdir1",
    "categories": [
      "Data"
    ],
    "restrict": "false",
    "storageIdentifier": "%s",
    "fileName": "file1.txt",
    "mimeType": "text/plain",
    "checksum": {
      "@type": "SHA-1",
      "@value": "123456"
    }
}
""".formatted(storageIdentifier);
        Response addRemoteFile = UtilIT.addRemoteFile(datasetId, jsonData, apiToken);
        addRemoteFile.prettyPrint();
        addRemoteFile.then().assertThat()
                .statusCode(200);
    }

    static Response getUploadUrls(String idOrPersistentIdOfDataset, long sizeInBytes, String apiToken) {
        String idInPath = idOrPersistentIdOfDataset; // Assume it's a number.
        String optionalQueryParam = ""; // If idOrPersistentId is a number we'll just put it in the path.
        if (!NumberUtils.isCreatable(idOrPersistentIdOfDataset)) {
            idInPath = ":persistentId";
            optionalQueryParam = "&persistentId=" + idOrPersistentIdOfDataset;
        }
        RequestSpecification requestSpecification = given();
        if (apiToken != null) {
            requestSpecification = given()
                    .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken);
        }
        return requestSpecification.get("/api/datasets/" + idInPath + "/uploadurls?size=" + sizeInBytes + optionalQueryParam);
    }

    static Response uploadFileDirect(String url, InputStream inputStream) {
        return given()
                .header("x-amz-tagging", "dv-state=temp")
                .body(inputStream)
                .put(url);
    }

}
