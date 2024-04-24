package edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc;

import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.api.auth.BearerTokenAuthMechanism;
import edu.harvard.iq.dataverse.api.auth.doubles.BearerTokenKeyContainerRequestTestFake;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2Exception;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2UserRecord;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.mocks.MockAuthenticatedUser;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import edu.harvard.iq.dataverse.util.testing.Tags;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.WebClient;
import org.htmlunit.WebResponse;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlSubmitInput;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc.OIDCAuthenticationProviderFactoryIT.clientId;
import static edu.harvard.iq.dataverse.authorization.providers.oauth2.oidc.OIDCAuthenticationProviderFactoryIT.clientSecret;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.when;

@Tag(Tags.INTEGRATION_TEST)
@Tag(Tags.USES_TESTCONTAINERS)
@Testcontainers(disabledWithoutDocker = true)
@ExtendWith(MockitoExtension.class)
// NOTE: order is important here - Testcontainers must be first, otherwise it's not ready when we call getAuthUrl()
@LocalJvmSettings
@JvmSetting(key = JvmSettings.OIDC_CLIENT_ID, value = clientId)
@JvmSetting(key = JvmSettings.OIDC_CLIENT_SECRET, value = clientSecret)
@JvmSetting(key = JvmSettings.OIDC_AUTH_SERVER_URL, method = "getAuthUrl")
class OIDCAuthenticationProviderFactoryIT {
    
    static final String clientId = "test";
    static final String clientSecret = "94XHrfNRwXsjqTqApRrwWmhDLDHpIYV8";
    static final String realm = "test";
    static final String realmAdminUser = "admin";
    static final String realmAdminPassword = "admin";
    
    static final String adminUser = "kcadmin";
    static final String adminPassword = "kcpassword";
    
    // The realm JSON resides in conf/keycloak/test-realm.json and gets avail here using <testResources> in pom.xml
    @Container
    static KeycloakContainer keycloakContainer = new KeycloakContainer("quay.io/keycloak/keycloak:22.0")
        .withRealmImportFile("keycloak/test-realm.json")
        .withAdminUsername(adminUser)
        .withAdminPassword(adminPassword);
    
    // simple method to retrieve the issuer URL, referenced to by @JvmSetting annotations (do no delete)
    private static String getAuthUrl() {
        return keycloakContainer.getAuthServerUrl() + "/realms/" + realm;
    }
    
    OIDCAuthProvider getProvider() throws Exception {
        OIDCAuthProvider oidcAuthProvider = (OIDCAuthProvider) OIDCAuthenticationProviderFactory.buildFromSettings();
        
        assumeTrue(oidcAuthProvider.getMetadata().getTokenEndpointURI().toString()
            .startsWith(keycloakContainer.getAuthServerUrl()));
        
        return oidcAuthProvider;
    }
    
    // NOTE: This requires the "direct access grants" for the client to be enabled!
    String getBearerTokenViaKeycloakAdminClient() throws Exception {
        try (Keycloak keycloak = KeycloakBuilder.builder()
            .serverUrl(keycloakContainer.getAuthServerUrl())
            .grantType(OAuth2Constants.PASSWORD)
            .realm(realm)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .username(realmAdminUser)
            .password(realmAdminPassword)
            .scope("openid")
            .build()) {
            return keycloak.tokenManager().getAccessTokenString();
        }
    }
    
    /**
     * This basic test covers configuring an OIDC provider via MPCONFIG and being able to use it.
     */
    @Test
    void testCreateProvider() throws Exception {
        // given
        OIDCAuthProvider oidcAuthProvider = getProvider();
        String token = getBearerTokenViaKeycloakAdminClient();
        assumeFalse(token == null);
        
        Optional<UserInfo> info = Optional.empty();
        
        // when
        try {
            info = oidcAuthProvider.getUserInfo(new BearerAccessToken(token));
        } catch (OAuth2Exception e) {
            System.out.println(e.getMessageBody());
        }
        
        //then
        assertTrue(info.isPresent());
        assertEquals(realmAdminUser, info.get().getPreferredUsername());
    }
    
    @Mock
    UserServiceBean userService;
    @Mock
    AuthenticationServiceBean authService;
    
    @InjectMocks
    BearerTokenAuthMechanism bearerTokenAuthMechanism;
    
