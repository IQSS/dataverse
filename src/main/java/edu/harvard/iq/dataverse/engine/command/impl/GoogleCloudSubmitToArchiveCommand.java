package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DOIDataCiteRegisterService;
import edu.harvard.iq.dataverse.DataCitation;
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
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.Charset;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Hex;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
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
            try {
                FileInputStream fis = new FileInputStream(System.getProperty("dataverse.files.directory") + System.getProperty("file.separator")+ "googlecloudkey.json");
                storage = StorageOptions.newBuilder()
                        .setCredentials(ServiceAccountCredentials.fromStream(fis))
                        .setProjectId(projectName)
                        .build()
                        .getService();
                Bucket bucket = storage.get(bucketName);

                Dataset dataset = dv.getDataset();
                if (dataset.getLockFor(Reason.pidRegister) == null) {

                    String spaceName = dataset.getGlobalId().asString().replace(':', '-').replace('/', '-')
                            .replace('.', '-').toLowerCase();

                    DataCitation dc = new DataCitation(dv);
                    Map<String, String> metadata = dc.getDataCiteMetadata();
                    String dataciteXml = DOIDataCiteRegisterService.getMetadataFromDvObject(
                            dv.getDataset().getGlobalId().asString(), metadata, dv.getDataset());
                    String blobIdString = null;
                    MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                    try (PipedInputStream dataciteIn = new PipedInputStream(); DigestInputStream digestInputStream = new DigestInputStream(dataciteIn, messageDigest)) {
                        // Add datacite.xml file

                        new Thread(new Runnable() {
                            public void run() {
                                try (PipedOutputStream dataciteOut = new PipedOutputStream(dataciteIn)) {

                                    dataciteOut.write(dataciteXml.getBytes(Charset.forName("utf-8")));
                                    dataciteOut.close();
                                } catch (Exception e) {
                                    logger.severe("Error creating datacite.xml: " + e.getMessage());
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                    throw new RuntimeException("Error creating datacite.xml: " + e.getMessage());
                                }
                            }
                        }).start();
                        //Have seen broken pipe in PostPublishDataset workflow without this delay
                        int i=0;
                        while(digestInputStream.available()<=0 && i<100) {
                            Thread.sleep(10);
                            i++;
                        }
                        Blob dcXml = bucket.create(spaceName + "/datacite.v" + dv.getFriendlyVersionNumber()+".xml", digestInputStream, "text/xml", Bucket.BlobWriteOption.doesNotExist());
                        String checksum = dcXml.getMd5ToHexString();
                        logger.fine("Content: datacite.xml added with checksum: " + checksum);
                        String localchecksum = Hex.encodeHexString(digestInputStream.getMessageDigest().digest());
                        if (!checksum.equals(localchecksum)) {
                            logger.severe(checksum + " not equal to " + localchecksum);
                            return new Failure("Error in transferring DataCite.xml file to GoogleCloud",
                                    "GoogleCloud Submission Failure: incomplete metadata transfer");
                        }

                        // Store BagIt file
                        String fileName = spaceName + ".v" + dv.getFriendlyVersionNumber() + ".zip";

                        // Add BagIt ZIP file
                        // Google uses MD5 as one way to verify the
                        // transfer
                        messageDigest = MessageDigest.getInstance("MD5");
                        try (PipedInputStream in = new PipedInputStream(100000); DigestInputStream digestInputStream2 = new DigestInputStream(in, messageDigest);) {
                            Thread writeThread = new Thread(new Runnable() {
                                public void run() {
                                    try (PipedOutputStream out = new PipedOutputStream(in)) {
                                        // Generate bag
                                        BagGenerator bagger = new BagGenerator(new OREMap(dv, false), dataciteXml);
                                        bagger.setAuthenticationKey(token.getTokenString());
                                        bagger.generateBag(out);
                                    } catch (Exception e) {
                                        logger.severe("Error creating bag: " + e.getMessage());
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                        try {
                                            digestInputStream2.close();
                                        } catch(Exception ex) {
                                            logger.warning(ex.getLocalizedMessage());
                                        }
                                        throw new RuntimeException("Error creating bag: " + e.getMessage());
                                    }
                                }
                            });
                            writeThread.start();
                            /*
                             * The following loop handles two issues. First, with no delay, the
                             * bucket.create() call below can get started before the piped streams are set
                             * up, causing a failure (seen when triggered in a PostPublishDataset workflow).
                             * A minimal initial wait, e.g. until some bytes are available, would address
                             * this. Second, the BagGenerator class, due to it's use of parallel streaming
                             * creation of the zip file, has the characteristic that it makes a few bytes
                             * available - from setting up the directory structure for the zip file -
                             * significantly earlier than it is ready to stream file content (e.g. for
                             * thousands of files and GB of content). If, for these large datasets,
                             * bucket.create() is called as soon as bytes are available, the call can
                             * timeout before the bytes for all the zipped files are available. To manage
                             * this, the loop waits until 90K bytes are available, larger than any expected
                             * dir structure for the zip and implying that the main zipped content is
                             * available, or until the thread terminates, with all of its content written to
                             * the pipe. (Note the PipedInputStream buffer is set at 100K above - I didn't
                             * want to test whether that means that exactly 100K bytes will be available()
                             * for large datasets or not, so the test below is at 90K.)
                             * 
                             * An additional sanity check limits the wait to 2K seconds. The BagGenerator
                             * has been used to archive >120K files, 2K directories, and ~600GB files on the
                             * SEAD project (streaming content to disk rather than over an internet
                             * connection) which would take longer than 2K seconds (10+ hours) and might
                             * produce an initial set of bytes for directories > 90K. If Dataverse ever
                             * needs to support datasets of this size, the numbers here would need to be
                             * increased, and/or a change in how archives are sent to google (e.g. as
                             * multiple blobs that get aggregated) would be required.
                             */
                            i=0;
                            while(digestInputStream2.available()<=90000 && i<2000 && writeThread.isAlive()) {
                                Thread.sleep(1000);
                                logger.fine("avail: " + digestInputStream2.available() + " : " + writeThread.getState().toString());
                                i++;
                            }
                            logger.fine("Bag: transfer started, i=" + i + ", avail = " + digestInputStream2.available());
                            if(i==2000) {
                                throw new IOException("Stream not available");
                            }
                            Blob bag = bucket.create(spaceName + "/" + fileName, digestInputStream2, "application/zip", Bucket.BlobWriteOption.doesNotExist());
                            if(bag.getSize()==0) {
                                throw new IOException("Empty Bag");
                            }
                            blobIdString = bag.getBlobId().getBucket() + "/" + bag.getBlobId().getName();
                            checksum = bag.getMd5ToHexString();
                            logger.fine("Bag: " + fileName + " added with checksum: " + checksum);
                            localchecksum = Hex.encodeHexString(digestInputStream2.getMessageDigest().digest());
                            if (!checksum.equals(localchecksum)) {
                                logger.severe(checksum + " not equal to " + localchecksum);
                                return new Failure("Error in transferring Zip file to GoogleCloud",
                                        "GoogleCloud Submission Failure: incomplete archive transfer");
                            }
                        } catch (RuntimeException rte) {
                            logger.severe("Error creating Bag during GoogleCloud archiving: " + rte.getMessage());
                            return new Failure("Error in generating Bag",
                                    "GoogleCloud Submission Failure: archive file not created");
                        }

                        logger.fine("GoogleCloud Submission step: Content Transferred");

                        // Document the location of dataset archival copy location (actually the URL
                        // where you can
                        // view it as an admin)

                        StringBuffer sb = new StringBuffer("https://console.cloud.google.com/storage/browser/");
                        sb.append(blobIdString);
                        dv.setArchivalCopyLocation(sb.toString());
                    } catch (RuntimeException rte) {
                        logger.severe("Error creating datacite xml file during GoogleCloud Archiving: " + rte.getMessage());
                        return new Failure("Error in generating datacite.xml file",
                                "GoogleCloud Submission Failure: metadata file not created");
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

            }
            return WorkflowStepResult.OK;
        } else {
            return new Failure("GoogleCloud Submission not configured - no \":GoogleCloudBucket\"  and/or \":GoogleCloudProject\".");
        }
    }

}
