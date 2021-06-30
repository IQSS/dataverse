package edu.harvard.iq.dataverse.persistence.ror;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Singleton;
import javax.persistence.Query;

@Singleton
public class RorDataRepository extends JpaRepository<Long, RorData> {

    // -------------------- CONSTRUCTORS --------------------

    public RorDataRepository() {
        super(RorData.class);
    }

    // -------------------- LOGIC --------------------

    public int truncateAll() {
        Query query = em.createNativeQuery(
                "TRUNCATE TABLE rordata, rordata_acronym, rordata_label, rordata_namealias " +
                        "RESTART IDENTITY");
        return query.executeUpdate();
    }
}
