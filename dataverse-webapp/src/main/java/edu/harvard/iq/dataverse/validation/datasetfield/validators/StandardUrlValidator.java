package edu.harvard.iq.dataverse.validation.datasetfield.validators;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.validation.datasetfield.ValidationResult;
import org.omnifaces.cdi.Eager;

import javax.enterprise.context.ApplicationScoped;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

@Eager
@ApplicationScoped
public class StandardUrlValidator extends FieldValidatorBase {

    @Override
    public String getName() {
        return "standard_url";
    }

    @Override
    public ValidationResult isValid(DatasetField field, Map<String, String> params, Map<String, List<DatasetField>> fieldIndex) {
        try {
            new URL(field.getValue());
            return ValidationResult.ok();
        } catch (MalformedURLException e) {
            return ValidationResult.invalid(field, BundleUtil.getStringFromBundle("isNotValidUrl",
                    field.getDatasetFieldType().getDisplayName(), field.getValue()));
        }
    }
}
