package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * @author Guillermo Portas
 * Authentication mechanism that attempts to authenticate a user from a Workflow Key provided in an API request.
 */
public class WorkflowKeyAuthMechanism implements AuthMechanism {

    public static final String DATAVERSE_WORKFLOW_KEY_REQUEST_HEADER_NAME = "X-Dataverse-invocationID";
    public static final String DATAVERSE_WORKFLOW_KEY_REQUEST_PARAM_NAME = "invocationID";
    public static final String RESPONSE_MESSAGE_BAD_WORKFLOW_KEY = "Bad workflow invocationID";

    @Inject
    protected AuthenticationServiceBean authSvc;

    @Override
    public User findUserFromRequest(ContainerRequestContext containerRequestContext) throws WrappedAuthErrorResponse {
        String workflowKey = getRequestWorkflowKey(containerRequestContext);
        if (workflowKey == null) {
            return null;
        }
        AuthenticatedUser authUser = authSvc.lookupUserForWorkflowInvocationID(workflowKey);
        if (authUser != null) {
            return authUser;
        }
        throw new WrappedUnauthorizedAuthErrorResponse(RESPONSE_MESSAGE_BAD_WORKFLOW_KEY);
    }

    private String getRequestWorkflowKey(ContainerRequestContext containerRequestContext) {
        String headerParamWorkflowKey = containerRequestContext.getHeaderString(DATAVERSE_WORKFLOW_KEY_REQUEST_HEADER_NAME);
        String queryParamWorkflowKey = containerRequestContext.getUriInfo().getQueryParameters().getFirst(DATAVERSE_WORKFLOW_KEY_REQUEST_PARAM_NAME);

        return headerParamWorkflowKey != null ? headerParamWorkflowKey : queryParamWorkflowKey;
    }
}
