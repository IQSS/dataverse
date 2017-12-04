/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author madunlap
 */
public class MalformedUrlFilter implements Filter{
    
    private static final Logger logger = Logger.getLogger(MalformedUrlFilter.class.getCanonicalName());
    
    //MAD: is this heavy?
    @Inject 
    PermissionsWrapper permissionsWrapper;
    
    @Override
    public void init(FilterConfig config) throws ServletException {
        //
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        //MAD: not sure if I should be using uri instead. That also doesn't return a stringbuffer which my example used
        //
        StringBuffer requestURL = request.getRequestURL(); 
        String fullRequestURL;
        
        String queryString = request.getQueryString();

        if (queryString == null) {
            fullRequestURL = requestURL.toString();
        } else {
            fullRequestURL = requestURL.append('?').append(queryString).toString();
        }

        
        logger.log(Level.WARNING,"MAD filter is triggered : " + requestURL);
        
        try {
            //response.encodeURL(requestURI);
            java.net.URI uri = new java.net.URI(fullRequestURL); //maybe this will just blow up...
           
            
            
        } catch (Exception e) { //MAD: catch more fine
            logger.log(Level.WARNING,"MAD Filter caught error " + e);

            //String newURI = requestURI.replace(toReplace, "?Contact_Id=");
            
//            HttpServletResponse httpResponse = (HttpServletResponse) res;
//            httpResponse.getWriter().println("{ status:\"error\", message:\"Malformed url explosion\"}" );
//            httpResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
//            httpResponse.setContentType("application/json");
    
            String requestURI = request.getRequestURI();
            logger.log(Level.WARNING,"MAD filter error old requestURI " + requestURI);

            String toReplace = requestURI.substring(requestURI.indexOf("?"), requestURI.length() + 1);
            String newURI = requestURI.replace(toReplace, "");

            logger.log(Level.WARNING,"MAD filter error old requestURI " + newURI);
            
            
            req.getRequestDispatcher(newURI).forward(req, res); //hopefully this won't include the query string... regardless we should use 400
        }
        
        
        //context.getExternalContext().responseSendError(errorCode,null);
        
        
        
        
        
        //req.getRequestDispatcher(permissionsWrapper.notAuthorized()).forward(req, res); //MAD: shouldn't be here but I want to see if it forwards good urls to the error page

        logger.log(Level.WARNING,"MAD filter has passed thru");
        chain.doFilter(req, res); 
        
//MAD: Not my code, example below
        
//        if (requestURI.startsWith("/Check_License/Dir_My_App/")) {
//            String toReplace = requestURI.substring(requestURI.indexOf("/Dir_My_App"), requestURI.lastIndexOf("/") + 1);
//            String newURI = requestURI.replace(toReplace, "?Contact_Id=");
//            req.getRequestDispatcher(newURI).forward(req, res);
//        } else {
//            chain.doFilter(req, res);
//        }
    }

    @Override
    public void destroy() {
        //
    }
}