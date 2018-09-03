package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.GroupUtil;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.Command;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.PermissionException;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toList;
import javax.persistence.Query;
import javax.validation.constraints.NotNull;

/**
 * Your one-stop-shop for deciding which user can do what action on which
 * objects (TM). Note that this bean accesses the permissions/user assignment on
 * a read-only basis. Changing the permissions a user has is done via roles and
 * groups, over at {@link DataverseRoleServiceBean}.
 *
 * @author michael
 */
@Stateless
@Named
public class PermissionServiceBean {

    private static final Logger logger = Logger.getLogger(PermissionServiceBean.class.getName());

    /**
     * A set of permissions that con only be granted to {@link AuthentictedUser}s.
     */
    private static final Set<Permission> PERMISSIONS_FOR_AUTHENTICATED_USERS_ONLY = 
        EnumSet.copyOf(Arrays.asList(Permission.values()).stream()
                    .filter(Permission::requiresAuthenticatedUser)
                    .collect(Collectors.toSet()));

    @EJB
    BuiltinUserServiceBean userService;

    @EJB
    AuthenticationServiceBean authenticationService;

    @EJB
    DataverseRoleServiceBean roleService;

    @EJB
    RoleAssigneeServiceBean roleAssigneeService;

    @EJB
    DataverseServiceBean dataverseService;
    
    @EJB
    DvObjectServiceBean dvObjectServiceBean;

    @PersistenceContext
    EntityManager em;

    @EJB
    GroupServiceBean groupService;

    @Inject
    DataverseSession session;

    @Inject
    DataverseRequestServiceBean dvRequestService;
    
    
    /**
     * This interface is used locally by {@link EjbDataverseEngine} beans to
     * create detailed reports on which permissions are missing. Probably not
     * useful beyond that area.
     */
    interface InsufficientPermissionCallback {
        void insufficientPermissions( DvObject dvo, Set<Permission> required, Set<Permission> granted);
    }
    
    /**
     * A request-level permission query (e.g includes IP groups). These queries
     * form a DSL* for querying permissions, in a way that's hopefully more
     * pleasant than passing a lot of parameters to a large method.
     * 
     * Do not instantiate a query yourself. Rather, use the query-returning methods
     * of this bean.
     * 
     * The example below takes the current request, and checks whether it has 
     * permission {@code p} over {@link DvObject} {@code dv}:
     * <code>
     * if ( bean.on(dvObject).has(p) ) {
     *  ...do the thing you must have p in order to do
     * }
     * </code>
     * 
     * In case you need to create your own request:
     * 
     * <code>
     * if ( bean.request(req).on(dvObject).has(p) ) {
     *  ...do the thing you must have p in order to do
     * }
     * </code>
     * 
     * * Domain Specific Language. 
     */
    public class RequestPermissionQuery {

        final DvObject subject;
        final DataverseRequest request;

        private RequestPermissionQuery(DvObject subject, DataverseRequest request) {
            this.subject = subject;
            this.request = request;
        }
        
        /**
         * Executes the query returning the require permissions.
         * @return permission required to run the specified command over the object.
         */
        public Set<Permission> get() {
            return permissionsFor(request, subject);
        }

        /**
         * Tests whether the request has permission {@code p} over the DvObject.
         * @param p The permission tested for.
         * @return {@code true} iff {@link #request} has permission {@code p}
         *           over object {@link #subject}.
         */
        public boolean has(Permission p) {
            return hasPermissionsFor(request, subject, EnumSet.of(p));
        }
        
        /**
         * Tests whether the request has all permissions {@code ps} over the DvObject.
         * @param ps The permissions tested for.
         * @return {@code true} iff {@link #request} has all the permissions
         *           in  {@code ps} over object {@link #subject}.
         */
        public boolean has(Set<Permission> ps) {
            if (ps.isEmpty()) {
                return true;
            }
            return hasPermissionsFor(request, subject, ps);
        }

