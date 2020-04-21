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
    
    public List<FileAccessRequest> findAllByAuthenticedUserId(Long authenticatedUserId){
        return em.createNamedQuery("FileAccessRequest.findByAuthenticatedUserId", FileAccessRequest.class)
                        .setParameter("authenticatedUserId", authenticatedUserId)
                        .getResultList();
    }
    
    public List<FileAccessRequest> findAllByGuestbookResponseId(Long guestbookResponseId){
        return em.createNamedQuery("FileAccessRequest.findByGuestbookResponseId", FileAccessRequest.class)
                        .setParameter("guestbookResponseId", guestbookResponseId)
                        .getResultList();
    
    }
    
    public List<FileAccessRequest> findAllByDataFileId(Long dataFileId){
        return em.createNamedQuery("FileAccessRequest.findByDataFileId", FileAccessRequest.class)
                        .setParameter("dataFileId", dataFileId)
                        .getResultList();
    }
    
    public List<FileAccessRequest> findAllByAuthenticatedUserIdAndRequestState(Long authenticatedUserId, FileAccessRequest.RequestState requestState){
        return em.createNamedQuery("FileAccessRequest.findByAuthenticatedUserIdAndRequestState", FileAccessRequest.class)
                        .setParameter("authenticatedUserId", authenticatedUserId)
                        .setParameter("requestState",requestState)
                        .getResultList();
    }
    
    public List<FileAccessRequest> findAllByGuestbookResponseIdAndRequestState(Long guestbookResponseId, FileAccessRequest.RequestState requestState){
        return em.createNamedQuery("FileAccessRequest.findByGuestbookResponseIdAndRequestState", FileAccessRequest.class)
                        .setParameter("dataFileId", guestbookResponseId)
                        .setParameter("requestState",requestState)
                        .getResultList();
    }
    
    public List<FileAccessRequest> findAllByDataFileIdAndRequestState(Long dataFileId, FileAccessRequest.RequestState requestState){
        return em.createNamedQuery("FileAccessRequest.findByDataFileIdAndRequestState", FileAccessRequest.class)
                        .setParameter("dataFileId", dataFileId)
                        .setParameter("requestState",requestState)
                        .getResultList();
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
