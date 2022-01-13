package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.common.RoleTranslationUtil;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserRepository;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserRepository.SortKey;

@Stateless
public class UserServiceBean {

    private static final Logger logger = Logger.getLogger(UserServiceBean.class.getCanonicalName());

    @PersistenceContext
    private EntityManager em;

    @Inject
    private AuthenticatedUserRepository authenticatedUserRepository;

    // -------------------- LOGIC --------------------

    public AuthenticatedUser find(Object pk) {
        return em.find(AuthenticatedUser.class, pk);
    }

    public AuthenticatedUser save(AuthenticatedUser user) {
        if (user.getId() == null) {
            em.persist(this);
        } else {
            if (user.getCreatedTime() == null) {
                user.setCreatedTime(new Timestamp(new Date().getTime())); // default new creation time
                user.setLastLoginTime(user.getCreatedTime()); // sets initial lastLoginTime to creation time
                logger.info("Creation time null! Setting user creation time to now");
            }
            user = em.merge(user);
        }
        em.flush();

        return user;
    }

    /**
     * Return the user information as a List of AuthenticatedUser objects -- easier to work with in the UI
     * - With Role added as a transient field
     */
    public List<AuthenticatedUser> getAuthenticatedUserList(String searchTerm, String sortKey,
                                                            boolean isSortAscending, Integer resultLimit, Integer offset) {
        offset = offset == null || offset < 0 ? Integer.valueOf(0) : offset;

        List<AuthenticatedUser> userResults = getUserListCore(searchTerm, sortKey, isSortAscending, resultLimit, offset);

        Map<String, List<String>> roleLookup = retrieveRolesForUsers(userResults);

        List<AuthenticatedUser> viewObjects = new ArrayList<>();
        for (AuthenticatedUser userInfo : userResults) {
            List<String> roleList = roleLookup.getOrDefault("@" + userInfo.getUserIdentifier(), Collections.emptyList());
            AuthenticatedUser singleUser = addAuthenticatedUserRoles(userInfo, String.join(", ", roleList));
            viewObjects.add(singleUser);
        }

        return viewObjects;
    }

    /**
     * Return the number of superusers -- for the dashboard
     */
    public Long getSuperUserCount() {
        String qstr = "SELECT count(au) FROM AuthenticatedUser au WHERE au.superuser = :superuserTrue";
        Query query = em.createQuery(qstr);
        query.setParameter("superuserTrue", true);

        return (Long) query.getSingleResult();
    }

    public Long getTotalUserCount() {
        return getUserCount("");
    }

    public Long getUserCount(String searchTerm) {
        if (StringUtils.isEmpty(searchTerm)) {
            searchTerm = "";
        }
        return authenticatedUserRepository.countSearchedAuthenticatedUsers(searchTerm.trim());
    }

    public AuthenticatedUser updateLastLogin(AuthenticatedUser user) {
        //assumes that AuthenticatedUser user already exists
        user.setLastLoginTime(new Timestamp(new Date().getTime()));
        return save(user);
    }

    public AuthenticatedUser updateLastApiUseTime(AuthenticatedUser user) {
        //assumes that AuthenticatedUser user already exists
        user.setLastApiUseTime(new Timestamp(new Date().getTime()));
        return save(user);
    }

    // -------------------- PRIVATE --------------------

    private AuthenticatedUser addAuthenticatedUserRoles(AuthenticatedUser authenticatedUser, String roles) {
        authenticatedUser.setRoles(roles);
        return authenticatedUser;
    }

