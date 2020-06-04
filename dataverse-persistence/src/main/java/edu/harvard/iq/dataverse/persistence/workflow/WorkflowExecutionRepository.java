package edu.harvard.iq.dataverse.persistence.workflow;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Singleton;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@Singleton
public class WorkflowExecutionRepository extends JpaRepository<Long, WorkflowExecution> {

    // -------------------- CONSTRUCTORS --------------------

    public WorkflowExecutionRepository() {
        super(WorkflowExecution.class);
    }

    // -------------------- LOGIC --------------------

    public Optional<WorkflowExecution> findByInvocationId(String invocationId) {
        return ofNullable(em.createQuery(
                        "select e from WorkflowExecution e where e.invocationId = :invocationId",
                        WorkflowExecution.class)
                .setParameter("invocationId", invocationId)
                .getSingleResult());
    }
}
