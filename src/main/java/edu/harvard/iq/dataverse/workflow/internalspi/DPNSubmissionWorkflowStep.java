package edu.harvard.iq.dataverse.workflow.internalspi;

import edu.harvard.iq.dataverse.export.BagIt_Exporter;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.export.OAI_OREExporter;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.workflow.WorkflowContext;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;

import org.apache.commons.cli.Options;
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

	private static final Logger logger = Logger.getLogger(DPNSubmissionWorkflowStep.class.getName());
	private static final String DEFAULT_PORT = "443";
	private static final String DEFAULT_CONTEXT = "durastore";

	private String host;
	private String username;
	private String password;
	private String spaceId;
	private String contentPath;

	private final Map<String, String> params;

	public DPNSubmissionWorkflowStep(Map<String, String> paramSet) {
		params = new HashMap<>(paramSet);
	}

	@Override
	public WorkflowStepResult run(WorkflowContext context) {
		for (String key : params.keySet()) {
			logger.info("DPN Workflow Param: " + key + " : " + params.get(key));
		}
		String port = params.containsKey("port") ? params.get("port") : DEFAULT_PORT;
		String dpnContext = params.containsKey("context") ? params.get("context") : DEFAULT_CONTEXT;
		ContentStoreManager storeManager = new ContentStoreManagerImpl(params.get("host"), port, dpnContext);
		Credential credential = new Credential(username, password);
		storeManager.login(credential);

		ContentStore store;
		try {
			store = storeManager.getPrimaryContentStore();

			String name = context.getDataset().getGlobalId().asString().replace(':', '.').replace('/', '-')
					.toLowerCase();
			store.createSpace(name);
			// Store file

			String contentId = name + ".zip";
			String checksum = store.addContent(spaceId, contentId,
					ExportService.getInstance(settingsService).getExport(context.getDataset(), BagIt_Exporter.NAME),
					-1l, null, null, null);
			System.out.println("Content added with checksum: " + checksum);

			// Print contents of space after content addition
			System.out.println("\n\n*** Content Listing of " + spaceId + " - after file is added ***\n\n");
			printContentListing(store, spaceId);
			
			logger.info("DPN step:");
			logger.log(Level.INFO, "Submitted {0} to DPN", name);
			logger.log(Level.INFO, "Dataset id:{0}", context.getDataset().getId());
			logger.log(Level.INFO, "Trigger Type {0}", context.getType());
			logger.log(Level.INFO, "Next version:{0}.{1} isMinor:{2}", new Object[] { context.getNextVersionNumber(),
					context.getNextMinorVersionNumber(), context.isMinorRelease() });

		} catch (ContentStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Delete file
		// store.deleteContent(spaceId, contentId);

		// Print contents of space after content is removed
		// System.out.println("\n\n*** Content Listing of " + spaceId +
		// " - after file is deleted ***\n\n");
		// printContentListing(store, spaceId);

		// System.out.println("\n\nSimple API Example process complete.");
		// }


		return WorkflowStepResult.OK;
	}

	/**
	 * Prints contents of space
	 */
	private void printContentListing(ContentStore store, String spaceId) throws ContentStoreException {
		Iterator<String> spaceContents = store.getSpaceContents(spaceId, null);
		while (spaceContents.hasNext()) {
			System.out.println(spaceContents.next());
		}
	}

	@Override
	public WorkflowStepResult resume(WorkflowContext context, Map<String, String> internalData, String externalData) {
		throw new UnsupportedOperationException("Not supported yet."); // This class does not need to resume.
	}

	@Override
	public void rollback(WorkflowContext context, Failure reason) {
		logger.log(Level.INFO, "rolling back workflow invocation {0}", context.getInvocationId());
	}
}
