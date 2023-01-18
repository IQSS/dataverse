package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import java.util.logging.Logger;

public class ApiKeyAuthMechanism implements AuthMechanism {

    private static final String DATAVERSE_API_KEY_REQUEST_HEADER_NAME = "X-Dataverse-key";
    private static final String DATAVERSE_API_KEY_REQUEST_PARAM_NAME = "key";

    @Inject
    protected PrivateUrlServiceBean privateUrlSvc;

    @Inject
    protected AuthenticationServiceBean authSvc;

    @Inject
    protected UserServiceBean userSvc;

    private static final Logger logger = Logger.getLogger(ApiKeyAuthMechanism.class.getName());

    @Override
    public AuthenticatedUser getAuthenticatedUserFromRequest(ContainerRequestContext containerRequestContext) throws WrappedAuthErrorResponse {
        String apiKey = getRequestApiKey(containerRequestContext);
        if (apiKey == null) {
            return null;
        }
        /*PrivateUrlUser privateUrlUser = privateUrlSvc.getPrivateUrlUserFromToken(apiKey);
        if (privateUrlUser != null) {
            if (privateUrlUser.hasAnonymizedAccess()) {
                String pathInfo = containerRequestContext.getUriInfo().getPath();
                String prefix = "/access/datafile/";
                if (!(pathInfo.startsWith(prefix) && !pathInfo.substring(prefix.length()).contains("/"))) {
                    logger.info("Anonymized access request for " + pathInfo);
                    throw new AuthException("API Access not allowed with this Key");
                }
            }
            return privateUrlUser;
        }*/
        AuthenticatedUser authUser = authSvc.lookupUser(apiKey);
        if (authUser != null) {
            authUser = userSvc.updateLastApiUseTime(authUser);
            return authUser;
        }
        throw new WrappedAuthErrorResponse(getBadApiKeyResponseMessage(apiKey));
    }

    private String getRequestApiKey(ContainerRequestContext containerRequestContext) {
        String headerParamApiKey = containerRequestContext.getHeaderString(DATAVERSE_API_KEY_REQUEST_HEADER_NAME);
        String queryParamApiKey = containerRequestContext.getUriInfo().getQueryParameters().getFirst(DATAVERSE_API_KEY_REQUEST_PARAM_NAME);

        return headerParamApiKey != null ? headerParamApiKey : queryParamApiKey;
    }

    protected String getBadApiKeyResponseMessage(String apiKey) {
        return (apiKey != null) ? "Bad api key" : "Please provide a key query parameter (?" + DATAVERSE_API_KEY_REQUEST_PARAM_NAME + "=XXX) or via the HTTP header " + DATAVERSE_API_KEY_REQUEST_HEADER_NAME;
    }
}
