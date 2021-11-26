package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseRepository;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole;
import edu.harvard.iq.dataverse.persistence.user.DataverseRoleRepository;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignee;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignmentRepository;
import edu.harvard.iq.dataverse.search.index.PermissionReindexEvent;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.persistence.EntityNotFoundException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author michael
 */
@Stateless
public class DataverseRoleServiceBean implements java.io.Serializable {

    @EJB
    private RoleAssigneeServiceBean roleAssigneeService;
    @Inject
    private Event<PermissionReindexEvent> permissionReindexEvent;
    @Inject
    private DataverseRoleRepository dataverseRoleRepository;
    @Inject
    private RoleAssignmentRepository roleAssignmentRepository;
    @Inject
    private DataverseRepository dataverseRepository;

    public DataverseRole save(DataverseRole aRole) {
        boolean shouldIndexPermissions = !aRole.isNew();

        aRole = dataverseRoleRepository.save(aRole);

        if (shouldIndexPermissions) {
            permissionReindexEvent.fire(new PermissionReindexEvent(aRole.getOwner()));
        }

        return aRole;
    }

    public RoleAssignment save(RoleAssignment assignment) {
        return save(assignment, true);
    }

    public RoleAssignment save(RoleAssignment assignment, boolean createIndex) {
        roleAssignmentRepository.save(assignment);

        if (createIndex) {
            permissionReindexEvent.fire(new PermissionReindexEvent(assignment.getDefinitionPoint()));
        }
        return assignment;
    }

    public DataverseRole find(Long id) {
        return dataverseRoleRepository.getById(id);
    }

    public List<DataverseRole> findAll() {
        return dataverseRoleRepository.findAll();
    }

    public void delete(Long id) {
        dataverseRoleRepository.deleteById(id);
    }

    public List<DataverseRole> findByOwnerId(Long ownerId) {
        return dataverseRoleRepository.findByOwnerId(ownerId);
    }

    public List<DataverseRole> findBuiltinRoles() {
        return dataverseRoleRepository.findWithoutOwner();
    }

    public DataverseRole findBuiltinRoleByAlias(BuiltInRole builtInRole) {
        return dataverseRoleRepository.findByAlias(builtInRole.getAlias())
                .orElseThrow(() -> new IllegalStateException("Builtin role is not present in database: " + builtInRole));
    }

    public DataverseRole findRoleByAliasAssignableInDataverse(String alias, Long dataverseId) {
        return dataverseRoleRepository.findByAlias(alias)
                .filter(role -> role.getOwner() == null || role.getOwner().getId() == dataverseId)
                .orElseThrow(() -> new EntityNotFoundException("No such role: " + alias + " that can be assigned in dataverse: " + dataverseId));
    }

    public void revoke(RoleAssignment ra) {
        roleAssignmentRepository.mergeAndDelete(ra);
        permissionReindexEvent.fire(new PermissionReindexEvent(ra.getDefinitionPoint()));
    }

    // "nuclear" remove-all roles for a user or group:
    // (Note that all the "definition points" - i.e., the dvObjects
    // on which the roles were assigned - need to be reindexed for permissions
    // once the role assignments are removed!
    public void revokeAll(RoleAssignee assignee) {
        List<DvObject> reindexSet = new ArrayList<>();

        for (RoleAssignment ra : roleAssigneeService.getAssignmentsFor(assignee.getIdentifier())) {
            roleAssignmentRepository.delete(ra);
            reindexSet.add(ra.getDefinitionPoint());
        }

        permissionReindexEvent.fire(new PermissionReindexEvent(reindexSet));
    }

    public List<RoleAssignment> roleAssignments(Long roleId) {
        return roleAssignmentRepository.findByRoleId(roleId);
    }

    public Set<RoleAssignment> rolesAssignments(DvObject dv) {
        LinkedList<Long> dvOwnerIds = new LinkedList<>();
        dvOwnerIds.add(dv.getId());

        while (!dv.isEffectivelyPermissionRoot()) {
            dv = dv.getOwner();
            dvOwnerIds.add(dv.getId());
        }

        return new HashSet<>(roleAssignmentRepository.findByDefinitionPointIds(dvOwnerIds));
    }

    /**
     * Retrieves the roles assignments for {@code user}, directly on {@code dv}.
     * No traversal on the containment hierarchy is done.
     *
     * @param roas the user whose roles are given
     * @param dvo  the object where the roles are defined.
     * @return Set of roles defined for the user in the given dataverse.
     * @see #roleAssignments(edu.harvard.iq.dataverse.DataverseUser,
     * edu.harvard.iq.dataverse.persistence.dataverse.Dataverse)
     */
    public List<RoleAssignment> directRoleAssignments(RoleAssignee roas, DvObject dvo) {
        List<RoleAssignment> unfiltered = roleAssignmentRepository.findByAssigneeIdentifier(roas.getIdentifier());
        return unfiltered.stream()
                .filter(roleAssignment -> Objects.equals(roleAssignment.getDefinitionPoint().getId(), dvo.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the roles assignments for {@code user}, directly on {@code dv}.
     * No traversal on the containment hierarchy is done.
     *
     * @param roleAssignees the user whose roles are given
     * @param dvos          the objects where the roles are defined.
     * @return Set of roles defined for the user in the given dataverse.
     * @see #roleAssignments(edu.harvard.iq.dataverse.DataverseUser,
     * edu.harvard.iq.dataverse.persistence.dataverse.Dataverse)
     */
    public List<RoleAssignment> directRoleAssignmentsByAssigneesAndDvObjects(Set<? extends RoleAssignee> roleAssignees, Collection<DvObject> dvos) {
        if (dvos.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> raIds = roleAssignees.stream().map(roas -> roas.getIdentifier()).collect(Collectors.toList());
        List<Long> dvoIds = dvos.stream().filter(dvo -> !(dvo.getId() == null)).map(dvo -> dvo.getId()).collect(Collectors.toList());

        return roleAssignmentRepository.findByAssigneeIdentifiersAndDefinitionPointIds(raIds, dvoIds);
    }

    /**
     * Retrieves the roles assignments for {@code user}, directly on {@code dv}.
     * No traversal on the containment hierarchy is done.
     *
     * @param dvo the object where the roles are defined.
     * @return Set of roles defined for the user in the given dataverse.
     * @see #roleAssignments(edu.harvard.iq.dataverse.DataverseUser,
     * edu.harvard.iq.dataverse.persistence.dataverse.Dataverse)
     */
    public List<RoleAssignment> directRoleAssignments(DvObject dvo) {
        return roleAssignmentRepository.findByDefinitionPointId(dvo.getId());
    }

    /**
     * Get all the available roles in a given dataverse, mapped by the dataverse
     * that defines them. Map entries are ordered by reversed hierarchy (root is
     * always last).
     *
     * @param dvId The id of dataverse whose available roles we query
     * @return map of available roles.
     */
    public Set<DataverseRole> availableRoles(Long dvId) {
        Dataverse dv = dataverseRepository.getById(dvId);
        Set<DataverseRole> roles = dv.getRoles();
        roles.addAll(findBuiltinRoles());

        while (!dv.isEffectivelyPermissionRoot()) {
            dv = dv.getOwner();
            roles.addAll(dv.getRoles());
        }

        return roles;
    }
}
