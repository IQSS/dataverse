package edu.harvard.iq.dataverse.passwordreset;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * PasswordValidatorTest
 *
 * Determine if the validator correctly handles limits applied to the password format.
 */
public class PasswordValidatorTest {

    @Test
    public void testPasswordValidatorNoForceMizedLetters() {

        // All taken from edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserPage
        int minPasswordLength = 6;
        boolean forceNumber = true;
        boolean forceSpecialChar = false;
        boolean forceMixedLetters = false;
        int maxPasswordLength = 255;
        final PasswordValidator passwordValidator = PasswordValidator.buildValidator(forceSpecialChar, forceMixedLetters, forceNumber, minPasswordLength, maxPasswordLength);

        boolean passwordIsComplexEnough = passwordValidator.validatePassword("short");
        assertFalse(passwordIsComplexEnough);

        passwordIsComplexEnough = passwordValidator.validatePassword("AAAaAA1");
        assertTrue(passwordIsComplexEnough);

        passwordIsComplexEnough = passwordValidator.validatePassword("aaaaaa1");
        assertTrue(passwordIsComplexEnough);

        passwordIsComplexEnough = passwordValidator.validatePassword("aaaaaa");
        assertFalse(passwordIsComplexEnough);

    }

    @Test
    public void testPasswordValidatorWithForceMizedLetters() {

        int minPasswordLength = 6;
        boolean forceNumber = true;
        boolean forceSpecialChar = false;
        boolean forceMixedLetters = true;
        int maxPasswordLength = 255;
        final PasswordValidator passwordValidator = PasswordValidator.buildValidator(forceSpecialChar, forceMixedLetters, forceNumber, minPasswordLength, maxPasswordLength);

        boolean passwordIsComplexEnough = passwordValidator.validatePassword("AAAaAA1");
        assertTrue(passwordIsComplexEnough);

        passwordIsComplexEnough = passwordValidator.validatePassword("aaaaaa1");
        assertFalse(passwordIsComplexEnough);

        passwordIsComplexEnough = passwordValidator.validatePassword("AAAAA1");
        assertFalse(passwordIsComplexEnough);

    }

}
