package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.SystemConfig;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.SessionCookieConfig;
import java.util.logging.Logger;

@WebListener
public class DataverseSessionConfigListener implements ServletContextListener {
    private final SystemConfig systemConfig;
    private static final Logger logger = Logger.getLogger(DataverseSessionConfigListener.class.getCanonicalName());

    @Inject
    public DataverseSessionConfigListener(SystemConfig systemConfig) {
        this.systemConfig = systemConfig;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("Initializing session cookie configuration");
        SessionCookieConfig sessionCookieConfig = sce.getServletContext().getSessionCookieConfig();
        String cookieName = systemConfig.getCookieName();
        if (cookieName != null && !cookieName.isEmpty()) {
            logger.info("Setting session cookie name to " + cookieName);
            sessionCookieConfig.setName(cookieName);
        }
        String cookieDomain = systemConfig.getCookieDomain();
        if (cookieDomain != null && !cookieName.isEmpty()) {
            logger.info("Setting session cookie domain to " + cookieDomain);
            sessionCookieConfig.setDomain(cookieDomain);
        }
        Boolean cookieSecure = systemConfig.getCookieSecure();
        if (cookieSecure != null && !cookieName.isEmpty()) {
            logger.info("Setting session cookie secure to " + cookieSecure);
            sessionCookieConfig.setSecure(cookieSecure);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        // nothing to do here
    }
}
