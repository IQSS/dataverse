package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.List;


@Stateless
@Named
public class RetentionServiceBean {

    @PersistenceContext
    EntityManager em;

    @EJB
    ActionLogServiceBean actionLogSvc;

    public List<Retention> findAllRetentions() {
        return em.createNamedQuery("Retention.findAll", Retention.class).getResultList();
    }

    public Retention findByRetentionId(Long id) {
        Query query = em.createNamedQuery("Retention.findById", Retention.class);
        query.setParameter("id", id);
        try {
            return (Retention) query.getSingleResult();
        } catch (Exception ex) {
            return null;
        }
    }

    public Retention merge(Retention r) {
        return em.merge(r);
    }
    
    public Long save(Retention retention, String userIdentifier) {
        if (retention.getId() == null) {
            em.persist(retention);
            em.flush();
        }
        //Not quite from a command, but this action can be done by anyone, so command seems better than Admin or other alternatives
        actionLogSvc.log(new ActionLogRecord(ActionLogRecord.ActionType.Command, "retentionCreate")
                .setInfo("id: " + retention.getId() + " date unavailable: " + retention.getDateUnavailable() + " reason: " + retention.getReason()).setUserIdentifier(userIdentifier));
        return retention.getId();
    }

    private int deleteById(long id, String userIdentifier) {
        //Not quite from a command, but this action can be done by anyone, so command seems better than Admin or other alternatives
        actionLogSvc.log(new ActionLogRecord(ActionLogRecord.ActionType.Command, "retentionDelete")
                .setInfo(Long.toString(id))
                .setUserIdentifier(userIdentifier));
        return em.createNamedQuery("Retention.deleteById")
                .setParameter("id", id)
                .executeUpdate();
    }
    public int delete(Retention retention, String userIdentifier) {
        return deleteById(retention.getId(), userIdentifier);
    }
}
