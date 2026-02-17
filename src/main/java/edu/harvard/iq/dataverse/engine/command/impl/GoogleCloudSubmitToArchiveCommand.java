package edu.harvard.iq.dataverse.engine.command.impl;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.GoogleCloudBucket;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.GoogleCloudProject;
import edu.harvard.iq.dataverse.util.bagit.BagGenerator;
import edu.harvard.iq.dataverse.util.bagit.BagGenerator.FileEntry;
import edu.harvard.iq.dataverse.util.json.JsonLDTerm;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.compress.parallel.InputStreamSupplier;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Map;
import java.util.logging.Logger;

@RequiredPermissions(Permission.PublishDataset)
public class GoogleCloudSubmitToArchiveCommand extends AbstractSubmitToArchiveCommand {

    private static final Logger logger = Logger.getLogger(GoogleCloudSubmitToArchiveCommand.class.getName());
    private static final String GOOGLECLOUD_BUCKET = GoogleCloudBucket.toString();
    private static final String GOOGLECLOUD_PROJECT = GoogleCloudProject.toString();

    public GoogleCloudSubmitToArchiveCommand(DataverseRequest aRequest, DatasetVersion version) {
        super(aRequest, version);
    }

    public static boolean supportsDelete() {
        return true;
    }
    @Override
    public boolean canDelete() {
        return supportsDelete();
    }
    
