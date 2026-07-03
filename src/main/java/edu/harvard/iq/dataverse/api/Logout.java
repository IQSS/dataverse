package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataverseHeaderFragment;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.settings.FeatureFlags;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("logout")
public class Logout extends AbstractApiBean {

    @Inject
    DataverseSession session;

    /**
     * The only current API authentication mechanism subject to Log Out is the session cookie auth, and this mechanism is only available when the corresponding feature flag is enabled:
     *
     * @see FeatureFlags#API_SESSION_AUTH
     * <p>
     * This endpoint replicates the logic from the JSF Log Out feature:
     * @see DataverseHeaderFragment#logout()
     * <p>
     * {@code @AuthRequired} routes this endpoint through {@code AuthFilter} so the
     * session-cookie CSRF hardening (when enabled) covers it: logout is state-changing
     * and session-cookie-authenticated, and without the annotation it would be the one
     * such endpoint the hardening never sees. Two behavior changes follow from running
     * the filter, both intended: with hardening enabled a session-cookie logout must
     * now carry the {@code X-Dataverse-CSRF-Token} header like any other state-changing
     * call, and an <em>invalid</em> non-session credential (e.g. a bad API key) is now
     * rejected by the filter with 401 rather than reaching the handler. A valid
     * non-session credential still falls through to the handler's "no session cookie"
     * response as before, since the handler reads the JSF session directly.
     * <p>
     * TODO: This endpoint must change when a final API authentication mechanism is established for use cases / applications subject to Log Out
     */
    @POST
    @AuthRequired
    @Path("/")
    public Response logout() {
        if (!FeatureFlags.API_SESSION_AUTH.enabled()) {
            return error(Response.Status.INTERNAL_SERVER_ERROR, "This endpoint is only available when session authentication feature flag is enabled");
        }
        if (!session.getUser().isAuthenticated()) {
            return error(Response.Status.BAD_REQUEST, "No valid session cookie was sent in the request");
        }
        session.setUser(null);
        session.setStatusDismissed(false);
        return ok("User logged out");
    }
}
