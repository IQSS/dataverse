package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.search.IndexServiceBean;
import java.sql.Timestamp;
import java.util.Date;
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
