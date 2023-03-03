package edu.harvard.iq.dataverse.validation;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PasswordValidatorTest
 * <p>
 * Fire different passwords and settings to the validator service and compare them to an expected outcome.
 *
 * @author Lucien van Wouw <lwo@iisg.nl>
 */
public class PasswordValidatorTest {

    private static final Logger logger = Logger.getLogger(PasswordValidatorTest.class.getCanonicalName());
    private static PasswordValidatorServiceBean passwordValidatorService;

    @BeforeAll
    public static void setUp() {
        passwordValidatorService = new PasswordValidatorServiceBean();
    }

    static Stream<Arguments> passwordParams() {
        long DAY = 86400000L;
        List<CharacterRule> characterRulesDefault = PasswordValidatorUtil.getCharacterRulesDefault();
        List<CharacterRule> characterRulesHarvardLevel3 = getCharacterRulesHarvardLevel3();
        final int numberOfCharactersDefault = 2;
        final int numberOfCharacters = 3;
        final int numConsecutiveDigitsAllowed = 4;
        final int goodStrength20 = 20;
        final int maxLength = 0;
        final int minLength = 8;
        final String dictionary = createDictionary("56pOtAtO", false);
        
        // Format: int numberOfExpectedErrors, String password, int goodStrength, int maxLength, int minLength,
        //         String dictionaries, int numberOfCharacteristics, List<CharacterRule> characterRules,
        //         int numConsecutiveDigitsAllowed
        return Stream.of(
            // everything wrong here for both validators.
            Arguments.of(6, "p otato", goodStrength20, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            // no GoodStrength validator
            Arguments.of(5, "p otato", 0, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            // no validation... everything if off
            Arguments.of(0, "p", 0, 0, 0, dictionary, 0, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            // this password is too long
            Arguments.of(1, "po", 0, 1, 0, dictionary, 0, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            // set expiration again
            Arguments.of(0, "potato", 0, 0, 0, dictionary, 0, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            Arguments.of(4, "one potato", 0, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            Arguments.of(3, "Two potato",  0, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            Arguments.of(0, "Three.potato",  0, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            Arguments.of(0, "F0ur.potato",  0, maxLength, 10, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            Arguments.of(0, "F0ur.potatos",  0, maxLength, 10, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            Arguments.of(0, "F0ur.potato",  0, maxLength, 10, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            Arguments.of(0, "4.potato",  0, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            Arguments.of(0, "55Potato",  0, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            // password in dictionary
            Arguments.of(1, "56pOtAtO",  0, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            // password in dictionary case insensitive
            Arguments.of(1, "56Potato",  0, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            // substring of password in dictionary
            Arguments.of(1, "56pOtAtOs",  0, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            Arguments.of(0, "6 Potato",  goodStrength20, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            // add a fourth characteristic
            Arguments.of(3, "7 Potato",  goodStrength20, maxLength, minLength, dictionary, 4, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            // Now it does not matter: 20 characters
            Arguments.of(0, "7 Potato901234567890", goodStrength20, maxLength, minLength, dictionary, 4, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            // Now we use all four
            Arguments.of(0, "8.Potato",  goodStrength20, maxLength, minLength, dictionary, 4, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            Arguments.of(2, "Potato.Too.12345.Short", 0, maxLength, 23, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            Arguments.of(0, "Potatoes on my plate with beef", 30, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            Arguments.of(0, "Potatoes on my plate with pie.", 30, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            Arguments.of(0, "Potatoes on a plate  .",  30, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            // Pass when repeating character maximum is 5
            Arguments.of(0, "Repeated Potatoes:0000",  30, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, 5),
            // Allow no more than 3 repeating characters (default)
            Arguments.of(0, "Repeated Potatoes:000",  30, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            //For some reason, whitespace doesn't count in the repeating rule?
            Arguments.of(6, "          ", 30, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            // Good enough for Dataverse 4.0.
            Arguments.of(0, "potat1", goodStrength20, maxLength, 6, dictionary, numberOfCharactersDefault, characterRulesDefault, numConsecutiveDigitsAllowed),
            // Has repeating chars exceeding limit, but goodstrength waives it
            Arguments.of(0, "potat000000000000000", goodStrength20, maxLength, 6, dictionary, numberOfCharactersDefault, characterRulesDefault, numConsecutiveDigitsAllowed),
            // 5 or more numbers in a row
            Arguments.of(2, "ma02138", goodStrength20, maxLength, 6, dictionary, numberOfCharactersDefault, characterRulesDefault, numConsecutiveDigitsAllowed),
            // 5 or more numbers in a row, but multiple times
            Arguments.of(3, "ma8312002138", goodStrength20, maxLength, 6, dictionary, numberOfCharactersDefault, characterRulesDefault, numConsecutiveDigitsAllowed)
        );
    }
    
    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @MethodSource("passwordParams")
    public void testDifferentPasswordsAndSettings(int numberOfExpectedErrors, String password, int goodStrength, int maxLength, int minLength, String dictionaries,
                                                  int numberOfCharacteristics, List<CharacterRule> characterRules, int numberOfConsecutiveDigitsAllowed) {
        // given
        passwordValidatorService.setGoodStrength(goodStrength);
        passwordValidatorService.setMaxLength(maxLength);
        passwordValidatorService.setMinLength(minLength);
        passwordValidatorService.setDictionaries(dictionaries);
        passwordValidatorService.setNumberOfCharacteristics(numberOfCharacteristics);
        passwordValidatorService.setCharacterRules(characterRules);
        passwordValidatorService.setNumberOfConsecutiveDigitsAllowed(numberOfConsecutiveDigitsAllowed);

        // when
        List<String> errors = passwordValidatorService.validate(password);
        
        // then
        String message = "";
        if (numberOfExpectedErrors != errors.size()) {
            message = StringUtils.join(errors, "\n");
        }
        assertEquals(numberOfExpectedErrors, errors.size(), message);
    }

    /**
     * createDictionary
     * <p>
     * Create a dictionary with a password
     *
     * @param password The string to add
     * @return The absolute file path of the dictionary file.
     */
    static String createDictionary(String password, boolean append) {
        File file = null;
        try {
            file = File.createTempFile("weak_passwords", ".txt");
            FileOutputStream fileOutputStream = new FileOutputStream(file, append);
            fileOutputStream.write(password.getBytes());
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert file != null;
        return file.getAbsolutePath();
    }
    
    public static List<CharacterRule> getCharacterRulesHarvardLevel3() {
        List<CharacterRule> characterRules = new ArrayList<>();
        characterRules.add(new CharacterRule(EnglishCharacterData.UpperCase, 1));
        characterRules.add(new CharacterRule(EnglishCharacterData.LowerCase, 1));
        characterRules.add(new CharacterRule(EnglishCharacterData.Digit, 1));
        characterRules.add(new CharacterRule(EnglishCharacterData.Special, 1));
        return characterRules;
    }

}