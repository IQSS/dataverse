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
        //allow accents
        assertEquals(true, UserNameValidator.isUserNameValid("àèìòùÀÈÌÒÙáéíóúýÁÉÍÓÚÝâêîôûÂÊÎÔÛãñõÃÑÕäëïöüÿÄËÏÖÜŸçÇßØøÅåÆæœ", null));
        //allow chinese characters
        assertEquals(true, UserNameValidator.isUserNameValid("谁日吧爸好", null));
        
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
        
    }
}
