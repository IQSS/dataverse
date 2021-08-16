package edu.harvard.iq.dataverse;

import com.google.common.collect.Sets;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.GroupUtil;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.mail.confirmemail.ConfirmEmailServiceBean;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.DvObjectContainer;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.TermsOfUseType;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.group.Group;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignee;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.harvard.iq.dataverse.engine.command.CommandHelper.CH;
import static java.util.stream.Collectors.toList;

/**
 * Your one-stop-shop for deciding which user can do what action on which
 * objects (TM). Note that this bean accesses the permissions/user assignment on
 * a read-only basis. Changing the permissions a user has is done via roles and
 * ras, over at {@link DataverseRoleServiceBean}.
 *
 * @author michael
 */
@Stateless
public class PermissionServiceBean {

    private static final Logger logger = LoggerFactory.getLogger(PermissionServiceBean.class);

    private static final Set<Permission> PERMISSIONS_FOR_AUTHENTICATED_USERS_ONLY
            = EnumSet.copyOf(Arrays.asList(Permission.values()).stream()
                                     .filter(Permission::requiresAuthenticatedUser)
                                     .collect(toList()));

    private static final Set<Permission> WRITE_PERMISSIONS
            = EnumSet.copyOf(Arrays.asList(Permission.values()).stream()
                    .filter(Permission::isRequiresWrite)
                    .collect(toList()));

    @EJB
    private DataverseRoleServiceBean roleService;

    @EJB
    private RoleAssigneeServiceBean roleAssigneeService;

    @EJB
    private DataverseDao dataverseDao;

    @EJB
    private DvObjectServiceBean dvObjectServiceBean;

    @PersistenceContext
    private EntityManager em;

    @EJB
    private GroupServiceBean groupService;

    @EJB
    private SystemConfig systemConfig;

    @Inject
    private ConfirmEmailServiceBean confirmEmailService;

    /**
     * A request-level permission query (e.g includes IP ras).
     */
    public class RequestPermissionQuery {

        private final DvObject subject;
        private final DataverseRequest request;

        private RequestPermissionQuery(DvObject subject, DataverseRequest request) {
            this.subject = subject;
            this.request = request;
        }

        public boolean has(Permission p) {
            return hasPermissionsFor(request, subject, EnumSet.of(p));
        }

        /*
         * This is a new and optimized method, for making a quick lookup on
         * a SET of permission all at once; it was originally called
         * has(Set<Permission> p)... however, while unambiguos in Java,
         * the fact that there were 2 has() methods - has(Permission) and
         * has(Set<Permission>) - was confusing PrimeFaces and resulting in
         * pages failing with "cannot convert "String" to "Set" error messages...
         * so it had to be renamed to hasPermissions(...)
         */
        public boolean hasPermissions(Set<Permission> p) {
            if (p.isEmpty()) {
                return true;
            }
            return hasPermissionsFor(request, subject, p);
        }

        /**
         * Tests whether a command of the passed class can be issued over the
         * {@link DvObject} in the context of the current request. Note that
         * since some commands have dynamic permissions, in some cases it's
         * better to instantiate a command object and pass it to
         * {@link #canIssue(edu.harvard.iq.dataverse.engine.command.Command)}.
         *
         * @param aCmdClass
         * @return {@code true} iff instances of the command class can be issued
         * in the context of the current request.
         */
        public boolean canIssue(Class<? extends Command> aCmdClass) {
            Map<String, Set<Permission>> required = CH.permissionsRequired(aCmdClass);
            if (required.isEmpty() || required.get("") == null) {
                logger.debug("IsUserAllowedOn: empty-true");
                return true;
            } else {
                Set<Permission> requiredPermissionSet = required.get("");
                return hasPermissions(requiredPermissionSet);
            }
        }

        /**
         * Tests whether the command can be issued over the {@link DvObject} in
         * the context of the current request.
         *
         * @param aCmd
         * @return {@code true} iff the command can be issued in the context of
         * the current request.
         */
        public boolean canIssue(Command<?> aCmd) {
            Map<String, Set<Permission>> required = aCmd.getRequiredPermissions();
            if (required.isEmpty() || required.get("") == null) {
                logger.debug("IsUserAllowedOn: empty-true");
                return true;
            } else {
                Set<Permission> requiredPermissionSet = required.get("");
                return hasPermissions(requiredPermissionSet);
            }
        }
    }

    /**
     * A permission query for a given role assignee. Does not cover
     * request-level permissions.
     */
    public class StaticPermissionQuery {

        private final DvObject subject;
        private final RoleAssignee user;

        private StaticPermissionQuery(RoleAssignee user, DvObject subject) {
            this.subject = subject;
            this.user = user;
        }

