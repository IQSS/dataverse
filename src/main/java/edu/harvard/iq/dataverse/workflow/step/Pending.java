package edu.harvard.iq.dataverse.workflow.step;

import java.util.HashMap;
import java.util.Map;

/**
 * Result returned when a {@link WorkflowStep} is waiting on 
 * 
 * @author michael
 */
public class Pending implements WorkflowStepResult {
    
    private final Map<String,String> data = new HashMap<>();

    public Pending(Map<String,String> someData) {
        data.putAll(someData);
    }
    public Pending() {}
    
    public Map<String, String> getData() {
        return data;
    }

    @Override
    public String toString() {
        return "WorkflowStepResult.Pending{" + "data=" + data + '}';
    }
    
    
}
