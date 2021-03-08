package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationSetupException;

import java.net.MalformedURLException;
import java.util.Optional;

public class OIDCValidator {

    private IDTokenValidator validator;

    // -------------------- LOGIC --------------------

    /**
     * Creates reusable validator (see
     * <a href="https://connect2id.com/blog/how-to-validate-an-openid-connect-id-token">
     * library documentation</a>) for ID token validation.
     */
    public void initialize(OIDCAuthenticationProvider provider) throws AuthorizationSetupException {
        OIDCProviderMetadata metadata = provider.getProviderMetadata();
        try {
            validator = new IDTokenValidator(
                    provider.getIssuer(),
                    provider.getClientID(),
                    JWSAlgorithm.RS256,
                    metadata.getJWKSetURI().toURL());
        } catch (MalformedURLException mue) {
            throw new AuthorizationSetupException(String.format("Cannot create validator for provider [%s]", provider.getId()), mue);
        }
    }

    /**
     * Handles validation of received ID token according to
     * <a href="https://openid.net/specs/openid-connect-core-1_0.html#IDTokenValidation">
     * OIDC specification</a>. In case of successful validation
     * returns the set of claims read from the token. Otherwise
     * throws an exception.
     */
    public IDTokenClaimsSet validateIDToken(JWT idToken) throws OAuth2Exception {
        if (validator == null) {
            throw createOAuth2Exception("Validator is not initialized.", null);
        }
        try {
            return validator.validate(idToken, null);
        } catch (BadJOSEException | JOSEException je) {
            throw createOAuth2Exception("Token validation failed.", je);
        }
    }

    /**
     * Handles validation of received user info according to
     * <a href="https://openid.net/specs/openid-connect-core-1_0.html#UserInfoResponse">
     * OIDC specification</a>. On success does nothing, otherwise
     * throws an exception.
     * @param userInfo user info received from external service
     * @param subjectFromIDToken subject read from ID token
     */
    public void validateUserInfoSubject(UserInfo userInfo, Subject subjectFromIDToken) throws OAuth2Exception {
        Boolean valid = Optional.ofNullable(userInfo.getSubject())
                .map(s -> s.equals(subjectFromIDToken))
                .orElseThrow(() -> createOAuth2Exception("Null user info received", null));
        if (!valid) {
            throw createOAuth2Exception("Received subjects do not match.", null);
        }
    }

    // -------------------- PRIVATE --------------------

    private OAuth2Exception createOAuth2Exception(String message, Throwable throwable) {
        return throwable != null
                ? new OAuth2Exception(HTTPResponse.SC_SERVER_ERROR, message, message, throwable)
                : new OAuth2Exception(HTTPResponse.SC_SERVER_ERROR, message, message);
    }
}
