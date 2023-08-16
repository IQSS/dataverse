package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import java.util.logging.Logger;

/**
 * @author Guillermo Portas
 * Authentication mechanism that attempts to authenticate a user from an API Key provided in an API request.
 */
public class ApiKeyAuthMechanism implements AuthMechanism {

    public static final String DATAVERSE_API_KEY_REQUEST_HEADER_NAME = "X-Dataverse-key";
    public static final String DATAVERSE_API_KEY_REQUEST_PARAM_NAME = "key";
    public static final String RESPONSE_MESSAGE_BAD_API_KEY = "Bad API key";
    public static final String ACCESS_DATAFILE_PATH_PREFIX = "/access/datafile/";

    @Inject
    protected PrivateUrlServiceBean privateUrlSvc;

    @Inject
    protected AuthenticationServiceBean authSvc;

    @Inject
    protected UserServiceBean userSvc;

    private static final Logger logger = Logger.getLogger(ApiKeyAuthMechanism.class.getName());

    @Override
    public User findUserFromRequest(ContainerRequestContext containerRequestContext) throws WrappedAuthErrorResponse {
        String apiKey = getRequestApiKey(containerRequestContext);
        if (apiKey == null) {
            return null;
        }
        PrivateUrlUser privateUrlUser = privateUrlSvc.getPrivateUrlUserFromToken(apiKey);
        if (privateUrlUser != null) {
            checkAnonymizedAccessToRequestPath(containerRequestContext.getUriInfo().getPath(), privateUrlUser);
            return privateUrlUser;
        }
        AuthenticatedUser authUser = authSvc.lookupUser(apiKey);
        if (authUser != null) {
            authUser = userSvc.updateLastApiUseTime(authUser);
            return authUser;
        }
        throw new WrappedAuthErrorResponse(RESPONSE_MESSAGE_BAD_API_KEY);
    }

    private String getRequestApiKey(ContainerRequestContext containerRequestContext) {
        String headerParamApiKey = containerRequestContext.getHeaderString(DATAVERSE_API_KEY_REQUEST_HEADER_NAME);
        String queryParamApiKey = containerRequestContext.getUriInfo().getQueryParameters().getFirst(DATAVERSE_API_KEY_REQUEST_PARAM_NAME);

        return headerParamApiKey != null ? headerParamApiKey : queryParamApiKey;
    }

    private void checkAnonymizedAccessToRequestPath(String requestPath, PrivateUrlUser privateUrlUser) throws WrappedAuthErrorResponse {
        if (!privateUrlUser.hasAnonymizedAccess()) {
            return;
        }
        // For privateUrlUsers restricted to anonymized access, all api calls are off-limits except for those used in the UI
        // to download the file or image thumbs
        if (!(requestPath.startsWith(ACCESS_DATAFILE_PATH_PREFIX) && !requestPath.substring(ACCESS_DATAFILE_PATH_PREFIX.length()).contains("/"))) {
            logger.info("Anonymized access request for " + requestPath);
            throw new WrappedAuthErrorResponse(RESPONSE_MESSAGE_BAD_API_KEY);
        }
    }
}
