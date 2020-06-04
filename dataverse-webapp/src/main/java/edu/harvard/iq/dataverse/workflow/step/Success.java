package edu.harvard.iq.dataverse.workflow.step;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static java.util.Collections.emptyMap;

/**
 * Result returned when a {@link WorkflowStep} is finished successfully.
 *
 * @author kaczynskid
 */
public class Success implements WorkflowStepResult {

    private final Map<String, String> data;

    public Success() {
        this(emptyMap());
    }

    public Success(Map<String, String> data) {
        this.data = data;
    }

    public static WorkflowStepResult.Source successWith(Consumer<Map<String, String>> moreData) {
        return data -> {
            moreData.accept(data);
            return new Success(data);
        };
    }

    @Override
    public Map<String, String> getData() {
        return data;
    }

    @Override
    public int hashCode() {
        return data != null ? data.hashCode() : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Success success = (Success) o;

        return Objects.equals(data, success.data);
    }

    @Override
    public String toString() {
        return "WorkflowStepResult.Success{" + "data=" + data + '}';
    }
}
