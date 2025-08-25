package edu.harvard.iq.dataverse.api;

import jakarta.servlet.Filter;
import java.io.IOException;
import java.util.logging.Logger;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.annotation.WebFilter;

/**
 * Routes API calls that don't have a version number to the latest API version
 * 
 * @author michael
 */
@WebFilter("/api/*")
public class ApiRouter implements Filter {
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