package edu.harvard.iq.dataverse;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class EMailValidatorTest {

    @Test
    public void testIsEmailValid() {
        assertEquals(true, EMailValidator.isEmailValid("pete@mailinator.com", null));
        /**
         * @todo How can " leadingWhitespace@mailinator.com" be a valid email
         * address?
         */
        assertEquals(true, EMailValidator.isEmailValid(" leadingWhitespace@mailinator.com", null));
        /**
         * @todo How can "trailingWhitespace@mailinator.com " be a valid email
         * address?
         */
        assertEquals(true, EMailValidator.isEmailValid("trailingWhitespace@mailinator.com ", null));
        assertEquals(false, EMailValidator.isEmailValid("elisah.da mota@example.com", null));
        assertEquals(false, EMailValidator.isEmailValid("pete1@mailinator.com;pete2@mailinator.com", null));
        /**
         * These examples are all from https://randomuser.me and seem to be
         * valid according to
         * http://sphinx.mythic-beasts.com/~pdw/cgi-bin/emailvalidate (except
         * رونیکا.محمدخان@example.com).
         *
         */
        assertEquals(true, EMailValidator.isEmailValid("michélle.pereboom@example.com", null));
        assertEquals(true, EMailValidator.isEmailValid("begüm.vriezen@example.com", null));
        assertEquals(true, EMailValidator.isEmailValid("lótus.gonçalves@example.com", null));
        assertEquals(true, EMailValidator.isEmailValid("lótus.gonçalves@éxample.com", null));
        assertEquals(true, EMailValidator.isEmailValid("begüm.vriezen@example.cologne", null));
        assertEquals(true, EMailValidator.isEmailValid("رونیکا.محمدخان@example.com", null));
        assertEquals(false, EMailValidator.isEmailValid("lótus.gonçalves@example.cóm", null));
        assertEquals(false, EMailValidator.isEmailValid("dora@.com", null));
        assertEquals(false, EMailValidator.isEmailValid("", null));
        assertEquals(false, EMailValidator.isEmailValid(null, null));
        /*
        Add tests for 4601
        */
        assertEquals(true, EMailValidator.isEmailValid("blah@wiso.uni-unc.de", null));
        assertEquals(true, EMailValidator.isEmailValid("foo@essex.co.uk", null));
        assertEquals(true, EMailValidator.isEmailValid("jack@bu.cloud", null));
    }

}
