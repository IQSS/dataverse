package edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.settings.JvmSettings;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Keeps a user's Dataverse authorisations in step with their Keycloak group membership.
 * Keycloak is the single source of truth: whatever it says wins, and nothing else feeds
 * this synchronisation.
 * <p>
 * The Keycloak side is expected to look like this:
 * <pre>
 *   /&lt;parent&gt;/admins                              -> Dataverse superuser
 *   /&lt;parent&gt;/&lt;tenants&gt;/&lt;tenant&gt;/admins    -> "admin" role on the tenant's collection
 *   /&lt;parent&gt;/&lt;tenants&gt;/&lt;tenant&gt;/curators  -> "curator" role on the tenant's collection
 *   /&lt;parent&gt;/&lt;tenants&gt;/&lt;tenant&gt;/users     -> "user" role on the tenant's collection
 * </pre>
 * Each {@code <tenant>} group carries an attribute (by default {@code dataverse-alias})
 * holding the alias of the Dataverse collection it maps to.
 * <p>
 * Rather than granting roles to users directly, we mirror each Keycloak tenant role into
 * an {@link ExplicitGroup} owned by the target collection, and grant the Dataverse role to
 * that group once. Syncing a login is then only a matter of adding or removing the user
 * from those groups, which is cheap: group membership is resolved per request and expanded
 * at query time by the search layer, so it needs no permission reindex and takes effect on
 * the user's very next request.
 * <p>
 * Only groups carrying our own prefix are ever touched. Roles and groups an administrator
 * created by hand are left strictly alone.
 */
@Stateless
public class KeycloakGroupSyncServiceBean {

    private static final Logger logger = Logger.getLogger(KeycloakGroupSyncServiceBean.class.getName());

    /**
     * The three Keycloak subgroups we understand under a tenant, in the order they appear
     * in the group tree. Each maps to a Dataverse role and to a mirrored explicit group.
     */
    enum TenantRole {
        ADMIN("admins", JvmSettings.OIDC_SYNC_ROLE_ADMIN, "admin", "Administrators"),
        CURATOR("curators", JvmSettings.OIDC_SYNC_ROLE_CURATOR, "curator", "Curators"),
        USER("users", JvmSettings.OIDC_SYNC_ROLE_USER, "member", "Users");

        private final String keycloakGroupName;
        private final JvmSettings roleAliasSetting;
        private final String defaultRoleAlias;
        private final String displayNameSuffix;

        TenantRole(String keycloakGroupName, JvmSettings roleAliasSetting, String defaultRoleAlias,
                   String displayNameSuffix) {
            this.keycloakGroupName = keycloakGroupName;
            this.roleAliasSetting = roleAliasSetting;
            this.defaultRoleAlias = defaultRoleAlias;
            this.displayNameSuffix = displayNameSuffix;
        }

        static Optional<TenantRole> fromKeycloakGroupName(String name) {
            return Arrays.stream(values())
                    .filter(role -> role.keycloakGroupName.equalsIgnoreCase(name))
                    .findFirst();
        }

        String roleAlias() {
            return roleAliasSetting.lookupOptional().orElse(defaultRoleAlias);
        }
    }

    @EJB
    KeycloakAdminClient keycloakAdmin;

    @EJB
    ExplicitGroupServiceBean explicitGroupService;

    @EJB
    DataverseServiceBean dataverseService;

    @EJB
    DataverseRoleServiceBean roleService;

    @EJB
    AuthenticationServiceBean authenticationService;

    @EJB
    UserServiceBean userService;