    @Override
    public WorkflowStepResult performArchiveSubmission(DatasetVersion dv, String dataciteXml, JsonObject ore,
            Map<String, JsonLDTerm> terms, ApiToken token, Map<String, String> requestedSettings) {
        logger.fine("In GoogleCloudSubmitToArchiveCommand...");
        String bucketName = requestedSettings.get(GOOGLECLOUD_BUCKET);
        String projectName = requestedSettings.get(GOOGLECLOUD_PROJECT);
        logger.fine("Project: " + projectName + " Bucket: " + bucketName);
        if (bucketName != null && projectName != null) {
            Storage storage;
            // Set a failure status that will be updated if we succeed
            JsonObjectBuilder statusObject = Json.createObjectBuilder();
            statusObject.add(DatasetVersion.ARCHIVAL_STATUS, DatasetVersion.ARCHIVAL_STATUS_FAILURE);
            statusObject.add(DatasetVersion.ARCHIVAL_STATUS_MESSAGE, "Bag not transferred");

            String cloudKeyFile = JvmSettings.FILES_DIRECTORY.lookup() + File.separator + "googlecloudkey.json";

            // Create temporary file for bag
            Path tempBagFile = null;

            try (FileInputStream cloudKeyStream = new FileInputStream(cloudKeyFile)) {
                storage = StorageOptions.newBuilder()
                        .setCredentials(ServiceAccountCredentials.fromStream(cloudKeyStream)).setProjectId(projectName)
                        .build().getService();
                Bucket bucket = storage.get(bucketName);

                Dataset dataset = dv.getDataset();

                String spaceName = dataset.getGlobalId().asString().replace(':', '-').replace('/', '-')
                        .replace('.', '-').toLowerCase();

                // Check for and delete existing files for this version
                String dataciteFileName = spaceName + "/datacite.v" + dv.getFriendlyVersionNumber() + ".xml";
                String bagFileName = spaceName + "/" + spaceName + ".v" + dv.getFriendlyVersionNumber() + ".zip";

                logger.fine("Checking for existing files in archive...");

                try {
                    Blob existingDatacite = bucket.get(dataciteFileName);
                    if (existingDatacite != null && existingDatacite.exists()) {
                        logger.fine("Found existing datacite.xml, deleting: " + dataciteFileName);
                        existingDatacite.delete();
                        logger.fine("Deleted existing datacite.xml");
                    }
                } catch (StorageException se) {
                    logger.warning("Error checking/deleting existing datacite.xml: " + se.getMessage());
                }

                try {
                    Blob existingBag = bucket.get(bagFileName);
                    if (existingBag != null && existingBag.exists()) {
                        logger.fine("Found existing bag file, deleting: " + bagFileName);
                        existingBag.delete();
                        logger.fine("Deleted existing bag file");
                    }
                } catch (StorageException se) {
                    logger.warning("Error checking/deleting existing bag file: " + se.getMessage());
                }

                // Upload datacite.xml
                MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                try (PipedInputStream dataciteIn = new PipedInputStream();
                        DigestInputStream digestInputStream = new DigestInputStream(dataciteIn, messageDigest)) {
                    // Add datacite.xml file

                    Thread dcThread = new Thread(new Runnable() {
                        public void run() {
                            try (PipedOutputStream dataciteOut = new PipedOutputStream(dataciteIn)) {

                                dataciteOut.write(dataciteXml.getBytes(StandardCharsets.UTF_8));
                                dataciteOut.close();
                                success = true;
                            } catch (Exception e) {
                                logger.severe("Error creating datacite.xml: " + e.getMessage());
                                e.printStackTrace();
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
                    Blob dcXml = bucket.create(dataciteFileName, digestInputStream, "text/xml",
                            Bucket.BlobWriteOption.doesNotExist());

                    dcThread.join();
                    String checksum = dcXml.getMd5ToHexString();
                    logger.fine("Content: datacite.xml added with checksum: " + checksum);
                    String localchecksum = Hex.encodeHexString(digestInputStream.getMessageDigest().digest());
                    if (!success || !checksum.equals(localchecksum)) {
                        logger.severe("Failure on " + spaceName);
                        logger.severe(success ? checksum + " not equal to " + localchecksum
                                : "datacite.xml transfer did not succeed");
                        try {
                            dcXml.delete(Blob.BlobSourceOption.generationMatch());
                        } catch (StorageException se) {
                            logger.warning(se.getMessage());
                        }
                        return new Failure("Error in transferring DataCite.xml file to GoogleCloud",
                                "GoogleCloud Submission Failure: incomplete metadata transfer");
                    }
                }

                tempBagFile = Files.createTempFile("dataverse-bag-", ".zip");
                logger.fine("Creating bag in temporary file: " + tempBagFile.toString());

                BagGenerator bagger = new BagGenerator(ore, dataciteXml, terms);
                bagger.setAuthenticationKey(token.getTokenString());
                // Generate bag to temporary file using the provided ore JsonObject
                try (FileOutputStream fos = new FileOutputStream(tempBagFile.toFile())) {
                    if (!bagger.generateBag(fos)) {
                        throw new IOException("Bag generation failed");
                    }
                }

                // Store BagIt file
                long bagSize = Files.size(tempBagFile);
                logger.fine("Bag created successfully, size: " + bagSize + " bytes");

                if (bagSize == 0) {
                    throw new IOException("Generated bag file is empty");
                }

                // Upload bag file and calculate checksum during upload
                messageDigest = MessageDigest.getInstance("MD5");
                String localChecksum;

                try (FileInputStream fis = new FileInputStream(tempBagFile.toFile());
                        DigestInputStream dis = new DigestInputStream(fis, messageDigest)) {

                    logger.fine("Uploading bag to GoogleCloud: " + bagFileName);

                    Blob bag = bucket.create(bagFileName, dis, "application/zip",
                            Bucket.BlobWriteOption.doesNotExist());

                    if (bag.getSize() == 0) {
                        throw new IOException("Uploaded bag has zero size");
                    }

                    // Get checksum after upload completes
                    localChecksum = Hex.encodeHexString(dis.getMessageDigest().digest());
                    String remoteChecksum = bag.getMd5ToHexString();

                    logger.fine("Bag: " + bagFileName + " uploaded");
                    logger.fine("Local checksum:  " + localChecksum);
                    logger.fine("Remote checksum: " + remoteChecksum);

                    if (!localChecksum.equals(remoteChecksum)) {
                        logger.severe("Bag checksum mismatch!");
                        logger.severe("Local: " + localChecksum + " != Remote: " + remoteChecksum);
                        try {
                            bag.delete(Blob.BlobSourceOption.generationMatch());
                        } catch (StorageException se) {
                            logger.warning(se.getMessage());
                        }
                        return new Failure("Error in transferring Zip file to GoogleCloud",
                                "GoogleCloud Submission Failure: bag checksum mismatch");
                    }
                }

                logger.fine("GoogleCloud Submission step: Content Transferred Successfully");

                // Now upload any files that were too large for the bag
                for (FileEntry entry : bagger.getOversizedFiles()) {
                    String childPath = entry.getChildPath(entry.getChildTitle());
                    String fileKey = spaceName + "/" + childPath;
                    logger.fine("Uploading oversized file to GoogleCloud: " + fileKey);
                    messageDigest = MessageDigest.getInstance("MD5");
                    InputStreamSupplier supplier = bagger.getInputStreamSupplier(entry.getDataUrl());
                    try (InputStream is = supplier.get();
                            DigestInputStream dis = new DigestInputStream(is, messageDigest)) {
                        Blob oversizedFileBlob = bucket.create(fileKey, dis, Bucket.BlobWriteOption.doesNotExist());
                        if (oversizedFileBlob.getSize() == 0) {
                            throw new IOException("Uploaded oversized file has zero size: " + fileKey);
                        }
                        localChecksum = Hex.encodeHexString(dis.getMessageDigest().digest());
                        String remoteChecksum = oversizedFileBlob.getMd5ToHexString();
                        logger.fine("Oversized file: " + fileKey + " uploaded");
                        logger.fine("Local checksum:  " + localChecksum);
                        logger.fine("Remote checksum: " + remoteChecksum);
                        if (!localChecksum.equals(remoteChecksum)) {
                            logger.severe("Oversized file checksum mismatch!");
                            logger.severe("Local: " + localChecksum + " != Remote: " + remoteChecksum);
                            try {
                                oversizedFileBlob.delete(Blob.BlobSourceOption.generationMatch());
                            } catch (StorageException se) {
                                logger.warning(se.getMessage());
                            }
                            return new Failure("Error in transferring oversized file to GoogleCloud",
                                    "GoogleCloud Submission Failure: oversized file transfer incomplete");
                        }
                    } catch (IOException e) {
                        logger.warning("Failed to upload oversized file: " + childPath + " : " + e.getMessage());
                        return new Failure("Error uploading oversized file to Google Cloud: " + childPath);
                    }
                }

                // Document the location of dataset archival copy location (actually the URL
                // to the bucket).
                statusObject.add(DatasetVersion.ARCHIVAL_STATUS, DatasetVersion.ARCHIVAL_STATUS_SUCCESS);
                statusObject.add(DatasetVersion.ARCHIVAL_STATUS_MESSAGE,
                        String.format("https://storage.cloud.google.com/%s/%s", bucketName, spaceName));

            } catch (Exception e) {
                logger.warning(e.getLocalizedMessage());
                e.printStackTrace();
                return new Failure("GoogleCloud Submission Failure",
                        e.getLocalizedMessage() + ": check log for details");

            } finally {
                if (tempBagFile != null) {
                    try {
                        Files.deleteIfExists(tempBagFile);
                    } catch (IOException e) {
                        logger.warning("Failed to delete temporary bag file: " + tempBagFile + " : " + e.getMessage());
                    }
                }
                dv.setArchivalCopyLocation(statusObject.build().toString());
            }
            return WorkflowStepResult.OK;
        } else {
            return new Failure(
                    "GoogleCloud Submission not configured - no \":GoogleCloudBucket\"  and/or \":GoogleCloudProject\".");
        }
    }

}
