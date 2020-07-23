package edu.harvard.iq.dataverse.api.datadeposit;

public class UrlManager {
    String originalUrl;
    String servlet;
    String targetType;
    String targetIdentifier;
    int port;
    String warning;

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

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }
}
