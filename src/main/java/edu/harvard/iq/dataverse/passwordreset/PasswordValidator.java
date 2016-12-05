package edu.harvard.iq.dataverse.passwordreset;

// based on http://howtodoinjava.com/2012/12/14/how-to-build-regex-based-password-validator-in-java/
/**
 * @todo perhaps replace with
 * https://code.google.com/p/vt-middleware/wiki/vtpassword
 */
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PasswordValidator {

    private static PasswordValidator INSTANCE = new PasswordValidator();
    private static String pattern = null;

    /**
     * No one can make a direct instance
     */
    private PasswordValidator() {
        //do nothing
    }

    /**
     * Force the user to build a validator using this way only
     *
     * @param forceSpecialChar
     * @param forceMixedLetters
     * @param forceNumber
     * @param minLength
     * @param maxLength
     * @return
     */
    public static PasswordValidator buildValidator(boolean forceSpecialChar,
            boolean forceMixedLetters,
            boolean forceNumber,
            int minLength,
            int maxLength) {

        final StringBuilder patternBuilder = new StringBuilder("(");

        if (forceMixedLetters) {
            patternBuilder.append("(?=.*[A-Z])(?=.*[a-z])");
        } else {
            patternBuilder.append("(?=.*[A-Za-z])");
        }

        /**
         * @todo should probably allow additional special characters
         */
        if (forceSpecialChar) {
            patternBuilder.append("(?=.*[@#$%])");
        }

        if (forceNumber) {
            patternBuilder.append("(?=.*\\d)");
        }

        patternBuilder.append(".{").append(minLength).append(",").append(maxLength).append("})");
        pattern = patternBuilder.toString();

        return INSTANCE;
    }

    /**
     * Here we will validate the password
     *
     * @param password
     */
    public boolean validatePassword(final String password) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(password);
        return m.matches();
    }

}
