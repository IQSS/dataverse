package edu.harvard.iq.dataverse.license;

import edu.harvard.iq.dataverse.persistence.datafile.license.License;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Stateless
public class LicenseDAO {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;


    //-------------------- LOGIC --------------------

    public License find(long id) {
        return em.find(License.class, id);
    }

    public License findFirstActive() {
        return em.createQuery("SELECT l FROM License l WHERE l.active = true ORDER BY l.position ASC", License.class)
                .setMaxResults(1)
                .getSingleResult();
    }

    public List<License> findAll() {
        return em.createQuery("SELECT l FROM License l ORDER BY l.position ASC", License.class).getResultList();
    }

    public List<License> findActive() {
        return em.createQuery("SELECT l FROM License l WHERE l.active = true ORDER BY l.position ASC", License.class).getResultList();
    }

    public License saveChanges(License license) {
        return em.merge(license);
    }

    public void save(License license) {
        em.persist(license);
    }

    public Long countActiveLicenses() {
        return em.createQuery("SELECT count(l) FROM License l where l.active = true", Long.class).getSingleResult();
    }

    public Long countInactiveLicenses() {
        return em.createQuery("SELECT count(l) FROM License l where l.active = false ", Long.class).getSingleResult();
    }

    public Long findMaxLicensePosition() {
        return em.createQuery("SELECT MAX(l.position) FROM License l", Long.class).getSingleResult();
    }
}
