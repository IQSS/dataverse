package edu.harvard.iq.dataverse.validation.field.validators.geobox;

import edu.harvard.iq.dataverse.validation.field.FieldValidator;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import edu.harvard.iq.dataverse.validation.field.validators.FieldValidatorBase;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import org.omnifaces.cdi.Eager;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;

@Eager
@ApplicationScoped
public class GeoboxComponentValidator extends FieldValidatorBase {

    // The order of validators is important!
    private static FieldValidator[] INTERNAL_VALIDATORS = new FieldValidator[] {
            new GeoboxFillValidator(),
            new GeoboxValueValidator(),
            new GeoboxLatitudeRelationValidator()
    };

    // -------------------- LOGIC --------------------

    @Override
    public String getName() {
        return "geobox_component_validator";
    }

    @Override
    public FieldValidationResult validate(ValidatableField field, Map<String, Object> params, Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {
        for (FieldValidator validator : INTERNAL_VALIDATORS) {
            FieldValidationResult intermediateResult = validator.validate(field, params, fieldIndex);
            if (!intermediateResult.isOk()) {
                return intermediateResult;
            }
        }
        return FieldValidationResult.ok();
    }
}
