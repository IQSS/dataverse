package edu.harvard.iq.dataverse.persistence.workflow;

import static edu.harvard.iq.dataverse.persistence.workflow.WorkflowMother.givenWorkflow;
import static edu.harvard.iq.dataverse.persistence.workflow.WorkflowMother.givenWorkflowStep;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.common.DBItegrationTest;

public class WorkflowRepositoryIT extends DBItegrationTest {

    private WorkflowRepository workflows = new WorkflowRepository(getEntityManager());

    // -------------------------------------------------------------------------
    @Test
    public void shouldSaveNewWorkflow() {
        // given
        Workflow workflow = givenWorkflow();
        // when
        workflows.saveFlushAndClear(workflow);
        // then
        Optional<Workflow> found = workflows.findById(workflow.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo(workflow.getName());

    }

    // -------------------------------------------------------------------------
    @Test
    public void shouldSaveWorkflowWithSteps() {
        // given
        Workflow workflow = givenWorkflow(givenWorkflowStep("step1"),
                givenWorkflowStep("step2"));
        // when
        workflows.saveFlushAndClear(workflow);
        // then
        Optional<Workflow> found = workflows.findById(workflow.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo(workflow.getName());
        assertThat(found.get().getSteps()).hasSize(2);
        found.get().getSteps().forEach(step -> {
            assertThat(step.getParent()).isSameAs(found.get());
            assertThat(step.getStepParameters())
                    .containsAllEntriesOf(singletonMap("param", "value"));
        });
    }
}
