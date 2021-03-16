package edu.harvard.iq.dataverse.persistence.workflow;

import edu.harvard.iq.dataverse.persistence.PersistenceArquillianDeployment;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.test.WithTestClock;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import java.util.Optional;

import static edu.harvard.iq.dataverse.persistence.dataset.DatasetMother.givenDataset;
import static edu.harvard.iq.dataverse.persistence.workflow.WorkflowMother.givenWorkflow;
import static edu.harvard.iq.dataverse.persistence.workflow.WorkflowMother.givenWorkflowExecution;
import static edu.harvard.iq.dataverse.persistence.workflow.WorkflowMother.givenWorkflowStep;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

public class WorkflowExecutionRepositoryIT extends PersistenceArquillianDeployment implements WithTestClock {

    @Inject DatasetRepository datasets;
    @Inject WorkflowRepository workflows;
    @Inject WorkflowExecutionRepository executions;

    Dataset dataset;
    Workflow workflow;

    @Before
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
