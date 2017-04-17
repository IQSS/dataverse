package edu.harvard.iq.dataverse.workflow.step;

import edu.harvard.iq.dataverse.workflow.WorkflowContext;
import java.util.Map;

/**
 * Atomic unit of workflows. This interface is implemented by classes performing
 * the workflow steps. We make very little assumptions about implementation of
 * this interface, as we expect most of them to come from through an SPI.
 * 
 * @author michael
 */
public interface WorkflowStep {
    
    /**
     * Performs the step of the workflow. Each step is run in its own transaction.
     * 
     * <em>NOTE:</em> If the step fails (i.e. returns {@link Failure}), it will not
     * be rolled back as part of the rollback process - the step has to clean up
     * on its own.
     * 
     * @param context Contains data about the invocation of this workflow.
     * @return A step result.
     */
    WorkflowStepResult run( WorkflowContext context );
    
    /**
     * Resumes a step when an external system has returned a response after finishing
     * the (lengthy) processing on its side.
     * 
     * @param context      Context of the workflow (Dataset, versions, etc).
     * @param internalData Data returned by this step in {@link Pending#getData()} returned when this step was last run.
     * @param externalData Data returned from the external system.
     * @return a step result. Any result is acceptable, including another {@link Pending}.
     */
    WorkflowStepResult resume( WorkflowContext context, Map<String,String> internalData, String externalData );
    
    /**
     * Attempt to roll back this step, if possible. The caller of this method assumes
     * that the step was completed successfully. Each step is rolled back in its own transaction.
     * 
     * @param context Required information about the workflow.
     * @param reason original reason for rolling back the workflow.
     */
    void rollback( WorkflowContext context, Failure reason );
    
}
