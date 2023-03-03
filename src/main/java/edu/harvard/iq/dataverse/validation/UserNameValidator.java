package edu.harvard.iq.dataverse.validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is not implementing the ConstraintValidatorMatcher interface, as this is not necessary any more.
 * It serves as the storage for the annotation configuration and convenient interface to a programmatic
 * validation of usernames.
 */
public class UserNameValidator {
    
    public static final int MIN_CHARS = 2;
    public static final int MAX_CHARS = 60;
    
    // NOTE: the size is checked by either the @Size annotation of @ValidateUserName or programmatically below!
    public static final String USERNAME_PATTERN = "[a-zA-Z0-9\\_\\-\\.]*";
    
    /*
     * If you would like to support accents or chinese characters in usernames, choose one of the below.
     *
     * With support for accents:
    private static final String USERNAME_PATTERN = "[a-zA-Z0-9\\_\\-\\.À-ÿ\\u00C0-\\u017F]";
     *
     * With support for chinese characters:
    private static final String USERNAME_PATTERN = "[a-zA-Z0-9\\_\\-\\.\\x{4e00}-\\x{9fa5}]";
     */
    
    private static final Matcher usernameMatcher = Pattern.compile(USERNAME_PATTERN).matcher("");

    /**
     * Validate a username against the pattern in {@link #USERNAME_PATTERN}
     * and check for length: min {@link #MIN_CHARS} chars, max {@link #MAX_CHARS} chars.
     * Nullsafe - null is considered invalid (as is empty).
     *
     * @param username The username to validate
     * @return true if matching, false otherwise
     */
    public static boolean isUserNameValid(final String username) {
        return username != null &&
               username.length() >= MIN_CHARS &&
               username.length() <= MAX_CHARS &&
               usernameMatcher.reset(username).matches();
    }

}



