package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Singleton;
import java.util.List;

@Singleton
public class DatasetLockRepository extends JpaRepository<Long, DatasetLock> {

    // -------------------- CONSTRUCTORS --------------------

    public DatasetLockRepository() {
        super(DatasetLock.class);
    }

    // -------------------- LOGIC --------------------

    public List<DatasetLock> findByDatasetId(long datasetId) {
        return em.createQuery(
                        "select l " +
                                "from DatasetLock l " +
                                "where l.dataset.id = :datasetId",
                        DatasetLock.class)
                .setParameter("datasetId", datasetId)
                .getResultList();
    }
}
