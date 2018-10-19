package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.DOIDataCiteRegisterService;
import edu.harvard.iq.dataverse.DataCitation;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DatasetLock.Reason;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.bagit.BagGenerator;
import edu.harvard.iq.dataverse.util.bagit.OREMap;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import static edu.harvard.iq.dataverse.engine.command.CommandHelper.CH;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.Charset;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.duracloud.client.ContentStore;
import org.duracloud.client.ContentStoreManager;
import org.duracloud.client.ContentStoreManagerImpl;
import org.duracloud.common.model.Credential;
import org.duracloud.error.ContentStoreException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@RequiredPermissions(Permission.ArchiveDatasetVersion)
public class SubmitToArchiveCommand implements Command<DatasetVersion> {

    private final DatasetVersion version;
    private final DataverseRequest request;
    private final Map<String, DvObject> affectedDvObjects;
    private static final Logger logger = Logger.getLogger(SubmitToArchiveCommand.class.getName());
    private static final String DEFAULT_PORT = "443";
    private static final String DEFAULT_CONTEXT = "durastore";

    public SubmitToArchiveCommand(DataverseRequest aRequest, DatasetVersion version) {
        this.request = aRequest;
        this.version = version;
        affectedDvObjects = new HashMap<>();
        affectedDvObjects.put("", version.getDataset());
    }

