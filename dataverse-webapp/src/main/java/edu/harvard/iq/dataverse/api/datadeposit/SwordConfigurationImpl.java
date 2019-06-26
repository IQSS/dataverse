package edu.harvard.iq.dataverse.api.datadeposit;

import org.swordapp.server.SwordConfiguration;

/**
 * pure model class for Sword Configuration implementation
 */
public class SwordConfigurationImpl implements SwordConfiguration {

    private String tempDirectory;
    private int maxUploadSize;
    private boolean depositReceipt;
    private boolean errorBody;
    private boolean stackTraceInError;
    private String generatorUrl;
    private String generatorVersion;
    private String administratorEmail;
    private String authType;
    private boolean storeAndCheckBinary;
    private String alternateUrl;
    private String alternateUrlContentType;
    private boolean allowUnauthenticatedMediaAccess;

    @Override
    public boolean returnDepositReceipt() {
        return depositReceipt;
    }

    @Override
    public boolean returnStackTraceInError() {
        return stackTraceInError;
    }

    @Override
    public boolean returnErrorBody() {
        return errorBody;
    }

    @Override
    public String generator() {
        return generatorUrl;
    }

    @Override
    public String generatorVersion() {
        return generatorVersion;
    }

    @Override
    public String administratorEmail() {
        return administratorEmail;
    }

    @Override
    public String getAuthType() {
        return authType;
    }

    @Override
    public boolean storeAndCheckBinary() {
        return storeAndCheckBinary;
    }

    @Override
    public String getTempDirectory() {
        return tempDirectory;
    }

    @Override
    public int getMaxUploadSize() {
        return maxUploadSize;
    }

    @Override
    public String getAlternateUrl() {
        return alternateUrl;
    }

    @Override
    public String getAlternateUrlContentType() {
        return alternateUrlContentType;
    }

    @Override
    public boolean allowUnauthenticatedMediaAccess() {
        return allowUnauthenticatedMediaAccess;
    }

    // -------------------- SETTERS --------------------
    public void setTempDirectory(String tempDirectory) {
        this.tempDirectory = tempDirectory;
    }

    public void setMaxUploadSize(int maxUploadSize) {
        this.maxUploadSize = maxUploadSize;
    }

    public void setErrorBody(boolean errorBody) {
        this.errorBody = errorBody;
    }

    public void setDepositReceipt(boolean depositReceipt) {
        this.depositReceipt = depositReceipt;
    }

    public void setStackTraceInError(boolean stackTraceInError) {
        this.stackTraceInError = stackTraceInError;
    }

    public void setGeneratorUrl(String generatorUrl) {
        this.generatorUrl = generatorUrl;
    }

    public void setGeneratorVersion(String generatorVersion) {
        this.generatorVersion = generatorVersion;
    }

    public void setAdministratorEmail(String administratorEmail) {
        this.administratorEmail = administratorEmail;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public void setStoreAndCheckBinary(boolean storeAndCheckBinary) {
        this.storeAndCheckBinary = storeAndCheckBinary;
    }

    public void setAlternateUrl(String alternateUrl) {
        this.alternateUrl = alternateUrl;
    }

    public void setAlternateUrlContentType(String alternateUrlContentType) {
        this.alternateUrlContentType = alternateUrlContentType;
    }

    public void setAllowUnauthenticatedMediaAccess(boolean allowUnauthenticatedMediaAccess) {
        this.allowUnauthenticatedMediaAccess = allowUnauthenticatedMediaAccess;
    }
}
