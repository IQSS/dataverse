package edu.harvard.iq.dataverse.workflow.internalspi;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.citation.CitationFactory;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.AbstractSubmitToArchiveCommand;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.workflow.execution.WorkflowExecutionContext;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;

import javax.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.util.ArchiverUtil.createSubmitToArchiveCommand;

/**
 * A step that submits a BagIT bag of the newly published dataset version via a
 * configured archiver.
 *
 * @author jimmyers
 */

public class ArchivalSubmissionWorkflowStep implements WorkflowStep {

    private static final Logger logger = Logger.getLogger(ArchivalSubmissionWorkflowStep.class.getName());

    private final DatasetVersionServiceBean versionsService;
    private final CitationFactory citationFactory;
    private final AuthenticationServiceBean authenticationService;
    private final Clock clock;

    public ArchivalSubmissionWorkflowStep(DatasetVersionServiceBean versionsService, CitationFactory citationFactory,
                                          AuthenticationServiceBean authenticationService, Clock clock) {
        this.versionsService = versionsService;
        this.citationFactory = citationFactory;
        this.authenticationService = authenticationService;
        this.clock = clock;
    }

    @Override
    public WorkflowStepResult run(WorkflowExecutionContext context) {
        Map<String, String> requestedSettings = new HashMap<>();
        Map<String, Object> typedSettings = context.getSettings();
        for (String setting : (typedSettings.keySet())) {
            Object val = typedSettings.get(setting);
            String stringValue = val != null ? val.toString() : null;
            requestedSettings.put(setting, stringValue);
        }

        return versionsService.withDatasetVersion(context,
                datasetVersion -> {
                    DataverseRequest dvr = new DataverseRequest(context.getRequest().getAuthenticatedUser(), (HttpServletRequest) null);
                    String className = requestedSettings.get(SettingsServiceBean.Key.ArchiverClassName.toString());
                    AbstractSubmitToArchiveCommand archiveCommand
                            = createSubmitToArchiveCommand(className, dvr, datasetVersion, authenticationService, clock);
                    if (archiveCommand != null) {
                        return archiveCommand.performArchiveSubmission(datasetVersion, requestedSettings, citationFactory);
                    } else {
                        logger.severe("No Archiver instance could be created for name: " + className);
                        return new Failure("No Archiver", "Could not create instance of class: " + className);
                    }
                }).orElseGet(() -> new Failure("Dataset version not found"));
    }

    @Override
    public WorkflowStepResult resume(WorkflowExecutionContext context, Map<String, String> internalData, String externalData) {
        throw new UnsupportedOperationException("Not supported yet."); // This class does not need to resume.
    }

    @Override
    public void rollback(WorkflowExecutionContext context, Failure reason) {
        logger.log(Level.INFO, "rolling back workflow invocation {0}", context.getInvocationId());
        versionsService.withDatasetVersion(context,
                datasetVersion -> {
                    logger.warning(
                            () -> String.format("Manual cleanup of Archive for: %s, version: %s may be required",
                                    datasetVersion.getDataset().getGlobalId().asString(), datasetVersion));
                    return datasetVersion;
                });
    }
}
