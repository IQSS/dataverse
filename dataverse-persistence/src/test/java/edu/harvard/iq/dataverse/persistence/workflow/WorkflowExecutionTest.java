package edu.harvard.iq.dataverse.persistence.workflow;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;

import static edu.harvard.iq.dataverse.persistence.workflow.WorkflowMother.givenWorkflow;
import static edu.harvard.iq.dataverse.persistence.workflow.WorkflowMother.givenWorkflowExecution;
import static edu.harvard.iq.dataverse.persistence.workflow.WorkflowMother.givenWorkflowStep;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowExecutionTest {

    long datasetId = 1L;
    Workflow workflow = givenWorkflow(1L,
            givenWorkflowStep("step1"),
            givenWorkflowStep("step2"),
            givenWorkflowStep("step3")
    );
    WorkflowExecution execution = givenWorkflowExecution(datasetId, workflow.getId());

    Clock clock = Clock.fixed(Instant.parse("2020-06-01T09:10:20.00Z"), UTC);

    @Test
    void shouldGenerateInvocationId() {
        // expect
        assertThat(execution.getInvocationId()).isNotBlank();
        assertThat(execution.isStarted()).isFalse();
        assertThat(execution.isPaused()).isFalse();
        assertThat(execution.isFinished()).isFalse();
    }

    @Test
    void shouldStartExecution() {
        // when
        execution.start("test", "127.0.1.1", clock);
        // then
        assertThat(execution.isStarted()).isTrue();
        assertThat(execution.getStartedAt()).isEqualTo(clock.instant());
        assertThat(execution.getUserId()).isEqualTo("test");
        assertThat(execution.getIpAddress()).isEqualTo("127.0.1.1");
        assertThat(execution.isPaused()).isFalse();
        assertThat(execution.isFinished()).isFalse();
    }

    @Test
    void shouldStartExecutionOnlyOnce() {
        // when
        execution.start("test", "127.0.1.1", clock);
        // then
        assertThatThrownBy(() -> execution.start("other", "127.0.2.1", clock))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot start workflow - already started");
    }

    @Test
    void shouldHaveMoreStepsWhenEmptyExecution() {
        // when
        boolean result = execution.hasMoreStepsToExecute(workflow.getSteps());
        // then
        assertThat(result).isTrue();
    }

    @Test
    void shouldNoHaveMoreStepsWhenEmptySteps() {
        // when
        boolean result = execution.hasMoreStepsToExecute(emptyList());
        // then
        assertThat(result).isFalse();
    }

    @Test
    void shouldBePausedOnlyWhenLastStepPaused() {
        // when
        WorkflowExecutionStep stepExecution = execution.newStepExecution(workflow.getSteps(), 0);
        execution.getSteps().add(stepExecution);
        // then
        assertThat(execution.isPaused()).isFalse();
        // when
        stepExecution.start(emptyMap(), clock);
        // then
        assertThat(execution.isPaused()).isFalse();
        // when
        stepExecution.pause(emptyMap(), clock);
        // then
        assertThat(execution.isPaused()).isTrue();
        // when
        stepExecution.resume("test", clock);
        // then
        assertThat(execution.isPaused()).isFalse();
    }
}
