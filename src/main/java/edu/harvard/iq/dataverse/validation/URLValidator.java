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

    @Override
    public void initialize(ValidateURL constraintAnnotation) {

    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        boolean valid = isURLValid(value);
        if (context != null && !valid) {
            context.buildConstraintViolationWithTemplate(value + "  " + BundleUtil.getStringFromBundle("url.invalid")).addConstraintViolation();
        }
        return valid;
    }

    public static boolean isURLValid(String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        
        String[] schemes = {"http","https", "ftp"};
        UrlValidator urlValidator = new UrlValidator(schemes);
            
        try {
            if (urlValidator.isValid(value)) {
            } else {
                return false;
            }
        } catch (NullPointerException npe) {
            return false;
        }
        
        return true;
        
    }


}
