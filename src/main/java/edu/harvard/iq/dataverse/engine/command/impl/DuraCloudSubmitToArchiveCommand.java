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

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.Charset;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.apache.commons.codec.binary.Hex;
import org.duracloud.client.ContentStore;
import org.duracloud.client.ContentStoreManager;
import org.duracloud.client.ContentStoreManagerImpl;
import org.duracloud.common.model.Credential;
import org.duracloud.error.ContentStoreException;

@RequiredPermissions(Permission.PublishDataset)
public class DuraCloudSubmitToArchiveCommand extends AbstractSubmitToArchiveCommand implements Command<DatasetVersion> {

    private static final Logger logger = Logger.getLogger(DuraCloudSubmitToArchiveCommand.class.getName());
    private static final String DEFAULT_PORT = "443";
    private static final String DEFAULT_CONTEXT = "durastore";
    private static final String DURACLOUD_PORT = ":DuraCloudPort";
    private static final String DURACLOUD_HOST = ":DuraCloudHost";
    private static final String DURACLOUD_CONTEXT = ":DuraCloudContext";


    public DuraCloudSubmitToArchiveCommand(DataverseRequest aRequest, DatasetVersion version) {
        super(aRequest, version);
    }

    @Override
    public WorkflowStepResult performArchiveSubmission(DatasetVersion dv, ApiToken token,
            Map<String, String> requestedSettings) {

        String port = requestedSettings.get(DURACLOUD_PORT) != null ? requestedSettings.get(DURACLOUD_PORT)
                : DEFAULT_PORT;
        String dpnContext = requestedSettings.get(DURACLOUD_CONTEXT) != null ? requestedSettings.get(DURACLOUD_CONTEXT)
                : DEFAULT_CONTEXT;
        String host = requestedSettings.get(DURACLOUD_HOST);
        
        if (host != null) {
            Dataset dataset = dv.getDataset();
            // ToDo - change after HDC 3A changes to status reporting
            // This will make the archivalCopyLocation non-null after a failure which should
            // stop retries
            
            if (dataset.getLockFor(Reason.finalizePublication) == null
                    && dataset.getLockFor(Reason.FileValidationFailed) == null) {
                // Use Duracloud client classes to login
                ContentStoreManager storeManager = new ContentStoreManagerImpl(host, port, dpnContext);
                Credential credential = new Credential(System.getProperty("duracloud.username"),
                        System.getProperty("duracloud.password"));
                storeManager.login(credential);
                /*
                 * Aliases can contain upper case characters which are not allowed in space
                 * names. Similarly, aliases can contain '_' which isn't allowed in a space
                 * name. The line below replaces any upper case chars with lowercase and
                 * replaces any '_' with '.-' . The '-' after the dot assures we don't break the
                 * rule that
                 * "The last period in a aspace may not immediately be followed by a number".
                 * (Although we could check, it seems better to just add '.-' all the time.As
                 * written the replaceAll will also change any chars not valid in a spaceName to
                 * '.' which would avoid code breaking if the alias constraints change. That
                 * said, this line may map more than one alias to the same spaceName, e.g.
                 * "test" and "Test" aliases both map to the "test" space name. This does not
                 * break anything but does potentially put bags from more than one collection in
                 * the same space.
                 */
                String spaceName = dataset.getOwner().getAlias().toLowerCase().replaceAll("[^a-z0-9-]", ".dcsafe");
                String baseFileName = dataset.getGlobalId().asString().replace(':', '-').replace('/', '-')
                        .replace('.', '-').toLowerCase() + "_v" + dv.getFriendlyVersionNumber();

                ContentStore store;
                //Set a failure status that will be updated if we succeed
                JsonObjectBuilder statusObject = Json.createObjectBuilder();
                statusObject.add(DatasetVersion.ARCHIVAL_STATUS, DatasetVersion.ARCHIVAL_STATUS_FAILURE);
                statusObject.add(DatasetVersion.ARCHIVAL_STATUS_MESSAGE, "Bag not transferred");
                
                try {
                    /*
                     * If there is a failure in creating a space, it is likely that a prior version
                     * has not been fully processed (snapshot created, archiving completed and files
                     * and space deleted - currently manual operations done at the project's
                     * duracloud website)
                     */
                    store = storeManager.getPrimaryContentStore();
                    // Create space to copy archival files to
                    if (!store.spaceExists(spaceName)) {
                        store.createSpace(spaceName);
                    }
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
                                    success=true;
                                } catch (Exception e) {
                                    logger.severe("Error creating datacite.xml: " + e.getMessage());
                                    // TODO Auto-generated catch block
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
                        String checksum = store.addContent(spaceName, baseFileName + "_datacite.xml", digestInputStream,
                                -1l, null, null, null);
                        logger.fine("Content: datacite.xml added with checksum: " + checksum);
                        dcThread.join();
                        String localchecksum = Hex.encodeHexString(digestInputStream.getMessageDigest().digest());
                        if (!success || !checksum.equals(localchecksum)) {
                            logger.severe("Failure on " + baseFileName);
                            logger.severe(success ? checksum + " not equal to " + localchecksum : "failed to transfer to DuraCloud");
                            try {
                                store.deleteContent(spaceName, baseFileName + "_datacite.xml");
                            } catch (ContentStoreException cse) {
                                logger.warning(cse.getMessage());
                            }
                            return new Failure("Error in transferring DataCite.xml file to DuraCloud",
                                    "DuraCloud Submission Failure: incomplete metadata transfer");
                        }

                        // Store BagIt file
                        success = false;
                        String fileName = baseFileName + ".zip";

                        // Add BagIt ZIP file
                        // Although DuraCloud uses SHA-256 internally, it's API uses MD5 to verify the
                        // transfer

                        messageDigest = MessageDigest.getInstance("MD5");
                        try (PipedInputStream in = new PipedInputStream(100000);
                                DigestInputStream digestInputStream2 = new DigestInputStream(in, messageDigest)) {
                            Thread bagThread = startBagThread(dv, in, digestInputStream2, dataciteXml, token);
                            checksum = store.addContent(spaceName, fileName, digestInputStream2, -1l, null, null, null);
                            bagThread.join();
                            if (success) {
                                logger.fine("Content: " + fileName + " added with checksum: " + checksum);
                                localchecksum = Hex.encodeHexString(digestInputStream2.getMessageDigest().digest());
                            }
                            if (!success || !checksum.equals(localchecksum)) {
                                logger.severe("Failure on " + fileName);
                                logger.severe(success ? checksum + " not equal to " + localchecksum : "failed to transfer to DuraCloud");
                                try {
                                    store.deleteContent(spaceName, fileName);
                                    store.deleteContent(spaceName, baseFileName + "_datacite.xml");
                                } catch (ContentStoreException cse) {
                                    logger.warning(cse.getMessage());
                                }
                                return new Failure("Error in transferring Zip file to DuraCloud",
                                        "DuraCloud Submission Failure: incomplete archive transfer");
                            }
                        }

                        logger.fine("DuraCloud Submission step: Content Transferred");

                        // Document the location of dataset archival copy location (actually the URL
                        // where you can
                        // view it as an admin)
                        StringBuffer sb = new StringBuffer("https://");
                        sb.append(host);
                        if (!port.equals("443")) {
                            sb.append(":" + port);
                        }
                        sb.append("/duradmin/spaces/sm/");
                        sb.append(store.getStoreId());
                        sb.append("/" + spaceName + "/" + fileName);
                        statusObject.add(DatasetVersion.ARCHIVAL_STATUS, DatasetVersion.ARCHIVAL_STATUS_SUCCESS);
                        statusObject.add(DatasetVersion.ARCHIVAL_STATUS_MESSAGE, sb.toString());
                        
                        logger.fine("DuraCloud Submission step complete: " + sb.toString());
                    } catch (ContentStoreException | IOException e) {
                        // TODO Auto-generated catch block
                        logger.warning(e.getMessage());
                        e.printStackTrace();
                        return new Failure("Error in transferring file to DuraCloud",
                                "DuraCloud Submission Failure: archive file not transferred");
                    } catch (InterruptedException e) {
                        logger.warning(e.getLocalizedMessage());
                        e.printStackTrace();
                    }
                } catch (ContentStoreException e) {
                    logger.warning(e.getMessage());
                    e.printStackTrace();
                    String mesg = "DuraCloud Submission Failure";
                    if (!(1 == dv.getVersion()) || !(0 == dv.getMinorVersionNumber())) {
                        mesg = mesg + ": Prior Version archiving not yet complete?";
                    }
                    return new Failure("Unable to create DuraCloud space with name: " + baseFileName, mesg);
                } catch (NoSuchAlgorithmException e) {
                    logger.severe("MD5 MessageDigest not available!");
                }
                finally {
                    dv.setArchivalCopyLocation(statusObject.build().toString());
                }
            } else {
                logger.warning(
                        "DuraCloud Submision Workflow aborted: Dataset locked for finalizePublication, or because file validation failed");
                return new Failure("Dataset locked");
            }
            return WorkflowStepResult.OK;
        } else {
            return new Failure("DuraCloud Submission not configured - no \":DuraCloudHost\".");
        }
    }
}
