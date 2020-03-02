package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.persistence.dataset.DownloadDatasetLog;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class DownloadDatasetLogService {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    // -------------------- LOGIC --------------------

    /**
     * Returns the count of whole dataset downloads for the given dataset id
     */
    public int fetchDownloadCountForDataset(Long datasetId) {
        DownloadDatasetLog log = em.find(DownloadDatasetLog.class, datasetId);
        return log != null ? log.getDownloadCount() : 0;
    }

    /**
     * Increments the count of whole dataset downloads for the given dataset id and returns the incremented value
     */
    public int incrementDownloadCountForDataset(Long datasetId) {
        DownloadDatasetLog log = em.find(DownloadDatasetLog.class, datasetId);
        if (log == null) {
            DownloadDatasetLog datasetLog = new DownloadDatasetLog();
            datasetLog.setDatasetId(datasetId);
            datasetLog.setDownloadCount(1);
            em.persist(datasetLog);
            return 1;
        } else {
            int newCount = log.getDownloadCount() + 1;
            log.setDownloadCount(newCount);
            return newCount;
        }
    }
}
