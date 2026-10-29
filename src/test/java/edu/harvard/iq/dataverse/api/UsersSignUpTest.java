package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2Exception;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2UserRecord;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OrcidSignUpTokens;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.impl.OrcidOAuth2AP;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.impl.SignUpBuiltinUserCommand;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.cache.CacheFactoryBean;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import jakarta.json.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@LocalJvmSettings
@JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-builtin-user-signup")
@JvmSetting(key = JvmSettings.API_SIGNUP_ORCID_REDIRECT_URIS, value = "https://other.example.edu/sign-up, https://spa.example.edu/sign-up")
class UsersSignUpTest {

    private static final String REDIRECT_URI = "https://spa.example.edu/sign-up";
    private static final String CLIENT_SECRET = "orcid-client-secret";
    private static final String ORCID_URL = "https://orcid.org/0000-0002-1825-0097";
    private static final String SIGN_UP_JSON = "{\"username\":\"alovelace\",\"firstName\":\"Ada\",\"lastName\":\"Lovelace\","
            + "\"emailAddress\":\"ada@example.edu\",\"password\":\"Str0ngPassw0rd\",\"termsAccepted\":true%s}";

    @Mock
    private EjbDataverseEngine engineSvc;
    @Mock
    private AuthenticationServiceBean authSvc;
    @Mock
    private SettingsServiceBean settingsSvc;
    @Mock
    private HttpServletRequest httpRequest;
    @Mock
    private CacheFactoryBean cacheFactory;
    @Mock
    private OrcidOAuth2AP orcidProvider;

    @InjectMocks
    private Users users;

    @BeforeEach
    void setUp() {
        when(settingsSvc.isTrueForKey(SettingsServiceBean.Key.AllowSignUp, true)).thenReturn(true);
        when(cacheFactory.checkRate(anyString(), anyInt())).thenReturn(true);
        when(httpRequest.getRemoteAddr()).thenReturn("192.0.2.10");
        when(authSvc.getOrcidAuthenticationProvider()).thenReturn(orcidProvider);
        when(orcidProvider.getClientSecret()).thenReturn(CLIENT_SECRET);
        when(orcidProvider.getOrcidUrl("0000-0002-1825-0097")).thenReturn(ORCID_URL);
    }

    @AfterEach
    void clearInterrupt() {
        Thread.interrupted();
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "false", varArgs = "api-builtin-user-signup")
    void signUpIsForbiddenWhenTheFeatureFlagIsOff() {
        assertEquals(403, users.signUpBuiltinUser(signUpJson("")).getStatus());
        assertEquals(403, users.getOrcidSignUpAuthorizationUrl(REDIRECT_URI).getStatus());
        assertEquals(403, users.connectOrcidForSignUp(connectJson(validState())).getStatus());
    }

    @Test
    void signUpIsForbiddenWhenSignUpIsNotAllowed() {
        when(settingsSvc.isTrueForKey(SettingsServiceBean.Key.AllowSignUp, true)).thenReturn(false);

        assertEquals(403, users.signUpBuiltinUser(signUpJson("")).getStatus());
    }

    @Test
    void signUpIsRateLimitedPerSourceAddress() {
        when(cacheFactory.checkRate(anyString(), anyInt())).thenReturn(false);

        assertEquals(429, users.signUpBuiltinUser(signUpJson("")).getStatus());
        assertEquals(429, users.connectOrcidForSignUp(connectJson(validState())).getStatus());
        verify(cacheFactory).checkRate("signup:192.0.2.10", 10);
    }

    @Test
    @JvmSetting(key = JvmSettings.API_SIGNUP_RATE_LIMIT_PER_HOUR, value = "0")
    void signUpIsNotRateLimitedWhenTheLimitIsZero() throws Exception {
        when(cacheFactory.checkRate(anyString(), anyInt())).thenReturn(false);
        when(engineSvc.submit(any(SignUpBuiltinUserCommand.class))).thenReturn(newUser());

        assertEquals(201, users.signUpBuiltinUser(signUpJson("")).getStatus());
        verify(cacheFactory, never()).checkRate(anyString(), anyInt());
    }

    @Test
    void signUpCreatesTheUser() throws Exception {
        when(engineSvc.submit(any(SignUpBuiltinUserCommand.class))).thenReturn(newUser());

        Response response = users.signUpBuiltinUser(signUpJson(""));

        assertEquals(201, response.getStatus());
        assertEquals("/api/admin/authenticatedUsers/alovelace", response.getLocation().toString());
        assertEquals("@alovelace", data(response).getString("identifier"));
    }

