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
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author madunlap
 */
public class MalformedUrlFilter implements Filter{
    
    private static final Logger logger = Logger.getLogger(MalformedUrlFilter.class.getCanonicalName());
    
    static class CleanHttpRequest extends HttpServletRequestWrapper {
        public CleanHttpRequest(ServletRequest request) {
            super((HttpServletRequest) request);
        }
        
        public String getParameter(String paramName) {
            return ""; //MAD: can we just return nothing?
        }
    }
    
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

        StringBuffer requestURL = request.getRequestURL(); 
        String fullRequestURL;
        
        String queryString = request.getQueryString();

        if (queryString == null) {
            fullRequestURL = requestURL.toString();
        } else {
            fullRequestURL = requestURL.append('?').append(queryString).toString();
        }

        //MAD: This url will blow up in the broswer
        //http://localhost:8080/dataverse.xhtml?q=&fq0=authorName_ss%3A%22Yu%%222C+M%22&types=files&sort=dateSort&order=asc
        
        //logger.log(Level.WARNING,"MAD filter is triggered : " + requestURL);
        
        try {
            java.net.URI uri = new java.net.URI(fullRequestURL);
            
            //logger.log(Level.WARNING,"MAD filter has passed thru");
            chain.doFilter(req, res); 

        } catch (Exception e) { //MAD: catch more fine
            logger.log(Level.WARNING,"MAD Filter caught error " + e);

            

            /** MAD: I tried these two options below to send the user to a 404 page to no success */
            
            //response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            //chain.doFilter(new CleanHttpRequest(req), res);
            
            //req.getRequestDispatcher("/404.xhtml").forward(new CleanHttpRequest(req), res);

        }
        
    }

    @Override
    public void destroy() {
        //
    }
}