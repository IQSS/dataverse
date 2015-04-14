package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.RoleAssignee;
import edu.harvard.iq.dataverse.authorization.groups.Group;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.builtin.AuthenticatedUsers;
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
import java.util.LinkedList;
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

    public class PermissionQuery {

        final RoleAssignee user;
        final DvObject subject;

        public PermissionQuery(RoleAssignee user, DvObject subject) {
            this.user = user;
            this.subject = subject;
        }

        public PermissionQuery user(User anotherUser) {
            return new PermissionQuery(anotherUser, subject);
        }

        public boolean canIssue(Class<? extends Command> cmd) {
            return isUserAllowedOn(user, cmd, subject);
        }

        /**
         * "Fast and loose" query mechanism, allowing to pass the command class
         * name. Command is assumed to live in
         * {@code edu.harvard.iq.dataverse.engine.command.impl.}
         *
         * @deprecated
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
     * Returns the set of permission a user has over a dataverse object. 
     * This method takes into consideration group memberships as well.
     * @param ra The role assignee.
     * @param d The {@link DvObject} on which the user wants to operate
     * @return the set of permissions {@code u} has over {@code d}.
     */
    public Set<Permission> permissionsFor(RoleAssignee ra, DvObject d) {

        Set<Permission> permissions = EnumSet.noneOf(Permission.class);
        
        // Add permissions specifically given to the user
        permissions.addAll( permissionsForSingleRoleAssignee(ra,d) );
        Set<Group> groupsRaBelongsTo = groupService.groupsFor(ra,d);
        // Add permissions gained from groups
        for ( Group g : groupsRaBelongsTo ) {
            permissions.addAll( permissionsForSingleRoleAssignee(g,d) );
        }
        
        return permissions;
    }

    public Set<Permission> permissionsForSingleRoleAssignee(RoleAssignee ra, DvObject d) {
        // super user check
        // @todo for 4.0, we are allowing superusers all permissions
        // for secure data, we may need to restrict some of the permissions
        if (ra instanceof AuthenticatedUser && ((AuthenticatedUser) ra).isSuperuser()) {
            return EnumSet.allOf(Permission.class);
        }

        Set<Permission> retVal = EnumSet.noneOf(Permission.class);

        if (d instanceof DataFile) {
            // unrestricted files that are part of a release dataset 
            // automatically get download permission for everybody:
            //      -- L.A. 4.0 beta12
            
            DataFile df = (DataFile)d;
            
            if (!df.isRestricted()) {
                //logger.info("restricted? - nope.");
                if (df.getOwner().getReleasedVersion() != null) {
                    //logger.info("file belongs to a dataset with a released version.");
                    if (df.getOwner().getReleasedVersion().getFileMetadatas() != null) {
                        //logger.info("going through the list of filemetadatas that belong to the released version.");
                        for (FileMetadata fm : df.getOwner().getReleasedVersion().getFileMetadatas()) {
                            if (df.equals(fm.getDataFile())) {
                                //logger.info("yep, found a match!");
                                retVal.add(Permission.DownloadFile);
                            }
                        }
                    }
                }
            }
        }
        
        for (RoleAssignment asmnt : assignmentsFor(ra, d)) {
            retVal.addAll(asmnt.getRole().permissions());
        }
        
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

    public PermissionQuery userOn(RoleAssignee u, DvObject d) {
        if (u == null) {
            // get guest user for dataverse d
            u = new GuestUser();
        }
        return new PermissionQuery(u, d);
    }

    public PermissionQuery on(DvObject d) {
        if (d == null) {
            throw new IllegalArgumentException("Cannot query permissions on a null DvObject");
        }
        if (d.getId() == null) {
            throw new IllegalArgumentException("Cannot query permissions on a DvObject with a null id.");
        }
        return userOn(session.getUser(), d);
    }

    /**
     * Go from (User, Permission) to a list of Dataverse objects that the user
     * has the permission on.
     *
     * @param user
     * @param permission
     * @return The list of dataverses {@code user} has permission {@code permission} on.
     */
    public List<Dataverse> getDataversesUserHasPermissionOn(User user, Permission permission) {
        /**
         * @todo What about groups? And how can we make this more performant?
         */
        Query nativeQuery = em.createNativeQuery("SELECT id FROM dvobject WHERE dtype = 'Dataverse' and id in (select definitionpoint_id from roleassignment where assigneeidentifier in ('" + user.getIdentifier() + "'));");
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
    
}
