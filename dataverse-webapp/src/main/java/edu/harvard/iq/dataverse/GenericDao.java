package edu.harvard.iq.dataverse;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class GenericDao {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public <T> T find(long id, Class<T> type) {
        return em.find(type, id);
    }

    public <T> T merge(T entity) {
        return em.merge(entity);
    }
}
