/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author skraffmiller
 */
@Stateless
@Named
public class GuestbookServiceBean implements java.io.Serializable {
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    
    public Long findCountUsages(Long guestbookId) {
        String queryString = "";
        if (guestbookId != null) {
            queryString = "select count(o.id) from Dataset  o  where o.guestbook_id  = " + guestbookId + " ";
        } else {
            return new Long(0) ;
        }
        Query query = em.createNativeQuery(queryString);
        return (Long) query.getSingleResult();
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
