package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import java.util.List;
import java.util.Optional;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * CRUD for {@link OAuth2TokenData}.
 * 
 * @author michael
 */
@Stateless
public class OAuth2TokenDataServiceBean {
    
    @PersistenceContext
    private EntityManager em;
    
    public void store( OAuth2TokenData tokenData ) {
        if ( tokenData.getId() != null ) {
            // token exists, this is an update
            em.merge(tokenData);
            
        } else {
            // ensure there's only one token for each user/service pair.
            em.createNamedQuery("OAuth2TokenData.deleteByUserIdAndProviderId")
                    .setParameter("userId", tokenData.getUser().getId() )
                    .setParameter("providerId", tokenData.getOauthProviderId() )
                    .executeUpdate();
            em.persist( tokenData );
        }
    }
    
    public Optional<OAuth2TokenData> get( long authenticatedUserId, String serviceId ) {
        final List<OAuth2TokenData> tokens = em.createNamedQuery("OAuth2TokenData.findByUserIdAndProviderId", OAuth2TokenData.class)
                .setParameter("userId", authenticatedUserId )
                .setParameter("providerId", serviceId )
                .getResultList();
        return Optional.ofNullable( tokens.isEmpty() ? null : tokens.get(0) );
    }
    
}
