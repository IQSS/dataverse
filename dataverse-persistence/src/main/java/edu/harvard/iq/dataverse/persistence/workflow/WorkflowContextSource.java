package edu.harvard.iq.dataverse.persistence.workflow;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionIdentifier;

/**
 * Marks the source of information about a workflow execution context.
 * @author kaczynskid
 */
public interface WorkflowContextSource extends DatasetVersionIdentifier {

    String getTriggerType();

    Long getDatasetId();

    Long getVersionNumber();

    Long getMinorVersionNumber();

    String getUserId();

    String getIpAddress();

    boolean isDatasetExternallyReleased();
}
