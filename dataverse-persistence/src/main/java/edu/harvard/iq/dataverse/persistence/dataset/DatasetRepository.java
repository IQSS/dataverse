package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Singleton;
import javax.persistence.EntityManager;

import java.util.List;

@Singleton
public class DatasetRepository extends JpaRepository<Long, Dataset> {

    // -------------------- CONSTRUCTORS --------------------

    public DatasetRepository() {
        super(Dataset.class);
    }
    
    public DatasetRepository(final EntityManager em) {
        
        super(Dataset.class);
        super.em = em;
    }

    // -------------------- LOGIC --------------------

    public List<Dataset> findByOwnerId(Long ownerId) {
        return em.createQuery("SELECT o FROM Dataset o WHERE o.owner.id=:ownerId", Dataset.class)
                .setParameter("ownerId", ownerId)
                .getResultList();
    }

    public List<Long> findIdsByOwnerId(Long ownerId) {
        return em.createQuery("SELECT o.id FROM Dataset o WHERE o.owner.id=:ownerId", Long.class)
                .setParameter("ownerId", ownerId)
                .getResultList();
    }

    public List<Long> findIdsByNullHarvestedFrom() {
        return em.createQuery("SELECT o.id FROM Dataset o WHERE o.harvestedFrom IS null ORDER BY o.id", Long.class)
                .getResultList();
    }

    public List<Dataset> findByNonRegisteredIdentifier() {
        return em.createQuery("SELECT o FROM Dataset o WHERE o.dtype = 'Dataset'" +
                                      " AND o.identifierRegistered = false AND o.harvestedFrom IS NULL ", Dataset.class)
          .getResultList();
    }
}
