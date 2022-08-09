package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetLock.Reason;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.engine.command.Command;
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
import java.util.Map;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;

@RequiredPermissions(Permission.PublishDataset)
public class S3SubmitToArchiveCommand extends AbstractSubmitToArchiveCommand implements Command<DatasetVersion> {

    private static final Logger logger = Logger.getLogger(S3SubmitToArchiveCommand.class.getName());
    private static final String S3_CONFIG = ":S3ArchiverConfig";

    private static final Config config = ConfigProvider.getConfig();
    protected AmazonS3 s3 = null;
    protected TransferManager tm = null;
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

            s3 = createClient(configObject);
            tm = TransferManagerBuilder.standard().withS3Client(s3).build();
            
            //Set a failure status that will be updated if we succeed
            JsonObjectBuilder statusObject = Json.createObjectBuilder();
            statusObject.add(DatasetVersion.ARCHIVAL_STATUS, DatasetVersion.ARCHIVAL_STATUS_FAILURE);
            statusObject.add(DatasetVersion.ARCHIVAL_STATUS_MESSAGE, "Bag not transferred");
            
            try {

                Dataset dataset = dv.getDataset();
                if (dataset.getLockFor(Reason.finalizePublication) == null) {

                    spaceName = getSpaceName(dataset);
                    String dataciteXml = getDataCiteXml(dv);
                    try (ByteArrayInputStream dataciteIn = new ByteArrayInputStream(dataciteXml.getBytes("UTF-8"))) {
                        // Add datacite.xml file
                        ObjectMetadata om = new ObjectMetadata();
                        om.setContentLength(dataciteIn.available());
                        String dcKey = spaceName + "/" + getDataCiteFileName(spaceName, dv) + ".xml";
                        tm.upload(new PutObjectRequest(bucketName, dcKey, dataciteIn, om)).waitForCompletion();
                        om = s3.getObjectMetadata(bucketName, dcKey);
                        if (om == null) {
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

                            try (FileInputStream in = new FileInputStream(bagFile)) {
                                om = new ObjectMetadata();
                                om.setContentLength(bagFile.length());

                                tm.upload(new PutObjectRequest(bucketName, bagKey, in, om)).waitForCompletion();
                                om = s3.getObjectMetadata(bucketName, bagKey);

                                if (om == null) {
                                    logger.severe("Error sending file to S3: " + fileName);
                                    return new Failure("Error in transferring Bag file to S3",
                                            "S3 Submission Failure: incomplete transfer");
                                }
                            } catch (RuntimeException rte) {
                                logger.severe("Error creating Bag during S3 archiving: " + rte.getMessage());
                                return new Failure("Error in generating Bag",
                                        "S3 Submission Failure: archive file not created");
                            }

                            logger.fine("S3 Submission step: Content Transferred");

                            // Document the location of dataset archival copy location (actually the URL
                            // where you can
                            // view it as an admin)

                            // Unsigned URL - gives location but not access without creds
                            statusObject.add(DatasetVersion.ARCHIVAL_STATUS, DatasetVersion.ARCHIVAL_STATUS_SUCCESS);
                            statusObject.add(DatasetVersion.ARCHIVAL_STATUS_MESSAGE, s3.getUrl(bucketName, bagKey).toString());
                        } else {
                            logger.warning("Could not write local Bag file " + fileName);
                            return new Failure("S3 Archiver fail writing temp local bag");
                        }

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

    private AmazonS3 createClient(JsonObject configObject) {
        // get a standard client, using the standard way of configuration the
        // credentials, etc.
        AmazonS3ClientBuilder s3CB = AmazonS3ClientBuilder.standard();

        ClientConfiguration cc = new ClientConfiguration();
        Integer poolSize = configObject.getInt("connection-pool-size", 256);
        cc.setMaxConnections(poolSize);
        s3CB.setClientConfiguration(cc);

        /**
         * Pass in a URL pointing to your S3 compatible storage. For possible values see
         * https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/client/builder/AwsClientBuilder.EndpointConfiguration.html
         */
        String s3CEUrl = configObject.getString("custom-endpoint-url", "");
        /**
         * Pass in a region to use for SigV4 signing of requests. Defaults to
         * "dataverse" as it is not relevant for custom S3 implementations.
         */
        String s3CERegion = configObject.getString("custom-endpoint-region", "dataverse");

        // if the admin has set a system property (see below) we use this endpoint URL
        // instead of the standard ones.
        if (!s3CEUrl.isEmpty()) {
            s3CB.setEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s3CEUrl, s3CERegion));
        }
        /**
         * Pass in a boolean value if path style access should be used within the S3
         * client. Anything but case-insensitive "true" will lead to value of false,
         * which is default value, too.
         */
        Boolean s3pathStyleAccess = configObject.getBoolean("path-style-access", false);
        // some custom S3 implementations require "PathStyleAccess" as they us a path,
        // not a subdomain. default = false
        s3CB.withPathStyleAccessEnabled(s3pathStyleAccess);

        /**
         * Pass in a boolean value if payload signing should be used within the S3
         * client. Anything but case-insensitive "true" will lead to value of false,
         * which is default value, too.
         */
        Boolean s3payloadSigning = configObject.getBoolean("payload-signing", false);
        /**
         * Pass in a boolean value if chunked encoding should not be used within the S3
         * client. Anything but case-insensitive "false" will lead to value of true,
         * which is default value, too.
         */
        Boolean s3chunkedEncoding = configObject.getBoolean("chunked-encoding", true);
        // Openstack SWIFT S3 implementations require "PayloadSigning" set to true.
        // default = false
        s3CB.setPayloadSigningEnabled(s3payloadSigning);
        // Openstack SWIFT S3 implementations require "ChunkedEncoding" set to false.
        // default = true
        // Boolean is inverted, otherwise setting
        // dataverse.files.<id>.chunked-encoding=false would result in leaving Chunked
        // Encoding enabled
        s3CB.setChunkedEncodingDisabled(!s3chunkedEncoding);

        /**
         * Pass in a string value if this archiver should use a non-default AWS S3
         * profile. The default is "default" which should work when only one profile
         * exists.
         */
        ProfileCredentialsProvider profileCredentials = new ProfileCredentialsProvider(configObject.getString("profile", "default"));

        // Try to retrieve credentials via Microprofile Config API, too. For production
        // use, you should not use env
        // vars or system properties to provide these, but use the secrets config source
        // provided by Payara.
        AWSStaticCredentialsProvider staticCredentials = new AWSStaticCredentialsProvider(new BasicAWSCredentials(
                config.getOptionalValue("dataverse.s3archiver.access-key", String.class).orElse(""),
                config.getOptionalValue("dataverse.s3archiver.secret-key", String.class).orElse("")));

        // Add both providers to chain - the first working provider will be used (so
        // static credentials are the fallback)
        AWSCredentialsProviderChain providerChain = new AWSCredentialsProviderChain(profileCredentials,
                staticCredentials);
        s3CB.setCredentials(providerChain);

        // let's build the client :-)
        AmazonS3 client = s3CB.build();
        return client;
    }

}
