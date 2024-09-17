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
 * Validates that a field that depends on another field has a value.
 * It works on pair of fields. If one of them is filled, then the other one is required.
 * It can work in one direction and in both directions.
 * F.e. "OriginTitle" field depends on "OtherTitle" field.
 * If "OtherTitle" field is filled, then "OriginTitle" field is required.
 * It can also be opposite to it:
 * F.e. "OtherTitle" field depends on "OriginTitle" field. If "OriginTitle" field
 * is filled, then "OtherTitle" field is required.
 * To use this validator you need to set up `dependantField` parameter in your
 * DatasetFieldType validator configuration
 * <pre>
 * F.e. `{"name":"required_dependant","parameters":{"context":["DATASET"], "dependantField": "OriginTitle"}}`
 * F.e. `{"name":"required_dependant","parameters":{"context":["DATASET"], "dependantField": "OtherTitle"}}`
 * </pre>
 * @see edu.harvard.iq.dataverse.persistence.dataset.ValidatableField
 * @see edu.harvard.iq.dataverse.persistence.dataset.DatasetField
 * @see edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType#getValidation()
 *
 * @author Filipe Dias Lewandowski, Krzysztof Mądry, Daniel Korbel, Sylwester Niewczas
 */
@Eager
@ApplicationScoped
public class RequiredDependantFieldValidator extends DependantFieldValidator {

    // -------------------- LOGIC --------------------

    @Override
    public String getName() {
        return "required_dependant";
    }

    @Override
    protected FieldValidationResult validateWithDependantField(
            ValidatableField field,
            ValidatableField dependantField,
            Map<String, Object> params,
            Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {

        if (dependantField.getValidatableValues().stream().noneMatch(StringUtils::isNotBlank)) {
            return FieldValidationResult.invalid(dependantField, BundleUtil.getStringFromBundle("isrequired",
                    dependantField.getDatasetFieldType().getDisplayName()));
        }
        return FieldValidationResult.ok();
    }
}
