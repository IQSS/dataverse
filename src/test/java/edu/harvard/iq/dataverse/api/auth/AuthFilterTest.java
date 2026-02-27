package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.api.ApiConstants;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@LocalJvmSettings
class AuthFilterTest {

    @Test
    void testFilter_HardeningDisabled_DoesNotAbort() throws Exception {
        AuthFilter sut = new AuthFilter();
        ContainerRequestContext requestContext = mockRequestContext("POST", "datasets/1");
        CompoundAuthMechanism compound = Mockito.mock(CompoundAuthMechanism.class);
        DataverseSession session = Mockito.mock(DataverseSession.class);
        SystemConfig systemConfig = Mockito.mock(SystemConfig.class);
        User user = new AuthenticatedUser();
        when(compound.findUserFromRequest(requestContext)).thenReturn(user);

        inject(sut, "compoundAuthMechanism", compound);
        inject(sut, "session", session);
        inject(sut, "systemConfig", systemConfig);

        sut.filter(requestContext);

        verify(requestContext, never()).abortWith(any(Response.class));
        verify(requestContext).setProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_USER, user);
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_HardeningEnabled_SessionCookieAccessGetAllowedForJsfDownloads() throws Exception {
        AuthFilter sut = new AuthFilter();
        ContainerRequestContext requestContext = mockRequestContext("GET", "access/datafile/123");
        CompoundAuthMechanism compound = Mockito.mock(CompoundAuthMechanism.class);
        DataverseSession session = Mockito.mock(DataverseSession.class);
        SystemConfig systemConfig = Mockito.mock(SystemConfig.class);

        when(compound.findUserFromRequest(requestContext)).thenAnswer(invocation -> {
            ContainerRequestContext crc = invocation.getArgument(0);
            crc.setProperty(
                    ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM,
                    ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
            return new AuthenticatedUser();
        });
        when(requestContext.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM))
                .thenReturn(ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);

        inject(sut, "compoundAuthMechanism", compound);
        inject(sut, "session", session);
        inject(sut, "systemConfig", systemConfig);

        sut.filter(requestContext);

        verify(requestContext, never()).abortWith(any(Response.class));
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_HardeningEnabled_SessionCookieAccessBatchDownloadPostRequiresSameOrigin() throws Exception {
        AuthFilter sut = new AuthFilter();
        ContainerRequestContext requestContext = mockRequestContext("POST", "access/datafiles");
        CompoundAuthMechanism compound = Mockito.mock(CompoundAuthMechanism.class);
        DataverseSession session = Mockito.mock(DataverseSession.class);
        SystemConfig systemConfig = Mockito.mock(SystemConfig.class);

        when(systemConfig.getDataverseSiteUrl()).thenReturn("https://demo.dataverse.org");
        when(compound.findUserFromRequest(requestContext)).thenAnswer(invocation -> {
            ContainerRequestContext crc = invocation.getArgument(0);
            crc.setProperty(
                    ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM,
                    ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
            return new AuthenticatedUser();
        });
        when(requestContext.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM))
                .thenReturn(ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
        when(requestContext.getHeaderString("Referer")).thenReturn("https://demo.dataverse.org/dataset.xhtml");

        inject(sut, "compoundAuthMechanism", compound);
        inject(sut, "session", session);
        inject(sut, "systemConfig", systemConfig);

        sut.filter(requestContext);

        verify(requestContext, never()).abortWith(any(Response.class));
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_HardeningEnabled_SessionCookieAccessBatchDownloadPostWithApiPrefixAndTrailingSlashAllowed() throws Exception {
        AuthFilter sut = new AuthFilter();
        ContainerRequestContext requestContext = mockRequestContext("POST", "api/access/datafiles/");
        CompoundAuthMechanism compound = Mockito.mock(CompoundAuthMechanism.class);
        DataverseSession session = Mockito.mock(DataverseSession.class);
        SystemConfig systemConfig = Mockito.mock(SystemConfig.class);

        when(systemConfig.getDataverseSiteUrl()).thenReturn("https://demo.dataverse.org");
        when(compound.findUserFromRequest(requestContext)).thenAnswer(invocation -> {
            ContainerRequestContext crc = invocation.getArgument(0);
            crc.setProperty(
                    ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM,
                    ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
            return new AuthenticatedUser();
        });
        when(requestContext.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM))
                .thenReturn(ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
        when(requestContext.getHeaderString("Referer")).thenReturn("https://demo.dataverse.org/dataset.xhtml");

        inject(sut, "compoundAuthMechanism", compound);
        inject(sut, "session", session);
        inject(sut, "systemConfig", systemConfig);

        sut.filter(requestContext);

        verify(requestContext, never()).abortWith(any(Response.class));
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_HardeningEnabled_SessionCookieAccessBatchDownloadPostCrossOriginBlocked() throws Exception {
        AuthFilter sut = new AuthFilter();
        ContainerRequestContext requestContext = mockRequestContext("POST", "access/datafiles");
        CompoundAuthMechanism compound = Mockito.mock(CompoundAuthMechanism.class);
        DataverseSession session = Mockito.mock(DataverseSession.class);
        SystemConfig systemConfig = Mockito.mock(SystemConfig.class);

        when(systemConfig.getDataverseSiteUrl()).thenReturn("https://demo.dataverse.org");
        when(compound.findUserFromRequest(requestContext)).thenAnswer(invocation -> {
            ContainerRequestContext crc = invocation.getArgument(0);
            crc.setProperty(
                    ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM,
                    ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
            return new AuthenticatedUser();
        });
        when(requestContext.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM))
                .thenReturn(ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
        when(requestContext.getHeaderString("Referer")).thenReturn("https://evil.example/malicious");

        inject(sut, "compoundAuthMechanism", compound);
        inject(sut, "session", session);
        inject(sut, "systemConfig", systemConfig);

        sut.filter(requestContext);

        ArgumentCaptor<Response> abortResponseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(abortResponseCaptor.capture());
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), abortResponseCaptor.getValue().getStatus());
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_HardeningEnabled_SessionCookieMutatingAccessEndpointBlocked() throws Exception {
        AuthFilter sut = new AuthFilter();
        ContainerRequestContext requestContext = mockRequestContext("PUT", "access/datafile/1/requestAccess");
        CompoundAuthMechanism compound = Mockito.mock(CompoundAuthMechanism.class);
        DataverseSession session = Mockito.mock(DataverseSession.class);
        SystemConfig systemConfig = Mockito.mock(SystemConfig.class);

        when(compound.findUserFromRequest(requestContext)).thenAnswer(invocation -> {
            ContainerRequestContext crc = invocation.getArgument(0);
            crc.setProperty(
                    ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM,
                    ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
            return new AuthenticatedUser();
        });
        when(requestContext.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM))
                .thenReturn(ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);

        inject(sut, "compoundAuthMechanism", compound);
        inject(sut, "session", session);
        inject(sut, "systemConfig", systemConfig);

        sut.filter(requestContext);

        ArgumentCaptor<Response> abortResponseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(abortResponseCaptor.capture());
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), abortResponseCaptor.getValue().getStatus());
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_HardeningEnabled_StateChangingCallNeedsCsrfAndOrigin() throws Exception {
        AuthFilter sut = new AuthFilter();
        ContainerRequestContext requestContext = mockRequestContext("POST", "datasets/1/add");
        CompoundAuthMechanism compound = Mockito.mock(CompoundAuthMechanism.class);
        DataverseSession session = Mockito.mock(DataverseSession.class);
        SystemConfig systemConfig = Mockito.mock(SystemConfig.class);

        when(systemConfig.getDataverseSiteUrl()).thenReturn("https://demo.dataverse.org");
        when(compound.findUserFromRequest(requestContext)).thenAnswer(invocation -> {
            ContainerRequestContext crc = invocation.getArgument(0);
            crc.setProperty(
                    ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM,
                    ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
            return new AuthenticatedUser();
        });
        when(requestContext.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM))
                .thenReturn(ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
        when(requestContext.getHeaderString("Origin")).thenReturn("https://demo.dataverse.org");
        when(requestContext.getHeaderString("Referer")).thenReturn("https://demo.dataverse.org/dataset.xhtml");
        when(requestContext.getHeaderString(ApiConstants.CSRF_TOKEN_HEADER)).thenReturn("valid-token");
        when(session.matchesApiCsrfToken("valid-token")).thenReturn(true);

        inject(sut, "compoundAuthMechanism", compound);
        inject(sut, "session", session);
        inject(sut, "systemConfig", systemConfig);

        sut.filter(requestContext);

        verify(requestContext, never()).abortWith(any(Response.class));
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_HardeningEnabled_MutatingGetNeedsCsrfAndOrigin() throws Exception {
        AuthFilter sut = new AuthFilter();
        ContainerRequestContext requestContext = mockRequestContext("GET", "datasets/1/uploadurls");
        CompoundAuthMechanism compound = Mockito.mock(CompoundAuthMechanism.class);
        DataverseSession session = Mockito.mock(DataverseSession.class);
        SystemConfig systemConfig = Mockito.mock(SystemConfig.class);

        when(systemConfig.getDataverseSiteUrl()).thenReturn("https://demo.dataverse.org");
        when(compound.findUserFromRequest(requestContext)).thenAnswer(invocation -> {
            ContainerRequestContext crc = invocation.getArgument(0);
            crc.setProperty(
                    ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM,
                    ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
            return new AuthenticatedUser();
        });
        when(requestContext.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM))
                .thenReturn(ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
        when(requestContext.getHeaderString("Origin")).thenReturn("https://demo.dataverse.org");
        when(requestContext.getHeaderString(ApiConstants.CSRF_TOKEN_HEADER)).thenReturn("valid-token");
        when(session.matchesApiCsrfToken("valid-token")).thenReturn(true);

        inject(sut, "compoundAuthMechanism", compound);
        inject(sut, "session", session);
        inject(sut, "systemConfig", systemConfig);

        sut.filter(requestContext);

        verify(requestContext, never()).abortWith(any(Response.class));
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_HardeningEnabled_CleanStorageMutatingGetNeedsCsrfAndOrigin() throws Exception {
        AuthFilter sut = new AuthFilter();
        ContainerRequestContext requestContext = mockRequestContext("GET", "datasets/1/cleanStorage");
        CompoundAuthMechanism compound = Mockito.mock(CompoundAuthMechanism.class);
        DataverseSession session = Mockito.mock(DataverseSession.class);
        SystemConfig systemConfig = Mockito.mock(SystemConfig.class);

        when(systemConfig.getDataverseSiteUrl()).thenReturn("https://demo.dataverse.org");
        when(compound.findUserFromRequest(requestContext)).thenAnswer(invocation -> {
            ContainerRequestContext crc = invocation.getArgument(0);
            crc.setProperty(
                    ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM,
                    ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
            return new AuthenticatedUser();
        });
        when(requestContext.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM))
                .thenReturn(ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
        when(requestContext.getHeaderString("Origin")).thenReturn("https://demo.dataverse.org");
        when(requestContext.getHeaderString(ApiConstants.CSRF_TOKEN_HEADER)).thenReturn("valid-token");
        when(session.matchesApiCsrfToken("valid-token")).thenReturn(true);

        inject(sut, "compoundAuthMechanism", compound);
        inject(sut, "session", session);
        inject(sut, "systemConfig", systemConfig);

        sut.filter(requestContext);

        verify(requestContext, never()).abortWith(any(Response.class));
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_HardeningEnabled_NonSessionAuthNotBlockedForAccessPath() throws Exception {
        AuthFilter sut = new AuthFilter();
        ContainerRequestContext requestContext = mockRequestContext("GET", "access/datafile/123");
        CompoundAuthMechanism compound = Mockito.mock(CompoundAuthMechanism.class);
        DataverseSession session = Mockito.mock(DataverseSession.class);
        SystemConfig systemConfig = Mockito.mock(SystemConfig.class);

        when(compound.findUserFromRequest(requestContext)).thenAnswer(invocation -> {
            ContainerRequestContext crc = invocation.getArgument(0);
            crc.setProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM, ApiConstants.AUTH_MECHANISM_API_KEY);
            return new AuthenticatedUser();
        });
        when(requestContext.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM))
                .thenReturn(ApiConstants.AUTH_MECHANISM_API_KEY);

        inject(sut, "compoundAuthMechanism", compound);
        inject(sut, "session", session);
        inject(sut, "systemConfig", systemConfig);

        sut.filter(requestContext);

        verify(requestContext, never()).abortWith(any(Response.class));
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_HardeningEnabled_StateChangingCallWithNoOriginOrRefererBlocked() throws Exception {
        AuthFilter sut = new AuthFilter();
        ContainerRequestContext requestContext = mockRequestContext("POST", "datasets/1/add");
        CompoundAuthMechanism compound = Mockito.mock(CompoundAuthMechanism.class);
        DataverseSession session = Mockito.mock(DataverseSession.class);
        SystemConfig systemConfig = Mockito.mock(SystemConfig.class);

        when(systemConfig.getDataverseSiteUrl()).thenReturn("https://demo.dataverse.org");
        when(compound.findUserFromRequest(requestContext)).thenAnswer(invocation -> {
            ContainerRequestContext crc = invocation.getArgument(0);
            crc.setProperty(
                    ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM,
                    ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
            return new AuthenticatedUser();
        });
        when(requestContext.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM))
                .thenReturn(ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
        // No Origin or Referer headers

        inject(sut, "compoundAuthMechanism", compound);
        inject(sut, "session", session);
        inject(sut, "systemConfig", systemConfig);

        sut.filter(requestContext);

        ArgumentCaptor<Response> abortResponseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(abortResponseCaptor.capture());
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), abortResponseCaptor.getValue().getStatus());
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_HardeningEnabled_StateChangingCallWithMissingCsrfTokenBlocked() throws Exception {
        AuthFilter sut = new AuthFilter();
        ContainerRequestContext requestContext = mockRequestContext("DELETE", "datasets/1");
        CompoundAuthMechanism compound = Mockito.mock(CompoundAuthMechanism.class);
        DataverseSession session = Mockito.mock(DataverseSession.class);
        SystemConfig systemConfig = Mockito.mock(SystemConfig.class);

        when(systemConfig.getDataverseSiteUrl()).thenReturn("https://demo.dataverse.org");
        when(compound.findUserFromRequest(requestContext)).thenAnswer(invocation -> {
            ContainerRequestContext crc = invocation.getArgument(0);
            crc.setProperty(
                    ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM,
                    ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
            return new AuthenticatedUser();
        });
        when(requestContext.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM))
                .thenReturn(ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
        when(requestContext.getHeaderString("Origin")).thenReturn("https://demo.dataverse.org");
        // CSRF token header absent (returns null by default from mock)

        inject(sut, "compoundAuthMechanism", compound);
        inject(sut, "session", session);
        inject(sut, "systemConfig", systemConfig);

        sut.filter(requestContext);

        ArgumentCaptor<Response> abortResponseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(abortResponseCaptor.capture());
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), abortResponseCaptor.getValue().getStatus());
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth-hardening")
    void testFilter_HardeningEnabled_StateChangingCallWithWrongCsrfTokenBlocked() throws Exception {
        AuthFilter sut = new AuthFilter();
        ContainerRequestContext requestContext = mockRequestContext("PUT", "datasets/1/editMetadata");
        CompoundAuthMechanism compound = Mockito.mock(CompoundAuthMechanism.class);
        DataverseSession session = Mockito.mock(DataverseSession.class);
        SystemConfig systemConfig = Mockito.mock(SystemConfig.class);

        when(systemConfig.getDataverseSiteUrl()).thenReturn("https://demo.dataverse.org");
        when(compound.findUserFromRequest(requestContext)).thenAnswer(invocation -> {
            ContainerRequestContext crc = invocation.getArgument(0);
            crc.setProperty(
                    ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM,
                    ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
            return new AuthenticatedUser();
        });
        when(requestContext.getProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM))
                .thenReturn(ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
        when(requestContext.getHeaderString("Origin")).thenReturn("https://demo.dataverse.org");
        when(requestContext.getHeaderString(ApiConstants.CSRF_TOKEN_HEADER)).thenReturn("wrong-token");
        when(session.matchesApiCsrfToken("wrong-token")).thenReturn(false);

        inject(sut, "compoundAuthMechanism", compound);
        inject(sut, "session", session);
        inject(sut, "systemConfig", systemConfig);

        sut.filter(requestContext);

        ArgumentCaptor<Response> abortResponseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(abortResponseCaptor.capture());
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), abortResponseCaptor.getValue().getStatus());
    }

    private ContainerRequestContext mockRequestContext(String method, String path) {
        ContainerRequestContext requestContext = Mockito.mock(ContainerRequestContext.class);
        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        when(requestContext.getMethod()).thenReturn(method);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn(path);
        return requestContext;
    }

    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
