package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import java.sql.Timestamp;
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
     * Convenience method to format dbResult
     * @param dbResult
     * @return 
     */
    private String getStringOrNull(Object dbResult){
        
        if (dbResult == null){
            return null;
        }
        return (String)dbResult;
    }
    
    /**
     * Convenience method to format dbResult
     * @param dbResult
     * @return 
     */
    private String getTimestampStringOrNull(Object dbResult){
        
        if (dbResult == null){
            return null;
        }
        return ((Timestamp)dbResult).toString();
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
                    .add("lastName", this.getStringOrNull(result[2]))
                    .add("firstName", this.getStringOrNull(result[3]))
                    .add("email", this.getStringOrNull(result[4]))
                    .add("affiliation", this.getStringOrNull(result[5]))
                    .add("isSuperuser", (boolean)result[6])
                    .add("position", this.getStringOrNull(result[7]))
                    .add("modificationTime", this.getTimestampStringOrNull(result[8]));
            jsonUserListArray.add(singleUserData);            
        }
       
        return jsonUserListArray;
       
    }
    
    
    
    /**
     * 
     * @param searchTerm
     * @param sortKey
     * @param resultLimit
     * @return 
     */
    public List<Object[]> getUserList(String searchTerm, String sortKey, Integer resultLimit, Integer offset) {

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
        
       
        System.out.println("Search key: " + searchTerm); 

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
        nativeQuery.setParameter("searchTerm", "%" + searchTerm + "%");  

        
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
        nativeQuery.setParameter("searchTerm", "%" + searchTerm + "%");  
        
        return (Long)nativeQuery.getSingleResult();

    }
    
    
    
}
