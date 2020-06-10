package edu.harvard.iq.dataverse.authorization;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class ProxyRequestWrapper extends HttpServletRequestWrapper {
    
    String realIP;
    
    public ProxyRequestWrapper(HttpServletRequest request, String realIP) {
        super(request);
        this.realIP = realIP;
    }
    
    @Override
    public String getRemoteAddr() {
        return this.realIP;
    }
}
