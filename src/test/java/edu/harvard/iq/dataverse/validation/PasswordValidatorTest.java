package edu.harvard.iq.dataverse.validation;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * PasswordValidatorTest
 * <p>
 * Fire different passwords and settings to the validator service and compare them to an expected outcome.
 *
 * @author Lucien van Wouw <lwo@iisg.nl>
 */
public class PasswordValidatorTest {

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
            this.dictionaries = dictionaries ;
            this.numberOfCharacteristics = numberOfCharacteristics;
        }

        int getNumberOfExpectedErrors() {
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
                            "%s@%h::numberOfExpectedErrors=%s,password=%s,passwordModificationTime=%s,expirationDays=%s," +
                                    "expirationMaxLength=%s,goodStrength=%s,maxLength=%s,minLength=%s,dictionaries=%s" +
                                    "numberOfCharacteristics=%s",
                            getClass().getName(),
                            hashCode(),
                            numberOfExpectedErrors,
                            password,
                            passwordModificationTime,
                            expirationDays,
                            expirationMaxLength,
                            goodStrength,
                            maxLength,
                            minLength,
                            dictionaries,
                            numberOfCharacteristics
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
        final Date not_expired = new Date(new Date().getTime() - DAY * 300);
        final int NUMBER_OF_CHARACTERISTICS_3 = 3;
        final int EXPIRATION_DAYS_365 = 365;
        final int EXPIRATION_MIN_LENGTH_10 = 10;
        final int GOOD_STRENGTH_20 = 20;
        final int MAX_LENGTH_0 = 0;
        final int MIN_LENGTH_8 = 8;

        final List<Params> paramsList = Arrays.asList(new Params[]{
                        new Params(7, "p otato", expired, EXPIRATION_DAYS_365, EXPIRATION_MIN_LENGTH_10, GOOD_STRENGTH_20, MAX_LENGTH_0, MIN_LENGTH_8, null, NUMBER_OF_CHARACTERISTICS_3), // everything wrong here for both validators.
                        new Params(6, "p otato", expired, EXPIRATION_DAYS_365, EXPIRATION_MIN_LENGTH_10, 0, MAX_LENGTH_0, MIN_LENGTH_8, null, NUMBER_OF_CHARACTERISTICS_3), // no GoodStrength validator
                        new Params(0, "p", expired, EXPIRATION_DAYS_365, 0, 0, 0, 0, null, 0), // no validation... everything if off
                        new Params(1, "po", expired, EXPIRATION_DAYS_365, 0, 0, 1, 0, null, 0), // this password is too long
                        new Params(1, "potato", expired, EXPIRATION_DAYS_365, 7, 0, 0, 0, null, 0), // set expiration again
                        new Params(5, "p otato", expired, 401, EXPIRATION_MIN_LENGTH_10, 0, MAX_LENGTH_0, MIN_LENGTH_8, null, NUMBER_OF_CHARACTERISTICS_3), // 401 days before expiration
                        new Params(5, "p otato", not_expired, EXPIRATION_DAYS_365, EXPIRATION_MIN_LENGTH_10, 0, MAX_LENGTH_0, MIN_LENGTH_8, null, NUMBER_OF_CHARACTERISTICS_3),
                        new Params(4, "one potato", not_expired, EXPIRATION_DAYS_365, EXPIRATION_MIN_LENGTH_10, 0, MAX_LENGTH_0, MIN_LENGTH_8, null, NUMBER_OF_CHARACTERISTICS_3),
                        new Params(3, "Two potato", not_expired, EXPIRATION_DAYS_365, EXPIRATION_MIN_LENGTH_10, 0, MAX_LENGTH_0, MIN_LENGTH_8, null, NUMBER_OF_CHARACTERISTICS_3),
                        new Params(0, "Three.potato", not_expired, EXPIRATION_DAYS_365, EXPIRATION_MIN_LENGTH_10, 0, MAX_LENGTH_0, MIN_LENGTH_8, null, NUMBER_OF_CHARACTERISTICS_3),
                        new Params(1, "F0ur.potato", expired, EXPIRATION_DAYS_365, 15, 0, MAX_LENGTH_0, 10, null, NUMBER_OF_CHARACTERISTICS_3),
                        new Params(0, "F0ur.potatos", not_expired, EXPIRATION_DAYS_365, 15, 0, MAX_LENGTH_0, 10, null, NUMBER_OF_CHARACTERISTICS_3),
                        new Params(1, "F0ur.potato", expired, EXPIRATION_DAYS_365, 15, 0, MAX_LENGTH_0, 10, null, NUMBER_OF_CHARACTERISTICS_3),
                        new Params(0, "4.potato", not_expired, EXPIRATION_DAYS_365, EXPIRATION_MIN_LENGTH_10, 0, MAX_LENGTH_0, MIN_LENGTH_8, null, NUMBER_OF_CHARACTERISTICS_3),
                        new Params(0, "55Potato", not_expired, EXPIRATION_DAYS_365, EXPIRATION_MIN_LENGTH_10, 0, MAX_LENGTH_0, MIN_LENGTH_8, null, NUMBER_OF_CHARACTERISTICS_3),
                        new Params(1, "56Potato", not_expired, EXPIRATION_DAYS_365, EXPIRATION_MIN_LENGTH_10, 0, MAX_LENGTH_0, MIN_LENGTH_8, createDictionary("56pOtAtO", false), NUMBER_OF_CHARACTERISTICS_3), // password in dictionary
                        new Params(0, "6 Potato", not_expired, EXPIRATION_DAYS_365, EXPIRATION_MIN_LENGTH_10, GOOD_STRENGTH_20, MAX_LENGTH_0, MIN_LENGTH_8, null, NUMBER_OF_CHARACTERISTICS_3),
                        new Params(3, "7 Potato", not_expired, EXPIRATION_DAYS_365, EXPIRATION_MIN_LENGTH_10, GOOD_STRENGTH_20, MAX_LENGTH_0, MIN_LENGTH_8, null, 4), // add a fourth characteristic
                        new Params(0, "7 Potato901234567890", not_expired, EXPIRATION_MIN_LENGTH_10, MIN_LENGTH_8, GOOD_STRENGTH_20, MAX_LENGTH_0, MIN_LENGTH_8, null, 4), // Now it does not matter: 20 characters
                        new Params(0, "8.Potato", not_expired, EXPIRATION_DAYS_365, EXPIRATION_MIN_LENGTH_10, GOOD_STRENGTH_20, MAX_LENGTH_0, MIN_LENGTH_8, null, 4), // Now we use all four
                        new Params(1, "Potato.Too.12345.Short", not_expired, EXPIRATION_DAYS_365, EXPIRATION_MIN_LENGTH_10, 0, MAX_LENGTH_0, 23, null, NUMBER_OF_CHARACTERISTICS_3),
                        new Params(0, "Potatoes on my plate with beef", expired, EXPIRATION_DAYS_365, EXPIRATION_MIN_LENGTH_10, 30, MAX_LENGTH_0, MIN_LENGTH_8, null, NUMBER_OF_CHARACTERISTICS_3),
                        new Params(0, "Potatoes on my plate with pie.", expired, EXPIRATION_DAYS_365, EXPIRATION_MIN_LENGTH_10, 30, MAX_LENGTH_0, MIN_LENGTH_8, null, NUMBER_OF_CHARACTERISTICS_3),
                        new Params(0, "Potatoes on a plate          .", expired, EXPIRATION_DAYS_365, EXPIRATION_MIN_LENGTH_10, 30, MAX_LENGTH_0, MIN_LENGTH_8, null, NUMBER_OF_CHARACTERISTICS_3),
                        new Params(0, "                              ", expired, EXPIRATION_DAYS_365, EXPIRATION_MIN_LENGTH_10, 30, MAX_LENGTH_0, MIN_LENGTH_8, null, NUMBER_OF_CHARACTERISTICS_3)
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
                    int actualErrors = actualErrors(errors);
                    int numberOfExpectedErrors = params.getNumberOfExpectedErrors();
                    Assert.assertTrue(message(params, errors, numberOfExpectedErrors, actualErrors), actualErrors == numberOfExpectedErrors);
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

    private int actualErrors(List<String> errors) {
        return (errors == null) ? 0 : errors.size();
    }

    private String message(Params params, List<String> errors, int expected, int actual) {
        String details = (actual == 0) ? params.toString() : PasswordValidatorServiceBean.parseMessages(errors) + " with " + params;
        return String.format("Expected %s but got %s validation errors. Details: %s", expected, actual, details);
    }

}
