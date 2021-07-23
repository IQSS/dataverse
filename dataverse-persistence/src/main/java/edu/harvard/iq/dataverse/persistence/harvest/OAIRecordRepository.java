package edu.harvard.iq.dataverse.persistence.harvest;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Stateless;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
    
    public List<OAIRecord> findBySetNameAndRemoved(String setName, boolean removed) {
        return em.createQuery("SELECT r from OAIRecord r where r.setName = :setName and r.removed = :removed", OAIRecord.class)
            .setParameter("setName", setName)
            .setParameter("removed", removed)
            .getResultList();
    }
    
    public List<OAIRecord> findByGlobalId(String globalId) {
        return em.createQuery("SELECT r from OAIRecord r where r.globalId = :globalId", OAIRecord.class)
                .setParameter("globalId", globalId)
                .getResultList();
    }
    
    public List<OAIRecord> findByGlobalIds(List<String> globalIds) {
        return em.createQuery("SELECT r from OAIRecord r where r.globalId in :globalIds", OAIRecord.class)
                .setParameter("globalIds", globalIds)
                .getResultList();
    }

    public List<OAIRecord> findBySetNameAndLastUpdateBetween(String setName, Date from, Date until) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();

        CriteriaQuery<OAIRecord> query = criteriaBuilder.createQuery(OAIRecord.class);
        Root<OAIRecord> root = query.from(OAIRecord.class);
        
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.equal(root.get("setName"), setName));

        if (from != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("lastUpdateTime"), from));
        }
        if (until != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("lastUpdateTime"), until));
        }
        query.select(root)
            .where(predicates.toArray(new Predicate[]{}))
            .orderBy(criteriaBuilder.asc(root.get("globalId")));
        
        return em.createQuery(query).getResultList();
    }

    public void deleteBySetName(String setName) {
        em.createQuery("delete from OAIRecord hs where hs.setName = :setName", OAIRecord.class)
            .setParameter("setName", setName)
            .executeUpdate();
    }
}
