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
    
    
    public void create(Trsa entity) {
        em.persist(entity);
    }
    
    
    public void edit(Trsa entity) {
        em.merge(entity);
    }
    
    
    public void remove(Trsa entity) {
        em.remove(em.merge(entity));
    }
    
    
    public Trsa find(long id) {
        return em.find(Trsa.class, id);
    }
    
    
    public List<Trsa> findAll() {
        javax.persistence.criteria.CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
        cq.select(cq.from(Trsa.class));
        return em.createQuery(cq).getResultList();
    }
    
    
    public List<Trsa> findRange(int[] range) {
        javax.persistence.criteria.CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
        cq.select(cq.from(Trsa.class));
        javax.persistence.Query q = em.createQuery(cq);
        q.setMaxResults(range[1] - range[0] + 1);
        q.setFirstResult(range[0]);
        return q.getResultList();
    }

    public long count() {
        javax.persistence.criteria.CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
        javax.persistence.criteria.Root<Trsa> rt = cq.from(Trsa.class);
        cq.select(em.getCriteriaBuilder().count(rt));
        javax.persistence.Query q = em.createQuery(cq);
        return (long) q.getSingleResult();
    }    
    
    
    public Trsa findById(long id) {
        TypedQuery<Trsa> typedQuery = em.createQuery("SELECT OBJECT(o) FROM Trsa AS o WHERE o.id = :id", Trsa.class);
        typedQuery.setParameter("id", id);
        try {
            Trsa trsa = typedQuery.getSingleResult();
            return trsa;
        } catch (NoResultException | NonUniqueResultException ex) {
            return null;
        }
    }
    
}
