package edu.harvard.iq.dataverse.dataset;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class DownloadDatasetLogDao {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    // -------------------- LOGIC --------------------

    public void deleteByDatasetId(Long datasetId) {
        em.createNamedQuery("DownloadDatasetLog.deleteByDatasetId")
                .setParameter("datasetId", datasetId)
                .executeUpdate();
    }
}
