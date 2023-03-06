package edu.harvard.iq.dataverse.validation.field.validators;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.field.ValidationResult;
import org.omnifaces.cdi.Eager;

import javax.enterprise.context.ApplicationScoped;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

@Eager
@ApplicationScoped
public class StandardUrlValidator extends MultiValueValidatorBase {

    @Override
    public String getName() {
        return "standard_url";
    }

    @Override
    public ValidationResult isValueValid(String value, ValidatableField field, Map<String, Object> params,
                                         Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {
        try {
            new URL(value);
            return ValidationResult.ok();
        } catch (MalformedURLException e) {
            return ValidationResult.invalid(field, BundleUtil.getStringFromBundle("isNotValidUrl",
                    field.getDatasetFieldType().getDisplayName(), value));
        }
    }
}
