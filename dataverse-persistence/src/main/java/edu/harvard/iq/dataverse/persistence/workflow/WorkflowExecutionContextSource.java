package edu.harvard.iq.dataverse.persistence.workflow;

public interface WorkflowExecutionContextSource extends WorkflowContextSource {

    long getId();

    long getWorkflowId();
}
