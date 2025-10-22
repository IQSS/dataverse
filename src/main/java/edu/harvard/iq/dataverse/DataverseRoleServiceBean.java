package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.authorization.RoleAssignmentSet;
import edu.harvard.iq.dataverse.search.IndexAsync;
import edu.harvard.iq.dataverse.search.IndexResponse;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.search.SolrIndexServiceBean;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

/**
 *
 * @author michael
 */
@Stateless
@Named
public class DataverseRoleServiceBean implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DataverseRoleServiceBean.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @EJB
    RoleAssigneeServiceBean roleAssigneeService;

    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    IndexServiceBean indexService;
    @EJB
    SolrIndexServiceBean solrIndexService;
    @EJB
    PermissionServiceBean permissionService;
    @EJB
    IndexAsync indexAsync;

    public DataverseRole save(DataverseRole aRole) {
        if (aRole.getId() == null) { // persist a new Role
            em.persist(aRole);
        } else { // update an existing Role
            aRole = em.merge(aRole);
        }

        DvObject owner = aRole.getOwner();
        if(owner == null) { // Builtin Role
            owner = dataverseService.findByAlias("root");
        }

        if(owner != null) { // owner may be null if a role is created before the root collection as in setup-all.sh
            IndexResponse indexDefinitionPointResult = indexDefinitionPoint(owner);
            logger.info("Indexing result: " + indexDefinitionPointResult);
        }

        return aRole;
    }

    public RoleAssignment save(RoleAssignment assignment) {
        return save(assignment, true);
    }
    
    public RoleAssignment save(RoleAssignment assignment, boolean createIndex) {
        if (assignment.getId() == null) {
            em.persist(assignment);
        } else {
            assignment = em.merge(assignment);
        }
        /**
         * @todo update permissionModificationTime here.
         */
        if ( createIndex ) {
            indexAsync.indexRole(assignment);
        }
        return assignment;
    }

    private IndexResponse indexDefinitionPoint(DvObject definitionPoint) {
        /**
         * @todo Do something with the index response. Was Solr down? Is
         * everything ok?
         */
        IndexResponse indexResponse = solrIndexService.indexPermissionsOnSelfAndChildren(definitionPoint);
        return indexResponse;
    }

    public DataverseRole find(Long id) {
        return em.find(DataverseRole.class, id);
    }

    public List<DataverseRole> findAll() {
        return em.createNamedQuery("DataverseRole.listAll", DataverseRole.class).getResultList();
    }

    public void delete(Long id) {
        em.createNamedQuery("DataverseRole.deleteById", DataverseRole.class)
            .setParameter("id", id)
            .executeUpdate();
    }

    public List<DataverseRole> findByOwnerId(Long ownerId) {
        return em.createNamedQuery("DataverseRole.findByOwnerId", DataverseRole.class)
            .setParameter("ownerId", ownerId)
            .getResultList();
    }

    public List<DataverseRole> findBuiltinRoles() {
        return em.createNamedQuery("DataverseRole.findBuiltinRoles", DataverseRole.class)
            .getResultList();
    }

    public DataverseRole findBuiltinRoleByAlias(String alias) {
        return em.createNamedQuery("DataverseRole.findBuiltinRoleByAlias", DataverseRole.class)
            .setParameter("alias", alias)
            .getSingleResult();
    }
    
    public DataverseRole findCustomRoleByAliasAndOwner(String alias, Long ownerId) {
        return em.createNamedQuery("DataverseRole.findCustomRoleByAliasAndOwner", DataverseRole.class)
            .setParameter("alias", alias)
            .setParameter("ownerId", ownerId)
            .getSingleResult();
    }

    public void revoke(Set<DataverseRole> roles, RoleAssignee assignee, DvObject defPoint) {
        for (DataverseRole role : roles) {
            em.createNamedQuery("RoleAssignment.deleteByAssigneeIdentifier_RoleIdDefinition_PointId")
                .setParameter("assigneeIdentifier", assignee.getIdentifier())
                .setParameter("roleId", role.getId())
                .setParameter("definitionPointId", defPoint.getId())
                .executeUpdate();
            em.refresh(role);
        }
        em.refresh(assignee);
    }

    public void revoke(RoleAssignment ra) {
        if (!em.contains(ra)) {
            ra = em.merge(ra);
        }
        em.remove(ra);
        /**
         * @todo update permissionModificationTime here.
         */
        indexAsync.indexRole(ra);
    }

    // "nuclear" remove-all roles for a user or group: 
    // (Note that all the "definition points" - i.e., the dvObjects
    // on which the roles were assigned - need to be reindexed for permissions
    // once the role assignments are removed!
    public void revokeAll(RoleAssignee assignee) {
        Set<DvObject> reindexSet = new HashSet<>();

        for (RoleAssignment ra : roleAssigneeService.getAssignmentsFor(assignee.getIdentifier())) {
            if (!em.contains(ra)) {
                ra = em.merge(ra);
            }
            em.remove(ra);

            reindexSet.add(ra.getDefinitionPoint());
        }

        indexAsync.indexRoles(reindexSet);
    }

    public RoleAssignmentSet roleAssignments(User user, Dataverse dv) {
        RoleAssignmentSet retVal = new RoleAssignmentSet(user);
        while (dv != null) {
            retVal.add(directRoleAssignments(user, dv));
            if (dv.isPermissionRoot()) {
                break;
            }
            dv = dv.getOwner();
        }
        return retVal;
    }

    public List<RoleAssignment> roleAssignments(Long roleId) {
        return em.createNamedQuery("RoleAssignment.listByRoleId", RoleAssignment.class)
            .setParameter("roleId", roleId)
            .getResultList();
    }

    public RoleAssignmentSet assignmentsFor(final User u, final DvObject d) {
        return d.accept(new DvObject.Visitor<RoleAssignmentSet>() {

            @Override
            public RoleAssignmentSet visit(Dataverse dv) {
                return roleAssignments(u, dv);
            }

            @Override
            public RoleAssignmentSet visit(Dataset ds) {
                RoleAssignmentSet asgn = ds.getOwner().accept(this);
                asgn.add(directRoleAssignments(u, ds));
                return asgn;
            }

            @Override
            public RoleAssignmentSet visit(DataFile df) {
                RoleAssignmentSet asgn = df.getOwner().accept(this);
                asgn.add(directRoleAssignments(u, df));
                return asgn;
            }
        });
    }

    public Set<RoleAssignment> rolesAssignments(DvObject dv) {
        Set<RoleAssignment> ras = new HashSet<>();
        while (!dv.isEffectivelyPermissionRoot()) {
            ras.addAll(em.createNamedQuery("RoleAssignment.listByDefinitionPointId", RoleAssignment.class)
                .setParameter("definitionPointId", dv.getId()).getResultList());
            dv = dv.getOwner();
        }

        ras.addAll(em.createNamedQuery("RoleAssignment.listByDefinitionPointId", RoleAssignment.class)
            .setParameter("definitionPointId", dv.getId()).getResultList());

        return ras;
    }
    
    /**
     * Retrieves the roles assignments for {@code user}, directly on {@code dv}.
     * No traversal on the containment hierarchy is done.
     *
     * @param roas the user whose roles are given
     * @param dvo the object where the roles are defined.
     * @return Set of roles defined for the user in the given dataverse.
     * @see #roleAssignments(edu.harvard.iq.dataverse.DataverseUser,
     * edu.harvard.iq.dataverse.Dataverse)
     */
    //public List<RoleAssignment> directRoleAssignments(@NotNull RoleAssignee roas, @NotNull DvObject dvo) {
    public List<RoleAssignment> directRoleAssignments(RoleAssignee roas, DvObject dvo) {
        return em.createNamedQuery("RoleAssignment.listByAssigneeIdentifier_DefinitionPointId", RoleAssignment.class)
            .setParameter("assigneeIdentifier", roas.getIdentifier())
            .setParameter("definitionPointId", dvo.getId())
            .getResultList();
    }
    
    /**
     * Retrieves the roles assignments for {@code user}, directly on {@code dv}.
     * No traversal on the containment hierarchy is done.
     *
     * @param roleAssignees the user whose roles are given
     * @param dvos the objects where the roles are defined.
     * @return Set of roles defined for the user in the given dataverse.
     * @see #roleAssignments(edu.harvard.iq.dataverse.DataverseUser,
     * edu.harvard.iq.dataverse.Dataverse)
     */
    //public List<RoleAssignment> directRoleAssignments(@NotNull Set<? extends RoleAssignee> roleAssignees, @NotNull Collection<DvObject> dvos) {
    public List<RoleAssignment> directRoleAssignments(Set<? extends RoleAssignee> roleAssignees, Collection<DvObject> dvos) {
        if (dvos.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> raIds = roleAssignees.stream().map(roas -> roas.getIdentifier()).collect(Collectors.toList());
        List<Long> dvoIds = dvos.stream().filter(dvo -> !(dvo.getId() == null)).map(dvo -> dvo.getId()).collect(Collectors.toList());
        
        return em.createNamedQuery("RoleAssignment.listByAssigneeIdentifiers", RoleAssignment.class)
                        .setParameter("assigneeIdentifiers", raIds)
                        .setParameter("definitionPointIds", dvoIds)
                        .getResultList();
    }

    /**
     * Retrieves the roles assignments for {@code user}, directly on {@code dv}.
     * No traversal on the containment hierarchy is done.
     *
     * @param dvo the object where the roles are defined.
     * @return Set of roles defined for the user in the given dataverse.
     * @see #roleAssignments(edu.harvard.iq.dataverse.DataverseUser,
     * edu.harvard.iq.dataverse.Dataverse)
     */
    public List<RoleAssignment> directRoleAssignments(DvObject dvo) {
        TypedQuery<RoleAssignment> query = em.createNamedQuery(
            "RoleAssignment.listByDefinitionPointId",
            RoleAssignment.class);
        query.setParameter("definitionPointId", dvo.getId());
        return query.getResultList();
    }

    /**
     * Get all the available roles in a given dataverse.
     *
     * @param dvId The id of dataverse whose available roles we query
     * @return Set of available roles
     */
    public Set<DataverseRole> availableRoles(Long dvId) {
        Dataverse dv = em.find(Dataverse.class, dvId);
        Set<DataverseRole> roles = dv.getRoles();
        roles.addAll(findBuiltinRoles());

        while (dv.getOwner() != null) {
            dv = dv.getOwner();
            roles.addAll(dv.getRoles());
        }

        return roles;
    }

    /**
     * Get all the available roles for a given Dataset, DataFile or Dataverse.
     * This excludes roles that are not relevant to the given DvObject type (e.g. for Datasets, this excludes roles that
     * only have Dataverse-level permissions).
     * Currently, the available roles for Datasets and DataFiles are gotten from the collection they are in.
     *
     * @param dvo The Dataset, DataFile or Dataverse whose available roles we query
     * @return Set of available roles
     */
    public Set<DataverseRole> availableRoles(DvObject dvo) {
        Set<DataverseRole> roles = new HashSet<>();

        // Get roles available for given DvObject
        if (dvo instanceof Dataverse) {
            roles = availableRoles(dvo.getId());

        } else if (dvo instanceof Dataset) {
            roles = availableRoles(dvo.getOwner().getId()).stream()
                    .filter(role -> role.permissions().stream()
                            .anyMatch(p -> p.appliesTo(Dataset.class)
                                    || p.appliesTo(DataFile.class)))
                    .collect(Collectors.toSet());

        } else if (dvo instanceof DataFile) {
            roles = availableRoles(dvo.getOwner().getOwner().getId()).stream()
                    .filter(role -> role.permissions().stream()
                            .anyMatch(p -> p.appliesTo(DataFile.class)))
                    .collect(Collectors.toSet());
        }

        return roles;
    }

    /**
     * Get all the available roles for a given Dataset, DataFile or Dataverse that can be assigned by a given User.
     * This excludes roles that are not relevant to the given DvObject type (e.g. for Datasets, this excludes roles that
     * only have Dataverse-level permissions).
     * Currently, the available roles for Datasets and DataFiles are gotten from the collection they are in.
     *
     * @param dvo The Dataset, DataFile or Dataverse whose available roles we query
     * @param user The user whose available roles we query
     * @return Set of available roles
     */
    public Set<DataverseRole> availableRoles(DvObject dvo, User user) {
        Set<DataverseRole> roles = availableRoles(dvo);

        // Filter roles assignable by given user
        Set<Permission> granted = permissionService.permissionsFor(user, dvo);
        roles = roles.stream()
                 .filter(role -> granted.containsAll(role.permissions()))
                 .collect(Collectors.toSet());

        return roles;
    }

    public List<DataverseRole> getDataverseRolesByPermission(Permission permissionIn, Long ownerId) {
        /*
         For a given permission and dataverse Id get all of the roles (built-in or owned by the dataverse)            
         that contain that permission
         */
        List<DataverseRole> rolesToCheck = findBuiltinRoles();
        List<DataverseRole> retVal = new ArrayList<>();
        rolesToCheck.addAll(findByOwnerId(ownerId));
        for (DataverseRole role : rolesToCheck) {
            if (role.permissions().contains(permissionIn)) {
                retVal.add(role);
            }
        }
        return retVal;
    }
}
