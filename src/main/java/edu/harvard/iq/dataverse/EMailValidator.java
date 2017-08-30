/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.apache.commons.validator.routines.EmailValidator;

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
    
    public static boolean isEmailValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            //A null email id is not valid, as email is a required field.
            return false;
        }
        boolean isValid = EmailValidator.getInstance().isValid(value);
        if (!isValid) {
            if (context != null) {
                context.buildConstraintViolationWithTemplate(value + " is not a valid email address.").addConstraintViolation();
            }
            return false;
        }
        return true;
    }
}
