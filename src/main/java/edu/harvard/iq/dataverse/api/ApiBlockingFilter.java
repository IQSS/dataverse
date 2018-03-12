package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A web filter to block API administration calls.
 * @author michael
 */
public class ApiBlockingFilter implements javax.servlet.Filter {
    private static final String UNBLOCK_KEY_QUERYPARAM = "unblock-key";
            
    interface BlockPolicy {
        public void doBlock(ServletRequest sr, ServletResponse sr1, FilterChain fc) throws IOException, ServletException;
    }
    
    /**
     * A policy that allows all requests.
     */
    private static final BlockPolicy ALLOW = new BlockPolicy(){
        @Override
        public void doBlock(ServletRequest sr, ServletResponse sr1, FilterChain fc) throws IOException, ServletException {
            fc.doFilter(sr, sr1);
        }
    };
    
    /**
     * A policy that drops blocked requests.
     */
    private static final BlockPolicy DROP = new BlockPolicy(){
        @Override
        public void doBlock(ServletRequest sr, ServletResponse sr1, FilterChain fc) throws IOException, ServletException {
            HttpServletResponse httpResponse = (HttpServletResponse) sr1;
            httpResponse.getWriter().println("{ status:\"error\", message:\"Endpoint blocked. Please contact the dataverse administrator\"}" );
            httpResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            httpResponse.setContentType("application/json");
        }
    };
    
    /**
     * Allow only from localhost.
     */
    private static final BlockPolicy LOCAL_HOST_ONLY = new BlockPolicy() {

        @Override
        public void doBlock(ServletRequest sr, ServletResponse sr1, FilterChain fc) throws IOException, ServletException {
            IpAddress origin = new DataverseRequest( null, (HttpServletRequest)sr ).getSourceAddress();
            if ( origin.isLocalhost() ) {
                fc.doFilter(sr, sr1);
            } else {
                HttpServletResponse httpResponse = (HttpServletResponse) sr1;
                httpResponse.getWriter().println("{ status:\"error\", message:\"Endpoint available from localhost only. Please contact the dataverse administrator\"}" );
                httpResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                httpResponse.setContentType("application/json");
            }
        }
    };
      
    /**
     * Allow only for requests that have the {@link #UNBLOCK_KEY_QUERYPARAM} param with
     * value from {@link SettingsServiceBean.Key.BlockedApiKey}
     */
    private final BlockPolicy unblockKey = new BlockPolicy() {

        @Override
        public void doBlock(ServletRequest sr, ServletResponse sr1, FilterChain fc) throws IOException, ServletException {
            boolean block = true;
            
            String masterKey = settingsSvc.getValueForKey(SettingsServiceBean.Key.BlockedApiKey);
            if ( masterKey != null ) {
                String queryString = ((HttpServletRequest)sr).getQueryString();
                if ( queryString != null ) {
                    for ( String paramPair : queryString.split("&") ) {
                        String[] curPair = paramPair.split("=",-1);
                        if ( (curPair.length >= 2 )
                               && UNBLOCK_KEY_QUERYPARAM.equals(curPair[0])
                               && masterKey.equals(curPair[1]) ) {
                            block = false;
                            break;
                        }
                    }
                }
            }
            
            if ( block ) {
                HttpServletResponse httpResponse = (HttpServletResponse) sr1;
                httpResponse.getWriter().println("{ status:\"error\", message:\"Endpoint available using API key only. Please contact the dataverse administrator\"}" );
                httpResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                httpResponse.setContentType("application/json");
            } else {
                fc.doFilter(sr, sr1);
            }
        }
    };
    
    private static final Logger logger = Logger.getLogger(ApiBlockingFilter.class.getName());
    
    @EJB
    protected SettingsServiceBean settingsSvc;
    
    final Set<String> blockedApiEndpoints = new TreeSet<>();
    private String lastEndpointList;
    private final Map<String, BlockPolicy> policies = new TreeMap<>();
    
    @Override
    public void init(FilterConfig fc) throws ServletException {
        updateBlockedPoints();
        policies.put("allow", ALLOW);
        policies.put("drop", DROP);
        policies.put("localhost-only", LOCAL_HOST_ONLY);
        policies.put("unblock-key", unblockKey);
    }

    private void updateBlockedPoints() {
        blockedApiEndpoints.clear();
        String endpointList = settingsSvc.getValueForKey(SettingsServiceBean.Key.BlockedApiEndpoints, "");
        for ( String endpoint : endpointList.split(",") ) {
            String endpointPrefix = canonize(endpoint);
            if ( ! endpointPrefix.isEmpty() ) {
                endpointPrefix = endpointPrefix + "/"; 
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
        String requestURI = hsr.getRequestURI();
        String apiEndpoint = canonize(requestURI.substring(hsr.getServletPath().length()));
        for ( String prefix : blockedApiEndpoints ) {
            if ( apiEndpoint.startsWith(prefix) ) {
                getBlockPolicy().doBlock(sr, sr1, fc);
                return;
            }
        }
        try {
            fc.doFilter(sr, sr1);
        } catch ( ServletException se ) {
            logger.log(Level.WARNING, "Error processing " + requestURI +": " + se.getMessage(), se);
            HttpServletResponse resp = (HttpServletResponse) sr1;
            resp.setStatus(500);
            resp.setHeader("PROCUDER", "ApiBlockingFilter");
            resp.getWriter().append("Error: " + se.getMessage());
        }
    }
    
    @Override
    public void destroy() {}
    
    private BlockPolicy getBlockPolicy() {
        String blockPolicyName = settingsSvc.getValueForKey(SettingsServiceBean.Key.BlockedApiPolicy, "");
        BlockPolicy p = policies.get(blockPolicyName.trim());
        if ( p != null ) {
            return p;
        } else {
            logger.log(Level.WARNING, "Undefined block policy {0}. Available policies are {1}",
                    new Object[]{blockPolicyName, policies.keySet()});
            return ALLOW;
        }
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
