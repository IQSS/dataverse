package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.api.auth.doubles.ContainerRequestTestFake;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.ws.rs.container.ContainerRequestContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

public class CompoundAuthMechanismTest {

    @Test
    public void testFindUserFromRequest_CanNotAuthenticateUserWithAnyMechanism() throws WrappedAuthErrorResponse {
        AuthMechanism authMechanismStub1 = Mockito.mock(AuthMechanism.class);
        Mockito.when(authMechanismStub1.findUserFromRequest(any(ContainerRequestContext.class))).thenReturn(null);

        AuthMechanism authMechanismStub2 = Mockito.mock(AuthMechanism.class);
        Mockito.when(authMechanismStub2.findUserFromRequest(any(ContainerRequestContext.class))).thenReturn(null);

        CompoundAuthMechanism sut = new CompoundAuthMechanism(authMechanismStub1, authMechanismStub2);

        User actual = sut.findUserFromRequest(new ContainerRequestTestFake());

        assertThat(actual, equalTo(GuestUser.get()));
    }

    @Test
    public void testFindUserFromRequest_UserAuthenticated() throws WrappedAuthErrorResponse {
        AuthMechanism authMechanismStub1 = Mockito.mock(AuthMechanism.class);
        AuthenticatedUser testAuthenticatedUser = new AuthenticatedUser();
        Mockito.when(authMechanismStub1.findUserFromRequest(any(ContainerRequestContext.class))).thenReturn(testAuthenticatedUser);

        AuthMechanism authMechanismStub2 = Mockito.mock(AuthMechanism.class);
        Mockito.when(authMechanismStub2.findUserFromRequest(any(ContainerRequestContext.class))).thenReturn(null);

        CompoundAuthMechanism sut = new CompoundAuthMechanism(authMechanismStub1, authMechanismStub2);

        User actual = sut.findUserFromRequest(new ContainerRequestTestFake());

        assertEquals(actual, testAuthenticatedUser);
    }
}
