package edu.harvard.iq.dataverse.workflow;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.mocks.MockAuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.group.IpAddress;

import static edu.harvard.iq.dataverse.workflow.WorkflowContext.TriggerType.PostPublishDataset;

public class WorkflowContextObjectMother {

    public static DataverseRequest givenDataverseRequest() {
        return new DataverseRequest(new MockAuthenticatedUser(), IpAddress.valueOf("127.0.0.1"));
    }

    public static Dataset givenDataset() {
        Dataset dataset = new Dataset();
        dataset.setId(0L);
        return dataset;
    }

    public static WorkflowContext givenWorkflowContext() {
        return new WorkflowContext(givenDataverseRequest(), givenDataset(), PostPublishDataset, false);
    }
}
