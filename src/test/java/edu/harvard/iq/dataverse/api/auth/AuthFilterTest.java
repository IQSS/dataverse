package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.api.ApiConstants;
import edu.harvard.iq.dataverse.api.Users;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the session-cookie CSRF hardening in {@link AuthFilter}.
 *
 * <p>The filter's decisions depend only on the auth-mechanism tag, the user,
 * the Origin/Referer/CSRF headers, and the resolved resource method (for the
 * bootstrap exemption) — deliberately NOT on the HTTP method or request path,
 * so no test stubs those.
 */
@LocalJvmSettings
class AuthFilterTest {

    private static final String SITE_URL = "https://demo.dataverse.org";
    private static final String VALID_TOKEN = "valid-token";

    @Test
    void testFilter_HardeningDisabled_DoesNotAbort() throws Exception {
        User user = new AuthenticatedUser();
        Fixture f = fixture(user, false);

        f.sut.filter(f.requestContext);

        verify(f.requestContext, never()).abortWith(any(Response.class));
        verify(f.requestContext).setProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_USER, user);
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_SessionCookie_MatchingOriginAndValidToken_Allowed() throws Exception {
        Fixture f = sessionCookieFixture(new AuthenticatedUser());
        when(f.requestContext.getHeaderString(ApiConstants.ORIGIN_HEADER)).thenReturn(SITE_URL);
        givenValidToken(f);

        f.sut.filter(f.requestContext);

        verify(f.requestContext, never()).abortWith(any(Response.class));
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_SessionCookie_MatchingRefererAndValidToken_Allowed() throws Exception {
        Fixture f = sessionCookieFixture(new AuthenticatedUser());
        when(f.requestContext.getHeaderString(ApiConstants.REFERER_HEADER))
                .thenReturn(SITE_URL + "/dataset.xhtml");
        givenValidToken(f);

        f.sut.filter(f.requestContext);

        verify(f.requestContext, never()).abortWith(any(Response.class));
    }

    /**
     * Browsers omit Origin on same-origin GETs, and Referrer-Policy: no-referrer
     * suppresses Referer — a legitimate same-origin request can carry neither
     * header. The CSRF token is then the deciding credential; failing closed here
     * would make the hardening unusable on no-referrer installations.
     */
    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_SessionCookie_NoOriginNoReferer_ValidTokenSufficient() throws Exception {
        Fixture f = sessionCookieFixture(new AuthenticatedUser());
        givenValidToken(f);

        f.sut.filter(f.requestContext);

        verify(f.requestContext, never()).abortWith(any(Response.class));
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_SessionCookie_NoOriginNoRefererNoToken_Blocked() throws Exception {
        Fixture f = sessionCookieFixture(new AuthenticatedUser());

        f.sut.filter(f.requestContext);

        assertForbidden(f);
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_SessionCookie_CrossOriginReferer_BlockedEvenWithValidToken() throws Exception {
        Fixture f = sessionCookieFixture(new AuthenticatedUser());
        when(f.requestContext.getHeaderString(ApiConstants.REFERER_HEADER))
                .thenReturn("https://evil.example/malicious");
        givenValidToken(f);

        f.sut.filter(f.requestContext);

        assertForbidden(f);
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_SessionCookie_CrossOriginOrigin_Blocked() throws Exception {
        Fixture f = sessionCookieFixture(new AuthenticatedUser());
        when(f.requestContext.getHeaderString(ApiConstants.ORIGIN_HEADER)).thenReturn("https://evil.example");

        f.sut.filter(f.requestContext);

        assertForbidden(f);
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_SessionCookie_MatchingOriginButMissingToken_Blocked() throws Exception {
        Fixture f = sessionCookieFixture(new AuthenticatedUser());
        when(f.requestContext.getHeaderString(ApiConstants.ORIGIN_HEADER)).thenReturn(SITE_URL);
        // CSRF token header absent (mock returns null by default)

        f.sut.filter(f.requestContext);

        assertForbidden(f);
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_SessionCookie_MatchingOriginButWrongToken_Blocked() throws Exception {
        Fixture f = sessionCookieFixture(new AuthenticatedUser());
        when(f.requestContext.getHeaderString(ApiConstants.ORIGIN_HEADER)).thenReturn(SITE_URL);
        when(f.requestContext.getHeaderString(ApiConstants.CSRF_TOKEN_HEADER)).thenReturn("wrong-token");
        when(f.session.matchesApiCsrfToken("wrong-token")).thenReturn(false);

        f.sut.filter(f.requestContext);

        assertForbidden(f);
    }

    /**
     * A guest session carries no privileges worth forging, so hardening
     * short-circuits before the origin/token checks.
     */
    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_GuestSessionCookieRequest_SkipsHardening() throws Exception {
        Fixture f = sessionCookieFixture(GuestUser.get());
        // No Origin/Referer and no CSRF token — would block an authenticated session.

        f.sut.filter(f.requestContext);

        verify(f.requestContext, never()).abortWith(any(Response.class));
        verify(f.requestContext).setProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_USER, GuestUser.get());
    }

    /**
     * PrivateUrlUser.isAuthenticated() is false, so a read-only private-URL
     * preview session is exempt like a guest: hardening it adds no protection
     * (no state to forge, responses not cross-origin readable) and the JSF
     * preview flow that mints these sessions cannot obtain a token.
     */
    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_PrivateUrlUserSessionCookieRequest_IsExemptLikeGuest() throws Exception {
        Fixture f = sessionCookieFixture(new PrivateUrlUser(42L));
        // No Origin/Referer and no token — would block an authenticated session.

        f.sut.filter(f.requestContext);

        verify(f.requestContext, never()).abortWith(any(Response.class));
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_NonSessionCookieMechanism_SkipsHardening() throws Exception {
        // No auth-mechanism property set — how every non-session mechanism looks.
        Fixture f = fixture(new AuthenticatedUser(), false);

        f.sut.filter(f.requestContext);

        verify(f.requestContext, never()).abortWith(any(Response.class));
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_CsrfBootstrapEndpoint_AllowedWithMatchingOriginAndNoToken() throws Exception {
        Fixture f = sessionCookieFixture(new AuthenticatedUser());
        when(f.requestContext.getHeaderString(ApiConstants.ORIGIN_HEADER)).thenReturn(SITE_URL);
        inject(f.sut, "resourceInfo", mockBootstrapResourceInfo());

        f.sut.filter(f.requestContext);

        verify(f.requestContext, never()).abortWith(any(Response.class));
    }

    /**
     * The bootstrap endpoint must also work when neither Origin nor Referer is
     * sent (no-referrer installations): the request holds the session cookie,
     * and the token it fetches is useless to a party that lacks that cookie.
     */
    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_CsrfBootstrapEndpoint_AllowedWithoutOriginHeaders() throws Exception {
        Fixture f = sessionCookieFixture(new AuthenticatedUser());
        inject(f.sut, "resourceInfo", mockBootstrapResourceInfo());

        f.sut.filter(f.requestContext);

        verify(f.requestContext, never()).abortWith(any(Response.class));
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_CsrfBootstrapEndpoint_CrossOriginBlocked() throws Exception {
        Fixture f = sessionCookieFixture(new AuthenticatedUser());
        when(f.requestContext.getHeaderString(ApiConstants.ORIGIN_HEADER)).thenReturn("https://evil.example");
        inject(f.sut, "resourceInfo", mockBootstrapResourceInfo());

        f.sut.filter(f.requestContext);

        assertForbidden(f);
    }

    /**
     * With an unparseable site URL the allowed origin cannot be established:
     * requests carrying origin headers fail closed, while header-less requests
     * are still decided by the token (ConfigCheckService flags this state at
     * startup).
     */
    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_SessionCookie_UnparseableSiteUrl_OriginCarryingRequestBlocked() throws Exception {
        Fixture f = sessionCookieFixture(new AuthenticatedUser());
        when(f.systemConfig.getDataverseSiteUrl()).thenReturn("not a url");
        when(f.requestContext.getHeaderString(ApiConstants.ORIGIN_HEADER)).thenReturn(SITE_URL);
        givenValidToken(f);

        f.sut.filter(f.requestContext);

        assertForbidden(f);
    }

    // ---- fixtures ---------------------------------------------------------

    private static final class Fixture {
        AuthFilter sut;
        ContainerRequestContext requestContext;
        DataverseSession session;
        SystemConfig systemConfig;
    }

    private Fixture fixture(User user, boolean sessionCookieAuthenticated) throws Exception {
        Fixture f = new Fixture();
        f.sut = new AuthFilter();
        f.requestContext = Mockito.mock(ContainerRequestContext.class);
        f.session = Mockito.mock(DataverseSession.class);
        f.systemConfig = Mockito.mock(SystemConfig.class);
        when(f.systemConfig.getDataverseSiteUrl()).thenReturn(SITE_URL);

        CompoundAuthMechanism compound = Mockito.mock(CompoundAuthMechanism.class);
        when(compound.findUserFromRequest(f.requestContext)).thenReturn(user);
        if (sessionCookieAuthenticated) {
            when(f.requestContext.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM))
                    .thenReturn(ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
        }

        inject(f.sut, "compoundAuthMechanism", compound);
        inject(f.sut, "session", f.session);
        inject(f.sut, "systemConfig", f.systemConfig);
        return f;
    }

    private Fixture sessionCookieFixture(User user) throws Exception {
        return fixture(user, true);
    }

    private void givenValidToken(Fixture f) {
        when(f.requestContext.getHeaderString(ApiConstants.CSRF_TOKEN_HEADER)).thenReturn(VALID_TOKEN);
        when(f.session.matchesApiCsrfToken(VALID_TOKEN)).thenReturn(true);
    }

    private void assertForbidden(Fixture f) {
        ArgumentCaptor<Response> abortResponseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(f.requestContext).abortWith(abortResponseCaptor.capture());
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), abortResponseCaptor.getValue().getStatus());
    }

    private ResourceInfo mockBootstrapResourceInfo() throws NoSuchMethodException {
        ResourceInfo resourceInfo = Mockito.mock(ResourceInfo.class);
        Method method = Users.class.getMethod("getSessionCsrfToken", ContainerRequestContext.class);
        Mockito.<Class<?>>when(resourceInfo.getResourceClass()).thenReturn(Users.class);
        when(resourceInfo.getResourceMethod()).thenReturn(method);
        return resourceInfo;
    }

    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
