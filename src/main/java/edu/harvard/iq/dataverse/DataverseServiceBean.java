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

/**
 *
 * @author gdurand
 */

@Stateless
@Named
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class DataverseServiceBean {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
        

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void save(Dataverse dataverse) {
         em.merge(dataverse);
    }
    

    public Dataverse find(Object pk) {
        return (Dataverse) em.find(Dataverse.class, pk);
    }    
    
    public List<Dataverse> findAll() {
        return em.createQuery("select object(o) from Dataverse as o order by o.name").getResultList();
    }
}