    /**
     * Reconcile a user against the group paths Keycloak just told us about, typically at login.
     *
     * @param user       the freshly authenticated user
     * @param groupPaths the full group paths from the IDP's group claim. {@code null} means the
     *                   IDP said nothing about groups, in which case we deliberately change
     *                   nothing -- a missing claim must never look like "member of nothing".
     */
    public void syncUser(AuthenticatedUser user, List<String> groupPaths) {
        if (!isEnabled()) {
            return;
        }
        if (user == null) {
            return;
        }
        if (groupPaths == null) {
            logger.warning("Keycloak group sync is enabled but the IDP sent no '"
                    + OIDCAuthProvider.getGroupsClaimName() + "' claim for " + user.getIdentifier()
                    + ". Leaving this user's authorisations untouched. Check the group membership "
                    + "mapper on the Dataverse client, including its 'Add to userinfo' setting.");
            return;
        }

        try {
            Set<String> desiredGroupAliases = new LinkedHashSet<>();
            Set<Long> provisioned = new HashSet<>();
            for (TenantAssignment assignment : parseTenantAssignments(groupPaths)) {
                Optional<Dataverse> collection = resolveCollection(assignment.tenantGroupPath);
                if (collection.isEmpty()) {
                    continue;
                }
                Dataverse dataverse = collection.get();
                if (provisioned.add(dataverse.getId())) {
                    provisionIfNeeded(dataverse, user);
                }
                ExplicitGroup group = explicitGroupService.findInOwner(dataverse.getId(),
                        groupAliasInOwner(assignment.role));
                if (group == null) {
                    logger.warning("No group " + groupAliasInOwner(assignment.role) + " on collection '"
                            + dataverse.getAlias() + "', so " + assignment.tenantGroupPath
                            + " grants nothing. See the warnings above for why it could not be created.");
                    continue;
                }
                desiredGroupAliases.add(group.getAlias());
            }

            reconcileMembership(user, desiredGroupAliases);
            reconcileSuperuser(user, groupPaths);
        } catch (Exception ex) {
            // A failed sync must never block a login: the user keeps whatever they had.
            logger.log(Level.SEVERE, "Keycloak group sync failed for " + user.getIdentifier()
                    + "; the user's authorisations were left as they were.", ex);
        }
    }

    public boolean isEnabled() {
        return JvmSettings.OIDC_SYNC_ENABLED.lookupOptional(Boolean.class).orElse(false);
    }

    // ------------------------------------------------------- parsing the group claim

    /** One "this user holds this role on this tenant" fact, as read from the group claim. */
    static final class TenantAssignment {
        private final String tenantGroupPath;
        private final TenantRole role;

        private TenantAssignment(String tenantGroupPath, TenantRole role) {
            this.tenantGroupPath = tenantGroupPath;
            this.role = role;
        }

        String getTenantGroupPath() {
            return tenantGroupPath;
        }

        TenantRole getRole() {
            return role;
        }
    }

    /**
     * Turn the raw group paths into tenant assignments, ignoring anything that is not shaped
     * like {@code /<parent>/<tenants>/<tenant>/<role>} (for instance {@code content-admins},
     * which does not concern Dataverse).
     */
    List<TenantAssignment> parseTenantAssignments(List<String> groupPaths) {
        String tenantsPrefix = KeycloakAdminClient.normalisePath(parentGroup() + "/" + tenantsGroup()) + "/";
        List<TenantAssignment> assignments = new ArrayList<>();

        for (String rawPath : groupPaths) {
            if (rawPath == null || rawPath.isBlank()) {
                continue;
            }
            String path = KeycloakAdminClient.normalisePath(rawPath);
            if (!path.startsWith(tenantsPrefix)) {
                continue;
            }
            String[] remainder = path.substring(tenantsPrefix.length()).split("/");
            if (remainder.length != 2) {
                // Either the tenant group itself (no role) or something deeper than we model.
                continue;
            }
            Optional<TenantRole> role = TenantRole.fromKeycloakGroupName(remainder[1]);
            if (role.isEmpty()) {
                logger.fine(() -> "Ignoring unknown Keycloak tenant subgroup: " + path);
                continue;
            }
            assignments.add(new TenantAssignment(tenantsPrefix + remainder[0], role.get()));
        }
        return assignments;
    }

    /**
     * Resolve a tenant group to the Dataverse collection named by its alias attribute.
     * Returns empty -- with a warning -- when the attribute is missing or points nowhere;
     * we never guess a collection.
     */
    private Optional<Dataverse> resolveCollection(String tenantGroupPath) {
        Optional<String> alias = keycloakAdmin.getGroupAttribute(tenantGroupPath, aliasAttribute());
        if (alias.isEmpty()) {
            logger.warning("Keycloak group " + tenantGroupPath + " has no '" + aliasAttribute()
                    + "' attribute, so it cannot be mapped to a Dataverse collection. Skipping it.");
            return Optional.empty();
        }
        Dataverse dataverse = dataverseService.findByAlias(alias.get());
        if (dataverse == null) {
            logger.warning("Keycloak group " + tenantGroupPath + " points at Dataverse collection '"
                    + alias.get() + "', which does not exist. Skipping it.");
            return Optional.empty();
        }
        return Optional.of(dataverse);
    }

