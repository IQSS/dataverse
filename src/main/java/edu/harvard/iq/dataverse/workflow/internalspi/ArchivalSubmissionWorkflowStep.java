package edu.harvard.iq.dataverse.workflow.internalspi;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.DPNSubmitToArchiveCommand;
import edu.harvard.iq.dataverse.workflow.WorkflowContext;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A step that submits a BagIT bag of the newly published dataset version via a configured archiver.
 * 
 * @author jimmyers
 */

public class ArchivalSubmissionWorkflowStep implements WorkflowStep {

    private static final Logger logger = Logger.getLogger(ArchivalSubmissionWorkflowStep.class.getName());

    public ArchivalSubmissionWorkflowStep(Map<String, String> paramSet) {
    }

    @Override
    public WorkflowStepResult run(WorkflowContext context) {
        Map<String, String> requestedSettings = new HashMap<String, String>();
        String host=(String) context.getSettings().get(":DuraCloudHost");
        requestedSettings.put(":DuraCloudHost", host);
        //String port = (String) context.getSettings().get(":DuraCloudPort");
        //String dpnContext = (String) context.getSettings().get(":DuraCloudContext");
        if(host==null) {
            logger.severe("No DuraCloudHost - DPN Submission not attempted");
            return new Failure("No DuraCloudHost", "DuraCloudHost not found in Settings");
        } else {
        return (new DPNSubmitToArchiveCommand(new DataverseRequest(context.getDataset().getReleasedVersion(), context.getRequest().getAuthenticatedUser())))
                .performDPNSubmission(context.getDataset().getReleasedVersion() context.getApiToken(), requestedSettings);
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
