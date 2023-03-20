package edu.harvard.iq.dataverse.validation.field.validators;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.field.ValidationResult;
import org.omnifaces.cdi.Eager;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;

@Eager
@ApplicationScoped
public class StandardNumberValidator extends MultiValueValidatorBase {

    @Override
    public String getName() {
        return "standard_number";
    }

    @Override
    public ValidationResult validateValue(String value, ValidatableField field, Map<String, Object> params,
                                          Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {
        try {
            Double.parseDouble(value);
            return ValidationResult.ok();
        } catch (NumberFormatException nfe) {
            return ValidationResult.invalid(field, BundleUtil.getStringFromBundle("isNotValidNumber",
                    field.getDatasetFieldType().getDisplayName()));
        }
    }
}
