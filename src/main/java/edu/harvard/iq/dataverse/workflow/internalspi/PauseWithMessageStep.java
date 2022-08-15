package edu.harvard.iq.dataverse.workflow.internalspi;

import edu.harvard.iq.dataverse.workflow.WorkflowContext;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.Pending;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;
import edu.harvard.iq.dataverse.workflows.WorkflowUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


/**
 * A sample step that pauses the workflow.
 * 
 * @author michael
 */
public class PauseWithMessageStep implements WorkflowStep {
    
    /** Constant used by testing to simulate a failed step. */
    public static final String FAILURE_RESPONSE="fail";
    
    private final Map<String,String> params = new HashMap<>();

    public PauseWithMessageStep( Map<String,String> paramSet ) {
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
        return WorkflowUtil.parseResponse(externalData);
    }

    @Override
    public void rollback(WorkflowContext context, Failure reason) {
        // nothing to roll back
    }
    
}
