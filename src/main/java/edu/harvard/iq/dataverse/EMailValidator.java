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
            //we'll let someone else decide if it's required
            return true;
        }
        boolean isValid = EmailValidator.getInstance().isValid(value.trim());
        if (!isValid) {
            context.buildConstraintViolationWithTemplate(java.text.MessageFormat.format("emailvalidator.notValid", value)).addConstraintViolation();
            return false;
        }
        return true;
    }
}
