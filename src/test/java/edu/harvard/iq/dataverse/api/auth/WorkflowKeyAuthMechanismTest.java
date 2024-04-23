package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.api.auth.doubles.WorkflowKeyContainerRequestTestFake;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.ws.rs.container.ContainerRequestContext;

import static edu.harvard.iq.dataverse.api.auth.WorkflowKeyAuthMechanism.RESPONSE_MESSAGE_BAD_WORKFLOW_KEY;
import static org.junit.jupiter.api.Assertions.*;

public class WorkflowKeyAuthMechanismTest {

    private static final String TEST_WORKFLOW_KEY = "test-workflow-key";

    private WorkflowKeyAuthMechanism sut;

    @BeforeEach
    public void setUp() {
        sut = new WorkflowKeyAuthMechanism();
    }

    @Test
    public void testFindUserFromRequest_WorkflowKeyNotProvided() throws WrappedAuthErrorResponse {
        sut.authSvc = Mockito.mock(AuthenticationServiceBean.class);

        ContainerRequestContext testContainerRequest = new WorkflowKeyContainerRequestTestFake(null);
        User actual = sut.findUserFromRequest(testContainerRequest);

        assertNull(actual);
    }

    @Test
    public void testFindUserFromRequest_WorkflowKeyProvided_UserAuthenticated() throws WrappedAuthErrorResponse {
        AuthenticationServiceBean authenticationServiceBeanStub = Mockito.mock(AuthenticationServiceBean.class);
        AuthenticatedUser testAuthenticatedUser = new AuthenticatedUser();
        Mockito.when(authenticationServiceBeanStub.lookupUserForWorkflowInvocationID(TEST_WORKFLOW_KEY)).thenReturn(testAuthenticatedUser);
        sut.authSvc = authenticationServiceBeanStub;

        ContainerRequestContext testContainerRequest = new WorkflowKeyContainerRequestTestFake(TEST_WORKFLOW_KEY);
        User actual = sut.findUserFromRequest(testContainerRequest);

        assertEquals(testAuthenticatedUser, actual);
    }

    @Test
    public void testFindUserFromRequest_WorkflowKeyProvided_UserNotAuthenticated() {
        AuthenticationServiceBean authenticationServiceBeanStub = Mockito.mock(AuthenticationServiceBean.class);
        Mockito.when(authenticationServiceBeanStub.lookupUserForWorkflowInvocationID(TEST_WORKFLOW_KEY)).thenReturn(null);
        sut.authSvc = authenticationServiceBeanStub;

        ContainerRequestContext testContainerRequest = new WorkflowKeyContainerRequestTestFake(TEST_WORKFLOW_KEY);
        WrappedAuthErrorResponse wrappedAuthErrorResponse = assertThrows(WrappedAuthErrorResponse.class, () -> sut.findUserFromRequest(testContainerRequest));

        assertEquals(RESPONSE_MESSAGE_BAD_WORKFLOW_KEY, wrappedAuthErrorResponse.getMessage());
    }
}
