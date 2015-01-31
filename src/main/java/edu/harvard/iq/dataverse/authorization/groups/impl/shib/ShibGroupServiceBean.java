package edu.harvard.iq.dataverse.authorization.groups.impl.shib;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Named
@Stateless
public class ShibGroupServiceBean {

    private static final Logger logger = Logger.getLogger(ShibGroupServiceBean.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    /**
     * @return A ShibGroup or null.
     */
    public ShibGroup findById(Long id) {
        TypedQuery<ShibGroup> typedQuery = em.createQuery("SELECT OBJECT(o) FROM ShibGroup o WHERE o.id = :id", ShibGroup.class);
        typedQuery.setParameter("id", id);
        try {
            ShibGroup shibGroup = typedQuery.getSingleResult();
            return shibGroup;
        } catch (NoResultException | NonUniqueResultException ex) {
            return null;
        }
    }

    public List<ShibGroup> findAll() {
        TypedQuery<ShibGroup> typedQuery = em.createQuery("SELECT OBJECT(o) FROM ShibGroup as o", ShibGroup.class);
        return typedQuery.getResultList();
    }

    public ShibGroup save(String name, String shibIdpAttribute, String shibIdp) {
        ShibGroup institutionalGroup = new ShibGroup(name, shibIdpAttribute, shibIdp);
        em.persist(institutionalGroup);
        em.flush();
        ShibGroup merged = em.merge(institutionalGroup);
        return merged;
    }

    public Set<ShibGroup> findFor(AuthenticatedUser authenticatedUser) {
        Set<ShibGroup> groupsForUser = new HashSet<>();
        String shibIdp = authenticatedUser.getShibIdentityProvider();
        logger.fine("IdP for user " + authenticatedUser.getIdentifier() + " is " + shibIdp);
        if (shibIdp != null) {
            /**
             * @todo Rather than a straight string equality match, we have a
             * requirement to support regular expressions:
             * https://docs.google.com/document/d/12Qru8Gjq4oDUiodI00oObHJog65S7QzFfFZuPU3n8aU/edit?usp=sharing
             */
            TypedQuery<ShibGroup> typedQuery = em.createQuery("SELECT OBJECT(o) FROM ShibGroup as o WHERE o.pattern =:shibIdP", ShibGroup.class);
            typedQuery.setParameter("shibIdP", shibIdp);
            List<ShibGroup> matches = typedQuery.getResultList();
            groupsForUser.addAll(matches);
        }
        /**
         * @todo In addition to supporting institution-wide Shibboleth groups
         * (Harvard, UNC, etc.), allow arbitrary Shibboleth attributes to be
         * matched (with a regex) such as "memberOf" etc.
         */
        return groupsForUser;
    }

    /**
     * @todo Delete role assignments that match this Shib group. Otherwise,
     * there will be orphaned records in the roleassignment table.
     */
    public boolean delete(ShibGroup doomed) {
        em.remove(doomed);
        // what could possibly go wrong?
        return true;
    }
}
