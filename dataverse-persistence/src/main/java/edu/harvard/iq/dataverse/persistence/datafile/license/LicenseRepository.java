package edu.harvard.iq.dataverse.persistence.datafile.license;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Stateless
public class LicenseRepository extends JpaRepository<Long, License> {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;


    public LicenseRepository() {
        super(License.class);
    }

    //-------------------- LOGIC --------------------

    public License findFirstActive() {
        return em.createQuery("SELECT l FROM License l WHERE l.active = true ORDER BY l.position ASC", License.class)
                 .setMaxResults(1)
                 .getSingleResult();
    }

    public Optional<License> findLicenseByName(String licenseName) {
        return JpaRepository.getSingleResult(
                em.createQuery("SELECT l FROM License l WHERE l.name=:licenseName", License.class)
                    .setParameter("licenseName", licenseName));
    }

    public List<License> findAllOrderedByPosition() {
        return em.createQuery("SELECT l FROM License l ORDER BY l.position ASC", License.class).getResultList();
    }

    public List<License> findActiveOrderedByPosition() {
        return em.createQuery("SELECT l FROM License l WHERE l.active = true ORDER BY l.position ASC", License.class)
                 .getResultList();
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
