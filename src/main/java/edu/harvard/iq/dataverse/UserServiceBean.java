package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.userdata.UserUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.ocpsoft.common.util.Strings;

@Stateless
@Named
public class UserServiceBean {

    private static final Logger logger = Logger.getLogger(UserServiceBean.class.getCanonicalName());

    @PersistenceContext
    EntityManager em;
    
    @EJB IndexServiceBean indexService;

    public AuthenticatedUser find(Object pk) {
        return (AuthenticatedUser) em.find(AuthenticatedUser.class, pk);
    }    

    public AuthenticatedUser save( AuthenticatedUser user ) {
        if ( user.getId() == null ) {
            em.persist(this);
        } else {
            user = em.merge(user);
        }
        em.flush();

        return user;
    }
   
    /**
     * Return the user information as a List of Arrays--e.g. straight from the db query
     * 
     * @param searchTerm
     * @param sortKey
     * @param resultLimit
     * @param offset
     * @return 
     */
    public List<Object[]> getUserList(String searchTerm, String sortKey, Integer resultLimit, Integer offset){
        
        return getUserListCore(searchTerm, sortKey, resultLimit, offset);
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
    public List<AuthenticatedUser> getAuthenticatedUserList(String searchTerm, String sortKey, Integer resultLimit, Integer offset){
        
        if ((offset == null)||(offset < 0)){
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

        // Iterate through results, retrieving only the assignee identifiers
        // Note: userInfo[1], the assigneeIdentifier, cannot be null in the database
        //
        List<String> identifiers = userResults.stream()
                                        .map(userInfo -> (String)userInfo[1])
                                        .collect(Collectors.toList())
                                       ;
  
        HashMap<String, List<String>> roleLookup = retrieveRolesForUsers(identifiers);
        if (roleLookup == null){
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
        user.setRowNum(rowNum);
        user.setId(new Long((Integer)dbRowValues[0]));
        user.setUserIdentifier((String)dbRowValues[1]);
        user.setLastName(UserUtil.getStringOrNull(dbRowValues[2]));
        user.setFirstName(UserUtil.getStringOrNull(dbRowValues[3]));
        user.setEmail(UserUtil.getStringOrNull(dbRowValues[4]));
        user.setAffiliation(UserUtil.getStringOrNull(dbRowValues[5]));
        user.setSuperuser((Boolean)(dbRowValues[6]));
        user.setPosition(UserUtil.getStringOrNull(dbRowValues[7]));
        user.setModificationTime(UserUtil.getTimestampOrNull(dbRowValues[8]));
        user.setAuthProviderId(UserUtil.getStringOrNull(dbRowValues[9]));
        user.setAuthProviderFactoryAlias(UserUtil.getStringOrNull(dbRowValues[10]));
        
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
    private HashMap<String, List<String>> retrieveRolesForUsers(List<String> userIdentifierList){
        
        if ((userIdentifierList==null)||(userIdentifierList.isEmpty())){
            return null;
        }
        
        // Add '@' to each identifier and delimit the list by ","
        //
        String identifierList = userIdentifierList.stream()
                                     .filter(x -> !Strings.isNullOrEmpty(x))
                                     .map(x -> "'@" + x + "'")
                                     .collect(Collectors.joining(", "));

        //System.out.println("identifierList: " + identifierList);

        
        String qstr = "SELECT distinct a.assigneeidentifier,";
        qstr += " d.name";
        qstr += " FROM roleassignment a,";
        qstr += " dataverserole d";
        qstr += " WHERE d.id = a.role_id";
        qstr += " AND a.assigneeidentifier IN (" + identifierList + ")";
        qstr += " ORDER by a.assigneeidentifier, d.name;";

        //System.out.println("qstr: " + qstr);

        Query nativeQuery = em.createNativeQuery(qstr);

        List<Object[]> dbRoleResults = nativeQuery.getResultList();
        if (dbRoleResults == null){
            return null;
        }
        
        HashMap<String, List<String>> userRoleLookup = new HashMap<>();

        String userIdentifier;
        String userRole;
        for (Object[] dbResultRow : dbRoleResults) {            
            
            userIdentifier = UserUtil.getStringOrNull(dbResultRow[0]);
            userRole = UserUtil.getStringOrNull(dbResultRow[1]);
            if ((userIdentifier != null)&&(userRole != null)){  // should never be null
                
                List<String> userRoleList = userRoleLookup.getOrDefault(userIdentifier, new ArrayList<String>());
                if (!userRoleList.contains(userRole)){
                    userRoleList.add(userRole);
                    userRoleLookup.put(userIdentifier, userRoleList);
                }
            }
        }
        
        System.out.println("userRoleLookup: " + userRoleLookup.size());

    
        
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
     * @param searchTerm
     * @param sortKey
     * @param resultLimit
     * @return 
     */
    private List<Object[]> getUserListCore(String searchTerm, String sortKey, Integer resultLimit, Integer offset) {

        if ((sortKey == null) || (sortKey.isEmpty())){
            sortKey = "u.username";
        }else{
            sortKey = "u." + sortKey;
        }
        
        if ((resultLimit == null)||(resultLimit < 1)){
            resultLimit = 1;
        }
        
        if ((searchTerm==null)||(searchTerm.isEmpty())){
            searchTerm = "";
        }
        
        if ((offset == null)||(offset < 0)){
            offset = 0;
        }
        
        //Results of thius query are used to build Authenticated User records

        String sharedSearchClause = getSharedSearchClause(searchTerm);
        if (sharedSearchClause.isEmpty()){
            sharedSearchClause = "";
        }else{
            sharedSearchClause = " AND " + sharedSearchClause;
        }
        
        String qstr = "SELECT u.id, u.useridentifier,";
        qstr += " u.lastname, u.firstname, u.email,";
        qstr += " u.affiliation, u.superuser,";
        qstr += " u.position, u.modificationtime,";
        qstr += " prov.id, prov.factoryalias";
        qstr += " FROM authenticateduser u,";
        qstr += " authenticateduserlookup prov_lookup,";
        qstr += " authenticationproviderrow prov";
        qstr += " WHERE";
        qstr += " u.id = prov_lookup.authenticateduser_id";
        qstr += " AND prov_lookup.authenticationproviderid = prov.id";       
        qstr += sharedSearchClause;
        qstr += " ORDER BY u.useridentifier";
        qstr += " LIMIT " + resultLimit;
        qstr += " OFFSET " + offset;
        qstr += ";";
        
        System.out.println("getUserCount: " + qstr);

        Query nativeQuery = em.createNativeQuery(qstr);           
       
        return nativeQuery.getResultList();

    }
    
    /**
     * The search clause needs to be consistent between the searches that:
     * (1) get a user count
     * (2) get a list of users
     * 
     * @param searchTerm
     * @return 
     */
    private String getSharedSearchClause(String searchTerm){       
        if (searchTerm == null){
            return "";
        }

        String searchClause = "";

        searchTerm = searchTerm.trim();
        if (searchTerm.isEmpty()){
            return "";
        }        
        
        //--------------------------------------------------------
        // Search for term anywhere at beginning of string
        //--------------------------------------------------------
        searchClause += " (u.useridentifier LIKE '" + searchTerm +"%'";
        searchClause += " OR u.firstname ILIKE '" + searchTerm +"%'";
        searchClause += " OR u.lastname ILIKE '" + searchTerm +"%'";
        searchClause += " OR u.email ILIKE '" + searchTerm +"%')";
        
        /*
        //--------------------------------------------------------
        // Search for term anywhere in string
        //--------------------------------------------------------
        searchClause += " u.useridentifier LIKE '%" + searchTerm +"%'";
        searchClause += " OR u.firstname ILIKE '%" + searchTerm +"%'";
        searchClause += " OR u.lastname ILIKE '%" + searchTerm +"%'";
        searchClause += " OR u.email ILIKE '%" + searchTerm +"%'";
        */
        return searchClause;
    }
    
    
    /**
     * Return the number of superusers -- for the dashboard
     * @return 
     */
    public Long getSuperUserCount() {
        
        String qstr = "SELECT count(id)";
        qstr += " FROM authenticateduser";
        qstr += " WHERE superuser = true";
        qstr += ";";
        
        Query nativeQuery = em.createNativeQuery(qstr);  

        return (Long)nativeQuery.getSingleResult();

    }

    /**
     * Return count of all users
     * @return 
     */
    public Long getTotalUserCount(){
        
        return getUserCount(null);
    }
    
    /**
     * 
     * @param searchTerm
     * @return 
     */
    public Long getUserCount(String searchTerm) {
        
        if ((searchTerm==null)||(searchTerm.isEmpty())){
            searchTerm = "";
        }        

        String sharedSearchClause = getSharedSearchClause(searchTerm);
        if (sharedSearchClause.isEmpty()){
            sharedSearchClause = "";
        }else{
            sharedSearchClause = " AND " + sharedSearchClause;
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
        
        //System.out.println("getUserCount: " + qstr);
        
        Query nativeQuery = em.createNativeQuery(qstr);  
        
        return (Long)nativeQuery.getSingleResult();

    }
    
    
    
}
