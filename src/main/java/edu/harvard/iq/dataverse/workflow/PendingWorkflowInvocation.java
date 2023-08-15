package edu.harvard.iq.dataverse.workflow;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.workflow.step.Pending;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;

/**
 * A workflow whose current step waits for an external system to complete a
 * (probably lengthy) process. Meanwhile, it sits in the database, pending away.
 * 
 * @author michael
 */
@NamedQueries({
    @NamedQuery( name="PendingWorkflowInvocation.listAll", query="SELECT pw FROM PendingWorkflowInvocation pw")
})
@Entity
public class PendingWorkflowInvocation implements Serializable {
    
    @Id
    String invocationId;
    
    @ManyToOne
    Workflow workflow;
    
    @OneToOne
    Dataset dataset;
    
    long nextVersionNumber;
    long nextMinorVersionNumber;
    
    @ElementCollection( fetch=FetchType.EAGER )
    private Map<String,String> localData;
    
    int pendingStepIdx;
    
    String userId;
    String ipAddress;
    int typeOrdinal;
    boolean datasetExternallyReleased;
    
    long lockid;

    public static final String AUTHORIZED= "authorized";
                
    /** Empty constructor for JPA */
    public PendingWorkflowInvocation(){
        
    }
    
    public PendingWorkflowInvocation(Workflow wf, WorkflowContext ctxt, Pending result) {
        invocationId = ctxt.getInvocationId();
        workflow = wf;
        dataset = ctxt.getDataset();
        nextVersionNumber = ctxt.getNextVersionNumber();
        nextMinorVersionNumber = ctxt.getNextMinorVersionNumber();
        userId = ctxt.getRequest().getUser().getIdentifier();
        ipAddress = ctxt.getRequest().getSourceAddress().toString();
        localData = new HashMap<>(result.getData());
        typeOrdinal = ctxt.getType().ordinal();
        datasetExternallyReleased=ctxt.getDatasetExternallyReleased();
        lockid=ctxt.getLockId();
    }
    
    public WorkflowContext reCreateContext(RoleAssigneeServiceBean roleAssignees) {
        DataverseRequest aRequest = new DataverseRequest((User)roleAssignees.getRoleAssignee(userId), IpAddress.valueOf(ipAddress));
        final WorkflowContext workflowContext = new WorkflowContext(aRequest, dataset, nextVersionNumber, 
                nextMinorVersionNumber, WorkflowContext.TriggerType.values()[typeOrdinal], null, null, datasetExternallyReleased, invocationId, lockid);
        return workflowContext;
    }
    
    public String getInvocationId() {
        return invocationId;
    }

    public void setInvocationId(String invocationId) {
        this.invocationId = invocationId;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public long getNextVersionNumber() {
        return nextVersionNumber;
    }

    public void setNextVersionNumber(long nextVersionNumber) {
        this.nextVersionNumber = nextVersionNumber;
    }

    public long getNextMinorVersionNumber() {
        return nextMinorVersionNumber;
    }

    public void setNextMinorVersionNumber(long nextMinorVersionNumber) {
        this.nextMinorVersionNumber = nextMinorVersionNumber;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Map<String, String> getLocalData() {
        return localData;
    }

    public void setLocalData(Map<String, String> localData) {
        this.localData = localData;
    }

    public int getPendingStepIdx() {
        return pendingStepIdx;
    }

    public void setPendingStepIdx(int pendingStepIdx) {
        this.pendingStepIdx = pendingStepIdx;
    }

    public int getTypeOrdinal() {
        return typeOrdinal;
    }

    public void setTypeOrdinal(int typeOrdinal) {
        this.typeOrdinal = typeOrdinal;
    }
    
    public long getLockId() {
        return lockid;
    }
    public void setLockId(long lockId) {
        this.lockid = lockId;
    }
}
