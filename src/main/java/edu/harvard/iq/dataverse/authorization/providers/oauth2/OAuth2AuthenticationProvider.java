package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.AuthenticationProviderDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationRequest;
import edu.harvard.iq.dataverse.authorization.AuthenticationResponse;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.identityproviders.GitHubOAuth2Idp;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Central point for OAuth2 authentication providers.
 * @author michael
 */
public class OAuth2AuthenticationProvider implements AuthenticationProvider {
    
    private final Map<String, AbstractOAuth2Idp> providers = new HashMap<>();
    
    public OAuth2AuthenticationProvider() {
        // TODO change this to be from the DB.
        registedProvider( new GitHubOAuth2Idp() );
    }
    
    @Override
    public String getId() {
        return "OAuth2";
    }

    @Override
    public AuthenticationProviderDisplayInfo getInfo() {
        return new AuthenticationProviderDisplayInfo(getId(), "OAuth2.0",
                "Adaptor for multiple OAuth2 providers, such as ORCiD and GitHub.");
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        throw new UnsupportedOperationException("Does not apply for OAuth.");
    }
    
    public final void registedProvider( AbstractOAuth2Idp p ) {
        providers.put( p.getId(), p );
    }

    public AbstractOAuth2Idp getProvider(String prvId) {
        return providers.get(prvId);
    }
    
    public Set<AbstractOAuth2Idp> providers() {
        return new HashSet<>(providers.values());
    }
}
