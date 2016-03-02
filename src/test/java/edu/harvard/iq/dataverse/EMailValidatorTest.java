package edu.harvard.iq.dataverse;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class EMailValidatorTest {

    @Test
    public void testIsEmailValid() {
        assertEquals(true, EMailValidator.isEmailValid("pete@mailinator.com", null));
        assertEquals(false, EMailValidator.isEmailValid("pete1@mailinator.com;pete2@mailinator.com", null));
        assertEquals(false, EMailValidator.isEmailValid("", null));
        /**
         * @todo How can null as an email address be valid?!?
         */
        assertEquals(true, EMailValidator.isEmailValid(null, null));
    }

}
