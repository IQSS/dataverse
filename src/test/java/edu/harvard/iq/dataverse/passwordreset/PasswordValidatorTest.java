package edu.harvard.iq.dataverse.passwordreset;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class PasswordValidatorTest {

    public String password;
    public boolean expected;
    public boolean mustContainSpecialCharacters;
    public boolean mustContainCapitalLetters;
    public boolean mustContainNumbers;
    public int minLenght;
    public int maxLenght;

    public PasswordValidatorTest(String password, boolean expected, boolean mustContainSpecialCharacters,
            boolean mustContainCapitalLetters, boolean mustContainNumbers, int minLenght, int maxLenght) {
        this.password = password;
        this.expected = expected;
        this.mustContainSpecialCharacters = mustContainSpecialCharacters;
        this.mustContainCapitalLetters = mustContainCapitalLetters;
        this.mustContainNumbers = mustContainNumbers;
        this.minLenght = minLenght;
        this.maxLenght = maxLenght;
    }

    @Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(
             new Object[][] {
                // Check if PasswordValidator correctly validates correct passwords
                // with all combinations of Special Characters,
                // Capital Letters and Numbers
                {"abcdefghabcdefgh", true, false, false, false, 8, 30},    
                {"@bcdefgh@bcdefgh", true, true, false, false, 8, 30},      
                {"@bAdefgh@bAdefgh", true, true, true, false, 8, 30},      
                {"abAdefghabAdefgh", true, false, true, false, 8, 30},     
                {"a1Adefgha1Adefgh", true, false, true, true, 8, 30},      
                {"ab1defghab1defgh", true, false, false, true, 8, 30},     
                {"@1cdefgh@1cdefgh", true, true, false, true, 8, 30},      
                {"@1Adefgh@1Adefgh", true, true, true, true, 8, 30},      
                // Check if PasswordValidator correctly rejects wrong passwords
                // with all combinations of Special Characters,
                // Capital Letters and Numbers
                {"abcabc", false, false, false, false, 8, 30},
                {"abcdabcd", false, true, false, false, 8, 30},       
                {"@bcd@bcd", false, true, true, false, 8, 30},       
                {"@bc1@bc1", false, false, true, false, 8, 30},      
                {"a1cda1cd", false, false, true, true, 8, 30},       
                {"AbcdAbcd", false, false, false, true, 8, 30},      
                {"@Bcd@Bcd", false, true, false, true, 8, 30},       
                {"a1Ada1Ad", false, true, true, true, 8, 30},
                {"", false, false, false, false, 1, 30},
                {" ", false, false, false, false, 1, 30},
                {"?!abcdef", false, true, false, false, 8, 30}
             }
        );
    }
    
    @Test
    public void testValidatePassword() {
        PasswordValidator validator = PasswordValidator.buildValidator(mustContainSpecialCharacters,
                mustContainCapitalLetters, mustContainNumbers, minLenght, maxLenght);
        boolean isValidPassword = validator.validatePassword(password);
        assertEquals(expected, isValidPassword);
    }
}
