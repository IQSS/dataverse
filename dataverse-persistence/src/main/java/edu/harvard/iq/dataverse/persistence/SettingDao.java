package edu.harvard.iq.dataverse.persistence;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Database access object to {@link Setting} entity.
 * 
 * @author madryk
 */
@Stateless
public class SettingDao {

    @PersistenceContext
    private EntityManager em;
    
    // -------------------- LOGIC --------------------
    
    public Setting find(String name) {
        return em.find(Setting.class, name);
    }
    
    public List<Setting> findAll() {
        return em.createNamedQuery("Setting.findAll", Setting.class).getResultList();
    }
    
    public Setting save(Setting setting) {
        return em.merge(setting);
    }
    
    public void delete(String name) {
        em.createNamedQuery("Setting.deleteByName")
            .setParameter("name", name)
            .executeUpdate();
    }
}
