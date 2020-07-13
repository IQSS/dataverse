package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLockRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.List;

@Singleton
public class DatasetLockServiceBean {

    private static final Logger log = LoggerFactory.getLogger(DatasetLockServiceBean.class);

    private final DatasetRepository datasets;
    private final DatasetLockRepository locks;

    // -------------------- CONSTRUCTORS --------------------

    /**
     * @deprecated for use by EJB proxy only.
     */
    public DatasetLockServiceBean() {
        this(null, null);
    }

    @Inject
    public DatasetLockServiceBean(DatasetRepository datasets, DatasetLockRepository locks) {
        this.datasets = datasets;
        this.locks = locks;
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
    public void lockDataset(long datasetId, AuthenticatedUser user, DatasetLock.Reason reason)  {
        Dataset dataset = datasets.findById(datasetId)
                .orElseThrow(() -> new IllegalStateException("Dataset " + datasetId + " not found"));
        DatasetLock datasetLock = new DatasetLock(reason, user);
        datasetLock.setDataset(dataset);
        locks.saveAndFlush(datasetLock);
    }

    /**
     * Since the lockDataset command above directly persists a lock to the database,
     * the ctxt.getDataset() is not updated and its list of locks can't be used.
     * Using the named query below will find the workflow lock and remove it
     * (actually all workflow locks for this Dataset but only one workflow should be active).
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void unlockDataset(long datasetId, DatasetLock.Reason reason)  {
        List<DatasetLock> datasetLocks = locks.findByDatasetId(datasetId);
        for (DatasetLock lock : datasetLocks) {
            if (reason == lock.getReason()) {
                log.trace("Removing lock");
                locks.delete(lock);
            }
        }
    }
}
