package edu.harvard.iq.dataverse.validation.field;

import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.ValidationResult;
import org.apache.commons.lang3.StringUtils;

public class FieldValidationResult extends ValidationResult {

    public static FieldValidationResult OK = new FieldValidationResult(true, null, null, StringUtils.EMPTY);

    private final ValidatableField field;
    private final String message;

    // -------------------- CONSTRUCTORS --------------------

    private FieldValidationResult(boolean ok, String errorCode, ValidatableField field, String validationMessage) {
        super(ok, errorCode);
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

    // -------------------- LOGIC --------------------

    public static FieldValidationResult ok() {
        return OK;
    }

    public static FieldValidationResult invalid(ValidatableField field, String message) {
        return new FieldValidationResult(false, null, field, message);
    }
}
