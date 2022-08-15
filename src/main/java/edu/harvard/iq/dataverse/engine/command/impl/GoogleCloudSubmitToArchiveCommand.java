package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetLock.Reason;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.Charset;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Map;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.apache.commons.codec.binary.Hex;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;

@RequiredPermissions(Permission.PublishDataset)
public class GoogleCloudSubmitToArchiveCommand extends AbstractSubmitToArchiveCommand implements Command<DatasetVersion> {

    private static final Logger logger = Logger.getLogger(GoogleCloudSubmitToArchiveCommand.class.getName());
    private static final String GOOGLECLOUD_BUCKET = ":GoogleCloudBucket";
    private static final String GOOGLECLOUD_PROJECT = ":GoogleCloudProject";

    public GoogleCloudSubmitToArchiveCommand(DataverseRequest aRequest, DatasetVersion version) {
        super(aRequest, version);
    }

    @Override
    public WorkflowStepResult performArchiveSubmission(DatasetVersion dv, ApiToken token, Map<String, String> requestedSettings) {
        logger.fine("In GoogleCloudSubmitToArchiveCommand...");
        String bucketName = requestedSettings.get(GOOGLECLOUD_BUCKET);
        String projectName = requestedSettings.get(GOOGLECLOUD_PROJECT);
        logger.fine("Project: " + projectName + " Bucket: " + bucketName);
        if (bucketName != null && projectName != null) {
            Storage storage;
            //Set a failure status that will be updated if we succeed
            JsonObjectBuilder statusObject = Json.createObjectBuilder();
            statusObject.add(DatasetVersion.ARCHIVAL_STATUS, DatasetVersion.ARCHIVAL_STATUS_FAILURE);
            statusObject.add(DatasetVersion.ARCHIVAL_STATUS_MESSAGE, "Bag not transferred");
            
            try {
                FileInputStream fis = new FileInputStream(System.getProperty("dataverse.files.directory") + System.getProperty("file.separator") + "googlecloudkey.json");
                storage = StorageOptions.newBuilder()
                        .setCredentials(ServiceAccountCredentials.fromStream(fis))
                        .setProjectId(projectName)
                        .build()
                        .getService();
                Bucket bucket = storage.get(bucketName);

                Dataset dataset = dv.getDataset();
                if (dataset.getLockFor(Reason.finalizePublication) == null) {

                    String spaceName = dataset.getGlobalId().asString().replace(':', '-').replace('/', '-')
                            .replace('.', '-').toLowerCase();

                    String dataciteXml = getDataCiteXml(dv);
                    MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                    try (PipedInputStream dataciteIn = new PipedInputStream();
                            DigestInputStream digestInputStream = new DigestInputStream(dataciteIn, messageDigest)) {
                        // Add datacite.xml file

                        Thread dcThread = new Thread(new Runnable() {
                            public void run() {
                                try (PipedOutputStream dataciteOut = new PipedOutputStream(dataciteIn)) {

                                    dataciteOut.write(dataciteXml.getBytes(Charset.forName("utf-8")));
                                    dataciteOut.close();
                                    success = true;
                                } catch (Exception e) {
                                    logger.severe("Error creating datacite.xml: " + e.getMessage());
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                    // throw new RuntimeException("Error creating datacite.xml: " + e.getMessage());
                                }
                            }
                        });
                        dcThread.start();
                        // Have seen Pipe Closed errors for other archivers when used as a workflow
                        // without this delay loop
                        int i = 0;
                        while (digestInputStream.available() <= 0 && i < 100) {
                            Thread.sleep(10);
                            i++;
                        }
                        Blob dcXml = bucket.create(spaceName + "/datacite.v" + dv.getFriendlyVersionNumber() + ".xml", digestInputStream, "text/xml", Bucket.BlobWriteOption.doesNotExist());

                        dcThread.join();
                        String checksum = dcXml.getMd5ToHexString();
                        logger.fine("Content: datacite.xml added with checksum: " + checksum);
                        String localchecksum = Hex.encodeHexString(digestInputStream.getMessageDigest().digest());
                        if (!success || !checksum.equals(localchecksum)) {
                            logger.severe("Failure on " + spaceName);
                            logger.severe(success ? checksum + " not equal to " + localchecksum : "datacite.xml transfer did not succeed");
                            try {
                                dcXml.delete(Blob.BlobSourceOption.generationMatch());
                            } catch (StorageException se) {
                                logger.warning(se.getMessage());
                            }
                            return new Failure("Error in transferring DataCite.xml file to GoogleCloud",
                                    "GoogleCloud Submission Failure: incomplete metadata transfer");
                        }

                        // Store BagIt file
                        success = false;
                        String fileName = spaceName + ".v" + dv.getFriendlyVersionNumber() + ".zip";

                        // Add BagIt ZIP file
                        // Google uses MD5 as one way to verify the
                        // transfer
                        messageDigest = MessageDigest.getInstance("MD5");
                        try (PipedInputStream in = new PipedInputStream(100000);
                                DigestInputStream digestInputStream2 = new DigestInputStream(in, messageDigest)) {
                            Thread bagThread = startBagThread(dv, in, digestInputStream2, dataciteXml, token);
                            Blob bag = bucket.create(spaceName + "/" + fileName, digestInputStream2, "application/zip",
                                    Bucket.BlobWriteOption.doesNotExist());
                            if (bag.getSize() == 0) {
                                throw new IOException("Empty Bag");
                            }
                            bagThread.join();

                            checksum = bag.getMd5ToHexString();
                            logger.fine("Bag: " + fileName + " added with checksum: " + checksum);
                            localchecksum = Hex.encodeHexString(digestInputStream2.getMessageDigest().digest());
                            if (!success || !checksum.equals(localchecksum)) {
                                logger.severe(success ? checksum + " not equal to " + localchecksum
                                        : "bag transfer did not succeed");
                                try {
                                    bag.delete(Blob.BlobSourceOption.generationMatch());
                                } catch (StorageException se) {
                                    logger.warning(se.getMessage());
                                }
                                return new Failure("Error in transferring Zip file to GoogleCloud",
                                        "GoogleCloud Submission Failure: incomplete archive transfer");
                            }
                        }

                        logger.fine("GoogleCloud Submission step: Content Transferred");

                        // Document the location of dataset archival copy location (actually the URL
                        // where you can view it as an admin)
                        // Changed to point at bucket where the zip and datacite.xml are visible

                        StringBuffer sb = new StringBuffer("https://console.cloud.google.com/storage/browser/");
                        sb.append(bucketName + "/" + spaceName);
                        statusObject.add(DatasetVersion.ARCHIVAL_STATUS, DatasetVersion.ARCHIVAL_STATUS_SUCCESS);
                        statusObject.add(DatasetVersion.ARCHIVAL_STATUS_MESSAGE, sb.toString());
                        
                    }
                } else {
                    logger.warning("GoogleCloud Submision Workflow aborted: Dataset locked for pidRegister");
                    return new Failure("Dataset locked");
                }
            } catch (Exception e) {
                logger.warning(e.getLocalizedMessage());
                e.printStackTrace();
                return new Failure("GoogleCloud Submission Failure",
                        e.getLocalizedMessage() + ": check log for details");

            } finally {
                dv.setArchivalCopyLocation(statusObject.build().toString());
            }
            return WorkflowStepResult.OK;
        } else {
            return new Failure("GoogleCloud Submission not configured - no \":GoogleCloudBucket\"  and/or \":GoogleCloudProject\".");
        }
    }

}
