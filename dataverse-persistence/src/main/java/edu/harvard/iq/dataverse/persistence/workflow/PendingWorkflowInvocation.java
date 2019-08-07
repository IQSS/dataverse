package edu.harvard.iq.dataverse.persistence.workflow;

import edu.harvard.iq.dataverse.persistence.dataset.Dataset;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A workflow whose current step waits for an external system to complete a
 * (probably lengthy) process. Meanwhile, it sits in the database, pending away.
 *
 * @author michael
 */
@NamedQueries({
        @NamedQuery(name = "PendingWorkflowInvocation.listAll", query = "SELECT pw FROM PendingWorkflowInvocation pw")
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

    @ElementCollection(fetch = FetchType.EAGER)
    private Map<String, String> localData;

    int pendingStepIdx;

    String userId;
    String ipAddress;
    int typeOrdinal;
    boolean datasetExternallyReleased;

    /**
     * Empty constructor for JPA
     */
    public PendingWorkflowInvocation() {

    }

    public PendingWorkflowInvocation(Workflow wf, Map<String, String> localData) {
        this.workflow = wf;
        this.localData = localData;
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

    public boolean isDatasetExternallyReleased() {
        return datasetExternallyReleased;
    }

    public void setDatasetExternallyReleased(boolean datasetExternallyReleased) {
        this.datasetExternallyReleased = datasetExternallyReleased;
    }
}
