package edu.harvard.iq.dataverse.workflow.internalspi;

import edu.harvard.iq.dataverse.workflow.WorkflowStepSPI;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepParams;

/**
 * Provider for steps that are available internally.
 *
 * @author michael
 */
public class InternalWorkflowStepSPI implements WorkflowStepSPI {

    public static final String INTERNAL_PROVIDER_ID = "internal";

    @Override
    public WorkflowStep getStep(String stepType, WorkflowStepParams stepParameters) {
        switch (stepType) {
            case LoggingWorkflowStep.STEP_ID:
                return new LoggingWorkflowStep(stepParameters);
            case "pause":
                return new PauseStep(stepParameters);
            case "http/sr":
                return new HttpSendReceiveClientStep(stepParameters);
            case "archiver":
                return new ArchivalSubmissionWorkflowStep();
            case SystemProcessStep.STEP_ID:
                return new SystemProcessStep(stepParameters);
            default:
                throw new IllegalArgumentException("Unsupported step type: '" + stepType + "'.");
        }
    }

}
