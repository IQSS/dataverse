package edu.harvard.iq.dataverse.validation.field.validators;

import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * Base class for validating fields with multiple alternative values.
 * The order of values is not important, as the empty/null are discarded.
 */
public abstract class MultiValueValidatorBase extends FieldValidatorBase {

    @Override
    public FieldValidationResult validate(ValidatableField field, Map<String, Object> params, Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {
        for (String value : field.getValidatableValues()) {
            if (StringUtils.isBlank(value)) {
                continue;
            }
            FieldValidationResult result = validateValue(value, field, params, fieldIndex);
            if (!result.isOk()) {
                return result;
            }
        }
        return FieldValidationResult.ok();
    }

    public abstract FieldValidationResult validateValue(String value, ValidatableField field, Map<String, Object> params, Map<String, ? extends List<? extends ValidatableField>> fieldIndex);
}
