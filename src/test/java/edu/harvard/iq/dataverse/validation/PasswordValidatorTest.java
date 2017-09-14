package edu.harvard.iq.dataverse.validation;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;

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

    /**
     * Helper class so we can pass on a list of passwords and validation settings to falsify the outcome.
     */
    private class Params {

        int numberOfExpectedErrors;
        String password;
        Date passwordModificationTime;
        int expirationDays;
        int expirationMaxLength;
        int goodStrength;
        int maxLength;
        int minLength;
        String dictionaries;
        List<CharacterRule> characterRules;
        int numberOfCharacteristics;
        int numberOfConsecutiveDigitsAllowed;

        Params(int numberOfExpectedErrors, String password, Date passwordModificationTime, int expirationDays,
               int expirationMinLength, int goodStrength, int maxLength, int minLength, String dictionaries,
               int numberOfCharacteristics, List<CharacterRule> characterRules, int numConsecutiveDigitsAllowed) {

            this.numberOfExpectedErrors = numberOfExpectedErrors;
            this.password = password;
            this.passwordModificationTime = passwordModificationTime;
            this.expirationDays = expirationDays;
            this.expirationMaxLength = expirationMinLength;
            this.goodStrength = goodStrength;
            this.maxLength = maxLength;
            this.minLength = minLength;
            this.dictionaries = dictionaries;
            this.characterRules = characterRules;
            this.numberOfCharacteristics = numberOfCharacteristics;
            this.numberOfConsecutiveDigitsAllowed = numConsecutiveDigitsAllowed;
        }

        int getExpectedErrors() {
            return numberOfExpectedErrors;
        }

        String getPassword() {
            return password;
        }

        Date getPasswordModificationTime() {
            return passwordModificationTime;
        }

        int getExpirationDays() {
            return expirationDays;
        }

        int getExpirationMaxLength() {
            return expirationMaxLength;
        }

        int getGoodStrength() {
            return goodStrength;
        }

        int getMaxLength() {
            return maxLength;
        }

        int getMinLength() {
            return minLength;
        }

        String getDictionaries() {
            return dictionaries;
        }

        List<CharacterRule> getCharacterRules() {
            return characterRules;
        }

        int getNumberOfCharacteristics() {
            return numberOfCharacteristics;
        }
        
        int getNumberOfConsecutiveDigitsAllowed() {
            return numberOfConsecutiveDigitsAllowed;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (CharacterRule rule : characterRules) {
                sb.append(rule.getNumberOfCharacters() + " of " + rule.getValidCharacters() + " ");
            }
            String characterRulesReadable = sb.toString();
            return
                    String.format("numberOfExpectedErrors=%s\npassword='%s'\npasswordModificationTime=%s\nexpirationDays=%s\n" +
                                    "expirationMaxLength=%s\ngoodStrength=%s\nmaxLength=%s\nminLength=%s\ndictionaries=%s\n" +
                                    "characterRules=%s\n" +
                                    "numberOfCharacteristics=%s\nnumberOfRepeatingCharactersAllowed=%s\n%s",
                            numberOfExpectedErrors,
                            password,
                            passwordModificationTime,
                            expirationDays,
                            expirationMaxLength,
                            goodStrength,
                            maxLength,
                            minLength,
                            dictionaries,
                            characterRulesReadable,
                            numberOfCharacteristics,
                            numberOfConsecutiveDigitsAllowed,
                            StringUtils.repeat("-", 80)
                    );
        }
    }

    @BeforeClass
    public static void setUp() {
        passwordValidatorService = new PasswordValidatorServiceBean();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDifferentPasswordsAndSettings() {

        long DAY = 86400000L;
        final Date expired = new Date(new Date().getTime() - DAY * 400);
        final Date notExpired = new Date(new Date().getTime() - DAY * 300);
        List<CharacterRule> characterRulesDefault = PasswordValidatorUtil.getCharacterRulesDefault();
        List<CharacterRule> characterRulesHarvardLevel3 = getCharacterRulesHarvardLevel3();
        final int numberOfCharactersDefault = 2;
        final int numberOfCharacters = 3;
        final int numConsecutiveDigitsAllowed = 4;
        final int expirationDays = 365;
        final int expirationMinLength = 10;
        final int goodStrength20 = 20;
        final int maxLength = 0;
        final int minLength = 8;
        final String dictionary = createDictionary("56pOtAtO", false);

        final List<Params> paramsList = Arrays.asList(new Params[]{
            new Params(6, "p otato", notExpired, expirationDays, expirationMinLength, goodStrength20, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed), // everything wrong here for both validators.
            new Params(5, "p otato", notExpired, expirationDays, expirationMinLength, 0, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed), // no GoodStrength validator
            new Params(0, "p", expired, expirationDays, 0, 0, 0, 0, dictionary, 0, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed), // no validation... everything if off
            new Params(1, "po", expired, expirationDays, 0, 0, 1, 0, dictionary, 0, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed), // this password is too long
            new Params(0, "potato", notExpired, expirationDays, 7, 0, 0, 0, dictionary, 0, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed), // set expiration again
            new Params(5, "p otato", expired, 401, expirationMinLength, 0, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed), // 401 days before expiration
            new Params(5, "p otato", notExpired, expirationDays, expirationMinLength, 0, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            new Params(4, "one potato", notExpired, expirationDays, expirationMinLength, 0, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            new Params(3, "Two potato", notExpired, expirationDays, expirationMinLength, 0, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            new Params(0, "Three.potato", notExpired, expirationDays, expirationMinLength, 0, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            new Params(0, "F0ur.potato", notExpired, expirationDays, 15, 0, maxLength, 10, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            new Params(0, "F0ur.potatos", notExpired, expirationDays, 15, 0, maxLength, 10, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            new Params(0, "F0ur.potato", notExpired, expirationDays, 15, 0, maxLength, 10, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            new Params(0, "4.potato", notExpired, expirationDays, expirationMinLength, 0, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            new Params(0, "55Potato", notExpired, expirationDays, expirationMinLength, 0, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            new Params(1, "56pOtAtO", notExpired, expirationDays, expirationMinLength, 0, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed), // password in dictionary
            new Params(1, "56Potato", notExpired, expirationDays, expirationMinLength, 0, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed), // password in dictionary case insensitive
            new Params(1, "56pOtAtOs", notExpired, expirationDays, expirationMinLength, 0, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed), // substring of password in dictionary
            new Params(0, "6 Potato", notExpired, expirationDays, expirationMinLength, goodStrength20, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            new Params(3, "7 Potato", notExpired, expirationDays, expirationMinLength, goodStrength20, maxLength, minLength, dictionary, 4, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed), // add a fourth characteristic
            new Params(0, "7 Potato901234567890", notExpired, expirationMinLength, minLength, goodStrength20, maxLength, minLength, dictionary, 4, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed), // Now it does not matter: 20 characters
            new Params(0, "8.Potato", notExpired, expirationDays, expirationMinLength, goodStrength20, maxLength, minLength, dictionary, 4, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed), // Now we use all four
            new Params(2, "Potato.Too.12345.Short", notExpired, expirationDays, expirationMinLength, 0, maxLength, 23, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            new Params(0, "Potatoes on my plate with beef", expired, expirationDays, expirationMinLength, 30, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            new Params(0, "Potatoes on my plate with pie.", expired, expirationDays, expirationMinLength, 30, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            new Params(0, "Potatoes on a plate  .", expired, expirationDays, expirationMinLength, 30, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed),
            new Params(0, "Repeated Potatoes:0000", expired, expirationDays, expirationMinLength, 30, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, 5), // Pass when repeating character maximum is 5
            new Params(0, "Repeated Potatoes:000", expired, expirationDays, expirationMinLength, 30, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed), // Allow no more than 3 repeating characters (default)
            new Params(6, "          ", expired, expirationDays, expirationMinLength, 30, maxLength, minLength, dictionary, numberOfCharacters, characterRulesHarvardLevel3, numConsecutiveDigitsAllowed), //For some reason, whitespace doesn't count in the repeating rule?
            new Params(0, "potat1", notExpired, expirationDays, expirationMinLength, goodStrength20, maxLength, 6, dictionary, numberOfCharactersDefault, characterRulesDefault, numConsecutiveDigitsAllowed), // Good enough for Dataverse 4.0.
            new Params(0, "potat000000000000000", notExpired, expirationDays, expirationMinLength, goodStrength20, maxLength, 6, dictionary, numberOfCharactersDefault, characterRulesDefault, numConsecutiveDigitsAllowed), // Has repeating chars exceeding limit, but goodstrength waives it
            new Params(2, "ma02138", notExpired, expirationDays, expirationMinLength, goodStrength20, maxLength, 6, dictionary, numberOfCharactersDefault, characterRulesDefault, numConsecutiveDigitsAllowed), // 5 or more numbers in a row
            new Params(2, "ma8312002138", notExpired, expirationDays, expirationMinLength, goodStrength20, maxLength, 6, dictionary, numberOfCharactersDefault, characterRulesDefault, numConsecutiveDigitsAllowed), // 5 or more numbers in a row
        }
        );

        paramsList.forEach(
                params -> {
                    int expectedErrors = params.getExpectedErrors();
//                    List<String> errors = passwordValidatorService.validate(params.getPassword(), params.getPasswordModificationTime());
//                    passwordValidatorService.setExpirationDays(params.getExpirationDays());
//                    passwordValidatorService.setExpirationMaxLength(params.getExpirationMaxLength());
                    passwordValidatorService.setGoodStrength(params.getGoodStrength());
                    passwordValidatorService.setMaxLength(params.getMaxLength());
                    passwordValidatorService.setMinLength(params.getMinLength());
                    passwordValidatorService.setDictionaries(params.getDictionaries());
                    passwordValidatorService.setNumberOfCharacteristics(params.getNumberOfCharacteristics());
                    passwordValidatorService.setCharacterRules(params.getCharacterRules());
                    passwordValidatorService.setNumberOfConsecutiveDigitsAllowed(params.getNumberOfConsecutiveDigitsAllowed());

                    List<String> errors = passwordValidatorService.validate(params.getPassword());
                    int actualErrors = errors.size();

                    String message = message(params, errors, expectedErrors, actualErrors);
                    logger.info(message);
                    Assert.assertTrue(message, actualErrors == expectedErrors);
                }
        );

    }

    /**
     * createDictionary
     * <p>
     * Create a dictionary with a password
     *
     * @param password The string to add
     * @return The absolute file path of the dictionary file.
     */
    private String createDictionary(String password, boolean append) {
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

    private String message(Params params, List<String> errors, int expected, int actual) {
        String details = (actual == 0) ? params.toString() : PasswordValidatorServiceBean.parseMessages(errors) + "\n" + params;
        return String.format("Expected errors: %s\nActual errors: %s\nDetails: %s", expected, actual, details);
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