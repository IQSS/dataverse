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
 * @author gdurand
 */

@Stateless
@Named
public class DataverseServiceBean {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
        

    public Dataverse save(Dataverse dataverse) {
         return em.merge(dataverse);
    }
    

    public Dataverse find(Object pk) {
        return (Dataverse) em.find(Dataverse.class, pk);
    }    
    
    public List<Dataverse> findAll() {
        return em.createQuery("select object(o) from Dataverse as o order by o.name").getResultList();
    }
    
    public List<Dataverse> findByOwnerId(Long ownerId) {
         Query query = em.createQuery("select object(o) from Dataverse as o where o.owner.id =:ownerId order by o.name");
         query.setParameter("ownerId", ownerId);
         return query.getResultList();
    }  
    
    public Dataverse findRootDataverse() {
        return (Dataverse) em.createQuery("select object(o) from Dataverse as o where o.owner.id = null").getSingleResult();
    }    
}
