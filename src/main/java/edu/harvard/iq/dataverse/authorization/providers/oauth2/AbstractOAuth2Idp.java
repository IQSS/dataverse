package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.BaseApi;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Base class for OAuth2 identity providers, such as GitHub and ORCiD.
 * 
 * @author michael
 */
public abstract class AbstractOAuth2Idp {
    
    protected static class ParsedUserResponse {
        public final AuthenticatedUserDisplayInfo displayInfo;
        public final String userIdInProvider;
        public final String username;

        public ParsedUserResponse(AuthenticatedUserDisplayInfo displayInfo, String userIdInProvider, String username) {
            this.displayInfo = displayInfo;
            this.userIdInProvider = userIdInProvider;
            this.username = username;
        }
        
    }
    
    protected String id;
    protected String title;
    protected String clientId;
    protected String clientSecret;
    protected String userEndpoint;
    protected String redirectUrl;
    protected String imageUrl;
    protected String scope;
    
    public AbstractOAuth2Idp(){}
    
    public abstract BaseApi getApiInstance();
    
    protected abstract ParsedUserResponse parseUserResponse( String responseBody );
    
    public OAuth20Service getService(String state) {
        ServiceBuilder svcBuilder = new ServiceBuilder()
                .apiKey(getClientId())
                .apiSecret(getClientSecret())
                .state(state)
                .callback(getRedirectUrl());
        if ( scope != null ) {        
            svcBuilder.scope(scope);
        }
        return (OAuth20Service) svcBuilder.build( getApiInstance() );
    }
    
    public OAuth2UserRecord getUserRecord(String code, String state) throws IOException, OAuth2Exception {
        OAuth20Service service = getService(state);
        OAuth2AccessToken accessToken = service.getAccessToken(code);
        Logger.getAnonymousLogger().info("Token: " + accessToken.getAccessToken()); // TODO remove.
        
        final OAuthRequest request = new OAuthRequest(Verb.GET, getUserEndpoint(), service);
        service.signRequest(accessToken, request);
        final Response response = request.send();
        int responseCode = response.getCode();
        final String body = response.getBody();
        if ( responseCode == 200 ) {
            ParsedUserResponse parsed = parseUserResponse(body);
            return new OAuth2UserRecord(getId(), parsed.userIdInProvider, parsed.username, accessToken.getAccessToken(), parsed.displayInfo);
        } else {
            throw new OAuth2Exception(responseCode, body, "Error getting the user info record.");
        }
    }
    
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
    
    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getUserEndpoint() {
        return userEndpoint;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.id);
        hash = 97 * hash + Objects.hashCode(this.clientId);
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
        if ( ! (obj instanceof AbstractOAuth2Idp)) {
            return false;
        }
        final AbstractOAuth2Idp other = (AbstractOAuth2Idp) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.clientId, other.clientId)) {
            return false;
        }
        if (!Objects.equals(this.clientSecret, other.clientSecret)) {
            return false;
        }
        return true;
    }

    
}
