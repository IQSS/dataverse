package edu.harvard.iq.dataverse.persistence.workflow;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Singleton;

@Singleton
public class WorkflowExecutionStepRepository extends JpaRepository<Long, WorkflowExecutionStep> {

    // -------------------- CONSTRUCTORS --------------------

    public WorkflowExecutionStepRepository() {
        super(WorkflowExecutionStep.class);
    }
}
