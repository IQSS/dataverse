package edu.harvard.iq.dataverse.persistence.workflow;

/**
 * Marks the source of information about a workflow execution context.
 * @author kaczynskid
 */
public interface WorkflowContextSource {

    String getTriggerType();

    long getDatasetId();

    long getMajorVersionNumber();

    long getMinorVersionNumber();

    String getUserId();

    String getIpAddress();

    boolean isDatasetExternallyReleased();
}
