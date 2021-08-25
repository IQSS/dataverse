package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.common.RoleTranslationUtil;
import edu.harvard.iq.dataverse.common.UserUtil;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserRepository;
import org.apache.commons.lang.StringUtils;
import org.ocpsoft.common.util.Strings;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserRepository.SortKey;

@Stateless
public class UserServiceBean {

    private static final Logger logger = Logger.getLogger(UserServiceBean.class.getCanonicalName());

    @PersistenceContext
    EntityManager em;

    @Inject
    private AuthenticatedUserRepository authenticatedUserRepository;

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
     *
     * @param searchTerm
     * @param sortKey
     * @param resultLimit
     * @param offset
     * @return
     */
    public List<AuthenticatedUser> getAuthenticatedUserList(String searchTerm, String sortKey, boolean isSortAscending, Integer resultLimit, Integer offset) {

        if ((offset == null) || (offset < 0)) {
            offset = 0;
        }

        List<AuthenticatedUser> userResults = getUserListCore(searchTerm, sortKey, isSortAscending, resultLimit, offset);

        // Initialize empty list for AuthenticatedUser objects
        //
        List<AuthenticatedUser> viewObjects = new ArrayList<>();

        if (userResults == null) {
            return viewObjects;
        }

        // -------------------------------------------------
        // GATHER GIANT HASHMAP OF ALL { user identifier : [role, role, role] }
        // -------------------------------------------------


        HashMap<String, List<String>> roleLookup = retrieveRolesForUsers(userResults);
        if (roleLookup == null) {
            roleLookup = new HashMap<>();
        }
        //  1st Loop : 
        // gather  [ @user, .....] 
        // get the hashmap


        // -------------------------------------------------
        // We have results, format them into AuthenticatedUser objects
        // -------------------------------------------------
        String roleString;
        for (AuthenticatedUser userInfo : userResults) {
            // GET ROLES FOR THIS USER FROM GIANT HASHMAP

            roleString = "";
            List<String> roleList = roleLookup.get("@" + userInfo.getUserIdentifier());
            if ((roleList != null) && (!roleList.isEmpty())) {
                roleString = roleList.stream().collect(Collectors.joining(", "));
            }
            AuthenticatedUser singleUser = addAuthenticatedUserRoles(userInfo, roleString);
            viewObjects.add(singleUser);
        }


        return viewObjects;
    }

    private AuthenticatedUser addAuthenticatedUserRoles(AuthenticatedUser authenticatedUser, String roles) {
        authenticatedUser.setRoles(roles);

        return authenticatedUser;
    }

