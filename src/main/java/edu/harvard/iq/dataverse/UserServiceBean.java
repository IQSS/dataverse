package edu.harvard.iq.dataverse;

import com.google.gson.Gson;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
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
     * @param searchKey
     * @param sortKey
     * @param resultLimit
     * @return 
     */
    public JsonObjectBuilder getUserListAsJSON(String searchKey, String sortKey, Integer resultLimit) {
        System.out.println("getUserListAsJSON 1");

        List<Object[]> userResults = getUserList(searchKey, sortKey, resultLimit);

        System.out.println("getUserListAsJSON 2");

        if ((userResults==null)||(userResults.isEmpty())){
            return null;
        }
        
        /*
         u.id, u.useridentifier,";
        qstr += " u.lastname, u.firstname, u.email,";
        qstr += " u.affiliation, u.superuser,";
        qstr += " u.position, u.modificationtime";
        qstr += " FROM authenticateduser u";
        */
        JsonArrayBuilder jsonArray = Json.createArrayBuilder();
        for (Object[] result : userResults) {
            JsonObjectBuilder jsonData = Json.createObjectBuilder();
            jsonData.add("id", (int)result[0])
                    .add("userIdentifier", (String)result[1])
                    .add("lastName", (String)result[2])
                    .add("firstName", (String)result[3])
                    .add("email", (String)result[4]);
            //jsonData.add("affiliation", (boolean)result[5]);
            //jsonData.add("superuser", (boolean)result[6]);
            jsonArray.add(jsonData);            
        }
        
        JsonObjectBuilder jsonUserData = Json.createObjectBuilder();
        jsonUserData.add("userCount", userResults.size())
                    .add("users", jsonArray);
        
       
        System.out.println("getUserListAsJSON 3: " + userResults.size());
        

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
        

        String qstr = "SELECT u.id, u.useridentifier,";
        qstr += " u.lastname, u.firstname, u.email,";
        qstr += " u.affiliation, u.superuser,";
        qstr += " u.position, u.modificationtime";
        qstr += " FROM authenticateduser u";
        /*qstr += " WHERE u.useridentifier ILIKE ':searchKey%'";
        qstr += " OR u.firstname ILIKE ':searchKey%'";
        qstr += " OR u.lastname ILIKE ':searchKey%'";*/
        qstr += " ORDER BY u.useridentifier";
        qstr += " LIMIT " + resultLimit;
        
        System.out.println("--------------\n\n" + qstr);
        
        Query nativeQuery = em.createNativeQuery(qstr);  
        //nativeQuery.setParameter("searchKey", searchKey);  
        //nativeQuery.setParameter(2, searchKey);  
        //nativeQuery.setParameter(3, searchKey);  
        //nativeQuery.setParameter(4, sortKey);  

        System.out.println("--------------\n\n" + nativeQuery.toString());

        
        return nativeQuery.getResultList();

    }
    
}
