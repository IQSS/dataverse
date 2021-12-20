package edu.harvard.iq.dataverse.workflow.execution;

import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.engine.TestSettingsServiceBean;
import edu.harvard.iq.dataverse.mocks.MockAuthenticatedUser;
import edu.harvard.iq.dataverse.mocks.MockAuthenticationServiceBean;
import edu.harvard.iq.dataverse.mocks.MockRoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.persistence.StubJpaPersistence;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLockRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionIdentifier;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionRepository;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionRepository;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionStepRepository;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowRepository;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.test.WithTestClock;
import org.junit.jupiter.api.BeforeEach;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;

public abstract class WorkflowExecutionTestBase implements WithTestClock {

    protected SettingsServiceBean settings = new TestSettingsServiceBean();
    protected StubJpaPersistence persistence = new StubJpaPersistence();
    protected DatasetRepository datasets = persistence.stub(DatasetRepository.class);
    protected DatasetLockRepository locks = persistence.stub(DatasetLockRepository.class);
    protected DatasetVersionRepository datasetVersions = persistence.stub(DatasetVersionRepository.class);
    protected WorkflowRepository workflows = persistence.stub(WorkflowRepository.class);
    protected WorkflowExecutionRepository executions = persistence.stub(WorkflowExecutionRepository.class);
    protected WorkflowExecutionStepRepository stepExecutions = persistence.stub(WorkflowExecutionStepRepository.class);
    protected RoleAssigneeServiceBean roleAssignees = new MockRoleAssigneeServiceBean() {{ add(new MockAuthenticatedUser()); }};

    protected WorkflowExecutionContextFactory contextFactory = new WorkflowExecutionContextFactory(
            settings, datasetVersions, workflows, executions, roleAssignees, clock);

    protected DatasetVersionServiceBean versionsService = new DatasetVersionServiceBean(datasetVersions);

    @BeforeEach
    public void setUp() throws Exception {
        doAnswer(invocation -> persistence.of(DatasetLock.class)
                .findAll(lock -> lock.getDataset().getId() == invocation.getArgument(0)))
                .when(locks).findByDatasetId(anyLong());

        doAnswer(invocation -> persistence.of(DatasetVersion.class)
                .findOne(version -> version.isIdentifiedBy(invocation.getArgument(0))))
                .when(datasetVersions).findByDatasetIdAndVersionNumber(any(DatasetVersionIdentifier.class));
    }
}
