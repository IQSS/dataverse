package edu.harvard.iq.dataverse.persistence.harvest;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Stateless;

import java.util.List;

@Stateless
public class OAIRecordRepository extends JpaRepository<Long, OAIRecord> {

    // -------------------- CONSTRUCTORS --------------------

    public OAIRecordRepository() {
        super(OAIRecord.class);
    }

    // -------------------- LOGIC --------------------

    public List<OAIRecord> findBySetName(String setName) {
        return em.createQuery("SELECT r from OAIRecord r where r.setName = :setName", OAIRecord.class)
            .setParameter("setName", setName)
            .getResultList();
    }
    
    public void deleteBySetName(String setName) {
        em.createQuery("delete from OAIRecord hs where hs.setName = :setName", OAIRecord.class)
            .setParameter("setName", setName)
            .executeUpdate();
    }
}