    @Test
    void signUpSavesTheOrcidIdFromAValidOrcidToken() throws Exception {
        when(engineSvc.submit(any(SignUpBuiltinUserCommand.class))).thenReturn(newUser());
        String orcidToken = OrcidSignUpTokens.createOrcidToken(ORCID_URL, CLIENT_SECRET, System.currentTimeMillis());

        Response response = users.signUpBuiltinUser(signUpJson(",\"orcidToken\":\"" + orcidToken + "\""));

        assertEquals(201, response.getStatus());
        verify(engineSvc).submit(any(SignUpBuiltinUserCommand.class));
    }

    @Test
    void signUpRejectsAnInvalidOrcidToken() throws Exception {
        Response response = users.signUpBuiltinUser(signUpJson(",\"orcidToken\":\"not-a-token\""));

        assertEquals(400, response.getStatus());
        assertTrue(body(response).getJsonObject("fieldErrors").containsKey(SignUpBuiltinUserCommand.FIELD_ORCID));
        verify(engineSvc, never()).submit(any());
    }

    @Test
    void signUpRejectsAnOrcidTokenWhenOrcidIsNotConfigured() {
        when(authSvc.getOrcidAuthenticationProvider()).thenReturn(null);

        assertEquals(400, users.signUpBuiltinUser(signUpJson(",\"orcidToken\":\"token\"")).getStatus());
    }

    @Test
    void signUpRejectsAffiliationRor() {
        assertEquals(400, users.signUpBuiltinUser(signUpJson(",\"affiliationRor\":\"https://ror.org/03vek6s52\"")).getStatus());
    }

    @Test
    void signUpRejectsInvalidJson() {
        assertEquals(400, users.signUpBuiltinUser("{not json").getStatus());
        assertEquals(400, users.signUpBuiltinUser("{\"username\":\"alovelace\"}").getStatus());
    }

    @Test
    void signUpReturnsCommandErrors() throws Exception {
        when(engineSvc.submit(any(SignUpBuiltinUserCommand.class))).thenThrow(new IllegalCommandException("Sign up is not allowed.", null));

        assertEquals(403, users.signUpBuiltinUser(signUpJson("")).getStatus());
    }

    @Test
    void authorizeReturnsTheOrcidAuthorizationUrl() {
        when(orcidProvider.buildAuthzUrl(anyString(), eq(REDIRECT_URI))).thenReturn("https://orcid.org/oauth/authorize?x=1");

        Response response = users.getOrcidSignUpAuthorizationUrl(REDIRECT_URI);

        assertEquals(200, response.getStatus());
        assertEquals("https://orcid.org/oauth/authorize?x=1", data(response).getString("authorizationUrl"));
    }

    @Test
    void authorizeRejectsARedirectUriThatIsNotAllowed() {
        assertEquals(400, users.getOrcidSignUpAuthorizationUrl("https://evil.example.com/sign-up").getStatus());
        assertEquals(400, users.getOrcidSignUpAuthorizationUrl(null).getStatus());
    }

    @Test
    @JvmSetting(key = JvmSettings.API_SIGNUP_ORCID_REDIRECT_URIS, method = "noValue")
    @JvmSetting(key = JvmSettings.SITE_URL, value = "https://dataverse.example.edu")
    void authorizeAllowsOnlyTheModernSignUpPageOfTheSiteWhenNoRedirectUrisAreSet() {
        when(orcidProvider.buildAuthzUrl(anyString(), anyString())).thenReturn("https://orcid.org/oauth/authorize?x=1");

        assertEquals(200, users.getOrcidSignUpAuthorizationUrl("https://dataverse.example.edu/modern/sign-up").getStatus());
        assertEquals(400, users.getOrcidSignUpAuthorizationUrl(REDIRECT_URI).getStatus());
        assertEquals(400, users.getOrcidSignUpAuthorizationUrl("https://dataverse.example.edu/modern/sign-up/").getStatus());
    }

    @Test
    void orcidEndpointsReturnNotFoundWhenOrcidIsNotConfigured() {
        when(authSvc.getOrcidAuthenticationProvider()).thenReturn(null);

        assertEquals(404, users.getOrcidSignUpAuthorizationUrl(REDIRECT_URI).getStatus());
        assertEquals(404, users.connectOrcidForSignUp(connectJson(validState())).getStatus());
    }

    @Test
    void connectReturnsTheOrcidDetailsAndAnOrcidToken() throws Exception {
        AuthenticatedUserDisplayInfo displayInfo = new AuthenticatedUserDisplayInfo("Ada", "Lovelace", "ada@example.edu", "Harvard University", "Research Data Manager");
        displayInfo.setAffiliationRor("https://ror.org/03vek6s52");
        when(orcidProvider.getUserRecord(eq("orcid-code"), anyString(), eq(REDIRECT_URI))).thenReturn(orcidRecord("ada", displayInfo));

        Response response = users.connectOrcidForSignUp(connectJson(validState()));

        assertEquals(200, response.getStatus());
        JsonObject data = data(response);
        assertEquals(ORCID_URL, data.getString("orcid"));
        assertEquals("ada", data.getString("username"));
        assertEquals("Ada", data.getString("firstName"));
        assertEquals("Harvard University", data.getString("affiliation"));
        assertEquals("https://ror.org/03vek6s52", data.getString("affiliationRor"));
        assertEquals("Research Data Manager", data.getString("position"));
        assertEquals(ORCID_URL, OrcidSignUpTokens.readOrcidToken(data.getString("orcidToken"), CLIENT_SECRET, System.currentTimeMillis()).orElseThrow());
    }

