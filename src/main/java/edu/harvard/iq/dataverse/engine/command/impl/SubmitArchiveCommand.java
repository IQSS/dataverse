package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DatasetLock.Reason;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.export.DataCiteExporter;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.bagit.BagIt_Export;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import static edu.harvard.iq.dataverse.engine.command.CommandHelper.CH;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.math.BigInteger;
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

@RequiredPermissions(Permission.ArchiveDatasetVersion)
public class SubmitArchiveCommand implements Command<DatasetVersion> {

    private final DatasetVersion version;
    private final DataverseRequest request;
    private final Map<String, DvObject> affectedDvObjects;
    private static final Logger logger = Logger.getLogger(SubmitArchiveCommand.class.getName());
	private static final String DEFAULT_PORT = "443";
	private static final String DEFAULT_CONTEXT = "durastore";

    public SubmitArchiveCommand(DataverseRequest aRequest, DatasetVersion version) {
    	this.request=aRequest;
        this.version = version;
        affectedDvObjects = new HashMap<>();
        affectedDvObjects.put("", version.getDataset());
    }

    @Override
    public DatasetVersion execute(CommandContext ctxt) throws CommandException {
    	
    	performDPNSubmission(version, request.getAuthenticatedUser(), ctxt.settings(), ctxt.authentication());
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
    	//Is this ever used?
        return "DatasetVersion: " + version.getId() + " " + version.getDataset().getDisplayName() + ".v" + version.getFriendlyVersionNumber();
    }
    
	public static WorkflowStepResult performDPNSubmission(DatasetVersion dv, AuthenticatedUser user, SettingsServiceBean settingsService, AuthenticationServiceBean authService) {
if(settingsService == null) {
	logger.severe("Nul SS bean");
}
		String host = settingsService.getValueForKey(SettingsServiceBean.Key.DuraCloudHost);
		if (host != null) {
			Dataset dataset = dv.getDataset();
			if (dataset.getLockFor(Reason.pidRegister) == null) {
				String port = settingsService.getValueForKey(SettingsServiceBean.Key.DuraCloudPort, DEFAULT_PORT);
				String dpnContext = settingsService.getValueForKey(SettingsServiceBean.Key.DuraCloudContext, DEFAULT_CONTEXT);

				ContentStoreManager storeManager = new ContentStoreManagerImpl(host, port, dpnContext);
				Credential credential = new Credential(System.getProperty("duracloud.username"), System.getProperty("duracloud.password"));
				storeManager.login(credential);

				String spaceName = dataset.getGlobalId().asString().replace(':', '-').replace('/', '-')
						.replace('.', '-').toLowerCase();

				ContentStore store;
				try {
					store = storeManager.getPrimaryContentStore();

					store.createSpace(spaceName);

					// Store file

					String fileName = spaceName + "v" + dv.getFriendlyVersionNumber() + ".zip";
					try {

						// Add BagIt ZIP file
						MessageDigest messageDigest = MessageDigest.getInstance("MD5");

						ApiToken token = authService.findApiTokenByUser(user);
						if ((token == null) || (token.getExpireTime().before(new Date()))) {
							token = authService.generateApiTokenForUser(user);
						}
						final ApiToken finalToken = token;

						PipedInputStream in = new PipedInputStream();
						PipedOutputStream out = new PipedOutputStream(in);
						new Thread(new Runnable() {
							public void run() {
								try {
									BagIt_Export.exportDatasetVersionAsBag(dv, finalToken, settingsService, out);
									out.close();
								} catch (Exception e) {
									logger.severe("Error creating bag: " + e.getMessage());
									// TODO Auto-generated catch block
									e.printStackTrace();
									IOUtils.closeQuietly(in);
									IOUtils.closeQuietly(out);
								}
							}
						}).start();

						DigestInputStream digestInputStream = new DigestInputStream(in, messageDigest);

						String checksum = store.addContent(spaceName, fileName, digestInputStream, -1l, null, null,
								null);
						logger.info("Content: " + fileName + " added with checksum: " + checksum);
						String localchecksum = Hex.encodeHexString(digestInputStream.getMessageDigest().digest());
						if (!checksum.equals(localchecksum)) {
							logger.severe(checksum + " not equal to " + localchecksum);
							return new Failure("Error in transferring Zip file to DPN",
									"DPN Submission Failure: incomplete archive transfer");
						}
						IOUtils.closeQuietly(digestInputStream);
						// Add datacite.xml file
						messageDigest = MessageDigest.getInstance("MD5");

						PipedInputStream dataciteIn = new PipedInputStream();
						PipedOutputStream dataciteOut = new PipedOutputStream(dataciteIn);
						new Thread(new Runnable() {
							public void run() {
								try {
									ExportService.getInstance(settingsService).exportFormatToStream(dv,
											DataCiteExporter.NAME, dataciteOut);
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

						digestInputStream = new DigestInputStream(dataciteIn, messageDigest);

						checksum = store.addContent(spaceName, "datacite.xml", digestInputStream, -1l, null, null,
								null);
						logger.info("Content: datacite.xml added with checksum: " + checksum);
						localchecksum = Hex.encodeHexString(digestInputStream.getMessageDigest().digest());
						if (!checksum.equals(localchecksum)) {
							logger.severe(checksum + " not equal to " + localchecksum);
							return new Failure("Error in transferring DataCite.xml file to DPN",
									"DPN Submission Failure: incomplete metadata transfer");
						}
						IOUtils.closeQuietly(digestInputStream);
						
						logger.info("DPN Submission step: Content Transferred");
						// Document location of dataset version replica (actually the URL where you can
						// view it as an admin)
						StringBuffer sb = new StringBuffer("https://");
						sb.append(host);
						if (!port.equals("443")) {
							sb.append(":" + port);
						}
						sb.append("/duradmin/spaces/sm/");
						sb.append(store.getStoreId());
						sb.append("/" + spaceName + "/" + fileName);
						dv.setReplicaLocation(sb.toString());
						logger.info("DPN Submission step complete: " + sb.toString());
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
					// TODO Auto-generated catch block
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
