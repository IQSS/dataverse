package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.GroupUtil;
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
import java.util.HashSet;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import static edu.harvard.iq.dataverse.engine.command.CommandHelper.CH;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.persistence.Query;

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
    
    private static final Set<Permission> PERMISSIONS_FOR_AUTHENTICATED_USERS_ONLY = 
           EnumSet.copyOf(Arrays.asList(Permission.values()).stream()
                    .filter( Permission::requiresAuthenticatedUser )
                    .collect( Collectors.toList() ));
    
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

    @PersistenceContext
    EntityManager em;

    @EJB
    GroupServiceBean groupService;
    
    @Inject
    DataverseSession session;
    
    @Inject
    DataverseRequestServiceBean dvRequestService;
    
    /**
     * A request-level permission query (e.g includes IP groups).
     */
    public class RequestPermissionQuery {
        final DvObject subject;
        final DataverseRequest request;

        public RequestPermissionQuery(DvObject subject, DataverseRequest request) {
            this.subject = subject;
            this.request = request;
        }
        
        public Set<Permission> get() {
            return PermissionServiceBean.this.permissionsFor(request, subject);
        }

        public boolean has(Permission p) {
            return get().contains(p);
        }
        
        public RequestPermissionQuery on( DvObject dvo ) {
            return new RequestPermissionQuery(dvo, request);
        }
        
        /**
         * Tests whether a command of the passed class can be issued over the {@link DvObject}
         * in the context of the current request. Note that since some commands have dynamic permissions, 
         * in some cases it's better to instantiate a command object and pass it to {@link #canIssue(edu.harvard.iq.dataverse.engine.command.Command)}.
         * @param aCmdClass
         * @return {@code true} iff instances of the command class can be issued in the context of the current request.
         */
        public boolean canIssue( Class<? extends Command> aCmdClass ) {
            Map<String, Set<Permission>> required = CH.permissionsRequired(aCmdClass);
            if (required.isEmpty() || required.get("") == null) {
                logger.fine("IsUserAllowedOn: empty-true");
                return true;
            } else {
                Set<Permission> grantedUserPermissions = permissionsFor(request, subject);
                Set<Permission> requiredPermissionSet = required.get("");
                return grantedUserPermissions.containsAll(requiredPermissionSet);
            }
        }
        
        /**
         * Tests whether the command can be issued over the {@link DvObject}
         * in the context of the current request. 
         * @param aCmd
         * @return {@code true} iff the command can be issued in the context of the current request.
         */
        public boolean canIssue( Command<?> aCmd ) {
            Map<String, Set<Permission>> required = aCmd.getRequiredPermissions();
            if (required.isEmpty() || required.get("") == null) {
                logger.fine("IsUserAllowedOn: empty-true");
                return true;
            } else {
                Set<Permission> grantedUserPermissions = permissionsFor(request, subject);
                Set<Permission> requiredPermissionSet = required.get("");
                return grantedUserPermissions.containsAll(requiredPermissionSet);
            }
        }
    }
    
    /**
     * A permission query for a given role assignee. Does not cover request-level permissions.
     */
    public class StaticPermissionQuery {

        final DvObject subject;
        final RoleAssignee user;

        private StaticPermissionQuery(RoleAssignee user, DvObject subject) {
            this.subject = subject;
            this.user = user;
        }

        public StaticPermissionQuery user(RoleAssignee anotherUser) {
            return new StaticPermissionQuery(anotherUser, subject);
        }

        /**
         * "Fast and loose" query mechanism, allowing to pass the command class
         * name, does not take request-level permissions into account. Command is assumed to live in
         * {@code edu.harvard.iq.dataverse.engine.command.impl.}
         *
         * @deprecated Use DynamicPermissionQuery instead
         * @param commandName
         * @return {@code true} iff the user has the permissions required by the
         * command on the object.
         * @throws ClassNotFoundException
         */
        @Deprecated
        public boolean canIssueCommand(String commandName) throws ClassNotFoundException {
            return isUserAllowedOn(user,
                    (Class<? extends Command>) Class.forName("edu.harvard.iq.dataverse.engine.command.impl." + commandName), subject);
        }

        public Set<Permission> get() {
            return permissionsFor(user, subject);
        }

        public boolean has(Permission p) {
            return get().contains(p);
        }

        public boolean has(String pName) {
            return get().contains(Permission.valueOf(pName));
        }

    }

    public List<RoleAssignment> assignmentsOn(DvObject d) {
        return em.createNamedQuery("RoleAssignment.listByDefinitionPointId", RoleAssignment.class)
                .setParameter("definitionPointId", d.getId()).getResultList();
    }
    
    /**
     * Finds all the permissions the {@link User} in {@code req} has over 
     * {@code dvo}, in the context of {@code req}.
     * @param req 
     * @param dvo
     * @return Permissions of {@code req.getUser()} over {@code dvo}.
     */
    public Set<Permission> permissionsFor( DataverseRequest req, DvObject dvo ) {
        Set<Permission> permissions = EnumSet.noneOf(Permission.class);
        
        // Add permissions specifically given to the user
        permissions.addAll( permissionsForSingleRoleAssignee(req.getUser(),dvo) );
        
        /*
        Set<Group> groups = groupService.groupsFor(req,dvo);
        
        // Add permissions gained from groups
        for ( Group g : groups ) {
            final Set<Permission> groupPremissions = permissionsForSingleRoleAssignee(g,dvo);
            permissions.addAll(groupPremissions);
        }
        */
        
        if ( ! req.getUser().isAuthenticated() ) {
            permissions.removeAll( PERMISSIONS_FOR_AUTHENTICATED_USERS_ONLY );
        }
        
        return permissions;
    }
    
    /**
     * Returns the set of permission a user/group has over a dataverse object. 
     * This method takes into consideration group memberships as well, but does
     * not look into request-level groups.
     * @param ra The role assignee.
     * @param dvo The {@link DvObject} on which the user wants to operate
     * @return the set of permissions {@code ra} has over {@code dvo}.
     */
    public Set<Permission> permissionsFor(RoleAssignee ra, DvObject dvo) {

        Set<Permission> permissions = EnumSet.noneOf(Permission.class);
        
        // Add permissions specifically given to the user
        permissions.addAll( permissionsForSingleRoleAssignee(ra,dvo) );
        
        // Add permissions gained from groups
        Set<Group> groupsRaBelongsTo = groupService.groupsFor(ra,dvo);
        for ( Group g : groupsRaBelongsTo ) {
            permissions.addAll( permissionsForSingleRoleAssignee(g,dvo) );
        }
        
        if ( (ra instanceof User) && (! ((User)ra).isAuthenticated()) ) {
            permissions.removeAll( PERMISSIONS_FOR_AUTHENTICATED_USERS_ONLY );
        }
        
        return permissions;
    }

    
    private Set<Permission> permissionsForSingleRoleAssignee(RoleAssignee ra, DvObject d) {
        // super user check
        // for 4.0, we are allowing superusers all permissions
        // for secure data, we may need to restrict some of the permissions
        if (ra instanceof AuthenticatedUser && ((AuthenticatedUser) ra).isSuperuser()) {
            return EnumSet.allOf(Permission.class);
        }
        
        // Start with no permissions, build from there.
        Set<Permission> retVal = EnumSet.noneOf(Permission.class);

        // File special case.
        if (d instanceof DataFile) {
            // unrestricted files that are part of a release dataset 
            // automatically get download permission for everybody:
            //      -- L.A. 4.0 beta12
            
            DataFile df = (DataFile)d;
            
            if (!df.isRestricted()) {
                if (df.getOwner().getReleasedVersion() != null) {
                    if (df.getOwner().getReleasedVersion().getFileMetadatas() != null) {
                        for (FileMetadata fm : df.getOwner().getReleasedVersion().getFileMetadatas()) {
                            if (df.equals(fm.getDataFile())) {
                                retVal.add(Permission.DownloadFile);
                                break;
                            }
                        }
                    }
                }
            }
        }
        
        // Direct assignments to ra on d
        assignmentsFor(ra, d).forEach( 
                asmnt -> retVal.addAll(asmnt.getRole().permissions())
        );
        
        // Recurse up the group containment hierarchy.
        groupService.groupsFor(ra, d).forEach(
                grp -> retVal.addAll(permissionsForSingleRoleAssignee(grp, d)));
        return retVal;
    }

    /**
     * Returns all the role assignments that are effective for {@code ra} over
     * {@code d}. Traverses the containment hierarchy of the {@code d}.
     * @param ra The role assignee whose role assignemnts we look for.
     * @param d The dataverse object over which the roles are assigned
     * @return A set of all the role assignments for {@code ra} over {@code d}.
     */
    public Set<RoleAssignment> assignmentsFor(RoleAssignee ra, DvObject d) {
        Set<RoleAssignment> assignments = new HashSet<>();
        while (d != null) {
            assignments.addAll(roleService.directRoleAssignments(ra, d));
            if (d instanceof Dataverse && ((Dataverse) d).isEffectivelyPermissionRoot()) {
                return assignments;
            } else {
                d = d.getOwner();
            }
        }

        return assignments;
    }

    /**
     * For commands with no named dvObjects, this allows a quick check whether
     * a user can issue the command on the dataverse or not.
     *
     * @param u
     * @param commandClass
     * @param dvo
     * @return
     * @deprecated As commands have dynamic permissions now, it is not enough to look at the static permissions anymore.
     * @see #isUserAllowedOn(edu.harvard.iq.dataverse.authorization.RoleAssignee, edu.harvard.iq.dataverse.engine.command.Command, edu.harvard.iq.dataverse.DvObject) 
     */
    public boolean isUserAllowedOn(RoleAssignee u, Class<? extends Command> commandClass, DvObject dvo) {
        Map<String, Set<Permission>> required = CH.permissionsRequired(commandClass);
        return isUserAllowedOn(u, required, dvo);
    }

    public boolean isUserAllowedOn(RoleAssignee u, Command<?> command, DvObject dvo) {
        Map<String, Set<Permission>> required = command.getRequiredPermissions();
        return isUserAllowedOn(u, required, dvo);
    }

    private boolean isUserAllowedOn(RoleAssignee u, Map<String, Set<Permission>> required, DvObject dvo) {
        if (required.isEmpty() || required.get("") == null) {
            logger.fine("IsUserAllowedOn: empty-true");
            return true;
        } else {
            Set<Permission> grantedUserPermissions = permissionsFor(u, dvo);
            Set<Permission> requiredPermissionSet = required.get("");
            return grantedUserPermissions.containsAll(requiredPermissionSet);
        }
    }

    public StaticPermissionQuery userOn(RoleAssignee u, DvObject d) {
        if (u == null) {
            // get guest user for dataverse d
            u = GuestUser.get();
        }
        return new StaticPermissionQuery(u, d);
    }

    public RequestPermissionQuery on(DvObject d) {
        if (d == null) {
            throw new IllegalArgumentException("Cannot query permissions on a null DvObject");
        }
        if (d.getId() == null) {
            throw new IllegalArgumentException("Cannot query permissions on a DvObject with a null id.");
        }
        return requestOn(dvRequestService.getDataverseRequest(), d);
    }
    
    public RequestPermissionQuery requestOn( DataverseRequest req, DvObject dvo ) {
        if (dvo.getId() == null) {
            throw new IllegalArgumentException("Cannot query permissions on a DvObject with a null id.");
        }
        return new RequestPermissionQuery(dvo, req);
    }
    
    public RequestPermissionQuery request( DataverseRequest req ) {
        return new RequestPermissionQuery(null, req);
    }
    
    /**
     * Go from (User, Permission) to a list of Dataverse objects that the user
     * has the permission on.
     *
     * @param user
     * @param permission
     * @return The list of dataverses {@code user} has permission {@code permission} on.
     */
    public List<Dataverse> getDataversesUserHasPermissionOn(AuthenticatedUser user, Permission permission) {
        Set<Group> groups = groupService.groupsFor(user);
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
            if (userOn(user, dataverse).has(permission)) {
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
    
    public List<Long> getDvObjectsUserHasRoleOn(User user) {
        return getDvObjectIdsUserHasRoleOn(user, null, null, false);
    }

    public List<Long> getDvObjectIdsUserHasRoleOn(User user, List<DataverseRole> roles) {
        return getDvObjectIdsUserHasRoleOn(user, roles, null, false);
    }

    /*
    Method takes in a user and optional list of roles and dvobject type
    queries the role assigment table filtering by optional roles and dv
    returns dvobject ids
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
    
       
    public List<Long> getDvObjectIdsUserHasRoleOn(User user, List<DataverseRole> roles, List<String> types, boolean indirect) {


        String roleString = getRolesClause (roles);
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
    
    
    
}
