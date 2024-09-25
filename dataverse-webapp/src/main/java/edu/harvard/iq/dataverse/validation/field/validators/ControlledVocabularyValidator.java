package edu.harvard.iq.dataverse.validation.field.validators;

import edu.harvard.iq.dataverse.ControlledVocabularyValueServiceBean;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import org.omnifaces.cdi.Eager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 * Validator for fields that should only contain values from a controlled vocabulary.
 * The vocabulary is passed in through the parameters.
 */
@Eager
@ApplicationScoped
public class ControlledVocabularyValidator extends MultiValueValidatorBase {

    @Inject
    private ControlledVocabularyValueServiceBean vocabularyDao;

    // -------------------- LOGIC --------------------

    @Override
    public String getName() {
        return "controlled_vocabulary_validator";
    }

    @Override
    public FieldValidationResult validateValue
            (String value,
             ValidatableField field,
             Map<String, Object> params,
             Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {

        List<ControlledVocabularyValue> matchesFromDb =
                vocabularyDao.findByDatasetFieldTypeNameAndValueLike(
                field.getDatasetFieldType().getName(), value, 1
        );
        if(matchesFromDb.size() == 1 && matchesFromDb.get(0).getStrValue().equalsIgnoreCase(value)) {
            return FieldValidationResult.ok();
        } else {
            return FieldValidationResult.invalid(
                    field,
                    BundleUtil.getStringFromBundle("validation.value.not.allowed.in.controlled.vocabulary", value)
            );
        }
    }
}