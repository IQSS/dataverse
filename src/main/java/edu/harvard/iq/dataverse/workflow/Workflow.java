package edu.harvard.iq.dataverse.workflow;

import edu.harvard.iq.dataverse.workflow.step.WorkflowStepData;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

/**
 * A list of steps that can be executed with a given context. 
 * @author michael
 */
@NamedQueries({
    @NamedQuery(name="Workflow.listAll", query="Select w from Workflow w"),
    @NamedQuery(name="Workflow.deleteById", query="Delete from Workflow w WHERE w.id=:id")
})
@Entity
public class Workflow implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderColumn(name = "index")
    List<WorkflowStepData> steps;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<WorkflowStepData> getSteps() {
        return steps;
    }

    public void setSteps(List<WorkflowStepData> steps) {
        this.steps = steps;
        for ( WorkflowStepData s : steps ) {
            s.setParent(this);
        }
    }

    Map<String, String> getRequiredSettings() {
        Map<String, String> settings = new HashMap<String, String>();
        for(WorkflowStepData step: steps) {
            settings.putAll(step.getStepSettings());
        }
        return settings;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if ( !(obj instanceof Workflow) ) {
            return false;
        }
        final Workflow other = (Workflow) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return Objects.deepEquals(this.steps, other.steps);
    }
    
    
}
