package edu.harvard.iq.dataverse;

import java.util.List;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 *
 * @author Marina
 */
@Stateless
@Named
public class FileAccessRequestServiceBean {

    private static final String AUTHENTICATED_USER_ID = "authenticatedUserId";

    private static final String DATA_FILE_ID = "dataFileId";

    private static final String REQUEST_STATE = "requestState";

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public FileAccessRequest find(Object pk) {
        return em.find(FileAccessRequest.class, pk);
    }

    public List<FileAccessRequest> findAll() {
        return em.createQuery("select object(o) from FileAccessRequest as o order by o.id", FileAccessRequest.class).getResultList();
    }

    public List<FileAccessRequest> findAll(Long authenticatedUserId, Long fileId, FileAccessRequest.RequestState requestState) {
        return em.createNamedQuery("FileAccessRequest.findByAuthenticatedUserIdAndDataFileIdAndRequestState", FileAccessRequest.class)
                .setParameter(AUTHENTICATED_USER_ID, authenticatedUserId)
                .setParameter(DATA_FILE_ID, fileId)
                .setParameter(REQUEST_STATE, requestState)
                .getResultList();
    }

    public List<FileAccessRequest> findAllByAuthenticedUserId(Long authenticatedUserId) {
        return em.createNamedQuery("FileAccessRequest.findByAuthenticatedUserId", FileAccessRequest.class)
                        .setParameter(AUTHENTICATED_USER_ID, authenticatedUserId)
                        .getResultList();
    }

    public List<FileAccessRequest> findAllByGuestbookResponseId(Long guestbookResponseId) {
        return em.createNamedQuery("FileAccessRequest.findByGuestbookResponseId", FileAccessRequest.class)
                        .setParameter("guestbookResponseId", guestbookResponseId)
                        .getResultList();

    }

    public List<FileAccessRequest> findAllByDataFileId(Long dataFileId) {
        return em.createNamedQuery("FileAccessRequest.findByDataFileId", FileAccessRequest.class)
                        .setParameter(DATA_FILE_ID, dataFileId)
                        .getResultList();
    }

    public List<FileAccessRequest> findAllByAuthenticatedUserIdAndRequestState(Long authenticatedUserId, FileAccessRequest.RequestState requestState) {
        return em.createNamedQuery("FileAccessRequest.findByAuthenticatedUserIdAndRequestState", FileAccessRequest.class)
                        .setParameter(AUTHENTICATED_USER_ID, authenticatedUserId)
                        .setParameter(REQUEST_STATE, requestState)
                        .getResultList();
    }

    public List<FileAccessRequest> findAllByGuestbookResponseIdAndRequestState(Long guestbookResponseId, FileAccessRequest.RequestState requestState) {
        return em.createNamedQuery("FileAccessRequest.findByGuestbookResponseIdAndRequestState", FileAccessRequest.class)
                        .setParameter(DATA_FILE_ID, guestbookResponseId)
                        .setParameter(REQUEST_STATE, requestState)
                        .getResultList();
    }

    public List<FileAccessRequest> findAllByDataFileIdAndRequestState(Long dataFileId, FileAccessRequest.RequestState requestState) {
        return em.createNamedQuery("FileAccessRequest.findByDataFileIdAndRequestState", FileAccessRequest.class)
                        .setParameter(DATA_FILE_ID, dataFileId)
                        .setParameter(REQUEST_STATE, requestState)
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
