package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.DataverseSession;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import jakarta.inject.Inject;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

public class AuthFilter implements Filter {

    private static final Logger logger = Logger.getLogger(AuthFilter.class.getCanonicalName());

    @Inject
    DataverseSession session;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info(AuthFilter.class.getName() + "initialized. filterConfig.getServletContext().getServerInfo(): " + filterConfig.getServletContext().getServerInfo());

        try {
            FileHandler logFile = new FileHandler( System.getProperty("com.sun.aas.instanceRoot") + File.separator + "logs" + File.separator + "authfilter.log");
            SimpleFormatter formatterTxt = new SimpleFormatter();
            logFile.setFormatter(formatterTxt);
            logger.addHandler(logFile);
        } catch (IOException ex) {
            Logger.getLogger(AuthFilter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(AuthFilter.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

        String username = session.getUser().getIdentifier();
        String remoteAddr = httpServletRequest.getRemoteAddr();
        String requestUri = httpServletRequest.getRequestURI();
        String userAgent = httpServletRequest.getHeader("User-Agent");

        String separator = "|";

        StringBuilder sb = new StringBuilder();
        for (String string : Arrays.asList(username, remoteAddr, requestUri, userAgent)) {
            sb.append(string + separator);
        }

        logger.info(sb.toString());

        filterChain.doFilter(servletRequest, response);
    }

    @Override
    public void destroy() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