    // ------------------------------------------------------- provisioning

    /**
     * Make sure the collection has our three mirrored groups, each holding its Dataverse role.
     * Idempotent, and cheap once everything is in place: three lookups and no writes.
     */
    private void provisionIfNeeded(Dataverse dataverse, AuthenticatedUser actingUser) {
        for (TenantRole tenantRole : TenantRole.values()) {
            ExplicitGroup group = explicitGroupService.findInOwner(dataverse.getId(), groupAliasInOwner(tenantRole));
            if (group == null) {
                group = createGroup(dataverse, tenantRole);
                if (group == null) {
                    continue;
                }
            }
            ensureRoleAssignment(dataverse, group, tenantRole, actingUser);
        }
    }

    private ExplicitGroup createGroup(Dataverse dataverse, TenantRole tenantRole) {
        ExplicitGroup group = new ExplicitGroup(explicitGroupService.getProvider());
        group.setOwner(dataverse);
        group.setGroupAliasInOwner(groupAliasInOwner(tenantRole));
        group.setDisplayName(dataverse.getName() + " " + tenantRole.displayNameSuffix);
        group.setDescription("Managed automatically from Keycloak. Do not edit its members by hand: "
                + "any change will be reverted on the next synchronisation.");
        group.updateAlias();
        ExplicitGroup persisted = explicitGroupService.persist(group);
        logger.info("Created Keycloak-managed group " + persisted.getAlias() + " on collection " + dataverse.getAlias());
        return persisted;
    }

    /**
     * Grant the tenant role to the mirrored group on the collection, once. Note this is the
     * only write in the whole flow that triggers a permission reindex of the subtree, which is
     * why it happens at provisioning time and not on every login.
     */
    private void ensureRoleAssignment(Dataverse dataverse, ExplicitGroup group, TenantRole tenantRole,
                                      AuthenticatedUser actingUser) {
        Optional<DataverseRole> role = findRole(tenantRole.roleAlias(), dataverse);
        if (role.isEmpty()) {
            logger.warning("No Dataverse role with alias '" + tenantRole.roleAlias() + "' is visible from collection '"
                    + dataverse.getAlias() + "'. Members of the Keycloak group " + tenantRole.keycloakGroupName
                    + " for this tenant will get no permissions.");
            return;
        }
        boolean alreadyAssigned = roleService.directRoleAssignments(group, dataverse).stream()
                .anyMatch(assignment -> assignment.getRole().equals(role.get()));
        if (alreadyAssigned) {
            return;
        }
        roleService.save(new RoleAssignment(role.get(), group, dataverse, null), internalRequest(actingUser));
        logger.info("Granted role '" + tenantRole.roleAlias() + "' to group " + group.getAlias()
                + " on collection " + dataverse.getAlias());
    }

    /**
     * Look for a role by alias among those usable at the collection: its own, its ancestors',
     * and the built-in ones.
     * <p>
     * Deliberately does not use {@code findCustomRoleByAliasAndOwner} or
     * {@code findBuiltinRoleByAlias}: both call {@code getSingleResult()}, and a
     * {@code NoResultException} escaping an EJB business method marks the caller's transaction
     * for rollback. Catching it here would not undo that -- every later query in the same
     * transaction would fail with "Client's transaction aborted". A role simply not existing
     * is an ordinary answer, not a reason to lose the transaction.
     */
    private Optional<DataverseRole> findRole(String alias, Dataverse dataverse) {
        return roleService.availableRoles(dataverse.getId()).stream()
                .filter(role -> alias.equals(role.getAlias()))
                .findFirst();
    }

    // ------------------------------------------------------- reconciliation

    /**
     * Bring the user's membership of Keycloak-managed groups in line with what Keycloak says,
     * writing only the difference. A login that changes nothing performs no writes at all.
     */
    private void reconcileMembership(AuthenticatedUser user, Set<String> desiredGroupAliases) {
        Map<String, ExplicitGroup> currentManagedGroups = new HashMap<>();
        for (ExplicitGroup group : explicitGroupService.findDirectlyContainingGroups(user)) {
            if (isManaged(group)) {
                currentManagedGroups.put(group.getAlias(), group);
            }
        }

        Set<String> toRemove = new HashSet<>(currentManagedGroups.keySet());
        toRemove.removeAll(desiredGroupAliases);
        for (String alias : toRemove) {
            ExplicitGroup group = currentManagedGroups.get(alias);
            group.remove(user);
            explicitGroupService.persist(group);
            logger.info("Keycloak sync: removed " + user.getIdentifier() + " from " + alias);
        }

        Set<String> toAdd = new HashSet<>(desiredGroupAliases);
        toAdd.removeAll(currentManagedGroups.keySet());
        for (String alias : toAdd) {
            ExplicitGroup group = explicitGroupService.getProvider().get(alias);
            if (group == null) {
                logger.warning("Keycloak sync: group " + alias + " vanished mid-sync; skipping it.");
                continue;
            }
            group.add(user);
            explicitGroupService.persist(group);
            logger.info("Keycloak sync: added " + user.getIdentifier() + " to " + alias);
        }
    }

