package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.util.SystemConfig;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.swordapp.server.SwordError;
import org.swordapp.server.UriRegistry;

public class UrlManager {

    private static final Logger logger = Logger.getLogger(UrlManager.class.getCanonicalName());
    String originalUrl;
    SwordConfigurationImpl swordConfiguration = new SwordConfigurationImpl();
    String servlet;
    String targetType;
    String targetIdentifier;
    int port;

    String processUrl(String url) throws SwordError {
        logger.fine("URL was: " + url);
        String warning = null;
        this.originalUrl = url;
        URI javaNetUri;
        try {
            javaNetUri = new URI(url);
        } catch (URISyntaxException ex) {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Invalid URL syntax: " + url);
        }
        this.port = javaNetUri.getPort();
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
        if (!swordConfiguration.getBaseUrlPathsValid().contains(dataDepositApiBasePath)) {
            throw new SwordError(dataDepositApiBasePath + " found but one of these required: " + swordConfiguration.getBaseUrlPathsValid() + ". Current version is " + swordConfiguration.getBaseUrlPathCurrent());
        } else {
            if (swordConfiguration.getBaseUrlPathsDeprecated().contains(dataDepositApiBasePath)) {
                String msg = "Deprecated version used for Data Deposit API. The current version expects '" + swordConfiguration.getBaseUrlPathCurrent() + "'. URL passed in: " + url;
                warning = msg;
            }
        }
        try {
            this.servlet = urlParts.get(6);
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Unable to determine servlet path from URL: " + url);
        }
        if (!servlet.equals("service-document")) {
            List<String> targetTypeAndIdentifier;
            try {
                //               6          7         8
                // for example: /collection/dataverse/sword
                targetTypeAndIdentifier = urlParts.subList(7, urlParts.size());
            } catch (IndexOutOfBoundsException ex) {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "No target components specified in URL: " + url);
            }
            this.targetType = targetTypeAndIdentifier.get(0);
            if (targetType != null) {
                if (targetType.equals("dataverse")) {
                    String dvAlias;
                    try {
                        dvAlias = targetTypeAndIdentifier.get(1);
                    } catch (IndexOutOfBoundsException ex) {
                        throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "No dataverse alias provided in URL: " + url);
                    }
                    this.targetIdentifier = dvAlias;
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
                } else if (targetType.equals("study")) {
                    String globalId;
                    try {
                        List<String> globalIdParts = targetTypeAndIdentifier.subList(1, targetTypeAndIdentifier.size());
                        globalId = StringUtils.join(globalIdParts, "/");
                    } catch (IndexOutOfBoundsException ex) {
                        throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Invalid study global id provided in URL: " + url);
                    }
                    this.targetIdentifier = globalId;
                } else if (targetType.equals("file")) {
                    String fileIdString;
                    try {
                        // a user might reasonably pass in a filename as well [.get(2)] since
                        // we expose it in the statement of a study but we ignore it here
                        // Some day it might be nice to support persistent IDs for files.
                        fileIdString = targetTypeAndIdentifier.get(1);
                    } catch (IndexOutOfBoundsException ex) {
                        throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "No file id provided in URL: " + url);
                    }
                    this.targetIdentifier = fileIdString;
                } else {
                    throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "unsupported target type: " + targetType);
                }
            } else {
                throw new SwordError(UriRegistry.ERROR_BAD_REQUEST, "Unable to determine target type from URL: " + url);
            }
            logger.fine("target type: " + targetType);
            logger.fine("target identifier: " + targetIdentifier);
        }
        if (warning != null) {
            logger.info(warning);
        }
        return warning;
    }

    String getHostnamePlusBaseUrlPath(String url) throws SwordError {
        /**
         * @todo Any problem with returning the current API version rather than
         * the version that was operated on? Both should work. If SWORD API
         * users are operating on the URLs returned (as they should) returning
         * the current version will avoid deprecation warnings on the Dataverse
         * side.
         */
        return SystemConfig.getDataverseSiteUrlStatic() + swordConfiguration.getBaseUrlPathCurrent();
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getServlet() {
        return servlet;
    }

    public void setServlet(String servlet) {
        this.servlet = servlet;
    }

    public String getTargetIdentifier() {
        return targetIdentifier;
    }

    public void setTargetIdentifier(String targetIdentifier) {
        this.targetIdentifier = targetIdentifier;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

}
