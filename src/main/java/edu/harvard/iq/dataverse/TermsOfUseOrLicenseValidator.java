package edu.harvard.iq.dataverse;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TermsOfUseOrLicenseValidator implements ConstraintValidator<ValidateTermsOfUseOrLicense, TermsOfUseOrLicense> {

    @Override
    public void initialize(ValidateTermsOfUseOrLicense constraintAnnotation) {

    }

    @Override
    public boolean isValid(TermsOfUseOrLicense value, ConstraintValidatorContext context) {

        return isTOUOLValid(value, context);

    }

    public static boolean isTOUOLValid(TermsOfUseOrLicense value, ConstraintValidatorContext context) {
        // we only need to validate file specific terms of use or license
        // TODO forgot why
        if (value.getTemplate() != null || value.getDatasetVersion() != null){
            return true;
        }
        if (value.getLicense() != null) {
            if (value.getTermsOfUse() != null && !value.getTermsOfUse().isEmpty()) {
                if (context != null) {
                    context.buildConstraintViolationWithTemplate("Terms of use and license cannot both be set.").addConstraintViolation();
                }
                return false;
            }
            // TODO no other values too
        } else {
            if (value.getTermsOfUse() == null || value.getTermsOfUse().isEmpty()) {
                if (context != null) {
                    context.buildConstraintViolationWithTemplate("Either terms of use or license must be set.").addConstraintViolation();
                }
                return false;
            }
            // TODO get the file based terms of use of existing/other files
        }
        return true;
    }
}
