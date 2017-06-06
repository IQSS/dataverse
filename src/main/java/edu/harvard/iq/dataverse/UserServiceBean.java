package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.userdata.SingleUserView;
import edu.harvard.iq.dataverse.userdata.UserUtil;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

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
     * 
     * @param searchTerm
     * @param sortKey
     * @param resultLimit
     * @return 
     */
    public JsonArrayBuilder getUserListAsJSON(String searchTerm, String sortKey, Integer resultLimit, Integer offset) {
        System.out.println("getUserListAsJSON 1");

        if ((offset == null)||(offset < 0)){
            offset = 0;
        }
        // -------------------------------------------------
        // Retrieve a list of user attributes from a native query
        // -------------------------------------------------
        List<Object[]> userResults = getUserList(searchTerm, sortKey, resultLimit, offset);

        // -------------------------------------------------
        // No results..... Return count of 0 and empty array
        // -------------------------------------------------
        if ((userResults==null)||(userResults.isEmpty())){
            return Json.createArrayBuilder(); // return an empty array
        }
        
        // -------------------------------------------------
        // We have results, format them into a JSON object
        // -------------------------------------------------
        JsonArrayBuilder jsonUserListArray = Json.createArrayBuilder();

        offset++;   // used for the rowNumber
        for (Object[] result : userResults) {            

            // not putting explicit nulls for now b/c https://stackoverflow.com/questions/22363925/jsr-353-how-to-add-null-values-using-javax-json-jsonobjectbuilder
            //
            NullSafeJsonBuilder singleUserData = NullSafeJsonBuilder.jsonObjectBuilder();
            
            singleUserData.add("id", (int)result[0])
                    .add("rowNum", offset++)
                    .add("userIdentifier", (String)result[1])
                    .add("lastName", UserUtil.getStringOrNull(result[2]))
                    .add("firstName", UserUtil.getStringOrNull(result[3]))
                    .add("email", UserUtil.getStringOrNull(result[4]))
                    .add("affiliation", UserUtil.getStringOrNull(result[5]))
                    .add("isSuperuser", (boolean)result[6])
                    .add("position", UserUtil.getStringOrNull(result[7]))
                    .add("modificationTime", UserUtil.getTimestampStringOrNull(result[8]));
            jsonUserListArray.add(singleUserData);            
        }
       
        return jsonUserListArray;
       
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
     * Return the user information as a List of SingleUserView objects -- easier to work with in the UI
     * 
     * @param searchTerm
     * @param sortKey
     * @param resultLimit
     * @param offset
     * @return 
     */
    public List<SingleUserView> getUserListAsSingleUserObjects(String searchTerm, String sortKey, Integer resultLimit, Integer offset){
        
        if ((offset == null)||(offset < 0)){
            offset = 0;
        }
        
        List<Object[]> userResults = getUserListCore(searchTerm, sortKey, resultLimit, offset);
        
        // Initialize empty list for SingleUserView objects
        //
        List<SingleUserView> viewObjects = new ArrayList<>();
        
        if (userResults == null){
            return viewObjects;
        }
        
        // -------------------------------------------------
        // We have results, format them into SingleUserView objects
        // -------------------------------------------------
        int rowNum = offset++;   // used for the rowNumber
        for (Object[] dbResultRow : userResults) {            
            rowNum++;
            String roles = getUserRolesAsString((Integer) dbResultRow[0]);
            SingleUserView singleUser = new SingleUserView(dbResultRow, roles, rowNum);            
            viewObjects.add(singleUser);
        }
        
        return viewObjects;
    }
    
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
        
              
        // NOTE: IF YOU CHANGE THIS QUERY, THEN CHANGE: SingleUserView.java

        String qstr = "SELECT u.id, u.useridentifier,";
        qstr += " u.lastname, u.firstname, u.email,";
        qstr += " u.affiliation, u.superuser,";
        qstr += " u.position, u.modificationtime";
        qstr += " FROM authenticateduser u";
        qstr += getSharedSearchClause(searchTerm);
        qstr += " ORDER BY u.useridentifier";
        qstr += " LIMIT " + resultLimit;
        qstr += " OFFSET " + offset;
        qstr += ";";
        
        System.out.println("--------------\n\n" + qstr);
        
        Query nativeQuery = em.createNativeQuery(qstr);          
        nativeQuery.setParameter("searchTerm", formatSearchTerm(searchTerm));  

        
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
        if (searchTerm.isEmpty()){
            return "";
        }
        
        /*
        String searchClause = " WHERE u.useridentifier LIKE '%" + searchTerm +"%'";
        searchClause += " OR u.firstname LIKE '%" + searchTerm +"%'";
        searchClause += " OR u.lastname LIKE '%" + searchTerm +"%'";
        searchClause += " OR u.email LIKE '%" + searchTerm +"%'";
        */
        String searchClause = " WHERE u.useridentifier ILIKE #searchTerm";
        searchClause += " OR u.firstname ILIKE #searchTerm";
        searchClause += " OR u.lastname ILIKE #searchTerm"; 
        searchClause += " OR u.email ILIKE #searchTerm"; 
        
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

    private String formatSearchTerm(String searchTerm){
        
        if (searchTerm == null){
            return "";
        }
        
        return searchTerm + "%";    // for LIKE query on right side

        //return "%" + searchTerm + "%";    // for LIKE query on both sides
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

        String qstr = "SELECT count(u.id)";
        qstr += " FROM authenticateduser u";
        qstr += getSharedSearchClause(searchTerm);
        qstr += ";";
        
        Query nativeQuery = em.createNativeQuery(qstr);  
        nativeQuery.setParameter("searchTerm", formatSearchTerm(searchTerm));  
        
        return (Long)nativeQuery.getSingleResult();

    }
    
    
    
}
