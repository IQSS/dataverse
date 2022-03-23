package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import com.github.scribejava.core.model.OAuth2AccessToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.io.Serializable;
import java.sql.Timestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;

/**
 * Token data for a given user, received from an OAuth2 system. Contains the 
 * user's access token for the remote system, as well as additional data,
 * such as refresh token and expiry date.
 * 
 * Persisting token data is a requirement for ORCID according to
 * https://members.orcid.org/api/news/xsd-20-update which says "Store full
 * responses from token exchange: access tokens, refresh tokens, scope, scope
 * expiry to indicate an iD has been authenticated and with what scope" but we
 * don't know how long responses need to be stored. There is no such requirement
 * to store responses for any other OAuth provider.
 *
 * @author michael
 */
@NamedQueries({
    @NamedQuery( name="OAuth2TokenData.findByUserIdAndProviderId",
                 query = "SELECT d FROM OAuth2TokenData d WHERE d.user.id=:userId AND d.oauthProviderId=:providerId" ),
    @NamedQuery( name="OAuth2TokenData.deleteByUserIdAndProviderId",
                 query = "DELETE FROM OAuth2TokenData d WHERE d.user.id=:userId AND d.oauthProviderId=:providerId" )
        
})
@Entity
public class OAuth2TokenData implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    private AuthenticatedUser user;
    
    private String oauthProviderId;
    
    private Timestamp expiryDate;
    
    /**
     * "Please don't put a maximum size on the storage for an access token" at
     * https://stackoverflow.com/questions/4408945/what-is-the-length-of-the-access-token-in-facebook-oauth2/16365828#16365828
     */
    @Column(columnDefinition = "TEXT")
    private String accessToken;
    
    @Column(length = 64)
    private String refreshToken;
    
    @Column(length = 32)
    private String tokenType;
    
    @Column(columnDefinition = "TEXT")
    private String rawResponse;
    
    
    /**
     * Creates a new {@link OAuth2TokenData} instance, based on the data in 
     * the passed {@link OAuth2AccessToken}.
     * @param accessTokenResponse The token parsed by the ScribeJava library.
     * @return A new, pre-populated {@link OAuth2TokenData}.
     */
    public static OAuth2TokenData from( OAuth2AccessToken accessTokenResponse ) {
        OAuth2TokenData retVal = new OAuth2TokenData();
        retVal.setAccessToken(accessTokenResponse.getAccessToken());
        retVal.setRefreshToken( accessTokenResponse.getRefreshToken() );
        retVal.setTokenType( accessTokenResponse.getTokenType() );
        if ( accessTokenResponse.getExpiresIn() != null ) {
            retVal.setExpiryDate( new Timestamp( System.currentTimeMillis() + accessTokenResponse.getExpiresIn()));
        }
        retVal.setRawResponse( accessTokenResponse.getRawResponse() );
        
        return retVal;
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AuthenticatedUser getUser() {
        return user;
    }

    public void setUser(AuthenticatedUser user) {
        this.user = user;
    }

    public String getOauthProviderId() {
        return oauthProviderId;
    }

    public void setOauthProviderId(String oauthProviderId) {
        this.oauthProviderId = oauthProviderId;
    }

    public Timestamp getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Timestamp expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + (int) (this.id ^ (this.id >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final OAuth2TokenData other = (OAuth2TokenData) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }
    
    
    
}
