package edu.harvard.iq.dataverse.authorization.providers.oauth2.impl;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.OAuth2AccessTokenJsonExtractor;
import com.github.scribejava.core.extractors.TokenExtractor;
import com.github.scribejava.core.model.OAuth2AccessToken;

/**
 * Adaptor for ORCiD OAuth identity Provider.
 * @author michael
 */
public class OrcidApi extends DefaultApi20 {
    
    /**
     * The instance holder pattern allows for lazy creation of the instance.
     */
    private static class SandboxInstanceHolder {
        private static final OrcidApi INSTANCE = 
                new OrcidApi("https://sandbox.orcid.org/oauth/token",
                             "https://sandbox.orcid.org/oauth/authorize");
    }
    
    private static class InstanceHolder {
        private static final OrcidApi INSTANCE =
                new OrcidApi("https://orcid.org/oauth/token",
                             "https://orcid.org/oauth/authorize");
    }
    
    public static OrcidApi instance(boolean isProduction) {
        return isProduction ? InstanceHolder.INSTANCE : SandboxInstanceHolder.INSTANCE;
    }
    
    private final String accessTokenEndpoint;
    private final String authorizationBaseUrl;

    protected OrcidApi(String accessTokenEndpoint, String authorizationBaseUrl) {
        this.accessTokenEndpoint = accessTokenEndpoint;
        this.authorizationBaseUrl = authorizationBaseUrl;
    }
    
    @Override
    public String getAccessTokenEndpoint() {
        return accessTokenEndpoint;
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return authorizationBaseUrl;
    }

    @Override
    public TokenExtractor<OAuth2AccessToken> getAccessTokenExtractor() {
        return OAuth2AccessTokenJsonExtractor.instance();
    }
    
}
