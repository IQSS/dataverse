package edu.harvard.iq.dataverse.validation;
import edu.harvard.iq.dataverse.util.BundleUtil;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.apache.commons.validator.routines.UrlValidator;

/**
 *
 * @author skraffmi
 */
public class URLValidator implements ConstraintValidator<ValidateURL, String> {

    private String[] allowedSchemes;
    @Override
    public void initialize(ValidateURL constraintAnnotation) {
        this.allowedSchemes = constraintAnnotation.schemes();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return isURLValid(value, this.allowedSchemes);
    }
    
    /**
     * Check if a URL is valid in a nullsafe way. (null = valid to allow optional values).
     * Empty values are no valid URLs. This variant allows default schemes HTTP, HTTPS and FTP.
     *
     * @param value The URL to validate
     * @return true when valid (null is also valid) or false
     */
    public static boolean isURLValid(String value) {
        // default schemes == ValidateURL schemes() default
        return isURLValid(value, new String[]{"http", "https", "ftp"});
    }
    
    /**
     * Check if a URL is valid in a nullsafe way. (null = valid to allow optional values).
     * Empty values are no valid URLs. This variant allows any schemes you hand over.
     *
     * @param value The URL to validate
     * @param schemes The list of allowed schemes
     * @return true when valid (null is also valid) or false
     */
    public static boolean isURLValid(String value, String[] schemes) {
        UrlValidator urlValidator = new UrlValidator(schemes);
        return value == null || urlValidator.isValid(value);
    }


}
