package edu.harvard.iq.dataverse.validation.field.validators;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import org.apache.commons.lang3.StringUtils;
import org.omnifaces.cdi.Eager;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;

/**
 * Validates that a field that depends on another field value has a value.
 * F.e.: "CustomerType" can be "Individual" or "Company".
 * If "CustomerType" is "Company" then "Company Name" field is required.
 * If "CustomerType" is "Individual" then "First Name" and "Last Name" fields
 * are required.
 * For usage, you need to declare validator on each dependant field, like:
 * "First Name", "Last Name", "Company Name" as follows:
 * <pre>
 *  [{"name":"required_by_value_dependant","parameters":{"context":["DATASET"], "runOnEmpty":"true", "dependantField": "customerType", "dependantFieldValue": "individual"}}]
 *  [{"name":"required_by_value_dependant","parameters":{"context":["DATASET"], "runOnEmpty":"true", "dependantField": "customerType", "dependantFieldValue": "company"}}]
 * </pre>
 *
 * @see edu.harvard.iq.dataverse.persistence.dataset.ValidatableField
 * @see edu.harvard.iq.dataverse.persistence.dataset.DatasetField
 * @see edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType#getValidation()
 *
 * @author Filipe Dias Lewandowski, Krzysztof Mądry, Daniel Korbel, Sylwester Niewczas
 */
@Eager
@ApplicationScoped
public class RequiredByValueDependantFieldValidator extends DependantFieldValidator {
    public static final String DEPENDANT_FIELD_VALUE_PARAM = "dependantFieldValue";

    // -------------------- LOGIC --------------------

    @Override
    public String getName() {
        return "required_by_value_dependant";
    }

    @Override
    protected FieldValidationResult validateWithDependantField(
            ValidatableField field,
            ValidatableField dependantField,
            Map<String, Object> params,
            Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {

        if (dependantField.getValidatableValues().stream().anyMatch(actualValue ->
                StringUtils.equalsIgnoreCase(actualValue, (String) params.get(DEPENDANT_FIELD_VALUE_PARAM)))) {
            if (field.getValidatableValues().stream().noneMatch(StringUtils::isNotBlank)) {
                return FieldValidationResult.invalid(field, BundleUtil.getStringFromBundle("isrequired",
                        field.getDatasetFieldType().getDisplayName()));
            }
            return FieldValidationResult.ok();
        }
        return FieldValidationResult.ok();
    }
}
