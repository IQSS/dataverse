/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Query;

/**
 *
 * @author skraffmiller
 */
@Stateless
@Named
public class GuestbookServiceBean implements java.io.Serializable {
    
    @Inject
    EntityManagerBean emBean;
    
    public Long findCountUsages(Long guestbookId, Long dataverseId) {
        String queryString = "";
        if (guestbookId != null && dataverseId != null) {
            queryString = "select count(o.id) from Dataset  o, DvObject obj  where o.id = obj.id and  o.guestbook_id  = " + guestbookId + " and obj.owner_id = " + dataverseId + ";";
            Query query = emBean.getMasterEM().createNativeQuery(queryString);
            return (Long) query.getSingleResult();
        } else if (guestbookId != null && dataverseId == null) {
            queryString = "select count(o.id) from Dataset  o  where o.guestbook_id  = " + guestbookId + " ";
            Query query = emBean.getMasterEM().createNativeQuery(queryString);
            return (Long) query.getSingleResult();
        } else {
            return new Long(0);
        }
    }
    
    public Long findCountResponsesForGivenDataset(Long guestbookId, Long datasetId) {
        String queryString = "";
        if (guestbookId != null && datasetId != null) {
            queryString = "select count(*) from guestbookresponse where guestbook_id = " + guestbookId + " and dataset_id = " + datasetId + ";";
            Query query = emBean.getMasterEM().createNativeQuery(queryString);
            return (Long) query.getSingleResult();
        } else {
            return new Long(0);
        }
    }
    
            
   public Guestbook find(Object pk) {
        return emBean.getEntityManager().find(Guestbook.class, pk);
    }

    public Guestbook save(Guestbook guestbook) {
        if (guestbook.getId() == null) {
            emBean.getMasterEM().persist(guestbook);
            return guestbook;
        } else {
            return emBean.getMasterEM().merge(guestbook);
        }
    }
    
}
