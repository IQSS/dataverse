/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse;

import java.util.List;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
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
public class DatasetServiceBean {
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
        
    public Dataset save(Dataset dataset) {
         return em.merge(dataset);
    }   

    public Dataset find(Object pk) {
        return (Dataset) em.find(Dataset.class, pk);
    }    
    
    public List<Dataset> findByOwnerId(Long ownerId) {
         Query query = em.createQuery("select object(o) from Dataset as o where o.owner.id =:ownerId order by o.title");
         query.setParameter("ownerId", ownerId);
         return query.getResultList();
    }  
    
    
}
