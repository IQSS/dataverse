package edu.harvard.iq.dataverse;

import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;

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

    public List<FileAccessRequest> findAll() {
        return em.createQuery("select object(o) from FileAccessRequest as o order by o.id", FileAccessRequest.class).getResultList();
    }
    
    public List<FileAccessRequest> findAllBy(AuthenticatedUser au){
        return em.createQuery("select object(o) from FileAccessRequest as o where authenticate_duser_id=" + au.getId() + " order by o.id", FileAccessRequest.class).getResultList();
    }
    
    public List<FileAccessRequest> findAllBy(GuestbookResponse gbr){
        return em.createQuery("select object(o) from FileAccessRequest as o where guestbook_response_id=" + gbr.getId() + " order by o.id", FileAccessRequest.class).getResultList();
    }
    
    public List<FileAccessRequest> findAllBy(DataFile dataFile){
        return em.createQuery("select object(o) from FileAccessRequest as o where datafile_id=" + dataFile.getId() + " order by o.id", FileAccessRequest.class).getResultList();
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
