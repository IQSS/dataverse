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

    public List<WorkflowArtifact> findAllByWorkflowExecutionId(Long workflowExecutionId) {
        return em.createQuery(
                "SELECT a FROM WorkflowArtifacts a WHERE a.workflowExecutionId = :workflowExecutionId",
                WorkflowArtifact.class)
                .setParameter("workflowExecutionId", workflowExecutionId)
                .getResultList();
    }

    public int deleteAllByWorkflowExecutionId(Long workflowExecutionId) {
        return em.createQuery("DELETE a FROM WorkflowArtifacts a WHERE a.workflowExecutionId = :workflowExecutionId")
                .setParameter("workflowExecutionId", workflowExecutionId)
                .executeUpdate();
    }
}
