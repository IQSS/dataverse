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

/**
 *
 * @author sarahferry
 * Modeled after PasswordValidator and EMailValidator
 */

public class UserNameValidator implements ConstraintValidator<ValidateUserName, String> {
    @Override
    public void initialize(ValidateUserName constraintAnnotation) {

    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return isUserNameValid(value, context);
    }

    /**
     * Here we will validate the username
     *
     * @param username
     * @return boolean 
     */
    public static boolean isUserNameValid(final String username, ConstraintValidatorContext context) {
        //TODO: What other characters do we need to support?
        String validCharacters = "[a-zA-ZÀ-ÿ0-9\\_\\-\\.";
        //support accents
        validCharacters += "\\u00C0-\\u017F";
        //support chinese characters
        validCharacters += "\\x{4e00}-\\x{9fa5}";
        //end
        validCharacters += "]*";
        Pattern p = Pattern.compile(validCharacters);
        Matcher m = p.matcher(username);
        return m.matches();
    }

}



