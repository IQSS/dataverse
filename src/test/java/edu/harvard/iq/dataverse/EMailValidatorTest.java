package edu.harvard.iq.dataverse;

import org.apache.commons.validator.routines.EmailValidator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EMailValidatorTest {

    static EmailValidator validator = EmailValidator.getInstance();

    @ParameterizedTest
    @CsvSource(value = {
        "true, 'pete@mailinator.com'",
        "false, ' leadingWhitespace@mailinator.com'",
        "false, 'trailingWhitespace@mailinator.com '",
    
        "false, 'elisah.da mota@example.com'",
        "false, 'pete1@mailinator.com;pete2@mailinator.com'",
    
        /**
         * These examples are all from https://randomuser.me and seem to be
         * valid according to
         * http://sphinx.mythic-beasts.com/~pdw/cgi-bin/emailvalidate (except
         * رونیکا.محمدخان@example.com).
        */
        "true, 'michélle.pereboom@example.com'",
        "true, 'begüm.vriezen@example.com'",
        "true, 'lótus.gonçalves@example.com'",
        "true, 'lótus.gonçalves@éxample.com'",
        "true, 'begüm.vriezen@example.cologne'",
        "true, 'رونیکا.محمدخان@example.com'",
        "false, 'lótus.gonçalves@example.cóm'",
        "false, 'dora@.com'",
        "false, ''",
        "false, NULL",
    
        // add tests for 4601
        "true, 'blah@wiso.uni-unc.de'",
        "true, 'foo@essex.co.uk'",
        "true, 'jack@bu.cloud'"
    }, nullValues = "NULL")
    public void testIsEmailValid(boolean expected, String mail) {
        assertEquals(expected, validator.isValid(mail));
    }
}
