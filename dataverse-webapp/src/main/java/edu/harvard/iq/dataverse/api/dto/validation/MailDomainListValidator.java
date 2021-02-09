package edu.harvard.iq.dataverse.api.dto.validation;

import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MailDomainListValidator implements ConstraintValidator<ValidMainDomainList, List<?>> {

    private static final Pattern VALID_DOMAIN_PATTERN = Pattern.compile("[a-zA-Z0-9.-]*");

    // -------------------- LOGIC --------------------

    @Override
    public void initialize(ValidMainDomainList constraintAnnotation) { }

    @Override
    public boolean isValid(List<?> value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        for (Object item : value) {
            if (item == null) {
                return false;
            }
            String stringValue = item.toString();
            Matcher matcher = VALID_DOMAIN_PATTERN.matcher(stringValue);
            if (StringUtils.isBlank(stringValue) || !matcher.matches()) {
                return false;
            }
        }
        return true;
    }
}
