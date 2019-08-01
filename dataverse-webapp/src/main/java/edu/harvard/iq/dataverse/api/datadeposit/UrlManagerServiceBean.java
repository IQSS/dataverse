package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.apache.commons.lang.StringUtils;
import org.swordapp.server.SwordError;
import org.swordapp.server.UriRegistry;

import javax.ejb.Stateful;
import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

@Stateful
public class UrlManagerServiceBean {
    private static final Logger logger = Logger.getLogger(UrlManagerServiceBean.class.getCanonicalName());

    private UrlManager urlManager = new UrlManager();

    @Inject
    private SettingsServiceBean settingsService;

    @Inject
    private SystemConfig systemConfig;

    // -------------------- GETTERS --------------------
    public UrlManager getUrlManager() {
        return urlManager;
    }

    // -------------------- LOGIC --------------------
    public String processUrl(String url) throws SwordError {
        String warning = null;
        urlManager.setOriginalUrl(url);
        URI javaNetUri;
        try {
            javaNetUri = new URI(url);
        } catch (URISyntaxException ex) {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Invalid URL syntax: " + url);
        }
        /**
         * @todo: figure out another way to check for http. We used to use
         * javaNetUri.getScheme() but now that we are using "ProxyPass /
         * ajp://localhost:8009/" in Apache it's always http rather than https.
         *
         * http://serverfault.com/questions/6128/how-do-i-force-apache-to-use-https-in-conjunction-with-ajp
         * http://stackoverflow.com/questions/1685563/apache-webserver-jboss-ajp-connectivity-with-https
         * http://stackoverflow.com/questions/12460422/how-do-ensure-that-apache-ajp-to-tomcat-connection-is-secure-encrypted
         */
        if (!"https".equals(javaNetUri.getScheme())) {
            /**
             * @todo figure out how to prevent this stackstrace from showing up
             * in Glassfish logs:
             *
             * Unable to populate SSL attributes
             * java.lang.IllegalStateException: SSLEngine is null at
             * org.glassfish.grizzly.ssl.SSLSupportImpl
             *
             * https://github.com/IQSS/dataverse/issues/643
             *
             * SSLOptions +StdEnvVars +ExportCertData ?
             *
             * [#GLASSFISH-20694] Glassfish 4.0 and jk Unable to populate SSL
             * attributes - Java.net JIRA -
             * https://java.net/jira/browse/GLASSFISH-20694
             */
//            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "https is required but protocol was " + javaNetUri.getScheme());
        }
        urlManager.setPort(javaNetUri.getPort());
        String[] urlPartsArray = javaNetUri.getPath().split("/");
        List<String> urlParts = Arrays.asList(urlPartsArray);
        String dataDepositApiBasePath;
        try {
            List<String> dataDepositApiBasePathParts;
            //             1 2   3   4            5  6       7          8         9
            // for example: /dvn/api/data-deposit/v1/swordv2/collection/dataverse/sword
            dataDepositApiBasePathParts = urlParts.subList(0, 6);
            dataDepositApiBasePath = StringUtils.join(dataDepositApiBasePathParts, "/");
        } catch (IndexOutOfBoundsException ex) {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Error processing URL: " + url);
        }
        if (!SwordConfigurationConstants.BASE_URL_PATHS_VALID.contains(dataDepositApiBasePath)) {
            throw new SwordError(dataDepositApiBasePath + " found but one of these required: " +
                    SwordConfigurationConstants.BASE_URL_PATHS_VALID + ". Current version is "
                    + SwordConfigurationConstants.BASE_URL_PATH_CURRENT);
        } else {
            if (SwordConfigurationConstants.BASE_URL_PATHS_DEPRECATED.contains(dataDepositApiBasePath)) {
                String msg = "Deprecated version used for Data Deposit API. The current version expects '" +
                        SwordConfigurationConstants.BASE_URL_PATH_CURRENT + "'. URL passed in: " + url;
                warning = msg;
            }
        }
        try {
            urlManager.setServlet(urlParts.get(6));
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Unable to determine servlet path from URL: " + url);
        }
        if (!urlManager.getServlet().equals("service-document")) {
            List<String> targetTypeAndIdentifier;
            try {
                //               6          7         8
                // for example: /collection/dataverse/sword
                targetTypeAndIdentifier = urlParts.subList(7, urlParts.size());
            } catch (IndexOutOfBoundsException ex) {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "No target components specified in URL: " + url);
            }
            urlManager.setTargetType(targetTypeAndIdentifier.get(0));
            if (urlManager.getTargetType() != null) {
                if (urlManager.getTargetType().equals("dataverse")) {
                    String dvAlias;
                    try {
                        dvAlias = targetTypeAndIdentifier.get(1);
                    } catch (IndexOutOfBoundsException ex) {
                        throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "No dataverse alias provided in URL: " + url);
                    }
                    urlManager.setTargetIdentifier(dvAlias);
                    /**
                     * @todo it would be nice to support "dataset" as an alias
                     * for "study" since that's what we call them now in
                     * Dataverse 4.0. We should continue to support "study" in
                     * the URL however because some API users have these URLs
                     * stored in databases:
                     * http://irclog.iq.harvard.edu/dvn/2014-05-14#i_9404
                     *
                     * Also, to support "dataset" in URLs properly, we'd need to
                     * examine all the places where we return the string "study"
                     * such as in the Deposit Receipt.
                     */
                } else if (urlManager.getTargetType().equals("study")) {
                    String globalId;
                    try {
                        List<String> globalIdParts = targetTypeAndIdentifier.subList(1, targetTypeAndIdentifier.size());
                        globalId = StringUtils.join(globalIdParts, "/");
                    } catch (IndexOutOfBoundsException ex) {
                        throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Invalid study global id provided in URL: " + url);
                    }
                    urlManager.setTargetIdentifier(globalId);
                } else if (urlManager.getTargetType().equals("file")) {
                    String fileIdString;
                    try {
                        // a user might reasonably pass in a filename as well [.get(2)] since
                        // we expose it in the statement of a study but we ignore it here
                        // Some day it might be nice to support persistent IDs for files.
                        fileIdString = targetTypeAndIdentifier.get(1);
                    } catch (IndexOutOfBoundsException ex) {
                        throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "No file id provided in URL: " + url);
                    }
                    urlManager.setTargetIdentifier(fileIdString);
                } else {
                    throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "unsupported target type: " + urlManager.getTargetType());
                }
            } else {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Unable to determine target type from URL: " + url);
            }
        }
        if (warning != null) {
            logger.info(warning);
        }
        return warning;
    }

    public String getHostnamePlusBaseUrlPath() {
        return systemConfig.getDataverseSiteUrl() + SwordConfigurationConstants.BASE_URL_PATH_CURRENT;
    }
}
