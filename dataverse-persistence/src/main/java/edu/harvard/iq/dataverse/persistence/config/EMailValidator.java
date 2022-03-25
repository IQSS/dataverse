package edu.harvard.iq.dataverse.persistence.config;

import edu.harvard.iq.dataverse.common.BundleUtil;
import org.apache.commons.validator.routines.EmailValidator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @author skraffmi
 */
public class EMailValidator implements ConstraintValidator<ValidateEmail, String> {

    // -------------------- LOGIC --------------------

    @Override
    public void initialize(ValidateEmail constraintAnnotation) { }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return isEmailValid(value, context);
    }

    public static boolean isEmailValid(String value, ConstraintValidatorContext context) {
        boolean isValid = EmailValidator.getInstance().isValid(value);
        if (!isValid && context != null) {
            context.buildConstraintViolationWithTemplate(value + "  "
                    + BundleUtil.getStringFromBundle("email.invalid"))
                    .addConstraintViolation();
        }
        return isValid;
    }
}
