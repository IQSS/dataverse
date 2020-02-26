package edu.harvard.iq.dataverse.api.filters;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.function.Consumer;

/**
 *  When one is using Dataverse REST Api we need to set user in session
 * so that user data will be accessible to all the services.
 *  This filter is only used when Api call is done and it tries to log
 * user into the session if following conditions are all met:
 * <ol>
 * <li>user is not already set in the session (i.e. user is not the guest user)</li>
 * <li>user token is passed in header <i>X-Dataverse-key</i> or in the <i>key</i> query parameter.</li>
 * </ol>
 *  If the user was logged in by the filter, after the service call is done, user's
 * session is invalidated.
 */
public class ApiAuthorizationFilter implements Filter {

    private DataverseSession session;
    private AuthenticationServiceBean authenticationService;
    private UserServiceBean userService;

    // -------------------- CONSTRUCTORS ---------------------

    @Inject
    public ApiAuthorizationFilter(DataverseSession session, AuthenticationServiceBean authenticationService, UserServiceBean userService) {
        this.session = session;
        this.authenticationService = authenticationService;
        this.userService = userService;
    }

    // -------------------- LOGIC ---------------------

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        FilterLogIn filterLogIn = logInUserByTokenIfNeeded(request);
        chain.doFilter(servletRequest, response);
        filterLogIn.logOut(request);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException { }

    @Override
    public void destroy() { }

    // -------------------- PRIVATE ---------------------

    private FilterLogIn logInUserByTokenIfNeeded(HttpServletRequest request) {
        if (GuestUser.get().equals(session.getUser())) {
            String token = getRequestApiKey(request);
            AuthenticatedUser user = authenticationService.lookupUser(token);
            if (user != null) {
                user = userService.updateLastApiUseTime(user);
                session.setUser(user);
                return FilterLogIn.TOKEN_LOG_IN;
            }
        }
        return FilterLogIn.NONE;
    }

    private String getRequestApiKey(HttpServletRequest request) {
        String headerParamApiKey = request.getHeader("X-Dataverse-key");
        String queryParamApiKey = request.getParameter("key");
        return headerParamApiKey != null ? headerParamApiKey : queryParamApiKey;
    }

    // -------------------- INNER CLASSES ---------------------

    private enum FilterLogIn {
        NONE(r -> { }),
        TOKEN_LOG_IN(r -> r.getSession().invalidate());

        private final Consumer<HttpServletRequest> logout;

        public void logOut(HttpServletRequest request) {
            logout.accept(request);
        }

        FilterLogIn(Consumer<HttpServletRequest> logout) {
            this.logout = logout;
        }
    }
}
