package edu.harvard.iq.dataverse.workflow.step;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Function;

/**
 * The result of performing a {@link WorkflowStep}.
 *
 * @author michael
 */
public interface WorkflowStepResult extends Serializable {

    /**
     * Arbitrary accompanying this result.
     * @return result data.
     */
    Map<String, String> getData();

    /**
     * The source of {@link WorkflowStepResult}.
     * Gives the ability for step wrappers or base classes to provide default result data.
     *
     * @author kaczynskid
     */
    interface Source extends Function<Map<String, String>, WorkflowStepResult> {
    }
}
