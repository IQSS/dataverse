package edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.*;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderConfigurationRequest;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationSetupException;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2Exception;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2TokenData;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2UserRecord;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

/**
 * TODO: this should not EXTEND, but IMPLEMENT the contract to be used in {@link edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2LoginBackingBean}
 */
public class OIDCAuthProvider extends AbstractOAuth2AuthenticationProvider {
    
    private static final Logger logger = Logger.getLogger(OIDCAuthProvider.class.getName());
    
    protected String id = "oidc";
    protected String title = "Open ID Connect";
    protected List<String> scope = Arrays.asList("openid", "email", "profile");
    
    Issuer issuer;
    ClientAuthentication clientAuth;
    OIDCProviderMetadata idpMetadata;
    
    public OIDCAuthProvider(String aClientId, String aClientSecret, String issuerEndpointURL) throws AuthorizationSetupException {
        this.clientSecret = aClientSecret; // nedded for state creation
        this.clientAuth = new ClientSecretBasic(new ClientID(aClientId), new Secret(aClientSecret));
        this.issuer = new Issuer(issuerEndpointURL);
        getMetadata();
    }
    
    /**
     * Although this is defined in {@link edu.harvard.iq.dataverse.authorization.AuthenticationProvider},
     * this needs to be present due to bugs in ELResolver (has been modified for Spring).
     * TODO: for the future it might be interesting to make this configurable via the provider JSON (it's used for ORCID!)
     * @see <a href="https://issues.jboss.org/browse/JBEE-159">JBoss Issue 159</a>
     * @see <a href="https://github.com/eclipse-ee4j/el-ri/issues/43">Jakarta EE Bug 43</a>
     * @return false
     */
    @Override
    public boolean isDisplayIdentifier() { return false; }
    
    /**
     * Setup metadata from OIDC provider during creation of the provider representation
     * @return The OIDC provider metadata, if successfull
     * @throws IOException when sth. goes wrong with the retrieval
     * @throws ParseException when the metadata is not parsable
     */
    void getMetadata() throws AuthorizationSetupException {
        try {
            this.idpMetadata = getMetadata(this.issuer);
        } catch (IOException ex) {
            logger.severe("OIDC provider metadata at \"+issuerEndpointURL+\" not retrievable: "+ex.getMessage());
            throw new AuthorizationSetupException("OIDC provider metadata at "+this.issuer.getValue()+" not retrievable.");
        } catch (ParseException ex) {
            logger.severe("OIDC provider metadata at \"+issuerEndpointURL+\" not parsable: "+ex.getMessage());
            throw new AuthorizationSetupException("OIDC provider metadata at "+this.issuer.getValue()+" not parsable.");
        }
    
        // Assert that the provider supports the code flow
        if (! this.idpMetadata.getResponseTypes().stream().filter(idp -> idp.impliesCodeFlow()).findAny().isPresent()) {
            throw new AuthorizationSetupException("OIDC provider at "+this.issuer.getValue()+" does not support code flow, disabling.");
        }
    }
    
    /**
     * Retrieve metadata from OIDC provider (moved here to be mock-/spyable)
     * @param issuer The OIDC provider (basically a wrapped URL to endpoint)
     * @return The OIDC provider metadata, if successfull
     * @throws IOException when sth. goes wrong with the retrieval
     * @throws ParseException when the metadata is not parsable
     */
    OIDCProviderMetadata getMetadata(Issuer issuer) throws IOException, ParseException {
        // Will resolve the OpenID provider metadata automatically
        OIDCProviderConfigurationRequest request = new OIDCProviderConfigurationRequest(issuer);
    
        // Make HTTP request
        HTTPRequest httpRequest = request.toHTTPRequest();
        HTTPResponse httpResponse = httpRequest.send();
    
        // Parse OpenID provider metadata
        return OIDCProviderMetadata.parse(httpResponse.getContentAsJSONObject());
    }
    
    /**
     * TODO: remove when refactoring package and {@link AbstractOAuth2AuthenticationProvider}
     */
    @Override
    public DefaultApi20 getApiInstance() {
        throw new UnsupportedOperationException("OIDC provider cannot provide a ScribeJava API instance object");
    }
    
    /**
     * TODO: remove when refactoring package and {@link AbstractOAuth2AuthenticationProvider}
     */
    @Override
    protected ParsedUserResponse parseUserResponse(String responseBody) {
        throw new UnsupportedOperationException("OIDC provider uses the SDK to parse the response.");
    }
    
    /**
     * Create the authz URL for the OIDC provider
     * @param state A randomized state, necessary to secure the authorization flow. @see OAuth2LoginBackingBean.createState()
     * @param callbackUrl URL where the provider should send the browser after authn in code flow
     * @return
     */
    @Override
    public String buildAuthzUrl(String state, String callbackUrl) {
        State stateObject = new State(state);
        URI callback = URI.create(callbackUrl);
        Nonce nonce = new Nonce();
        
        AuthenticationRequest req = new AuthenticationRequest.Builder(new ResponseType("code"),
                                                                      Scope.parse(this.scope),
                                                                      this.clientAuth.getClientID(),
                                                                      callback)
            .endpointURI(idpMetadata.getAuthorizationEndpointURI())
            .state(stateObject)
            .nonce(nonce)
            .build();
        
        return req.toURI().toString();
    }
    
