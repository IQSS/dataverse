package edu.harvard.iq.dataverse.workflow.internalspi;

import edu.harvard.iq.dataverse.workflow.WorkflowStepSPI;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import java.util.Map;

/**
 * Provider for steps that are available internally.
 * @author michael
 */
public class InternalWorkflowStepSP implements WorkflowStepSPI {

    @Override
    public WorkflowStep getStep(String stepType, Map<String, String> stepParameters) {
        switch (stepType) {
            case "log":
                return new LoggingWorkflowStep(stepParameters);
            case "pause":
                return new PauseStep(stepParameters);
            case "http/sr":
                return new HttpSendReceiveClientStep(stepParameters);
            case "http/authExt":
                return new AuthorizedExternalStep(stepParameters);
            case "archiver":
                return new ArchivalSubmissionWorkflowStep(stepParameters);
            default:
                throw new IllegalArgumentException("Unsupported step type: '" + stepType + "'.");
        }
    }
    
}
