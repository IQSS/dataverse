package edu.harvard.iq.dataverse.validation;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

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
        int numberOfCharacteristics;

        Params(int numberOfExpectedErrors, String password, Date passwordModificationTime, int expirationDays,
               int expirationMinLength, int goodStrength, int maxLength, int minLength, String dictionaries,
               int numberOfCharacteristics) {

            this.numberOfExpectedErrors = numberOfExpectedErrors;
            this.password = password;
            this.passwordModificationTime = passwordModificationTime;
            this.expirationDays = expirationDays;
            this.expirationMaxLength = expirationMinLength;
            this.goodStrength = goodStrength;
            this.maxLength = maxLength;
            this.minLength = minLength;
            this.dictionaries = dictionaries;
            this.numberOfCharacteristics = numberOfCharacteristics;
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

        int getNumberOfCharacteristics() {
            return numberOfCharacteristics;
        }

        @Override
        public String toString() {
            return
                    String.format(
                            "numberOfExpectedErrors=%s\npassword='%s'\npasswordModificationTime=%s\nexpirationDays=%s\n" +
                                    "expirationMaxLength=%s\ngoodStrength=%s\nmaxLength=%s\nminLength=%s\ndictionaries=%s\n" +
                                    "numberOfCharacteristics=%s\n%s",
                            numberOfExpectedErrors,
                            password,
                            passwordModificationTime,
                            expirationDays,
                            expirationMaxLength,
                            goodStrength,
                            maxLength,
                            minLength,
                            dictionaries,
                            numberOfCharacteristics,
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
        final int numberOfCharacters = 3;
        final int expirationDays = 365;
        final int expirationMinLength = 10;
        final int goodStrength20 = 20;
        final int maxLength = 0;
        final int minLength = 8;
        final String dictionary = createDictionary("56pOtAtO", false);

        final List<Params> paramsList = Arrays.asList(new Params[]{
                        new Params(7, "p otato", expired, expirationDays, expirationMinLength, goodStrength20, maxLength, minLength, dictionary, numberOfCharacters), // everything wrong here for both validators.
                        new Params(6, "p otato", expired, expirationDays, expirationMinLength, 0, maxLength, minLength, dictionary, numberOfCharacters), // no GoodStrength validator
                        new Params(0, "p", expired, expirationDays, 0, 0, 0, 0, dictionary, 0), // no validation... everything if off
                        new Params(1, "po", expired, expirationDays, 0, 0, 1, 0, dictionary, 0), // this password is too long
                        new Params(1, "potato", expired, expirationDays, 7, 0, 0, 0, dictionary, 0), // set expiration again
                        new Params(5, "p otato", expired, 401, expirationMinLength, 0, maxLength, minLength, dictionary, numberOfCharacters), // 401 days before expiration
                        new Params(5, "p otato", notExpired, expirationDays, expirationMinLength, 0, maxLength, minLength, dictionary, numberOfCharacters),
                        new Params(4, "one potato", notExpired, expirationDays, expirationMinLength, 0, maxLength, minLength, dictionary, numberOfCharacters),
                        new Params(3, "Two potato", notExpired, expirationDays, expirationMinLength, 0, maxLength, minLength, dictionary, numberOfCharacters),
                        new Params(0, "Three.potato", notExpired, expirationDays, expirationMinLength, 0, maxLength, minLength, dictionary, numberOfCharacters),
                        new Params(1, "F0ur.potato", expired, expirationDays, 15, 0, maxLength, 10, dictionary, numberOfCharacters),
                        new Params(0, "F0ur.potatos", notExpired, expirationDays, 15, 0, maxLength, 10, dictionary, numberOfCharacters),
                        new Params(1, "F0ur.potato", expired, expirationDays, 15, 0, maxLength, 10, dictionary, numberOfCharacters),
                        new Params(0, "4.potato", notExpired, expirationDays, expirationMinLength, 0, maxLength, minLength, dictionary, numberOfCharacters),
                        new Params(0, "55Potato", notExpired, expirationDays, expirationMinLength, 0, maxLength, minLength, dictionary, numberOfCharacters),
                        new Params(1, "56Potato", notExpired, expirationDays, expirationMinLength, 0, maxLength, minLength, dictionary, numberOfCharacters), // password in dictionary
                        new Params(0, "6 Potato", notExpired, expirationDays, expirationMinLength, goodStrength20, maxLength, minLength, dictionary, numberOfCharacters),
                        new Params(3, "7 Potato", notExpired, expirationDays, expirationMinLength, goodStrength20, maxLength, minLength, dictionary, 4), // add a fourth characteristic
                        new Params(0, "7 Potato901234567890", notExpired, expirationMinLength, minLength, goodStrength20, maxLength, minLength, dictionary, 4), // Now it does not matter: 20 characters
                        new Params(0, "8.Potato", notExpired, expirationDays, expirationMinLength, goodStrength20, maxLength, minLength, dictionary, 4), // Now we use all four
                        new Params(1, "Potato.Too.12345.Short", notExpired, expirationDays, expirationMinLength, 0, maxLength, 23, dictionary, numberOfCharacters),
                        new Params(0, "Potatoes on my plate with beef", expired, expirationDays, expirationMinLength, 30, maxLength, minLength, dictionary, numberOfCharacters),
                        new Params(0, "Potatoes on my plate with pie.", expired, expirationDays, expirationMinLength, 30, maxLength, minLength, dictionary, numberOfCharacters),
                        new Params(0, "Potatoes on a plate          .", expired, expirationDays, expirationMinLength, 30, maxLength, minLength, dictionary, numberOfCharacters),
                        new Params(0, "                              ", expired, expirationDays, expirationMinLength, 30, maxLength, minLength, dictionary, numberOfCharacters)
                }
        );

        paramsList.forEach(
                params -> {
                    passwordValidatorService.setGoodStrength(params.getGoodStrength());
                    passwordValidatorService.setExpirationDays(params.getExpirationDays());
                    passwordValidatorService.setExpirationMaxLength(params.getExpirationMaxLength());
                    passwordValidatorService.setMaxLength(params.getMaxLength());
                    passwordValidatorService.setMinLength(params.getMinLength());
                    passwordValidatorService.setDictionaries(params.getDictionaries());
                    passwordValidatorService.setNumberOfCharacteristics(params.getNumberOfCharacteristics());
                    List<String> errors = passwordValidatorService.validate(params.getPassword(), params.getPasswordModificationTime());
                    int actualErrors = errors.size();
                    int expectedErrors = params.getExpectedErrors();
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

}
