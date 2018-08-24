package edu.harvard.iq.dataverse.workflow.internalspi;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLock.Reason;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.export.DataCiteExporter;
import edu.harvard.iq.dataverse.export.ExportException;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.bagit.BagIt_Export;
import edu.harvard.iq.dataverse.workflow.WorkflowContext;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;

import org.apache.commons.io.IOUtils;
import org.duracloud.client.ContentStore;
import org.duracloud.client.ContentStoreManager;
import org.duracloud.client.ContentStoreManagerImpl;
import org.duracloud.common.model.Credential;
import org.duracloud.error.ContentStoreException;

/**
 * A step that submits a BagIT bag of the newly published dataset version to DPN
 * via he Duracloud Vault API.
 * 
 * @author jimmyers
 */
public class DPNSubmissionWorkflowStep implements WorkflowStep {

	@EJB
	SettingsServiceBean settingsService;

	@EJB
	AuthenticationServiceBean authService;

	private static final Logger logger = Logger.getLogger(DPNSubmissionWorkflowStep.class.getName());
	private static final String DEFAULT_PORT = "443";
	private static final String DEFAULT_CONTEXT = "durastore";

	public DPNSubmissionWorkflowStep(Map<String, String> paramSet) {
	}

	@Override
	public WorkflowStepResult run(WorkflowContext context) {

		return performDPNSubmission(
				context.getDataset().getVersion(context.getNextVersionNumber(), context.getNextMinorVersionNumber()),
				context.getRequest().getAuthenticatedUser(), settingsService, authService);
	}

	public static WorkflowStepResult performDPNSubmission(DatasetVersion dv, AuthenticatedUser user, SettingsServiceBean settingsService, AuthenticationServiceBean authService) {

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
						String localchecksum = new BigInteger(1, digestInputStream.getMessageDigest().digest())
								.toString(16);
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
						localchecksum = new BigInteger(1, digestInputStream.getMessageDigest().digest()).toString(16);
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

	@Override
	public WorkflowStepResult resume(WorkflowContext context, Map<String, String> internalData, String externalData) {
		throw new UnsupportedOperationException("Not supported yet."); // This class does not need to resume.
	}

	@Override
	public void rollback(WorkflowContext context, Failure reason) {
		logger.log(Level.INFO, "rolling back workflow invocation {0}", context.getInvocationId());
		logger.warning("Manual cleanup of DPN Space: " + context.getDataset().getGlobalId().asString().replace(':', '-')
				.replace('/', '-').replace('.', '-').toLowerCase() + " may be required");
	}
}
