package edu.harvard.iq.dataverse.workflow.internalspi;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.impl.SubmitArchiveCommand;
import edu.harvard.iq.dataverse.workflow.WorkflowContext;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;

/**
 * A step that submits a BagIT bag of the newly published dataset version to DPN
 * via he Duracloud Vault API.
 * 
 * @author jimmyers
 */

public class DPNSubmissionWorkflowStep implements WorkflowStep {

    private static final Logger logger = Logger.getLogger(DPNSubmissionWorkflowStep.class.getName());

    public DPNSubmissionWorkflowStep(Map<String, String> paramSet) {
    }

    @Override
    public WorkflowStepResult run(WorkflowContext context) {
        String host=(String) context.getSettings().get("DuraCloudHost");
        String port = (String) context.getSettings().get("DuraCloudPort");
        String dpnContext = (String) context.getSettings().get("DuraCloudContext");
        if(host==null) {
            logger.severe("No DuraCloudHost - DPN Submission not attempted");
            return new Failure("No DuraCloudHost", "DuraCloudHost not found in Settings");
        } else {
        return SubmitArchiveCommand.performDPNSubmission(
                context.getDataset().getVersion(context.getNextVersionNumber(), context.getNextMinorVersionNumber()),
                context.getRequest().getAuthenticatedUser(), host, port, dpnContext, context.getApiToken());
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
