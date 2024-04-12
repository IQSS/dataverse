package edu.harvard.iq.dataverse.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author skraffmi
 */
public class URLValidatorTest {
    
    final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    
    public static Stream<Arguments> stdUrlExamples() {
        return Stream.of(
            Arguments.of(true, null),
            Arguments.of(false, ""),
            Arguments.of(true, "https://twitter.com"),
            Arguments.of(true, "http://foobar.com:9101"),
            Arguments.of(true, "ftp://user@foobar.com"),
            Arguments.of(false, "cnn.com"),
            Arguments.of(false, "smb://user@foobar.com")
        );
    }
    
    @ParameterizedTest
    @MethodSource("stdUrlExamples")
    public void testIsURLValid(boolean expected, String url) {
        assertEquals(expected, URLValidator.isURLValid(url));
    }
    
    /**
     * This is a simple test class, as we defined the annotation for fields only.
     */
    private final class SubjectClass {
        @ValidateURL
        String url;
        
        SubjectClass(String url) {
            this.url = url;
        }
    }
    
    @ParameterizedTest
    @MethodSource("stdUrlExamples")
    public void testConstraint(boolean expected, String url) {
        // given
        URLValidatorTest.SubjectClass sut = new URLValidatorTest.SubjectClass(url);
        
        //when
        Set<ConstraintViolation<URLValidatorTest.SubjectClass>> violations = validator.validate(sut);
        
        // then
        assertEquals(expected ? 0 : 1, violations.size());
        violations.stream().findFirst().ifPresent( c -> {
            assertTrue(c.getMessage().contains(url)); });
    }
    
    public static Stream<Arguments> fancyUrlExamples() {
        return Stream.of(
            Arguments.of(true, null),
            Arguments.of(false, ""),
            Arguments.of(false, "https://twitter.com"),
            Arguments.of(true, "http://foobar.com:9101"),
            Arguments.of(false, "ftp://user@foobar.com"),
            Arguments.of(false, "cnn.com"),
            Arguments.of(true, "smb://user@foobar.com")
        );
    }
    
    /**
     * This is a simple test class like above, but with a scheme given
     */
    private final class SubjectSchemeClass {
        @ValidateURL(schemes = {"http", "smb"})
        String url;
    
        SubjectSchemeClass(String url) {
            this.url = url;
        }
    }
    
    @ParameterizedTest
    @MethodSource("fancyUrlExamples")
    public void testConstraintWithSchemes(boolean expected, String url) {
        // given
        URLValidatorTest.SubjectSchemeClass sut = new URLValidatorTest.SubjectSchemeClass(url);
        
        //when
        Set<ConstraintViolation<URLValidatorTest.SubjectSchemeClass>> violations = validator.validate(sut);
        
        // then
        assertEquals(expected ? 0 : 1, violations.size());
        violations.stream().findFirst().ifPresent( c -> {
            assertTrue(c.getMessage().contains(url)); });
    }
}
