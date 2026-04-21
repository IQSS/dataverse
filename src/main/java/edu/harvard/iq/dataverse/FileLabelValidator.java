package edu.harvard.iq.dataverse;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileLabelValidator implements ConstraintValidator<ValidateDataFileLabel, String> {

    @Override
    public void initialize(ValidateDataFileLabel constraintAnnotation) {
        
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return isFileLabelValid(value, context);
        
    }
    
    public static boolean isFileLabelValid(String value, ConstraintValidatorContext context) {

        if (value == null || value.isEmpty()) {
            return true;
        }
        String validCharacters = "^[^:<>;#/\"\\*\\|\\?\\\\]*$";
        Pattern p = Pattern.compile(validCharacters);
        Matcher m = p.matcher(value);
        return m.matches();

    }
    
}
