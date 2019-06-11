package edu.harvard.iq.dataverse.workflow;

import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import java.util.Map;

/**
 * Interface for step factories. Implement this interface and register an instance 
 * of it with the {@link WorkflowServiceBean} in order to make the steps available
 * to workflows.
 * 
 * 
 * @author michael
 */
public interface WorkflowStepSPI {
    
    WorkflowStep getStep( String stepType, Map<String,String> stepParameters );
    
}
