package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Singleton;

@Singleton
public class DatasetRepository extends JpaRepository<Long, Dataset> {

    // -------------------- CONSTRUCTORS --------------------

    public DatasetRepository() {
        super(Dataset.class);
    }
}
