package edu.harvard.iq.dataverse.workflow.internalspi;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.AbstractSubmitToArchiveCommand;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.ArchiverUtil;
import edu.harvard.iq.dataverse.workflow.WorkflowContext;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.http.HttpServletRequest;

/**
 * A step that submits a BagIT bag of the newly published dataset version via a
 * configured archiver.
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
        Map<String, Object> typedSettings = context.getSettings();
        for (String setting : (typedSettings.keySet())) {
            Object val = typedSettings.get(setting);
            if (val instanceof String) {
                requestedSettings.put(setting, (String) val);
            } else if (val instanceof Boolean) {
                requestedSettings.put(setting, ((Boolean) val).booleanValue() ? "true" : "false");
            } else if (val instanceof Long) {
                requestedSettings.put(setting, val.toString());
            }
        }

        DataverseRequest dvr = new DataverseRequest(context.getRequest().getAuthenticatedUser(), (HttpServletRequest) null);
        String className = requestedSettings.get(SettingsServiceBean.Key.ArchiverClassName.toString());
        AbstractSubmitToArchiveCommand archiveCommand = ArchiverUtil.createSubmitToArchiveCommand(className, dvr, context.getDataset().getReleasedVersion());
        if (archiveCommand != null) {
            return (archiveCommand.performArchiveSubmission(context.getDataset().getReleasedVersion(), context.getApiToken(), requestedSettings));
        } else {
            logger.severe("No Archiver instance could be created for name: " + className);
            return new Failure("No Archiver", "Could not create instance of class: " + className);
        }

    }

    @Override
    public WorkflowStepResult resume(WorkflowContext context, Map<String, String> internalData, String externalData) {
        throw new UnsupportedOperationException("Not supported yet."); // This class does not need to resume.
    }

    @Override
    public void rollback(WorkflowContext context, Failure reason) {
        logger.log(Level.INFO, "rolling back workflow invocation {0}", context.getInvocationId());
        logger.warning("Manual cleanup of Archive for: " + context.getDataset().getGlobalId().asString() + ", version: " + context.getDataset().getReleasedVersion() + " may be required");
    }
}
