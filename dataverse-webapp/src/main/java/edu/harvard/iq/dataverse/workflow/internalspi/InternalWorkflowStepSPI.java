package edu.harvard.iq.dataverse.workflow.internalspi;

import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.workflow.WorkflowStepRegistry;
import edu.harvard.iq.dataverse.workflow.WorkflowStepSPI;
import edu.harvard.iq.dataverse.workflow.step.ClearWorkingDirWorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepParams;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

/**
 * Provider for steps that are available internally.
 *
 * @author michael
 */
@Startup
@Singleton
public class InternalWorkflowStepSPI implements WorkflowStepSPI {

    public static final String INTERNAL_PROVIDER_ID = "internal";

    private final WorkflowStepRegistry stepRegistry;
    private final DatasetVersionServiceBean datasetVersions;

    // -------------------- CONSTRUCTORS --------------------

    @Inject
    public InternalWorkflowStepSPI(WorkflowStepRegistry stepRegistry, DatasetVersionServiceBean datasetVersions) {
        this.stepRegistry = stepRegistry;
        this.datasetVersions = datasetVersions;
    }

    @PostConstruct
    public void init() {
        stepRegistry.register(INTERNAL_PROVIDER_ID, this);
    }

    // -------------------- LOGIC --------------------

    @Override
    public WorkflowStep getStep(String stepType, WorkflowStepParams stepParameters) {
        switch (stepType) {
            case LoggingWorkflowStep.STEP_ID:
                return new LoggingWorkflowStep(stepParameters);
            case "pause":
                return new PauseStep(stepParameters);
            case "http/sr":
                return new HttpSendReceiveClientStep(stepParameters, datasetVersions);
            case "archiver":
                return new ArchivalSubmissionWorkflowStep(datasetVersions);
            case SystemProcessStep.STEP_ID:
                return new SystemProcessStep(stepParameters);
            case ClearWorkingDirWorkflowStep.STEP_ID:
                return new ClearWorkingDirWorkflowStep(stepParameters);
            default:
                throw new IllegalArgumentException("Unsupported step type: '" + stepType + "'.");
        }
    }

}
