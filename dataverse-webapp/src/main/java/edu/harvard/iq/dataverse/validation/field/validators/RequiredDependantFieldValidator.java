package edu.harvard.iq.dataverse.validation.field.validators;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import org.apache.commons.lang3.StringUtils;
import org.omnifaces.cdi.Eager;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;

@Eager
@ApplicationScoped
public class RequiredDependantFieldValidator extends DependantFieldValidator {

    // -------------------- LOGIC --------------------

    @Override
    public String getName() {
        return "required_dependant";
    }

    @Override
    protected FieldValidationResult validateWithDependantField(ValidatableField field, ValidatableField dependantField, Map<String, Object> params, Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {
        if (dependantField.getValidatableValues().stream().noneMatch(StringUtils::isNotBlank)) {
            return FieldValidationResult.invalid(dependantField, BundleUtil.getStringFromBundle("isrequired",
                    dependantField.getDatasetFieldType().getDisplayName()));
        }
        return FieldValidationResult.ok();
    }
}
