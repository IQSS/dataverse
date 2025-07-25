package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.DataverseRoleServiceBean.RoleAssignmentHistoryConsolidatedEntry;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.authorization.RoleAssignmentSet;
import edu.harvard.iq.dataverse.search.IndexAsync;
import edu.harvard.iq.dataverse.search.IndexResponse;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.search.SolrIndexServiceBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import edu.harvard.iq.dataverse.settings.FeatureFlags;

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

    public RoleAssignment save(RoleAssignment assignment, DataverseRequest req) {
        return save(assignment, true, req);
    }
    
    public RoleAssignment save(RoleAssignment assignment, boolean createIndex, DataverseRequest req) {
        if (assignment.getId() == null) {
            em.persist(assignment);
            em.flush(); // Force synchronization with the database
        } else {
            assignment = em.merge(assignment);
        }
        
        if (createIndex) {
            indexAsync.indexRole(assignment);
        }
        
        // Check if ROLE_ASSIGNMENT_HISTORY feature flag is enabled
        if (FeatureFlags.ROLE_ASSIGNMENT_HISTORY.enabled()) {
            RoleAssignmentHistory entry = new RoleAssignmentHistory(assignment, req, RoleAssignmentHistory.ActionType.ASSIGN);
            saveHistoryEntry(entry);
        }

        return assignment;
    }
    

    /**
     * Saves a RoleAssignmentHistory entry to the database.
     * 
     * @param entry The RoleAssignmentHistory object to be saved
     * @return The persisted RoleAssignmentHistory object
     */
    private RoleAssignmentHistory saveHistoryEntry(RoleAssignmentHistory entry) {
        if (entry.getEntryId() == null) {
            em.persist(entry);
            em.flush(); // Ensure the entity is persisted immediately
        } else {
            entry = em.merge(entry);
        }
        return entry;
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

/*    public void revoke(Set<DataverseRole> roles, RoleAssignee assignee, DvObject defPoint) {
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
*/
    public void revoke(RoleAssignment ra, DataverseRequest req) {
        if (!em.contains(ra)) {
            ra = em.merge(ra);
        }
        
        // Create history entry if feature flag is set
        if (FeatureFlags.ROLE_ASSIGNMENT_HISTORY.enabled()) {
            RoleAssignmentHistory entry = new RoleAssignmentHistory(ra, req, RoleAssignmentHistory.ActionType.REVOKE);
            saveHistoryEntry(entry);
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
    public void revokeAll(RoleAssignee assignee, DataverseRequest req) {
        Set<DvObject> reindexSet = new HashSet<>();
    
        for (RoleAssignment ra : roleAssigneeService.getAssignmentsFor(assignee.getIdentifier())) {
            if (!em.contains(ra)) {
                ra = em.merge(ra);
            }
            
            // Create history entry if feature flag is set
            if (FeatureFlags.ROLE_ASSIGNMENT_HISTORY.enabled()) {
                RoleAssignmentHistory entry = new RoleAssignmentHistory(ra, req, RoleAssignmentHistory.ActionType.REVOKE);
                saveHistoryEntry(entry);
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
        List<RoleAssignment> unfiltered = em.createNamedQuery("RoleAssignment.listByAssigneeIdentifier", RoleAssignment.class).
                            setParameter("assigneeIdentifier", roas.getIdentifier())
                            .getResultList();
        return unfiltered.stream().filter(roleAssignment -> Objects.equals(roleAssignment.getDefinitionPoint().getId(), dvo.getId())).collect(Collectors.toList());
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
     * Get all the available roles in a given dataverse, mapped by the dataverse
     * that defines them. Map entries are ordered by reversed hierarchy (root is
     * always last).
     *
     * @param dvId The id of dataverse whose available roles we query
     * @return map of available roles.
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
    
    /**
     * Retrieves role assignment history for a specific definition point
     * 
     * @param definitionPointId The ID of the definition point
     * @return List of role assignment history entries
     */
    public List<RoleAssignmentHistoryConsolidatedEntry> getRoleAssignmentHistory(Long definitionPointId) {
        List<RoleAssignmentHistory> entries = em.createNamedQuery("RoleAssignmentHistory.findByDefinitionPointId", RoleAssignmentHistory.class)
                .setParameter("definitionPointId", definitionPointId)
                .getResultList();
        
        return processRoleAssignmentEntries(entries, false);
    }

    /**
     * Retrieves role assignment history for all files in a dataset
     * 
     * @param datasetId The ID of the dataset
     * @return List of role assignment history entries
     */
    public List<RoleAssignmentHistoryConsolidatedEntry> getFilesRoleAssignmentHistory(Long datasetId) {
        List<RoleAssignmentHistory> entries = em.createNamedQuery("RoleAssignmentHistory.findByOwnerId", RoleAssignmentHistory.class)
                .setParameter("datasetId", datasetId)
                .getResultList();
        
        return processRoleAssignmentEntries(entries, true);
    }
    
    /**
     * Common method to process role assignment history entries and create consolidated history entries
     * 
     * @param entries List of role assignment history records
     * @param combineEntries Whether to combine entries for different files
     * @return List of role assignment history entries
     */
    private List<RoleAssignmentHistoryConsolidatedEntry> processRoleAssignmentEntries(List<RoleAssignmentHistory> entries, boolean combineEntries) {
        List<RoleAssignmentHistoryConsolidatedEntry> roleAssignmentHistory = new ArrayList<>();
        Map<Long, RoleAssignmentHistoryConsolidatedEntry> historyMap = new HashMap<>();

        // First pass: Create consolidatedEntries from history records
        for (RoleAssignmentHistory entry : entries) {
            Long roleAssignmentId = entry.getRoleAssignmentId();
            RoleAssignmentHistoryConsolidatedEntry consolidatedEntry = historyMap.get(roleAssignmentId);

            if (consolidatedEntry == null) {
                consolidatedEntry = new RoleAssignmentHistoryConsolidatedEntry(entry.getAssigneeIdentifier(), entry.getRoleAlias(), entry.getDefinitionPointId());
                historyMap.put(roleAssignmentId, consolidatedEntry);
            }

            if (entry.getActionType() == RoleAssignmentHistory.ActionType.ASSIGN) {
                consolidatedEntry.setAssignedBy(entry.getActionByIdentifier());
                consolidatedEntry.setAssignedAt(entry.getActionTimestamp());
            } else if (entry.getActionType() == RoleAssignmentHistory.ActionType.REVOKE) {
                consolidatedEntry.setRevokedBy(entry.getActionByIdentifier());
                consolidatedEntry.setRevokedAt(entry.getActionTimestamp());
            }
        }
        
        // Second pass: Combine entries with matching criteria if requested
        if (combineEntries) {
            Map<String, RoleAssignmentHistoryConsolidatedEntry> finalHistoryMap = new HashMap<>();
            for (RoleAssignmentHistoryConsolidatedEntry entry : historyMap.values()) {
                String key = entry.getAssigneeIdentifier() + "|" + entry.getRoleName() + "|" +
                        entry.getAssignedBy() + "|" + entry.getAssignedAt() + "|" +
                        entry.getRevokedBy() + "|" + entry.getRevokedAt();

                RoleAssignmentHistoryConsolidatedEntry existingEntry = finalHistoryMap.get(key);
                if (existingEntry == null) {
                    finalHistoryMap.put(key, entry);
                } else {
                    existingEntry.addDefinitionPointId(entry.getDefinitionPointIds().get(0));
                }
            }
            roleAssignmentHistory.addAll(finalHistoryMap.values());
        } else {
            roleAssignmentHistory.addAll(historyMap.values());
        }

        // Sort the entries
        roleAssignmentHistory.sort(Comparator
                .comparing(RoleAssignmentHistoryConsolidatedEntry::getRevokedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(RoleAssignmentHistoryConsolidatedEntry::getAssignedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed());

        return roleAssignmentHistory;
    }

    public static class RoleAssignmentHistoryConsolidatedEntry {
        private String roleName;
        private String assigneeIdentifier;
        private String assignedBy;
        private Date assignedAt;
        private String revokedBy;
        private Date revokedAt;
        private List<Long> definitionPointIds;  // New field
    
        public RoleAssignmentHistoryConsolidatedEntry(String assigneeIdentifier, String roleName, Long definitionPointId) {
            this.roleName = roleName;
            this.assigneeIdentifier = assigneeIdentifier;
            this.definitionPointIds = new ArrayList<Long>();
            definitionPointIds.add(definitionPointId);
        }
    
        public void setRevokedAt(Date actionTimestamp) {
            revokedAt = actionTimestamp;
        }
    
        public void setRevokedBy(String actionByIdentifier) {
            revokedBy = actionByIdentifier;
        }
    
        public void setAssignedAt(Date actionTimestamp) {
            assignedAt = actionTimestamp;
        }
    
        public void setAssignedBy(String actionByIdentifier) {
            assignedBy = actionByIdentifier;
        }
    
        public String getRoleName() {
            return roleName;
        }
    
        public String getAssigneeIdentifier() {
            return assigneeIdentifier;
        }
    
        public String getAssignedBy() {
            return assignedBy;
        }
    
        public Date getAssignedAt() {
            return assignedAt;
        }
    
        public String getRevokedBy() {
            return revokedBy;
        }
    
        public Date getRevokedAt() {
            return revokedAt;
        }
    
        public List<Long> getDefinitionPointIds() {
            return definitionPointIds;
        }
    
        public void addDefinitionPointId(Long definitionPointId) {
            definitionPointIds.add(definitionPointId);
        }
        
        public String getDefinitionPointIdsAsString() {
            return definitionPointIds.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
        }
    }
}