    @Test
    void connectLeavesOutAUsernameThatIsTakenOrInvalid() throws Exception {
        AuthenticatedUserDisplayInfo displayInfo = new AuthenticatedUserDisplayInfo("Ada", "Lovelace", null, null, null);
        when(orcidProvider.getUserRecord(anyString(), anyString(), anyString())).thenReturn(orcidRecord("ada", displayInfo));
        when(authSvc.identifierExists("ada")).thenReturn(true);

        assertFalse(data(users.connectOrcidForSignUp(connectJson(validState()))).containsKey("username"));

        when(orcidProvider.getUserRecord(anyString(), anyString(), anyString())).thenReturn(orcidRecord("a", displayInfo));

        assertFalse(data(users.connectOrcidForSignUp(connectJson(validState()))).containsKey("username"));
    }

    @Test
    void connectRejectsAnOrcidIdLinkedToAnotherAccount() throws Exception {
        when(orcidProvider.getUserRecord(anyString(), anyString(), anyString()))
                .thenReturn(orcidRecord("ada", new AuthenticatedUserDisplayInfo()));
        when(authSvc.lookupUserByOrcid(ORCID_URL)).thenReturn(new AuthenticatedUser());

        assertEquals(409, users.connectOrcidForSignUp(connectJson(validState())).getStatus());
    }

    @Test
    void connectRejectsInvalidRequests() throws Exception {
        assertEquals(400, users.connectOrcidForSignUp("{not json").getStatus());
        assertEquals(400, users.connectOrcidForSignUp("{\"state\":\"" + validState() + "\",\"redirectUri\":\"" + REDIRECT_URI + "\"}").getStatus());
        assertEquals(400, users.connectOrcidForSignUp(connectJson("forged-state")).getStatus());
        assertEquals(400, users.connectOrcidForSignUp("{\"code\":\"orcid-code\",\"state\":\"" + validState()
                + "\",\"redirectUri\":\"https://evil.example.com/sign-up\"}").getStatus());
        verify(orcidProvider, never()).getUserRecord(anyString(), anyString(), anyString());
    }

    @Test
    void connectRejectsAFailedOrcidAuthentication() throws Exception {
        when(orcidProvider.getUserRecord(anyString(), anyString(), anyString())).thenThrow(new IOException("ORCID is down"));

        assertEquals(400, users.connectOrcidForSignUp(connectJson(validState())).getStatus());
    }

    @Test
    void connectRejectsARejectedOrcidCode() throws Exception {
        when(orcidProvider.getUserRecord(anyString(), anyString(), anyString())).thenThrow(new OAuth2Exception(400, "invalid_grant", "Invalid code"));

        assertEquals(400, users.connectOrcidForSignUp(connectJson(validState())).getStatus());
    }

    @Test
    void connectKeepsTheInterruptFlagWhenInterrupted() throws Exception {
        when(orcidProvider.getUserRecord(anyString(), anyString(), anyString())).thenThrow(new InterruptedException());

        assertEquals(400, users.connectOrcidForSignUp(connectJson(validState())).getStatus());
        assertTrue(Thread.currentThread().isInterrupted());
    }

    private static String signUpJson(String extraFields) {
        return String.format(SIGN_UP_JSON, extraFields);
    }

    static String noValue() {
        return null;
    }

    private static String validState() {
        return OrcidSignUpTokens.createState(REDIRECT_URI, CLIENT_SECRET, System.currentTimeMillis());
    }

    private static String connectJson(String state) {
        return "{\"code\":\"orcid-code\",\"state\":\"" + state + "\",\"redirectUri\":\"" + REDIRECT_URI + "\"}";
    }

    private static OAuth2UserRecord orcidRecord(String username, AuthenticatedUserDisplayInfo displayInfo) {
        return new OAuth2UserRecord("orcid", "0000-0002-1825-0097", username, null, displayInfo, List.of());
    }

    private static AuthenticatedUser newUser() {
        AuthenticatedUser user = new AuthenticatedUser();
        user.setUserIdentifier("alovelace");
        return user;
    }

    private static JsonObject body(Response response) {
        return JsonUtil.getJsonObject(response.getEntity().toString());
    }

    private static JsonObject data(Response response) {
        return body(response).getJsonObject("data");
    }
}
