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
    public WorkflowStep getStep(String stepId, Map<String, String> stepParameters) {
        switch (stepId) {
            case "logging":
                return new LoggingWorkflowStep(stepParameters);
            case "pause":
                return new PauseStep(stepParameters);
            default:
                return null;
        }
    }
    
}
