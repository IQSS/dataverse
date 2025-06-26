package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetLock.Reason;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.util.bagit.BagGenerator;
import edu.harvard.iq.dataverse.util.bagit.OREMap;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectAttributesRequest;
import software.amazon.awssdk.services.s3.model.GetObjectAttributesResponse;
import software.amazon.awssdk.services.s3.model.ObjectAttributes;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

@RequiredPermissions(Permission.PublishDataset)
public class S3SubmitToArchiveCommand extends AbstractSubmitToArchiveCommand {

    @Resource(name = "java:comp/env/concurrent/s3UploadExecutor")
    private ManagedExecutorService executorService;

    private static final Logger logger = Logger.getLogger(S3SubmitToArchiveCommand.class.getName());
    private static final String S3_CONFIG = ":S3ArchiverConfig";

    private static final Config config = ConfigProvider.getConfig();
    protected S3AsyncClient s3 = null;
    private S3TransferManager tm = null;
    private String spaceName = null;
    protected String bucketName = null;

    public S3SubmitToArchiveCommand(DataverseRequest aRequest, DatasetVersion version) {
        super(aRequest, version);
    }

    @Override
    public WorkflowStepResult performArchiveSubmission(DatasetVersion dv, ApiToken token,
            Map<String, String> requestedSettings) {
        logger.fine("In S3SubmitToArchiveCommand...");
        JsonObject configObject = null;

        try {
            configObject = JsonUtil.getJsonObject(requestedSettings.get(S3_CONFIG));
            logger.fine("Config: " + configObject);
            bucketName = configObject.getString("s3_bucket_name", null);
        } catch (Exception e) {
            logger.warning("Unable to parse " + S3_CONFIG + " setting as a Json object");
        }
        if (configObject != null && bucketName != null) {

            createClient(configObject);

            // Set a failure status that will be updated if we succeed
            JsonObjectBuilder statusObject = Json.createObjectBuilder();
            statusObject.add(DatasetVersion.ARCHIVAL_STATUS, DatasetVersion.ARCHIVAL_STATUS_FAILURE);
            statusObject.add(DatasetVersion.ARCHIVAL_STATUS_MESSAGE, "Bag not transferred");

            try {

                Dataset dataset = dv.getDataset();
                if (dataset.getLockFor(Reason.finalizePublication) == null) {

                    spaceName = getSpaceName(dataset);
                    String dataciteXml = getDataCiteXml(dv);
                 // Add datacite.xml file
                    String dcKey = spaceName + "/" + getDataCiteFileName(spaceName, dv) + ".xml";

                    PutObjectRequest putRequest = PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(dcKey)
                            .build();

                    CompletableFuture<PutObjectResponse> putFuture = s3.putObject(putRequest, 
                            AsyncRequestBody.fromString(dataciteXml, StandardCharsets.UTF_8));

                    // Wait for the put operation to complete
                    PutObjectResponse putResponse = putFuture.join();

                    if (!putResponse.sdkHttpResponse().isSuccessful()) {
                        logger.warning("Could not write datacite xml to S3");
                        return new Failure("S3 Archiver failed writing datacite xml file");
                    }

                    // Store BagIt file
                    String fileName = getFileName(spaceName, dv);

                    String bagKey = spaceName + "/" + fileName + ".zip";
                    // Add BagIt ZIP file
                    // Google uses MD5 as one way to verify the
                    // transfer

                    // Generate bag
                    BagGenerator bagger = new BagGenerator(new OREMap(dv, false), dataciteXml);
                    bagger.setAuthenticationKey(token.getTokenString());
                    if (bagger.generateBag(fileName, false)) {
                        File bagFile = bagger.getBagFile(fileName);

                        UploadFileRequest uploadFileRequest = UploadFileRequest.builder()
                                .putObjectRequest(req -> req.bucket(bucketName).key(bagKey)).source(bagFile.toPath())
                                .build();

                        FileUpload fileUpload = tm.uploadFile(uploadFileRequest);

                        CompletedFileUpload uploadResult = fileUpload.completionFuture().join();

                        if (uploadResult.response().sdkHttpResponse().isSuccessful()) {
                            logger.fine("S3 Submission step: Content Transferred");

                            statusObject.add(DatasetVersion.ARCHIVAL_STATUS, DatasetVersion.ARCHIVAL_STATUS_SUCCESS);
                            statusObject.add(DatasetVersion.ARCHIVAL_STATUS_MESSAGE,
                                    String.format("https://%s.s3.amazonaws.com/%s", bucketName, bagKey));
                        } else {
                            logger.severe("Error sending file to S3: " + fileName);
                            return new Failure("Error in transferring Bag file to S3",
                                    "S3 Submission Failure: incomplete transfer");
                        }
                    } else {
                        logger.warning("Could not write local Bag file " + fileName);
                        return new Failure("S3 Archiver fail writing temp local bag");
                    }

                } else {
                    logger.warning(
                            "S3 Archiver Submision Workflow aborted: Dataset locked for publication/pidRegister");
                    return new Failure("Dataset locked");
                }
            } catch (Exception e) {
                logger.warning(e.getLocalizedMessage());
                e.printStackTrace();
                return new Failure("S3 Archiver Submission Failure",
                        e.getLocalizedMessage() + ": check log for details");

            } finally {
                if (tm != null) {
                    tm.close();
                }
                dv.setArchivalCopyLocation(statusObject.build().toString());
            }
            return WorkflowStepResult.OK;
        } else {
            return new Failure(
                    "S3 Submission not configured - no \":S3ArchivalProfile\"  and/or \":S3ArchivalConfig\" or no bucket-name defined in config.");
        }
    }

