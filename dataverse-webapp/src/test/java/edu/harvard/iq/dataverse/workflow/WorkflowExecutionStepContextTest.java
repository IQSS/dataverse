package edu.harvard.iq.dataverse.workflow;

import edu.harvard.iq.dataverse.persistence.workflow.Workflow;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecution;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionRepository;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.Pending;
import edu.harvard.iq.dataverse.workflow.step.Success;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.WorkflowStepResult;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static edu.harvard.iq.dataverse.persistence.workflow.WorkflowMother.givenWorkflow;
import static edu.harvard.iq.dataverse.persistence.workflow.WorkflowMother.givenWorkflowExecution;
import static edu.harvard.iq.dataverse.persistence.workflow.WorkflowMother.givenWorkflowStep;
import static edu.harvard.iq.dataverse.workflow.WorkflowContextMother.givenWorkflowExecutionContext;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class WorkflowExecutionStepContextTest {

    WorkflowExecutionRepository executions = mock(WorkflowExecutionRepository.class);
    WorkflowStepRegistry steps = mock(WorkflowStepRegistry.class);

    long datasetId = 1L;
    Workflow workflow = givenWorkflow(1L,
            givenWorkflowStep("step1")
    );

    WorkflowExecution execution = givenWorkflowExecution(datasetId, workflow.getId());
    WorkflowExecutionContext context = givenWorkflowExecutionContext(workflow, execution);

    Clock clock = Clock.fixed(Instant.parse("2020-06-01T09:10:20.00Z"), UTC);

    @BeforeEach
    void setUp() {
        context.start(executions, clock);
    }

    @Test
    void shouldNotBeStartedUponCreation() {
        // when
        WorkflowExecutionStepContext stepContext = context.nextStepToExecute(executions);
        // then
        assertThat(stepContext.getStepExecution().isStarted()).isFalse();
    }

    @Test
    void shouldStartStepExecution() {
        // given
        givenImmediateWorkflowStep();
        WorkflowExecutionStepContext stepContext = context.nextStepToExecute(executions);
        Map<String, String> params = Stream.of(
                Tuple.of("test", "value"),
                Tuple.of("param", "value")
        ).collect(toMap(Tuple2::_1, Tuple2::_2));
        // when
        Supplier<WorkflowStepResult> stepRunner = stepContext.start(singletonMap("test", "value"), steps, clock);
        // then
        assertThat(stepContext.getStepExecution().isStarted()).isTrue();
        assertThat(stepContext.getStepExecution().getInputParams()).containsExactlyEntriesOf(params);
        assertThat(stepRunner.get()).isEqualTo(new Success(params));
    }

    @Test
    void shouldPauseStartedExecution() {
        // given
        givenPausingWorkflowStep();
        WorkflowExecutionStepContext stepContext = context.nextStepToExecute(executions);
        stepContext.start(emptyMap(), steps, clock);
        // when
        stepContext.pause(singletonMap("test", "value"), clock);
        // then
        assertThat(stepContext.getStepExecution().isPaused()).isTrue();
        assertThat(stepContext.getStepExecution().getPausedData())
                .containsExactlyEntriesOf(singletonMap("test", "value"));
    }

    @Test
    void shouldResumePausedExecution() {
        // given
        givenPausingWorkflowStep();
        WorkflowExecutionStepContext stepContext = context.nextStepToExecute(executions);
        stepContext.start(emptyMap(), steps, clock);
        stepContext.pause(singletonMap("test", "value"), clock);
        // when
        Supplier<WorkflowStepResult> stepRunner = stepContext.resume("test", steps, clock);
        // then
        assertThat(stepContext.getStepExecution().isResumed()).isTrue();
        assertThat(stepContext.getStepExecution().getResumedData()).isEqualTo("test");
        assertThat(stepRunner.get()).isEqualTo(new Success(singletonMap("test", "value")));
    }

    @Test
    void shouldFinishSuccessfully() {
        // given
        givenPausingWorkflowStep();
        WorkflowExecutionStepContext stepContext = context.nextStepToExecute(executions);
        stepContext.start(emptyMap(), steps, clock);
        // when
        stepContext.success(singletonMap("test", "value"), clock);
        // then
        assertThat(stepContext.getStepExecution().isFinished()).isTrue();
        assertThat(stepContext.getStepExecution().getFinishedSuccessfully()).isTrue();
        assertThat(stepContext.getStepExecution().getOutputParams())
                .containsExactlyEntriesOf(singletonMap("test", "value"));
    }

    @Test
    void shouldFinishWithError() {
        // given
        givenPausingWorkflowStep();
        WorkflowExecutionStepContext stepContext = context.nextStepToExecute(executions);
        stepContext.start(emptyMap(), steps, clock);
        // when
        stepContext.failure(singletonMap("test", "value"), clock);
        assertThat(stepContext.getStepExecution().isFinished()).isTrue();
        assertThat(stepContext.getStepExecution().getFinishedSuccessfully()).isFalse();
        assertThat(stepContext.getStepExecution().getOutputParams())
                .containsExactlyEntriesOf(singletonMap("test", "value"));
    }

    private void givenImmediateWorkflowStep() {
        doAnswer(invocation -> new TestWorkflowStep(invocation.getArgument(2), false))
                .when(steps).getStep(anyString(), anyString(), anyMap());
    }

    private void givenPausingWorkflowStep() {
        doAnswer(invocation -> new TestWorkflowStep(invocation.getArgument(2), true))
                .when(steps).getStep(anyString(), anyString(), anyMap());
    }

    private static class TestWorkflowStep implements WorkflowStep {

        private final Map<String, String> params;
        private final boolean pause;

        public TestWorkflowStep(Map<String, String> params, boolean pause) {
            this.params = params;
            this.pause = pause;
        }

        @Override
        public WorkflowStepResult run(WorkflowExecutionContext context) {
            return pause ? new Pending(params) : new Success(params);
        }

        @Override
        public WorkflowStepResult resume(WorkflowExecutionContext context, Map<String, String> internalData, String externalData) {
            return new Success(internalData);
        }

        @Override
        public void rollback(WorkflowExecutionContext context, Failure reason) {
        }
    }
}
