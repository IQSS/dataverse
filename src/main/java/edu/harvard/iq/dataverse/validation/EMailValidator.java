package edu.harvard.iq.dataverse.validation;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.apache.commons.validator.routines.EmailValidator;

import java.nio.charset.StandardCharsets;

/**
 *
 * @author skraffmi
 */
public class EMailValidator implements ConstraintValidator<ValidateEmail, String> {

    private static final boolean MTA_SUPPORTS_UTF8 = JvmSettings.MAIL_MTA_SUPPORT_UTF8.lookup(Boolean.class);
    
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
        return value == null || (EmailValidator.getInstance().isValid(value) &&
            // If the MTA isn't able to handle UTF-8 mail addresses following RFC 6530/6531/6532, we can only declare
            // mail addresses using 7bit ASCII (RFC 821) as valid.
            // Beyond scope for Apache Commons Validator, see also https://issues.apache.org/jira/browse/VALIDATOR-487
            (StandardCharsets.US_ASCII.newEncoder().canEncode(value) || MTA_SUPPORTS_UTF8) );
    }
}
