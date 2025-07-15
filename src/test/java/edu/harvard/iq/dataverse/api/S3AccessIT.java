package edu.harvard.iq.dataverse.api;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.OK;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.json.JsonObject;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * This test requires LocalStack and Minio to be running. Developers can use our
 * docker-compose file, which has all the necessary configuration.
 */
public class S3AccessIT {

    private static final Logger logger = Logger.getLogger(S3AccessIT.class.getCanonicalName());

    static final String BUCKET_NAME = "mybucket";
    static S3Client s3localstack = null;
    static S3Client s3minio = null;

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

        // At least in when spun up by our docker-compose file, the creds don't matter for LocalStack.
        String accessKeyLocalStack = "whatever";
        String secretKeyLocalStack = "not used";

        s3localstack = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyLocalStack, secretKeyLocalStack)))
                .endpointOverride(URI.create("http://s3.localhost.localstack.cloud:4566"))
                .region(Region.US_EAST_2)
                .build();

        String accessKeyMinio = "4cc355_k3y";
        String secretKeyMinio = "s3cr3t_4cc355_k3y";
        s3minio = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyMinio, secretKeyMinio)))
                .endpointOverride(URI.create("http://localhost:9000"))
                .region(Region.US_EAST_1)
                .forcePathStyle(true)
                .build();

        // create bucket if it doesn't exist
        try {
            s3localstack.headBucket(HeadBucketRequest.builder().bucket(BUCKET_NAME).build());
        } catch (NoSuchBucketException ex) {
            s3localstack.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
        }

        try {
            s3minio.headBucket(HeadBucketRequest.builder().bucket(BUCKET_NAME).build());
        } catch (NoSuchBucketException ex) {
            try {
                CreateBucketResponse createBucketResponse = s3minio.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
                if (createBucketResponse.sdkHttpResponse().isSuccessful()) {
                    System.out.println("Bucket created successfully");
                } else {
                    System.err.println("Failed to create bucket: " + createBucketResponse.sdkHttpResponse().statusCode());
                }
            } catch (S3Exception e) {
                System.err.println("Error creating bucket: " + e.getMessage());
            }
        }
    }

    /**
     * We're using MinIO for testing non-direct upload.
     */
    @Test
    public void testNonDirectUpload() {
        String driverId = "minio1";
        String driverLabel = "MinIO";

        Response createSuperuser = UtilIT.createRandomUser();
        createSuperuser.then().assertThat().statusCode(200);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        String superusername = UtilIT.getUsernameFromResponse(createSuperuser);
        UtilIT.makeSuperUser(superusername).then().assertThat().statusCode(200);
        Response storageDrivers = UtilIT.listStorageDrivers(superuserApiToken);
        storageDrivers.prettyPrint();
        // TODO where is "Local/local" coming from?
        String drivers = """
{
    "status": "OK",
    "data": {
        "LocalStack": "localstack1",
        "MinIO": "minio1",
        "Local": "local",
        "Filesystem": "file1"
    }
}""";

        //create user who will make a dataverse/dataset
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(200);
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response originalStorageDriver = UtilIT.getStorageDriver(dataverseAlias, superuserApiToken);
        originalStorageDriver.prettyPrint();
        originalStorageDriver.then().assertThat()
                .body("data.message", equalTo("undefined"))
                .statusCode(200);

        Response setStorageDriverToS3 = UtilIT.setStorageDriver(dataverseAlias, driverLabel, superuserApiToken);
        setStorageDriverToS3.prettyPrint();
        setStorageDriverToS3.then().assertThat()
                .statusCode(200);

        Response updatedStorageDriver = UtilIT.getStorageDriver(dataverseAlias, superuserApiToken);
        updatedStorageDriver.prettyPrint();
        updatedStorageDriver.then().assertThat()
                .statusCode(200);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        createDatasetResponse.then().assertThat().statusCode(201);
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        String datasetPid = JsonPath.from(createDatasetResponse.body().asString()).getString("data.persistentId");
        String datasetStorageIdentifier = datasetPid.substring(4);

        Response getDatasetMetadata = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetMetadata.prettyPrint();
        getDatasetMetadata.then().assertThat().statusCode(200);

        //upload a tabular file via native, check storage id prefix for driverId
        String pathToFile = "scripts/search/data/tabular/1char";
        Response addFileResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);
        addFileResponse.prettyPrint();
        addFileResponse.then().assertThat()
                .statusCode(200)
                .body("data.files[0].dataFile.storageIdentifier", startsWith(driverId + "://"));

        String fileId = JsonPath.from(addFileResponse.body().asString()).getString("data.files[0].dataFile.id");

        Response getfileMetadata = UtilIT.getFileData(fileId, apiToken);
        getfileMetadata.prettyPrint();
        getfileMetadata.then().assertThat().statusCode(200);

        String storageIdentifier = JsonPath.from(addFileResponse.body().asString()).getString("data.files[0].dataFile.storageIdentifier");
        String keyInDataverse = storageIdentifier.split(":")[2];
        Assertions.assertEquals(driverId + "://" + BUCKET_NAME + ":" + keyInDataverse, storageIdentifier);

        String keyInS3 = datasetStorageIdentifier + "/" + keyInDataverse;
        String s3Object = null;
        try {
            ResponseInputStream<GetObjectResponse> s3ObjectResponse = s3minio.getObject(GetObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(keyInS3)
                    .build());
            // Read the content of the object into a string
            s3Object = new String(s3ObjectResponse.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("s3Object: " + s3Object);

            // The file uploaded above only contains the character "a".
            assertEquals("a".trim(), s3Object.trim());
        } catch (S3Exception ex) {
            fail("Failed to get object from S3: " + ex.getMessage());
        } catch (IOException ex) {
            fail("Failed to read S3 object content: " + ex.getMessage());
        }

        System.out.println("non-direct download...");
        Response downloadFile = UtilIT.downloadFile(Integer.valueOf(fileId), apiToken);
        downloadFile.then().assertThat().statusCode(200);

        String contentsOfDownloadedFile = downloadFile.getBody().asString();
        assertEquals("a\n", contentsOfDownloadedFile);

        Response deleteFile = UtilIT.deleteFileApi(Integer.parseInt(fileId), apiToken);
        deleteFile.prettyPrint();
        deleteFile.then().assertThat().statusCode(200);

        S3Exception expectedException = null;
        try {
            ResponseInputStream<GetObjectResponse> s3ObjectResponse = s3minio.getObject(GetObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(keyInS3)
                    .build());
            // Read the content of the object into a string
            String s3ObjectString = new String(s3ObjectResponse.readAllBytes(), StandardCharsets.UTF_8);
        } catch (S3Exception ex) {
            expectedException = ex;
        } catch (IOException ex) {
            // Handle IO exception
            logger.log(Level.SEVERE, "Error reading S3 object", ex);
        }
        assertNotNull(expectedException);
        // 404 because the file has been successfully deleted
        assertEquals(404, expectedException.statusCode());

    }

    /**
     * We use LocalStack to test direct upload.
     */
    @Test
    public void testDirectUpload() {
        String driverId = "localstack1";
        String driverLabel = "LocalStack";
        Response createSuperuser = UtilIT.createRandomUser();
        createSuperuser.then().assertThat().statusCode(200);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        String superusername = UtilIT.getUsernameFromResponse(createSuperuser);
        UtilIT.makeSuperUser(superusername).then().assertThat().statusCode(200);
        Response storageDrivers = UtilIT.listStorageDrivers(superuserApiToken);
        storageDrivers.prettyPrint();
        // TODO where is "Local/local" coming from?
        String drivers = """
{
    "status": "OK",
    "data": {
        "LocalStack": "localstack1",
        "MinIO": "minio1",
        "Local": "local",
        "Filesystem": "file1"
    }
}""";

        //create user who will make a dataverse/dataset
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(200);
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response originalStorageDriver = UtilIT.getStorageDriver(dataverseAlias, superuserApiToken);
        originalStorageDriver.prettyPrint();
        originalStorageDriver.then().assertThat()
                .body("data.message", equalTo("undefined"))
                .statusCode(200);

        Response setStorageDriverToS3 = UtilIT.setStorageDriver(dataverseAlias, driverLabel, superuserApiToken);
        setStorageDriverToS3.prettyPrint();
        setStorageDriverToS3.then().assertThat()
                .statusCode(200);

        Response updatedStorageDriver = UtilIT.getStorageDriver(dataverseAlias, superuserApiToken);
        updatedStorageDriver.prettyPrint();
        updatedStorageDriver.then().assertThat()
                .statusCode(200);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        createDatasetResponse.then().assertThat().statusCode(201);
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        String datasetPid = JsonPath.from(createDatasetResponse.body().asString()).getString("data.persistentId");
        String datasetStorageIdentifier = datasetPid.substring(4);

        Response getDatasetMetadata = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetMetadata.prettyPrint();
        getDatasetMetadata.then().assertThat().statusCode(200);

//        //upload a tabular file via native, check storage id prefix for driverId
//        String pathToFile = "scripts/search/data/tabular/1char";
//        Response addFileResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);
//        addFileResponse.prettyPrint();
//        addFileResponse.then().assertThat()
//                .statusCode(200)
//                .body("data.files[0].dataFile.storageIdentifier", startsWith(driverId + "://"));
//
//        String fileId = JsonPath.from(addFileResponse.body().asString()).getString("data.files[0].dataFile.id");
        long size = 1000000000l;
        Response getUploadUrls = UtilIT.getUploadUrls(datasetPid, size, apiToken);
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

        // change to localhost because LocalStack is running in a container locally
        String localhostUrl = decodedUrl.replace("http://localstack", "http://localhost");
        String contentsOfFile = "foobar";

        InputStream inputStream = new ByteArrayInputStream(contentsOfFile.getBytes(StandardCharsets.UTF_8));
        Response uploadFileDirect = UtilIT.uploadFileDirect(localhostUrl, inputStream);
        uploadFileDirect.prettyPrint();
        /*
        Direct upload to MinIO is failing with errors like this:
        <Error>
          <Code>SignatureDoesNotMatch</Code>
          <Message>The request signature we calculated does not match the signature you provided. Check your key and signing method.</Message>
          <Key>10.5072/FK2/KGFCEJ/18b8c06688c-21b8320a3ee5</Key>
          <BucketName>mybucket</BucketName>
          <Resource>/mybucket/10.5072/FK2/KGFCEJ/18b8c06688c-21b8320a3ee5</Resource>
          <RequestId>1793915CCC5BC95C</RequestId>
          <HostId>dd9025bab4ad464b049177c95eb6ebf374d3b3fd1af9251148b658df7ac2e3e8</HostId>
        </Error>
         */
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

        // "There was an error when trying to add the new file. File size must be explicitly specified when creating DataFiles with Direct Upload"
        Response addRemoteFile = UtilIT.addRemoteFile(datasetId.toString(), jsonData, apiToken);
        addRemoteFile.prettyPrint();
        addRemoteFile.then().assertThat()
                .statusCode(200);

        String fileId = JsonPath.from(addRemoteFile.asString()).getString("data.files[0].dataFile.id");
        Response getfileMetadata = UtilIT.getFileData(fileId, apiToken);
        getfileMetadata.prettyPrint();
        getfileMetadata.then().assertThat().statusCode(200);

//        String storageIdentifier = JsonPath.from(addFileResponse.body().asString()).getString("data.files[0].dataFile.storageIdentifier");
        String keyInDataverse = storageIdentifier.split(":")[2];
        Assertions.assertEquals(driverId + "://" + BUCKET_NAME + ":" + keyInDataverse, storageIdentifier);

        String keyInS3 = datasetStorageIdentifier + "/" + keyInDataverse;
        String s3Object = null;
        try {
            ResponseInputStream<GetObjectResponse> s3ObjectResponse = s3localstack.getObject(GetObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(keyInS3)
                    .build());
            // Read the content of the object into a string
            s3Object = new String(s3ObjectResponse.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("s3Object: " + s3Object);
        } catch (S3Exception ex) {
            fail("Failed to get object from S3: " + ex.getMessage());
        } catch (IOException ex) {
            fail("Failed to read S3 object content: " + ex.getMessage());
        }

//        assertEquals(contentsOfFile.trim(), s3Object.trim());
        assertEquals(contentsOfFile, s3Object);

        System.out.println("direct download...");
        Response getHeaders = UtilIT.downloadFileNoRedirect(Integer.valueOf(fileId), apiToken);
        for (Header header : getHeaders.getHeaders()) {
            System.out.println("direct download header: " + header);
        }
        getHeaders.then().assertThat().statusCode(303);

        String urlFromResponse = getHeaders.getHeader("Location");
        String localhostDownloadUrl = urlFromResponse.replace("localstack", "localhost");
        String decodedDownloadUrl = null;
        try {
            decodedDownloadUrl = URLDecoder.decode(localhostDownloadUrl, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ex) {
        }

        Response downloadFile = UtilIT.downloadFromUrl(decodedDownloadUrl);
        downloadFile.prettyPrint();
        downloadFile.then().assertThat().statusCode(200);

        String contentsOfDownloadedFile = downloadFile.getBody().asString();
        assertEquals(contentsOfFile, contentsOfDownloadedFile);

        Response getFileData = UtilIT.getFileData(fileId, apiToken);
        getFileData.prettyPrint();
        getFileData.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.label", equalTo("file1.txt"))
                .body("data.dataFile.filename", equalTo("file1.txt"))
                .body("data.dataFile.contentType", equalTo("text/plain"))
                .body("data.dataFile.filesize", equalTo(6));

        Response deleteFile = UtilIT.deleteFileApi(Integer.parseInt(fileId), apiToken);
        deleteFile.prettyPrint();
        deleteFile.then().assertThat().statusCode(200);

        S3Exception expectedException = null;
        try {
            ResponseInputStream<GetObjectResponse> s3ObjectResponse = s3minio.getObject(GetObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(keyInS3)
                    .build());
            // Read the content of the object into a string
            String s3ObjectString = new String(s3ObjectResponse.readAllBytes(), StandardCharsets.UTF_8);
        } catch (S3Exception ex) {
            expectedException = ex;
        } catch (IOException ex) {
            // Handle IO exception
            logger.log(Level.SEVERE, "Error reading S3 object", ex);
        }
        assertNotNull(expectedException);
        // 404 because the file has been successfully deleted
        assertEquals(404, expectedException.statusCode());

    }

    @Test
    public void testDirectUploadDetectStataFile() {
        String driverId = "localstack1";
        String driverLabel = "LocalStack";
        Response createSuperuser = UtilIT.createRandomUser();
        createSuperuser.then().assertThat().statusCode(200);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        String superusername = UtilIT.getUsernameFromResponse(createSuperuser);
        UtilIT.makeSuperUser(superusername).then().assertThat().statusCode(200);
        Response storageDrivers = UtilIT.listStorageDrivers(superuserApiToken);
        storageDrivers.prettyPrint();
        // TODO where is "Local/local" coming from?
        String drivers = """
{
    "status": "OK",
    "data": {
        "LocalStack": "localstack1",
        "MinIO": "minio1",
        "Local": "local",
        "Filesystem": "file1"
    }
}""";

        //create user who will make a dataverse/dataset
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(200);
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response originalStorageDriver = UtilIT.getStorageDriver(dataverseAlias, superuserApiToken);
        originalStorageDriver.prettyPrint();
        originalStorageDriver.then().assertThat()
                .body("data.message", equalTo("undefined"))
                .statusCode(200);

        Response setStorageDriverToS3 = UtilIT.setStorageDriver(dataverseAlias, driverLabel, superuserApiToken);
        setStorageDriverToS3.prettyPrint();
        setStorageDriverToS3.then().assertThat()
                .statusCode(200);

        Response updatedStorageDriver = UtilIT.getStorageDriver(dataverseAlias, superuserApiToken);
        updatedStorageDriver.prettyPrint();
        updatedStorageDriver.then().assertThat()
                .statusCode(200);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        createDatasetResponse.then().assertThat().statusCode(201);
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        String datasetPid = JsonPath.from(createDatasetResponse.body().asString()).getString("data.persistentId");
        String datasetStorageIdentifier = datasetPid.substring(4);

        Response getDatasetMetadata = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetMetadata.prettyPrint();
        getDatasetMetadata.then().assertThat().statusCode(200);

        long size = 1000000000l;
        Response getUploadUrls = UtilIT.getUploadUrls(datasetPid, size, apiToken);
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

        // change to localhost because LocalStack is running in a container locally
        String localhostUrl = decodedUrl.replace("http://localstack", "http://localhost");

        Path stataFilePath = Paths.get("scripts/search/data/tabular/stata14-auto-withstrls.dta");
        InputStream inputStream = null;
        try {
            inputStream = java.nio.file.Files.newInputStream(stataFilePath);
        } catch (IOException ex) {
            Logger.getLogger(S3AccessIT.class.getName()).log(Level.SEVERE, null, ex);
        }
        Response uploadFileDirect = UtilIT.uploadFileDirect(localhostUrl, inputStream);
        uploadFileDirect.prettyPrint();
        /*
        Direct upload to MinIO is failing with errors like this:
        <Error>
          <Code>SignatureDoesNotMatch</Code>
          <Message>The request signature we calculated does not match the signature you provided. Check your key and signing method.</Message>
          <Key>10.5072/FK2/KGFCEJ/18b8c06688c-21b8320a3ee5</Key>
          <BucketName>mybucket</BucketName>
          <Resource>/mybucket/10.5072/FK2/KGFCEJ/18b8c06688c-21b8320a3ee5</Resource>
          <RequestId>1793915CCC5BC95C</RequestId>
          <HostId>dd9025bab4ad464b049177c95eb6ebf374d3b3fd1af9251148b658df7ac2e3e8</HostId>
        </Error>
         */
        uploadFileDirect.then().assertThat().statusCode(200);

        // TODO: Use MD5 or whatever Dataverse is configured for and
        // actually calculate it.
        //
        // Note that we falsely set mimeType=application/octet-stream so that
        // later we can test file detection. The ".dta" file extension is
        // necessary for file detection to work.
        String jsonData = """
{
    "description": "My description.",
    "directoryLabel": "data/subdir1",
    "categories": [
      "Data"
    ],
    "restrict": "false",
    "storageIdentifier": "%s",
    "fileName": "stata14-auto-withstrls.dta",
    "mimeType": "application/octet-stream",
    "checksum": {
      "@type": "SHA-1",
      "@value": "123456"
    }
}
""".formatted(storageIdentifier);

        // "There was an error when trying to add the new file. File size must be explicitly specified when creating DataFiles with Direct Upload"
        Response addRemoteFile = UtilIT.addRemoteFile(datasetId.toString(), jsonData, apiToken);
        addRemoteFile.prettyPrint();
        addRemoteFile.then().assertThat()
                .statusCode(200);

        String fileId = JsonPath.from(addRemoteFile.asString()).getString("data.files[0].dataFile.id");
        Response getfileMetadata = UtilIT.getFileData(fileId, apiToken);
        getfileMetadata.prettyPrint();
        getfileMetadata.then().assertThat().statusCode(200);

        String keyInDataverse = storageIdentifier.split(":")[2];
        Assertions.assertEquals(driverId + "://" + BUCKET_NAME + ":" + keyInDataverse, storageIdentifier);

        String keyInS3 = datasetStorageIdentifier + "/" + keyInDataverse;
        // UtilIT.MAXIMUM_INGEST_LOCK_DURATION is 3 but not long enough.
        assertTrue(UtilIT.sleepForLock(datasetId.longValue(), "Ingest", apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION + 3), "Failed test if Ingest Lock exceeds max duration " + keyInS3);

        Response getFileData1 = UtilIT.getFileData(fileId, apiToken);
        getFileData1.prettyPrint();
        getFileData1.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.dataFile.originalFileName", equalTo("stata14-auto-withstrls.dta"))
                .body("data.dataFile.originalFileFormat", equalTo("application/x-stata-14"))
                .body("data.dataFile.filename", equalTo("stata14-auto-withstrls.tab"))
                .body("data.dataFile.contentType", equalTo("text/tab-separated-values"));

        Response redetectDryRun = UtilIT.redetectFileType(fileId, false, apiToken);
        redetectDryRun.prettyPrint();
        redetectDryRun.then().assertThat()
                // Tabular files can't be redetected. See discussion in
                // https://github.com/IQSS/dataverse/issues/9429
                // and the change in https://github.com/IQSS/dataverse/pull/9768
                .statusCode(BAD_REQUEST.getStatusCode());

        Response deleteFile = UtilIT.deleteFileApi(Integer.parseInt(fileId), apiToken);
        deleteFile.prettyPrint();
        deleteFile.then().assertThat().statusCode(200);

        S3Exception expectedException = null;
        try {
            ResponseInputStream<GetObjectResponse> s3ObjectResponse = s3localstack.getObject(GetObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(keyInS3)
                    .build());
            // If we reach this point, it means the object still exists
            fail("Expected S3Exception was not thrown");
        } catch (S3Exception ex) {
            expectedException = ex;
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
        assertNotNull(expectedException);
        // 404 because the file has been successfully deleted
        assertEquals(404, expectedException.statusCode());

    }

    @Test
    public void testDirectUploadWithFileCountLimit() throws JsonParseException {
        String driverId = "localstack1";
        String driverLabel = "LocalStack";
        Response createSuperuser = UtilIT.createRandomUser();
        createSuperuser.then().assertThat().statusCode(200);
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        String superusername = UtilIT.getUsernameFromResponse(createSuperuser);
        UtilIT.makeSuperUser(superusername).then().assertThat().statusCode(200);
        Response storageDrivers = UtilIT.listStorageDrivers(superuserApiToken);
        storageDrivers.prettyPrint();
        // TODO where is "Local/local" coming from?
        String drivers = """
{
    "status": "OK",
    "data": {
        "LocalStack": "localstack1",
        "MinIO": "minio1",
        "Local": "local",
        "Filesystem": "file1"
    }
}""";

        //create user who will make a dataverse/dataset
        Response createUser = UtilIT.createRandomUser();
        createUser.then().assertThat().statusCode(200);
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);
        // Update the dataverse with a datasetFileCountLimit of 1
        JsonObject data = JsonUtil.getJsonObject(createDataverseResponse.getBody().asString());
        JsonParser parser = new JsonParser();
        Dataverse dv = parser.parseDataverse(data.getJsonObject("data"));
        dv.setDatasetFileCountLimit(1);
        Response updateDataverseResponse = UtilIT.updateDataverse(dataverseAlias, dv, superuserApiToken); // only superuser can update the datasetFileCountLimit
        updateDataverseResponse.prettyPrint();
        updateDataverseResponse.then().assertThat()
                .statusCode(OK.getStatusCode())
                .body("data.effectiveDatasetFileCountLimit", equalTo(1))
                .body("data.datasetFileCountLimit", equalTo(1));

        Response originalStorageDriver = UtilIT.getStorageDriver(dataverseAlias, superuserApiToken);
        originalStorageDriver.prettyPrint();
        originalStorageDriver.then().assertThat()
                .body("data.message", equalTo("undefined"))
                .statusCode(200);

        Response setStorageDriverToS3 = UtilIT.setStorageDriver(dataverseAlias, driverLabel, superuserApiToken);
        setStorageDriverToS3.prettyPrint();
        setStorageDriverToS3.then().assertThat()
                .statusCode(200);

        Response updatedStorageDriver = UtilIT.getStorageDriver(dataverseAlias, superuserApiToken);
        updatedStorageDriver.prettyPrint();
        updatedStorageDriver.then().assertThat()
                .statusCode(200);

        Response createDatasetResponse = UtilIT.createRandomDatasetViaNativeApi(dataverseAlias, apiToken);
        createDatasetResponse.prettyPrint();
        createDatasetResponse.then().assertThat().statusCode(201);
        Integer datasetId = JsonPath.from(createDatasetResponse.body().asString()).getInt("data.id");
        String datasetPid = JsonPath.from(createDatasetResponse.body().asString()).getString("data.persistentId");

        Response getDatasetMetadata = UtilIT.nativeGet(datasetId, apiToken);
        getDatasetMetadata.prettyPrint();
        getDatasetMetadata.then().assertThat().statusCode(200);

        // -------------------------
        // Add initial file
        // -------------------------
        String pathToFile = "scripts/search/data/tabular/50by1000.dta";
        Response uploadFileResponse = UtilIT.uploadFileViaNative(datasetId.toString(), pathToFile, apiToken);
        uploadFileResponse.then().assertThat()
                .statusCode(OK.getStatusCode());
        UtilIT.sleepForLock(datasetId, null, apiToken, UtilIT.MAXIMUM_INGEST_LOCK_DURATION);

        // Get upload Urls when limit has been reached
        long size = 1000000000l;
        Response getUploadUrls = UtilIT.getUploadUrls(datasetPid, size, apiToken);
        getUploadUrls.prettyPrint();
        getUploadUrls.then().assertThat()
                .body("message", containsString(BundleUtil.getStringFromBundle("file.add.count_exceeds_limit", Collections.singletonList("1"))))
                .statusCode(BAD_REQUEST.getStatusCode());

        // Get upload Urls as superuser when limit has been reached (superuser ignores limit)
        getUploadUrls = UtilIT.getUploadUrls(datasetPid, size, superuserApiToken);
        getUploadUrls.prettyPrint();
        getUploadUrls.then().assertThat()
                .statusCode(OK.getStatusCode());
    }
}
