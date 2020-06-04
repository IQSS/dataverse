package edu.harvard.iq.dataverse.workflow;

import edu.harvard.iq.dataverse.persistence.workflow.Workflow;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecution;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowExecutionRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;

import static edu.harvard.iq.dataverse.persistence.workflow.WorkflowMother.givenWorkflow;
import static edu.harvard.iq.dataverse.persistence.workflow.WorkflowMother.givenWorkflowExecution;
import static edu.harvard.iq.dataverse.persistence.workflow.WorkflowMother.givenWorkflowStep;
import static edu.harvard.iq.dataverse.workflow.WorkflowContextMother.givenWorkflowExecutionContext;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class WorkflowExecutionContextTest {

    WorkflowExecutionRepository executions = mock(WorkflowExecutionRepository.class);
    WorkflowStepRegistry steps = mock(WorkflowStepRegistry.class);

    long datasetId = 1L;
    Workflow workflow = givenWorkflow(1L,
            givenWorkflowStep("step1")
    );

    WorkflowExecution execution = givenWorkflowExecution(datasetId, workflow.getId());
    WorkflowExecutionContext context = givenWorkflowExecutionContext(workflow, execution);

    Clock clock = Clock.fixed(Instant.parse("2020-06-01T09:10:20.00Z"), UTC);

    @Test
    void shouldHaveFirstStepToExecute() {
        // expect
        assertThat(context.hasMoreStepsToExecute()).isTrue();
    }

    @Test
    void shouldStartExecution() {
        // when
        context.start(executions, clock);
        // then
        assertThat(execution.isStarted()).isTrue();
        assertThat(execution.getStartedAt()).isEqualTo(clock.instant());
        assertThat(execution.getUserId()).isNotBlank();
        assertThat(execution.getIpAddress()).isNotBlank();
    }

    @Test
    void shouldNotGetFirstStepWhenNotStarted() {
        // expect
        assertThatThrownBy(() -> context.nextStepToExecute(executions))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldGetFirstStepToExecute() {
        // given
        context.start(executions, clock);
        // when
        WorkflowExecutionStepContext stepContext = context.nextStepToExecute(executions);
        // then
        assertThat(execution.getSteps()).containsExactly(stepContext.getStepExecution());
        assertThat(stepContext.getStepExecution().isStarted()).isFalse();
    }

    @Test
    void shouldGetSameStepToExecuteWhenNotFinished() {
        // given
        context.start(executions, clock);
        WorkflowExecutionStepContext stepContext = context.nextStepToExecute(executions);
        // when
        WorkflowExecutionStepContext otherContext = context.nextStepToExecute(executions);
        // then
        assertThat(stepContext.getStepExecution()).isSameAs(otherContext.getStepExecution());
    }

    @Test
    void shouldNotGetMoreStepsToExecuteWhenAllFinished() {
        // given
        context.start(executions, clock);
        WorkflowExecutionStepContext stepContext = context.nextStepToExecute(executions);
        stepContext.start(emptyMap(), steps, clock);
        stepContext.success(emptyMap(), clock);
        // expect
        assertThatThrownBy(() -> context.nextStepToExecute(executions))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldNotFinishWhenNotStarted() {
        assertThatThrownBy(() -> context.finish(executions, clock))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldFinishExecution() {
        // given
        context.start(executions, clock);
        // when
        context.finish(executions, clock);
        // then
        assertThat(execution.isFinished()).isTrue();
        assertThat(execution.getFinishedAt()).isEqualTo(clock.instant());
    }

    @Test
    void shouldHaveNothingToRollbackWhenNothingExecuted() {
        // expect
        assertThat(context.hasMoreStepsToRollback()).isFalse();
    }

    @Test
    void shouldHaveNothingToRollbackWhenNothingStarted() {
        // given
        context.start(executions, clock);
        context.nextStepToExecute(executions);
        // expect
        assertThat(context.hasMoreStepsToRollback()).isFalse();
    }

    @Test
    void shouldHaveLastStartedStepToRollBack() {
        // given
        context.start(executions, clock);
        context.nextStepToExecute(executions)
                .start(emptyMap(), steps, clock);
        // expect
        assertThat(context.hasMoreStepsToRollback()).isTrue();
    }

    @Test
    void shouldNotGetStepToRollbackWhenNothingExecuted() {
        // expect
        assertThatThrownBy(() -> context.nextStepToRollback(executions))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldNotGetStepToRollbackWhenNotFinished() {
        // given
        context.start(executions, clock);
        WorkflowExecutionStepContext stepContext = context.nextStepToExecute(executions);
        stepContext.start(emptyMap(), steps, clock);
        // expect
        assertThatThrownBy(() -> context.nextStepToRollback(executions))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldGetLastStartedStepToRollBack() {
        // given
        context.start(executions, clock);
        WorkflowExecutionStepContext stepContext = context.nextStepToExecute(executions);
        stepContext.start(emptyMap(), steps, clock);
        context.finish(executions, clock);
        // when
        WorkflowExecutionStepContext otherContext = context.nextStepToRollback(executions);
        // then
        assertThat(stepContext.getStepExecution()).isSameAs(otherContext.getStepExecution());
        assertThat(otherContext.getStepExecution().isRollBackNeeded()).isTrue();
    }
}
