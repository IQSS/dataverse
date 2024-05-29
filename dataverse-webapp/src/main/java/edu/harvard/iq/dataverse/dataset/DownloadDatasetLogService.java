package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.persistence.dataset.DownloadDatasetLog;
import edu.harvard.iq.dataverse.util.SystemConfig;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Date;
import java.util.logging.Logger;

@Stateless
public class DownloadDatasetLogService {

    private static final Logger logger = Logger.getLogger(DownloadDatasetLogService.class.getCanonicalName());

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @EJB
    protected SystemConfig systemConfig;

    // -------------------- LOGIC --------------------

    /**
     * Logs the event of downloading whole dataset
     */
    public void logWholeSetDownload(Long datasetId) {
        DownloadDatasetLog downloadDatasetLog = new DownloadDatasetLog();
        downloadDatasetLog.setDatasetId(datasetId);
        downloadDatasetLog.setDownloadDate(new Date());
        if (systemConfig.isReadonlyMode()) {
            logger.info(downloadDatasetLog.toString());
        } else {
            em.persist(downloadDatasetLog);
        }
    }
}
