package edu.harvard.iq.dataverse.workflow.step;

import edu.harvard.iq.dataverse.workflow.Workflow;
import java.io.Serializable;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * A database row describing a step in a workflow. Actual steps can be instantiated
 * using the step provider.
 * 
 * This design is needed to support step classes from external SPIs.
 * 
 * @author michael
 */
@Entity
public class WorkflowStepData implements Serializable {
    
    @Id
    @GeneratedValue( strategy=GenerationType.IDENTITY )
    long id;
    
    @ManyToOne
    private Workflow parent;
    
    private String providerId;
    
    private String stepType;
    
    @ElementCollection( fetch=FetchType.EAGER )
    @Column(length = 2048)
    private Map<String,String> stepParameters;

    public Workflow getParent() {
        return parent;
    }

    public void setParent(Workflow parent) {
        this.parent = parent;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getStepType() {
        return stepType;
    }

    public void setStepType(String stepType) {
        this.stepType = stepType;
    }

    public Map<String, String> getStepParameters() {
        return stepParameters;
    }

    public void setStepParameters(Map<String, String> stepParameters) {
        this.stepParameters = stepParameters;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "WorkflowStepData{" + "parent=" + parent + ", providerId=" + providerId + ", stepType=" + stepType + ", parameters=" + stepParameters + '}';
    }
    
    
}
