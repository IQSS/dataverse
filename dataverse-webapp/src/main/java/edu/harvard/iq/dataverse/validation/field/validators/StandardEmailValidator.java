package edu.harvard.iq.dataverse.validation.field.validators;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.field.ValidationResult;
import org.apache.commons.validator.routines.EmailValidator;
import org.omnifaces.cdi.Eager;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;

@Eager
@ApplicationScoped
public class StandardEmailValidator extends MultiValueValidatorBase {

    @Override
    public String getName() {
        return "standard_email";
    }

    @Override
    public ValidationResult validateValue(String value, ValidatableField field, Map<String, Object> params,
                                          Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {
        return EmailValidator.getInstance().isValid(value)
                ? ValidationResult.ok()
                : ValidationResult.invalid(field,
                String.format("%s %s", value, BundleUtil.getStringFromBundle("email.invalid")));
    }
}
