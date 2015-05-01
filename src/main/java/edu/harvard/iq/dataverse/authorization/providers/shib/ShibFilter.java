package edu.harvard.iq.dataverse.authorization.providers.shib;

import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The purpose of this filter is cosmetic. We relax the same origin policy when
 * the browser is on port 8181 on shib.xhtml (having just returned from a
 * successful authentication attempt) so that images and CSS are properly loaded
 * while the user is deciding whether or not to accept the general terms of use
 * and create (or convert) an account.
 */
public class ShibFilter implements Filter {

    private static final Logger logger = Logger.getLogger(ShibFilter.class.getCanonicalName());

    @Override
    public void init(FilterConfig fc) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        String headerKey = "Access-Control-Allow-Origin";
        /**
         * Using "*" here would be less secure.
         *
         * "Be careful setting that to *, though... I suspect that may make you
         * more vulnerable to scripting attacks."
         * http://javabot.evanchooly.com/logs/%23%23jsf/2015-04-29
         *
         * Instead, we set it to "https://dataverse.harvard.edu:8181" for
         * example.
         */
        String headerValue = "https://" + httpServletRequest.getServerName() + ":" + SystemConfig.APACHE_HTTPS_PORT;
        httpServletResponse.setHeader(headerKey, headerValue);
        logger.fine("Setting header \"" + headerKey + "\" to \"" + headerValue + "\" for " + httpServletRequest.getRequestURI());
        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    @Override
    public void destroy() {
    }

}
