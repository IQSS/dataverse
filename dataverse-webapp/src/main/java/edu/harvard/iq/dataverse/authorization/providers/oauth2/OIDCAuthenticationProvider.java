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
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderConfigurationRequest;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import edu.harvard.iq.dataverse.authorization.AuthenticationProviderDisplayInfo;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationSetupException;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserDisplayInfo;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

/*
 * Partially based on Dataverse #6433 pull request
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

    // -------------------- CONSTRUCTORS --------------------

    public OIDCAuthenticationProvider(String clientId, String clientSecret, String issuerUrl) {
        this.clientID = new ClientID(clientId);
        this.clientSecret = new ClientSecretBasic(this.clientID, new Secret(clientSecret));
        this.issuer = new Issuer(issuerUrl);
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
    public OAuth2UserRecord getUserRecord(String code, String _state, String redirectUrl) throws OAuth2Exception {
        AuthorizationGrant codeGrant = new AuthorizationCodeGrant(new AuthorizationCode(code), URI.create(redirectUrl));

        BearerAccessToken token = obtainToken(codeGrant).orElseThrow(() -> createOAuth2Exception("Null token received"));
        return obtainUserInfo(token)
                .map(this::createUserRecord)
                .orElseThrow(() -> createOAuth2Exception("Null user info received"));
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

    private OAuth2Exception createOAuth2Exception(String message) {
        return new OAuth2Exception(HTTPResponse.SC_SERVER_ERROR, message, message);
    }

    private Optional<BearerAccessToken> obtainToken(AuthorizationGrant codeGrant) throws OAuth2Exception {
        TokenRequest tokenRequest = new TokenRequest(providerMetadata.getTokenEndpointURI(), clientSecret, codeGrant);
        BearerAccessToken bearerAccessToken = handleRequest(tokenRequest, OIDCTokenResponseParser::parse,
                t -> t.toSuccessResponse().getTokens().toOIDCTokens().getBearerAccessToken(), TokenResponse::toErrorResponse);
        return Optional.ofNullable(bearerAccessToken);
    }

    private Optional<UserInfo> obtainUserInfo(BearerAccessToken bearerAccessToken) throws OAuth2Exception {
        UserInfoRequest userInfoRequest = new UserInfoRequest(providerMetadata.getUserInfoEndpointURI(), bearerAccessToken);
        UserInfo userInfo = handleRequest(userInfoRequest, UserInfoResponse::parse,
                u -> u.toSuccessResponse().getUserInfo(), UserInfoResponse::toErrorResponse);
        return Optional.ofNullable(userInfo);
    }

    private <R extends Response, S, E extends ErrorResponse> S handleRequest(
            Request request, Parser<R> parser, Function<R, S> onSuccess, Function<R, E> onError) throws OAuth2Exception {
        try {
            R parsedResponse = parser.parse(request.toHTTPRequest().send());
            if (!parsedResponse.indicatesSuccess()) {
                ErrorObject error = onError.apply(parsedResponse).getErrorObject();
                throw new OAuth2Exception(error.getHTTPStatusCode(), error.getDescription(), "OIDC data retrieval error.");
            }
            return onSuccess.apply(parsedResponse);
        } catch (ParseException pe) {
            throw new OAuth2Exception(HTTPResponse.SC_BAD_REQUEST, pe.getMessage(), "Cannot parse response.", pe);
        } catch (IOException ioe) {
            throw new OAuth2Exception(HTTPResponse.SC_SERVER_ERROR, ioe.getMessage(), "IO error.", ioe);
        }
    }

    private OAuth2UserRecord createUserRecord(UserInfo userInfo) {
        return new OAuth2UserRecord(
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
}
