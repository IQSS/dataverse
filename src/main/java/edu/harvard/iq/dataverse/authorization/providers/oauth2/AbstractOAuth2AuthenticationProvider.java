package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.BaseApi;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.AuthenticationProviderDisplayInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for OAuth2 identity providers, such as GitHub and ORCiD.
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
    protected String scope;
    
    public abstract BaseApi<OAuth20Service> getApiInstance();
    
    protected abstract ParsedUserResponse parseUserResponse( String responseBody );
    
    public OAuth20Service getService(String state, String redirectUrl) {
        ServiceBuilder svcBuilder = new ServiceBuilder()
                .apiKey(getClientId())
                .apiSecret(getClientSecret())
                .state(state)
                .callback(redirectUrl);
        if ( scope != null ) {        
            svcBuilder.scope(scope);
        }
        return svcBuilder.build( getApiInstance() );
    }
    
    public OAuth2UserRecord getUserRecord(String code, String state, String redirectUrl) throws IOException, OAuth2Exception {
        OAuth20Service service = getService(state, redirectUrl);
        OAuth2AccessToken accessToken = service.getAccessToken(code);

        final String userEndpoint = getUserEndpoint(accessToken);
        
        final OAuthRequest request = new OAuthRequest(Verb.GET, userEndpoint, service);
        request.addHeader("Authorization", "Bearer " + accessToken.getAccessToken());
        request.setCharset("UTF-8");
        
        final Response response = request.send();
        int responseCode = response.getCode();
        final String body = response.getBody();        
        logger.log(Level.FINE, "In getUserRecord. Body: {0}", body);

        if ( responseCode == 200 ) {
            final ParsedUserResponse parsed = parseUserResponse(body);
            return new OAuth2UserRecord(getId(), parsed.userIdInProvider,
                                        parsed.username, 
                                        OAuth2TokenData.from(accessToken),
                                        parsed.displayInfo,
                                        parsed.emails);
        } else {
            throw new OAuth2Exception(responseCode, body, "Error getting the user info record.");
        }
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
