package edu.harvard.iq.dataverse.engine.command.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.citation.CitationFactory;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.export.datacite.DataCiteResourceCreator;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock.Reason;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.ApiToken;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.bagit.BagGenerator;
import edu.harvard.iq.dataverse.util.bagit.OREMap;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.Success;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;
import org.apache.commons.codec.binary.Hex;
import org.duracloud.client.ContentStore;
import org.duracloud.client.ContentStoreManager;
import org.duracloud.client.ContentStoreManagerImpl;
import org.duracloud.common.model.Credential;
import org.duracloud.error.ContentStoreException;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredPermissions(Permission.PublishDataset)
public class DuraCloudSubmitToArchiveCommand extends AbstractSubmitToArchiveCommand implements Command<DatasetVersion> {

    private static final Logger logger = Logger.getLogger(DuraCloudSubmitToArchiveCommand.class.getName());
    private static final String DEFAULT_PORT = "443";
    private static final String DEFAULT_CONTEXT = "durastore";
    private static final String DURACLOUD_PORT = ":DuraCloudPort";
    private static final String DURACLOUD_HOST = ":DuraCloudHost";
    private static final String DURACLOUD_CONTEXT = ":DuraCloudContext";

    public DuraCloudSubmitToArchiveCommand(DataverseRequest aRequest, DatasetVersion version,
                                           AuthenticationServiceBean authenticationService, Clock clock) {
        super(aRequest, version, authenticationService, clock);
    }

    @Override
    public WorkflowStepResult performArchiveSubmission(
            DatasetVersion dv, Map<String, String> requestedSettings, CitationFactory citationFactory) {

        if (!getUser().isAuthenticated()) {
            return new Failure("User must be authenticated");
        }
        ApiToken token = getOrGenerateToken((AuthenticatedUser) getUser());
        String port = requestedSettings.get(DURACLOUD_PORT) != null ? requestedSettings.get(DURACLOUD_PORT) : DEFAULT_PORT;
        String dpnContext = requestedSettings.get(DURACLOUD_CONTEXT) != null ? requestedSettings.get(DURACLOUD_CONTEXT) : DEFAULT_CONTEXT;
        String host = requestedSettings.get(DURACLOUD_HOST);
        String dataverseUrl = requestedSettings.get(SettingsServiceBean.Key.SiteUrl.toString());

        if (host == null) {
            return new Failure("DuraCloud Submission not configured - no \":DuraCloudHost\".");
        }

        Dataset dataset = dv.getDataset();

        if (dataset.getLockFor(Reason.pidRegister) != null) {
            logger.warning("DuraCloud Submision Workflow aborted: Dataset locked for pidRegister");
            return new Failure("Dataset locked");
        }

        // Use Duracloud client classes to login
        ContentStoreManager storeManager = new ContentStoreManagerImpl(host, port, dpnContext);
        Credential credential = new Credential(System.getProperty("duracloud.username"),
                                               System.getProperty("duracloud.password"));
        storeManager.login(credential);

        String spaceName = dataset.getGlobalId().asString()
                .replace(':', '-')
                .replace('/', '-')
                .replace('.', '-')
                .toLowerCase();

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
            String publicationYear = citationFactory.create(dv)
                    .getCitationData()
                    .getYear();

            String dataciteXml;
            try {
                XmlMapper mapper = new XmlMapper();
                mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
                dataciteXml = mapper.writeValueAsString(new DataCiteResourceCreator()
                        .create(dv.getDataset().getGlobalId().asString(), publicationYear, dv.getDataset()));
            } catch (JsonProcessingException jpe) {
                logger.log(Level.WARNING, "Error while creating XML", jpe);
                throw new RuntimeException(jpe);
            }

            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            try (PipedInputStream dataciteIn = new PipedInputStream(); DigestInputStream digestInputStream = new DigestInputStream(dataciteIn, messageDigest)) {
                // Add datacite.xml file

                new Thread(() -> {
                    try (PipedOutputStream dataciteOut = new PipedOutputStream(dataciteIn)) {
                        dataciteOut.write(dataciteXml.getBytes(StandardCharsets.UTF_8));
                    } catch (Exception e) {
                        logger.severe("Error creating datacite.xml: " + e.getMessage());
                        throw new RuntimeException("Error creating datacite.xml: " + e.getMessage());
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
                try (PipedInputStream in = new PipedInputStream(); DigestInputStream digestInputStream2 = new DigestInputStream(in, messageDigest)) {
                    new Thread(() -> {
                        try (PipedOutputStream out = new PipedOutputStream(in)) {
                            // Generate bag
                            BagGenerator bagger = new BagGenerator(new OREMap(dv, false, dataverseUrl), dataciteXml);
                            bagger.setAuthenticationKey(token.getTokenString());
                            bagger.generateBag(out);
                        } catch (Exception e) {
                            logger.severe("Error creating bag: " + e.getMessage());
                            throw new RuntimeException("Error creating bag: " + e.getMessage());
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
                // where you can view it as an admin)
                StringBuilder sb = new StringBuilder("https://").append(host);
                if (!"443".equals(port)) {
                    sb.append(":").append(port);
                }
                sb.append("/duradmin/spaces/sm/")
                        .append(store.getStoreId())
                        .append("/").append(spaceName).append("/")
                        .append(fileName);
                dv.setArchivalCopyLocation(sb.toString());
                logger.fine("DuraCloud Submission step complete: " + sb.toString());
            } catch (ContentStoreException | IOException e) {
                logger.warning(e.getMessage());
                return new Failure("Error in transferring file to DuraCloud",
                                   "DuraCloud Submission Failure: archive file not transferred");
            } catch (RuntimeException rte) {
                logger.severe(rte.getMessage());
                return new Failure("Error in generating datacite.xml file",
                                   "DuraCloud Submission Failure: metadata file not created");
            }
        } catch (ContentStoreException e) {
            logger.warning(e.getMessage());
            String mesg = "DuraCloud Submission Failure";
            if (!(1 == dv.getVersion()) || !(0 == dv.getMinorVersionNumber())) {
                mesg = mesg + ": Prior Version archiving not yet complete?";
            }
            return new Failure("Unable to create DuraCloud space with name: " + spaceName, mesg);
        } catch (NoSuchAlgorithmException e) {
            logger.severe("MD5 MessageDigest not available!");
        }
        return new Success();
    }
}
