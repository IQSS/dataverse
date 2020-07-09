package edu.harvard.iq.dataverse.workflow.execution;

import edu.harvard.iq.dataverse.persistence.workflow.Workflow;
import edu.harvard.iq.dataverse.workflow.internalspi.LoggingWorkflowStep;
import edu.harvard.iq.dataverse.workflow.step.Failure;
import edu.harvard.iq.dataverse.workflow.step.Success;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static edu.harvard.iq.dataverse.persistence.workflow.WorkflowMother.givenWorkflow;
import static edu.harvard.iq.dataverse.persistence.workflow.WorkflowMother.givenWorkflowStep;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

class WorkflowExecutionSchedulerTest extends WorkflowExecutionJMSTestBase {

    long datasetId = 1L;
    Workflow workflow = givenWorkflow(1L,
            givenWorkflowStep(LoggingWorkflowStep.STEP_ID)
    );

    WorkflowExecutionScheduler scheduler = new WorkflowExecutionScheduler();

    WorkflowExecutionSchedulerTest() throws Exception { }

    @BeforeEach
    void setUp() {
        scheduler.setFactory(factory);
        scheduler.setQueue(queue);
    }

    @Test
    void shouldSendFirstStepExecutionMessage() throws Exception {
        // given
        WorkflowExecutionContext context = givenWorkflowExecutionContext(datasetId, workflow);
        // when
        WorkflowExecutionMessage message = callProducer(() -> scheduler.executeFirstWorkflowStep(context))
                .andAwaitOneMessageWithBody();
        // then
        assertThat(message.getTriggerType()).isEqualTo(context.type.name());
        assertThat(message.getDatasetId()).isEqualTo(datasetId);
        assertThat(message.getMajorVersionNumber()).isEqualTo(1L);
        assertThat(message.getMinorVersionNumber()).isEqualTo(0L);
        assertThat(message.getUserId()).isEqualTo("@null");
        assertThat(message.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(message.isDatasetExternallyReleased()).isFalse();
        assertThat(message.getWorkflowId()).isEqualTo(1L);
        assertThat(message.getWorkflowExecutionId()).isEqualTo(1L);
        assertThat(message.isRollback()).isFalse();
        assertThat(message.getLastStepSuccess()).isEqualTo(new Success());
        assertThat(message.getExternalData()).isNull();
    }

    @Test
    void shouldSendNextStepExecutionMessage() throws Exception {
        // given
        WorkflowExecutionContext context = givenWorkflowExecutionContext(datasetId, workflow);
        Success lastStepResult = new Success(singletonMap("test", "123"));
        // when
        WorkflowExecutionMessage message = callProducer(() -> scheduler.executeNextWorkflowStep(context, lastStepResult))
                .andAwaitOneMessageWithBody();
        // then
        assertThat(message.getTriggerType()).isEqualTo(context.type.name());
        assertThat(message.getDatasetId()).isEqualTo(datasetId);
        assertThat(message.getMajorVersionNumber()).isEqualTo(1L);
        assertThat(message.getMinorVersionNumber()).isEqualTo(0L);
        assertThat(message.getUserId()).isEqualTo("@null");
        assertThat(message.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(message.isDatasetExternallyReleased()).isFalse();
        assertThat(message.getWorkflowId()).isEqualTo(1L);
        assertThat(message.getWorkflowExecutionId()).isEqualTo(1L);
        assertThat(message.isRollback()).isFalse();
        assertThat(message.getLastStepSuccess()).isEqualTo(lastStepResult);
        assertThat(message.getExternalData()).isNull();
    }

    @Test
    void shouldSendPausedStepResumeMessage() throws Exception {
        // given
        WorkflowExecutionContext context = givenWorkflowExecutionContext(datasetId, workflow);
        String externalData = "test";
        // when
        WorkflowExecutionMessage message = callProducer(() -> scheduler.resumePausedWorkflowStep(context, externalData))
                .andAwaitOneMessageWithBody();
        // then
        assertThat(message.getTriggerType()).isEqualTo(context.type.name());
        assertThat(message.getDatasetId()).isEqualTo(datasetId);
        assertThat(message.getMajorVersionNumber()).isEqualTo(1L);
        assertThat(message.getMinorVersionNumber()).isEqualTo(0L);
        assertThat(message.getUserId()).isEqualTo("@null");
        assertThat(message.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(message.isDatasetExternallyReleased()).isFalse();
        assertThat(message.getWorkflowId()).isEqualTo(1L);
        assertThat(message.getWorkflowExecutionId()).isEqualTo(1L);
        assertThat(message.isRollback()).isFalse();
        assertThat(message.getLastStepSuccess()).isEqualTo(new Success());
        assertThat(message.getExternalData()).isEqualTo(externalData);
    }

    @Test
    void shouldSendNextStepRollbackMessage() throws Exception {
        // given
        WorkflowExecutionContext context = givenWorkflowExecutionContext(datasetId, workflow);
        Failure lastStepResult = new Failure("error");
        // when
        WorkflowExecutionMessage message = callProducer(() -> scheduler.rollbackNextWorkflowStep(context, lastStepResult))
                .andAwaitOneMessageWithBody();
        // then
        assertThat(message.getTriggerType()).isEqualTo(context.type.name());
        assertThat(message.getDatasetId()).isEqualTo(datasetId);
        assertThat(message.getMajorVersionNumber()).isEqualTo(1L);
        assertThat(message.getMinorVersionNumber()).isEqualTo(0L);
        assertThat(message.getUserId()).isEqualTo("@null");
        assertThat(message.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(message.isDatasetExternallyReleased()).isFalse();
        assertThat(message.getWorkflowId()).isEqualTo(1L);
        assertThat(message.getWorkflowExecutionId()).isEqualTo(1L);
        assertThat(message.isRollback()).isTrue();
        assertThat(message.getLastStepFailure()).isEqualTo(lastStepResult);
        assertThat(message.getExternalData()).isNull();
    }
}
