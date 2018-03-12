package edu.harvard.iq.dataverse.workflow;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import java.util.UUID;

/**
 * The context in which a workflow is performed. Contains information steps might
 * need, such as the dataset being worked on and version data.
 * 
 * Design-wise, this class allows us to add parameters to {@link WorkflowStep} without 
 * changing its method signatures, which would break break client code.
 * 
 * @author michael
 */
public class WorkflowContext {
    
    public enum TriggerType {
        PrePublishDataset, PostPublishDataset
    }
    
    private final DataverseRequest request;
    private final Dataset dataset;
    private final long    nextVersionNumber;
    private final long    nextMinorVersionNumber;
    private final TriggerType    type;
    private final String  doiProvider;
    
    private String invocationId = UUID.randomUUID().toString();

    public WorkflowContext( DataverseRequest aRequest, Dataset aDataset, String doiProvider, TriggerType aTriggerType ) {
        this( aRequest, aDataset,
                aDataset.getLatestVersion().getVersionNumber(), 
                aDataset.getLatestVersion().getMinorVersionNumber(),
                aTriggerType, 
                doiProvider);
    }
    
    public WorkflowContext(DataverseRequest request, Dataset dataset, long nextVersionNumber, 
                            long nextMinorVersionNumber, TriggerType type, String doiProvider) {
        this.request = request;
        this.dataset = dataset;
        this.nextVersionNumber = nextVersionNumber;
        this.nextMinorVersionNumber = nextMinorVersionNumber;
        this.type = type;
        this.doiProvider = doiProvider;
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

    public void setInvocationId(String invocationId) {
        this.invocationId = invocationId;
    }

    public String getInvocationId() {
        return invocationId;
    }

    public String getDoiProvider() {
        return doiProvider;
    }

    public TriggerType getType() {
        return type;
    }
    
}
