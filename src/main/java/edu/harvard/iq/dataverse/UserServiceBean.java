package edu.harvard.iq.dataverse;
import edu.harvard.iq.dataverse.UserNotification.Type;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.userdata.UserUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.apache.commons.lang3.StringUtils;

@Stateless
@Named
public class UserServiceBean {

    private static final Logger logger = Logger.getLogger(UserServiceBean.class.getCanonicalName());
    public static final List<String> acceptableSortKeys = Arrays.asList(
      "id", "useridentifier", "lastname", "firstname", "email", "affiliation",
      "superuser", "position", "createdtime", "lastlogintime", "lastapiusetime",
      "authproviderid",
      "id desc", "useridentifier desc", "lastname desc", "firstname desc", "email desc", "affiliation desc",
      "superuser desc", "position desc", "createdtime desc", "lastlogintime desc", "lastapiusetime desc",
      "authproviderid desc"
    );

    @PersistenceContext
    EntityManager em;

    @EJB IndexServiceBean indexService;

    public AuthenticatedUser find(Object pk) {
        return (AuthenticatedUser) em.find(AuthenticatedUser.class, pk);
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
     * @param searchTerm
     * @param sortKey
     * @param resultLimit
     * @param offset
     * @return
     */
    public List<AuthenticatedUser> getAuthenticatedUserList(String searchTerm,
                                                            String sortKey,
                                                            Integer resultLimit,
                                                            Integer offset) {
        if ((offset == null) || (offset < 0)) {
            offset = 0;
        }

        List<Object[]> userResults = getUserListCore(searchTerm, sortKey, resultLimit, offset);

        // Initialize empty list for AuthenticatedUser objects
        //
        List<AuthenticatedUser> viewObjects = new ArrayList<>();

        if (userResults == null){
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
        int rowNum = offset++;   // used for the rowNumber
        String roleString;
        for (Object[] userInfo : userResults) {
            // GET ROLES FOR THIS USER FROM GIANT HASHMAP
            rowNum++;

            //String roles = getUserRolesAsString((Integer) dbResultRow[0]);
            roleString = "";
            List<String> roleList = roleLookup.get("@" + (String)userInfo[1]);
            if ((roleList != null)&&(!roleList.isEmpty())){
                roleString = roleList.stream().collect(Collectors.joining(", "));
            }
            AuthenticatedUser singleUser = createAuthenticatedUserForView(userInfo, roleString, rowNum);
            viewObjects.add(singleUser);
        }

        return viewObjects;
    }

    private AuthenticatedUser createAuthenticatedUserForView (Object[] dbRowValues, String roles, int rowNum){
        AuthenticatedUser user = new AuthenticatedUser();

        user.setId(new Long((int)dbRowValues[0]));
        user.setUserIdentifier((String)dbRowValues[1]);
        user.setLastName(UserUtil.getStringOrNull(dbRowValues[2]));
        user.setFirstName(UserUtil.getStringOrNull(dbRowValues[3]));
        user.setEmail(UserUtil.getStringOrNull(dbRowValues[4]));
        user.setAffiliation(UserUtil.getStringOrNull(dbRowValues[5]));
        user.setSuperuser((Boolean)(dbRowValues[6]));
        user.setPosition(UserUtil.getStringOrNull(dbRowValues[7]));

        user.setCreatedTime(UserUtil.getTimestampOrNull(dbRowValues[8]));
        user.setLastLoginTime(UserUtil.getTimestampOrNull(dbRowValues[9]));
        user.setLastApiUseTime(UserUtil.getTimestampOrNull(dbRowValues[10]));

        user.setAuthProviderId(UserUtil.getStringOrNull(dbRowValues[11]));
        user.setAuthProviderFactoryAlias(UserUtil.getStringOrNull(dbRowValues[12]));

        user.setDeactivated((Boolean)(dbRowValues[13]));
        user.setDeactivatedTime(UserUtil.getTimestampOrNull(dbRowValues[14]));

        user.setMutedEmails(Type.tokenizeToSet((String) dbRowValues[15]));
        user.setMutedNotifications(Type.tokenizeToSet((String) dbRowValues[15]));

        user.setRateLimitTier((int)dbRowValues[17]);

        user.setRoles(roles);
        return user;
    }

    /**
     * Attempt to retrieve all the user roles in 1 query
     * Consider putting limits on this -- e.g. no more than 1,000 user identifiers or something similar
     * 
     * @param userIdentifierList
     * @return
     */
    private HashMap<String, List<String>> retrieveRolesForUsers(List<Object[]> userObjectList){
        // Iterate through results, retrieving only the assignee identifiers
        // Note: userInfo[1], the assigneeIdentifier, cannot be null in the database
        //
        List<String> userIdentifierList = userObjectList.stream()
                                        .map(userInfo -> (String)userInfo[1])
                                        .collect(Collectors.toList())
                                       ;

        List<Integer> databaseIds = userObjectList.stream()
                                        .map(userInfo -> (Integer)userInfo[0])
                                        .collect(Collectors.toList());

        if ((userIdentifierList==null)||(userIdentifierList.isEmpty())){
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
                                     .filter(x -> x != null && !x.isEmpty())
                                     .map(x -> "'@" + x + "'")
                                     .collect(Collectors.joining(", "));

        // -------------------------------------------------
        // Create/Run the query to find directly assigned roles
        // -------------------------------------------------
        String qstr = "SELECT distinct a.assigneeidentifier,";
        qstr += " d.name";
        qstr += " FROM roleassignment a,";
        qstr += " dataverserole d";
        qstr += " WHERE d.id = a.role_id";
        qstr += " AND a.assigneeidentifier IN (" + identifierListString + ")";
        qstr += " ORDER by a.assigneeidentifier, d.name;";

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
            userRole = UserUtil.getStringOrNull(dbResultRow[1]);
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

        for (Object group[] : groupResults) {
            String alias = UserUtil.getStringOrNull(group[0]);
            String user = UserUtil.getStringOrNull(group[1]);
            if (alias != null ) {
                alias = "&explicit/"+alias;

                if (groupIdentifiers == null) {
                    groupIdentifiers = "'"+alias+"'";
                } else {
                    groupIdentifiers += ", '"+alias+"'";
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
        qstr += " d.name";
        qstr += " FROM roleassignment a,";
        qstr += " dataverserole d";
        qstr += " WHERE d.id = a.role_id";
        qstr += " AND a.assigneeidentifier IN (";
        qstr += groupIdentifiers;
        qstr += ") ORDER by a.assigneeidentifier, d.name;";

        //System.out.println("qstr: " + qstr);

        nativeQuery = em.createNativeQuery(qstr);

        dbRoleResults = nativeQuery.getResultList();
        if (dbRoleResults == null) {
            return userRoleLookup;
        }

        for (Object[] dbResultRow : dbRoleResults) {
            String groupIdentifier = UserUtil.getStringOrNull(dbResultRow[0]);
            String groupRole = UserUtil.getStringOrNull(dbResultRow[1]);
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
     * 
     * @param userId
     * @return
     */
    private String getUserRolesAsString(Integer userId) {
        String retval = "";
        String userIdentifier = "";
        String qstr = "select useridentifier ";
        qstr += " FROM authenticateduser";
        qstr += " WHERE id = " + userId.toString();
        qstr += ";";

        Query nativeQuery = em.createNativeQuery(qstr);

        userIdentifier = '@' + (String) nativeQuery.getSingleResult();

        qstr = " select distinct d.name from roleassignment a, dataverserole d";
        qstr += " where d.id = a.role_id and a.assigneeidentifier='" + userIdentifier + "'"
                + " Order by d.name;";

        nativeQuery = em.createNativeQuery(qstr);

        List<Object[]> roleList = nativeQuery.getResultList();

        for (Object o : roleList) {
            if (!retval.isEmpty()) {
                retval += ", ";
            }
            retval += (String) o;
        }
        return retval;
    }

    /**
     * 
     * Run a native query, returning a List<Object[]> containing
     * AuthenticatedUser information as well as information about the
     * Authenticated Provider (e.g. builtin user, etc)
     * 
     * @param searchTerm
     * @param sortKey
     * @param resultLimit
     * @return
     */
    private List<Object[]> getUserListCore(String searchTerm,
                                           String sortKey,
                                           Integer resultLimit,
                                           Integer offset) {
        if (StringUtils.isNotBlank(sortKey) && acceptableSortKeys.contains(sortKey.toLowerCase())) {
            if (sortKey.toLowerCase().equals("authproviderid")) {
                sortKey = "prov.id";
            } else if (sortKey.toLowerCase().equals("authproviderid desc")) {
                sortKey = "prov.id desc";
            } else {
                sortKey = "u." + sortKey.toLowerCase();
            }
        } else {
            sortKey = "u.useridentifier";
        }

        if ((resultLimit == null) || (resultLimit < 1)) {
            resultLimit = 1;
        }

        if ((searchTerm==null) || (searchTerm.isEmpty())) {
            searchTerm = "";
        }

        if ((offset == null) || (offset < 0)) {
            offset = 0;
        }

        //Results of this query are used to build Authenticated User records:

        searchTerm = searchTerm.trim();

        String sharedSearchClause = "";

        if (!searchTerm.isEmpty()) {
            sharedSearchClause = " AND " + getSharedSearchClause(searchTerm);
        }

        String qstr = "SELECT u.id, u.useridentifier,";
        qstr += " u.lastname, u.firstname, u.email,";
        qstr += " u.affiliation, u.superuser,";
        qstr += " u.position,";
        qstr += " u.createdtime, u.lastlogintime, u.lastapiusetime, ";
        qstr += " prov.id, prov.factoryalias, ";
        qstr += " u.deactivated, u.deactivatedtime, ";
        qstr += " u.mutedEmails, u.mutedNotifications, u.rateLimitTier ";
        qstr += " FROM authenticateduser u,";
        qstr += " authenticateduserlookup prov_lookup,";
        qstr += " authenticationproviderrow prov";
        qstr += " WHERE";
        qstr += " u.id = prov_lookup.authenticateduser_id";
        qstr += " AND prov_lookup.authenticationproviderid = prov.id";
        qstr += sharedSearchClause;
        qstr += " ORDER BY " + sortKey; // u.useridentifier
        qstr += " LIMIT " + resultLimit;
        qstr += " OFFSET " + offset;
        qstr += ";";

        logger.log(Level.FINE, "getUserCount: {0}", qstr);

        Query nativeQuery = em.createNativeQuery(qstr);

        return nativeQuery.getResultList();
    }

    /**
     * The search clause needs to be consistent between the searches that:
     * (1) get a user count
     * (2) get a list of users
     * 
     * @return
     */

    private String getSharedSearchClause(String searchTerm) {
        String[] searchTermTokens = searchTerm.replaceAll("['\"]", "").split("[ ,][ ,]*");

        String searchClause = "(";

        for (int i = 0; i < searchTermTokens.length; i++) {
            if (i > 0) {
                searchClause += " AND ";
            }
            searchClause += "(u.useridentifier ILIKE '" + searchTermTokens[i] + "%'";
            searchClause += " OR u.firstname ILIKE '" + searchTermTokens[i] + "%'";
            searchClause += " OR u.lastname ILIKE '" + searchTermTokens[i] + "%'";
            searchClause += " OR u.affiliation ILIKE '" + searchTermTokens[i] + "%'";
            searchClause += " OR u.affiliation ILIKE '% " + searchTermTokens[i] + "%'";
            searchClause += " OR u.email ILIKE '" + searchTermTokens[i] + "%')";
        }

        return searchClause + ")";
    }

    /**
     * Return the number of superusers -- for the dashboard
     * @return
     */
    public Long getSuperUserCount() {
        String qstr = "SELECT count(au)";
        qstr += " FROM AuthenticatedUser au";
        qstr += " WHERE au.superuser = :superuserTrue";

        Query query = em.createQuery(qstr);
        query.setParameter("superuserTrue", true);

        return (Long)query.getSingleResult();
    }

    /**
     * Return count of all users
     * @return
     */
    public Long getTotalUserCount() {
        return getUserCount("");
    }

    /**
     * 
     * @param searchTerm
     * @return
     */
    public Long getUserCount(String searchTerm) {
        if ((searchTerm==null) || (searchTerm.isEmpty())) {
            searchTerm = "";
        }
        searchTerm = searchTerm.trim();

        String sharedSearchClause = "";

        if (!searchTerm.isEmpty()) {
            sharedSearchClause = " AND " + getSharedSearchClause(searchTerm);
        }

        String qstr = "SELECT count(u.id)";
        qstr += " FROM authenticateduser u,";
        qstr += " authenticateduserlookup prov_lookup,";
        qstr += " authenticationproviderrow prov";
        qstr += " WHERE";
        qstr += " u.id = prov_lookup.authenticateduser_id";
        qstr += " AND prov_lookup.authenticationproviderid = prov.id";
        qstr += sharedSearchClause;
        qstr += ";";

        Query nativeQuery = em.createNativeQuery(qstr);

        return (Long)nativeQuery.getSingleResult();
    }

    public AuthenticatedUser updateLastLogin(AuthenticatedUser user) {
        //assumes that AuthenticatedUser user already exists
        user.setLastLoginTime(new Timestamp(new Date().getTime()));

        return save(user);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public AuthenticatedUser updateLastApiUseTime(AuthenticatedUser user) {
        //assumes that AuthenticatedUser user already exists
        user.setLastApiUseTime(new Timestamp(new Date().getTime()));
        return save(user);
    }
}
