/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.persistence.user;

import edu.harvard.iq.dataverse.persistence.user.UserNameValidator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * @author sarahferry
 * @author alexscheitlin
 */
@RunWith(Parameterized.class)
public class UserNameValidatorTest {

    public boolean isValid;
    public String userName;

    public UserNameValidatorTest(boolean isValid, String userName) {
        this.isValid = isValid;
        this.userName = userName;
    }

    @Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                // good usernames
                {true, "sarah"},
                {true, ".-_5Arah_-."},

                // dont allow accents
                {false, "àèìòùÀÈÌÒÙáéíóúýÁÉÍÓÚÝâêîôûÂÊÎÔÛãñõÃÑÕäëïöüÿÄËÏÖÜŸçÇßØøÅåÆæœ"},

                // dont allow chinese characters
                {false, "谁日吧爸好"},

                // dont allow middle white space
                {false, "sarah f"},

                // dont allow leading white space
                {false, " sarah"},

                // dont allow trailing white space
                {false, "sarah "},

                // dont allow symbols
                {false, "sarah!"},
                {false, "sarah?"},
                {false, "sarah:("},
                {false, "💲🅰️®️🅰️🚧"},

                // only allow between 2 and 60 characters
                {false, "q"},
                {true, "q2"},
                {false, "q2jsalfhjopiwurtiosfhkdhasjkdhfgkfhkfrhnefcn4cqonroclmooi4oiqwhrfq4jrlqhaskdalwehrlwhflhklasdjfglq0kkajfelirhilwhakjgv"},
                {false, ""},
                {false, null}
        });
    }

    @Test
    public void testIsUserNameValid() {
        assertEquals(isValid, UserNameValidator.isUserNameValid(userName, null));
    }
}
