package edu.harvard.iq.dataverse.workflow.execution;

import edu.harvard.iq.dataverse.dataset.DatasetLockServiceBean;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.workflow.Workflow;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowArtifactRepository;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecution;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionStep;
import edu.harvard.iq.dataverse.workflow.WorkflowStepRegistry;
import edu.harvard.iq.dataverse.workflow.WorkflowStepSPI;
import edu.harvard.iq.dataverse.workflow.artifacts.MemoryWorkflowArtifactStorage;
import edu.harvard.iq.dataverse.workflow.artifacts.WorkflowArtifactServiceBean;
import edu.harvard.iq.dataverse.workflow.listener.WorkflowExecutionListener;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.enterprise.inject.Instance;
import javax.naming.NamingException;
import java.util.List;
import java.util.Optional;

import static edu.harvard.iq.dataverse.persistence.dataset.DatasetMother.givenDataset;
import static edu.harvard.iq.dataverse.persistence.workflow.WorkflowMother.givenWorkflow;
import static edu.harvard.iq.dataverse.persistence.workflow.WorkflowMother.givenWorkflowStep;
import static edu.harvard.iq.dataverse.workflow.execution.WorkflowContextMother.givenWorkflowExecutionContext;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class WorkflowExecutionWorkerTest extends WorkflowExecutionJMSTestBase implements WorkflowStepSPI {

    static final String TEST_PROVIDER_ID = "test";
    static final String SUCCESS_STEP_ID = "successful";
    static final String PAUSING_STEP_ID = "pausing";
    static final String FAILING_STEP_ID = "failing";

    WorkflowArtifactRepository artifacts = persistence.stub(WorkflowArtifactRepository.class);
    WorkflowStepRegistry steps = new WorkflowStepRegistry();
    Instance<WorkflowExecutionListener> executionListeners = mock(Instance.class);

    WorkflowExecutionScheduler scheduler = new WorkflowExecutionScheduler() {{
        setQueue(queue); setFactory(factory); }};

    WorkflowArtifactServiceBean artifactsService = new WorkflowArtifactServiceBean(
            artifacts, new MemoryWorkflowArtifactStorage(), clock);
    DatasetLockServiceBean locksService = new DatasetLockServiceBean(datasets, locks);
    WorkflowExecutionService executionService = new WorkflowExecutionService(
            locksService, executions, stepExecutions, contextFactory, artifactsService, executionListeners);
    WorkflowExecutionFacade executionFacade = new WorkflowExecutionFacade(executionService, scheduler);
    WorkflowExecutionWorker worker = new WorkflowExecutionWorker(executionService, scheduler, steps);

    WorkflowExecutionWorkerTest() throws NamingException { }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        steps.register(TEST_PROVIDER_ID, this);
    }

    @Test
    void shouldExecuteSimpleWorkflowSuccessfully() throws Exception {
        // given
        Dataset dataset = datasets.save(givenDataset());
        datasetVersions.save(dataset.getLatestVersion());
        Workflow workflow = workflows.save(givenWorkflow(1L,
                givenWorkflowStep(TEST_PROVIDER_ID, SUCCESS_STEP_ID))
        );
        WorkflowContext context = givenWorkflowExecutionContext(dataset.getId(), workflow);
        // when
        givenMessageConsumer(worker)
                .callProducer(() -> executionFacade.start(workflow, context))
                .andAwaitMessages(2);
        // then
        List<WorkflowExecution> persistedExecutions = persistence.of(WorkflowExecution.class).findAll();
        assertThat(persistedExecutions).hasSize(1);
        // and
        WorkflowExecution execution = persistedExecutions.get(0);
        assertThat(execution.getWorkflowId()).isEqualTo(workflow.getId());
        assertThat(execution.getDatasetId()).isEqualTo(dataset.getId());
        assertThat(execution.getVersionNumber()).isEqualTo(Long.valueOf(dataset.getVersionNumber()));
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
        datasetVersions.save(dataset.getLatestVersion());
        Workflow workflow = workflows.save(givenWorkflow(1L,
                givenWorkflowStep(TEST_PROVIDER_ID, PAUSING_STEP_ID))
        );
        WorkflowContext context = givenWorkflowExecutionContext(dataset.getId(), workflow);
        // when
        givenMessageConsumer(worker)
                .callProducer(() -> executionFacade.start(workflow, context))
                .andAwaitMessages(1);
        // then
        List<WorkflowExecution> persistedExecutions = persistence.of(WorkflowExecution.class).findAll();
        assertThat(persistedExecutions).hasSize(1);
        // and
        WorkflowExecution execution = persistedExecutions.get(0);
        assertThat(execution.getWorkflowId()).isEqualTo(workflow.getId());
        assertThat(execution.getDatasetId()).isEqualTo(dataset.getId());
        assertThat(execution.getVersionNumber()).isEqualTo(Long.valueOf(dataset.getVersionNumber()));
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
        datasetVersions.save(dataset.getLatestVersion());
        Workflow workflow = workflows.save(givenWorkflow(1L,
                givenWorkflowStep(TEST_PROVIDER_ID, PAUSING_STEP_ID))
        );
        WorkflowContext context = givenWorkflowExecutionContext(dataset.getId(), workflow);
        doAnswer(invocation -> Optional.of(persistence.of(WorkflowExecution.class).findAll().get(0)))
                .when(executions).findByInvocationId("invocationId");
        // when
        givenMessageConsumer(worker)
                .callProducer(() -> executionFacade.start(workflow, context))
                .andAwaitMessages(1);
        // and
        givenMessageConsumer(worker)
                .callProducer(() -> executionFacade.resume("invocationId", "test"))
                .andAwaitMessages(2);
        // then
        List<WorkflowExecution> executions = persistence.of(WorkflowExecution.class).findAll();
        assertThat(executions).hasSize(1);
        // and
        WorkflowExecution execution = executions.get(0);
        assertThat(execution.getWorkflowId()).isEqualTo(workflow.getId());
        assertThat(execution.getDatasetId()).isEqualTo(dataset.getId());
        assertThat(execution.getVersionNumber()).isEqualTo(Long.valueOf(dataset.getVersionNumber()));
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
        datasetVersions.save(dataset.getLatestVersion());
        Workflow workflow = workflows.save(givenWorkflow(1L,
                givenWorkflowStep(TEST_PROVIDER_ID, FAILING_STEP_ID))
        );
        WorkflowContext context = givenWorkflowExecutionContext(dataset.getId(), workflow);
        // when
        givenMessageConsumer(worker)
                .callProducer(() -> executionFacade.start(workflow, context))
                .andAwaitMessages(3);
        // then
        List<WorkflowExecution> executions = persistence.of(WorkflowExecution.class).findAll();
        assertThat(executions).hasSize(1);
        // and
        WorkflowExecution execution = executions.get(0);
        assertThat(execution.getWorkflowId()).isEqualTo(workflow.getId());
        assertThat(execution.getDatasetId()).isEqualTo(dataset.getId());
        assertThat(execution.getVersionNumber()).isEqualTo(Long.valueOf(dataset.getVersionNumber()));
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
