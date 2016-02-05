package edu.harvard.iq.dataverse.authorization.providers.builtin;

import java.util.Set;
import java.util.logging.Logger;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class BuiltinUserTest {

    private static final Logger logger = Logger.getLogger(BuiltinUserTest.class.getCanonicalName());

    private Validator validator;

    @Before
    public void setUp() {
        ValidatorFactory vf = Validation.buildDefaultValidatorFactory();
        this.validator = vf.getValidator();
    }

    @Test
    public void testUsernameAsEmailAddress() {
        BuiltinUser builtinUser = new BuiltinUser();
        builtinUser.setUserName("foo@bar.com");
        builtinUser.setFirstName("firstName");
        builtinUser.setLastName("lastName");
        builtinUser.setEmail("foo@bar.com");
        Set<ConstraintViolation<BuiltinUser>> violations = this.validator.validate(builtinUser);
        printViolations(violations);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void testUsernameNotAsEmailAddress() {
        BuiltinUser builtinUser = new BuiltinUser();
        builtinUser.setUserName("foo");
        builtinUser.setFirstName("firstName");
        builtinUser.setLastName("lastName");
        builtinUser.setEmail("foo@bar.com");
        Set<ConstraintViolation<BuiltinUser>> violations = this.validator.validate(builtinUser);
        printViolations(violations);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void testUsernameTooLong() {
        BuiltinUser builtinUser = new BuiltinUser();
        builtinUser.setUserName("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
        builtinUser.setFirstName("firstName");
        builtinUser.setLastName("lastName");
        builtinUser.setEmail("foo@bar.com");
        Set<ConstraintViolation<BuiltinUser>> violations = this.validator.validate(builtinUser);
        printViolations(violations);
        assertFalse(violations.isEmpty());
    }

    @Test
    public void testEmailAddressWithDashAndDot() {
        BuiltinUser builtinUser = new BuiltinUser();
        builtinUser.setUserName("foo");
        builtinUser.setFirstName("firstName");
        builtinUser.setLastName("lastName");
        // https://en.wikipedia.org/wiki/Email_address#Valid_email_addresses
        String strangeButValidEmailAddress = "other.email-with-dash@example.com";
        builtinUser.setEmail(strangeButValidEmailAddress);
        Set<ConstraintViolation<BuiltinUser>> violations1 = this.validator.validate(builtinUser);
        printViolations(violations1);
        assertTrue(violations1.isEmpty());

        builtinUser.setUserName(strangeButValidEmailAddress);
        Set<ConstraintViolation<BuiltinUser>> violations2 = this.validator.validate(builtinUser);
        printViolations(violations2);
        assertTrue(violations2.isEmpty());
    }

    @Test
    public void testEmailAddressWithPlusSign() {
        BuiltinUser builtinUser = new BuiltinUser();
        builtinUser.setUserName("foo");
        builtinUser.setFirstName("firstName");
        builtinUser.setLastName("lastName");
        // https://en.wikipedia.org/wiki/Email_address#Valid_email_addresses
        String strangeButValidEmailAddress = "disposable.style.email.with+symbol@example.com";
        builtinUser.setEmail(strangeButValidEmailAddress);
        Set<ConstraintViolation<BuiltinUser>> violations1 = this.validator.validate(builtinUser);
        printViolations(violations1);
        assertTrue(violations1.isEmpty());

        builtinUser.setUserName(strangeButValidEmailAddress);
        Set<ConstraintViolation<BuiltinUser>> violations2 = this.validator.validate(builtinUser);
        printViolations(violations2);
        assertTrue(violations2.isEmpty());
    }

    @Test
    public void testEmailWithApostrophe() {
        BuiltinUser builtinUser = new BuiltinUser();
        builtinUser.setUserName("foo");
        builtinUser.setFirstName("firstName");
        builtinUser.setLastName("lastName");
        // http://stackoverflow.com/questions/8527180/can-there-be-an-apostrophe-in-an-email-address
        String strangeButValidEmailAddress = "Tim_O'Reilly@oreilly.com";
        builtinUser.setEmail(strangeButValidEmailAddress);
        Set<ConstraintViolation<BuiltinUser>> violations1 = this.validator.validate(builtinUser);
        printViolations(violations1);
        assertTrue(violations1.isEmpty());

        builtinUser.setUserName(strangeButValidEmailAddress);
        Set<ConstraintViolation<BuiltinUser>> violations2 = this.validator.validate(builtinUser);
        printViolations(violations2);
        assertTrue(violations2.isEmpty());
    }

    @Test
    public void testEmailWithQuotesAndSpaces() {
        BuiltinUser builtinUser = new BuiltinUser();
        builtinUser.setUserName("foo");
        builtinUser.setFirstName("firstName");
        builtinUser.setLastName("lastName");
        // https://en.wikipedia.org/wiki/Email_address#Valid_email_addresses
        String strangeButValidEmailAddress = "\"much.more unusual\"@example.com";
        builtinUser.setEmail(strangeButValidEmailAddress);
        Set<ConstraintViolation<BuiltinUser>> violations1 = this.validator.validate(builtinUser);
        printViolations(violations1);
        assertTrue(violations1.isEmpty());

        builtinUser.setUserName(strangeButValidEmailAddress);
        Set<ConstraintViolation<BuiltinUser>> violations2 = this.validator.validate(builtinUser);
        printViolations(violations2);
        // don't allows usernames with spaces even though it's technically a valid email address
        assertFalse(violations2.isEmpty());
    }

    /**
     * We have a business requirement to support login using email address at
     * https://github.com/IQSS/dataverse/issues/2115 but it's possible to have a
     * very long email address that isn't a valid username because we limit the
     * number of characters in a username. We should expect violations in this
     * case.
     */
    @Test
    public void testValidEmailAddressButTooLongAsForUsername() {
        BuiltinUser builtinUser = new BuiltinUser();
        String longEmailAddress = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@mailinator.com";
        builtinUser.setUserName(longEmailAddress);
        builtinUser.setFirstName("firstName");
        builtinUser.setLastName("lastName");
        builtinUser.setEmail(longEmailAddress);
        Set<ConstraintViolation<BuiltinUser>> violations = this.validator.validate(builtinUser);
        printViolations(violations);
        assertFalse(violations.isEmpty());
    }

    private void printViolations(Set<ConstraintViolation<BuiltinUser>> violations) {
        StringBuilder sb = new StringBuilder();
        for (ConstraintViolation<?> violation : violations) {
            sb.append("(invalid value: <<<").append(violation.getInvalidValue()).append(">>> for ").append(violation.getPropertyPath()).append(" - ").append(violation.getMessage()).append("\n\n");
            logger.info(sb.toString());
        }
    }
}
