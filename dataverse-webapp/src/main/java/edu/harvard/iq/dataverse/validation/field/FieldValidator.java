package edu.harvard.iq.dataverse.validation.field;

import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;

import java.util.List;
import java.util.Map;

public interface FieldValidator {

    String getName();

    ValidationResult isValid(ValidatableField field, Map<String, Object> params,
                             Map<String, ? extends List<? extends ValidatableField>> fieldIndex);

}