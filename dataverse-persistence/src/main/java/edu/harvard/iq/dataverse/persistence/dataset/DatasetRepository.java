package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.persistence.JpaRepository;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.TypedQuery;
import java.util.List;

@Singleton
public class DatasetRepository extends JpaRepository<Long, Dataset> {

    // -------------------- CONSTRUCTORS --------------------

    public DatasetRepository() {
        super(Dataset.class);
    }

    // -------------------- LOGIC --------------------

    /**
     * Note that this method directly adds a lock to the database rather than adding it via
     * engine.submit(new AddLockCommand(ctxt.getRequest(), ctxt.getDataset(), datasetLock));
     * which would update the dataset's list of locks, etc.
     * An em.find() for the dataset would get a Dataset that has an updated list of locks,
     * but this copy would not have any changes made in a calling command (e.g. for a PostPublication workflow),
     * the fact that the latest version is 'released' is not yet in the database.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void lockDataset(Dataset dataset, AuthenticatedUser user, DatasetLock.Reason reason)  {
        final DatasetLock datasetLock = new DatasetLock(reason, user);
        datasetLock.setDataset(dataset);
        em.persist(datasetLock);
        em.flush();
    }

    /**
     * Since the lockDataset command above directly persists a lock to the database,
     * the ctxt.getDataset() is not updated and its list of locks can't be used.
     * Using the named query below will find the workflow lock and remove it
     * (actually all workflow locks for this Dataset but only one workflow should be active).
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void unlockDataset(Dataset dataset, DatasetLock.Reason reason)  {
        TypedQuery<DatasetLock> lockCounter = em.createNamedQuery("DatasetLock.getLocksByDatasetId", DatasetLock.class);
        lockCounter.setParameter("datasetId", dataset.getId());
        List<DatasetLock> locks = lockCounter.getResultList();
        for (DatasetLock lock : locks) {
            if (reason == lock.getReason()) {
                log.trace("Removing lock");
                em.remove(lock);
            }
        }
        em.flush();
    }
}