    /**
     * Receive user data from OIDC provider after authn/z has been successfull. (Callback view uses this)
     * Request a token and access the resource, parse output and return user details.
     * @param code The authz code sent from the provider
     * @param redirectUrl The redirect URL (some providers require this when fetching the access token, e. g. Google)
     * @return A user record containing all user details accessible for us
     * @throws IOException Thrown when communication with the provider fails
     * @throws OAuth2Exception Thrown when we cannot access the user details for some reason
     * @throws InterruptedException Thrown when the requests thread is failing
     * @throws ExecutionException Thrown when the requests thread is failing
     */
    @Override
    public OAuth2UserRecord getUserRecord(String code, String redirectUrl)
        throws IOException, OAuth2Exception, InterruptedException, ExecutionException {
        // Create grant object
        AuthorizationGrant codeGrant = new AuthorizationCodeGrant(new AuthorizationCode(code), URI.create(redirectUrl));
    
        // Get Access Token first
        Optional<BearerAccessToken> accessToken = getAccessToken(codeGrant);
        
        // Now retrieve User Info
        if (accessToken.isPresent()) {
            Optional<UserInfo> userInfo = getUserInfo(accessToken.get());
            
            // Construct our internal user representation
            if (userInfo.isPresent()) {
                return getUserRecord(userInfo.get());
            }
        }
        
        // this should never happen, as we are throwing exceptions like champs before.
        throw new OAuth2Exception(-1, "", "auth.providers.token.failGetUser");
    }
    
    /**
     * Create the OAuth2UserRecord from the OIDC UserInfo.
     * TODO: extend to retrieve and insert claims about affiliation and position.
     * @param userInfo
     * @return the usable user record for processing ing {@link edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2LoginBackingBean}
     */
    OAuth2UserRecord getUserRecord(UserInfo userInfo) {
        return new OAuth2UserRecord(
            this.getId(),
            userInfo.getSubject().getValue(),
            userInfo.getPreferredUsername(),
            null,
            new AuthenticatedUserDisplayInfo(userInfo.getGivenName(), userInfo.getFamilyName(), userInfo.getEmailAddress(), "", ""),
            null
        );
    }
    
    /**
     * Retrieve the Access Token from provider. Encapsulate for testing.
     * @param grant
     * @return The bearer access token used in code (grant) flow. May be empty if SDK could not cast internally.
     */
    Optional<BearerAccessToken> getAccessToken(AuthorizationGrant grant) throws IOException, OAuth2Exception {
        // Request token
        HTTPResponse response = new TokenRequest(this.idpMetadata.getTokenEndpointURI(),
                                                 this.clientAuth,
                                                 grant,
                                                 Scope.parse(this.scope))
                                        .toHTTPRequest()
                                        .send();
        
        // Parse response
        try {
            TokenResponse tokenRespone = OIDCTokenResponseParser.parse(response);
    
            // If error --> oauth2 ex
            if (! tokenRespone.indicatesSuccess() ) {
                ErrorObject error = tokenRespone.toErrorResponse().getErrorObject();
                throw new OAuth2Exception(error.getHTTPStatusCode(), error.getDescription(), "auth.providers.token.failRetrieveToken");
            }
    
            // Success --> return token
            OIDCTokenResponse successResponse = (OIDCTokenResponse)tokenRespone.toSuccessResponse();
            
            return Optional.of(successResponse.getOIDCTokens().getBearerAccessToken());
            
        } catch (ParseException ex) {
            throw new OAuth2Exception(-1, ex.getMessage(), "auth.providers.token.failParseToken");
        }
    }
    
    /**
     * Retrieve User Info from provider. Encapsulate for testing.
     * @param accessToken The access token to enable reading data from userinfo endpoint
     */
    Optional<UserInfo> getUserInfo(BearerAccessToken accessToken) throws IOException, OAuth2Exception {
        // Retrieve data
        HTTPResponse response = new UserInfoRequest(this.idpMetadata.getUserInfoEndpointURI(), accessToken)
                                        .toHTTPRequest()
                                        .send();
        
        // Parse/Extract
        try {
            UserInfoResponse infoResponse = UserInfoResponse.parse(response);
    
            // If error --> oauth2 ex
            if (! infoResponse.indicatesSuccess() ) {
                ErrorObject error = infoResponse.toErrorResponse().getErrorObject();
                throw new OAuth2Exception(error.getHTTPStatusCode(),
                                          error.getDescription(),
                                          BundleUtil.getStringFromBundle("auth.providers.exception.userinfo", Arrays.asList(this.getTitle())));
            }
            
            // Success --> return info
            return Optional.of(infoResponse.toSuccessResponse().getUserInfo());
            
        } catch (ParseException ex) {
            throw new OAuth2Exception(-1, ex.getMessage(), BundleUtil.getStringFromBundle("auth.providers.exception.userinfo", Arrays.asList(this.getTitle())));
        }
    }
}
