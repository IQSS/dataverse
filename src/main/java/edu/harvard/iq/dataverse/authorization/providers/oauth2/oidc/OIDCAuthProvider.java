package edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc;

import com.github.scribejava.core.builder.api.DefaultApi20;

import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;

/**
 * TODO: this should not EXTEND, but IMPLEMENT the contract to be used in
 * {@link edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2LoginBackingBean}
 */
public class OIDCAuthProvider extends AbstractOAuth2AuthenticationProvider {
    protected String id = "oidc";
    protected String title = "Open ID Connect";

    final String aClientId;
    final String aClientSecret;
    final String issuerEndpointURL;

    public OIDCAuthProvider(String aClientId, String aClientSecret, String issuerEndpointURL) {
        this.aClientId = aClientId;
        this.aClientSecret = aClientSecret;
        this.issuerEndpointURL = issuerEndpointURL;
    }

    public String getClientId() {
        return aClientId;
    }

    public String getClientSecret() {
        return aClientSecret;
    }

    public String getIssuerEndpointURL() {
        return this.issuerEndpointURL;
    }

    @Override
    public boolean isDisplayIdentifier() {
        return false;
    }

    @Override
    public DefaultApi20 getApiInstance() {
        throw new UnsupportedOperationException("OIDC provider cannot provide a ScribeJava API instance object");
    }

    @Override
    protected ParsedUserResponse parseUserResponse(String responseBody) {
        throw new UnsupportedOperationException("OIDC provider uses the SDK to parse the response.");
    }
}
