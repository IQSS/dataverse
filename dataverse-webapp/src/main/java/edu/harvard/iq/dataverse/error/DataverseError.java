package edu.harvard.iq.dataverse.error;

import java.util.Optional;

public class DataverseError {

    private Exception exception;
    private String errorMsg;

    // -------------------- CONSTRUCTORS --------------------

    public DataverseError(Exception exception, String errorMsg) {
        this.exception = exception;
        this.errorMsg = errorMsg;
    }

    public DataverseError(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    // -------------------- GETTERS --------------------

    public Optional<Exception> getException() {
        return Optional.ofNullable(exception);
    }

    public String getErrorMsg() {
        return errorMsg;
    }
}
