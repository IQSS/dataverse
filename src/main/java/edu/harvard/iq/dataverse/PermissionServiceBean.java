package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.Permission;
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
import java.util.ArrayList;

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
    DataverseRoleServiceBean roleService;

    @EJB
    DataverseServiceBean dataverseService;

    @PersistenceContext
    EntityManager em;

    @Inject
    DataverseSession session;

    public class PermissionQuery {

        final User user;
        final DvObject subject;

        public PermissionQuery(User user, DvObject subject) {
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

    public Set<Permission> permissionsFor(User u, DvObject d) {
        // super user check
        // @todo for 4.0, we are allowing superusers all permissions
        // for secure data, we may need to restrict some of the permissions
        if (u instanceof AuthenticatedUser && ((AuthenticatedUser) u).isSuperuser()) {
            return EnumSet.allOf(Permission.class);
        }        
        
        Set<Permission> retVal = EnumSet.noneOf(Permission.class);
        
        // temporary HACK for allowing any authenticated user create 
        // new objects inside the root dataverse, and in dataverses 
        // with the aliases that end with "_open". 
        // TODO: this should be replaced with some default group, to which 
        // all the new users should be assigned automatically. This group will 
        // get as much or as little permissions as the local dataverse are 
        // willing to give it. 
        // - Leonid, 4.0 beta 7 (merge party)
        // NOT solvable until we get groups in. Then, we assign this roel to a
        // the authenticated users group.
        
        if (d instanceof Dataverse) {
            Dataverse dv = (Dataverse) d;
            if (u.isAuthenticated()) {
                if (dv.getOwner() == null || dv.getAlias().endsWith("_open")) {
                    retVal.add(Permission.AddDataset);
                    retVal.add(Permission.AddDataverse);
                }
            }
        }
        
        for (RoleAssignment asmnt : roleService.assignmentsFor(u, d)) {
            retVal.addAll(asmnt.getRole().permissions());
        }

        // Every user can access released DvObjects
        if (d.isReleased()) {
            retVal.add(Permission.Discover);
        }

        return retVal;
    }

    public Set<RoleAssignment> assignmentsFor(User u, DvObject d) {
        Set<RoleAssignment> assignments = new HashSet<>();
        while (d != null) {
            assignments.addAll(roleService.directRoleAssignments(u, d));
            if (d instanceof Dataverse && ((Dataverse) d).isEffectivelyPermissionRoot()) {
                return assignments;
            } else {
                d = d.getOwner();
            }
        }

        return assignments;
    }

    /**
     * For commands with no named dataverses, this allows a quick check whether
     * a user can issue the command on the dataverse or not.
     *
     * @param u
     * @param commandClass
     * @param dvo
     * @return
     */
    public boolean isUserAllowedOn(User u, Class<? extends Command> commandClass, DvObject dvo) {        
        Map<String, Set<Permission>> required = CH.permissionsRequired(commandClass);
        if (required.isEmpty() || required.get("") == null) {
            logger.fine("IsUserAllowedOn: empty-true");
            return true;
        } else {
            Set<Permission> grantedUserPermissions = permissionsFor(u, dvo);
            Set<Permission> requiredPermissionSet = required.get("");
            return grantedUserPermissions.containsAll(requiredPermissionSet);
        }
    }

    public PermissionQuery userOn(User u, DvObject d) {
        if (u == null) {
            // get guest user for dataverse d
            u = GuestUser.get();
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
     * @todo Check isPermissionRoot (or [sic] isEffectivlyPermissionRoot?)
     *
     * @todo Refactor this into something more performant:
     * https://github.com/IQSS/dataverse/issues/784
     *
     * In DVN 3.x we had this: List<VDC> vdcList =
     * vdcService.getUserVDCs(vdcUser.getId());
     */
    public List<Dataverse> getDataversesUserHasPermissionOn(User user, Permission permission) {
        List<Dataverse> allDataverses = dataverseService.findAll();
        List<Dataverse> dataversesUserHasPermissionOn = new ArrayList<>();
        for (Dataverse dataverse : allDataverses) {
            if (userOn(user, dataverse).has(permission)) {
                dataversesUserHasPermissionOn.add(dataverse);
            }
        }
        return dataversesUserHasPermissionOn;
    }

}
