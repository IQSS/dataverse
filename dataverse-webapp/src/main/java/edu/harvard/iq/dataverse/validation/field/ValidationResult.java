package edu.harvard.iq.dataverse.validation.field;

import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import org.apache.commons.lang3.StringUtils;

public class ValidationResult {

    public static ValidationResult OK = new ValidationResult(true, null, null, StringUtils.EMPTY);

    private final boolean ok;
    private final String errorCode;
    private final ValidatableField field;
    private final String message;

    // -------------------- CONSTRUCTORS --------------------

    private ValidationResult(boolean ok, String errorCode, ValidatableField field, String validationMessage) {
        this.ok = ok;
        this.errorCode = errorCode;
        this.field = field;
        this.message = validationMessage;
    }

    // -------------------- GETTERS --------------------

    public ValidatableField getField() {
        return field;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    // -------------------- LOGIC --------------------

    public ValidationResult withInfo(ValidatableField field, String validationMessage) {
        return new ValidationResult(ok, errorCode, field, validationMessage);
    }

    public static ValidationResult ok() {
        return OK;
    }

    public static ValidationResult invalid(ValidatableField field, String message) {
        return new ValidationResult(false, null, field, message);
    }

    public static ValidationResult invalid(String errorCode) {
        return new ValidationResult(false, errorCode, null, StringUtils.EMPTY);
    }

    public boolean isOk() {
        return ok;
    }
}
