package edu.harvard.iq.dataverse.persistence.harvest;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Stateless;

import java.util.List;

@Stateless
public class OAIRecordRepository extends JpaRepository<Long, OAIRecord> {

    public OAIRecordRepository() {
        super(OAIRecord.class);
    }


    public List<OAIRecord> findBySetName(String setName) {
        return em.createQuery("SELECT r from OAIRecord r where r.setName = :setName", OAIRecord.class)
            .setParameter("setName", setName)
            .getResultList();
    }
}
