package edu.harvard.iq.dataverse.persistence.user;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Singleton;

import java.util.List;

@Singleton
public class RoleAssignmentRepository extends JpaRepository<Long, RoleAssignment> {

    // -------------------- CONSTRUCTORS --------------------

    public RoleAssignmentRepository() {
        super(RoleAssignment.class);
    }

    // -------------------- LOGIC --------------------

    public List<RoleAssignment> findByDefinitionPointId(long definitionPointId) {
        return em.createQuery("SELECT r FROM RoleAssignment r WHERE r.definitionPoint.id=:definitionPointId", RoleAssignment.class)
                .setParameter("definitionPointId", definitionPointId)
                .getResultList();
    }

    public List<RoleAssignment> findByAssigneeIdentifier(String assigneeIdentifier) {
        return em.createQuery("SELECT r FROM RoleAssignment r WHERE r.assigneeIdentifier=:assigneeIdentifier", RoleAssignment.class).
                setParameter("assigneeIdentifier", assigneeIdentifier)
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
}
