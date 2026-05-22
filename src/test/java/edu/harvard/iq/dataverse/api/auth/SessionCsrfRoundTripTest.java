package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.api.ApiConstants;
import edu.harvard.iq.dataverse.api.Users;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Wider unit tests that exercise the {@link AuthFilter} + {@link Users#getSessionCsrfToken}
 * composition with a real {@link DataverseSession} (not mocked). These tests verify the
 * actual CSRF token round-trip: the endpoint mints a token, the filter accepts the same
 * token on follow-up requests, and rejects mismatches.
 *
 * <p>Existing tests mock the session, so they don't catch issues where the endpoint and
 * filter diverge on token storage or comparison.
 */
@LocalJvmSettings
class SessionCsrfRoundTripTest {

    private static final String SITE_URL = "https://demo.dataverse.org";

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testRoundTrip_BootstrapThenMutatingCall_Allowed() throws Exception {
        DataverseSession session = newSessionWithAuthenticatedUser();
        SystemConfig systemConfig = mockSystemConfig();
        AuthenticatedUser user = (AuthenticatedUser) session.getUser();

        // Step 1: bootstrap GET /api/users/:csrf-token through the filter.
        ContainerRequestContext bootstrapCtx = mockSessionCookieRequest("GET", "users/:csrf-token");
        when(bootstrapCtx.getHeaderString("Origin")).thenReturn(SITE_URL);
        AuthFilter filter = new AuthFilter();
        injectAuthFilterDeps(filter, mockCompoundAuth(user), session, systemConfig, mockBootstrapResourceInfo());

        filter.filter(bootstrapCtx);
        verify(bootstrapCtx, never()).abortWith(any(Response.class));

        // Step 2: call the endpoint method directly and verify it issued an OK response.
        Users users = newUsersWithSession(session);
        when(bootstrapCtx.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_USER)).thenReturn(user);
        Response bootstrapResponse = users.getSessionCsrfToken(bootstrapCtx);
        assertEquals(Response.Status.OK.getStatusCode(), bootstrapResponse.getStatus());

        // The endpoint stored the token on the real session — pull it back out to drive the next request.
        String token = session.getOrCreateApiCsrfToken();
        assertNotNull(token);

        // Step 3: subsequent mutating call with that token should pass through the filter.
        ContainerRequestContext mutatingCtx = mockSessionCookieRequest("POST", "datasets/1/add");
        when(mutatingCtx.getHeaderString("Origin")).thenReturn(SITE_URL);
        when(mutatingCtx.getHeaderString(ApiConstants.CSRF_TOKEN_HEADER)).thenReturn(token);
        AuthFilter mutatingFilter = new AuthFilter();
        injectAuthFilterDeps(mutatingFilter, mockCompoundAuth(user), session, systemConfig, null);

        mutatingFilter.filter(mutatingCtx);
        verify(mutatingCtx, never()).abortWith(any(Response.class));
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testRoundTrip_MutatingCallWithWrongToken_Blocked() throws Exception {
        DataverseSession session = newSessionWithAuthenticatedUser();
        SystemConfig systemConfig = mockSystemConfig();
        AuthenticatedUser user = (AuthenticatedUser) session.getUser();
        // Mint a token on the real session.
        String validToken = session.getOrCreateApiCsrfToken();
        assertNotNull(validToken);

        ContainerRequestContext mutatingCtx = mockSessionCookieRequest("POST", "datasets/1/add");
        when(mutatingCtx.getHeaderString("Origin")).thenReturn(SITE_URL);
        when(mutatingCtx.getHeaderString(ApiConstants.CSRF_TOKEN_HEADER))
                .thenReturn(validToken + "-not-quite");

        AuthFilter filter = new AuthFilter();
        injectAuthFilterDeps(filter, mockCompoundAuth(user), session, systemConfig, null);

        filter.filter(mutatingCtx);

        ArgumentCaptor<Response> abortResponseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(mutatingCtx).abortWith(abortResponseCaptor.capture());
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(),
                abortResponseCaptor.getValue().getStatus());
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testRoundTrip_MutatingCallWithoutBootstrap_Blocked() throws Exception {
        DataverseSession session = newSessionWithAuthenticatedUser();
        SystemConfig systemConfig = mockSystemConfig();
        AuthenticatedUser user = (AuthenticatedUser) session.getUser();

        // No bootstrap: don't call getOrCreateApiCsrfToken. Client supplies a fabricated value.
        ContainerRequestContext mutatingCtx = mockSessionCookieRequest("POST", "datasets/1/add");
        when(mutatingCtx.getHeaderString("Origin")).thenReturn(SITE_URL);
        when(mutatingCtx.getHeaderString(ApiConstants.CSRF_TOKEN_HEADER))
                .thenReturn("00000000-0000-0000-0000-000000000000");

        AuthFilter filter = new AuthFilter();
        injectAuthFilterDeps(filter, mockCompoundAuth(user), session, systemConfig, null);

        filter.filter(mutatingCtx);

        ArgumentCaptor<Response> abortResponseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(mutatingCtx).abortWith(abortResponseCaptor.capture());
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(),
                abortResponseCaptor.getValue().getStatus());
    }

    private DataverseSession newSessionWithAuthenticatedUser() {
        DataverseSession session = new DataverseSession();
        AuthenticatedUser user = new AuthenticatedUser();
        // setUser does FacesContext-dependent work; we bypass it by writing the user field directly
        // (the session-cookie auth chain only reads getUser()).
        try {
            Field userField = DataverseSession.class.getDeclaredField("user");
            userField.setAccessible(true);
            userField.set(session, user);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        return session;
    }

    private SystemConfig mockSystemConfig() {
        SystemConfig systemConfig = Mockito.mock(SystemConfig.class);
        when(systemConfig.getDataverseSiteUrl()).thenReturn(SITE_URL);
        return systemConfig;
    }

    private CompoundAuthMechanism mockCompoundAuth(AuthenticatedUser user) throws WrappedAuthErrorResponse {
        CompoundAuthMechanism compound = Mockito.mock(CompoundAuthMechanism.class);
        when(compound.findUserFromRequest(any(ContainerRequestContext.class))).thenAnswer(invocation -> {
            ContainerRequestContext crc = invocation.getArgument(0);
            crc.setProperty(
                    ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM,
                    ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
            return user;
        });
        return compound;
    }

    private ContainerRequestContext mockSessionCookieRequest(String method, String path) {
        ContainerRequestContext crc = Mockito.mock(ContainerRequestContext.class);
        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        when(crc.getMethod()).thenReturn(method);
        when(crc.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn(path);
        when(crc.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM))
                .thenReturn(ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
        return crc;
    }

    private ResourceInfo mockBootstrapResourceInfo() throws NoSuchMethodException {
        ResourceInfo resourceInfo = Mockito.mock(ResourceInfo.class);
        Method method = Users.class.getMethod("getSessionCsrfToken", ContainerRequestContext.class);
        when(resourceInfo.getResourceMethod()).thenReturn(method);
        return resourceInfo;
    }

    private void injectAuthFilterDeps(AuthFilter filter, CompoundAuthMechanism compound,
                                      DataverseSession session, SystemConfig systemConfig,
                                      ResourceInfo resourceInfo) throws Exception {
        injectField(filter, "compoundAuthMechanism", compound);
        injectField(filter, "session", session);
        injectField(filter, "systemConfig", systemConfig);
        injectField(filter, "resourceInfo", resourceInfo);
    }

    private Users newUsersWithSession(DataverseSession session) throws Exception {
        Users users = new Users();
        injectField(users, "session", session);
        return users;
    }

    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
