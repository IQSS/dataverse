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

    private RoleAssigneeServiceBean roleAssigneeService;
    private Event<PermissionReindexEvent> permissionReindexEvent;
    private DataverseRoleRepository dataverseRoleRepository;
    private RoleAssignmentRepository roleAssignmentRepository;
    private DataverseRepository dataverseRepository;

    // -------------------- CONSTRUCTORS --------------------

    public DataverseRoleServiceBean() { }

    @Inject
    public DataverseRoleServiceBean(RoleAssigneeServiceBean roleAssigneeService, Event<PermissionReindexEvent> permissionReindexEvent,
                                    DataverseRoleRepository dataverseRoleRepository, RoleAssignmentRepository roleAssignmentRepository,
                                    DataverseRepository dataverseRepository) {
        this.roleAssigneeService = roleAssigneeService;
        this.permissionReindexEvent = permissionReindexEvent;
        this.dataverseRoleRepository = dataverseRoleRepository;
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.dataverseRepository = dataverseRepository;
    }

    // -------------------- LOGIC --------------------

    public DataverseRole save(DataverseRole role) {
        boolean shouldIndexPermissions = !role.isNew();
        role = dataverseRoleRepository.save(role);
        if (shouldIndexPermissions) {
            permissionReindexEvent.fire(new PermissionReindexEvent(role.getOwner()));
        }
        return role;
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
                .filter(r -> r.getOwner() == null || Objects.equals(r.getOwner().getId(), dataverseId))
                .orElseThrow(() -> new EntityNotFoundException("No such role: " + alias + " that can be assigned in dataverse: " + dataverseId));
    }

    public void revoke(RoleAssignment assignment) {
        roleAssignmentRepository.mergeAndDelete(assignment);
        permissionReindexEvent.fire(new PermissionReindexEvent(assignment.getDefinitionPoint()));
    }

    // "nuclear" remove-all roles for a user or group:
    // (Note that all the "definition points" - i.e., the dvObjects
    // on which the roles were assigned - need to be reindexed for permissions
    // once the role assignments are removed!
    public void revokeAll(RoleAssignee assignee) {
        List<DvObject> reindexSet = new ArrayList<>();
        for (RoleAssignment assignment : roleAssigneeService.getAssignmentsFor(assignee.getIdentifier())) {
            roleAssignmentRepository.delete(assignment);
            reindexSet.add(assignment.getDefinitionPoint());
        }
        permissionReindexEvent.fire(new PermissionReindexEvent(reindexSet));
    }

    public List<RoleAssignment> roleAssignments(Long roleId) {
        return roleAssignmentRepository.findByRoleId(roleId);
    }

    public Set<RoleAssignment> rolesAssignments(DvObject dvObject) {
        LinkedList<Long> dvOwnerIds = new LinkedList<>();
        dvOwnerIds.add(dvObject.getId());
        while (!dvObject.isEffectivelyPermissionRoot()) {
            dvObject = dvObject.getOwner();
            dvOwnerIds.add(dvObject.getId());
        }
        return new HashSet<>(roleAssignmentRepository.findByDefinitionPointIds(dvOwnerIds));
    }

    /**
     * Retrieves the roles assignments for {@code user}, directly on {@code dv}.
     * No traversal on the containment hierarchy is done.
     *
     * @param assignee the user whose roles are given
     * @param dvObject  the object where the roles are defined.
     * @return Set of roles defined for the user in the given dataverse.
     * @see #roleAssignments(edu.harvard.iq.dataverse.DataverseUser,
     * edu.harvard.iq.dataverse.persistence.dataverse.Dataverse)
     */
    public List<RoleAssignment> directRoleAssignments(RoleAssignee assignee, DvObject dvObject) {
        List<RoleAssignment> assignments = roleAssignmentRepository.findByAssigneeIdentifier(assignee.getIdentifier());
        return assignments.stream()
                .filter(a -> Objects.equals(a.getDefinitionPoint().getId(), dvObject.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the roles assignments for {@code user}, directly on {@code dv}.
     * No traversal on the containment hierarchy is done.
     *
     * @param roleAssignees the user whose roles are given
     * @param dvObjects the objects where the roles are defined.
     * @return Set of roles defined for the user in the given dataverse.
     * @see #roleAssignments(edu.harvard.iq.dataverse.DataverseUser,
     * edu.harvard.iq.dataverse.persistence.dataverse.Dataverse)
     */
    public List<RoleAssignment> directRoleAssignmentsByAssigneesAndDvObjects(Set<? extends RoleAssignee> roleAssignees,
                                                                             Collection<DvObject> dvObjects) {
        if (dvObjects.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> assigneesIds = roleAssignees.stream()
                .map(RoleAssignee::getIdentifier)
                .collect(Collectors.toList());
        List<Long> objectsIds = dvObjects.stream()
                .map(DvObject::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return roleAssignmentRepository.findByAssigneeIdentifiersAndDefinitionPointIds(assigneesIds, objectsIds);
    }

    /**
     * Retrieves the roles assignments for {@code user}, directly on {@code dv}.
     * No traversal on the containment hierarchy is done.
     *
     * @param dvObject the object where the roles are defined.
     * @return Set of roles defined for the user in the given dataverse.
     * @see #roleAssignments(edu.harvard.iq.dataverse.DataverseUser,
     * edu.harvard.iq.dataverse.persistence.dataverse.Dataverse)
     */
    public List<RoleAssignment> directRoleAssignments(DvObject dvObject) {
        return roleAssignmentRepository.findByDefinitionPointId(dvObject.getId());
    }

    /**
     * Get all the available roles in a given dataverse, mapped by the dataverse
     * that defines them. Map entries are ordered by reversed hierarchy (root is
     * always last).
     *
     * @param dataverseId The id of dataverse whose available roles we query
     * @return map of available roles.
     */
    public Set<DataverseRole> availableRoles(Long dataverseId) {
        Dataverse dv = dataverseRepository.getById(dataverseId);
        Set<DataverseRole> roles = dv.getRoles();
        roles.addAll(findBuiltinRoles());

        while (!dv.isEffectivelyPermissionRoot()) {
            dv = dv.getOwner();
            roles.addAll(dv.getRoles());
        }
        return roles;
    }
}