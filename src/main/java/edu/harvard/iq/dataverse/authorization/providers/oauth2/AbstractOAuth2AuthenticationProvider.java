package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.AuthorizationUrlBuilder;
import com.github.scribejava.core.oauth.OAuth20Service;
import edu.harvard.iq.dataverse.LoginPage;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.AuthenticationProviderDisplayInfo;
import edu.harvard.iq.dataverse.util.BundleUtil;

import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for OAuth2 identity providers, such as GitHub and ORCiD.
 *
 * TODO: this really should become an interface (contract with {@link OAuth2LoginBackingBean}) when refactoring package
 * 
 * @author michael
 */
public abstract class AbstractOAuth2AuthenticationProvider implements AuthenticationProvider {

    final static Logger logger = Logger.getLogger(AbstractOAuth2AuthenticationProvider.class.getName());

    protected static class ParsedUserResponse {
        public final AuthenticatedUserDisplayInfo displayInfo;
        public final String userIdInProvider;
        public final String username;
        public final List<String> emails = new ArrayList<>();

        public ParsedUserResponse(AuthenticatedUserDisplayInfo aDisplayInfo, String aUserIdInProvider, String aUsername, List<String> someEmails) {
            displayInfo = aDisplayInfo;
            userIdInProvider = aUserIdInProvider;
            username = aUsername;
            emails.addAll(emails);
        }
        public ParsedUserResponse(AuthenticatedUserDisplayInfo displayInfo, String userIdInProvider, String username) {
            this(displayInfo, userIdInProvider, username, Collections.emptyList());
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 47 * hash + Objects.hashCode(this.userIdInProvider);
            hash = 47 * hash + Objects.hashCode(this.username);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ParsedUserResponse other = (ParsedUserResponse) obj;
            if (!Objects.equals(this.userIdInProvider, other.userIdInProvider)) {
                return false;
            }
            if (!Objects.equals(this.username, other.username)) {
                return false;
            }
            if (!Objects.equals(this.displayInfo, other.displayInfo)) {
                return false;
            }
            return Objects.equals(this.emails, other.emails);
        }

        @Override
        public String toString() {
            return "ParsedUserResponse{" + "displayInfo=" + displayInfo + ", userIdInProvider=" + userIdInProvider + ", username=" + username + ", emails=" + emails + '}';
        }
    }
    
    protected String id;
    protected String title;
    protected String subTitle;
    protected String clientId;
    protected String clientSecret;
    protected String baseUserEndpoint;
    protected String redirectUrl;
    
    /**
     * List of scopes to be requested for authorization at identity provider.
     * Defaults to empty so no scope will be requested (use case: public info from GitHub)
     */
    protected List<String> scope = Arrays.asList("");
    
    /**
     * TODO: when refactoring the package to be about token flow auth, this hard dependency should be removed.
     */
    public abstract DefaultApi20 getApiInstance();
    
    protected abstract ParsedUserResponse parseUserResponse( String responseBody );
    
    /**
     * Build an Authorization URL for this identity provider
     * @param state A randomized state, necessary to secure the authorization flow. @see OAuth2LoginBackingBean.createState()
     * @param callbackUrl URL where the provider should send the browser after authn in code flow
     */
    public String buildAuthzUrl(String state, String callbackUrl) {
        OAuth20Service svc = this.getService(callbackUrl);
        
        AuthorizationUrlBuilder aub = svc.createAuthorizationUrlBuilder().state(state);
        // Do not include scope if empty string (necessary for GitHub)
        if (!this.getSpacedScope().isEmpty()) { aub.scope(this.getSpacedScope()); }
        
        return aub.build();
    }
    
    /**
     * Build an OAuth20Service based on client ID & secret, also inserting the
     * callback URL. Build uses the real API object for the target service like GitHub etc.
     * @param callbackUrl URL where the OAuth2 Provider should send browsers to after authz.
     * @return A usable OAuth20Service object
     */
    public OAuth20Service getService(String callbackUrl) {
        return new ServiceBuilder(getClientId())
                    .apiSecret(getClientSecret())
                    .callback(callbackUrl)
                    .build(getApiInstance());
    }
    
