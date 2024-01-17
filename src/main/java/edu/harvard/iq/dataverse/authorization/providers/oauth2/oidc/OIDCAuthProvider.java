package edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.UserInfoRequest;
import com.nimbusds.openid.connect.sdk.UserInfoResponse;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderConfigurationRequest;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthorizationSetupException;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.AbstractOAuth2AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2Exception;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2UserRecord;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TODO: this should not EXTEND, but IMPLEMENT the contract to be used in {@link edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2LoginBackingBean}
 */
public class OIDCAuthProvider extends AbstractOAuth2AuthenticationProvider {
    
    private static final Logger logger = Logger.getLogger(OIDCAuthProvider.class.getName());
    
    protected String id = "oidc";
    protected String title = "Open ID Connect";
    protected List<String> scope = Arrays.asList("openid", "email", "profile");
    
    final Issuer issuer;
    final ClientAuthentication clientAuth;
    final OIDCProviderMetadata idpMetadata;
    final boolean pkceEnabled;
    final CodeChallengeMethod pkceMethod;
    
    /**
     * Using PKCE, we create and send a special {@link CodeVerifier}. This contains a secret
     * we need again when verifying the response by the provider, thus the cache.
     * To be absolutely sure this may not be abused to DDoS us and not let unused verifiers rot,
     * use an evicting cache implementation and not a standard map.
     */
    private final Cache<String,CodeVerifier> verifierCache = Caffeine.newBuilder()
        .maximumSize(JvmSettings.OIDC_PKCE_CACHE_MAXSIZE.lookup(Integer.class))
        .expireAfterWrite(Duration.of(JvmSettings.OIDC_PKCE_CACHE_MAXAGE.lookup(Integer.class), ChronoUnit.SECONDS))
        .build();
    
    public OIDCAuthProvider(String aClientId, String aClientSecret, String issuerEndpointURL,
                            boolean pkceEnabled, String pkceMethod) throws AuthorizationSetupException {
        this.clientSecret = aClientSecret; // nedded for state creation
        this.clientAuth = new ClientSecretBasic(new ClientID(aClientId), new Secret(aClientSecret));
        this.issuer = new Issuer(issuerEndpointURL);
        
        this.idpMetadata = getMetadata();
        
        this.pkceEnabled = pkceEnabled;
        this.pkceMethod = CodeChallengeMethod.parse(pkceMethod);
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
    public boolean isDisplayIdentifier() {
        return false;
    }
    
    /**
     * Setup metadata from OIDC provider during creation of the provider representation
     * @return The OIDC provider metadata, if successfull
     * @throws IOException when sth. goes wrong with the retrieval
     * @throws ParseException when the metadata is not parsable
     */
    OIDCProviderMetadata getMetadata() throws AuthorizationSetupException {
        try {
            var metadata = getMetadata(this.issuer);
            // Assert that the provider supports the code flow
            if (metadata.getResponseTypes().stream().noneMatch(ResponseType::impliesCodeFlow)) {
                throw new AuthorizationSetupException("OIDC provider at "+this.issuer.getValue()+" does not support code flow, disabling.");
            }
            return metadata;
        } catch (IOException ex) {
            logger.severe("OIDC provider metadata at \"+issuerEndpointURL+\" not retrievable: "+ex.getMessage());
            throw new AuthorizationSetupException("OIDC provider metadata at "+this.issuer.getValue()+" not retrievable.");
        } catch (ParseException ex) {
            logger.severe("OIDC provider metadata at \"+issuerEndpointURL+\" not parsable: "+ex.getMessage());
            throw new AuthorizationSetupException("OIDC provider metadata at "+this.issuer.getValue()+" not parsable.");
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
        CodeVerifier pkceVerifier = pkceEnabled ? new CodeVerifier() : null;
        
        AuthenticationRequest req = new AuthenticationRequest.Builder(new ResponseType("code"),
                                                                      Scope.parse(this.scope),
                                                                      this.clientAuth.getClientID(),
                                                                      callback)
            .endpointURI(idpMetadata.getAuthorizationEndpointURI())
            .state(stateObject)
            // Called method is nullsafe - will disable sending a PKCE challenge in case the verifier is not present
            .codeChallenge(pkceVerifier, pkceMethod)
            .nonce(nonce)
            .build();
        
        // Cache the PKCE verifier, as we need the secret in it for verification later again, after the client sends us
        // the auth code! We use the state to cache the verifier, as the state is unique per authentication event.
        if (pkceVerifier != null) {
            this.verifierCache.put(state, pkceVerifier);
        }
        
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
    public OAuth2UserRecord getUserRecord(String code, String state, String redirectUrl) throws IOException, OAuth2Exception {
        // Retrieve the verifier from the cache and clear from the cache. If not found, will be null.
        // Will be sent to token endpoint for verification, so if required but missing, will lead to exception.
        CodeVerifier verifier = verifierCache.getIfPresent(state);
        
        // Create grant object - again, this is null-safe for the verifier
        AuthorizationGrant codeGrant = new AuthorizationCodeGrant(
            new AuthorizationCode(code), URI.create(redirectUrl), verifier);
    
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

    /**
     * Trades an access token for an {@link UserRecordIdentifier} (if valid).
     *
     * @apiNote The resulting {@link UserRecordIdentifier} may be used with
     *          {@link edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean#lookupUser(UserRecordIdentifier)}
     *          to look up an {@link edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser} from the database.
     * @see edu.harvard.iq.dataverse.api.auth.BearerTokenAuthMechanism
     *
     * @param accessToken The token to use when requesting user information from the provider
     * @return Returns an {@link UserRecordIdentifier} for a valid access token or an empty {@link Optional}.
     * @throws IOException In case communication with the endpoint fails to succeed for an I/O reason
     */
    public Optional<UserRecordIdentifier> getUserIdentifier(BearerAccessToken accessToken) throws IOException {
        OAuth2UserRecord userRecord;
        try {
            // Try to retrieve with given token (throws if invalid token)
            Optional<UserInfo> userInfo = getUserInfo(accessToken);
            
            if (userInfo.isPresent()) {
                // Take this detour to avoid code duplication and potentially hard to track conversion errors.
                userRecord = getUserRecord(userInfo.get());
            } else {
                // This should not happen - an error at the provider side will lead to an exception.
                logger.log(Level.WARNING,
                    "User info retrieval from {0} returned empty optional but expected exception for token {1}.",
                    List.of(getId(), accessToken).toArray()
                );
                return Optional.empty();
            }
        } catch (OAuth2Exception e) {
            logger.log(Level.FINE,
                "Could not retrieve user info with token {0} at provider {1}: {2}",
                List.of(accessToken, getId(), e.getMessage()).toArray());
            logger.log(Level.FINER, "Retrieval failed, details as follows: ", e);
            return Optional.empty();
        }
        
        return Optional.of(userRecord.getUserRecordIdentifier());
    }
}
