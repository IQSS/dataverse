package edu.harvard.iq.dataverse.workflow.step;

/**
 * The result of performing a {@link WorkflowStep}.
 * @author michael
 */
public interface WorkflowStepResult {
    public static final WorkflowStepResult OK = new WorkflowStepResult(){
        @Override 
        public String toString() {
            return "WorkflowStepResult.OK";
        }
    };
}
