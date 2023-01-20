package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.container.ContainerRequestContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ApiKeyAuthMechanismTest {

    private final PrivateUrlUser testPrivateUrlUser = new PrivateUrlUser(1L);

    private static final String TEST_API_KEY = "test-api-key";

    @Test
    public void testFindUserFromRequestPrivateUrlUserNotAnonymized() throws WrappedAuthErrorResponse {
        ApiKeyAuthMechanism sut = new ApiKeyAuthMechanism();

        sut.authSvc = Mockito.mock(AuthenticationServiceBean.class);
        sut.userSvc = Mockito.mock(UserServiceBean.class);

        PrivateUrlServiceBean privateUrlServiceStub = Mockito.mock(PrivateUrlServiceBean.class);
        Mockito.when(privateUrlServiceStub.getPrivateUrlUserFromToken(TEST_API_KEY)).thenReturn(testPrivateUrlUser);
        sut.privateUrlSvc = privateUrlServiceStub;

        ContainerRequestContext testContainerRequest = new ContainerRequestFake(TEST_API_KEY);
        User actual = sut.findUserFromRequest(testContainerRequest);

        assertEquals(testPrivateUrlUser, actual);
    }
}
