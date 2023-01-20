package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.ws.rs.container.ContainerRequestContext;

import static edu.harvard.iq.dataverse.api.auth.ApiKeyAuthMechanism.ACCESS_DATAFILE_PATH_PREFIX;
import static edu.harvard.iq.dataverse.api.auth.ApiKeyAuthMechanism.RESPONSE_MESSAGE_BAD_API_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ApiKeyAuthMechanismTest {

    private static final String TEST_API_KEY = "test-api-key";
    private static final String TEST_PATH = "/test/path/";

    private ApiKeyAuthMechanism sut;

    private final PrivateUrlUser testAnonymizedPrivateUrlUser = new PrivateUrlUser(1L, true);

    @BeforeEach
    public void setUp() {
        sut = new ApiKeyAuthMechanism();
        sut.authSvc = Mockito.mock(AuthenticationServiceBean.class);
        sut.userSvc = Mockito.mock(UserServiceBean.class);
    }

    @Test
    public void testFindUserFromRequest_NotAnonymizedPrivateUrlUser() throws WrappedAuthErrorResponse {
        PrivateUrlServiceBean privateUrlServiceStub = Mockito.mock(PrivateUrlServiceBean.class);
        PrivateUrlUser testPrivateUrlUser = new PrivateUrlUser(1L);
        Mockito.when(privateUrlServiceStub.getPrivateUrlUserFromToken(TEST_API_KEY)).thenReturn(testPrivateUrlUser);
        sut.privateUrlSvc = privateUrlServiceStub;

        ContainerRequestContext testContainerRequest = new ContainerRequestFake(TEST_API_KEY, TEST_PATH);
        User actual = sut.findUserFromRequest(testContainerRequest);

        assertEquals(testPrivateUrlUser, actual);
    }

    @Test
    public void testFindUserFromRequest_AnonymizedPrivateUrlUserAccessingDatafile() throws WrappedAuthErrorResponse {
        PrivateUrlServiceBean privateUrlServiceStub = Mockito.mock(PrivateUrlServiceBean.class);
        Mockito.when(privateUrlServiceStub.getPrivateUrlUserFromToken(TEST_API_KEY)).thenReturn(testAnonymizedPrivateUrlUser);
        sut.privateUrlSvc = privateUrlServiceStub;

        ContainerRequestContext testContainerRequest = new ContainerRequestFake(TEST_API_KEY, ACCESS_DATAFILE_PATH_PREFIX);
        User actual = sut.findUserFromRequest(testContainerRequest);

        assertEquals(testAnonymizedPrivateUrlUser, actual);
    }

    @Test
    public void testFindUserFromRequest_AnonymizedPrivateUrlUserNotAccessingDatafile() {
        PrivateUrlServiceBean privateUrlServiceStub = Mockito.mock(PrivateUrlServiceBean.class);
        Mockito.when(privateUrlServiceStub.getPrivateUrlUserFromToken(TEST_API_KEY)).thenReturn(testAnonymizedPrivateUrlUser);
        sut.privateUrlSvc = privateUrlServiceStub;

        ContainerRequestContext testContainerRequest = new ContainerRequestFake(TEST_API_KEY, TEST_PATH);
        WrappedAuthErrorResponse wrappedAuthErrorResponse = assertThrows(WrappedAuthErrorResponse.class, () -> sut.findUserFromRequest(testContainerRequest));
        assertEquals(RESPONSE_MESSAGE_BAD_API_KEY, wrappedAuthErrorResponse.getMessage());
    }
}
