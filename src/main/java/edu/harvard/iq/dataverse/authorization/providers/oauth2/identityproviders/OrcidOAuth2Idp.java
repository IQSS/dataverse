package edu.harvard.iq.dataverse.authorization.providers.oauth2.identityproviders;

import com.github.scribejava.core.builder.api.BaseApi;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import java.util.logging.Logger;

/**
 * OAuth2 identity provider for ORCiD. Note that ORCiD has two systems: sandbox
 * and production. Hence having the user endpoint as a parameter.
 * @author michael
 */
public class OrcidOAuth2Idp extends AbstractOAuth2AuthenticationProvider {
    
    public OrcidOAuth2Idp(String clientId, String clientSecret, String userEndpoint) {
        scope = "/read-limited"; 
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.userEndpoint = userEndpoint;
    }
    
    @Override
    public BaseApi getApiInstance() {
        return OrcidApi.instance( ! userEndpoint.contains("sandbox") );
    }

    @Override
    protected ParsedUserResponse parseUserResponse(String responseBody) {
        Logger.getAnonymousLogger().info("ORCiD Response:");
        Logger.getAnonymousLogger().info(responseBody);
        String username = null;
        return new ParsedUserResponse(new AuthenticatedUserDisplayInfo("fn", "ln", "email", "aff", "pos"), "id in ORCiD", username);
    }
    
}
