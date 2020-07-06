package edu.harvard.iq.dataverse.workflow;

import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepParams;

/**
 * Interface for step factories. Implement this interface and register an instance
 * of it with the {@link WorkflowStepRegistry} in order to make the steps available
 * to workflows.
 *
 * @author michael
 */
public interface WorkflowStepSPI {

    WorkflowStep getStep(String stepType, WorkflowStepParams stepParameters);

}