    /**
     * Attempt to retrieve all the user roles in 1 query
     * Consider putting limits on this -- e.g. no more than 1,000 user identifiers or something similar
     *
     * @param userObjectList
     * @return
     */
    private HashMap<String, List<String>> retrieveRolesForUsers(List<AuthenticatedUser> userObjectList) {
        // Iterate through results, retrieving only the assignee identifiers
        // Note: userInfo[1], the assigneeIdentifier, cannot be null in the database
        //
        List<String> userIdentifierList = userObjectList.stream()
                .map(AuthenticatedUser::getUserIdentifier)
                .collect(Collectors.toList());

        List<Long> databaseIds = userObjectList.stream()
                .map(AuthenticatedUser::getId)
                .collect(Collectors.toList());


        if ((userIdentifierList == null) || (userIdentifierList.isEmpty())) {
            return null;
        }

        // -------------------------------------------------
        // Prepare a string to use within the SQL "a.assigneeidentifier IN (....)" clause
        //
        // Note: This is not ideal but .setParameter was failing with attempts using:
        //
        //            Collection<String>, List<String>, String[]
        //
        // This appears to be due to the JDBC driver or Postgres.  In this case SQL
        // injection isn't possible b/c the list of assigneeidentifier strings comes
        // from a previous query
        // 
        // Add '@' to each identifier and delimit the list by ","
        // -------------------------------------------------
        String identifierListString = userIdentifierList.stream()
                .filter(x -> !Strings.isNullOrEmpty(x))
                .map(x -> "'@" + x + "'")
                .collect(Collectors.joining(", "));

        // -------------------------------------------------
        // Create/Run the query to find directly assigned roles
        // -------------------------------------------------
        String qstr = "SELECT distinct a.assigneeidentifier,";
        qstr += " d.alias,";
        qstr += " d.name";
        qstr += " FROM roleassignment a,";
        qstr += " dataverserole d";
        qstr += " WHERE d.id = a.role_id";
        qstr += " AND a.assigneeidentifier IN (" + identifierListString + ")";
        qstr += " ORDER by a.assigneeidentifier, d.alias;";

        Query nativeQuery = em.createNativeQuery(qstr);

        List<Object[]> dbRoleResults = nativeQuery.getResultList();
        if (dbRoleResults == null) {
            return null;
        }

        HashMap<String, List<String>> userRoleLookup = new HashMap<>();

        String userIdentifier;
        String userRole;
        for (Object[] dbResultRow : dbRoleResults) {

            userIdentifier = UserUtil.getStringOrNull(dbResultRow[0]);
            userRole = RoleTranslationUtil.getLocaleNameFromAlias(UserUtil.getStringOrNull(dbResultRow[1]),
                        UserUtil.getStringOrNull(dbResultRow[2]));
            if ((userIdentifier != null) && (userRole != null)) {  // should never be null

                List<String> userRoleList = userRoleLookup.getOrDefault(userIdentifier, new ArrayList<String>());
                if (!userRoleList.contains(userRole)) {
                    userRoleList.add(userRole);
                    userRoleLookup.put(userIdentifier, userRoleList);
                }
            }
        }

        // And now the roles assigned via groups: 


        // 1. One query for selecting all the groups to which these users may belong: 

        HashMap<String, List<String>> groupsLookup = new HashMap<>();

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


        //System.out.println("qstr: " + qstr);

        nativeQuery = em.createNativeQuery(qstr);
        List<Object[]> groupResults = nativeQuery.getResultList();
        if (groupResults == null) {
            return userRoleLookup;
        }

        String groupIdentifiers = null;

        for (Object[] group : groupResults) {
            String alias = UserUtil.getStringOrNull(group[0]);
            String user = UserUtil.getStringOrNull(group[1]);
            if (alias != null) {

                alias = "&explicit/" + alias;

                if (groupIdentifiers == null) {
                    groupIdentifiers = "'" + alias + "'";
                } else {
                    groupIdentifiers += ", '" + alias + "'";
                }

                List<String> groupUserList = groupsLookup.getOrDefault(alias, new ArrayList<String>());
                if (!groupUserList.contains(user)) {
                    groupUserList.add(user);
                    groupsLookup.put(alias, groupUserList);
                }
            }
        }

        // 2. And now we can make another lookup on the roleassignment table, using the list 
        // of the explicit group aliases we have just generated: 

        if (groupIdentifiers == null) {
            return userRoleLookup;
        }

        qstr = "SELECT distinct a.assigneeidentifier,";
        qstr += " d.alias,";
        qstr += " d.name";
        qstr += " FROM roleassignment a,";
        qstr += " dataverserole d";
        qstr += " WHERE d.id = a.role_id";
        qstr += " AND a.assigneeidentifier IN (";
        qstr += groupIdentifiers;
        qstr += ") ORDER by a.assigneeidentifier, d.alias;";

        //System.out.println("qstr: " + qstr);

        nativeQuery = em.createNativeQuery(qstr);

        dbRoleResults = nativeQuery.getResultList();
        if (dbRoleResults == null) {
            return userRoleLookup;
        }


        for (Object[] dbResultRow : dbRoleResults) {

            String groupIdentifier = UserUtil.getStringOrNull(dbResultRow[0]);
            String groupRole = RoleTranslationUtil.getLocaleNameFromAlias(UserUtil.getStringOrNull(dbResultRow[1]),
                        UserUtil.getStringOrNull(dbResultRow[2]));
            if ((groupIdentifier != null) && (groupRole != null)) {  // should never be null

                List<String> groupUserList = groupsLookup.get(groupIdentifier);

                if (groupUserList != null) {

                    for (String groupUserIdentifier : groupUserList) {
                        groupUserIdentifier = "@" + groupUserIdentifier;
                        //System.out.println("Group user: "+groupUserIdentifier);
                        List<String> userRoleList = userRoleLookup.getOrDefault(groupUserIdentifier, new ArrayList<String>());
                        if (!userRoleList.contains(groupRole)) {
                            //System.out.println("User Role: "+groupRole);
                            userRoleList.add(groupRole);
                            userRoleLookup.put(groupUserIdentifier, userRoleList);
                        }
                    }
                }
            }
        }


        return userRoleLookup;
    }

    /**
     * Run a native query, returning a List<Object[]> containing
     * AuthenticatedUser information as well as information about the
     * Authenticated Provider (e.g. builtin user, etc)
     *
     * @param searchTerm
     * @param sortKey
     * @param resultLimit
     * @return
     */
    private List<AuthenticatedUser> getUserListCore(String searchTerm, String sortKey, boolean isSortAscending, Integer resultLimit, Integer offset) {

        SortKey dashboardUserSortKey = parseSortColumn(sortKey);

        if ((resultLimit == null) || (resultLimit < 1)) {
            resultLimit = 1;
        }

        if ((searchTerm == null) || (searchTerm.isEmpty())) {
            searchTerm = "";
        }

        if ((offset == null) || (offset < 0)) {
            offset = 0;
        }

        searchTerm = searchTerm.trim();

        return authenticatedUserRepository.findSearchedAuthenticatedUsers(dashboardUserSortKey,
                resultLimit, offset, searchTerm, isSortAscending);
    }

    private SortKey parseSortColumn(String sortKey) {
        if ((sortKey == null) || (sortKey.isEmpty())) {
            return SortKey.ID;
        }

        return validateSortColumn(sortKey);
    }

    private SortKey validateSortColumn(String sortKey) {
        return Arrays.stream(SortKey.values())
                .filter(dashboardUserSortKey -> dashboardUserSortKey.equals(SortKey.fromString(sortKey)))
                .findAny()
                .orElse(SortKey.ID);
    }

    /**
     * Return the number of superusers -- for the dashboard
     *
     * @return
     */
    public Long getSuperUserCount() {

        String qstr = "SELECT count(au)";
        qstr += " FROM AuthenticatedUser au";
        qstr += " WHERE au.superuser = :superuserTrue";

        Query query = em.createQuery(qstr);
        query.setParameter("superuserTrue", true);

        return (Long) query.getSingleResult();
    }

    /**
     * Return count of all users
     *
     * @return
     */
    public Long getTotalUserCount() {

        return getUserCount("");
    }

    /**
     * @param searchTerm
     * @return
     */
    public Long getUserCount(String searchTerm) {

        if ((searchTerm == null) || (searchTerm.isEmpty())) {
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

}