    /**
     * Receive user data from OAuth2 provider after authn/z has been successfull. (Callback view uses this)
     * Request a token and access the resource, parse output and return user details.
     * @param code The authz code sent from the provider
     * @param state The state which was communicated between us and the provider, identifying the exact request
     * @param redirectUrl The redirect URL (some providers require this when fetching the access token, e. g. Google)
     * @return A user record containing all user details accessible for us
     * @throws IOException Thrown when communication with the provider fails
     * @throws OAuth2Exception Thrown when we cannot access the user details for some reason
     * @throws InterruptedException Thrown when the requests thread is failing
     * @throws ExecutionException Thrown when the requests thread is failing
     */
    public OAuth2UserRecord getUserRecord(String code, String state, String redirectUrl)
        throws IOException, OAuth2Exception, InterruptedException, ExecutionException {
        
        OAuth20Service service = getService(redirectUrl);
        OAuth2AccessToken accessToken = service.getAccessToken(code);
        
        // We need to check if scope is null first: GitHub is used without scope, so the responses scope is null.
        // Checking scopes via Stream to be independent from order.
        if ( ( accessToken.getScope() != null && ! getScope().stream().allMatch(accessToken.getScope()::contains) ) ||
             ( accessToken.getScope() == null && ! getSpacedScope().isEmpty() ) ) {
            // We did not get the permissions on the scope(s) we need. Abort and inform the user.
            throw new OAuth2Exception(200, BundleUtil.getStringFromBundle("auth.providers.insufficientScope", Arrays.asList(this.getTitle())), "");
        }
        OAuthRequest request = new OAuthRequest(Verb.GET, getUserEndpoint(accessToken));
        request.setCharset("UTF-8");
        if (id.equals("microsoft")) {
            request.addHeader("Accept", "application/json");
        }
        service.signRequest(accessToken, request);
        Response response = service.execute(request);
        int responseCode = response.getCode();
        String body = response.getBody();
        logger.log(Level.FINE, "In requestUserRecord. Body: {0}", body);
        if ( responseCode == 200 && body != null ) {
            return getUserRecord(body, accessToken, service);
        } else {
            throw new OAuth2Exception(responseCode, body, BundleUtil.getStringFromBundle("auth.providers.exception.userinfo", Arrays.asList(this.getTitle())));
        }
    }
    
    /**
     * Get the user record from the response body.
     * Might be overriden by subclasses to add information from the access token response not included
     * within the request response body.
     * @param accessToken Access token used to create the request
     * @param responseBody The response body = message from provider
     * @param service Not used in base class, but may be used in overrides to lookup more data
     * @return A complete record to be forwarded to user handling logic
     * @throws OAuth2Exception When some lookup fails in overrides
     */
    protected OAuth2UserRecord getUserRecord(@NotNull String responseBody, @NotNull OAuth2AccessToken accessToken, @NotNull OAuth20Service service)
        throws OAuth2Exception {
        
        final ParsedUserResponse parsed = parseUserResponse(responseBody);
        return new OAuth2UserRecord(getId(), parsed.userIdInProvider,
            parsed.username,
            OAuth2TokenData.from(accessToken),
            parsed.displayInfo,
            parsed.emails);
    }

    @Override
    public boolean isUserInfoUpdateAllowed() {
        return true;
    }

    @Override
    public void updateUserInfo(String userIdInProvider, AuthenticatedUserDisplayInfo updatedUserData) {
        // ignore - no account info is stored locally.
        // We override this to prevent the UnsupportedOperationException thrown by
        // the default implementation.
    }

    @Override
    public AuthenticationProviderDisplayInfo getInfo() {
        return new AuthenticationProviderDisplayInfo(getId(), getTitle(), getSubTitle());
    }
    
    /**
     * Used in {@link LoginPage#listAuthenticationProviders()} for sorting the providers in the UI
     * TODO: this might be extended to use a value set by the admin when configuring the provider via JSON.
     * @return an integer value (sort ascending)
     */
    @Override
    public int getOrder() { return 100; }
    
    @Override
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
    
    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getUserEndpoint( OAuth2AccessToken token ) {
        return baseUserEndpoint;
    }
    
    public String getRedirectUrl() {
        return redirectUrl;
    }

    public Optional<String> getIconHtml() {
        return Optional.empty();
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSubTitle(String subtitle) {
        this.subTitle = subtitle;
    }

    public String getSubTitle() {
        return subTitle;
    }
    
    public List<String> getScope() { return scope; }
    
    public String getSpacedScope() { return String.join(" ", getScope()); }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.id);
        hash = 97 * hash + Objects.hashCode(this.clientId);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if ( ! (obj instanceof AbstractOAuth2AuthenticationProvider)) {
            return false;
        }
        final AbstractOAuth2AuthenticationProvider other = (AbstractOAuth2AuthenticationProvider) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.clientId, other.clientId)) {
            return false;
        }
        return Objects.equals(this.clientSecret, other.clientSecret);
    }

    @Override
    public boolean isOAuthProvider() {
        return true;
    }

    public enum DevOAuthAccountType {
        PRODUCTION,
        RANDOM_EMAIL0,
        RANDOM_EMAIL1,
        RANDOM_EMAIL2,
        RANDOM_EMAIL3,
    };
}
