package edu.harvard.iq.dataverse.validation.field.validators;

import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import io.vavr.control.Option;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Base class for author identifier validators. Implementing validators are expected to be configured with
 * an `authorIdentifierScheme` parameter indicating for which type of identifier it should be
 * applied. Validation is omitted for other types.
 */
public abstract class AuthorIdentifierValidator extends MultiValueValidatorBase {

    private static final Logger logger = Logger.getLogger(AuthorIdentifierValidator.class.getCanonicalName());

    public static final String IDENTIFIER_SCHEME_PARAM = "authorIdentifierScheme";

    // -------------------- LOGIC --------------------

    @Override
    public FieldValidationResult validateValue(String value, ValidatableField field, Map<String, Object> params, Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {
        if (isSelectedIdentifierScheme(field, params)) {
            return validateIdentifier(value, field, params, fieldIndex);
        }
        return FieldValidationResult.ok();
    }

    protected abstract FieldValidationResult validateIdentifier(String identifier, ValidatableField field, Map<String, Object> params, Map<String, ? extends List<? extends ValidatableField>> fieldIndex);

    // -------------------- PRIVATE ---------------------

    private static boolean isSelectedIdentifierScheme(ValidatableField field, Map<String, Object> params) {
        return field.getParent()
                .flatMap(f -> Option.ofOptional(f.getChildren().stream()
                        .filter(c -> c.getDatasetFieldType().getName().equals(DatasetFieldConstant.authorIdType))
                        .findFirst()))
                .map(ValidatableField::getSingleValue)
                .filter(type -> type.equals(params.get(IDENTIFIER_SCHEME_PARAM)))
                .isDefined();
    }
}