        public RequestPermissionQuery on(DvObject dvo) {
            if (dvo == null) {
                throw new IllegalArgumentException("Cannot query permissions on a null DvObject");
            }
            if (dvo.getId() == null) {
                throw new IllegalArgumentException("Cannot query permissions on a DvObject with a null id.");
            }
            return new RequestPermissionQuery(dvo, request);
        }
       
    }
    
    /**
     * Start querying permissions for {@link DvObject} {@code d}. 
     * @param d the object on which the permissions will be queried
     * @return A permission query
     */
    public RequestPermissionQuery on( @NotNull DvObject d) {
        if (d == null) {
            throw new IllegalArgumentException("Cannot query permissions on a null DvObject");
        }
        if (d.getId() == null) {
            throw new IllegalArgumentException("Cannot query permissions on a DvObject with a null id.");
        }
        return new RequestPermissionQuery(d, null);
    }

    /**
     * Start querying permissions for {@link DataverseRequest} {@code req}. 
     * @param req the {@link DataverseRequest} whose permissions will be queried
     * @return A permission query
     */
    public RequestPermissionQuery request(DataverseRequest req) {
        return new RequestPermissionQuery(null, req);
    }
    
    /**
     * Checks whether a command has enough permissions to run.
     * 
     * Note that the command may fail due to other reasons, such as internal 
     * errors or making no sense (see {@link IllegalCommandException}).
     * 
     * @param aCommand
     * @return {@code true} iff submitting the command will not cause a {@link PermissionException} to be thrown.
     */
    public boolean isPermitted( Command aCommand ) {
        return isPermitted(aCommand, null);
    }
    
