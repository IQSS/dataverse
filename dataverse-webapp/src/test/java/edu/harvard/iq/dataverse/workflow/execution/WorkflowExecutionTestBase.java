package edu.harvard.iq.dataverse.workflow.execution;

import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.engine.TestSettingsServiceBean;
import edu.harvard.iq.dataverse.mocks.MockAuthenticatedUser;
import edu.harvard.iq.dataverse.mocks.MockAuthenticationServiceBean;
import edu.harvard.iq.dataverse.mocks.MockRoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.persistence.StubJpaPersistence;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.user.ApiToken;
import edu.harvard.iq.dataverse.persistence.workflow.Workflow;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecution;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionRepository;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionStepRepository;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowRepository;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.test.WithTestClock;

import static edu.harvard.iq.dataverse.persistence.workflow.WorkflowMother.givenWorkflowExecution;
import static edu.harvard.iq.dataverse.workflow.execution.WorkflowContextMother.givenWorkflowContext;
import static java.util.Collections.emptyMap;

public abstract class WorkflowExecutionTestBase implements WithTestClock {

    protected SettingsServiceBean settings = new TestSettingsServiceBean();
    protected StubJpaPersistence persistence = new StubJpaPersistence();
    protected DatasetRepository datasets = persistence.stub(DatasetRepository.class);
    protected WorkflowRepository workflows = persistence.stub(WorkflowRepository.class);
    protected WorkflowExecutionRepository executions = persistence.stub(WorkflowExecutionRepository.class);
    protected WorkflowExecutionStepRepository stepExecutions = persistence.stub(WorkflowExecutionStepRepository.class);
    protected RoleAssigneeServiceBean roleAssignees = new MockRoleAssigneeServiceBean() {{ add(new MockAuthenticatedUser()); }};
    protected AuthenticationServiceBean authentication = new MockAuthenticationServiceBean(clock);

    protected WorkflowExecutionContextFactory contextFactory = new WorkflowExecutionContextFactory(
            settings, datasets, workflows, executions, stepExecutions, roleAssignees, authentication, clock);

    protected WorkflowExecutionContext givenWorkflowExecutionContext(long datasetId, Workflow workflow) {
        return givenWorkflowExecutionContext(workflow, givenWorkflowExecution(datasetId, workflow.getId()));
    }

    protected WorkflowExecutionContext givenWorkflowExecutionContext(Workflow workflow, WorkflowExecution execution) {
        WorkflowContext context = givenWorkflowContext(execution.getDatasetId());
        return new WorkflowExecutionContext(workflow, context, execution, new ApiToken(), emptyMap(),
                executions, stepExecutions, clock);
    }
}
