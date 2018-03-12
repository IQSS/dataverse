/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author sarahferry
 */
public class UserNameValidatorTest {
    
    @Test
    public void testIsUserNameValid() {
        //good usernames
        assertEquals(true, UserNameValidator.isUserNameValid("sarah", null));
        assertEquals(true, UserNameValidator.isUserNameValid(".-_5Arah_-.", null));
        //dont allow accents
        assertEquals(false, UserNameValidator.isUserNameValid("àèìòùÀÈÌÒÙáéíóúýÁÉÍÓÚÝâêîôûÂÊÎÔÛãñõÃÑÕäëïöüÿÄËÏÖÜŸçÇßØøÅåÆæœ", null));
        //dont allow chinese characters
        assertEquals(false, UserNameValidator.isUserNameValid("谁日吧爸好", null));
        
        //middle white space
        assertEquals(false, UserNameValidator.isUserNameValid("sarah f", null));
        //leading white space
        assertEquals(false, UserNameValidator.isUserNameValid(" sarah", null));
        //trailing white space
        assertEquals(false, UserNameValidator.isUserNameValid("sarah ", null));
        
        //symbols!
        assertEquals(false, UserNameValidator.isUserNameValid("sarah!", null));
        assertEquals(false, UserNameValidator.isUserNameValid("sarah?", null));
        assertEquals(false, UserNameValidator.isUserNameValid("sarah:(", null));
        assertEquals(false, UserNameValidator.isUserNameValid("💲🅰️®️🅰️🚧", null));
        
        //length of userame
        assertEquals(false, UserNameValidator.isUserNameValid("q", null));
        assertEquals(true, UserNameValidator.isUserNameValid("q2", null));
        assertEquals(false, UserNameValidator.isUserNameValid("q2jsalfhjopiwurtiosfhkdhasjkdhfgkfhkfrhnefcn4cqonroclmooi4oiqwhrfq4jrlqhaskdalwehrlwhflhklasdjfglq0kkajfelirhilwhakjgv", null));
        assertEquals(false, UserNameValidator.isUserNameValid("", null));
        assertEquals(false, UserNameValidator.isUserNameValid(null, null));
    }
}
