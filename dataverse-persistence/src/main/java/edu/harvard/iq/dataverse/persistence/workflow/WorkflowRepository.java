package edu.harvard.iq.dataverse.persistence.workflow;

import javax.ejb.Singleton;
import javax.persistence.EntityManager;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Singleton
public class WorkflowRepository extends JpaRepository<Long, Workflow> {

    // -------------------------------------------------------------------------
    public WorkflowRepository() {
        
        super(Workflow.class);
    }

    // -------------------------------------------------------------------------
    public WorkflowRepository(final EntityManager em) {
        
        super(Workflow.class);
        super.em = em;
    }
}
