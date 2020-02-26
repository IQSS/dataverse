package edu.harvard.iq.dataverse.api.filters;


import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApiAuthorizationFilterTest {
    private static final AuthenticatedUser AUTHENTICATED_USER = new AuthenticatedUser();
    private static final String TOKEN = "token";

    @Mock
    private DataverseSession dataverseSession;

    @Mock
    private AuthenticationServiceBean authenticationService;

    @Mock
    private UserServiceBean userService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession httpSession;

    private FilterChain filterChain = (servletRequest, servletResponse) -> { };

    private ApiAuthorizationFilter filter;

    @Before
    public void setUp() {
        filter = new ApiAuthorizationFilter(dataverseSession, authenticationService, userService);
        lenient().when(request.getSession()).thenReturn(httpSession);
    }

    @Test
    public void shouldDoNothingIfUserIsAlreadyLogged() throws IOException, ServletException {
        // given
        when(dataverseSession.getUser()).thenReturn(AUTHENTICATED_USER);

        // when
        filter.doFilter(request, null, filterChain);

        // then
        verify(authenticationService, never()).lookupUser(nullable(String.class));
        verify(userService, never()).updateLastApiUseTime(nullable(AuthenticatedUser.class));
        verify(dataverseSession, never()).setUser(nullable(AuthenticatedUser.class));
        verify(httpSession, never()).invalidate();
    }

    @Test
    public void shouldLogAndLogoutUserWhenKeyIsPassedAndUserIsNotAlreadyLoggedIn()
            throws IOException, ServletException {
        // given
        when(dataverseSession.getUser()).thenReturn(GuestUser.get());
        when(request.getParameter("key")).thenReturn(TOKEN);
        when(authenticationService.lookupUser(TOKEN)).thenReturn(AUTHENTICATED_USER);
        when(userService.updateLastApiUseTime(AUTHENTICATED_USER)).thenReturn(AUTHENTICATED_USER);

        // when
        filter.doFilter(request, null, filterChain);

        // then
        verify(authenticationService, times(1)).lookupUser(anyString());
        verify(userService, times(1)).updateLastApiUseTime(any(AuthenticatedUser.class));
        verify(dataverseSession, times(1)).setUser(any(AuthenticatedUser.class));
        verify(httpSession, times(1)).invalidate();
    }

    @Test
    public void shouldDoNothingWhenNoKeyPassedAndUserIsNotAlreadyLoggedIn()
            throws IOException, ServletException {
        // given
        when(dataverseSession.getUser()).thenReturn(GuestUser.get());
        when(request.getParameter("key")).thenReturn(null);

        // when
        filter.doFilter(request, null, filterChain);

        // then
        verify(authenticationService, times(1)).lookupUser(nullable(String.class));
        verify(userService, never()).updateLastApiUseTime(any(AuthenticatedUser.class));
        verify(dataverseSession, never()).setUser(any(AuthenticatedUser.class));
        verify(httpSession, never()).invalidate();
    }
}