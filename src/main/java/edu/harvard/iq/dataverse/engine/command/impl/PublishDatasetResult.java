package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLock;

/**
 * The result of an attempt to publish a dataset.
 * 
 * @author michael
 */
public class PublishDatasetResult {
    
    private final Dataset dataset;
    private final Status status;
    
    public enum Status {
        /** Dataset has been published */
        Completed,
        /** Publish workflow is in progress */
        Workflow,
        /** Publishing is being finalized asynchronously */
        Inprogress
    }
        

    public PublishDatasetResult(Dataset dataset, Status status) {
        this.dataset = dataset;
        this.status = status;
    }
    
    /**
     * @return the dataset that was published.
     */
    public Dataset getDataset() {
        return dataset;
    }

    /**
     * A publication operation can either be completed or pending, in case there's
     * a workflow that has to be completed before the dataset publication is done.
     * Workflows can take an arbitrary amount of time, as they might require external
     * systems to perform lengthy operations, or have a human manually validate them.
     * 
     * @return {@code true} iff the publication process was completed. {@code false} otherwise.
     */
    public boolean isCompleted() {
        return status == Status.Completed;
    }
    
    public boolean isWorkflow() {
        return status == Status.Workflow;
    }
    
    public boolean isInProgress() {
        return status == Status.Inprogress;
    }
}
