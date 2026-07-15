package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.api.ApiConstants;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.ws.rs.container.ContainerRequestContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@LocalJvmSettings
public class CompoundAuthMechanismTest {

    @Test
    public void testFindUserFromRequest_CanNotAuthenticateUserWithAnyMechanism() throws WrappedAuthErrorResponse {
        AuthMechanism authMechanismStub1 = Mockito.mock(AuthMechanism.class);
        Mockito.when(authMechanismStub1.findUserFromRequest(any(ContainerRequestContext.class))).thenReturn(null);

        AuthMechanism authMechanismStub2 = Mockito.mock(AuthMechanism.class);
        Mockito.when(authMechanismStub2.findUserFromRequest(any(ContainerRequestContext.class))).thenReturn(null);

        CompoundAuthMechanism sut = new CompoundAuthMechanism(authMechanismStub1, authMechanismStub2);
        ContainerRequestContext containerRequestContext = Mockito.mock(ContainerRequestContext.class);

        User actual = sut.findUserFromRequest(containerRequestContext);

        assertThat(actual, equalTo(GuestUser.get()));
        // The auth-mechanism tag is only ever set for session-cookie auth;
        // for everything else (including the guest fallback) it stays absent.
        verify(containerRequestContext, never()).setProperty(
                Mockito.eq(ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM), Mockito.any());
    }

    @Test
    public void testFindUserFromRequest_UserAuthenticated() throws WrappedAuthErrorResponse {
        AuthMechanism authMechanismStub1 = Mockito.mock(AuthMechanism.class);
        AuthenticatedUser testAuthenticatedUser = new AuthenticatedUser();
        Mockito.when(authMechanismStub1.findUserFromRequest(any(ContainerRequestContext.class)))
                .thenReturn(testAuthenticatedUser);

        AuthMechanism authMechanismStub2 = Mockito.mock(AuthMechanism.class);
        Mockito.when(authMechanismStub2.findUserFromRequest(any(ContainerRequestContext.class))).thenReturn(null);

        CompoundAuthMechanism sut = new CompoundAuthMechanism(authMechanismStub1, authMechanismStub2);
        ContainerRequestContext containerRequestContext = Mockito.mock(ContainerRequestContext.class);

        User actual = sut.findUserFromRequest(containerRequestContext);

        assertEquals(actual, testAuthenticatedUser);
    }

    @Test
    @JvmSetting(key = JvmSettings.FEATURE_FLAG, value = "true", varArgs = "api-session-auth")
    void testFindUserFromRequest_TagsSessionCookieMechanism() throws WrappedAuthErrorResponse {
        SessionCookieAuthMechanism sessionCookieAuthMechanism = new SessionCookieAuthMechanism();
        DataverseSession dataverseSessionStub = Mockito.mock(DataverseSession.class);
        AuthenticatedUser testAuthenticatedUser = new AuthenticatedUser();
        Mockito.when(dataverseSessionStub.getUser()).thenReturn(testAuthenticatedUser);
        sessionCookieAuthMechanism.session = dataverseSessionStub;

        CompoundAuthMechanism sut = new CompoundAuthMechanism(sessionCookieAuthMechanism);
        ContainerRequestContext containerRequestContext = Mockito.mock(ContainerRequestContext.class);

        User actual = sut.findUserFromRequest(containerRequestContext);

        assertEquals(actual, testAuthenticatedUser);
        verify(containerRequestContext).setProperty(
                ApiConstants.CONTAINER_REQUEST_CONTEXT_AUTH_MECHANISM,
                ApiConstants.AUTH_MECHANISM_SESSION_COOKIE);
    }
}
