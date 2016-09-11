package edu.harvard.iq.dataverse.authorization.providers.oauth2.identityproviders;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.OAuth2AccessTokenExtractor;
import com.github.scribejava.core.extractors.OAuth2AccessTokenJsonExtractor;
import com.github.scribejava.core.extractors.TokenExtractor;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.Response;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Adaptor for ORCiD OAuth identity Provider.
 * @author michael
 */
public class OrcidApi extends DefaultApi20 {
    
    /**
     * The instance holder pattern allows for lazy creation of the intance.
     */
    private static class InstanceHolder {
        private static final OrcidApi INSTANCE = new OrcidApi();
    }
    
    public static OrcidApi instance() {
        return InstanceHolder.INSTANCE;
    }
    
    protected OrcidApi(){}
    
    @Override
    public String getAccessTokenEndpoint() {
        return "https://orcid.org/oauth/token";
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return "https://orcid.org/oauth/authorize";
    }

    @Override
    public TokenExtractor<OAuth2AccessToken> getAccessTokenExtractor() {
        return OAuth2AccessTokenJsonExtractor.instance();
    }
    
    static class TE extends OAuth2AccessTokenJsonExtractor {

        @Override
        public OAuth2AccessToken extract(String response) {
            Logger.getAnonymousLogger().info("Response:");
            Logger.getAnonymousLogger().info(response);
            Logger.getAnonymousLogger().info("/Response");
            return super.extract(response);
        }
    
    }
    
}
