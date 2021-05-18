package edu.harvard.iq.dataverse.datafile;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class FileAccessRequestDao {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public void updateAuthenticatedUser(Long consumedUserId, Long baseUserId) {
        em.createNativeQuery("UPDATE fileaccessrequests SET authenticated_user_id=" + baseUserId +
                " WHERE authenticated_user_id=" + consumedUserId).executeUpdate();
    }
}
