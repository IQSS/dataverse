package edu.harvard.iq.dataverse.api;

import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * Routes API calls that don't have a version number to the latest API version
 * 
 * @author michael
 */
public class ApiRouter implements javax.servlet.Filter {
    private static final Logger logger = Logger.getLogger(ApiRouter.class.getName());
    
    @Override
    public void init(FilterConfig fc) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse sr1, FilterChain fc) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        String requestUri = request.getRequestURI();
        if ( requestUri.startsWith("/api/v1/") ) {
            fc.doFilter(req, sr1);
        } else {
            String newRequestUri = "/api/v1" + requestUri.substring(4);
            RequestDispatcher dsp = request.getRequestDispatcher(newRequestUri);
            dsp.forward(req, sr1);
        }
    }

    @Override
    public void destroy() {
    }
    
}