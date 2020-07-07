package edu.harvard.iq.dataverse.workflow.step;

import edu.harvard.iq.dataverse.persistence.workflow.WorkflowArtifactSource;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

/**
 * Result returned when a {@link WorkflowStep} is finished successfully.
 *
 * @author kaczynskid
 */
public class Success implements WorkflowStepResult {

    private final Map<String, String> data;
    private transient final List<WorkflowArtifactSource> artifacts;

    public Success() {
        this(emptyMap());
    }

    public Success(Map<String, String> data) {
        this(data, emptyList());
    }

    public Success(List<WorkflowArtifactSource> artifacts) {
        this(emptyMap(), artifacts);
    }

    public Success(Map<String, String> data, List<WorkflowArtifactSource> artifacts) {
        this.data = data;
        this.artifacts = artifacts;
    }

    public static WorkflowStepResult.Source successWith(WorkflowArtifactSource...artifacts) {
        return successWith(data -> {}, artifacts);
    }

    public static WorkflowStepResult.Source successWith(List<WorkflowArtifactSource> artifacts) {
        return successWith(data -> {}, artifacts);
    }

    public static WorkflowStepResult.Source successWith(
            Consumer<Map<String, String>> moreData, WorkflowArtifactSource...artifacts) {
        return successWith(moreData, asList(artifacts));
    }
    public static WorkflowStepResult.Source successWith(
            Consumer<Map<String, String>> moreData, List<WorkflowArtifactSource> artifacts) {
        return data -> {
            moreData.accept(data);
            return new Success(data, artifacts);
        };
    }

    @Override
    public Map<String, String> getData() {
        return data;
    }

    @Override
    public List<WorkflowArtifactSource> getArtifacts() {
        return artifacts;
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
