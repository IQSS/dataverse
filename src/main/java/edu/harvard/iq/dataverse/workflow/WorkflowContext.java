package edu.harvard.iq.dataverse.workflow;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import java.util.UUID;

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
    
    private final DataverseRequest request;
    private final Dataset dataset;
    private final long nextVersionNumber;
    private final long nextMinorVersionNumber;
    private final String invocationId = UUID.randomUUID().toString();

    public WorkflowContext(DataverseRequest aRequest, Dataset aDataset, long aNextVersionNumber, long aNextMinorVersionNumber) {
        request = aRequest;
        dataset = aDataset;
        nextVersionNumber = aNextVersionNumber;
        nextMinorVersionNumber = aNextMinorVersionNumber;       
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

    public DataverseRequest getRequest() {
        return request;
    }
    
    public boolean isMinorRelease() {
        return getNextMinorVersionNumber()!=0;
    }

    public String getInvocationId() {
        return invocationId;
    }
    
}
