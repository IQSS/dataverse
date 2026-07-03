package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.api.ApiConstants;
import edu.harvard.iq.dataverse.api.Users;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.settings.FeatureFlags;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.UrlOriginUtil;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * @author Guillermo Portas
 * Dedicated filter to authenticate the user requesting an API endpoint that requires user authentication.
 *
 * <p>Note on scope: this filter is name-bound to {@link AuthRequired}, so the
 * session-cookie CSRF hardening below protects exactly the {@code @AuthRequired}
 * endpoints. Endpoints that read the session directly without the annotation are
 * NOT covered — keep that set empty for state-changing endpoints, or annotate
 * them (as {@code /api/logout} is).
 */
@AuthRequired
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter {

    private static final Logger logger = Logger.getLogger(AuthFilter.class.getCanonicalName());

    @Inject
    private CompoundAuthMechanism compoundAuthMechanism;

    @Inject
    private DataverseSession session;

    @Inject
    private SystemConfig systemConfig;

    @Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        try {
            User user = compoundAuthMechanism.findUserFromRequest(containerRequestContext);
            containerRequestContext.setProperty(ApiConstants.CONTAINER_REQUEST_CONTEXT_USER, user);
            applySessionAuthHardening(containerRequestContext, user);
        } catch (WrappedAuthErrorResponse e) {
            containerRequestContext.abortWith(e.getResponse());
        }
    }

    private void applySessionAuthHardening(ContainerRequestContext containerRequestContext, User user)
            throws WrappedAuthErrorResponse {
        if (!FeatureFlags.API_SESSION_AUTH_HARDENING.enabled()) {
            return;
        }
        if (!CompoundAuthMechanism.isSessionCookieRequest(containerRequestContext)) {
            return;
        }
        // Only fully-authenticated session users are hardened. isAuthenticated() is false
        // for both GuestUser and PrivateUrlUser, and both are deliberately exempt:
        //   - a guest carries no privileges worth forging;
        //   - a private-URL preview session is read-only (no state to forge) and its
        //     responses are not cross-origin readable, so CSRF hardening adds no
        //     protection — while the JSF preview flow that mints these sessions has no
        //     way to obtain or send a token, so hardening it would only break anonymous
        //     preview downloads.
        if (user == null || !user.isAuthenticated()) {
            return;
        }

        // Browser-sent Origin/Referer headers must match the site origin when present.
        // When BOTH are absent we fall through to the CSRF token instead of failing
        // closed: browsers omit Origin on same-origin GETs and suppress Referer under
        // Referrer-Policy: no-referrer, so a legitimate same-origin request can carry
        // neither — while the cross-site deliveries that matter cannot reach the token
        // check anyway (cross-site fetch/XHR/form POSTs always carry Origin, and a
        // cross-site attacker cannot read or set the X-Dataverse-CSRF-Token header).
        if (!isOriginOrRefererAllowed(containerRequestContext)) {
            throw new WrappedForbiddenAuthErrorResponse(
                    "Request origin validation failed for session-cookie authentication.");
        }

        // Allow the CSRF-token-issuing endpoint to be called without an existing CSRF header.
        // This endpoint is used to bootstrap the CSRF token for the current authenticated session.
        if (isCsrfTokenBootstrapEndpoint()) {
            return;
        }

        if (!isCsrfTokenValid(containerRequestContext)) {
            throw new WrappedForbiddenAuthErrorResponse(
                    "Missing or invalid CSRF token for session-cookie authentication.");
        }
    }

    /**
     * Returns {@code true} if JAX-RS resolved the request to {@link Users#getSessionCsrfToken}.
     * For this single endpoint we rely on the authenticated session cookie plus same-origin
     * checks and do not require an existing CSRF header; that's how clients bootstrap the token.
     * Matching on the resolved resource method (rather than a path string) keeps the exemption
     * scoped to exactly the intended endpoint.
     */
    private boolean isCsrfTokenBootstrapEndpoint() {
        if (resourceInfo == null) {
            return false;
        }
        Method method = resourceInfo.getResourceMethod();
        return method != null
                && Users.class.equals(method.getDeclaringClass())
                && "getSessionCsrfToken".equals(method.getName());
    }

    /**
     * Validates any Origin/Referer headers that are present against the site origin.
     * Absence of both headers is allowed — the CSRF token check that follows is then
     * the deciding credential (see the caller for the reasoning).
     */
    private boolean isOriginOrRefererAllowed(ContainerRequestContext containerRequestContext) {
        String originHeader = containerRequestContext.getHeaderString(ApiConstants.ORIGIN_HEADER);
        String refererHeader = containerRequestContext.getHeaderString(ApiConstants.REFERER_HEADER);
        boolean hasOrigin = originHeader != null && !originHeader.isBlank();
        boolean hasReferer = refererHeader != null && !refererHeader.isBlank();

        if (!hasOrigin && !hasReferer) {
            return true;
        }
        String allowedOrigin = UrlOriginUtil.toOrigin(systemConfig.getDataverseSiteUrl());
        if (allowedOrigin == null) {
            logger.warning("Unable to validate Origin/Referer for session hardening: dataverse site URL is invalid.");
            return false;
        }
        if (hasOrigin && !allowedOrigin.equals(UrlOriginUtil.toOrigin(originHeader))) {
            return false;
        }
        if (hasReferer && !allowedOrigin.equals(UrlOriginUtil.toOrigin(refererHeader))) {
            return false;
        }
        return true;
    }

    private boolean isCsrfTokenValid(ContainerRequestContext containerRequestContext) {
        String requestToken = containerRequestContext.getHeaderString(ApiConstants.CSRF_TOKEN_HEADER);
        return requestToken != null && !requestToken.isBlank() && session.matchesApiCsrfToken(requestToken);
    }
}
