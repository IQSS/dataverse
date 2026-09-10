package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.api.ApiConstants;
import edu.harvard.iq.dataverse.api.auth.doubles.ContainerRequestTestFake;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@LocalJvmSettings
class SessionCookieAuthMechanismTest {

    private static final String SITE_URL = "https://demo.dataverse.org";

    private SessionCookieAuthMechanism sut;

    @BeforeEach
    public void setUp() {
        sut = new SessionCookieAuthMechanism();
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "false", varArgs = "api-session-auth")
    void testFindUserFromRequest_FeatureFlagDisabled() throws WrappedAuthErrorResponse {
        sut.session = Mockito.mock(DataverseSession.class);

        User actual = sut.findUserFromRequest(new ContainerRequestTestFake());

        assertNull(actual);
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth")
    void testFindUserFromRequest_FeatureFlagEnabled_UserAuthenticated() throws WrappedAuthErrorResponse {
        DataverseSession dataverseSessionStub = Mockito.mock(DataverseSession.class);
        User testAuthenticatedUser = new AuthenticatedUser();
        Mockito.when(dataverseSessionStub.getUser()).thenReturn(testAuthenticatedUser);
        sut.session = dataverseSessionStub;

        User actual = sut.findUserFromRequest(new ContainerRequestTestFake());

        assertEquals(testAuthenticatedUser, actual);
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth")
    void testFindUserFromRequest_FeatureFlagEnabled_GuestSessionUserReturnedAsIs() throws WrappedAuthErrorResponse {
        DataverseSession dataverseSessionStub = Mockito.mock(DataverseSession.class);
        Mockito.when(dataverseSessionStub.getUser()).thenReturn(GuestUser.get());
        sut.session = dataverseSessionStub;

        User actual = sut.findUserFromRequest(new ContainerRequestTestFake());

        assertEquals(GuestUser.get(), actual);
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "false", varArgs = "api-session-auth")
    void testFindUserFromRequest_AccessApiAuthenticatesWithoutSessionAuthFlag() throws WrappedAuthErrorResponse {
        User testAuthenticatedUser = givenSessionUser(new AuthenticatedUser());

        User actual = sut.findUserFromRequest(givenRequest("access/datafile/1", null, null));

        assertEquals(testAuthenticatedUser, actual);
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth")
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFindUserFromRequest_HardeningEnabled_MatchingOrigin() throws WrappedAuthErrorResponse {
        User testAuthenticatedUser = givenSessionUser(new AuthenticatedUser());

        User actual = sut.findUserFromRequest(givenRequest("users/:me", SITE_URL, null));

        assertEquals(testAuthenticatedUser, actual);
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth")
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFindUserFromRequest_HardeningEnabled_MatchingReferer() throws WrappedAuthErrorResponse {
        User testAuthenticatedUser = givenSessionUser(new AuthenticatedUser());

        User actual = sut.findUserFromRequest(givenRequest("access/datafile/1", null, SITE_URL + "/dataset.xhtml?id=1"));

        assertEquals(testAuthenticatedUser, actual);
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth")
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFindUserFromRequest_HardeningEnabled_ForeignOriginRejected() {
        givenSessionUser(new AuthenticatedUser());

        assertThrows(WrappedForbiddenAuthErrorResponse.class,
                () -> sut.findUserFromRequest(givenRequest("users/:me", "https://attacker.example.com", null)));
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth")
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFindUserFromRequest_HardeningEnabled_ForeignRefererRejected() {
        givenSessionUser(new AuthenticatedUser());

        assertThrows(WrappedForbiddenAuthErrorResponse.class,
                () -> sut.findUserFromRequest(givenRequest("access/datafile/1", null, "https://attacker.example.com/p")));
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth")
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFindUserFromRequest_HardeningEnabled_NoHeadersDeclinesSession() throws WrappedAuthErrorResponse {
        givenSessionUser(new AuthenticatedUser());

        User actual = sut.findUserFromRequest(givenRequest("access/datafile/1", null, null));

        assertNull(actual);
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth")
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFindUserFromRequest_HardeningEnabled_GuestExempt() throws WrappedAuthErrorResponse {
        givenSessionUser(GuestUser.get());

        User actual = sut.findUserFromRequest(givenRequest("access/datafile/1", null, null));

        assertEquals(GuestUser.get(), actual);
    }

    private User givenSessionUser(User user) {
        DataverseSession dataverseSessionStub = Mockito.mock(DataverseSession.class);
        Mockito.when(dataverseSessionStub.getUser()).thenReturn(user);
        sut.session = dataverseSessionStub;
        SystemConfig systemConfigStub = Mockito.mock(SystemConfig.class);
        Mockito.when(systemConfigStub.getDataverseSiteUrl()).thenReturn(SITE_URL);
        sut.systemConfig = systemConfigStub;
        return user;
    }

    private ContainerRequestContext givenRequest(String path, String origin, String referer) {
        UriInfo uriInfoStub = Mockito.mock(UriInfo.class);
        Mockito.when(uriInfoStub.getPath()).thenReturn(path);
        ContainerRequestContext requestStub = Mockito.mock(ContainerRequestContext.class);
        Mockito.when(requestStub.getUriInfo()).thenReturn(uriInfoStub);
        Mockito.when(requestStub.getHeaderString(ApiConstants.ORIGIN_HEADER)).thenReturn(origin);
        Mockito.when(requestStub.getHeaderString(ApiConstants.REFERER_HEADER)).thenReturn(referer);
        return requestStub;
    }
}
