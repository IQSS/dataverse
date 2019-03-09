/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 *
 * @author sarahferry
 * @author alexscheitlin
 */
@RunWith(Parameterized.class)
public class UserNameValidatorTest {

    public String userName;
    public boolean isValid;

    public UserNameValidatorTest(String userName, boolean isValid) {
        this.userName = userName;
        this.isValid = isValid;
    }

    @Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][] {
            // good usernames
            { "sarah", true },
            { ".-_5Arah_-.", true },

            // dont allow accents
            { "àèìòùÀÈÌÒÙáéíóúýÁÉÍÓÚÝâêîôûÂÊÎÔÛãñõÃÑÕäëïöüÿÄËÏÖÜŸçÇßØøÅåÆæœ", false },

            // dont allow chinese characters
            { "谁日吧爸好", false },

            // dont allow middle white space
            { "sarah f", false },

            // dont allow leading white space
            { " sarah", false },

            // dont allow trailing white space
            { "sarah ", false },

            // dont allow symbols
            { "sarah!", false },
            { "sarah?", false },
            { "sarah:(", false },
            { "💲🅰️®️🅰️🚧", false },

            // only allow between 2 and 60 characters
            { "q", false },
            { "q2", true },
            { "q2jsalfhjopiwurtiosfhkdhasjkdhfgkfhkfrhnefcn4cqonroclmooi4oiqwhrfq4jrlqhaskdalwehrlwhflhklasdjfglq0kkajfelirhilwhakjgv", false },
            { "", false },
            { null, false }
        });
    }

    @Test
    public void testIsUserNameValid() {
        assertEquals(UserNameValidator.isUserNameValid(userName, null), isValid);
    }
}
