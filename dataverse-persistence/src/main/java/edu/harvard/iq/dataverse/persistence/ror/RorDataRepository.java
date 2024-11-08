package edu.harvard.iq.dataverse.persistence.ror;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.Query;

@Singleton
public class RorDataRepository extends JpaRepository<Long, RorData> {

    // -------------------- CONSTRUCTORS --------------------

    public RorDataRepository() {
        super(RorData.class);
    }
    
    public RorDataRepository(final EntityManager em) {
        
        super(RorData.class);
        super.em = em;
    }

    // -------------------- LOGIC --------------------

    public int truncateAll() {
        Query query = em.createNativeQuery(
                "TRUNCATE TABLE rordata, rordata_acronym, rordata_label, rordata_namealias " +
                        "RESTART IDENTITY");
        return query.executeUpdate();
    }
}
