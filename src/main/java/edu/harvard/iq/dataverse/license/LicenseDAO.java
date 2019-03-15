package edu.harvard.iq.dataverse.license;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
@Named
public class LicenseDAO {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    
    //-------------------- LOGIC --------------------
    
    public License find(long id) {
        return em.find(License.class, id);
    }
    
    public List<License> findAll() {
        return em.createQuery("SELECT l FROM License l ORDER BY l.position ASC", License.class).getResultList();
    }
}
