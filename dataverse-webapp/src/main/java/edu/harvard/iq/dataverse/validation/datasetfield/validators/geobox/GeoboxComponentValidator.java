package edu.harvard.iq.dataverse.validation.datasetfield.validators.geobox;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.validation.datasetfield.FieldValidator;
import edu.harvard.iq.dataverse.validation.datasetfield.ValidationResult;
import edu.harvard.iq.dataverse.validation.datasetfield.validators.FieldValidatorBase;
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
    public ValidationResult isValid(DatasetField field, Map<String, String> params, Map<String, List<DatasetField>> fieldIndex) {
        for (FieldValidator validator : INTERNAL_VALIDATORS) {
            ValidationResult intermediateResult = validator.isValid(field, params, fieldIndex);
            if (!intermediateResult.isOk()) {
                return intermediateResult;
            }
        }
        return ValidationResult.ok();
    }
}
