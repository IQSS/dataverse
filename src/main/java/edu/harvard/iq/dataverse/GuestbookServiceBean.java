/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author skraffmiller
 */
@Stateless
@Named
public class GuestbookServiceBean implements java.io.Serializable {
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public List<Guestbook> findGuestbooksForGivenDataverse(Dataverse dataverse) {
        if (dataverse != null) {
            Query query = em.createNamedQuery("Guestbook.findByDataverse");
            query.setParameter("dataverse", dataverse);
            return query.getResultList();
        } else {
            return List.of();
        }
    }
    // Get all guestbooks for this collection and it's parent collections
    public List<Guestbook> findEffectiveGuestbooksForGivenDataverse(Dataverse dataverse) {
        List<Guestbook> guestbooks = findGuestbooksForGivenDataverse(dataverse);
        if (dataverse != null) {
            List<Dataverse> parentDataverses = dataverse.getOwners();
            for (Dataverse dv : parentDataverses) {
                guestbooks.addAll(findGuestbooksForGivenDataverse(dv));
            }
        }
        return guestbooks;
    }

    public Long findCountUsages(Long guestbookId, Long dataverseId) {
        String queryString = "";
        if (guestbookId != null && dataverseId != null) {
            queryString = "select count(o.id) from Dataset  o, DvObject obj  where o.id = obj.id and  o.guestbook_id  = " + guestbookId + " and obj.owner_id = " + dataverseId + ";";
            Query query = em.createNativeQuery(queryString);
            return (Long) query.getSingleResult();
        } else if (guestbookId != null && dataverseId == null) {
            queryString = "select count(o.id) from Dataset  o  where o.guestbook_id  = " + guestbookId + " ";
            Query query = em.createNativeQuery(queryString);
            return (Long) query.getSingleResult();
        } else {
            return new Long(0);
        }
    }

    /**
     * Batch version of {@link #findCountUsages(Long, Long)}: counts dataset
     * usages for many guestbooks with a single {@code GROUP BY} query
     * instead of one query per guestbook. Parameters are bound (unlike the
     * concatenated single-row query above).
     *
     * @param guestbookIds guestbook ids; empty or null yields an empty map
     * @param dataverseId optional owner scope, as in the single-row method
     * @return usage counts by guestbook id (zero-count ids are absent)
     */
    @SuppressWarnings("unchecked")
    public Map<Long, Long> findCountUsagesByGuestbookIds(Collection<Long> guestbookIds, Long dataverseId) {
        Map<Long, Long> counts = new HashMap<>();
        if (guestbookIds == null || guestbookIds.isEmpty()) {
            return counts;
        }
        List<Long> ids = new ArrayList<>();
        for (Long id : guestbookIds) {
            if (id != null) {
                ids.add(id);
            }
        }
        if (ids.isEmpty()) {
            return counts;
        }
        String queryString;
        if (dataverseId == null) {
            queryString = "SELECT d.guestbook.id, COUNT(d) FROM Dataset d "
                    + "WHERE d.guestbook.id IN :ids GROUP BY d.guestbook.id";
        } else {
            queryString = "SELECT d.guestbook.id, COUNT(d) FROM Dataset d "
                    + "WHERE d.guestbook.id IN :ids AND d.owner.id = :dvId GROUP BY d.guestbook.id";
        }
        Query query = em.createQuery(queryString);
        query.setParameter("ids", ids);
        if (dataverseId != null) {
            query.setParameter("dvId", dataverseId);
        }
        for (Object row : query.getResultList()) {
            Object[] columns = (Object[]) row;
            counts.put(((Number) columns[0]).longValue(), ((Number) columns[1]).longValue());
        }
        return counts;
    }
    
    public Long findCountResponsesForGivenDataset(Long guestbookId, Long datasetId) {
        String queryString = "";
        if (guestbookId != null && datasetId != null) {
            queryString = "select count(*) from guestbookresponse where guestbook_id = " + guestbookId + " and dataset_id = " + datasetId + ";";
            Query query = em.createNativeQuery(queryString);
            return (Long) query.getSingleResult();
        } else {
            return new Long(0);
        }
    }
    
            
   public Guestbook find(Object pk) {
        return em.find(Guestbook.class, pk);
    }

    public Guestbook save(Guestbook guestbook) {
        if (guestbook.getId() == null) {
            em.persist(guestbook);
            return guestbook;
        } else {
            return em.merge(guestbook);
        }
    }
    
}
