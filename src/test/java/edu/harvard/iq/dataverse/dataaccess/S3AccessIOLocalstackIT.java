package edu.harvard.iq.dataverse.dataaccess;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.testing.Tags;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.localstack.LocalStackContainer;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

// https://java.testcontainers.org/modules/localstack/
@Tag(Tags.INTEGRATION_TEST)
@Tag(Tags.USES_TESTCONTAINERS)
@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(MockitoExtension.class)
class S3AccessIOLocalstackIT {

    @BeforeAll
    static void setUp() {
        System.setProperty(staticFiles + "access-key", localstack.getAccessKey());
        System.setProperty(staticFiles + "secret-key", localstack.getSecretKey());
        System.setProperty(staticFiles + "custom-endpoint-url", localstack.getEndpoint().toString());
        System.setProperty(staticFiles + "custom-endpoint-region", localstack.getRegion());
        System.setProperty(staticFiles + "bucket-name", bucketName);

        s3 = AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                localstack.getEndpoint().toString(),
                                localstack.getRegion()
                        )
                )
                .withCredentials(
                        new AWSStaticCredentialsProvider(
                                new BasicAWSCredentials(localstack.getAccessKey(), localstack.getSecretKey())
                        )
                )
                .build();
        s3.createBucket(bucketName);
    }

    static final String storageDriverId = "si1";
    static final String staticFiles = "dataverse.files." + storageDriverId + ".";
    static final String bucketName = "bucket-" + UUID.randomUUID().toString();
    static AmazonS3 s3 = null;

    static DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:2.3.2");
    @Container
    static LocalStackContainer localstack = new LocalStackContainer(localstackImage)
            .withServices(S3);

    //new S3AccessIO<>(dvObject, req, storageDriverId);
    @Test
    void test1() {
        DvObject dvObject = new Dataset();
        dvObject.setProtocol("doi");
        dvObject.setAuthority("10.5072/FK2");
        dvObject.setIdentifier("ABC123");
        DataAccessRequest req = null;
        S3AccessIO s3AccessIO = new S3AccessIO<>(dvObject, req, storageDriverId);
        String textIn = "Hello";
        InputStream inputStream = new ByteArrayInputStream(textIn.getBytes());
        // Without this temp directory, saveInputStream fails
        String tempDirPath = "/tmp/dataverse/temp";
        try {
            Files.createDirectories(Paths.get(tempDirPath));
        } catch (IOException ex) {
            System.out.println("failed to create " + tempDirPath + ": " + ex);
        }
        try {
            s3AccessIO.saveInputStream(inputStream);
            System.out.println("save complete!");
        } catch (IOException ex) {
            System.out.println("saveInputStream exception: " + ex);
        }

        String textOut = null;
        try {
            textOut = new Scanner(s3AccessIO.getInputStream()).useDelimiter("\\A").next();
        } catch (IOException ex) {
        }
        assertEquals(textIn, textOut);
    }

    // testing a specific constructor
    @Test
    void test2() {
        Dataset dataset = new Dataset();
        dataset.setProtocol("doi");
        dataset.setAuthority("10.5072/FK2");
        dataset.setIdentifier("ABC123");
        String sid = sid = bucketName + dataset.getAuthorityForFileStorage() + "/" + dataset.getIdentifierForFileStorage() + "/" + FileUtil.generateStorageIdentifier();
        S3AccessIO<DataFile> s3io = new S3AccessIO<DataFile>(sid, storageDriverId);
    }

    // just to test this: saveInputStream exception: java.io.IOException: ERROR: s3 not initialised
    @Test
    void test3() {
        DvObject dvObject = new Dataset();
        dvObject.setProtocol("doi");
        dvObject.setAuthority("10.5072/FK2");
        dvObject.setIdentifier("ABC123");
        DataAccessRequest req = null;
        AmazonS3 nullAmazonS3 = null;
        S3AccessIO s3AccessIO = new S3AccessIO<>(dvObject, req, nullAmazonS3, storageDriverId);
        InputStream inputStream = null;
        try {
            s3AccessIO.saveInputStream(inputStream);
            System.out.println("save complete!");
        } catch (IOException ex) {
            System.out.println("saveInputStream exception: " + ex);
        }
    }

    @Test
    void test4() {
        DvObject dvObject = new DataFile();
        dvObject.setProtocol("doi");
        dvObject.setAuthority("10.5072/FK2");
        dvObject.setIdentifier("ABC123");
        DataAccessRequest req = null;
        S3AccessIO s3AccessIO = new S3AccessIO<>(dvObject, req, storageDriverId);
        InputStream inputStream = null;
        try {
            s3AccessIO.saveInputStream(inputStream);
            System.out.println("save complete!");
        } catch (IOException ex) {
            System.out.println("saveInputStream exception: " + ex);
        }
    }
}
