package edu.harvard.iq.dataverse.authorization;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class OAuthTokenDataDao {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public void removeAuthenticatedUserTokenData(Long consumedUserId) {
        em.createNativeQuery("Delete from OAuth2TokenData where user_id =" +
                consumedUserId).executeUpdate();
    }
}
