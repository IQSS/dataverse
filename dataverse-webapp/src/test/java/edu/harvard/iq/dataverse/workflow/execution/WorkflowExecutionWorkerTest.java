package edu.harvard.iq.dataverse.workflow.execution;

import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.engine.TestSettingsServiceBean;
import edu.harvard.iq.dataverse.mocks.MockAuthenticatedUser;
import edu.harvard.iq.dataverse.mocks.MockAuthenticationServiceBean;
import edu.harvard.iq.dataverse.mocks.MockRoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.persistence.StubJpaPersistence;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.workflow.Workflow;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecution;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionRepository;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionStep;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowRepository;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.test.WithTestClock;
import edu.harvard.iq.dataverse.workflow.WorkflowStepRegistry;
import edu.harvard.iq.dataverse.workflow.WorkflowStepSPI;
import edu.harvard.iq.dataverse.workflow.listener.WorkflowExecutionListener;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.enterprise.inject.Instance;
import javax.naming.NamingException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static edu.harvard.iq.dataverse.persistence.dataset.DatasetMother.givenDataset;
import static edu.harvard.iq.dataverse.persistence.workflow.WorkflowMother.givenWorkflow;
import static edu.harvard.iq.dataverse.persistence.workflow.WorkflowMother.givenWorkflowStep;
import static edu.harvard.iq.dataverse.workflow.execution.WorkflowContextMother.givenWorkflowExecutionContext;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

class WorkflowExecutionWorkerTest extends WorkflowJMSTestBase implements WorkflowStepSPI, WithTestClock {

    static final String TEST_PROVIDER_ID = "test";
    static final String SUCCESS_STEP_ID = "successful";
    static final String PAUSING_STEP_ID = "pausing";
    static final String FAILING_STEP_ID = "failing";

    SettingsServiceBean settings = new TestSettingsServiceBean();
    StubJpaPersistence persistence = new StubJpaPersistence();
    DatasetRepository datasets = persistence.stub(DatasetRepository.class);
    WorkflowRepository workflows = persistence.stub(WorkflowRepository.class);
    WorkflowExecutionRepository executions = persistence.stub(WorkflowExecutionRepository.class);
    WorkflowStepRegistry steps = new WorkflowStepRegistry() {{ init(); }};
    RoleAssigneeServiceBean roleAssignees = new MockRoleAssigneeServiceBean() {{ add(new MockAuthenticatedUser()); }};
    AuthenticationServiceBean authentication = new MockAuthenticationServiceBean(clock);
    Instance<WorkflowExecutionListener> executionListeners = mock(Instance.class);

    WorkflowExecutionContextFactory contextFactory = new WorkflowExecutionContextFactory(
        settings, datasets, workflows, executions, roleAssignees, authentication);

    WorkflowExecutionScheduler scheduler = new WorkflowExecutionScheduler() {{
        setQueue(queue); setFactory(factory); }};

    WorkflowExecutionStepRunner runner = new WorkflowExecutionStepRunner(steps, clock);

    WorkflowExecutionServiceBean service = new WorkflowExecutionServiceBean(
            datasets, executions, contextFactory, scheduler, clock);

    WorkflowExecutionWorker worker = new WorkflowExecutionWorker(
        datasets, executions, contextFactory, scheduler, runner, executionListeners, clock);

    WorkflowExecutionWorkerTest() throws NamingException { }

    @BeforeEach
    void setUp() {
        steps.register(TEST_PROVIDER_ID, this);

        doNothing().when(datasets)
                .lockDataset(any(Dataset.class), any(AuthenticatedUser.class), any(DatasetLock.Reason.class));
        doNothing().when(datasets)
                .unlockDataset(any(Dataset.class), any(DatasetLock.Reason.class));
    }

