package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A web filter to block API administration calls.
 * @author michael
 */
@WebFilter( urlPatterns={"/api/*"} )
public class ApiBlockingFilter implements javax.servlet.Filter {
    
    private static final Logger logger = Logger.getLogger(ApiBlockingFilter.class.getName());
    
    @EJB
    protected SettingsServiceBean settingsSvc;
    
    final Set<String> blockedApiEndpoints = new TreeSet<>();
    private String lastEndpointList;
    
    @Override
    public void init(FilterConfig fc) throws ServletException {
        updateBlockedPoints();
    }

    private void updateBlockedPoints() {
        blockedApiEndpoints.clear();
        String endpointList = settingsSvc.getValueForKey(SettingsServiceBean.Key.BlockedApiEndpoints, "");
        for ( String endpoint : endpointList.split(",") ) {
            String endpointPrefix = canonize(endpoint);
            if ( ! endpointPrefix.isEmpty() ) {
                logger.log(Level.INFO, "Blocking API endpoint: {0}", endpointPrefix);
                blockedApiEndpoints.add(endpointPrefix);
            }
        }
        lastEndpointList = endpointList;
    }

    @Override
    public void doFilter(ServletRequest sr, ServletResponse sr1, FilterChain fc) throws IOException, ServletException {
        String endpointList = settingsSvc.getValueForKey(SettingsServiceBean.Key.BlockedApiEndpoints, "");
        if ( ! endpointList.equals(lastEndpointList) ) {
            updateBlockedPoints();
        }
        
        HttpServletRequest hsr = (HttpServletRequest) sr;
        String apiEndpoint = canonize(hsr.getRequestURI().substring(hsr.getServletPath().length()));
        
        for ( String prefix : blockedApiEndpoints ) {
            if ( apiEndpoint.startsWith(prefix) ) {
                // Block!
                HttpServletResponse httpResponse = (HttpServletResponse) sr1;
                httpResponse.getWriter().println("{ status:\"error\", message:\"Endpoint blocked. Please contact the dataverse administrator\"}" );
                httpResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                httpResponse.setContentType("application/json");
                return;
            }
        }
        
        fc.doFilter(sr, sr1);
    }

    @Override
    public void destroy() {
        logger.info("WebFilter destroy");
    }
    
    /**
     * Creates a canonical representation of {@code in}: trimmed spaces and slashes
     * @param in the raw string
     * @return {@code in} with no trailing and leading spaces and slashes.
     */
    private String canonize( String in ) {
        in = in.trim();
        if ( in.startsWith("/") ) {
            in = in.substring(1);
        }
        if ( in.endsWith("/") ) {
            in = in.substring(0, in.length()-1);
        }
        return in;
    } 
    
}
