package edu.harvard.iq.dataverse.validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EMailValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    
    public static Stream<Arguments> emailExamples() {
        return Stream.of(
            Arguments.of("true", "pete@mailinator.com"),
            Arguments.of("false", " leadingWhitespace@mailinator.com"),
            Arguments.of("false", "trailingWhitespace@mailinator.com "),
            Arguments.of("false", "elisah.da mota@example.com"),
            Arguments.of("false", "pete1@mailinator.com;pete2@mailinator.com"),

            /*
             * These examples are all from https://randomuser.me and seem to be
             * valid according to
             * http://sphinx.mythic-beasts.com/~pdw/cgi-bin/emailvalidate (except رونیکا.محمدخان@example.com)
             */
            Arguments.of("true", "michélle.pereboom@example.com"),
            Arguments.of("true", "begüm.vriezen@example.com"),
            Arguments.of("true", "lótus.gonçalves@example.com"),
            Arguments.of("true", "lótus.gonçalves@éxample.com"),
            Arguments.of("true", "begüm.vriezen@example.cologne"),
            Arguments.of("true", "رونیکا.محمدخان@example.com"),
            Arguments.of("false", "lótus.gonçalves@example.cóm"),
            Arguments.of("false", "dora@.com"),
            Arguments.of("false", ""),
            // Null means "no value given" - that's valid, you'll need to use @NotNull or similar to enforce a value!
            Arguments.of("true", null),
    
            // add tests for 4601
            Arguments.of("true", "blah@wiso.uni-unc.de"),
            Arguments.of("true", "foo@essex.co.uk"),
            Arguments.of("true", "jack@bu.cloud")
        );
    }
    
    @ParameterizedTest
    @MethodSource("emailExamples")
    public void testIsEmailValid(boolean expected, String mail) {
        assertEquals(expected, EMailValidator.isEmailValid(mail));
    }
    
    /**
     * This is a simple test class, as we defined the annotation for fields only.
     */
    private final class SubjectClass {
        @ValidateEmail
        String mail;
    
        SubjectClass(String mail) {
            this.mail = mail;
        }
    }
    
    @ParameterizedTest
    @MethodSource("emailExamples")
    public void testConstraint(boolean expected, String mail) {
        // given
        SubjectClass sut = new SubjectClass(mail);
        
        //when
        Set<ConstraintViolation<SubjectClass>> violations = validator.validate(sut);
        
        // then
        assertEquals(expected ? 0 : 1, violations.size());
        violations.stream().findFirst().ifPresent( c -> {
            assertTrue(c.getMessage().contains(mail)); });
    }
}
