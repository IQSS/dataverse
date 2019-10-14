package edu.harvard.iq.dataverse;

import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author Marina
 */
@Stateless
@Named
public class FileAccessRequestServiceBean {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    public FileAccessRequest find(Object pk) {
        return em.find(FileAccessRequest.class, pk);
    }

    public FileAccessRequest save(FileAccessRequest far) {
        if (far.getId() == null) {
            em.persist(far);
            return far;
        } else {
            return em.merge(far);
        }
    }
  
    
}
