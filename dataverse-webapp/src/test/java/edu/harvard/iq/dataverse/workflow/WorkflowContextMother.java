package edu.harvard.iq.dataverse.workflow;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.mocks.MockAuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.group.IpAddress;
import edu.harvard.iq.dataverse.persistence.user.ApiToken;
import edu.harvard.iq.dataverse.persistence.workflow.Workflow;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecution;

import static edu.harvard.iq.dataverse.persistence.workflow.WorkflowMother.givenWorkflowExecution;
import static edu.harvard.iq.dataverse.workflow.WorkflowContext.TriggerType.PostPublishDataset;
import static java.util.Collections.emptyMap;

public class WorkflowContextMother {

    public static Dataset givenDataset() {
        Dataset dataset = new Dataset();
        dataset.setId(1L);
        return dataset;
    }

    public static DataverseRequest givenDataverseRequest() {
        return new DataverseRequest(new MockAuthenticatedUser(), IpAddress.valueOf("127.0.0.1"));
    }

    public static WorkflowContext givenWorkflowContext() {
        return new WorkflowContext(PostPublishDataset, givenDataset(), 1L, 0L, givenDataverseRequest(), false);
    }

    public static WorkflowExecutionContext givenWorkflowExecutionContext(long datasetId, Workflow workflow) {
        return givenWorkflowExecutionContext(workflow, givenWorkflowExecution(datasetId, workflow.getId()));
    }

    public static WorkflowExecutionContext givenWorkflowExecutionContext(Workflow workflow, WorkflowExecution execution) {
        WorkflowContext workflowContext = givenWorkflowContext();
        return new WorkflowExecutionContext(workflow, workflowContext, execution, new ApiToken(), emptyMap());
    }
}
