package edu.harvard.iq.dataverse.workflow;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;

import java.util.Map;
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
    private final ApiToken apiToken;
    private final boolean datasetExternallyReleased;
    private Map<String, Object> settings;
    private Long lockId = null;
    
    private String invocationId = UUID.randomUUID().toString();

    public WorkflowContext( DataverseRequest aRequest, Dataset aDataset, TriggerType aTriggerType, boolean datasetExternallyReleased ) {
        this( aRequest, aDataset,
                aDataset.getLatestVersion().getVersionNumber(), 
                aDataset.getLatestVersion().getMinorVersionNumber(),
                aTriggerType, null, null, datasetExternallyReleased);
    }
    public WorkflowContext(DataverseRequest request, Dataset dataset, long nextVersionNumber, 
            long nextMinorVersionNumber, TriggerType type, Map<String, Object> settings, ApiToken apiToken, boolean datasetExternallyReleased) {
        this(request, dataset, nextVersionNumber,nextMinorVersionNumber, type, settings, apiToken, datasetExternallyReleased, null, null);
    }

    public WorkflowContext(DataverseRequest request, Dataset dataset, long nextVersionNumber, 
                            long nextMinorVersionNumber, TriggerType type, Map<String, Object> settings, ApiToken apiToken, boolean datasetExternallyReleased, String invocationId, Long lockId) {
        this.request = request;
        this.dataset = dataset;
        this.nextVersionNumber = nextVersionNumber;
        this.nextMinorVersionNumber = nextMinorVersionNumber;
        this.type = type;
        this.settings = settings;
        this.apiToken = apiToken;
        this.datasetExternallyReleased = datasetExternallyReleased;
        //If null, we'll keep the randomly generated one
        if(invocationId!=null) {
            setInvocationId(invocationId);
        }
        if(lockId != null) {
          this.setLockId(lockId);
        }
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

    public TriggerType getType() {
        return type;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    public ApiToken getApiToken() {
        return apiToken;
    }

    public boolean getDatasetExternallyReleased() {
       return datasetExternallyReleased;
    }
    public Long getLockId() {
        return lockId;
    }
    public void setLockId(Long lockId) {
        this.lockId = lockId;
    }
    
}
