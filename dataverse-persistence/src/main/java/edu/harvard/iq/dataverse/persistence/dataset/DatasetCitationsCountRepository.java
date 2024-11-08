package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;

import java.util.Optional;

@Stateless
public class DatasetCitationsCountRepository extends JpaRepository<Long, DatasetCitationsCount> {

    // -------------------- CONSTRUCTORS --------------------

    public DatasetCitationsCountRepository() {
        super(DatasetCitationsCount.class);
    }
    
    public DatasetCitationsCountRepository(final EntityManager em) {
        
        super(DatasetCitationsCount.class);
        super.em = em;
    }

    // -------------------- LOGIC --------------------

    public Optional<DatasetCitationsCount> findByDatasetId(Long datasetId) {
        return getSingleResult(em.createQuery(
                "select dcc " +
                "from DatasetCitationsCount dcc " +
                "where dcc.dataset.id = :datasetId ",
                DatasetCitationsCount.class)
        .setParameter("datasetId", datasetId)
        .setMaxResults(1));
    }
}
