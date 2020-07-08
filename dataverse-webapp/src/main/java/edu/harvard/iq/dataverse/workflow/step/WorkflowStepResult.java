package edu.harvard.iq.dataverse.workflow.step;

import edu.harvard.iq.dataverse.persistence.workflow.WorkflowArtifactSource;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Collections.emptyList;

/**
 * The result of performing a {@link WorkflowStep}.
 *
 * @author michael
 */
public interface WorkflowStepResult extends Serializable {

    /**
     * Arbitrary data accompanying this result.
     * @return result data.
     */
    Map<String, String> getData();

    /**
     * Optional artifacts to store as step result.
     * @return list of artifact sources to be stored.
     */
    default List<WorkflowArtifactSource> getArtifacts() {
        return emptyList();
    }

    /**
     * The source of {@link WorkflowStepResult}.
     * Gives the ability for step wrappers or base classes to provide default result data.
     *
     * @author kaczynskid
     */
    interface Source extends Function<Map<String, String>, WorkflowStepResult> {
    }
}
