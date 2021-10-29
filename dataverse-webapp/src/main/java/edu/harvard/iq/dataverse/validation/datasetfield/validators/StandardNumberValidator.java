package edu.harvard.iq.dataverse.validation.datasetfield.validators;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.validation.datasetfield.ValidationResult;
import org.omnifaces.cdi.Eager;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;

@Eager
@ApplicationScoped
public class StandardNumberValidator extends FieldValidatorBase {

    @Override
    public String getName() {
        return "standard_number";
    }

    @Override
    public ValidationResult isValid(DatasetField field, Map<String, String> params, Map<String, List<DatasetField>> fieldIndex) {
        try {
            Double.parseDouble(field.getValue());
            return ValidationResult.ok();
        } catch (NumberFormatException nfe) {
            return ValidationResult.invalid(field, BundleUtil.getStringFromBundle("isNotValidNumber",
                    field.getDatasetFieldType().getDisplayName()));
        }
    }
}
