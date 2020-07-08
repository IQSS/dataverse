package edu.harvard.iq.dataverse.persistence.workflow;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.io.Serializable;
import java.util.Map;

/**
 * A database row describing a step in a workflow. Actual steps can be instantiated
 * using the step provider.
 * <p>
 * This design is needed to support step classes from external SPIs.
 *
 * @author michael
 */
@Entity
public class WorkflowStepData implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Workflow parent;

    private String providerId;

    private String stepType;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(length = 2048)
    private Map<String, String> stepParameters;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(length = 2048)
    private Map<String, String> stepSettings;

    public Long getId() {
        return id;
    }

    public Workflow getParent() {
        return parent;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getStepType() {
        return stepType;
    }

    public Map<String, String> getStepParameters() {
        return stepParameters;
    }

    public Map<String, String> getStepSettings() {
        return stepSettings;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setParent(Workflow parent) {
        this.parent = parent;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public void setStepType(String stepType) {
        this.stepType = stepType;
    }

    public void setStepParameters(Map<String, String> stepParameters) {
        this.stepParameters = stepParameters;
    }

    public void setStepSettings(Map<String, String> stepSettings) {
        this.stepSettings = stepSettings;
    }

    @Override
    public String toString() {
        return "WorkflowStepData{" +
                "parent=" + parent +
                ", providerId=" + providerId +
                ", stepType=" + stepType +
                ", parameters=" + stepParameters +
                ", settings=" + stepSettings +
                '}';
    }
}