    @Test
    void shouldExecuteSimpleWorkflowSuccessfully() throws Exception {
        // given
        Dataset dataset = datasets.save(givenDataset());
        Workflow workflow = workflows.save(givenWorkflow(1L,
                givenWorkflowStep(TEST_PROVIDER_ID, SUCCESS_STEP_ID))
        );
        WorkflowContext context = givenWorkflowExecutionContext(dataset.getId(), workflow);
        // when
        givenMessageConsumer(worker)
                .callProducer(() -> service.start(workflow, context))
                .andAwaitMessages(2);
        // then
        List<WorkflowExecution> persistedExecutions = persistence.of(WorkflowExecution.class).findAll();
        assertThat(persistedExecutions).hasSize(1);
        // and
        WorkflowExecution execution = persistedExecutions.get(0);
        assertThat(execution.getWorkflowId()).isEqualTo(workflow.getId());
        assertThat(execution.getDatasetId()).isEqualTo(dataset.getId());
        assertThat(execution.getMajorVersionNumber()).isEqualTo(Long.valueOf(dataset.getVersionNumber()));
        assertThat(execution.getMinorVersionNumber()).isEqualTo(Long.valueOf(dataset.getMinorVersionNumber()));
        assertThat(execution.getDescription()).isEqualTo("test workflow");
        assertThat(execution.getStartedAt()).isEqualTo(clock.instant());
        assertThat(execution.getUserId()).isEqualTo("@null");
        assertThat(execution.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(execution.getFinishedAt()).isEqualTo(clock.instant());
        assertThat(execution.getSteps()).hasSize(1);
        // and
        WorkflowExecutionStep executionStep = execution.getSteps().get(0);
        assertThat(executionStep.getIndex()).isEqualTo(0);
        assertThat(executionStep.getProviderId()).isEqualTo(TEST_PROVIDER_ID);
        assertThat(executionStep.getStepType()).isEqualTo(SUCCESS_STEP_ID);
        assertThat(executionStep.getDescription()).isEqualTo("test workflow #0");
        assertThat(executionStep.getStartedAt()).isEqualTo(clock.instant());
        assertThat(executionStep.getInputParams()).isEqualTo(singletonMap("param", "value"));
        assertThat(executionStep.getPausedAt()).isNull();
        assertThat(executionStep.getPausedData()).isEqualTo(emptyMap());
        assertThat(executionStep.getResumedAt()).isNull();
        assertThat(executionStep.getResumedData()).isNull();
        assertThat(executionStep.getFinishedAt()).isEqualTo(clock.instant());
        assertThat(executionStep.getFinishedSuccessfully()).isTrue();
        assertThat(executionStep.getOutputParams()).isEqualTo(singletonMap("param", "value"));
        assertThat(executionStep.getRolledBackAt()).isNull();
    }

    @Test
    void shouldExecutePausingWorkflow() throws Exception {
        // given
        Dataset dataset = datasets.save(givenDataset());
        Workflow workflow = workflows.save(givenWorkflow(1L,
                givenWorkflowStep(TEST_PROVIDER_ID, PAUSING_STEP_ID))
        );
        WorkflowContext context = givenWorkflowExecutionContext(dataset.getId(), workflow);
        // when
        givenMessageConsumer(worker)
                .callProducer(() -> service.start(workflow, context))
                .andAwaitMessages(1);
        // then
        List<WorkflowExecution> persistedExecutions = persistence.of(WorkflowExecution.class).findAll();
        assertThat(persistedExecutions).hasSize(1);
        // and
        WorkflowExecution execution = persistedExecutions.get(0);
        assertThat(execution.getWorkflowId()).isEqualTo(workflow.getId());
        assertThat(execution.getDatasetId()).isEqualTo(dataset.getId());
        assertThat(execution.getMajorVersionNumber()).isEqualTo(Long.valueOf(dataset.getVersionNumber()));
        assertThat(execution.getMinorVersionNumber()).isEqualTo(Long.valueOf(dataset.getMinorVersionNumber()));
        assertThat(execution.getDescription()).isEqualTo("test workflow");
        assertThat(execution.getStartedAt()).isEqualTo(clock.instant());
        assertThat(execution.getUserId()).isEqualTo("@null");
        assertThat(execution.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(execution.getFinishedAt()).isNull();
        assertThat(execution.getSteps()).hasSize(1);
        // and
        WorkflowExecutionStep executionStep = execution.getSteps().get(0);
        assertThat(executionStep.getIndex()).isEqualTo(0);
        assertThat(executionStep.getProviderId()).isEqualTo(TEST_PROVIDER_ID);
        assertThat(executionStep.getStepType()).isEqualTo(PAUSING_STEP_ID);
        assertThat(executionStep.getDescription()).isEqualTo("test workflow #0");
        assertThat(executionStep.getStartedAt()).isEqualTo(clock.instant());
        assertThat(executionStep.getInputParams()).isEqualTo(singletonMap("param", "value"));
        assertThat(executionStep.getPausedAt()).isEqualTo(clock.instant());
        assertThat(executionStep.getPausedData()).isEqualTo(singletonMap("param", "value"));
        assertThat(executionStep.getResumedAt()).isNull();
        assertThat(executionStep.getResumedData()).isNull();
        assertThat(executionStep.getFinishedAt()).isNull();
        assertThat(executionStep.getFinishedSuccessfully()).isNull();;
        assertThat(executionStep.getOutputParams()).isEqualTo(emptyMap());
        assertThat(executionStep.getRolledBackAt()).isNull();
    }

    @Test
    void shouldExecutePausingAndResumingWorkflow() throws Exception {
        // given
        Dataset dataset = datasets.save(givenDataset());
        Workflow workflow = workflows.save(givenWorkflow(1L,
                givenWorkflowStep(TEST_PROVIDER_ID, PAUSING_STEP_ID))
        );
        WorkflowContext context = givenWorkflowExecutionContext(dataset.getId(), workflow);
        doAnswer(invocation -> Optional.of(persistence.of(WorkflowExecution.class).findAll().get(0)))
                .when(executions).findByInvocationId("invocationId");
        // when
        givenMessageConsumer(worker)
                .callProducer(() -> service.start(workflow, context))
                .andAwaitMessages(1);
        // and
        givenMessageConsumer(worker)
                .callProducer(() -> service.resume("invocationId", "test"))
                .andAwaitMessages(2);
        // then
        List<WorkflowExecution> executions = persistence.of(WorkflowExecution.class).findAll();
        assertThat(executions).hasSize(1);
        // and
        WorkflowExecution execution = executions.get(0);
        assertThat(execution.getWorkflowId()).isEqualTo(workflow.getId());
        assertThat(execution.getDatasetId()).isEqualTo(dataset.getId());
        assertThat(execution.getMajorVersionNumber()).isEqualTo(Long.valueOf(dataset.getVersionNumber()));
        assertThat(execution.getMinorVersionNumber()).isEqualTo(Long.valueOf(dataset.getMinorVersionNumber()));
        assertThat(execution.getDescription()).isEqualTo("test workflow");
        assertThat(execution.getStartedAt()).isEqualTo(clock.instant());
        assertThat(execution.getUserId()).isEqualTo("@null");
        assertThat(execution.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(execution.getFinishedAt()).isEqualTo(clock.instant());
        assertThat(execution.getSteps()).hasSize(1);
        // and
        WorkflowExecutionStep executionStep = execution.getSteps().get(0);
        assertThat(executionStep.getIndex()).isEqualTo(0);
        assertThat(executionStep.getProviderId()).isEqualTo(TEST_PROVIDER_ID);
        assertThat(executionStep.getStepType()).isEqualTo(PAUSING_STEP_ID);
        assertThat(executionStep.getDescription()).isEqualTo("test workflow #0");
        assertThat(executionStep.getStartedAt()).isEqualTo(clock.instant());
        assertThat(executionStep.getInputParams()).isEqualTo(singletonMap("param", "value"));
        assertThat(executionStep.getPausedAt()).isEqualTo(clock.instant());
        assertThat(executionStep.getPausedData()).isEqualTo(singletonMap("param", "value"));
        assertThat(executionStep.getResumedAt()).isEqualTo(clock.instant());
        assertThat(executionStep.getResumedData()).isEqualTo("test");
        assertThat(executionStep.getFinishedAt()).isEqualTo(clock.instant());
        assertThat(executionStep.getFinishedSuccessfully()).isTrue();
        assertThat(executionStep.getOutputParams()).isEqualTo(singletonMap("param", "value"));
        assertThat(executionStep.getRolledBackAt()).isNull();
    }

    @Test
    void shouldExecuteAbdRollbackFailingWorkflow() throws Exception {
        // given
        Dataset dataset = datasets.save(givenDataset());
        Workflow workflow = workflows.save(givenWorkflow(1L,
                givenWorkflowStep(TEST_PROVIDER_ID, FAILING_STEP_ID))
        );
        WorkflowContext context = givenWorkflowExecutionContext(dataset.getId(), workflow);
        // when
        givenMessageConsumer(worker)
                .callProducer(() -> service.start(workflow, context))
                .andAwaitMessages(3);
        // then
        List<WorkflowExecution> executions = persistence.of(WorkflowExecution.class).findAll();
        assertThat(executions).hasSize(1);
        // and
        WorkflowExecution execution = executions.get(0);
        assertThat(execution.getWorkflowId()).isEqualTo(workflow.getId());
        assertThat(execution.getDatasetId()).isEqualTo(dataset.getId());
        assertThat(execution.getMajorVersionNumber()).isEqualTo(Long.valueOf(dataset.getVersionNumber()));
        assertThat(execution.getMinorVersionNumber()).isEqualTo(Long.valueOf(dataset.getMinorVersionNumber()));
        assertThat(execution.getDescription()).isEqualTo("test workflow");
        assertThat(execution.getStartedAt()).isEqualTo(clock.instant());
        assertThat(execution.getUserId()).isEqualTo("@null");
        assertThat(execution.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(execution.getFinishedAt()).isEqualTo(clock.instant());
        assertThat(execution.getSteps()).hasSize(1);
        // and
        WorkflowExecutionStep executionStep = execution.getSteps().get(0);
        assertThat(executionStep.getIndex()).isEqualTo(0);
        assertThat(executionStep.getProviderId()).isEqualTo(TEST_PROVIDER_ID);
        assertThat(executionStep.getStepType()).isEqualTo(FAILING_STEP_ID);
        assertThat(executionStep.getDescription()).isEqualTo("test workflow #0");
        assertThat(executionStep.getStartedAt()).isEqualTo(clock.instant());
        assertThat(executionStep.getInputParams()).isEqualTo(singletonMap("param", "value"));
        assertThat(executionStep.getPausedAt()).isNull();
        assertThat(executionStep.getPausedData()).isEqualTo(emptyMap());
        assertThat(executionStep.getResumedAt()).isNull();
        assertThat(executionStep.getResumedData()).isNull();
        assertThat(executionStep.getFinishedAt()).isEqualTo(clock.instant());
        assertThat(executionStep.getFinishedSuccessfully()).isFalse();
        assertThat(executionStep.getOutputParams()).isEqualTo(new Failure("error", "test").getData());
        assertThat(executionStep.getRolledBackAt()).isEqualTo(clock.instant());
    }

    @Override
    public WorkflowStep getStep(String stepType, WorkflowStepParams stepParameters) {
        TestWorkflowStep step = new TestWorkflowStep(stepParameters);
        if (PAUSING_STEP_ID.equals(stepType)) {
            step.pausingAndResumingSuccessfully();
        } else if (FAILING_STEP_ID.equals(stepType)) {
            step.withRunningLogic((ctx, params) -> new Failure("error", "test"));
        }
        return step;
    }
}
