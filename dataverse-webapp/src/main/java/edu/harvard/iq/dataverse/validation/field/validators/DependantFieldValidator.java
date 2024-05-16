package edu.harvard.iq.dataverse.validation.field.validators;

import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import io.vavr.control.Option;

import java.util.List;
import java.util.Map;

/**
 * Base class for validators on dependant fields. Implementing validators are expected to be configured with
 * an `dependantField` parameter indicating for which field it should be applied.
 */
public abstract class DependantFieldValidator extends FieldValidatorBase {

    public static final String DEPENDANT_FIELD_PARAM = "dependantField";

    // -------------------- LOGIC --------------------

    @Override
    public FieldValidationResult validate(ValidatableField field, Map<String, Object> params,
                                   Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {

        Object dependantTypeName = params.get(DEPENDANT_FIELD_PARAM);
        if (!(dependantTypeName instanceof String)) {
            return FieldValidationResult.ok();
        }

        return findDependantField(field, (String) dependantTypeName)
                .map(dependantField -> validateWithDependantField(field, dependantField, params, fieldIndex))
                .getOrElse(FieldValidationResult.ok());
    }

    protected abstract FieldValidationResult validateWithDependantField(ValidatableField field, ValidatableField dependantField, Map<String, Object> params,
                                                                        Map<String, ? extends List<? extends ValidatableField>> fieldIndex);

    // -------------------- PRIVATE ---------------------

    private static Option<ValidatableField> findDependantField(ValidatableField field, String dependantTypeName) {
        Option<? extends ValidatableField> parent = field.getParent();
        if (parent.isEmpty()) {
            return Option.none();
        }

        Option<? extends ValidatableField> dependantType = parent.flatMap(p -> Option.ofOptional(p.getChildren().stream()
                .filter(c -> c.getDatasetFieldType().getName().equals(dependantTypeName))
                .findFirst()));

        if (dependantType.isEmpty()) {
            return findDependantField(parent.get(), dependantTypeName);
        }

        return Option.narrow(dependantType);
    }
}