    @Override
    public DatasetVersion execute(CommandContext ctxt) throws CommandException {
        String host = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DuraCloudHost);
        String port = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DuraCloudPort);
        String dpnContext = ctxt.settings().getValueForKey(SettingsServiceBean.Key.DuraCloudContext);
        AuthenticatedUser user = request.getAuthenticatedUser();
        ApiToken token = ctxt.authentication().findApiTokenByUser(user);
        if ((token == null) || (token.getExpireTime().before(new Date()))) {
            token = ctxt.authentication().generateApiTokenForUser(user);
        }
        performDPNSubmission(version, user, host, port, dpnContext, token);
        return ctxt.em().merge(version);
    }

    @Override
    public Map<String, DvObject> getAffectedDvObjects() {

        return affectedDvObjects;
    }

    @Override
    public DataverseRequest getRequest() {
        return request;
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return CH.permissionsRequired(getClass());
    }

    @Override
    public String describe() {
        // Is this ever used?
        return "DatasetVersion: " + version.getId() + " " + version.getDataset().getDisplayName() + ".v"
                + version.getFriendlyVersionNumber();
    }

    public static WorkflowStepResult performDPNSubmission(DatasetVersion dv, AuthenticatedUser user, String host,
            String aPort, String aDpnContext, ApiToken token) {

        String port = aPort != null ? aPort : DEFAULT_PORT;
        String dpnContext = aDpnContext != null ? aDpnContext : DEFAULT_CONTEXT;
        if (host != null) {
            Dataset dataset = dv.getDataset();
            if (dataset.getLockFor(Reason.pidRegister) == null) {
                //Use Duracloud client classes to login
                ContentStoreManager storeManager = new ContentStoreManagerImpl(host, port, dpnContext);
                Credential credential = new Credential(System.getProperty("duracloud.username"),
                        System.getProperty("duracloud.password"));
                storeManager.login(credential);

                String spaceName = dataset.getGlobalId().asString().replace(':', '-').replace('/', '-')
                        .replace('.', '-').toLowerCase();

                ContentStore store;
                try {
                    /* If there is a failure in creating a space, it is likely that a prior version has not been fully processed 
                     * (snapshot created, archiving completed and files and space deleted - currently manual operations 
                     * done at the project's duracloud website) 
                     */
                    store = storeManager.getPrimaryContentStore();
                    //Create space to copy archival files to
                    store.createSpace(spaceName);
                    DataCitation dc = new DataCitation(dv);
                    Map<String, String> metadata = dc.getDataCiteMetadata();
                    String dataciteXml = DOIDataCiteRegisterService.getMetadataFromDvObject(
                            dv.getDataset().getGlobalId().asString(), metadata, dv.getDataset());
                 
                    try {
                 // Add datacite.xml file
                    MessageDigest messageDigest = MessageDigest.getInstance("MD5");

                    PipedInputStream dataciteIn = new PipedInputStream();
                    PipedOutputStream dataciteOut = new PipedOutputStream(dataciteIn);
                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                
                                dataciteOut.write(dataciteXml.getBytes(Charset.forName("utf-8")));
                                dataciteOut.close();
                            } catch (Exception e) {
                                logger.severe("Error creating datacite.xml: " + e.getMessage());
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                                IOUtils.closeQuietly(dataciteIn);
                                IOUtils.closeQuietly(dataciteOut);
                            }
                        }
                    }).start();

                    DigestInputStream digestInputStream = new DigestInputStream(dataciteIn, messageDigest);
                    String checksum = store.addContent(spaceName, "datacite.xml", digestInputStream, -1l, null, null,
                            null);
                    logger.fine("Content: datacite.xml added with checksum: " + checksum);
                    String localchecksum = Hex.encodeHexString(digestInputStream.getMessageDigest().digest());
                    if (!checksum.equals(localchecksum)) {
                        logger.severe(checksum + " not equal to " + localchecksum);
                        return new Failure("Error in transferring DataCite.xml file to DPN",
                                "DPN Submission Failure: incomplete metadata transfer");
                    }
                    IOUtils.closeQuietly(digestInputStream);

                    
                    // Store BagIt file
                    String fileName = spaceName + "v" + dv.getFriendlyVersionNumber() + ".zip";

                        // Add BagIt ZIP file
                        //Although DPN uses SHA-256 internally, it's API uses MD5 to verify the transfer
                        messageDigest = MessageDigest.getInstance("MD5");
                        PipedInputStream in = new PipedInputStream();
                        PipedOutputStream out = new PipedOutputStream(in);
                        new Thread(new Runnable() {
                            public void run() {
                                try {
                                    //Get OREmap, convert from javax.json.JsonObject to com.google.gson.JsonObject
                                    OREMap oreMap = new OREMap(dv);
                                    JsonObject jsonOreMap = (JsonObject) new JsonParser().parse(oreMap.getOREMap().toString());
                                    
                                    //Generate bag
                                    //To do: Change to this when #5185/#5192 is merged.
                                    //The archival copy should include contact Emails regardless of the  :ExcludeEmailFromExport setting
                                    //BagGenerator bagger = new BagGenerator(new OREMap(dv, false), dataciteXml);
                                    BagGenerator bagger = new BagGenerator(new OREMap(dv), dataciteXml);
                                    bagger.setAuthenticationKey(token.getTokenString());
                                    bagger.generateBag(out);
                                } catch (Exception e) {
                                    logger.severe("Error creating bag: " + e.getMessage());
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                    IOUtils.closeQuietly(in);
                                    IOUtils.closeQuietly(out);
                                }
                            }
                        }).start();
                        digestInputStream = new DigestInputStream(in, messageDigest);
                        checksum = store.addContent(spaceName, fileName, digestInputStream, -1l, null, null,
                                null);
                        logger.fine("Content: " + fileName + " added with checksum: " + checksum);
                        localchecksum = Hex.encodeHexString(digestInputStream.getMessageDigest().digest());
                        if (!checksum.equals(localchecksum)) {
                            logger.severe(checksum + " not equal to " + localchecksum);
                            return new Failure("Error in transferring Zip file to DPN",
                                    "DPN Submission Failure: incomplete archive transfer");
                        }
                        IOUtils.closeQuietly(digestInputStream);
                        
                        
                        logger.fine("DPN Submission step: Content Transferred");
                        
                        // Document the location of dataset archival copy location (actually the URL where you can
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
                        logger.fine("DPN Submission step complete: " + sb.toString());
                    } catch (ContentStoreException | IOException e) {
                        // TODO Auto-generated catch block
                        logger.warning(e.getMessage());
                        e.printStackTrace();
                        return new Failure("Error in transferring file to DPN",
                                "DPN Submission Failure: archive file not transferred");
                    } catch (NoSuchAlgorithmException e) {
                        logger.severe("MD5 MessageDigest not available!");
                    }
                } catch (ContentStoreException e) {
                    logger.warning(e.getMessage());
                    e.printStackTrace();
                    String mesg = "DPN Submission Failure";
                    if (!(1 == dv.getVersion()) || !(0 == dv.getMinorVersionNumber())) {
                        mesg = mesg + ": Prior Version archiving not yet complete?";
                    }
                    return new Failure("Unable to create DPN space with name: " + spaceName, mesg);
                }
            } else {
                logger.warning("DPN Submision Workflow aborted: Dataset locked for pidRegister");
                return new Failure("Dataset locked");
            }
            return WorkflowStepResult.OK;
        } else {
            return new Failure("DPN Submission not configured.");
        }
    }
}