    /**
     * A group is ours -- and therefore safe to rewrite -- only if its alias inside its owner
     * carries our prefix. Everything else was made by a human and is left alone.
     */
    private boolean isManaged(ExplicitGroup group) {
        String aliasInOwner = group.getGroupAliasInOwner();
        if (aliasInOwner == null) {
            return false;
        }
        String prefix = groupPrefix() + "-";
        return Arrays.stream(TenantRole.values())
                .anyMatch(role -> aliasInOwner.equals(prefix + role.keycloakGroupName));
    }

    /**
     * Mirror membership of the platform-wide admins group onto the Dataverse superuser flag.
     * <p>
     * Note this only takes effect immediately for the user logging in. Revoking the flag from
     * someone with a live session does not end that session -- the flag is read from the user
     * object held in {@code DataverseSession}.
     */
    private void reconcileSuperuser(AuthenticatedUser user, List<String> groupPaths) {
        boolean shouldBeSuperuser = holdsSuperuserGroup(groupPaths);

        if (user.isSuperuser() == shouldBeSuperuser) {
            return;
        }
        if (!shouldBeSuperuser && protectedUsers().contains(user.getUserIdentifier())) {
            logger.info("Keycloak sync: not revoking superuser from protected user " + user.getIdentifier());
            return;
        }
        user.setSuperuser(shouldBeSuperuser);
        userService.save(user);
        logger.info("Keycloak sync: " + (shouldBeSuperuser ? "granted" : "revoked")
                + " superuser status for " + user.getIdentifier());
    }

    /**
     * Whether the group claim contains the platform-wide admins group.
     */
    boolean holdsSuperuserGroup(List<String> groupPaths) {
        String superuserPath = KeycloakAdminClient.normalisePath(parentGroup() + "/" + superuserGroup());
        return groupPaths.stream()
                .filter(path -> path != null && !path.isBlank())
                .map(KeycloakAdminClient::normalisePath)
                .anyMatch(superuserPath::equals);
    }

    /**
     * Role assignments are recorded against a request. Nothing in this flow is user-initiated,
     * so we attribute it to the admin user to keep the assignment history readable. On an
     * installation that has no superuser yet, fall back to the user being synced: the request's
     * user must not be null, or writing the assignment history would fail.
     */
    private DataverseRequest internalRequest(AuthenticatedUser actingUser) {
        AuthenticatedUser adminUser = authenticationService.getAdminUser();
        return new DataverseRequest(adminUser != null ? adminUser : actingUser, IpAddress.valueOf("0.0.0.0"));
    }

    // ------------------------------------------------------- configuration

    private String groupAliasInOwner(TenantRole role) {
        return groupPrefix() + "-" + role.keycloakGroupName;
    }

    private String parentGroup() {
        return JvmSettings.OIDC_SYNC_PARENT_GROUP.lookupOptional().orElse("platica");
    }

    private String tenantsGroup() {
        return JvmSettings.OIDC_SYNC_TENANTS_GROUP.lookupOptional().orElse("tenant-users");
    }

    private String superuserGroup() {
        return JvmSettings.OIDC_SYNC_SUPERUSER_GROUP.lookupOptional().orElse("admins");
    }

    private String aliasAttribute() {
        return JvmSettings.OIDC_SYNC_ALIAS_ATTRIBUTE.lookupOptional().orElse("dataverse-alias");
    }

    private String groupPrefix() {
        return JvmSettings.OIDC_SYNC_GROUP_PREFIX.lookupOptional().orElse("kc");
    }

    private Set<String> protectedUsers() {
        return Arrays.stream(JvmSettings.OIDC_SYNC_PROTECTED_USERS.lookupOptional().orElse("dataverseAdmin").split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());
    }
}
