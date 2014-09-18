package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.Permission;
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
    DataverseUserServiceBean userService;

    @EJB
    DataverseRoleServiceBean roleService;

    @PersistenceContext
    EntityManager em;

    @Inject
    DataverseSession session;

    public class PermissionQuery {

        final DataverseUser user;
        final DvObject subject;

        public PermissionQuery(DataverseUser user, DvObject subject) {
            this.user = user;
            this.subject = subject;
        }

        public PermissionQuery user(DataverseUser anotherUser) {
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

    public Set<Permission> permissionsFor(DataverseUser u, DvObject d) {
        Set<Permission> retVal = EnumSet.noneOf(Permission.class);
        for (RoleAssignment asmnt : roleService.assignmentsFor(u, d)) {
            retVal.addAll(asmnt.getRole().permissions());
        }

        // Every user can access released DvObjects
        if (d.isReleased()) {
            retVal.add(Permission.Access);
        }
        
		// special case - All registered users can add to the root dv (no owner)
        // or if alias ends in "_open"
        if (d instanceof Dataverse) {
            Dataverse dv = (Dataverse) d;
            if ((dv.getOwner() == null || dv.getAlias().endsWith("_open")) && (!u.isGuest())) {
                // TODO when groups arrive, this has to go.
                retVal.add(Permission.UndoableEdit);
                retVal.add(Permission.AddDataset);
                retVal.add(Permission.AddDataverse);
            }
        }

        return retVal;
    }

    public Set<RoleAssignment> assignmentsFor(DataverseUser u, DvObject d) {
        Set<RoleAssignment> assignments = new HashSet<>();
        while (d != null) {
            assignments.addAll(roleService.directRoleAssignments(u, d));
            if (d instanceof Dataverse && ((Dataverse) d).isEffectivlyPermissionRoot()) {
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
    public boolean isUserAllowedOn(DataverseUser u, Class<? extends Command> commandClass, DvObject dvo) {
        Map<String, Set<Permission>> required = CH.permissionsRequired(commandClass);
        if (required.isEmpty() || required.get("") == null) {
            logger.info("IsUserAllowedOn: empty-true");
            return true;
        } else {
            Set<Permission> grantedUserPermissions = permissionsFor(u, dvo);
            Set<Permission> requiredPermissionSet = required.get("");
            return grantedUserPermissions.containsAll(requiredPermissionSet);
        }
    }

    public PermissionQuery userOn(DataverseUser u, DvObject d) {
        if (u == null) {
            // get guest user for dataverse d
            u = userService.findByUserName("GabbiGuest");
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

}
