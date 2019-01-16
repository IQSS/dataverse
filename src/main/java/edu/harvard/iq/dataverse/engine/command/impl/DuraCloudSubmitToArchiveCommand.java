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
    public WorkflowStepResult performArchiveSubmission(DatasetVersion dv, ApiToken token, Map<String, String> requestedSettings) {

        String port = requestedSettings.get(DURACLOUD_PORT) != null ? requestedSettings.get(DURACLOUD_PORT) : DEFAULT_PORT;
        String dpnContext = requestedSettings.get(DURACLOUD_CONTEXT) != null ? requestedSettings.get(DURACLOUD_CONTEXT) : DEFAULT_CONTEXT;
        String host = requestedSettings.get(DURACLOUD_HOST);
        if (host != null) {
            Dataset dataset = dv.getDataset();
            if (dataset.getLockFor(Reason.pidRegister) == null) {
                // Use Duracloud client classes to login
                ContentStoreManager storeManager = new ContentStoreManagerImpl(host, port, dpnContext);
                Credential credential = new Credential(System.getProperty("duracloud.username"),
                        System.getProperty("duracloud.password"));
                storeManager.login(credential);

                String spaceName = dataset.getGlobalId().asString().replace(':', '-').replace('/', '-')
                        .replace('.', '-').toLowerCase();

                ContentStore store;
                try {
                    /*
                     * If there is a failure in creating a space, it is likely that a prior version
                     * has not been fully processed (snapshot created, archiving completed and files
                     * and space deleted - currently manual operations done at the project's
                     * duracloud website)
                     */
                    store = storeManager.getPrimaryContentStore();
                    // Create space to copy archival files to
                    store.createSpace(spaceName);
                    DataCitation dc = new DataCitation(dv);
                    Map<String, String> metadata = dc.getDataCiteMetadata();
                    String dataciteXml = DOIDataCiteRegisterService.getMetadataFromDvObject(
                            dv.getDataset().getGlobalId().asString(), metadata, dv.getDataset());

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

                        String checksum = store.addContent(spaceName, "datacite.xml", digestInputStream, -1l, null, null,
                                null);
                        logger.fine("Content: datacite.xml added with checksum: " + checksum);
                        String localchecksum = Hex.encodeHexString(digestInputStream.getMessageDigest().digest());
                        if (!checksum.equals(localchecksum)) {
                            logger.severe(checksum + " not equal to " + localchecksum);
                            return new Failure("Error in transferring DataCite.xml file to DuraCloud",
                                    "DuraCloud Submission Failure: incomplete metadata transfer");
                        }

                        // Store BagIt file
                        String fileName = spaceName + "v" + dv.getFriendlyVersionNumber() + ".zip";

                        // Add BagIt ZIP file
                        // Although DuraCloud uses SHA-256 internally, it's API uses MD5 to verify the
                        // transfer
                        messageDigest = MessageDigest.getInstance("MD5");
                        try (PipedInputStream in = new PipedInputStream(); PipedOutputStream out = new PipedOutputStream(in); DigestInputStream digestInputStream2 = new DigestInputStream(in, messageDigest)) {
                            new Thread(new Runnable() {
                                public void run() {
                                    try {
                                        // Generate bag
                                        BagGenerator bagger = new BagGenerator(new OREMap(dv, false), dataciteXml);
                                        bagger.setAuthenticationKey(token.getTokenString());
                                        bagger.generateBag(out);
                                    } catch (Exception e) {
                                        logger.severe("Error creating bag: " + e.getMessage());
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                        throw new RuntimeException("Error creating bag: " + e.getMessage());
                                    }
                                }
                            }).start();

                            checksum = store.addContent(spaceName, fileName, digestInputStream2, -1l, null, null,
                                    null);
                            logger.fine("Content: " + fileName + " added with checksum: " + checksum);
                            localchecksum = Hex.encodeHexString(digestInputStream2.getMessageDigest().digest());
                            if (!checksum.equals(localchecksum)) {
                                logger.severe(checksum + " not equal to " + localchecksum);
                                return new Failure("Error in transferring Zip file to DuraCloud",
                                        "DuraCloud Submission Failure: incomplete archive transfer");
                            }
                        } catch (RuntimeException rte) {
                            logger.severe(rte.getMessage());
                            return new Failure("Error in generating Bag",
                                    "DuraCloud Submission Failure: archive file not created");
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
                        dv.setArchivalCopyLocation(sb.toString());
                        logger.fine("DuraCloud Submission step complete: " + sb.toString());
                    } catch (ContentStoreException | IOException e) {
                        // TODO Auto-generated catch block
                        logger.warning(e.getMessage());
                        e.printStackTrace();
                        return new Failure("Error in transferring file to DuraCloud",
                                "DuraCloud Submission Failure: archive file not transferred");
                    }  catch (RuntimeException rte) {
                        logger.severe(rte.getMessage());
                        return new Failure("Error in generating datacite.xml file",
                                "DuraCloud Submission Failure: metadata file not created");
                    }
                } catch (ContentStoreException e) {
                    logger.warning(e.getMessage());
                    e.printStackTrace();
                    String mesg = "DuraCloud Submission Failure";
                    if (!(1 == dv.getVersion()) || !(0 == dv.getMinorVersionNumber())) {
                        mesg = mesg + ": Prior Version archiving not yet complete?";
                    }
                    return new Failure("Unable to create DuraCloud space with name: " + spaceName, mesg);
                } catch (NoSuchAlgorithmException e) {
                    logger.severe("MD5 MessageDigest not available!");
                }
            } else {
                logger.warning("DuraCloud Submision Workflow aborted: Dataset locked for pidRegister");
                return new Failure("Dataset locked");
            }
            return WorkflowStepResult.OK;
        } else {
            return new Failure("DuraCloud Submission not configured - no \":DuraCloudHost\".");
        }
    }
    
}