    protected String getDataCiteFileName(String spaceName, DatasetVersion dv) {
        return spaceName + "_datacite.v" + dv.getFriendlyVersionNumber();
    }

    protected String getFileName(String spaceName, DatasetVersion dv) {
        return spaceName + ".v" + dv.getFriendlyVersionNumber();
    }

    protected String getSpaceName(Dataset dataset) {
        if (spaceName == null) {
            spaceName = dataset.getGlobalId().asString().replace(':', '-').replace('/', '-').replace('.', '-')
                    .toLowerCase();
        }
        return spaceName;
    }

    private S3AsyncClient createClient(JsonObject configObject) {

        // Create a builder for the S3AsyncClient
        S3AsyncClientBuilder s3CB = S3AsyncClient.builder();

        // Create a custom HTTP client with the desired pool size
        Integer poolSize = configObject.getInt("connection-pool-size", 256);
        SdkAsyncHttpClient httpClient = NettyNioAsyncHttpClient.builder().maxConcurrency(poolSize).build();

        // Apply the custom HTTP client to the S3AsyncClientBuilder
        s3CB.httpClient(httpClient);

        String s3CEUrl = configObject.getString("custom-endpoint-url", "");
        String s3CERegion = configObject.getString("custom-endpoint-region", "dataverse");

        if (!StringUtils.isBlank(s3CEUrl)) {
            s3CB.endpointOverride(java.net.URI.create(s3CEUrl));
            s3CB.region(Region.of(s3CERegion));
        }

        Boolean s3pathStyleAccess = configObject.getBoolean("path-style-access", false);
        s3CB.forcePathStyle(s3pathStyleAccess);

        String profile = configObject.getString("profile", "default");
        AwsCredentialsProvider profileCredentials = ProfileCredentialsProvider.create(profile);

        String accessKey = config.getOptionalValue("dataverse.s3archiver.access-key", String.class).orElse("");
        String secretKey = config.getOptionalValue("dataverse.s3archiver.secret-key", String.class).orElse("");
        AwsCredentialsProvider staticCredentials = StaticCredentialsProvider
                .create(AwsBasicCredentials.create(accessKey, secretKey));

        AwsCredentialsProvider credentialsProviderChain = AwsCredentialsProviderChain.builder()
                .addCredentialsProvider(profileCredentials).addCredentialsProvider(staticCredentials)
                .addCredentialsProvider(DefaultCredentialsProvider.create()).build();

        s3CB.credentialsProvider(credentialsProviderChain);
        s3 = s3CB.build();
        // Create TransferManager
        tm = S3TransferManager.builder().s3Client(s3).build();
        return s3;
    }
}