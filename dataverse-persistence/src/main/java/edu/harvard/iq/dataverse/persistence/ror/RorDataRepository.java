package edu.harvard.iq.dataverse.persistence.ror;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Singleton;

@Singleton
public class RorDataRepository extends JpaRepository<Long, RorData> {

    // -------------------- CONSTRUCTORS --------------------

    public RorDataRepository() {
        super(RorData.class);
    }
}
