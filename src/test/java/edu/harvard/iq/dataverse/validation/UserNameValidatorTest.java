package edu.harvard.iq.dataverse.validation;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserNameValidatorTest {
    
    public static Stream<Arguments> usernameExamples() {
        return Stream.of(
            // good usernames
            Arguments.of(true, "sarah"),
            Arguments.of(true, ".-_5Arah_-."),
            // dont allow accents
            Arguments.of(false, "àèìòùÀÈÌÒÙáéíóúýÁÉÍÓÚÝâêîôûÂÊÎÔÛãñõÃÑÕäëïöüÿÄËÏÖÜŸçÇßØøÅåÆæœ"),
            // dont allow chinese characters
            Arguments.of(false, "谁日吧爸好"),
            // dont allow middle white space
            Arguments.of(false, "sarah f"),
            // dont allow leading white space
            Arguments.of(false, " sarah"),
            // dont allow trailing white space
            Arguments.of(false, "sarah "),
            // dont allow symbols
            Arguments.of(false, "sarah!"),
            Arguments.of(false, "sarah?"),
            Arguments.of(false, "sarah:("),
            Arguments.of(false, "💲🅰️®️🅰️🚧"),
            // only allow between 2 and 60 characters
            Arguments.of(false, "q"),
            Arguments.of(true, "q2"),
            Arguments.of(false, "q2jsalfhjopiwurtiosfhkdhasjkdhfgkfhkfrhnefcn4cqonroclmooi4oiqwhrfq4jrlqhaskdalwehrlwhflhklasdjfglq0kkajfelirhilwhakjgv"),
            Arguments.of(false, "q2jsalfhjopiwurtiosfhkdhasj!1! !#fhkfrhnefcn4cqonroclmooi4oiqwhrfq4jrlqhaskdalwehrlwhflhklasdjfglq0kkajfelirhilwhakjgv"),
            Arguments.of(false, ""),
            Arguments.of(false, null)
        );
    }
    
    @ParameterizedTest
    @MethodSource("usernameExamples")
    public void testUsernames(boolean expected, String username) {
        assertEquals(expected, UserNameValidator.isUserNameValid(username));
    }
    
    /**
     * This is a simple test class, as we defined the annotation for fields only.
     */
    private final class SubjectClass {
        @ValidateUserName
        String username;
        
        SubjectClass(String username) {
            this.username = username;
        }
    }
    
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    
    @ParameterizedTest
    @MethodSource("usernameExamples")
    public void testConstraint(boolean expected, String username) {
        // given
        UserNameValidatorTest.SubjectClass sut = new UserNameValidatorTest.SubjectClass(username);
        
        //when
        Set<ConstraintViolation<UserNameValidatorTest.SubjectClass>> violations = validator.validate(sut);
        
        // then
        assertEquals(expected, violations.size() < 1);
        // TODO: one might add tests for the two violations (size and illegal chars) here...
    }
    
}
