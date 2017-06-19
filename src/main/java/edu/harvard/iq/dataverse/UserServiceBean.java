package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

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
               
        // Make sure there is a "created" time
        if (user.getCreated() == null){
            user.setCreatedToCurrentTime(); // default new creation time
            logger.info("Creation time null! Setting user creation time to now");
        } 
        
        if ( user.getId() == null ) {
            em.persist(this);
        } else {
            user = em.merge(user);
        }
        em.flush();

        return user;
    }

    public AuthenticatedUser updateLastLogin(AuthenticatedUser user) {
        if (user == null){
            logger.severe("user should not be null");
            return null;
        }
        
        user.setLastLoginToCurrentTime();
        
        return save(user);
    }

    
    
    public AuthenticatedUser updateLastApiUse(AuthenticatedUser user) {
        if (user == null){
            logger.severe("user should not be null");
            return null;
        }
        
        user.setLastApiUseToCurrentTime();
        
        return save(user);
    }
}
