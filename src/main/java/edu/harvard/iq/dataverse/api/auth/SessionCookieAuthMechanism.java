package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.api.ApiConstants;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.settings.FeatureFlags;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.UrlOriginUtil;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;

public class SessionCookieAuthMechanism implements AuthMechanism {
    @Inject
    DataverseSession session;

    public static final String ACCESS_PATH_PREFIX = "access/";

    public static final String RESPONSE_MESSAGE_ORIGIN_VALIDATION_FAILED = "Request origin validation failed for session-cookie authentication.";

    @Override
    public User findUserFromRequest(ContainerRequestContext containerRequestContext) throws WrappedAuthErrorResponse {
        if (FeatureFlags.API_SESSION_AUTH.enabled() || isAccessApi(containerRequestContext)) {
            return getHardenedSessionUser(containerRequestContext);
        }
        return null;
    }

    private User getHardenedSessionUser(ContainerRequestContext containerRequestContext) throws WrappedAuthErrorResponse {
        User user = session.getUser();
        if (!FeatureFlags.API_SESSION_AUTH_HARDENING.enabled() || user == null || !user.isAuthenticated()) {
            return user;
        }
        String originHeader = containerRequestContext.getHeaderString(ApiConstants.ORIGIN_HEADER);
        String refererHeader = containerRequestContext.getHeaderString(ApiConstants.REFERER_HEADER);
        // Neither header proves same origin, so decline the session instead of rejecting the
        // request: bookmarked and shared links carry neither and still resolve as guest.
        if (isBlank(originHeader) && isBlank(refererHeader)) {
            return null;
        }
        String siteOrigin = UrlOriginUtil.toOrigin(SystemConfig.getDataverseSiteUrlStatic());
        if (siteOrigin == null || !isSiteOrigin(originHeader, siteOrigin) || !isSiteOrigin(refererHeader, siteOrigin)) {
            throw new WrappedForbiddenAuthErrorResponse(RESPONSE_MESSAGE_ORIGIN_VALIDATION_FAILED);
        }
        return user;
    }

    private boolean isSiteOrigin(String header, String siteOrigin) {
        return isBlank(header) || siteOrigin.equals(UrlOriginUtil.toOrigin(header));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isAccessApi(ContainerRequestContext containerRequestContext) {
        String requestPath = containerRequestContext.getUriInfo() != null ? containerRequestContext.getUriInfo().getPath() : "";
        return (requestPath.startsWith(ACCESS_PATH_PREFIX));
    }
}
