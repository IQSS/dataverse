/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 *
 * @author skraffmi
 */
public class FileDirectoryNameValidator implements ConstraintValidator<ValidateDataFileDirectoryName, String> {

    @Override
    public void initialize(ValidateDataFileDirectoryName constraintAnnotation) {
        
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return isFileDirectoryNameValid(value, context);
        
    }
    
    public static boolean isFileDirectoryNameValid(String value, ConstraintValidatorContext context) {

        if (value == null || value.isEmpty()) {
            return true;
        }
        String validCharacters = "[\\w\\\\/. -]+";
        Pattern p = Pattern.compile(validCharacters);
        Matcher m = p.matcher(value);
        return m.matches();

    }
    
}
