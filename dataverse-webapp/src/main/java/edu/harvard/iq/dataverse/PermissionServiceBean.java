package edu.harvard.iq.dataverse;

import com.google.common.collect.Sets;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
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
import edu.harvard.iq.dataverse.persistence.user.RoleAssignmentRepository;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.ArrayList;
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
            = EnumSet.copyOf(Arrays.stream(Permission.values())
                                     .filter(Permission::requiresAuthenticatedUser)
                                     .collect(toList()));

    private static final Set<Permission> WRITE_PERMISSIONS
            = EnumSet.copyOf(Arrays.stream(Permission.values())
                    .filter(Permission::isRequiresWrite)
                    .collect(toList()));

    private DataverseRoleServiceBean roleService;
    private RoleAssigneeServiceBean roleAssigneeService;
    private DataverseDao dataverseDao;
    private DvObjectServiceBean dvObjectServiceBean;
    private GroupServiceBean groupService;
    private SystemConfig systemConfig;
    private ConfirmEmailServiceBean confirmEmailService;
    private RoleAssignmentRepository roleAssignmentRepository;

    // -------------------- CONSTRUCTORS --------------------

    public PermissionServiceBean() { }

    @Inject
    public PermissionServiceBean(DataverseRoleServiceBean roleService, RoleAssigneeServiceBean roleAssigneeService,
                                 DataverseDao dataverseDao, DvObjectServiceBean dvObjectServiceBean,
                                 GroupServiceBean groupService, SystemConfig systemConfig,
                                 ConfirmEmailServiceBean confirmEmailService, RoleAssignmentRepository roleAssignmentRepository) {
        this.roleService = roleService;
        this.roleAssigneeService = roleAssigneeService;
        this.dataverseDao = dataverseDao;
        this.dvObjectServiceBean = dvObjectServiceBean;
        this.groupService = groupService;
        this.systemConfig = systemConfig;
        this.confirmEmailService = confirmEmailService;
        this.roleAssignmentRepository = roleAssignmentRepository;
    }

    // -------------------- LOGIC --------------------

    public List<RoleAssignment> assignmentsOn(DvObject dvObject) {
        return roleAssignmentRepository.findByDefinitionPointId(dvObject.getId());
    }

    public List<DvObject> whichChildrenHasPermissionsForOrReleased(DataverseRequest request, DvObjectContainer objectContainer,
                                                                   Set<Permission> required) {
        return whichChildrenHasPermissionsFor(request, objectContainer, required, true);
    }

    /**
     * Finds all the permissions the {@link User} in {@code request} has over
     * {@code dvObject}, in the context of {@code request}.
     *
     * @return Permissions of {@code request.getUser()} over {@code dvObject}.
     */
    public Set<Permission> permissionsFor(DataverseRequest request, DvObject dvObject) {
        User user = request.getUser();
        if (user.isSuperuser()) {
            if (systemConfig.isReadonlyMode()) {
              Set<Permission> readonlyPermissions = EnumSet.allOf(Permission.class);
              readonlyPermissions.removeAll(WRITE_PERMISSIONS);
              return readonlyPermissions;
            }
            return EnumSet.allOf(Permission.class);
        }

        Set<Permission> permissions = getInferredPermissions(dvObject);

        // Add permissions gained from ras
        Set<RoleAssignee> ras = new HashSet<>(groupService.groupsFor(request, dvObject));
        ras.add(user);
        addGroupPermissionsFor(ras, dvObject, permissions);

        if (systemConfig.isReadonlyMode() || confirmEmailService.hasEffectivelyUnconfirmedMail(user)) {
            permissions.removeAll(WRITE_PERMISSIONS);
        }
        if (!user.isAuthenticated()) {
            permissions.removeAll(PERMISSIONS_FOR_AUTHENTICATED_USERS_ONLY);
        }
        return permissions;
    }

    public boolean isUserAllowedOn(User user, Command<?> command, DvObject dvObject) {
        Map<String, Set<Permission>> required = command.getRequiredPermissions();
        return isUserAllowedOn(user, required, dvObject);
    }

    public StaticPermissionQuery userOn(RoleAssignee assignee, DvObject dvObject) {
        if (assignee == null) {
            // get guest user for dataverse dvObject
            assignee = GuestUser.get();
        }
        return new StaticPermissionQuery(assignee, dvObject);
    }

    public RequestPermissionQuery requestOn(DataverseRequest request, DvObject dvObject) {
        if (dvObject.getId() == null) {
            throw new IllegalArgumentException("Cannot query permissions on a DvObject with a null id.");
        }
        return new RequestPermissionQuery(dvObject, request);
    }

    /**
     * Go from (User, Permission) to a list of Dataverse objects that the user
     * has the permission on.
     *
     * @return The list of dataverses {@code user} has permission
     * {@code permission} on.
     */
    public List<Dataverse> getDataversesUserHasPermissionOn(AuthenticatedUser user, Permission permission) {
        Set<Group> groups = groupService.groupsFor(user);
        List<String> identifiers = new ArrayList<>();
        if (user != null) {
            identifiers.add(user.getIdentifier());
            identifiers.addAll(groups.stream().map(Group::getIdentifier).collect(Collectors.toSet()));
        }
        List<Integer> dataverseIdsToCheck = roleAssignmentRepository.findDataversesWithUserPermitted(identifiers);
        List<Dataverse> dataversesUserHasPermissionOn = new LinkedList<>();
        for (int dvIdAsInt : dataverseIdsToCheck) {
            Dataverse dataverse = dataverseDao.find((long) dvIdAsInt);
            if (userOn(user, dataverse).has(permission)) {
                dataversesUserHasPermissionOn.add(dataverse);
            }
        }
        return dataversesUserHasPermissionOn;
    }

    public List<AuthenticatedUser> getUsersWithPermissionOn(Permission permission, DvObject dvObject) {
        List<AuthenticatedUser> usersHasPermissionOn = new LinkedList<>();
        Set<RoleAssignment> roleAssignments = roleService.rolesAssignments(dvObject);
        for (RoleAssignment assignment : roleAssignments) {
            if (assignment.getRole().permissions().contains(permission)) {
                RoleAssignee assignee = roleAssigneeService.getRoleAssignee(assignment.getAssigneeIdentifier());
                usersHasPermissionOn.addAll(roleAssigneeService.getExplicitUsers(assignee));
            }
        }
        return usersHasPermissionOn;
    }

    public Map<String, AuthenticatedUser> getDistinctUsersWithPermissionOn(Permission permission, DvObject dvObject) {

        List<AuthenticatedUser> users = getUsersWithPermissionOn(permission, dvObject);
        Map<String, AuthenticatedUser> distinctUsers = new HashMap<>();
        users.forEach(u -> distinctUsers.put(u.getIdentifier(), u));

        return distinctUsers;
    }

    public boolean checkEditDatasetLock(Dataset dataset, DataverseRequest dataverseRequest, Command<?> command)
            throws IllegalCommandException {
        if (!dataset.isLocked()) {
            return false;
        }
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
        return false;
    }

    public void checkDownloadFileLock(Dataset dataset, DataverseRequest dataverseRequest, Command<?> command)
            throws IllegalCommandException {
        if (!dataset.isLocked()) {
            return;
        }
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
     * Returns roles that are effective for {@code authenticatedUser}
     * over {@code dvObject}. Traverses the containment hierarchy of the {@code d}.
     * Takes into consideration all groups that {@code authenticatedUser} is part of.
     *
     * @param authenticatedUser    The authenticated user whose role assignments we look for.
     * @param dvObject The Dataverse object over which the roles are assigned
     * @return A set of all the role assignments for {@code ra} over {@code d}.
     */
    public Set<RoleAssignment> getRolesOfUser(AuthenticatedUser authenticatedUser, DvObject dvObject) {

        Set<RoleAssignment> roleAssignments = assignmentsFor(authenticatedUser, dvObject);

        Set<Group> groupsUserBelongsTo = groupService.groupsFor(authenticatedUser, dvObject);
        for (Group g : groupsUserBelongsTo) {
            roleAssignments.addAll(assignmentsFor(g, dvObject));
        }

        return roleAssignments;
    }

    // -------------------- PRIVATE --------------------

    /**
     * Returns all the children (direct descendants) of {@code objectContainer}, on which the user
     * has all the permissions specified in {@code permissions}. This method takes into
     * account which permissions apply for which object type, so a permission that
     * applies only to {@link Dataset}s will not be considered when looking into
     * the question of whether a {@link Dataverse} should be contained in the output list.
     *
     * @param request             The request whose permissions are queried
     * @param objectContainer             The objects whose children we list
     * @param required        (sub)set of permissions {@code request} has on the objects in the returned list
     * @param includeReleased include released dataverses and datasets without checking permissions
     * @return list of {@code objectContainer} children over which {@code request} has at least {@code required} permissions.
     */
    private List<DvObject> whichChildrenHasPermissionsFor(DataverseRequest request, DvObjectContainer objectContainer,
                                                          Set<Permission> required, boolean includeReleased) {
        List<DvObject> children = dvObjectServiceBean.findByOwnerId(objectContainer.getId());
        User user = request.getUser();

        if (user.isSuperuser()) {
            return children;
        } else if (!user.isAuthenticated() && required.stream().anyMatch(PERMISSIONS_FOR_AUTHENTICATED_USERS_ONLY::contains)) {
            // At least one of the permissions requires that the user is authenticated, which is not the case.
            return Collections.emptyList();
        }

        // Actually look at permissions
        Set<DvObject> parents = getPermissionAncestors(objectContainer);
        Set<RoleAssignee> roleAssignees = new HashSet<>(groupService.groupsFor(request));
        roleAssignees.add(user);
        List<RoleAssignment> parentsAsignments = roleService.directRoleAssignmentsByAssigneesAndDvObjects(roleAssignees, parents);

        boolean unconfirmedMailMode = confirmEmailService.hasEffectivelyUnconfirmedMail(user);

        for (RoleAssignment assignment : parentsAsignments) {
            Set<Permission> permissions = unconfirmedMailMode
                    ? assignment.getRole().permissions()
                    .stream()
                    .filter(p -> !p.isRequiresWrite())
                    .collect(Collectors.toSet())
                    : assignment.getRole().permissions();
            required.removeAll(permissions);
        }
        if (required.isEmpty()) {
            // All permissions are met by role assignments on the request
            return children;
        }

        // Looking at each child at a time now.
        // 1. Map childs to permissions
        List<RoleAssignment> childrenAssignments = roleService.directRoleAssignmentsByAssigneesAndDvObjects(roleAssignees,
                includeReleased ? children.stream().filter(child -> !child.isReleased()).collect(toList()) : children);

        Map<DvObject, Set<Permission>> roleMap = new HashMap<>();
        for (RoleAssignment assignment : childrenAssignments) {
            DvObject definitionPoint = assignment.getDefinitionPoint();
            if (roleMap.containsKey(definitionPoint)) {
                roleMap.get(definitionPoint).addAll(assignment.getRole().permissions());
            } else {
                roleMap.put(definitionPoint, assignment.getRole().permissions());
            }
        }

        // 2. Filter by permission map created at (1).
        return children.stream()
                .filter(c -> (includeReleased && c.isReleased())
                        || hasPermissions(required, roleMap, c, unconfirmedMailMode))
                .collect(toList());
    }

    private boolean hasPermissions(Set<Permission> required, Map<DvObject, Set<Permission>> roleMap, DvObject child,
                                   boolean unconfirmedMailMode) {
        Set<Permission> permissionsApplicableToObject = required.stream()
                .filter(p -> p.appliesTo(child.getClass()))
                .collect(Collectors.toSet());
        return (roleMap.containsKey(child)
                && roleMap.get(child).containsAll(permissionsApplicableToObject)
                && unconfirmedMailMode)
                ? permissionsApplicableToObject.stream().noneMatch(Permission::isRequiresWrite)
                : true;
    }

    private boolean hasPermissionsFor(DataverseRequest request, DvObject dvObject, Set<Permission> required) {
        if ((systemConfig.isReadonlyMode() || confirmEmailService.hasEffectivelyUnconfirmedMail(request.getUser()))
                && required.stream().anyMatch(WRITE_PERMISSIONS::contains)) {
            return false;
        }
        User user = request.getUser();
        if (user.isSuperuser()) {
            return true;
        } else if (!user.isAuthenticated()) {
            Set<Permission> requiredCopy = EnumSet.copyOf(required);
            requiredCopy.retainAll(PERMISSIONS_FOR_AUTHENTICATED_USERS_ONLY);
            if (!requiredCopy.isEmpty()) {
                return false;
            }
        }

        Set<RoleAssignee> ras = new HashSet<>(groupService.groupsFor(request, dvObject));
        ras.add(user);
        return hasGroupPermissionsFor(ras, dvObject, required);
    }

    private boolean hasPermissionsFor(RoleAssignee roleAssignee, DvObject dvObject, Set<Permission> required) {
        boolean unconfirmedEmail = roleAssignee instanceof User
                && confirmEmailService.hasEffectivelyUnconfirmedMail((User) roleAssignee);
        if ((systemConfig.isReadonlyMode() || unconfirmedEmail)
                && required.stream().anyMatch(WRITE_PERMISSIONS::contains)) {
            return false;
        }

        if (roleAssignee instanceof User) {
            User user = (User) roleAssignee;
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
        required.removeAll(getInferredPermissions(dvObject));
        if (required.isEmpty()) {
            return true;
        }

        Set<RoleAssignee> ras = new HashSet<>(groupService.groupsFor(roleAssignee, dvObject));
        ras.add(roleAssignee);
        return hasGroupPermissionsFor(ras, dvObject, required);
    }

    private boolean hasGroupPermissionsFor(Set<RoleAssignee> roleAssignees, DvObject dvObject, Set<Permission> required) {
        for (RoleAssignment asmnt : assignmentsFor(roleAssignees, dvObject)) {
            required.removeAll(asmnt.getRole().permissions());
        }
        return required.isEmpty();
    }

    private void addGroupPermissionsFor(Set<RoleAssignee> roleAssignees, DvObject dvObject, Set<Permission> permissions) {
        for (RoleAssignment assignment : assignmentsFor(roleAssignees, dvObject)) {
            permissions.addAll(assignment.getRole().permissions());
        }
    }


    /**
     * Calculates permissions based on object state and other context
     */
    private Set<Permission> getInferredPermissions(DvObject dvObject) {
        Set<Permission> permissions = EnumSet.noneOf(Permission.class);

        if (isPubliclyDownloadable(dvObject)) {
            permissions.add(Permission.DownloadFile);
        }

        return permissions;
    }

    /**
     * unrestricted files that are part of a release dataset automatically get
     * download permission for everybody:
     */
    private boolean isPubliclyDownloadable(DvObject dvObject) {
        if (dvObject instanceof DataFile) {
            // unrestricted files that are part of a release dataset
            // automatically get download permission for everybody:
            //      -- L.A. 4.0 beta12

            DataFile df = (DataFile) dvObject;
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
     * Returns all the role assignments that are effective for {@code assignee} over
     * {@code dvObject}. Traverses the containment hierarchy of the {@code dvObject}.
     *
     * @param assignee The role assignee whose role assignemnts we look for.
     * @param dvObject  The dataverse object over which the roles are assigned
     * @return A set of all the role assignments for {@code assignee} over {@code dvObject}.
     */
    private Set<RoleAssignment> assignmentsFor(RoleAssignee assignee, DvObject dvObject) {
        return assignmentsFor(Collections.singleton(assignee), dvObject);
    }

    private Set<RoleAssignment> assignmentsFor(Set<RoleAssignee> roleAssignees, DvObject dvObject) {
        Set<DvObject> permAncestors = getPermissionAncestors(dvObject);
        return new HashSet<>(roleService.directRoleAssignmentsByAssigneesAndDvObjects(roleAssignees, permAncestors));
    }

    private Set<DvObject> getPermissionAncestors(DvObject dvObject) {
        Set<DvObject> ancestors = new HashSet<>();
        DvObject currentDvObject = dvObject;
        while (currentDvObject != null) {
            ancestors.add(currentDvObject);
            if (currentDvObject instanceof Dataverse && currentDvObject.isEffectivelyPermissionRoot()) {
                return ancestors;
            }
            currentDvObject = currentDvObject.getOwner();
        }
        return ancestors;
    }

    private boolean isUserAllowedOn(User user, Map<String, Set<Permission>> required, DvObject dvObject) {
        if (required.isEmpty() || required.get("") == null) {
            logger.debug("IsUserAllowedOn: empty-true");
            return true;
        } else {
            Set<Permission> requiredPermissionSet = required.get("");
            return hasPermissionsFor(user, dvObject, requiredPermissionSet);
        }
    }

    // -------------------- INNER CLASSES --------------------

    /**
     * A request-level permission query (e.g includes IP ras).
     */
    public class RequestPermissionQuery {

        private final DvObject subject;
        private final DataverseRequest request;

        // -------------------- CONSTRUCTORS --------------------

        private RequestPermissionQuery(DvObject subject, DataverseRequest request) {
            this.subject = subject;
            this.request = request;
        }

        // -------------------- LOGIC --------------------

        public boolean has(Permission permission) {
            return hasPermissionsFor(request, subject, EnumSet.of(permission));
        }

        /*
         * This is a new and optimized method, for making a quick lookup on
         * a SET of permission all at once; it was originally called
         * has(Set<Permission> permissions)... however, while unambiguos in Java,
         * the fact that there were 2 has() methods - has(Permission) and
         * has(Set<Permission>) - was confusing PrimeFaces and resulting in
         * pages failing with "cannot convert "String" to "Set" error messages...
         * so it had to be renamed to hasPermissions(...)
         */
        public boolean hasPermissions(Set<Permission> permissions) {
            if (permissions.isEmpty()) {
                return true;
            }
            return hasPermissionsFor(request, subject, permissions);
        }

        /**
         * Tests whether a command of the passed class can be issued over the
         * {@link DvObject} in the context of the current request. Note that
         * since some commands have dynamic permissions, in some cases it's
         * better to instantiate a command object and pass it to
         * {@link #canIssue(edu.harvard.iq.dataverse.engine.command.Command)}.
         *
         * @return {@code true} iff instances of the command class can be issued
         * in the context of the current request.
         */
        public boolean canIssue(Class<? extends Command<?>> commandClass) {
            Map<String, Set<Permission>> required = CH.permissionsRequired(commandClass);
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
         * @return {@code true} iff the command can be issued in the context of
         * the current request.
         */
        public boolean canIssue(Command<?> command) {
            Map<String, Set<Permission>> required = command.getRequiredPermissions();
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

        public boolean has(Permission permission) {
            return hasPermissionsFor(user, subject, EnumSet.of(permission));
        }

    }
}
