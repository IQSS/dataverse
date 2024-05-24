package edu.harvard.iq.dataverse.passwordreset;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class PasswordValidatorTest {
    
    static Stream<Arguments> testCases() {
        return Stream.of(
            // Check if PasswordValidator correctly validates correct passwords
            // with all combinations of Special Characters,
            // Capital Letters and Numbers
            Arguments.of("abcdefghabcdefgh", true, false, false, false, 8, 30),
            Arguments.of("@bcdefgh@bcdefgh", true, true, false, false, 8, 30),
            Arguments.of("@bAdefgh@bAdefgh", true, true, true, false, 8, 30),
            Arguments.of("abAdefghabAdefgh", true, false, true, false, 8, 30),
            Arguments.of("a1Adefgha1Adefgh", true, false, true, true, 8, 30),
            Arguments.of("ab1defghab1defgh", true, false, false, true, 8, 30),
            Arguments.of("@1cdefgh@1cdefgh", true, true, false, true, 8, 30),
            Arguments.of("@1Adefgh@1Adefgh", true, true, true, true, 8, 30),
            // Check if PasswordValidator correctly rejects wrong passwords
            // with all combinations of Special Characters,
            // Capital Letters and Numbers
            Arguments.of("abcabc", false, false, false, false, 8, 30),
            Arguments.of("abcdabcd", false, true, false, false, 8, 30),
            Arguments.of("@bcd@bcd", false, true, true, false, 8, 30),
            Arguments.of("@bc1@bc1", false, false, true, false, 8, 30),
            Arguments.of("a1cda1cd", false, false, true, true, 8, 30),
            Arguments.of("AbcdAbcd", false, false, false, true, 8, 30),
            Arguments.of("@Bcd@Bcd", false, true, false, true, 8, 30),
            Arguments.of("a1Ada1Ad", false, true, true, true, 8, 30),
            Arguments.of("", false, false, false, false, 1, 30),
            Arguments.of(" ", false, false, false, false, 1, 30),
            Arguments.of("?!abcdef", false, true, false, false, 8, 30)
        );
    }
    
    @ParameterizedTest
    @MethodSource("testCases")
    void testValidatePassword(String password, boolean expected, boolean mustContainSpecialCharacters,
                              boolean mustContainCapitalLetters, boolean mustContainNumbers, int minLength,
                              int maxLength) {
        PasswordValidator validator = PasswordValidator.buildValidator(mustContainSpecialCharacters,
                mustContainCapitalLetters, mustContainNumbers, minLength, maxLength);
        boolean isValidPassword = validator.validatePassword(password);
        assertEquals(expected, isValidPassword);
    }
}
