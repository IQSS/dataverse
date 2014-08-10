package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.ApiKey;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserLookup;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

@Stateless
@Named
public class UserServiceBean {

    private static final Logger logger = Logger.getLogger(UserServiceBean.class.getCanonicalName());

    @PersistenceContext
    EntityManager em;

    public List<AuthenticatedUser> findAllAuthenticatedUsers() {
        Query query = em.createQuery("SELECT OBJECT(o) FROM AuthenticatedUser AS o");
        List resultList = query.getResultList();
        return resultList;
    }

    // FIXEME this is user id, not name (username is on the idp)
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

    public AuthenticatedUserLookup findByPersitentIdFromIdp( String idp, String persistentIdFromIdp ) {
        TypedQuery<AuthenticatedUserLookup> typedQuery = em.createNamedQuery("AuthenticatedUserLookup.findByIdp,PersistentId", AuthenticatedUserLookup.class);
        typedQuery.setParameter("persistentId", persistentIdFromIdp);
        typedQuery.setParameter("idp", idp);
        try {
            return typedQuery.getSingleResult();
        } catch (NoResultException | NonUniqueResultException ex) {
            logger.info("caught " + ex.getClass() + " querying for " + persistentIdFromIdp);
            return null;
        }
    }
    
    public AuthenticatedUser findUser( String idp, String persistentIdpId ) {
        AuthenticatedUserLookup lu = findByPersitentIdFromIdp(idp, persistentIdpId);
        return (lu != null ) ? lu.getAuthenticatedUser() : null;
    }

    public List<AuthenticatedUserLookup> findByAllLookupStrings() {
        TypedQuery<AuthenticatedUserLookup> typedQuery = em.createQuery("SELECT OBJECT(o) FROM AuthenticatedUserLookup o", AuthenticatedUserLookup.class);
        return typedQuery.getResultList();
    }

    public List<ApiKey> findAllApiKeys() {
        TypedQuery<ApiKey> typedQuery = em.createQuery("SELECT OBJECT(o) FROM ApiKey o", ApiKey.class);
        return typedQuery.getResultList();
    }

    public ApiKey findApiKey(String key) {
        TypedQuery<ApiKey> typedQuery = em.createQuery("SELECT OBJECT(o) FROM ApiKey o where o.key = :apiKey", ApiKey.class);
        typedQuery.setParameter("apiKey", key);
        ApiKey apiKey = null;
        try {
            apiKey = typedQuery.getSingleResult();
        } catch (NoResultException | NonUniqueResultException ex) {
            logger.info("caught " + ex.getClass() + " querying for " + key);
        }
        return apiKey;
    }
}
