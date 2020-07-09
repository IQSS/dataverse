package edu.harvard.iq.dataverse.workflow.execution;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.mocks.MockAuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.group.IpAddress;
import edu.harvard.iq.dataverse.test.WithTestClock;

import static edu.harvard.iq.dataverse.persistence.dataset.DatasetMother.givenDataset;
import static edu.harvard.iq.dataverse.workflow.execution.WorkflowContext.TriggerType.PostPublishDataset;

public class WorkflowContextMother implements WithTestClock {

    public static DataverseRequest givenDataverseRequest() {
        return new DataverseRequest(new MockAuthenticatedUser(), IpAddress.valueOf("127.0.0.1"));
    }

    public static WorkflowContext givenWorkflowContext(long datasetId) {
        return givenWorkflowContext(givenDataset(datasetId));
    }

    public static WorkflowContext givenWorkflowContext(Dataset dataset) {
        return new WorkflowContext(PostPublishDataset, dataset, 1L, 0L, givenDataverseRequest(), false);
    }
}
