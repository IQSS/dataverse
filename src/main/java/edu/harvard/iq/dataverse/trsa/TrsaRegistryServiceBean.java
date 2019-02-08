/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.trsa;

import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

/**
 *
 * @author asone
 */
@Stateless
@Named
public class TrsaRegistryServiceBean {

    private static final Logger logger = Logger.getLogger(TrsaRegistryServiceBean.class.getName());
    
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    
    public void create(TrsaRegistry entity) {
        em.persist(entity);
    }
    
    
    public void edit(TrsaRegistry entity) {
        em.merge(entity);
    }
    
    
    public void remove(TrsaRegistry entity) {
        em.remove(em.merge(entity));
    }
    
    
    public TrsaRegistry find(long id) {
        return em.find(TrsaRegistry.class, id);
    }
    
    
    public List<TrsaRegistry> findAll() {
        javax.persistence.criteria.CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
        cq.select(cq.from(TrsaRegistry.class));
        return em.createQuery(cq).getResultList();
    }
    
    
    public List<TrsaRegistry> findRange(int[] range) {
        javax.persistence.criteria.CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
        cq.select(cq.from(TrsaRegistry.class));
        javax.persistence.Query q = em.createQuery(cq);
        q.setMaxResults(range[1] - range[0] + 1);
        q.setFirstResult(range[0]);
        return q.getResultList();
    }

    public long count() {
        javax.persistence.criteria.CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
        javax.persistence.criteria.Root<TrsaRegistry> rt = cq.from(TrsaRegistry.class);
        cq.select(em.getCriteriaBuilder().count(rt));
        javax.persistence.Query q = em.createQuery(cq);
        return (long) q.getSingleResult();
    }    
    
    
    public TrsaRegistry findById(long id) {
        TypedQuery<TrsaRegistry> typedQuery = em.createQuery("SELECT OBJECT(o) FROM TrsaRegistry AS o WHERE o.id = :id", TrsaRegistry.class);
        typedQuery.setParameter("id", id);
        try {
            TrsaRegistry trsaRegistry = typedQuery.getSingleResult();
            return trsaRegistry;
        } catch (NoResultException | NonUniqueResultException ex) {
            return null;
        }
    }
    
}
