package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ErrorResponse;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.Request;
import com.nimbusds.oauth2.sdk.Response;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.PlainClientSecret;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.id.Subject;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderConfigurationRequest;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import edu.harvard.iq.dataverse.authorization.AuthenticationProviderDisplayInfo;
import edu.harvard.iq.dataverse.authorization.common.ExternalIdpUserRecord;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationSetupException;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserDisplayInfo;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.function.Function;

/*
 * Partially based on Dataverse pull request #6433
 */
public class OIDCAuthenticationProvider implements OAuth2AuthenticationProvider {

    private String id = "";
    private String title = "";
    private String subTitle = "";

    private ClientID clientID;
    private PlainClientSecret clientSecret;

    private Scope scope = new Scope(OIDCScopeValue.OPENID, OIDCScopeValue.PROFILE, OIDCScopeValue.EMAIL);

    private Issuer issuer;

    private OIDCProviderMetadata providerMetadata;

    private OIDCValidator validator;

    // -------------------- CONSTRUCTORS --------------------

    public OIDCAuthenticationProvider(String clientId, String clientSecret, String issuerUrl, OIDCValidator validator) {
        this.clientID = new ClientID(clientId);
        this.clientSecret = new ClientSecretBasic(this.clientID, new Secret(clientSecret));
        this.issuer = new Issuer(issuerUrl);
        this.validator = validator;
    }

    public OIDCAuthenticationProvider(String clientId, String clientSecret, String issuerUrl) {
        this(clientId, clientSecret, issuerUrl, new OIDCValidator());
    }

    // -------------------- GETTERS --------------------

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTitle() {
        return title;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public ClientID getClientID() {
        return clientID;
    }

    public Issuer getIssuer() {
        return issuer;
    }

    public OIDCProviderMetadata getProviderMetadata() {
        return providerMetadata;
    }

    // -------------------- LOGIC --------------------

    @Override
    public void initialize() throws AuthorizationSetupException {
        OIDCProviderConfigurationRequest providerConfigurationRequest = new OIDCProviderConfigurationRequest(issuer);
        try {
            providerMetadata = OIDCProviderMetadata.parse(providerConfigurationRequest.toHTTPRequest().send().getContentAsJSONObject());
            if (providerMetadata.getResponseTypes().stream().noneMatch(ResponseType::impliesCodeFlow)) {
                throw new AuthorizationSetupException(String.format("Provider [%s] does not support code flow.", getId()));
            }
            validator.initialize(this);
        } catch (IOException | ParseException e) {
            throw new AuthorizationSetupException(String.format("Cannot initialize OIDC provider [%s]", getId()), e);
        }
    }

    @Override
    public String createAuthorizationUrl(String state, String redirectUrl) {
        return new AuthorizationRequest.Builder(
                new ResponseType(ResponseType.Value.CODE), clientID)
                .endpointURI(providerMetadata.getAuthorizationEndpointURI())
                .redirectionURI(URI.create(redirectUrl))
                .state(new State(state))
                .scope(scope)
                .build()
                .toURI()
                .toString();
    }

    @Override
    public ExternalIdpUserRecord getUserRecord(String code, String _state, String redirectUrl) throws OAuth2Exception {
        AuthorizationGrant codeGrant = new AuthorizationCodeGrant(new AuthorizationCode(code), URI.create(redirectUrl));
        TokenAndSubject tokenAndSubject = obtainTokenAndSubject(codeGrant);
        return createUserRecord(obtainUserInfo(tokenAndSubject));
    }

    @Override
    public String getClientSecret() {
        return clientSecret.getClientSecret()
                .getValue();
    }

    @Override
    public AuthenticationProviderDisplayInfo getInfo() {
        return new AuthenticationProviderDisplayInfo(getId(), getTitle(), getSubTitle());
    }

    /* Must be present as EL has problems with default implementations */
    @Override
    public boolean isDisplayIdentifier() {
        return false;
    }

    /* Must be present as EL has problems with default implementations */
    @Override
    public boolean isOAuthProvider() {
        return true;
    }

    // -------------------- PRIVATE --------------------

    private TokenAndSubject obtainTokenAndSubject(AuthorizationGrant codeGrant) throws OAuth2Exception {
        TokenRequest tokenRequest = new TokenRequest(providerMetadata.getTokenEndpointURI(), clientSecret, codeGrant);
        OIDCTokens tokens = handleRequest(tokenRequest, OIDCTokenResponseParser::parse,
                t -> t.toSuccessResponse().getTokens().toOIDCTokens(), TokenResponse::toErrorResponse);
        IDTokenClaimsSet claims = validator.validateIDToken(tokens.getIDToken());
        return new TokenAndSubject(tokens.getBearerAccessToken(), claims.getSubject());
    }

    private UserInfo obtainUserInfo(TokenAndSubject tokenAndSubject) throws OAuth2Exception {
        UserInfoRequest userInfoRequest = new UserInfoRequest(providerMetadata.getUserInfoEndpointURI(), tokenAndSubject.getToken());
        UserInfo userInfo = handleRequest(userInfoRequest, UserInfoResponse::parse,
                u -> u.toSuccessResponse().getUserInfo(), UserInfoResponse::toErrorResponse);
        validator.validateUserInfoSubject(userInfo, tokenAndSubject.getSubject());
        return userInfo;
    }

    private <R extends Response, S, E extends ErrorResponse> S handleRequest(
            Request request, Parser<R> parser, Function<R, S> onSuccess, Function<R, E> onError) throws OAuth2Exception {
        try {
            R parsedResponse = parser.parse(request.toHTTPRequest().send());
            if (!parsedResponse.indicatesSuccess()) {
                ErrorObject error = onError.apply(parsedResponse).getErrorObject();
                throw new OAuth2Exception(error.getHTTPStatusCode(), error.getDescription(), "OIDC data retrieval error.");
            }
            S result = onSuccess.apply(parsedResponse);
            if (result == null) {
                throw new OAuth2Exception(HTTPResponse.SC_SERVER_ERROR, "Null result", "Null result of request processing");
            }
            return result;
        } catch (ParseException pe) {
            throw new OAuth2Exception(HTTPResponse.SC_BAD_REQUEST, pe.getMessage(), "Cannot parse response.", pe);
        } catch (IOException ioe) {
            throw new OAuth2Exception(HTTPResponse.SC_SERVER_ERROR, ioe.getMessage(), "IO error.", ioe);
        }
    }

    private ExternalIdpUserRecord createUserRecord(UserInfo userInfo) {
        return new ExternalIdpUserRecord(
                getId(),
                userInfo.getSubject().getValue(),
                userInfo.getPreferredUsername(),
                null,
                new AuthenticatedUserDisplayInfo(
                        userInfo.getGivenName(),
                        userInfo.getFamilyName(),
                        userInfo.getEmailAddress(),
                        "", ""),
                null);
    }

    // -------------------- SETTERS --------------------

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    // -------------------- INNER CLASSES --------------------

    private interface Parser<R extends Response> {
        R parse(HTTPResponse response) throws ParseException;
    }

    private static class TokenAndSubject {
        private final BearerAccessToken token;
        private final Subject subject;

        // -------------------- CONSTRUCTORS --------------------

        public TokenAndSubject(BearerAccessToken token, Subject subject) {
            this.token = Objects.requireNonNull(token);
            this.subject = Objects.requireNonNull(subject);
        }

        // -------------------- GETTERS --------------------

        public BearerAccessToken getToken() {
            return token;
        }

        public Subject getSubject() {
            return subject;
        }
    }
}
