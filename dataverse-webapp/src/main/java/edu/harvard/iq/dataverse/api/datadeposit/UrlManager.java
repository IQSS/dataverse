package edu.harvard.iq.dataverse.api.datadeposit;

import edu.harvard.iq.dataverse.util.SystemConfig;
import org.apache.commons.lang.StringUtils;
import org.swordapp.server.SwordError;
import org.swordapp.server.UriRegistry;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;


public class UrlManager {
    String originalUrl;
    String servlet;
    String targetType;
    String targetIdentifier;
    int port;

    // -------------------- GETTERS --------------------
    public String getOriginalUrl() {
        return originalUrl;
    }

    public int getPort() {
        return port;
    }

    public String getServlet() {
        return servlet;
    }

    public String getTargetIdentifier() {
        return targetIdentifier;
    }

    public String getTargetType() {
        return targetType;
    }

    // -------------------- SETTERS --------------------
    public void setPort(int port) {
        this.port = port;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public void setServlet(String servlet) {
        this.servlet = servlet;
    }

    public void setTargetIdentifier(String targetIdentifier) {
        this.targetIdentifier = targetIdentifier;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

}
