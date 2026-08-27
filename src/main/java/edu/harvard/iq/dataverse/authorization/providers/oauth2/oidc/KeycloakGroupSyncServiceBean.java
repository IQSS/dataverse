package edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc;

import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserLookup;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.settings.JvmSettings;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.json.JsonObject;

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

    @EJB
    KeycloakGroupMembershipWriter membershipWriter;

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

    /**
     * Reconcile every tenant against Keycloak, for users who are not logging in.
     * <p>
     * The login sync only ever sees the person walking through the door. This sweep is what
     * catches the rest: someone demoted in Keycloak who never comes back, a change made
     * directly in the Keycloak console, a tenant whose attribute was fixed after the fact.
     * <p>
     * Users who exist in Keycloak but have never logged into Dataverse have no account here
     * yet, so they cannot be added to anything. Their first login handles them.
     *
     * @return a short human-readable report of what happened
     */
    public String reconcileAll() {
        if (!isEnabled()) {
            return "Keycloak group sync is disabled.";
        }
        if (!keycloakAdmin.isConfigured()) {
            return "Keycloak group sync is enabled but not configured; nothing done.";
        }

        // Read everything from Keycloak first, and only then write. A partial read must not
        // turn into a partial revocation.
        String tenantsPath = KeycloakAdminClient.normalisePath(parentGroup() + "/" + tenantsGroup());
        List<JsonObject> tenants = keycloakAdmin.getSubgroups(tenantsPath);
        if (tenants.isEmpty()) {
            logger.warning("Keycloak reported no tenants under " + tenantsPath
                    + ". Doing nothing rather than risk revoking everyone.");
            return "No tenants found under " + tenantsPath + "; nothing done.";
        }

        Map<String, Set<String>> desiredMembers = new HashMap<>();
        int unreadable = 0;
        int skippedTenants = 0;

        for (JsonObject tenant : tenants) {
            String tenantName = tenant.getString("name", null);
            if (tenantName == null) {
                continue;
            }
            String tenantPath = tenantsPath + "/" + tenantName;
            Optional<Dataverse> collection = resolveCollection(tenantPath);
            if (collection.isEmpty()) {
                skippedTenants++;
                continue;
            }
            Dataverse dataverse = collection.get();
            provisionIfNeeded(dataverse, null);

            for (JsonObject subgroup : keycloakAdmin.getSubgroups(tenantPath)) {
                Optional<TenantRole> role = TenantRole.fromKeycloakGroupName(subgroup.getString("name", ""));
                String subgroupId = subgroup.getString("id", null);
                if (role.isEmpty() || subgroupId == null) {
                    continue;
                }
                ExplicitGroup group = explicitGroupService.findInOwner(dataverse.getId(),
                        groupAliasInOwner(role.get()));
                if (group == null) {
                    continue;
                }
                Optional<List<JsonObject>> members = keycloakAdmin.getGroupMembers(subgroupId);
                if (members.isEmpty()) {
                    // Could not read this group. Leave its membership exactly as it is.
                    logger.warning("Could not read the members of " + tenantPath + "/"
                            + role.get().keycloakGroupName + "; leaving " + group.getAlias() + " untouched.");
                    unreadable++;
                    continue;
                }
                String what = tenantPath + "/" + role.get().keycloakGroupName;
                Optional<Set<String>> resolved = identifiersOf(members.get(), what);
                if (resolved.isEmpty()) {
                    unreadable++;
                    continue;
                }
                desiredMembers.put(group.getAlias(), resolved.get());
            }
        }

        String report = applyMembership(desiredMembers)
                + reconcileSuperuserSweep()
                + (skippedTenants > 0 ? ", " + skippedTenants + " tenant(s) skipped" : "")
                + (unreadable > 0 ? ", " + unreadable + " group(s) unreadable and left untouched" : "");
        logger.info("Keycloak reconciliation: " + report);
        return report;
    }

    /**
     * Map Keycloak members onto Dataverse accounts. Members who have never logged in have no
     * account yet and are dropped here.
     */
    /**
     * Map Keycloak members onto Dataverse accounts, refusing to answer at all when a non-empty
     * group maps to nothing.
     * <p>
     * This is the guard that matters most. "The group has 20 members, none of whom I can find"
     * is never a legitimate answer -- it means the provider id, the realm or the subject claim
     * is wrong. Reporting it as an empty group would revoke every one of those 20 people.
     *
     * @return the resolved accounts, or empty when the mapping is not trustworthy
     */
    private Optional<List<AuthenticatedUser>> resolveMembers(List<JsonObject> keycloakMembers, String what) {
        List<AuthenticatedUser> users = usersOf(keycloakMembers);
        if (!keycloakMembers.isEmpty() && users.isEmpty()) {
            logger.severe("Keycloak reports " + keycloakMembers.size() + " member(s) in " + what
                    + " but none of them match a Dataverse account. Refusing to treat this as an empty"
                    + " group. Check dataverse.auth.oidc.sync.provider-id and the realm: the accounts"
                    + " are looked up by their subject under provider id(s) " + oidcProviderIds() + ".");
            return Optional.empty();
        }
        return Optional.of(users);
    }

    private List<AuthenticatedUser> usersOf(List<JsonObject> keycloakMembers) {
        List<AuthenticatedUser> users = new ArrayList<>();
        for (JsonObject member : keycloakMembers) {
            String subject = member.getString("id", null);
            if (subject == null) {
                continue;
            }
            AuthenticatedUser user = lookupBySubject(subject);
            if (user == null) {
                logger.fine(() -> "Keycloak user " + subject + " has never logged into Dataverse; skipping.");
                continue;
            }
            users.add(user);
        }
        return users;
    }

    private Optional<Set<String>> identifiersOf(List<JsonObject> keycloakMembers, String what) {
        return resolveMembers(keycloakMembers, what)
                .map(users -> users.stream()
                        .map(AuthenticatedUser::getIdentifier)
                        .collect(Collectors.toSet()));
    }

    /**
     * Write the computed membership, but refuse to do it if the change looks like a wipe.
     * A bad group path or a Keycloak reorganisation should not be able to strip everyone's
     * permissions in one sweep; it should page a human instead.
     */
    private String applyMembership(Map<String, Set<String>> desiredMembers) {
        Map<ExplicitGroup, Set<String>> removals = new HashMap<>();
        Map<ExplicitGroup, Set<String>> additions = new HashMap<>();
        int currentTotal = 0;

        for (Map.Entry<String, Set<String>> entry : desiredMembers.entrySet()) {
            ExplicitGroup group = explicitGroupService.getProvider().get(entry.getKey());
            if (group == null) {
                continue;
            }
            Set<String> current = group.getContainedRoleAssgineeIdentifiers();
            currentTotal += current.size();

            Set<String> toRemove = new HashSet<>(current);
            toRemove.removeAll(entry.getValue());
            if (!toRemove.isEmpty()) {
                removals.put(group, toRemove);
            }
            Set<String> toAdd = new HashSet<>(entry.getValue());
            toAdd.removeAll(current);
            if (!toAdd.isEmpty()) {
                additions.put(group, toAdd);
            }
        }

        int removalCount = removals.values().stream().mapToInt(Set::size).sum();
        int additionCount = additions.values().stream().mapToInt(Set::size).sum();

        // A sweep that empties everything it looked at is a configuration failure, not a
        // legitimate change -- and the absolute floor must not license it on a small
        // installation, which is exactly how this got through once.
        if (currentTotal > 0 && removalCount >= currentTotal && additionCount == 0) {
            logger.severe("Keycloak reconciliation would remove every one of the " + currentTotal
                    + " managed memberships and add none. Refusing: this is a configuration failure,"
                    + " not a legitimate change. Check the group layout and provider id, then re-run.");
            return "aborted: the sweep would empty every managed group";
        }
        int limit = removalLimit(currentTotal);
        if (removalCount > limit) {
            logger.severe("Keycloak reconciliation would remove " + removalCount + " of " + currentTotal
                    + " memberships, over the safety limit of " + limit + ". Refusing to write anything. "
                    + "Check the Keycloak group layout and the sync configuration, then re-run.");
            return "aborted: " + removalCount + " removals exceed the safety limit of " + limit;
        }

        // One transaction per group, so a failure on one tenant leaves the others reconciled.
        Set<ExplicitGroup> touched = new HashSet<>();
        touched.addAll(removals.keySet());
        touched.addAll(additions.keySet());

        int written = 0;
        int failed = 0;
        for (ExplicitGroup group : touched) {
            boolean ok = membershipWriter.apply(group.getAlias(),
                    removals.getOrDefault(group, Set.of()),
                    additions.getOrDefault(group, Set.of()));
            if (ok) {
                written++;
            } else {
                failed++;
            }
        }

        return written + " group(s) updated (" + removalCount + " removals, "
                + additionCount + " additions)"
                + (failed > 0 ? ", " + failed + " group(s) failed" : "");
    }

    /**
     * Reconcile the superuser flag from the platform-wide admins group.
     * <p>
     * Note this updates the database only. Someone with a live session keeps the flag until
     * that session ends, because it is read from the user object held in the session.
     */
    private String reconcileSuperuserSweep() {
        Optional<JsonObject> adminGroup = keycloakAdmin.getGroupByPath(
                parentGroup() + "/" + superuserGroup());
        if (adminGroup.isEmpty()) {
            return "";
        }
        String groupId = adminGroup.get().getString("id", null);
        if (groupId == null) {
            return "";
        }
        Optional<List<JsonObject>> members = keycloakAdmin.getGroupMembers(groupId);
        if (members.isEmpty()) {
            logger.warning("Could not read the members of the platform admins group; "
                    + "leaving superuser flags untouched.");
            return "";
        }
        Optional<List<AuthenticatedUser>> resolved = resolveMembers(members.get(),
                parentGroup() + "/" + superuserGroup());
        if (resolved.isEmpty()) {
            logger.severe("Leaving every superuser flag untouched.");
            return "";
        }
        List<AuthenticatedUser> shouldBeSuperuser = resolved.get();
        Set<String> shouldBeSuperuserIds = shouldBeSuperuser.stream()
                .map(AuthenticatedUser::getIdentifier)
                .collect(Collectors.toSet());

        // Only ever demote accounts that authenticate through the provider we are syncing.
        // Keycloak is the source of truth for its own users, not for builtin accounts or
        // service accounts belonging to other providers.
        Set<String> ownedProviders = oidcProviderIds();
        List<AuthenticatedUser> demotable = authenticationService.findSuperUsers().stream()
                .filter(user -> !shouldBeSuperuserIds.contains(user.getIdentifier()))
                .filter(user -> !protectedUsers().contains(user.getUserIdentifier()))
                .filter(user -> {
                    AuthenticatedUserLookup lookup = user.getAuthenticatedUserLookup();
                    if (lookup == null || !ownedProviders.contains(lookup.getAuthenticationProviderId())) {
                        logger.fine(() -> "Not demoting " + user.getIdentifier()
                                + ": not an account of a synchronised provider.");
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        int limit = removalLimit(demotable.size() + shouldBeSuperuser.size());
        if (demotable.size() > limit) {
            logger.severe("Keycloak reconciliation would revoke superuser from " + demotable.size()
                    + " account(s), over the safety limit of " + limit + ": "
                    + demotable.stream().map(AuthenticatedUser::getIdentifier).collect(Collectors.toList())
                    + ". Refusing. Check the '" + superuserGroup() + "' group before re-running.");
            return ", superuser changes aborted (" + demotable.size() + " revocations exceed the limit of " + limit + ")";
        }

        int changed = 0;
        for (AuthenticatedUser user : demotable) {
            user.setSuperuser(false);
            userService.save(user);
            logger.info("Keycloak reconciliation: revoked superuser status for " + user.getIdentifier());
            changed++;
        }
        for (AuthenticatedUser user : shouldBeSuperuser) {
            if (!user.isSuperuser()) {
                user.setSuperuser(true);
                userService.save(user);
                logger.info("Keycloak reconciliation: granted superuser status for " + user.getIdentifier());
                changed++;
            }
        }
        return changed > 0 ? ", " + changed + " superuser change(s)" : "";
    }

    /**
     * Ids of the authentication providers whose accounts this sync owns.
     * <p>
     * Never guess this. A provider configured through MicroProfile Config -- the usual case --
     * is registered as {@code oidc-mpconfig}, not {@code oidc}, and guessing wrong makes every
     * account lookup return null, which reads as "nobody is in any group" and revokes
     * everyone. So ask the registry which OIDC providers actually exist.
     */
    Set<String> oidcProviderIds() {
        Optional<String> configured = JvmSettings.OIDC_SYNC_PROVIDER_ID.lookupOptional();
        if (configured.isPresent()) {
            return Set.of(configured.get());
        }
        return authenticationService.getAuthenticationProviderIdsOfType(OIDCAuthProvider.class);
    }

    private AuthenticatedUser lookupBySubject(String subject) {
        for (String providerId : oidcProviderIds()) {
            AuthenticatedUser user = authenticationService.lookupUser(providerId, subject);
            if (user != null) {
                return user;
            }
        }
        return null;
    }

    /**
     * How many memberships a single sweep is allowed to remove.
     * <p>
     * A bad group path or a Keycloak reorganisation should not be able to strip everyone's
     * permissions in one pass. The absolute floor keeps small installations workable, where any
     * ratio would be too tight to ever remove anybody.
     */
    int removalLimit(int currentTotal) {
        return Math.max(minRemovals(), (int) Math.ceil(removalRatio() * currentTotal));
    }

    private int minRemovals() {
        return JvmSettings.OIDC_SYNC_MIN_REMOVALS.lookupOptional(Integer.class).orElse(5);
    }

    private double removalRatio() {
        return JvmSettings.OIDC_SYNC_MAX_REMOVAL_RATIO.lookupOptional(Double.class).orElse(0.2);
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
        User attributedTo = adminUser != null ? adminUser : actingUser;
        if (attributedTo == null) {
            // No superuser yet and no one logged in (a timer sweep on a fresh installation).
            // The request's user must not be null: the assignment history dereferences it.
            attributedTo = GuestUser.get();
        }
        return new DataverseRequest(attributedTo, IpAddress.valueOf("0.0.0.0"));
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
