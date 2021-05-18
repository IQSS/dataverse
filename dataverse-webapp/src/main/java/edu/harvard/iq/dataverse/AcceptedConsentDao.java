package edu.harvard.iq.dataverse;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class AcceptedConsentDao {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public void updateAuthenticatedUser(Long consumedUserId, Long baseUserId) {
        em.createNativeQuery("UPDATE acceptedconsent SET user_id=" +
                baseUserId + " WHERE user_id=" + consumedUserId).executeUpdate();
    }
}
