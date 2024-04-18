package edu.harvard.iq.dataverse.validation.field.validators;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.RorValidator;
import edu.harvard.iq.dataverse.validation.ValidationResult;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import org.omnifaces.cdi.Eager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;

@Eager
@ApplicationScoped
public class RorFieldValidator extends MultiValueValidatorBase {

    @Inject
    private RorValidator rorValidator;

    // -------------------- LOGIC --------------------

    @Override
    public String getName() {
        return "ror_validator";
    }

    @Override
    public FieldValidationResult validateValue(String fullRor, ValidatableField field, Map<String, Object> params,
                                               Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {
        String fieldName = field.getDatasetFieldType().getDisplayName();
        ValidationResult result = rorValidator.validate(fullRor);
        if (result.isOk()) {
            return FieldValidationResult.ok();
        } else {
            return FieldValidationResult.invalid(field, BundleUtil.getStringFromBundle(result.getErrorCode(), fieldName));
        }
    }
}
