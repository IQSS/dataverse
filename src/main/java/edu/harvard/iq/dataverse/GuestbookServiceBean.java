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

/**
 *
 * @author skraffmiller
 */
@Stateless
@Named
public class GuestbookServiceBean implements java.io.Serializable {
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    
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
