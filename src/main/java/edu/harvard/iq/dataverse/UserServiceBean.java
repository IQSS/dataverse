package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
@Named
public class UserServiceBean {

    private static final Logger logger = Logger.getLogger(UserServiceBean.class.getCanonicalName());

    @PersistenceContext
    EntityManager em;
    
    @EJB IndexServiceBean indexService;


    // FIXEME this is user id, not name (username is on the idp)
    @Deprecated
    public AuthenticatedUser findByUsername(String username) {
        TypedQuery<AuthenticatedUser> typedQuery = em.createQuery("SELECT OBJECT(o) FROM AuthenticatedUser AS o where o.identifier = :username", AuthenticatedUser.class);
        typedQuery.setParameter("username", username);
        AuthenticatedUser authenticatedUser = null;
        try {
            authenticatedUser = typedQuery.getSingleResult();
        } catch (NoResultException | NonUniqueResultException ex) {
            logger.info("caught " + ex.getClass() + " querying for " + username);
        }
        return authenticatedUser;
    }

    
    public AuthenticatedUser save( AuthenticatedUser user ) {
        if ( user.getId() == null ) {
            em.persist(this);
        } else {
            user = em.merge(user);
        }
        em.flush();
        String indexingResult = indexService.indexUser(user);
        logger.log(Level.INFO, "during user save, indexing result was: {0}", indexingResult);

        return user;
    }

    // TODO is this needed at all?
    public List<ApiToken> findAllApiKeys() {
        TypedQuery<ApiToken> typedQuery = em.createQuery("SELECT OBJECT(o) FROM ApiKey o", ApiToken.class);
        return typedQuery.getResultList();
    }

}
