package edu.harvard.iq.dataverse;

import com.google.gson.Gson;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
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
     * @param searchKey
     * @param sortKey
     * @param resultLimit
     * @return 
     */
    public JsonObjectBuilder getUserListAsJSON(String searchKey, String sortKey, Integer resultLimit) {
        System.out.println("getUserListAsJSON 1");

        // -------------------------------------------------
        // Retrieve a list of user attributes from a native query
        // -------------------------------------------------
        List<Object[]> userResults = getUserList(searchKey, sortKey, resultLimit);


        // -------------------------------------------------
        // Initialize the overall JSON response object (jsonUserData)
        // as well as the JSOn user List
        // -------------------------------------------------
        JsonObjectBuilder jsonUserData = Json.createObjectBuilder();
        JsonArrayBuilder jsonUserListArray = Json.createArrayBuilder();

        // -------------------------------------------------
        // No results..... Return count of 0 and empty array
        // -------------------------------------------------
        if ((userResults==null)||(userResults.isEmpty())){
            jsonUserData.add("userCount", 0)
                        .add("users", jsonUserListArray);
            return jsonUserData;
        }
        
        // -------------------------------------------------
        // We have results, format them into a JSON object
        // -------------------------------------------------
        for (Object[] result : userResults) {            

            // not putting explicit nulls for now b/c https://stackoverflow.com/questions/22363925/jsr-353-how-to-add-null-values-using-javax-json-jsonobjectbuilder
            //
            NullSafeJsonBuilder singleUserData = NullSafeJsonBuilder.jsonObjectBuilder();
            
            singleUserData.add("id", (int)result[0])
                    .add("userIdentifier", (String)result[1])
                    .add("lastName", this.getStringOrNull(result[2]))
                    .add("firstName", this.getStringOrNull(result[3]))
                    .add("email", this.getStringOrNull(result[4]))
                    .add("affiliation", this.getStringOrNull(result[5]))
                    .add("is_superuser", (boolean)result[6])
                    .add("position", this.getStringOrNull(result[7]))
                    .add("modificationTime", this.getTimestampStringOrNull(result[8]));
            jsonUserListArray.add(singleUserData);            
        }
        
        jsonUserData.add("userCount", userResults.size())
                    .add("users", jsonUserListArray);
                

        return jsonUserData;

       
    }
    
    
    
    /**
     * 
     * @param searchKey
     * @param sortKey
     * @param resultLimit
     * @return 
     */
    public List<Object[]> getUserList(String searchKey, String sortKey, Integer resultLimit) {

        if ((sortKey == null) || (sortKey.isEmpty())){
            sortKey = "u.username";
        }else{
            sortKey = "u." + sortKey;
        }
        
        if ((resultLimit == null)||(resultLimit < 25)){
            resultLimit = 25;
        }
        
        if ((searchKey==null)||(searchKey.isEmpty())){
            searchKey = "";
        }
        
       
        System.out.println("Search key: " + searchKey); 

        String qstr = "SELECT u.id, u.useridentifier,";
        qstr += " u.lastname, u.firstname, u.email,";
        qstr += " u.affiliation, u.superuser,";
        qstr += " u.position, u.modificationtime";
        qstr += " FROM authenticateduser u";
        qstr += " WHERE u.useridentifier ILIKE #searchKey";
        qstr += " OR u.firstname ILIKE #searchKey";
        qstr += " OR u.lastname ILIKE #searchKey";
        //qstr += " WHERE u.useridentifier = #searchKey";
  
        qstr += " ORDER BY u.useridentifier";
        qstr += " LIMIT " + resultLimit;
        
        System.out.println("--------------\n\n" + qstr);
        
        Query nativeQuery = em.createNativeQuery(qstr);  
        nativeQuery.setParameter("searchKey", searchKey + "%");  

        
        return nativeQuery.getResultList();

    }
    
}
