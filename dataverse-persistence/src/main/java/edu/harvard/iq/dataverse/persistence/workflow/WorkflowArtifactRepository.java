package edu.harvard.iq.dataverse.persistence.workflow;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Singleton;
import java.util.List;

@Singleton
public class WorkflowArtifactRepository extends JpaRepository<Long, WorkflowArtifact> {

    // -------------------- CONSTRUCTORS --------------------

    public WorkflowArtifactRepository() {
        super(WorkflowArtifact.class);
    }

    // -------------------- LOGIC --------------------

    public List<WorkflowArtifact> findByWorkflowExecutionId(Long workflowExecutionId) {
        return em.createQuery(
                        "select a " +
                                "from WorkflowArtifact a " +
                                "where a.workflowExecutionId = :workflowExecutionId",
                        WorkflowArtifact.class)
                .setParameter("workflowExecutionId", workflowExecutionId)
                .getResultList();
    }

    public int deleteByWorkflowExecutionId(Long workflowExecutionId) {
        return em.createQuery(
                        "select a " +
                                "from WorkflowArtifact a " +
                                "where a.workflowExecutionId = :workflowExecutionId")
                .setParameter("workflowExecutionId", workflowExecutionId)
                .executeUpdate();
    }
}
