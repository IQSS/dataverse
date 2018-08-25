package edu.harvard.iq.dataverse.workflow.internalspi;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.engine.command.impl.SubmitArchiveCommand;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.workflow.WorkflowContext;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * A step that submits a BagIT bag of the newly published dataset version to DPN
 * via he Duracloud Vault API.
 * 
 * @author jimmyers
 */
@Stateless
public class DPNSubmissionWorkflowStep implements WorkflowStep {

    @EJB
    SettingsServiceBean settingsService;

    @EJB
    AuthenticationServiceBean authService;

    private static final Logger logger = Logger.getLogger(DPNSubmissionWorkflowStep.class.getName());


    public DPNSubmissionWorkflowStep(Map<String, String> paramSet) {
    }

    @Override
    public WorkflowStepResult run(WorkflowContext context) {

        return SubmitArchiveCommand.performDPNSubmission(
                context.getDataset().getVersion(context.getNextVersionNumber(), context.getNextMinorVersionNumber()),
                context.getRequest().getAuthenticatedUser(), settingsService, authService);
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
