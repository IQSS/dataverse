package edu.harvard.iq.dataverse.workflow.step;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;

/**
 * Result returned when a {@link WorkflowStep} is waiting on
 *
 * @author michael
 */
public class Pending implements WorkflowStepResult {

    private final Map<String, String> data;

    public Pending() {
        this(emptyMap());
    }

    public Pending(Map<String, String> someData) {
        data = new HashMap<>(someData);
    }

    @Override
    public Map<String, String> getData() {
        return data;
    }

    @Override
    public String toString() {
        return "WorkflowStepResult.Pending{" + "data=" + data + '}';
    }
}
