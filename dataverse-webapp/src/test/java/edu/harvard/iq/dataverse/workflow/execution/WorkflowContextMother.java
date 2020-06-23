package edu.harvard.iq.dataverse.workflow.execution;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.mocks.MockAuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.group.IpAddress;
import edu.harvard.iq.dataverse.persistence.user.ApiToken;
import edu.harvard.iq.dataverse.persistence.workflow.Workflow;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecution;
import edu.harvard.iq.dataverse.workflow.execution.WorkflowContext;
import edu.harvard.iq.dataverse.workflow.execution.WorkflowExecutionContext;

import static edu.harvard.iq.dataverse.persistence.dataset.DatasetMother.givenDataset;
import static edu.harvard.iq.dataverse.persistence.workflow.WorkflowMother.givenWorkflowExecution;
import static edu.harvard.iq.dataverse.workflow.execution.WorkflowContext.TriggerType.PostPublishDataset;
import static java.util.Collections.emptyMap;

public class WorkflowContextMother {

    public static DataverseRequest givenDataverseRequest() {
        return new DataverseRequest(new MockAuthenticatedUser(), IpAddress.valueOf("127.0.0.1"));
    }

    public static WorkflowContext givenWorkflowContext(long datasetId) {
        return givenWorkflowContext(givenDataset(datasetId));
    }

    public static WorkflowContext givenWorkflowContext(Dataset dataset) {
        return new WorkflowContext(PostPublishDataset, dataset, 1L, 0L, givenDataverseRequest(), false);
    }

    public static WorkflowExecutionContext givenWorkflowExecutionContext(long datasetId, Workflow workflow) {
        return givenWorkflowExecutionContext(workflow, givenWorkflowExecution(datasetId, workflow.getId()));
    }

    public static WorkflowExecutionContext givenWorkflowExecutionContext(Dataset dataset, Workflow workflow) {
        WorkflowContext context = givenWorkflowContext(dataset);
        WorkflowExecution execution = givenWorkflowExecution(dataset.getId(), workflow.getId());
        return new WorkflowExecutionContext(workflow, context, execution, new ApiToken(), emptyMap());
    }

    public static WorkflowExecutionContext givenWorkflowExecutionContext(Workflow workflow, WorkflowExecution execution) {
        WorkflowContext context = givenWorkflowContext(execution.getDatasetId());
        return new WorkflowExecutionContext(workflow, context, execution, new ApiToken(), emptyMap());
    }
}
