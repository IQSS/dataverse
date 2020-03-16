package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.persistence.dataset.DownloadDatasetLog;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Date;

@Stateless
public class DownloadDatasetLogService {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    // -------------------- LOGIC --------------------

    /**
     * Logs the event of downloading whole dataset
     */
    public void logWholeSetDownload(Long datasetId) {
        DownloadDatasetLog downloadDatasetLog = new DownloadDatasetLog();
        downloadDatasetLog.setDatasetId(datasetId);
        downloadDatasetLog.setDownloadDate(new Date());
        em.persist(downloadDatasetLog);
    }
}
