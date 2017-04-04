package edu.harvard.iq.dataverse.workflow;

import edu.harvard.iq.dataverse.Dataset;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;

/**
 * A workflow whose current step waits for an external system to complete a
 * (probably lengthy) process. Meanwhile, it sits in the database, pending.
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
    
    String userId;
    String ipAddress;

    /** Empty constructor for JPA */
    public PendingWorkflowInvocation(){
        
    }
    
    public PendingWorkflowInvocation(Workflow wf, WorkflowContext ctxt) {
        invocationId = ctxt.getInvocationId();
        workflow = wf;
        dataset = ctxt.getDataset();
        nextVersionNumber = ctxt.getNextVersionNumber();
        nextMinorVersionNumber = ctxt.getNextMinorVersionNumber();
        userId = ctxt.getRequest().getUser().getIdentifier();
        ipAddress = ctxt.getRequest().getSourceAddress().toString();
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
    
    
    
}
