package edu.harvard.iq.dataverse.persistence.config;

import edu.harvard.iq.dataverse.common.BundleUtil;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.net.MalformedURLException;
import java.net.URL;

/**
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
        try {
            URL url = new URL(value);
        } catch (MalformedURLException e) {
            return false;
        }
        return true;
    }

}
