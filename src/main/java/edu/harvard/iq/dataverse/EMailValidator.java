/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author skraffmi
 */
public class EMailValidator implements ConstraintValidator<ValidateEmail, String> {

    @Override
    public void initialize(ValidateEmail constraintAnnotation) {

    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        
        return isEmailValid(value, context);

        
    }
    
    public static boolean isEmailValid(String value, ConstraintValidatorContext context){
          Pattern p =  Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");
            
            Matcher m = p.matcher(value.trim());
            boolean matchFound = m.matches();
            if (!matchFound) {
                context.buildConstraintViolationWithTemplate( value + " is not a valid email address.").addConstraintViolation();
                return false;
            }
        
        
        return true;
        
    }
}
