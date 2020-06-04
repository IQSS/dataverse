package edu.harvard.iq.dataverse.persistence.workflow;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Singleton;

@Singleton
public class WorkflowRepository extends JpaRepository<Long, Workflow> {

    // -------------------- CONSTRUCTORS --------------------

    public WorkflowRepository() {
        super(Workflow.class);
    }
}
