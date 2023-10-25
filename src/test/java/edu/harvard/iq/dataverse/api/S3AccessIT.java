package edu.harvard.iq.dataverse.api;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.HeadBucketRequest;
import com.amazonaws.services.s3.model.HeadBucketResult;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.List;
import java.util.logging.Logger;
import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.startsWith;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This test requires services spun up in Docker.
 */
public class S3AccessIT {

    private static final Logger logger = Logger.getLogger(S3AccessIT.class.getCanonicalName());

    public enum TypesOfS3 {
        MINIO,
        LOCALSTACK
    };

    static final String accessKey = "minioadmin";
    static final String secretKey = "minioadmin";
    static final String bucketName = "mybucket";
    static String driverId;
    static String driverLabel;
    static AmazonS3 s3 = null;

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = UtilIT.getRestAssuredBaseUri();

        TypesOfS3 typeToTest = TypesOfS3.LOCALSTACK;
        typeToTest = TypesOfS3.MINIO;

        switch (typeToTest) {
            case LOCALSTACK -> {
                driverId = "localstack1";
                driverLabel = "LocalStack";
                s3 = AmazonS3ClientBuilder.standard()
                        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                        .withEndpointConfiguration(new EndpointConfiguration("s3.localhost.localstack.cloud:4566", Regions.US_EAST_2.getName())).build();
            }
            case MINIO -> {
                driverId = "minio1";
                driverLabel = "MinIO";
                s3 = AmazonS3ClientBuilder.standard()
                        // https://stackoverflow.com/questions/72205086/amazonss3client-throws-unknownhostexception-if-attempting-to-connect-to-a-local
                        .withPathStyleAccessEnabled(Boolean.TRUE)
                        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                        .withEndpointConfiguration(new EndpointConfiguration("http://localhost:9000", Regions.US_EAST_1.getName())).build();
//                String location = s3.getBucketLocation(bucketName);
//                if (location != "US") {
//                    Bucket bucket = s3.createBucket(bucketName);
//                }
            }
        }
        System.out.println("buckets before attempting to create " + bucketName);
        for (Bucket bucket : s3.listBuckets()) {
            System.out.println("bucket: " + bucket);
        }

        // create bucket if it doesn't exist
        // Note that we create the localstack bucket with conf/localstack/buckets.sh
        // because we haven't figured out how to create it properly in Java.
        // Perhaps it is missing ACLs.
        try {
            s3.headBucket(new HeadBucketRequest(bucketName));
        } catch (AmazonS3Exception ex) {
            s3.createBucket(bucketName);
        }

//        String location = s3.getBucketLocation(bucketName);
////        HeadBucketRequest headBucketRequest;
//        s3.headBucket(headBucketRequest);
//        if (location != null && !"US".equals(location)) {
//            System.out.println("Creating bucket. Location was " + location);
//            Bucket createdBucket = s3.createBucket(bucketName);
//            System.out.println("created bucket: " + createdBucket);
//        }
//        System.out.println("buckets after creating " + bucketName);
//        for (Bucket bucket : s3.listBuckets()) {
//            System.out.println("bucket: " + bucket);
//        }
    }

    @Test
    public void testAddDataFileS3Prefix() {
        Response createSuperuser = UtilIT.createRandomUser();
        String superuserApiToken = UtilIT.getApiTokenFromResponse(createSuperuser);
        String superusername = UtilIT.getUsernameFromResponse(createSuperuser);
        UtilIT.makeSuperUser(superusername);
        Response storageDrivers = listStorageDrivers(superuserApiToken);
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
        String username = UtilIT.getUsernameFromResponse(createUser);
        String apiToken = UtilIT.getApiTokenFromResponse(createUser);

        Response createDataverseResponse = UtilIT.createRandomDataverse(apiToken);
        createDataverseResponse.prettyPrint();
        String dataverseAlias = UtilIT.getAliasFromResponse(createDataverseResponse);

        Response originalStorageDriver = getStorageDriver(dataverseAlias, superuserApiToken);
        originalStorageDriver.prettyPrint();
        originalStorageDriver.then().assertThat()
                .body("data.message", equalTo("undefined"))
                .statusCode(200);

        Response setStorageDriverToS3 = setStorageDriver(dataverseAlias, driverLabel, superuserApiToken);
        setStorageDriverToS3.prettyPrint();
        setStorageDriverToS3.then().assertThat()
                .statusCode(200);

        Response updatedStorageDriver = getStorageDriver(dataverseAlias, superuserApiToken);
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
        Assertions.assertEquals(driverId + "://" + bucketName + ":" + keyInDataverse, storageIdentifier);

        for (Bucket bucket : s3.listBuckets()) {
            System.out.println("bucket: " + bucket);
        }

//        List<S3ObjectSummary> summaries = s3.listObjects(bucketName).getObjectSummaries();
//        for (S3ObjectSummary summary : summaries) {
//            System.out.println("summary: " + summary);
//            /**
//             * summary: S3ObjectSummary{bucketName='mybucket',
//             * key='10.5072/FK2/6MGSJD/18b631645ef-4c6a6c2d49f8',
//             * eTag='60b725f10c9c85c70d97880dfe8191b3', size=2, lastModified=Tue
//             * Oct 24 19:08:06 UTC 2023, storageClass='STANDARD', owner=S3Owner
//             * [name=webfile,id=75aa57f09aa0c8caeab4f8c24e99d10f8e7faeebf76c078efc7c6caea54ba06a]}
//             */
//        }
        String keyInS3 = datasetStorageIdentifier + "/" + keyInDataverse;
        String s3Object = s3.getObjectAsString(bucketName, keyInS3);
        System.out.println("s3Object: " + s3Object);

        // The file uploaded above only contains the character "a".
        assertEquals("a".trim(), s3Object.trim());

        Response deleteFile = UtilIT.deleteFileApi(Integer.parseInt(fileId), apiToken);
        deleteFile.prettyPrint();
        deleteFile.then().assertThat().statusCode(200);

        AmazonS3Exception expectedException = null;
        try {
            s3.getObjectAsString(bucketName, keyInS3);
        } catch (AmazonS3Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
        // 404 because the file has been sucessfully deleted
        assertEquals(404, expectedException.getStatusCode());

    }

    //TODO: move these into UtilIT. They are here for now to avoid merge conflicts
    static Response listStorageDrivers(String apiToken) {
        return given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/admin/dataverse/storageDrivers");
    }

    static Response getStorageDriver(String dvAlias, String apiToken) {
        return given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken)
                .get("/api/admin/dataverse/" + dvAlias + "/storageDriver");
    }

    static Response setStorageDriver(String dvAlias, String label, String apiToken) {
        return given()
                .header(UtilIT.API_TOKEN_HTTP_HEADER, apiToken)
                .body(label)
                .put("/api/admin/dataverse/" + dvAlias + "/storageDriver");
    }

}
