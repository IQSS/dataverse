package edu.harvard.iq.dataverse.authorization.groups;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class ExplicitGroupDao {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public void updateAuthenticatedUser(Long consumedUserId, Long baseUserId) {
        em.createNativeQuery("UPDATE explicitgroup_authenticateduser SET containedauthenticatedusers_id=" +
                baseUserId + " WHERE containedauthenticatedusers_id=" + consumedUserId).executeUpdate();
    }
}
