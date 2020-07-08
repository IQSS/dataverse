package edu.harvard.iq.dataverse.persistence.workflow;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Singleton;
import java.util.Optional;

@Singleton
public class WorkflowExecutionRepository extends JpaRepository<Long, WorkflowExecution> {

    // -------------------- CONSTRUCTORS --------------------

    public WorkflowExecutionRepository() {
        super(WorkflowExecution.class);
    }

    // -------------------- LOGIC --------------------

    /**
     * Finds a workflow execution matching given invocation ID.
     * @param invocationId invocation ID to match.
     * @return {@link Optional Optionally} a {@link WorkflowExecution} matching given invocation ID.
     */
    public Optional<WorkflowExecution> findByInvocationId(String invocationId) {
        return getSingleResult(em.createQuery(
                        "select e " +
                                "from WorkflowExecution e " +
                                "where e.invocationId = :invocationId",
                        WorkflowExecution.class)
                .setParameter("invocationId", invocationId));
    }

    /**
     * Finds most recent workflow execution of given trigger type for given dataset version.
     * @param triggerType trigger type to match.
     * @param datasetId dataset ID.
     * @param majorVersionNumber dataset major version number.
     * @param minorVersionNumber dataset minor version number.
     * @return {@link Optional Optionally} a {@link WorkflowExecution} matching given criteria.
     */
    public Optional<WorkflowExecution> findLatestByTriggerTypeAndDatasetVersion(
            String triggerType, long datasetId, long majorVersionNumber, long minorVersionNumber) {

        return getSingleResult(em.createQuery(
                        "select e " +
                                "from WorkflowExecution e " +
                                "where e.triggerType = :triggerType " +
                                    "and e.datasetId = :datasetId " +
                                    "and e.majorVersionNumber = :majorVersionNumber " +
                                    "and e.minorVersionNumber = :minorVersionNumber " +
                                "order by e.startedAt desc",
                        WorkflowExecution.class)
                .setParameter("triggerType", triggerType)
                .setParameter("datasetId", datasetId)
                .setParameter("majorVersionNumber", majorVersionNumber)
                .setParameter("minorVersionNumber", minorVersionNumber)
                .setMaxResults(1));
    }
}