        public boolean has(Permission p) {
            return hasPermissionsFor(user, subject, EnumSet.of(p));
        }

    }

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
     *
     * @param req             The request whose permissions are queried
     * @param dvo             The objects whose children we list
     * @param required        (sub)set of permissions {@code req} has on the objects in the returned list
     * @param includeReleased include released dataverses and datasets without checking permissions
     * @return list of {@code dvo} children over which {@code req} has at least {@code required} permissions.
     */
    private List<DvObject> whichChildrenHasPermissionsFor(DataverseRequest req, DvObjectContainer dvo, Set<Permission> required, boolean includeReleased) {
        List<DvObject> children = dvObjectServiceBean.findByOwnerId(dvo.getId());
        User user = req.getUser();

        // quick cases
        if (user.isSuperuser()) {
            return children; // it's good to be king

        } else if (!user.isAuthenticated()) {
            if (required.stream().anyMatch(PERMISSIONS_FOR_AUTHENTICATED_USERS_ONLY::contains)) {
                // At least one of the permissions requires that the user is authenticated, which is not the case.
                return Collections.emptyList();
            }
        }

        // Actually look at permissions
        Set<DvObject> parents = getPermissionAncestors(dvo);
        Set<RoleAssignee> ras = new HashSet<>(groupService.groupsFor(req));
        ras.add(user);
        List<RoleAssignment> parentsAsignments = roleService.directRoleAssignmentsByAssigneesAndDvObjects(ras, parents);

        boolean unconfirmedMailMode = confirmEmailService.hasEffectivelyUnconfirmedMail(user);

        for (RoleAssignment asmnt : parentsAsignments) {
            Set<Permission> permissions = unconfirmedMailMode
                    ? asmnt.getRole().permissions()
                        .stream()
                        .filter(p -> !p.isRequiresWrite())
                        .collect(Collectors.toSet())
                    : asmnt.getRole().permissions();
            required.removeAll(permissions);
        }
        if (required.isEmpty()) {
            // All permissions are met by role assignments on the request
            return children;
        }

        // Looking at each child at a time now.
        // 1. Map childs to permissions
        List<RoleAssignment> childrenAssignments = roleService.directRoleAssignmentsByAssigneesAndDvObjects(ras,
                 includeReleased ? children.stream().filter(child -> !child.isReleased()).collect(toList()) : children);

        Map<DvObject, Set<Permission>> roleMap = new HashMap<>();
        childrenAssignments.forEach(assignment -> {
            DvObject definitionPoint = assignment.getDefinitionPoint();
            if (!roleMap.containsKey(definitionPoint)) {
                roleMap.put(definitionPoint, assignment.getRole().permissions());
            } else {
                roleMap.get(definitionPoint).addAll(assignment.getRole().permissions());
            }
        });

        // 2. Filter by permission map created at (1).
        return children.stream().filter(
                child -> ((includeReleased && child.isReleased())
                            || hasPermissions(required, roleMap, child, unconfirmedMailMode)))
                .collect(toList());

    }

    private boolean hasPermissions(Set<Permission> required, Map<DvObject, Set<Permission>> roleMap, DvObject child,
                                   boolean unconfirmedMailMode) {
        Set<Permission> permissionsApplicableToObject = required.stream()
                .filter(p -> p.appliesTo(child.getClass()))
                .collect(Collectors.toSet());
        return roleMap.containsKey(child)
                && roleMap.get(child).containsAll(permissionsApplicableToObject)
                && unconfirmedMailMode
                    ? !permissionsApplicableToObject.stream().anyMatch(Permission::isRequiresWrite)
                    : true;
    }

    // A shortcut for calling the method above, with the assumption that all the
    // released dataverses and datasets should be included:
    public List<DvObject> whichChildrenHasPermissionsForOrReleased(DataverseRequest req, DvObjectContainer dvo, Set<Permission> required) {
        return whichChildrenHasPermissionsFor(req, dvo, required, true);
    }

