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
    private final boolean isCompleted;

    public PublishDatasetResult(Dataset dataset, boolean isCompleted) {
        this.dataset = dataset;
        this.isCompleted = isCompleted;
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
        return isCompleted;
    }
    
}
