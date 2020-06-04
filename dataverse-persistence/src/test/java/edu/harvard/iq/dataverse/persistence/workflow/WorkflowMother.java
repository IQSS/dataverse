package edu.harvard.iq.dataverse.persistence.workflow;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Optional.ofNullable;

public final class WorkflowMother {

    private WorkflowMother() {}

    public static Workflow givenWorkflow(WorkflowStepData...steps) {
        return givenWorkflow(null, steps);
    }

    public static Workflow givenWorkflow(Long id, WorkflowStepData...steps) {
        Workflow workflow = new Workflow();
        workflow.setId(id);
        workflow.setName("test workflow");
        workflow.setSteps(ofNullable(steps).map(Arrays::asList).orElseGet(Collections::emptyList));
        return workflow;
    }

    public static WorkflowStepData givenWorkflowStep(String stepType) {
        return givenWorkflowStep("internal", stepType, singletonMap("param", "value"), emptyMap());
    }

    public static WorkflowStepData givenWorkflowStep(String providerId, String stepType,
                                                     Map<String, String> parameters, Map<String, String> settings) {
        WorkflowStepData step = new WorkflowStepData();
        step.setProviderId(providerId);
        step.setStepType(stepType);
        step.setStepParameters(parameters);
        step.setStepSettings(settings);
        return step;
    }

    public static WorkflowExecution givenWorkflowExecution(long datasetId, long workflowId) {
        return givenWorkflowExecution(workflowId, datasetId, 1L, 0L);
    }

    public static WorkflowExecution givenWorkflowExecution(long workflowId, long datasetId, long majorVersionNumber, long minorVersionNumber) {
        return new WorkflowExecution(workflowId, "PostPublishDataset", datasetId, majorVersionNumber,
                minorVersionNumber, false, "test workflow execution");
    }
}
