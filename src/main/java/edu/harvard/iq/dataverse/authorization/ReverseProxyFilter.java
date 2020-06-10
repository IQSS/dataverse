package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.util.SystemConfig;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@WebFilter( filterName = "reverse-proxy", urlPatterns = "/*")
public class ReverseProxyFilter extends HttpFilter {
    
    @Inject
    SystemConfig systemConfig;
    
    String headerField;
    
    private static final Logger logger = Logger.getLogger(ReverseProxyFilter.class.getName());
    private static final String X_FORWARDED_FOR = "X-Forwarded-For"; // de-facto standard
    private static final String RFC7239_FORWARDED = "Forwarded";
    private static HashSet<String> ALLOWED_HEADERS = new HashSet<String>(Arrays.asList(
        X_FORWARDED_FOR,
        RFC7239_FORWARDED,
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP",
        "HTTP_CLIENT_IP",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED",
        "HTTP_VIA",
        "REMOTE_ADDR" ));
    
    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest proxy = req;
        
        // process headers and try to find an ip if a header field is present (=configured)
        if (headerField != null) {
            String proxiedIp;
            
            // get header(s) matching the field and process if not empty (= not present in request)
            List<String> headers = Collections.list(req.getHeaders(headerField));
            logger.log(Level.FINER, "Header(s) \""+headerField+"\": "+StringUtils.join(", "));
            
            if ( ! headers.isEmpty()) {
                // special parsing for X-Forwarded-For de-facto standard
                if (headerField.equals(X_FORWARDED_FOR)) {
                    proxiedIp = parseXForwardedFor(headers);
                }
                // special parsing for RFC 7239 "Forwarded" syntax
                else if (headerField.equals(RFC7239_FORWARDED)) {
                    proxiedIp = parseRFC7239Forwarded(headers);
                }
                // otherwise get last header (if multiple), which should have been added by the proxy the client hits
                // *first*, thus being the real client (and not an intermediate proxy). we assume headers are in order.
                else {
                    proxiedIp = validateIp(headers.get(headers.size()-1));
                }
                
                // the ip has been found and verified/validated, let's create a usefull proxied request, allowing
                // to use getRemoteAddr() and retrieve clients instead of proxies ip.
                if (proxiedIp != null) {
                    logger.log(Level.FINE, "Real client IP: \""+proxiedIp+"\"");
                    proxy = new ProxyRequestWrapper(req, proxiedIp);
                }
            }
        }
        
        super.doFilter(proxy, res, chain);
    }
    
    String parseXForwardedFor(List<String> headers) {
       return null;
    }
    
    String parseRFC7239Forwarded(List<String> headers) {
        return null;
    }
    
    String validateIp(String ip) {
        return null;
    }
    
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String field = systemConfig.getUserIpAddressSourceHeaderField();
        if (! ALLOWED_HEADERS.contains(field)) {
            throw new ServletException("\""+field+"\" is not a valid header field name to retrieve a client IP from. Choose one of "+ALLOWED_HEADERS.toString()+".");
        }
        this.headerField = field;
    }
    
    @Override
    public void destroy() {
        // intentionally left blank - no deconstruction needed.
    }
}
