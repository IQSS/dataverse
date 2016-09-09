package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import com.github.scribejava.core.builder.api.DefaultApi20;

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
    
    private static OrcidApi instance() {
        return InstanceHolder.INSTANCE;
    }
    
    protected OrcidApi(){}
    
    @Override
    public String getAccessTokenEndpoint() {
        return "https://pub.orcid.org/oauth/token";
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return "https://orcid.org/oauth/authorize";
    }
    
}
