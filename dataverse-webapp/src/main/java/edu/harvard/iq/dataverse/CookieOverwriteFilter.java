package edu.harvard.iq.dataverse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.util.Collection;

public class CookieOverwriteFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(CookieOverwriteFilter.class);

    private HttpSession session;

    // -------------------- CONSTRUCTOR --------------------

    @Inject
    public CookieOverwriteFilter(HttpSession session) {
        this.session = session;
    }

    // -------------------- LOGIC --------------------

    @Override
    public void init(FilterConfig filterConfig) throws ServletException { }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (session != null) {
            // This call will somehow force the session to be initialized if it's not initialized yet
            session.getId();
        }
        HttpServletResponse servletResponse = (HttpServletResponse) response;
        tryToOverwriteCookieHeader(servletResponse);
        chain.doFilter(request, response);
        tryToOverwriteCookieHeader(servletResponse);
    }

    @Override
    public void destroy() { }

    // -------------------- PRIVATE --------------------

    private void tryToOverwriteCookieHeader(HttpServletResponse response) {
        Collection<String> headers = response.getHeaders(HttpHeaders.SET_COOKIE);
        int i = 0;
        for (String header : headers) {
            if (!header.contains("SameSite")) {
                String extendedHeader = header + "; SameSite=None; Secure";
                if (i == 0) {
                    response.setHeader(HttpHeaders.SET_COOKIE, extendedHeader);
                } else {
                    response.addHeader(HttpHeaders.SET_COOKIE, extendedHeader);
                }
                ++i;
            }
        }
    }
}