    private boolean hasPermissionsFor(DataverseRequest req, DvObject dvo, Set<Permission> required) {
        if ((systemConfig.isReadonlyMode() || confirmEmailService.hasEffectivelyUnconfirmedMail(req.getUser()))
                && required.stream().anyMatch(WRITE_PERMISSIONS::contains)) {
            return false;
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

    private boolean hasPermissionsFor(RoleAssignee ra, DvObject dvo, Set<Permission> required) {
        boolean unconfirmedEmail = ra instanceof User && confirmEmailService.hasEffectivelyUnconfirmedMail((User) ra);
        if ((systemConfig.isReadonlyMode() || unconfirmedEmail) && required.stream().anyMatch(WRITE_PERMISSIONS::contains)) {
            return false;
        }

        if (ra instanceof User) {
            User user = (User) ra;
            if (user.isSuperuser()) {
                return true;
            } else if (!user.isAuthenticated()) {
                Set<Permission> requiredCopy = EnumSet.copyOf(required);
                requiredCopy.retainAll(PERMISSIONS_FOR_AUTHENTICATED_USERS_ONLY);
                if (!requiredCopy.isEmpty()) {
                    return false;
                }
            }
        }
        required.removeAll(getInferredPermissions(dvo));
        if (required.isEmpty()) {
            return true;
        }

        Set<RoleAssignee> ras = new HashSet<>(groupService.groupsFor(ra, dvo));
        ras.add(ra);
        return hasGroupPermissionsFor(ras, dvo, required);
    }

    private boolean hasGroupPermissionsFor(Set<RoleAssignee> ras, DvObject dvo, Set<Permission> required) {
        for (RoleAssignment asmnt : assignmentsFor(ras, dvo)) {
            required.removeAll(asmnt.getRole().permissions());
        }
        return required.isEmpty();
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
        User user = req.getUser();
        if (user.isSuperuser()) {
            if (systemConfig.isReadonlyMode()) {
              Set<Permission> readonlyPermissions = EnumSet.allOf(Permission.class);
              readonlyPermissions.removeAll(WRITE_PERMISSIONS);
              return readonlyPermissions;
            }
            return EnumSet.allOf(Permission.class);
        }

        Set<Permission> permissions = getInferredPermissions(dvo);

        // Add permissions gained from ras
        Set<RoleAssignee> ras = new HashSet<>(groupService.groupsFor(req, dvo));
        ras.add(user);
        addGroupPermissionsFor(ras, dvo, permissions);

        if (systemConfig.isReadonlyMode() || confirmEmailService.hasEffectivelyUnconfirmedMail(user)) {
            permissions.removeAll(WRITE_PERMISSIONS);
        }
        if (!user.isAuthenticated()) {
            permissions.removeAll(PERMISSIONS_FOR_AUTHENTICATED_USERS_ONLY);
        }
        return permissions;
    }

    private void addGroupPermissionsFor(Set<RoleAssignee> ras, DvObject dvo, Set<Permission> permissions) {
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
            DatasetVersion realeasedDatasetVersion = df.getOwner().getReleasedVersion();

            if (realeasedDatasetVersion != null) {
                for (FileMetadata fm : realeasedDatasetVersion.getFileMetadatas()) {
                    if (df.equals(fm.getDataFile())) {
                        return fm.getTermsOfUse().getTermsOfUseType() != TermsOfUseType.RESTRICTED;

                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns all the role assignments that are effective for {@code ra} over
     * {@code d}. Traverses the containment hierarchy of the {@code d}.
     *
     * @param ra The role assignee whose role assignemnts we look for.
     * @param d  The dataverse object over which the roles are assigned
     * @return A set of all the role assignments for {@code ra} over {@code d}.
     */
    public Set<RoleAssignment> assignmentsFor(RoleAssignee ra, DvObject d) {
        return assignmentsFor(Collections.singleton(ra), d);
    }

    private Set<RoleAssignment> assignmentsFor(Set<RoleAssignee> ras, DvObject d) {
        Set<DvObject> permAncestors = getPermissionAncestors(d);
        return new HashSet<>(roleService.directRoleAssignmentsByAssigneesAndDvObjects(ras, permAncestors));
    }

    private Set<DvObject> getPermissionAncestors(DvObject d) {
        Set<DvObject> ancestors = new HashSet<>();
        DvObject currentDvObject = d;
        while (currentDvObject != null) {
            ancestors.add(currentDvObject);
            if (currentDvObject instanceof Dataverse && currentDvObject.isEffectivelyPermissionRoot()) {
                return ancestors;
            }

            currentDvObject = currentDvObject.getOwner();
        }
        return ancestors;
    }

    public boolean isUserAllowedOn(User user, Command<?> command, DvObject dvo) {
        Map<String, Set<Permission>> required = command.getRequiredPermissions();
        return isUserAllowedOn(user, required, dvo);
    }

    private boolean isUserAllowedOn(User user, Map<String, Set<Permission>> required, DvObject dvo) {
        if (required.isEmpty() || required.get("") == null) {
            logger.debug("IsUserAllowedOn: empty-true");
            return true;
        } else {
            Set<Permission> requiredPermissionSet = required.get("");
            return hasPermissionsFor(user, dvo, requiredPermissionSet);
        }
    }

    public StaticPermissionQuery userOn(RoleAssignee u, DvObject d) {
        if (u == null) {
            // get guest user for dataverse d
            u = GuestUser.get();
        }
        return new StaticPermissionQuery(u, d);
    }

    public RequestPermissionQuery requestOn(DataverseRequest req, DvObject dvo) {
        if (dvo.getId() == null) {
            throw new IllegalArgumentException("Cannot query permissions on a DvObject with a null id.");
        }
        return new RequestPermissionQuery(dvo, req);
    }

    /**
     * Go from (User, Permission) to a list of Dataverse objects that the user
     * has the permission on.
     *
     * @param user
     * @param permission
     * @return The list of dataverses {@code user} has permission
     * {@code permission} on.
     */
    public List<Dataverse> getDataversesUserHasPermissionOn(AuthenticatedUser user, Permission permission) {
        Set<Group> groups = groupService.groupsFor(user);
        String identifiers = GroupUtil.getAllIdentifiersForUser(user, groups);
        /**
         * @todo Are there any strings in identifiers that would break this SQL
         * query?
         */
        String query = "SELECT id FROM dvobject WHERE dtype = 'Dataverse' and id in (select definitionpoint_id from roleassignment where assigneeidentifier in (" + identifiers + "));";
        logger.debug("query: {}", query);
        Query nativeQuery = em.createNativeQuery(query);
        List<Integer> dataverseIdsToCheck = nativeQuery.getResultList();
        List<Dataverse> dataversesUserHasPermissionOn = new LinkedList<>();
        for (int dvIdAsInt : dataverseIdsToCheck) {
            Dataverse dataverse = dataverseDao.find(Long.valueOf(dvIdAsInt));
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

    public Map<String, AuthenticatedUser> getDistinctUsersWithPermissionOn(Permission permission, DvObject dvo) {

        List<AuthenticatedUser> users = getUsersWithPermissionOn(permission, dvo);
        Map<String, AuthenticatedUser> distinctUsers = new HashMap<>();
        users.forEach((au) -> {
            distinctUsers.put(au.getIdentifier(), au);
        });

        return distinctUsers;
    }

    public boolean checkEditDatasetLock(Dataset dataset, DataverseRequest dataverseRequest, Command command) throws IllegalCommandException {
        if (dataset.isLocked()) {
            if (dataset.isLockedFor(DatasetLock.Reason.InReview)) {
                // The "InReview" lock is not really a lock for curators. They can still make edits.
                if (!isUserAllowedOn(dataverseRequest.getUser(), new PublishDatasetCommand(dataset, dataverseRequest, true), dataset)) {
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
        return false;
    }

    public void checkDownloadFileLock(Dataset dataset, DataverseRequest dataverseRequest, Command command) throws IllegalCommandException {
        if (dataset.isLocked()) {
            if (dataset.isLockedFor(DatasetLock.Reason.InReview)) {
                // The "InReview" lock is not really a lock for curators or contributors. They can still download.
                if (!isUserAllowedOn(dataverseRequest.getUser(), new UpdateDatasetVersionCommand(dataset, dataverseRequest), dataset)) {
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


    public boolean isUserAbleToEditDataverse(User user, Dataverse dataverse) {
        return hasPermissionsFor(user, dataverse, Sets.newHashSet(Permission.EditDataverse));
    }

    public boolean isUserCanEditDataverseTextMessagesAndBanners(User user, Long dataverseId) {

        if (dataverseId == null) {
            return false;
        }
        if (systemConfig.isReadonlyMode()) {
            return false;
        }
        Dataverse dataverse = dataverseDao.find(dataverseId);

        if (dataverse == null) {
            return false;
        }

        return (isUserAbleToEditDataverse(user, dataverse) || user.isSuperuser()) && dataverse.isAllowMessagesBanners();
    }

    /**
     * Returns roles that are effective for {@code au}
     * over {@code dvObj}. Traverses the containment hierarchy of the {@code d}.
     * Takes into consideration all groups that {@code au} is part of.
     *
     * @param au    The authenticated user whose role assignments we look for.
     * @param dvObj The Dataverse object over which the roles are assigned
     * @return A set of all the role assignments for {@code ra} over {@code d}.
     */
    public Set<RoleAssignment> getRolesOfUser(AuthenticatedUser au, DvObject dvObj) {

        Set<RoleAssignment> roles = assignmentsFor(au, dvObj);

        Set<Group> groupsUserBelongsTo = groupService.groupsFor(au, dvObj);
        for (Group g : groupsUserBelongsTo) {
            roles.addAll(assignmentsFor(g, dvObj));
        }

        return roles;
    }

}
