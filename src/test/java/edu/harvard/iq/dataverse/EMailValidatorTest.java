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
        boolean issue2998resolved = false;
        /**
         * @todo Evaluate if we should upgrade to commons-validator 1.5 or
         * newer, which seems to allow these non-ASCII email addresses to pass
         * validation.
         *
         * "In addition to the above ASCII characters, international characters
         * above U+007F, encoded as UTF-8, are permitted by RFC 6531, though
         * mail systems may restrict which characters to use when assigning
         * local parts." https://en.wikipedia.org/wiki/Email_address
         *
         * These examples are all from https://randomuser.me and seem to be
         * valid according to
         * http://sphinx.mythic-beasts.com/~pdw/cgi-bin/emailvalidate (except
         * رونیکا.محمدخان@example.com).
         *
         * See https://github.com/IQSS/dataverse/issues/2998
         */
        assertEquals(issue2998resolved, EMailValidator.isEmailValid("michélle.pereboom@example.com", null));
        assertEquals(issue2998resolved, EMailValidator.isEmailValid("begüm.vriezen@example.com", null));
        assertEquals(issue2998resolved, EMailValidator.isEmailValid("lótus.gonçalves@example.com", null));
        assertEquals(issue2998resolved, EMailValidator.isEmailValid("رونیکا.محمدخان@example.com", null));
        assertEquals(false, EMailValidator.isEmailValid("", null));
        /**
         * @todo How can null as an email address be valid?!?
         */
        assertEquals(true, EMailValidator.isEmailValid(null, null));
    }

}
