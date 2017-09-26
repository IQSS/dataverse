package edu.harvard.iq.dataverse.workflow.internalspi;

import edu.harvard.iq.dataverse.workflow.WorkflowContext;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.Pending;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A sample step that pauses the workflow.
 * 
 * @author michael
 */
public class PauseStep implements WorkflowStep {
    
    private static final Logger logger = Logger.getLogger(PauseStep.class.getName());
    
    /** Constant used by testing to simulate a failed step. */
    public static final String FAILURE_RESPONSE="fail";
    
    private final Map<String,String> params = new HashMap<>();

    public PauseStep( Map<String,String> paramSet ) {
        params.putAll(paramSet);
    }
    
    @Override
    public WorkflowStepResult run(WorkflowContext context) {
        final Pending result = new Pending();
        result.getData().putAll(params);
        return result;
    }

    @Override
    public WorkflowStepResult resume(WorkflowContext context, Map<String, String> internalData, String externalData) {
        logger.log(Level.INFO, "local parameters match: {0}", internalData.equals(params));
        logger.log(Level.INFO, "externalData: \"{0}\"", externalData);
        return externalData.trim().equals(FAILURE_RESPONSE) ? new Failure("Simulated fail") : WorkflowStepResult.OK;
    }

    @Override
    public void rollback(WorkflowContext context, Failure reason) {
        // nothing to roll back
    }
    
}
