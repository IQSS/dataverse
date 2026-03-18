package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the GET /api/users/:csrf-token endpoint in {@link Users}.
 * <p>
 * These tests exercise the endpoint handler directly with mocked dependencies
 * to verify the success path (session-cookie auth) and rejection paths
 * (hardening disabled, non-session auth).
 */
@LocalJvmSettings
class UsersSessionCsrfTokenTest {

    @Test
    void testGetCsrfToken_HardeningDisabled_Returns400() throws Exception {
        Users sut = new Users();
        ContainerRequestContext crc = Mockito.mock(ContainerRequestContext.class);

        Response response = sut.getSessionCsrfToken(crc);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testGetCsrfToken_NonSessionCookieAuth_Returns403() throws Exception {
        Users sut = new Users();
        ContainerRequestContext crc = Mockito.mock(ContainerRequestContext.class);
        when(crc.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM))
                .thenReturn(ApiConstants.AUTH_MECHANISM_API_KEY);

        Response response = sut.getSessionCsrfToken(crc);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testGetCsrfToken_SessionCookieAuth_ReturnsToken() throws Exception {
        Users sut = new Users();
        DataverseSession session = Mockito.mock(DataverseSession.class);
        ContainerRequestContext crc = Mockito.mock(ContainerRequestContext.class);

        when(crc.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM))
                .thenReturn(ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
        AuthenticatedUser authenticatedUser = new AuthenticatedUser();
        when(crc.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_USER))
                .thenReturn(authenticatedUser);
        when(session.getOrCreateApiCsrfToken()).thenReturn("test-csrf-token-value");

        inject(sut, Users.class, "session", session);

        Response response = sut.getSessionCsrfToken(crc);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        String body = response.getEntity().toString();
        assertTrue(body.contains("test-csrf-token-value"));
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testGetCsrfToken_SessionCookieAuthUnauthenticatedUser_ReturnsUnauthorized() throws Exception {
        Users sut = new Users();
        DataverseSession session = Mockito.mock(DataverseSession.class);
        ContainerRequestContext crc = Mockito.mock(ContainerRequestContext.class);

        when(crc.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM))
                .thenReturn(ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
        // Simulate a guest/unauthenticated user set by AuthFilter
        when(crc.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_USER))
                .thenReturn(edu.harvard.iq.dataverse.authorization.users.GuestUser.get());

        inject(sut, Users.class, "session", session);

        Response response = sut.getSessionCsrfToken(crc);

        // getRequestAuthenticatedUserOrDie throws WrappedResponse for non-authenticated users
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    private void inject(Object target, Class<?> clazz, String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
