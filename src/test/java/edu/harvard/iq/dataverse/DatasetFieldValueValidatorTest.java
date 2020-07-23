/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.sql.Timestamp;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DatasetFieldValueValidatorTest {
    
    @Mock
    ConstraintValidatorContext context;
    
    DatasetFieldValueValidator validator = new DatasetFieldValueValidator();
    
    // provides inputs to testShowVerifyEmailButton()
    private static Stream<Arguments> provider_testIsValid() {
        return Stream.of(
            // DATES
            Arguments.of(DatasetFieldType.FieldType.DATE, "1999AD", true),
            Arguments.of(DatasetFieldType.FieldType.DATE, "44BCE", true),
            Arguments.of(DatasetFieldType.FieldType.DATE, "2004-10-27", true),
            Arguments.of(DatasetFieldType.FieldType.DATE, "2002-08", true),
            Arguments.of(DatasetFieldType.FieldType.DATE, "[1999?]", true),
            Arguments.of(DatasetFieldType.FieldType.DATE, "Blergh", false),
            
            // FLOAT
            Arguments.of(DatasetFieldType.FieldType.FLOAT, "44", true),
            Arguments.of(DatasetFieldType.FieldType.FLOAT, "44 1/2", false),
            Arguments.of(DatasetFieldType.FieldType.FLOAT, "12.14", true),
            
            // INTEGER
            Arguments.of(DatasetFieldType.FieldType.INT, "44", true),
            Arguments.of(DatasetFieldType.FieldType.INT, "-44", true),
            Arguments.of(DatasetFieldType.FieldType.INT, "12.14", false),
    
            // URL
            Arguments.of(DatasetFieldType.FieldType.URL, "http://foo.bar", true),
            Arguments.of(DatasetFieldType.FieldType.URL, "foo.bar", false)
        );
    }
    
    @ParameterizedTest
    @MethodSource("provider_testIsValid")
    public void testIsValid(DatasetFieldType.FieldType type, String date, boolean expected) {
        // given
        DatasetFieldValue value = generateDatasetFieldValue(type);
        value.setValue(date);
        
        // when
        boolean result = validator.isValid(value, context);
        
        // then
        assertEquals(expected, result);
    }
    
    // provides inputs to testShowVerifyEmailButton()
    private static Stream<Arguments> provider_testIsValidForRegex() {
        return Stream.of(
            Arguments.of("asdfg", true),
            Arguments.of("asdfgX", false),
            Arguments.of("asdf", false)
        );
    }
    
    @ParameterizedTest
    @MethodSource("provider_testIsValidForRegex")
    public void testIsValidForRegex(String test, boolean expected) {
        // given
        DatasetFieldValue value = generateDatasetFieldValue(DatasetFieldType.FieldType.TEXT);
        value.getDatasetField().getDatasetFieldType().setValidationFormat("^[a-zA-Z ]{5,5}$");
        value.setValue(test);
    
        // when
        boolean result = validator.isValid(value, context);
    
        // then
        assertEquals(expected, result);
    }

    @Test
    public void testIsValidAuthorIdentifierOrcid() {
        DatasetFieldValueValidator validator = new DatasetFieldValueValidator();
        Pattern pattern = DatasetAuthor.getValidPattern(DatasetAuthor.REGEX_ORCID);
        assertTrue(validator.isValidAuthorIdentifier("0000-0002-1825-0097", pattern));
        // An "X" at the end of an ORCID is less common but still valid.
        assertTrue(validator.isValidAuthorIdentifier("0000-0002-1694-233X", pattern));
        assertFalse(validator.isValidAuthorIdentifier("0000 0002 1825 0097", pattern));
        assertFalse(validator.isValidAuthorIdentifier(" 0000-0002-1825-0097", pattern));
        assertFalse(validator.isValidAuthorIdentifier("0000-0002-1825-0097 ", pattern));
        assertFalse(validator.isValidAuthorIdentifier("junk", pattern));
    }

    @Test
    public void testIsValidAuthorIdentifierIsni() {
        DatasetFieldValueValidator validator = new DatasetFieldValueValidator();
        Pattern pattern = DatasetAuthor.getValidPattern(DatasetAuthor.REGEX_ISNI);
        assertTrue(validator.isValidAuthorIdentifier("0000000121032683", pattern));
        assertFalse(validator.isValidAuthorIdentifier("junk", pattern));
    }

    @Test
    public void testIsValidAuthorIdentifierLcna() {
        DatasetFieldValueValidator validator = new DatasetFieldValueValidator();
        Pattern pattern = DatasetAuthor.getValidPattern(DatasetAuthor.REGEX_LCNA);
        assertTrue(validator.isValidAuthorIdentifier("n82058243", pattern));
        assertTrue(validator.isValidAuthorIdentifier("foobar123", pattern));
        assertFalse(validator.isValidAuthorIdentifier("junk", pattern));
    }

    @Test
    public void testIsValidAuthorIdentifierViaf() {
        DatasetFieldValueValidator validator = new DatasetFieldValueValidator();
        Pattern pattern = DatasetAuthor.getValidPattern(DatasetAuthor.REGEX_VIAF);
        assertTrue(validator.isValidAuthorIdentifier("172389567", pattern));
        assertFalse(validator.isValidAuthorIdentifier("junk", pattern));
    }

    @Test
    public void testIsValidAuthorIdentifierGnd() {
        DatasetFieldValueValidator validator = new DatasetFieldValueValidator();
        Pattern pattern = DatasetAuthor.getValidPattern(DatasetAuthor.REGEX_GND);
        assertTrue(validator.isValidAuthorIdentifier("4079154-3", pattern));
        assertFalse(validator.isValidAuthorIdentifier("junk", pattern));
    }
    
    
    static DatasetFieldValue generateDatasetFieldValue(DatasetFieldType.FieldType type) {
        DatasetFieldValue value = new DatasetFieldValue();
        DatasetField df = new DatasetField();
        DatasetFieldType dft = new DatasetFieldType();
        
        dft.setFieldType(type);
        df.setDatasetFieldType(dft);
        value.setDatasetField(df);
        return value;
    }
}
