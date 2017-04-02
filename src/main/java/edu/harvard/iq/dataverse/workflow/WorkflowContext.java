package edu.harvard.iq.dataverse.workflow.step;

import edu.harvard.iq.dataverse.Dataset;

/**
 * The context in which the workflow is performed. Contains information steps might
 * need, such as the dataset being worked on an version data.
 * 
 * Design-wise, this class allows us to add parameters to {@link WorkflowStep} without 
 * changing its method signatures, which would break break client code.
 * 
 * @author michael
 */
public class WorkflowContext {
    
    private final Dataset dataset;
    private final long nextVersionNumber;
    private final long nextMinorVersionNumber;

    public WorkflowContext(Dataset dataset, long nextVersionNumber, long nextMinorVersionNumber) {
        this.dataset = dataset;
        this.nextVersionNumber = nextVersionNumber;
        this.nextMinorVersionNumber = nextMinorVersionNumber;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public long getNextMinorVersionNumber() {
        return nextMinorVersionNumber;
    }

    public long getNextVersionNumber() {
        return nextVersionNumber;
    }
    
    public boolean isMinorRelease() {
        return getNextMinorVersionNumber()!=0;
    }
    
}
