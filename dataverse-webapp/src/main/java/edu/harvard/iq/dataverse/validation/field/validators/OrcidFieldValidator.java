package edu.harvard.iq.dataverse.validation.field.validators;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.OrcidValidator;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import edu.harvard.iq.dataverse.validation.ValidationResult;
import org.omnifaces.cdi.Eager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;

@Eager
@ApplicationScoped
public class OrcidFieldValidator extends AuthorIdentifierValidator {

    @Inject
    private OrcidValidator orcidValidator;

    // -------------------- LOGIC --------------------

    @Override
    public String getName() {
        return "orcid_validator";
    }

    @Override
    protected FieldValidationResult validateIdentifier(String orcid, ValidatableField field, Map<String, Object> params, Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {
        String fieldName = field.getDatasetFieldType().getDisplayName();
        ValidationResult result = orcidValidator.validate(orcid);
        if (result.isOk()) {
            return FieldValidationResult.ok();
        } else {
            return FieldValidationResult.invalid(field, BundleUtil.getStringFromBundle(result.getErrorCode(), fieldName));
        }
    }
}
