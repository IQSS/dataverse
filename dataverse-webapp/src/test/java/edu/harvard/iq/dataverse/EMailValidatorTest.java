package edu.harvard.iq.dataverse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * @author philip durbin
 * @author oscardssmith
 * @author rohit bhattacharjee
 * @author stephen kraffmiller
 * @author alex scheitlin
 */
@RunWith(Parameterized.class)
public class EMailValidatorTest {

    public boolean isValid;
    public String email;

    public EMailValidatorTest(boolean isValid, String email) {
        this.isValid = isValid;
        this.email = email;
    }

    @Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {true, "pete@mailinator.com"},

                // @todo How can this be a valid email address?
                {true, " leadingWhitespace@mailinator.com"},

                // @todo How can this be a valid email address?
                {true, "trailingWhitespace@mailinator.com "},

                {false, "elisah.da mota@example.com"},
                {false, "pete1@mailinator.com;pete2@mailinator.com"},

                /**
                 * These examples are all from https://randomuser.me and seem to be
                 * valid according to
                 * http://sphinx.mythic-beasts.com/~pdw/cgi-bin/emailvalidate (except
                 * رونیکا.محمدخان@example.com).
                */
                {true, "michélle.pereboom@example.com"},
                {true, "begüm.vriezen@example.com"},
                {true, "lótus.gonçalves@example.com"},
                {true, "lótus.gonçalves@éxample.com"},
                {true, "begüm.vriezen@example.cologne"},
                {true, "رونیکا.محمدخان@example.com"},
                {false, "lótus.gonçalves@example.cóm"},
                {false, "dora@.com"},
                {false, ""},
                {false, null},

                // add tests for 4601
                {true, "blah@wiso.uni-unc.de"},
                {true, "foo@essex.co.uk"},
                {true, "jack@bu.cloud"},
        });
    }

    @Test
    public void testIsEmailValid() {
        assertEquals(isValid, EMailValidator.isEmailValid(email, null));
    }

}
