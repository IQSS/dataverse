package edu.harvard.iq.dataverse.repositorystorageabstractionlayer;

import edu.harvard.iq.dataverse.StorageLocation;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class RepositoryStorageAbstractionLayerServiceBean {

    private static final Logger logger = Logger.getLogger(RepositoryStorageAbstractionLayerServiceBean.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public StorageLocation find(long id) {
        TypedQuery<StorageLocation> typedQuery = em.createQuery("SELECT OBJECT(o) FROM StorageLocation AS o WHERE o.id = :id", StorageLocation.class);
        typedQuery.setParameter("id", id);
        try {
            return typedQuery.getSingleResult();
        } catch (NoResultException | NonUniqueResultException ex) {
            return null;
        }
    }

    public List<StorageLocation> findAll() {
        // TODO: order by primary first, then whatever
        TypedQuery<StorageLocation> typedQuery = em.createQuery("SELECT OBJECT(o) FROM StorageLocation AS o ORDER BY o.id", StorageLocation.class);
        return typedQuery.getResultList();
    }

    public StorageLocation add(StorageLocation toPersist) {
        StorageLocation persisted = null;
        try {
            persisted = em.merge(toPersist);
        } catch (Exception ex) {
            logger.info("Exception caught during add: " + ex);
        }
        return persisted;
    }

    public boolean delete(long id) {
        StorageLocation doomed = find(id);
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

}
