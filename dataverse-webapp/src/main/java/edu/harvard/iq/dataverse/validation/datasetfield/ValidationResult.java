package edu.harvard.iq.dataverse.validation.datasetfield;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import org.apache.commons.lang3.StringUtils;

public class ValidationResult {
    public static ValidationResult OK = new ValidationResult(true,null, StringUtils.EMPTY);

    private boolean ok;
    private DatasetField field;
    private String message;

    // -------------------- CONSTRUCTORS --------------------

    private ValidationResult(boolean ok, DatasetField field, String validationMessage) {
        this.ok = ok;
        this.field = field;
        this.message = validationMessage;
    }

    // -------------------- GETTERS --------------------

    public DatasetField getField() {
        return field;
    }

    public String getMessage() {
        return message;
    }

    // -------------------- LOGIC --------------------

    public static ValidationResult ok() {
        return OK;
    }

    public static ValidationResult invalid(DatasetField field, String message) {
        return new ValidationResult(false, field, message);
    }

    public boolean isOk() {
        return ok;
    }
}