    /**
     * Attempt to retrieve all the user roles in 1 query
     * Consider putting limits on this -- e.g. no more than 1,000 user identifiers or something similar
     */
    private Map<String, List<String>> retrieveRolesForUsers(List<AuthenticatedUser> userObjectList) {
        // Iterate through results, retrieving only the assignee identifiers
        // Note: userInfo[1], the assigneeIdentifier, cannot be null in the database
        List<String> userIdentifierList = userObjectList.stream()
                .map(AuthenticatedUser::getUserIdentifier)
                .collect(Collectors.toList());

        if (userIdentifierList.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> databaseIds = userObjectList.stream()
                .map(AuthenticatedUser::getId)
                .collect(Collectors.toList());

        // -------------------------------------------------
        // Note: This is not ideal but in this case SQL
        // injection isn't possible b/c the list of assigneeidentifier
        // strings comes from a previous query
        // -------------------------------------------------
        String identifierListString = userIdentifierList.stream()
                .filter(StringUtils::isNotEmpty)
                .map(i -> "'@" + i + "'")
                .collect(Collectors.joining(", "));

        // Create/Run the query to find directly assigned roles
        String qstr = "SELECT distinct a.assigneeidentifier, d.alias, d.name"
              + " FROM roleassignment a, dataverserole d"
              + " WHERE d.id = a.role_id"
              + " AND a.assigneeidentifier IN (" + identifierListString + ")"
              + " ORDER by a.assigneeidentifier, d.alias;";

        Query nativeQuery = em.createNativeQuery(qstr);

        List<Object[]> dbRoleResults = nativeQuery.getResultList();

        Map<String, List<String>> userRoleLookup = new HashMap<>();

        String userIdentifier;
        String userRole;
        for (Object[] dbResultRow : dbRoleResults) {
            userIdentifier = (String) dbResultRow[0];
            userRole = RoleTranslationUtil.getLocaleNameFromAlias((String) dbResultRow[1], (String) dbResultRow[2]);
            List<String> userRoleList = userRoleLookup.getOrDefault(userIdentifier, new ArrayList<>());
            if (!userRoleList.contains(userRole)) {
                userRoleList.add(userRole);
                userRoleLookup.put(userIdentifier, userRoleList);
            }
        }

        // And now the roles assigned via groups:
        // 1. One query for selecting all the groups to which these users may belong:

        Map<String, List<String>> groupsLookup = new HashMap<>();
        String idListString = StringUtils.join(databaseIds, ",");

        // A *RECURSIVE* native query, that finds all the groups that the specified
        // users are part of, BOTH by direct inclusion, AND via parent groups:

        qstr = "WITH RECURSIVE group_user AS ((" +
                " SELECT distinct g.groupalias, g.id, u.useridentifier" +
                "  FROM explicitgroup g, explicitgroup_authenticateduser e, authenticateduser u" +
                "  WHERE e.explicitgroup_id = g.id " +
                "   AND u.id IN (" + idListString + ")" +
                "   AND u.id = e.containedauthenticatedusers_id)" +
                "  UNION\n" +
                "   SELECT p.groupalias, p.id, c.useridentifier" +
                "    FROM group_user c, explicitgroup p, explicitgroup_explicitgroup e" +
                "    WHERE e.explicitgroup_id = p.id" +
                "     AND e.containedexplicitgroups_id = c.id)" +
                "SELECT distinct groupalias, useridentifier FROM group_user;";

        nativeQuery = em.createNativeQuery(qstr);
        List<Object[]> groupResults = nativeQuery.getResultList();

        Set<String> groupIdentifiers = new HashSet<>();

        for (Object[] group : groupResults) {
            String alias = (String) group[0];
            String user = (String) group[1];
            if (alias == null) {
                continue;
            }

            alias = "&explicit/" + alias;
            groupIdentifiers.add("'" + alias + "'");

            List<String> groupUserList = groupsLookup.getOrDefault(alias, new ArrayList<>());
            if (!groupUserList.contains(user)) {
                groupUserList.add(user);
                groupsLookup.put(alias, groupUserList);
            }
        }

        // 2. And now we can make another lookup on the roleassignment table, using the list
        // of the explicit group aliases we have just generated:

        if (groupIdentifiers.isEmpty()) {
            return userRoleLookup;
        }

        qstr = "SELECT distinct a.assigneeidentifier, d.alias, d.name"
             + " FROM roleassignment a, dataverserole d"
             + " WHERE d.id = a.role_id"
             + " AND a.assigneeidentifier IN (" + String.join(", ", groupIdentifiers) + ")"
             + " ORDER by a.assigneeidentifier, d.alias;";

        nativeQuery = em.createNativeQuery(qstr);
        dbRoleResults = nativeQuery.getResultList();
        if (dbRoleResults == null) {
            return userRoleLookup;
        }

        for (Object[] dbResultRow : dbRoleResults) {
            String groupIdentifier = (String) dbResultRow[0];
            String groupRole = RoleTranslationUtil.getLocaleNameFromAlias((String) dbResultRow[1], (String) dbResultRow[2]);
            List<String> groupUserList = groupsLookup.getOrDefault(groupIdentifier, Collections.emptyList());
            for (String groupUserIdentifier : groupUserList) {
                groupUserIdentifier = "@" + groupUserIdentifier;
                List<String> userRoleList = userRoleLookup.getOrDefault(groupUserIdentifier, new ArrayList<>());
                if (!userRoleList.contains(groupRole)) {
                    userRoleList.add(groupRole);
                    userRoleLookup.put(groupUserIdentifier, userRoleList);
                }
            }
        }
        return userRoleLookup;
    }

    /**
     * Run a native query, returning a List<Object[]> containing
     * AuthenticatedUser information as well as information about the
     * Authenticated Provider (e.g. builtin user, etc)
     */
    private List<AuthenticatedUser> getUserListCore(String searchTerm, String sortKey, boolean isSortAscending, Integer resultLimit, Integer offset) {

        SortKey dashboardUserSortKey = parseSortColumn(sortKey);
        resultLimit = resultLimit == null || resultLimit < 1 ? Integer.valueOf(1) : resultLimit;
        offset = offset == null || offset < 0 ? Integer.valueOf(0) : offset;
        searchTerm = StringUtils.isEmpty(searchTerm) ? "" : searchTerm.trim();

        return authenticatedUserRepository.findSearchedAuthenticatedUsers(dashboardUserSortKey,
                resultLimit, offset, searchTerm, isSortAscending);
    }

    private SortKey parseSortColumn(String sortKey) {
        return StringUtils.isEmpty(sortKey) ? SortKey.ID : validateSortColumn(sortKey);
    }

    private SortKey validateSortColumn(String sortKey) {
        return Arrays.stream(SortKey.values())
                .filter(dashboardUserSortKey -> dashboardUserSortKey.equals(SortKey.fromString(sortKey)))
                .findAny()
                .orElse(SortKey.ID);
    }

    private String getStringOrNull(Object dbResult) {
        return dbResult != null ? (String) dbResult : null;
    }
}
