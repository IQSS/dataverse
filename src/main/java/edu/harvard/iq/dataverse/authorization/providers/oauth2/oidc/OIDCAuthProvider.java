package edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc;

import com.github.scribejava.core.builder.api.DefaultApi20;

import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import fish.payara.security.openid.api.AccessToken;
import fish.payara.security.openid.api.OpenIdConstant;

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
    final String issuerIdentifier;
    final String issuerIdentifierField;
    final String subjectIdentifierField;

    public OIDCAuthProvider(String aClientId, String aClientSecret, String issuerEndpointURL, String issuerIdentifier, String issuerIdentifierField, String subjectIdentifierField) {
        this.aClientId = aClientId;
        this.aClientSecret = aClientSecret;
        this.issuerEndpointURL = issuerEndpointURL;
        this.issuerIdentifier = issuerIdentifier == null ? issuerEndpointURL : issuerIdentifier;
        this.issuerIdentifierField = issuerIdentifierField == null ? OpenIdConstant.ISSUER_IDENTIFIER : issuerIdentifierField;
        this.subjectIdentifierField = subjectIdentifierField == null ? OpenIdConstant.SUBJECT_IDENTIFIER : subjectIdentifierField;
    }

    public boolean isIssuerOf(AccessToken accessToken) {
        try {
            final String issuerIdentifierValue = accessToken.getJwtClaims().getStringClaim(issuerIdentifierField).orElse(null);
            return issuerIdentifier.equals(issuerIdentifierValue);
        } catch (final Exception ignore) {
            return false;
        }
    }

    public String getSubject(AccessToken accessToken) {
        try {
            return accessToken.getJwtClaims().getStringClaim(subjectIdentifierField).orElse(null);
        } catch (final Exception ignore) {
            return null;
        }
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
