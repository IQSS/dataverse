package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Singleton;

import java.util.List;

@Singleton
public class DatasetRepository extends JpaRepository<Long, Dataset> {

    // -------------------- CONSTRUCTORS --------------------

    public DatasetRepository() {
        super(Dataset.class);
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
}
