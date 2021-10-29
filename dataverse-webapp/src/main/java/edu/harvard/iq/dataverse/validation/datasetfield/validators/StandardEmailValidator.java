package edu.harvard.iq.dataverse.validation.datasetfield.validators;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.validation.datasetfield.ValidationResult;
import org.apache.commons.validator.routines.EmailValidator;
import org.omnifaces.cdi.Eager;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;

@Eager
@ApplicationScoped
public class StandardEmailValidator extends FieldValidatorBase {

    @Override
    public String getName() {
        return "standard_email";
    }

    @Override
    public ValidationResult isValid(DatasetField field, Map<String, String> params, Map<String, List<DatasetField>> fieldIndex) {
        return EmailValidator.getInstance().isValid(field.getValue())
                ? ValidationResult.ok()
                : ValidationResult.invalid(field,
                String.format("%s %s", field.getValue(), BundleUtil.getStringFromBundle("email.invalid")));
    }
}
