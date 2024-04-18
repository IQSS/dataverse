package edu.harvard.iq.dataverse.validation;

public class ValidationResult {

    public static ValidationResult OK = new ValidationResult(true, null);

    private final boolean ok;

    private final String errorCode;

    // -------------------- CONSTRUCTORS --------------------

    protected ValidationResult(boolean ok, String errorCode) {
        this.ok = ok;
        this.errorCode = errorCode;
    }

    // -------------------- GETTERS --------------------

    public String getErrorCode() {
        return errorCode;
    }

    // -------------------- LOGIC --------------------

    public static ValidationResult ok() {
        return OK;
    }

    public static ValidationResult invalid(String errorCode) {
        return new ValidationResult(false, errorCode);
    }

    public boolean isOk() {
        return ok;
    }
}