    /**
     * Internal call; checks permissions, allows a callback for fine-grained
     * response if case there are not enough permissions.
     * 
     * @param aCommand The command to be tested.
     * @param ipc Callback, called if there are insufficient permissions for the passed command.
     * @return {@code true} iff the command will not be blocked due to permission issues.
     */
    boolean isPermitted(Command aCommand, InsufficientPermissionCallback ipc) {
        Map<String, ? extends Set<Permission>> requiredMap = aCommand.getRequiredPermissions();
        if (requiredMap == null) {
            throw new IllegalArgumentException("Command " + aCommand + " does not define required permissions.");
        }

        DataverseRequest dvReq = aCommand.getRequest();

        Map<String, DvObject> affectedDvObjects = aCommand.getAffectedDvObjects();
        
        for (Map.Entry<String, ? extends Set<Permission>> pair : requiredMap.entrySet()) {
            String dvName = pair.getKey();
            if (!affectedDvObjects.containsKey(dvName)) {
                throw new IllegalArgumentException("Command instance " + aCommand + " does not have a DvObject named '" + dvName + "'");
            }
            DvObject dvo = affectedDvObjects.get(dvName);

            Set<Permission> granted = (dvo != null) ? permissionsFor(dvReq, dvo) : EnumSet.allOf(Permission.class);
            Set<Permission> required = requiredMap.get(dvName);

            if (!granted.containsAll(required)) {
                if ( ipc != null ) {
                    ipc.insufficientPermissions(dvo, required, granted);
                }
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Lists all the direct assignments on object {@code d}. Not that containment hierarchies
     * are not traversed here; assignments on {@link DvObject#getOwner()} are not
     * tested for.
     * 
     * @param d the object whose assignments are queried.
     * @return All direct assignments on {@code d}.
     */
    public List<RoleAssignment> assignmentsOn(DvObject d) {
        return em.createNamedQuery("RoleAssignment.listByDefinitionPointId", RoleAssignment.class)
                .setParameter("definitionPointId", d.getId()).getResultList();
    }

    /**
     * Returns all the children (direct descendants) of {@code dvo}, on which the user 
     * has all the permissions specified in {@code permissions}. This method takes into
     * account which permissions apply for which object type, so a permission that 
     * applies only to {@link Dataset}s will not be considered when looking into
     * the question of whether a {@link Dataverse} should be contained in the output list.
     * @param req The request whose permissions are queried
     * @param dvo The objects whose children we list
     * @param required (sub)set of permissions {@code req} has on the objects in the returned list
     * @return list of {@code dvo} children over which {@code req} has at least {@code required} permissions.
     */
    public List<DvObject> whichChildrenHasPermissionsFor(DataverseRequest req, DvObjectContainer dvo, Set<Permission> required) {
        List<DvObject> children = dvObjectServiceBean.findByOwnerId(dvo.getId());
        User user = req.getUser();
        
        // quick cases
        if (user.isSuperuser()) {
            return children; // it's good to be king
            
        } else if (!user.isAuthenticated()) {
            if ( required.stream().anyMatch(PERMISSIONS_FOR_AUTHENTICATED_USERS_ONLY::contains) ){
                // At least one of the permissions requires that the user is authenticated, which is not the case.
                return Collections.emptyList();
            }
        }
              
        // Actually look at permissions
        Set<DvObject> parents = getPermissionAncestors(dvo);
        Set<? extends RoleAssignee> groups = groupService.groupsFor(req);
        List<RoleAssignment> parentsAsignments = roleService.directRoleAssignments(groups, parents);
        
        for (RoleAssignment asmnt : parentsAsignments) {
            required.removeAll(asmnt.getRole().permissions());
        }
        if (required.isEmpty()) {
            // All permissions are met by role assignments on the request
            return children;
        }
        
        // Looking at each child at a time now.
        // 1. Map childs to permissions
        List<RoleAssignment> childrenAssignments = roleService.directRoleAssignments(groups, children);
        Map<DvObject, Set<Permission>> roleMap = new HashMap<>();
        childrenAssignments.forEach( assignment -> {
            DvObject definitionPoint = assignment.getDefinitionPoint();
            if (!roleMap.containsKey(definitionPoint)){
                roleMap.put(definitionPoint, assignment.getRole().permissions());
            } else {
                roleMap.get(definitionPoint).addAll(assignment.getRole().permissions());
            }
        });
        
        // 2. Filter by permission map created at (1).
        return children.stream().filter( child -> 
            (roleMap.containsKey(child)) &&
            (roleMap.get(child).containsAll(required.stream().filter(perm -> perm.appliesTo(child.getClass())).collect(Collectors.toSet())))
        ).collect( toList() );
        
    }

    /**
     * Checks whether a request has a set of permissions over a {@link DvObject}.
     * 
     * @param req The request tested
     * @param dvo The object tested
     * @param required The required permissions
     * @return if the permissions {@code req} has over {@code dvo} is a subset of {@code required}.
     */
    public boolean hasPermissionsFor(DataverseRequest req, DvObject dvo, Set<Permission> required) {
        if ( required.isEmpty() ) {
            return true;
        }
        
        User user = req.getUser();
        if (user.isSuperuser()) {
            return true;
        } else if (!user.isAuthenticated()) {
            Set<Permission> requiredCopy = EnumSet.copyOf(required);
            requiredCopy.retainAll(PERMISSIONS_FOR_AUTHENTICATED_USERS_ONLY);
            if (!requiredCopy.isEmpty()) {
                return false;
            }
        }
        
        Set<RoleAssignee> ras = new HashSet<>(groupService.groupsFor(req, dvo));
        ras.add(user);
        return hasGroupPermissionsFor(ras, dvo, required);
    }

    /**
     * Finds all the permissions the {@link User} in {@code req} has over
     * {@code dvo}, in the context of {@code req}.
     *
     * @param req
     * @param dvo
     * @return Permissions of {@code req.getUser()} over {@code dvo}.
     */
    public Set<Permission> permissionsFor(DataverseRequest req, DvObject dvo) {
        if (req.getUser().isSuperuser()) {
            return EnumSet.allOf(Permission.class);
        }

        Set<Permission> permissions = getInferredPermissions(dvo);

        // Add permissions gained from groups
        Set<RoleAssignee> ras = new HashSet<>(groupService.groupsFor(req, dvo));
        ras.add(req.getUser());
        addAllPermissions(ras, dvo, permissions);

        if (!req.getUser().isAuthenticated()) {
            permissions.removeAll(PERMISSIONS_FOR_AUTHENTICATED_USERS_ONLY);
        }
        return permissions;
    }

    /**
     * Returns all the role assignments that are effective for {@code ra} over
     * {@code d}. Traverses the containment hierarchy of the {@code d}.
     *
     * @param ra The role assignee whose role assignemnts we look for.
     * @param d The dataverse object over which the roles are assigned
     * @return A set of all the role assignments for {@code ra} over {@code d}.
     */
    public Set<RoleAssignment> assignmentsFor(RoleAssignee ra, DvObject d) {
        return assignmentsFor(Collections.singleton(ra), d);
    }

    public Set<RoleAssignment> assignmentsFor(Set<RoleAssignee> ras, DvObject d) {
        Set<DvObject> permAncestors = getPermissionAncestors(d);
        return new HashSet<>(roleService.directRoleAssignments(ras, permAncestors));
    }

    /**
     * Gets all the ancestors of {@code d} where role assignments might affect
     * the permissions at {@code d}.
     * @param d
     * @return Ancestors of {@code d} up to the nearest permission root.
     * @see DvObjectContainer#isPermissionRoot()
     * @see DvObjectContainer#isEffectivelyPermissionRoot()
     */
    public Set<DvObject> getPermissionAncestors(DvObject d) {
        Set<DvObject> ancestors = new HashSet<>();
        while (d != null) {
            ancestors.add(d);
            if (d instanceof Dataverse && ((Dataverse) d).isEffectivelyPermissionRoot()) {
                return ancestors;
            } else {
                d = d.getOwner();
            }
        }
        return ancestors;
    }

    /**
     * Go from (User, Permission) to a list of Dataverse objects that the user
     * has the permission on.
     *
     * Note: for performance reasons:
     * <ul>
     *  <li>Ignores {@link IpGroup}s, which are not useful in the current use cases</li>
     *  <li>Ignores containment hierarchy, which does not matter at the moment since
     *       all dataverses are permission roots.</li>
     * </ul>
     * 
     * @param user
     * @param permission
     * @return The list of dataverses {@code user} has permission {@code permission} on.
     */
    public List<Dataverse> getDataversesUserHasPermissionOn(AuthenticatedUser user, Permission permission) {
        DataverseRequest fauxRequest = new DataverseRequest(user, (IpAddress)null);
        Set<Group> groups = groupService.groupsFor(fauxRequest);
        String identifiers = GroupUtil.getAllIdentifiersForUser(user, groups);
        /**
         * @todo Are there any strings in identifiers that would break this SQL
         * query?
         */
        String query = "SELECT id FROM dvobject WHERE dtype = 'Dataverse' and id in (select definitionpoint_id from roleassignment where assigneeidentifier in (" + identifiers + "));";
        logger.log(Level.FINE, "query: {0}", query);
        Query nativeQuery = em.createNativeQuery(query);
        List<Integer> dataverseIdsToCheck = nativeQuery.getResultList();
        List<Dataverse> dataversesUserHasPermissionOn = new LinkedList<>();
        for (int dvIdAsInt : dataverseIdsToCheck) {
            Dataverse dataverse = dataverseService.find(Long.valueOf(dvIdAsInt));
            if (request(fauxRequest).on(dataverse).has(permission)) {
                dataversesUserHasPermissionOn.add(dataverse);
            }
        }
        return dataversesUserHasPermissionOn;
    }

    public List<AuthenticatedUser> getUsersWithPermissionOn(Permission permission, DvObject dvo) {
        List<AuthenticatedUser> usersHasPermissionOn = new LinkedList<>();
        Set<RoleAssignment> ras = roleService.rolesAssignments(dvo);
        for (RoleAssignment ra : ras) {
            if (ra.getRole().permissions().contains(permission)) {
                RoleAssignee raee = roleAssigneeService.getRoleAssignee(ra.getAssigneeIdentifier());
                usersHasPermissionOn.addAll(roleAssigneeService.getExplicitUsers(raee));
            }
        }

        return usersHasPermissionOn;
    }

    public Map<String, AuthenticatedUser> getDistinctUsersWithPermissionOn(Permission permission, DvObject dvo) {

        List<AuthenticatedUser> users = getUsersWithPermissionOn(permission, dvo);
        Map<String, AuthenticatedUser> distinctUsers = new HashMap<>();
        users.forEach((au) -> {
            distinctUsers.put(au.getIdentifier(), au);
        });

        return distinctUsers;
    }

    public List<Long> getDvObjectIdsUserHasRoleOn(User user, List<DataverseRole> roles, List<String> types, boolean indirect) {

        String roleString = getRolesClause(roles);
        String typeString = getTypesClause(types);

        Query nativeQuery = em.createNativeQuery("SELECT id FROM dvobject WHERE "
                + typeString + " id in (select definitionpoint_id from roleassignment where assigneeidentifier in ('" + user.getIdentifier() + "') "
                + roleString + ");");
        List<Integer> dataverseIdsToCheck = nativeQuery.getResultList();
        List<Long> dataversesUserHasPermissionOn = new LinkedList<>();
        String indirectParentIds = "";
        Boolean indirectFirst = true;
        for (int dvIdAsInt : dataverseIdsToCheck) {
            dataversesUserHasPermissionOn.add(Long.valueOf(dvIdAsInt));
            if (indirect) {
                if (indirectFirst) {
                    indirectParentIds = "(" + Integer.toString(dvIdAsInt);
                    indirectFirst = false;
                } else {
                    indirectParentIds += ", " + Integer.toString(dvIdAsInt);
                }
            }
        }

        // Get child datasets and files
        if (indirect) {
            indirectParentIds += ") ";
            Query nativeQueryIndirect = em.createNativeQuery("SELECT id FROM dvobject WHERE "
                    + " owner_id in " + indirectParentIds + " and dType = 'Dataset'; ");

            List<Integer> childDatasetIds = nativeQueryIndirect.getResultList();

            String indirectDatasetParentIds = "";
            Boolean indirectFileFirst = true;
            for (int dvIdAsInt : childDatasetIds) {
                dataversesUserHasPermissionOn.add(Long.valueOf(dvIdAsInt));
                if (indirect) {
                    if (indirectFileFirst) {
                        indirectDatasetParentIds = "(" + Integer.toString(dvIdAsInt);
                        indirectFileFirst = false;
                    } else {
                        indirectDatasetParentIds += ", " + Integer.toString(dvIdAsInt);
                    }
                }
            }
            Query nativeQueryFileIndirect = em.createNativeQuery("SELECT id FROM dvobject WHERE "
                    + " owner_id in " + indirectDatasetParentIds + " and dType = 'DataFile'; ");

            List<Integer> childFileIds = nativeQueryFileIndirect.getResultList();

            for (int dvIdAsInt : childFileIds) {
                dataversesUserHasPermissionOn.add(Long.valueOf(dvIdAsInt));
            }
        }
        return dataversesUserHasPermissionOn;
    }

    public void checkEditDatasetLock(Dataset dataset, DataverseRequest dataverseRequest, Command command) throws IllegalCommandException {
        if (dataset.isLocked()) {
            if (dataset.isLockedFor(DatasetLock.Reason.InReview)) {
                // The "InReview" lock is not really a lock for curators. They can still make edits.
                if (!isPermitted(new PublishDatasetCommand(dataset, dataverseRequest, true))) {
                    throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.message.locked.editNotAllowedInReview"), command);
                }
            }
            if (dataset.isLockedFor(DatasetLock.Reason.Ingest)) {
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.message.locked.editNotAllowed"), command);
            }
            if (dataset.isLockedFor(DatasetLock.Reason.pidRegister)) {
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.message.locked.editNotAllowed"), command);
            }
            // TODO: Do we need to check for "Workflow"? Should the message be more specific?
            if (dataset.isLockedFor(DatasetLock.Reason.Workflow)) {
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.message.locked.editNotAllowed"), command);
            }
            // TODO: Do we need to check for "DcmUpload"? Should the message be more specific?
            if (dataset.isLockedFor(DatasetLock.Reason.DcmUpload)) {
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.message.locked.editNotAllowed"), command);
            }
        }
    }

    public void checkDownloadFileLock(Dataset dataset, DataverseRequest dataverseRequest, Command command) throws IllegalCommandException {
        if (dataset.isLocked()) {
            if (dataset.isLockedFor(DatasetLock.Reason.InReview)) {
                // The "InReview" lock is not really a lock for curators or contributors. They can still download.                
                if (!isPermitted(new UpdateDatasetVersionCommand(dataset, dataverseRequest))) {
                    throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.message.locked.downloadNotAllowedInReview"), command);
                }
            }
            if (dataset.isLockedFor(DatasetLock.Reason.Ingest)) {
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.message.locked.downloadNotAllowed"), command);
            }
            if (dataset.isLockedFor(DatasetLock.Reason.pidRegister)) {
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.message.locked.downloadNotAllowed"), command);
            }
            // TODO: Do we need to check for "Workflow"? Should the message be more specific?
            if (dataset.isLockedFor(DatasetLock.Reason.Workflow)) {
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.message.locked.downloadNotAllowed"), command);
            }
            // TODO: Do we need to check for "DcmUpload"? Should the message be more specific?
            if (dataset.isLockedFor(DatasetLock.Reason.DcmUpload)) {
                throw new IllegalCommandException(BundleUtil.getStringFromBundle("dataset.message.locked.downloadNotAllowed"), command);
            }
        }
    }
    
    
    private void addAllPermissions(Set<RoleAssignee> ras, DvObject dvo, Set<Permission> permissions) {
        for (RoleAssignment asmnt : assignmentsFor(ras, dvo)) {
            permissions.addAll(asmnt.getRole().permissions());
        }
    }


    /**
     * Calculates permissions based on object state and other context
     *
     * @param dvo
     * @return
     */
    private Set<Permission> getInferredPermissions(DvObject dvo) {

        Set<Permission> permissions = EnumSet.noneOf(Permission.class);

        if (isPublicallyDownloadable(dvo)) {
            permissions.add(Permission.DownloadFile);
        }

        return permissions;
    }

    /**
     * unrestricted files that are part of a release dataset automatically get
     * download permission for everybody:
     */
    private boolean isPublicallyDownloadable(DvObject dvo) {
        if (dvo instanceof DataFile) {
            // unrestricted files that are part of a release dataset 
            // automatically get download permission for everybody:
            //      -- L.A. 4.0 beta12

            DataFile df = (DataFile) dvo;

            if (!df.isRestricted()) {
                if (df.getOwner().getReleasedVersion() != null) {
                    if (df.getOwner().getReleasedVersion().getFileMetadatas() != null) {
                        for (FileMetadata fm : df.getOwner().getReleasedVersion().getFileMetadatas()) {
                            if (df.equals(fm.getDataFile())) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Method takes in a user and optional list of roles and dvobject type
     * queries the role assignment table filtering by optional roles and dv
     * returns dvobject ids
     */
    private String getRolesClause(List<DataverseRole> roles) {
        StringBuilder roleStringBld = new StringBuilder();
        if (roles != null && !roles.isEmpty()) {
            roleStringBld.append(" and role_id in (");
            boolean first = true;
            for (DataverseRole role : roles) {
                if (!first) {
                    roleStringBld.append(",");
                }
                roleStringBld.append(role.getId());
                first = false;
            }
            roleStringBld.append(")");
        }
        return roleStringBld.toString();
    }

    private String getTypesClause(List<String> types) {
        boolean firstType = true;
        StringBuilder typeStringBld = new StringBuilder();
        if (types != null && !types.isEmpty()) {
            typeStringBld.append(" dtype in (");
            for (String type : types) {
                if (!firstType) {
                    typeStringBld.append(",");
                }
                typeStringBld.append("'").append(type).append("'");
            }
            typeStringBld.append(") and ");
        }
        return typeStringBld.toString();
    }
        
    private boolean hasGroupPermissionsFor(Set<RoleAssignee> ras, DvObject dvo, Set<Permission> required) {
        for (RoleAssignment asmnt : assignmentsFor(ras, dvo)) {
            required.removeAll(asmnt.getRole().permissions());
        }
        return required.isEmpty();
    }
    
}
