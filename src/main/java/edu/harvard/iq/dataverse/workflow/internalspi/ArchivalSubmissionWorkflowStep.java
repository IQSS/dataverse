package edu.harvard.iq.dataverse.workflow.internalspi;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLock.Reason;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.AbstractSubmitToArchiveCommand;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.ArchiverUtil;
import edu.harvard.iq.dataverse.util.bagit.OREMap;
import edu.harvard.iq.dataverse.util.json.JsonLDTerm;
import edu.harvard.iq.dataverse.workflow.WorkflowContext;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.json.JsonObject;
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

        Dataset d = context.getDataset();
        if (d.isLockedFor(Reason.FileValidationFailed)) {
            logger.severe("Dataset locked for file validation failure - will not archive");
            return new Failure("File Validation Lock", "Dataset has file validation problem - will not archive");
        }
        DataverseRequest dvr = new DataverseRequest(context.getRequest().getAuthenticatedUser(), (HttpServletRequest) null);
        String className = requestedSettings.get(SettingsServiceBean.Key.ArchiverClassName.toString());
        AbstractSubmitToArchiveCommand archiveCommand = ArchiverUtil.createSubmitToArchiveCommand(className, dvr, context.getDataset().getReleasedVersion());
        if (archiveCommand != null) {
            // Generate the required components for archiving
            DatasetVersion version = context.getDataset().getReleasedVersion();
            
            // Generate DataCite XML
            String dataCiteXml = archiveCommand.getDataCiteXml(version);
            
            // Generate OREMap
            OREMap oreMap = new OREMap(version, false);
            JsonObject ore = oreMap.getOREMap();
            
            // Get JSON-LD terms
            Map<String, JsonLDTerm> terms = archiveCommand.getJsonLDTerms(oreMap);
            
            // Call the updated method with all required parameters
            /*
             * Note: because this must complete before the workflow can complete and update the version status
             * in the db a long-running archive submission via workflow could hit a transaction timeout and fail.
             * The commands themselves have been updated to run archive submission outside of any transaction
             * and update the status in a separate transaction, so archiving a given version that way could 
             * succeed where this workflow failed.
             * 
             * Another difference when running in a workflow - this step has no way to set the archiving status to 
             * pending as is done when running archiving from the UI/API. Instead, there is a generic workflow
             * lock on the dataset. 
             */
            return archiveCommand.performArchiveSubmission(
                version, 
                dataCiteXml, 
                ore, 
                terms, 
                context.getApiToken(), 
                requestedSettings
            );

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
