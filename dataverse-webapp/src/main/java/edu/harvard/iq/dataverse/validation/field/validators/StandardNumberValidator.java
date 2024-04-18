package edu.harvard.iq.dataverse.validation.field.validators;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
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
    public FieldValidationResult validateValue(String value, ValidatableField field, Map<String, Object> params,
                                               Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {
        try {
            Double.parseDouble(value);
            return FieldValidationResult.ok();
        } catch (NumberFormatException nfe) {
            return FieldValidationResult.invalid(field, BundleUtil.getStringFromBundle("isNotValidNumber",
                    field.getDatasetFieldType().getDisplayName()));
        }
    }
}
