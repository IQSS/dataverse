package edu.harvard.iq.dataverse.persistence.user;

import edu.harvard.iq.dataverse.persistence.JpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Singleton;
import javax.persistence.Query;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class RoleAssignmentRepository extends JpaRepository<Long, RoleAssignment> {
    private static final Logger logger = LoggerFactory.getLogger(RoleAssignmentRepository.class);

    // -------------------- CONSTRUCTORS --------------------

    public RoleAssignmentRepository() {
        super(RoleAssignment.class);
    }

    // -------------------- LOGIC --------------------

    public List<RoleAssignment> findByDefinitionPointId(long definitionPointId) {
        return em.createNamedQuery("RoleAssignment.listByDefinitionPointId", RoleAssignment.class)
                .setParameter("definitionPointId", definitionPointId)
                .getResultList();
    }

    public List<RoleAssignment> findByDefinitionPointIds(List<Long> definitionPointIds) {
        return em.createQuery("SELECT r FROM RoleAssignment r WHERE r.definitionPoint.id IN :definitionPointIds", RoleAssignment.class)
                .setParameter("definitionPointIds", definitionPointIds)
                .getResultList();
    }

    public List<RoleAssignment> findByAssigneeIdentifier(String assigneeIdentifier) {
        return em.createNamedQuery("RoleAssignment.listByAssigneeIdentifier", RoleAssignment.class)
                .setParameter("assigneeIdentifier", assigneeIdentifier)
                .getResultList();
    }

    public List<RoleAssignment> findByRoleId(long roleId) {
        return em.createQuery("SELECT r FROM RoleAssignment r WHERE r.role.id=:roleId", RoleAssignment.class)
                .setParameter("roleId", roleId)
                .getResultList();
    }

    public List<RoleAssignment> findByAssigneeIdentifiersAndDefinitionPointIds(List<String> assigneeIdentifiers,
                                                                               List<Long> definitionPointIds) {
        return em.createQuery("SELECT r FROM RoleAssignment r WHERE "
                                + " r.assigneeIdentifier in :assigneeIdentifiers AND "
                                + " r.definitionPoint.id in :definitionPointIds", RoleAssignment.class)
                .setParameter("assigneeIdentifiers", assigneeIdentifiers)
                .setParameter("definitionPointIds", definitionPointIds)
                .getResultList();
    }

    public int deleteAllByAssigneeIdentifier(String identifier) {
        return em.createNamedQuery("RoleAssignment.deleteAllByAssigneeIdentifier")
                .setParameter("assigneeIdentifier", identifier)
                .executeUpdate();
    }

    public List<Integer> findDataversesWithUserPermitted(List<String> identifiers) {
        if (identifiers == null || identifiers.isEmpty()) {
            return Collections.emptyList();
        }
        String query = "SELECT id FROM dvobject WHERE dtype = 'Dataverse' " +
                "and id in (select definitionpoint_id from roleassignment " +
                "where assigneeidentifier in ("
                + identifiers.stream().map(i -> "'" + i + "'").collect(Collectors.joining(",")) + "));";
        logger.info("query: {}", query);
        Query nativeQuery = em.createNativeQuery(query);
        return (List<Integer>) nativeQuery.getResultList();
    }
}
