package edu.harvard.iq.dataverse.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.apache.commons.validator.routines.EmailValidator;

/**
 *
 * @author skraffmi
 */
public class EMailValidator implements ConstraintValidator<ValidateEmail, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return isEmailValid(value);
    }
    
    /**
     * Validate an email address in a null safe way (null = valid).
     * (Null is valid to allow for optional values - use @NotNull to enforce!)
     * @param value The email address to validate
     * @return true when valid, false when invalid (null = valid!)
     */
    public static boolean isEmailValid(String value) {
        return value == null || EmailValidator.getInstance().isValid(value);
    }
}