    /**
     * This test covers using an OIDC provider as authorization party when accessing the Dataverse API with a
     * Bearer Token. See {@link BearerTokenAuthMechanism}. It needs to mock the auth services to avoid adding
     * more dependencies.
     */
    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, varArgs = "api-bearer-auth", value = "true")
    void testApiBearerAuth() throws Exception {
        assumeFalse(userService == null);
        assumeFalse(authService == null);
        assumeFalse(bearerTokenAuthMechanism == null);
        
        // given
        // Get the access token from the remote Keycloak in the container
        String accessToken = getBearerTokenViaKeycloakAdminClient();
        assumeFalse(accessToken == null);
        
        OIDCAuthProvider oidcAuthProvider = getProvider();
        // This will also receive the details from the remote Keycloak in the container
        UserRecordIdentifier identifier = oidcAuthProvider.getUserIdentifier(new BearerAccessToken(accessToken)).get();
        String token = "Bearer " + accessToken;
        BearerTokenKeyContainerRequestTestFake request = new BearerTokenKeyContainerRequestTestFake(token);
        AuthenticatedUser user = new MockAuthenticatedUser();
        
        // setup mocks (we don't want or need a database here)
        when(authService.getAuthenticationProviderIdsOfType(OIDCAuthProvider.class)).thenReturn(Set.of(oidcAuthProvider.getId()));
        when(authService.getAuthenticationProvider(oidcAuthProvider.getId())).thenReturn(oidcAuthProvider);
        when(authService.lookupUser(identifier)).thenReturn(user);
        when(userService.updateLastApiUseTime(user)).thenReturn(user);
        
        // when (let's do this again, but now with the actual subject under test!)
        User lookedUpUser = bearerTokenAuthMechanism.findUserFromRequest(request);
        
        // then
        assertNotNull(lookedUpUser);
        assertEquals(user, lookedUpUser);
    }
    
    /**
     * This test covers the {@link OIDCAuthProvider#buildAuthzUrl(String, String)} and
     * {@link OIDCAuthProvider#getUserRecord(String, String, String)} methods that are used when
     * a user authenticates via the JSF UI. It covers enabling PKCE, which is no hard requirement
     * by the protocol, but might be required by some provider (as seen with Microsoft Azure AD).
     * As we don't have a real browser, we use {@link WebClient} from HtmlUnit as a replacement.
     */
    @Test
    @JvmSetting(key = JvmSettings.OIDC_PKCE_ENABLED, value = "true")
    void testAuthorizationCodeFlowWithPKCE() throws Exception {
        // given
        String state = "foobar";
        String callbackUrl = "http://localhost:8080/oauth2callback.xhtml";
        
        OIDCAuthProvider oidcAuthProvider = getProvider();
        String authzUrl = oidcAuthProvider.buildAuthzUrl(state, callbackUrl);
        //System.out.println(authzUrl);
        
        try (WebClient webClient = new WebClient()) {
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setJavaScriptEnabled(false);
            // We *want* to know about the redirect, as it contains the data we need!
            webClient.getOptions().setRedirectEnabled(false);
            
            HtmlPage loginPage = webClient.getPage(authzUrl);
            assumeTrue(loginPage.getTitleText().contains("Sign in to " + realm));
            
            HtmlForm form = loginPage.getForms().get(0);
            HtmlInput username = form.getInputByName("username");
            HtmlInput password = form.getInputByName("password");
            HtmlSubmitInput submit = form.getInputByName("login");
            
            username.type(realmAdminUser);
            password.type(realmAdminPassword);
            
            FailingHttpStatusCodeException exception = assertThrows(FailingHttpStatusCodeException.class, submit::click);
            assertEquals(302, exception.getStatusCode());
            
            WebResponse response = exception.getResponse();
            assertNotNull(response);
            
            String callbackLocation = response.getResponseHeaderValue("Location");
            assertTrue(callbackLocation.startsWith(callbackUrl));
            //System.out.println(callbackLocation);
            
            String queryPart = callbackLocation.trim().split("\\?")[1];
            Map<String,String> parameters = Pattern.compile("\\s*&\\s*")
                .splitAsStream(queryPart)
                .map(s -> s.split("=", 2))
                .collect(Collectors.toMap(a -> a[0], a -> a.length > 1 ? a[1]: ""));
            //System.out.println(map);
            assertTrue(parameters.containsKey("code"));
            assertTrue(parameters.containsKey("state"));
            
            OAuth2UserRecord userRecord = oidcAuthProvider.getUserRecord(
                parameters.get("code"),
                parameters.get("state"),
                callbackUrl
            );
            
            assertNotNull(userRecord);
            assertEquals(realmAdminUser, userRecord.getUsername());
        } catch (OAuth2Exception e) {
            System.out.println(e.getMessageBody());
            throw e;
        }
    }
}