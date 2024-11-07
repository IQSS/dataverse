package edu.harvard.iq.dataverse.persistence.workflow;

import static edu.harvard.iq.dataverse.persistence.dataset.DatasetMother.givenDataset;
import static edu.harvard.iq.dataverse.persistence.workflow.WorkflowMother.givenWorkflow;
import static edu.harvard.iq.dataverse.persistence.workflow.WorkflowMother.givenWorkflowExecution;
import static edu.harvard.iq.dataverse.persistence.workflow.WorkflowMother.givenWorkflowStep;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.common.DBItegrationTest;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.test.WithTestClock;

public class WorkflowExecutionRepositoryIT extends DBItegrationTest implements WithTestClock {
    
    private DatasetRepository datasets = new DatasetRepository(getEntityManager());
    private WorkflowRepository workflows = new WorkflowRepository(getEntityManager());
    private WorkflowExecutionRepository executions = new WorkflowExecutionRepository(getEntityManager());

    Dataset dataset;
    Workflow workflow;

    @BeforeEach
    public void setUp() {
        dataset = datasets.save(givenDataset());
        workflow = workflows.saveFlushAndClear(givenWorkflow(
                givenWorkflowStep("step1"),
                givenWorkflowStep("step2")
        ));
    }

    @Test
    public void shouldSaveNewExecution() {
        // given
        WorkflowExecution execution = givenWorkflowExecution(dataset.getId(), workflow.getId());
        execution.setId(null);
        // when
        executions.saveFlushAndClear(execution);
        // then
        Optional<WorkflowExecution> found = executions.findById(execution.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getDatasetId()).isEqualTo(dataset.getId());
        assertThat(found.get().getWorkflowId()).isEqualTo(workflow.getId());
    }

    @Test
    public void shouldFindExecutionByInvocationId() {
        // given
        WorkflowExecution execution = givenWorkflowExecution(dataset.getId(), workflow.getId());
        // when
        executions.saveFlushAndClear(execution);
        // then
        Optional<WorkflowExecution> found = executions.findByInvocationId(execution.getInvocationId());
        assertThat(found).isPresent();
        assertThat(found.get().getDatasetId()).isEqualTo(dataset.getId());
        assertThat(found.get().getWorkflowId()).isEqualTo(workflow.getId());
    }

    @Test
    public void shouldSaveNewStepExecution() {
        // given
        WorkflowExecution execution = givenWorkflowExecution(dataset.getId(), workflow.getId());
        execution.setId(null);
        execution.start("test", "127.0.0.1", clock);
        executions.saveFlushAndClear(execution);
        // when
        WorkflowExecutionStep stepExecution = execution.nextStepToExecute(workflow.getSteps());
        stepExecution.start(singletonMap("param", "value"), clock);
        executions.saveFlushAndClear(execution);
        // then
        Optional<WorkflowExecution> found = executions.findById(execution.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getSteps()).hasSize(1);
        found.get().getSteps().forEach(step -> {
            assertThat(step.getIndex()).isEqualTo(0);
            assertThat(step.getProviderId()).isEqualTo("internal");
            assertThat(step.getStepType()).isEqualTo("step1");
            assertThat(step.getStartedAt()).isEqualTo(clock.instant());
            assertThat(step.getInputParams()).containsAllEntriesOf(singletonMap("param", "value"));
        });
    }
}
