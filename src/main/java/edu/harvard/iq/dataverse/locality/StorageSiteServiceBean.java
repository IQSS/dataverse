package edu.harvard.iq.dataverse.locality;

import java.util.List;
import java.util.logging.Logger;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Stateless
public class StorageSiteServiceBean {

    private static final Logger logger = Logger.getLogger(StorageSiteServiceBean.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public StorageSite find(long id) {
        TypedQuery<StorageSite> typedQuery = em.createQuery("SELECT OBJECT(o) FROM StorageSite AS o WHERE o.id = :id", StorageSite.class);
        typedQuery.setParameter("id", id);
        try {
            return typedQuery.getSingleResult();
        } catch (NoResultException | NonUniqueResultException ex) {
            return null;
        }
    }

    public List<StorageSite> findAll() {
        // TODO: order by primary first, then whatever
        TypedQuery<StorageSite> typedQuery = em.createQuery("SELECT OBJECT(o) FROM StorageSite AS o ORDER BY o.id", StorageSite.class);
        return typedQuery.getResultList();
    }

    public StorageSite add(StorageSite toPersist) {
        StorageSite persisted = null;
        try {
            persisted = em.merge(toPersist);
        } catch (Exception ex) {
            logger.info("Exception caught during add: " + ex);
        }
        return persisted;
    }

    public boolean delete(long id) {
        StorageSite doomed = find(id);
        boolean wasDeleted = false;
        if (doomed != null) {
            logger.fine("deleting id " + doomed.getId());
            em.remove(doomed);
            em.flush();
            wasDeleted = true;
        } else {
            logger.warning("problem deleting id " + id);
        }
        return wasDeleted;
    }

    public StorageSite save(StorageSite storageSite) {
        return em.merge(storageSite);
    }

}
