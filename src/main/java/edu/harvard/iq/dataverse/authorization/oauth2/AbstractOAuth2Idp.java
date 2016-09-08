package edu.harvard.iq.dataverse.authorization.oauth2;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.BaseApi;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import java.io.IOException;
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

        public ParsedUserResponse(AuthenticatedUserDisplayInfo displayInfo, String userIdInProvider) {
            this.displayInfo = displayInfo;
            this.userIdInProvider = userIdInProvider;
        }
        
    }
    
    protected String id;
    protected String title;
    protected String clientId;
    protected String clientSecret;
    protected String userEndpoint;
    protected String redirectUrl;
    protected String imageUrl;
    
    public AbstractOAuth2Idp(){}
    
    public abstract BaseApi getApiInstance();
    
    protected abstract ParsedUserResponse parseUserResponse( String responseBody );
    
    public OAuth20Service getService(String state) {
        return (OAuth20Service) new ServiceBuilder()
                .apiKey(getClientId())
                .apiSecret(getClientSecret())
                .state(state)
                .callback(getRedirectUrl())
                .scope(getUserEndpoint())
                .build( getApiInstance() );
    }
    
    public OAuth2UserRecord getUserRecord(String code, String state) throws IOException, OAuth2Exception {
        OAuth20Service service = getService(state);
        OAuth2AccessToken accessToken = service.getAccessToken(code);
        
        final OAuthRequest request = new OAuthRequest(Verb.GET, getUserEndpoint(), service);
        service.signRequest(accessToken, request);
        final Response response = request.send();
        int responseCode = response.getCode();
        final String body = response.getBody();
        if ( responseCode == 200 ) {
            ParsedUserResponse parsed = parseUserResponse(body);
            return new OAuth2UserRecord(getId(), parsed.userIdInProvider, accessToken.getAccessToken(), parsed.displayInfo);
        } else {
            throw new OAuth2Exception(responseCode, body, "Error while exchanging OAuth code for an access token.");
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
    
}
