/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.Set;
import java.util.regex.Pattern;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 *
 * @author skraffmi
 */
public class DatasetFieldValueValidatorTest {

    /**
     * Test of isValid method, of class DatasetFieldValueValidator.
     */
    @Test
    public void testIsValid() {
        System.out.println("isValid");
        DatasetFieldValue value = new DatasetFieldValue();
        DatasetField df = new DatasetField();
        DatasetFieldType dft = new DatasetFieldType();
        dft.setFieldType(DatasetFieldType.FieldType.TEXT);
        //Test Text against regular expression that takes a 5 character string
        dft.setValidationFormat("^[a-zA-Z ]{5,5}$");
        df.setDatasetFieldType(dft);
        value.setDatasetField(df);
        value.setValue("asdfg");
        final ConstraintValidatorContext ctx =
            Mockito.mock(ConstraintValidatorContext.class);
        DatasetFieldValueValidator instance = new DatasetFieldValueValidator();
        boolean expResult = true;
        boolean result = instance.isValid(value, ctx);
        assertEquals(expResult, result);
        
        //Make string too long - should fail.
        value.setValue("asdfgX");
        result = instance.isValid(value, ctx);
        assertFalse(result);
        
        //Make string too long - should fail.
        value.setValue("asdf");
        result = instance.isValid(value, ctx);
        assertFalse(result);
        
        //Now lets try Dates
        dft.setFieldType(DatasetFieldType.FieldType.DATE);   
        dft.setValidationFormat(null);
        value.setValue("1999AD");
        result = instance.isValid(value, ctx);
        assertTrue(result); 
        
        value.setValue("44BCE");
        result = instance.isValid(value, ctx);
        assertTrue(result); 
        
        value.setValue("2004-10-27");
        result = instance.isValid(value, ctx);
        assertTrue(result); 
        
        value.setValue("2002-08");
        result = instance.isValid(value, ctx);
        assertTrue(result);  
        
        value.setValue("[1999?]");
        result = instance.isValid(value, ctx);
        assertTrue(result); 
        
        value.setValue("Blergh");
        result = instance.isValid(value, ctx);
        assertFalse(result);  
        
        //Float
        dft.setFieldType(DatasetFieldType.FieldType.FLOAT); 
        value.setValue("44");
        result = instance.isValid(value, ctx);
        assertTrue(result);
        
        value.setValue("44 1/2");
        result = instance.isValid(value, ctx);
        assertFalse(result);
        
        //Integer
        dft.setFieldType(DatasetFieldType.FieldType.INT); 
        value.setValue("44");
        result = instance.isValid(value, ctx);
        assertTrue(result);
        
        value.setValue("-44");
        result = instance.isValid(value, ctx);
        assertTrue(result);
        
        value.setValue("12.14");
        result = instance.isValid(value, ctx);
        assertFalse(result);
    }

    final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    
    @ParameterizedTest
    @CsvSource(
        {
            "true, https://www.google.com",
            "true, http://google.com",
            "true, https://do-not-exist-123-123.com/",
            "true, ftp://somesite.com",
            "false, google.com",
            "false, git@github.com:IQSS/dataverse.git"
        }
    )
    public void testInvalidURL(boolean expected, String url) {
        // given
        String fieldName = "testField";
        
        DatasetField field = new DatasetField();
        field.setDatasetFieldType(new DatasetFieldType(fieldName, DatasetFieldType.FieldType.URL, false));
        DatasetFieldValue sut = new DatasetFieldValue(field);
        sut.setValue(url);
        
        // when
        Set<ConstraintViolation<DatasetFieldValue>> violations = validator.validate(sut);
        
        // then
        assertEquals(expected, violations.size() < 1);
        violations.stream().findFirst().ifPresent(c -> {
            assertTrue(c.getMessage().startsWith(fieldName + " " + url + " "));
            assertTrue(c.getMessage().contains("not"));
            assertTrue(c.getMessage().contains("URL"));
        });
    }
    
    @Test
    public void testInvalidEmail() {
        // given
        String fieldName = "testField";
        String invalidMail = "myinvalidmail";
        
        DatasetField field = new DatasetField();
        field.setDatasetFieldType(new DatasetFieldType(fieldName, DatasetFieldType.FieldType.EMAIL, false));
        DatasetFieldValue sut = new DatasetFieldValue(field);
        sut.setValue(invalidMail);
        
        // when
        Set<ConstraintViolation<DatasetFieldValue>> violations = validator.validate(sut);
        
        // then
        assertTrue(violations.size() == 1);
        violations.stream().findFirst().ifPresent(c -> {
            assertTrue(c.getMessage().startsWith(fieldName + " " + invalidMail + " "));
            assertTrue(c.getMessage().contains("not"));
            assertTrue(c.getMessage().contains("email"));
        });
    }
    @Test
    public void testBoundingBoxValidity() {
        // valid tests
        assertTrue(DatasetFieldValueValidator.validateBoundingBox("-180", "180", "90", "-90"));
        assertTrue(DatasetFieldValueValidator.validateBoundingBox("0", "0", "0", "0"));

        // invalid tests
        assertTrue(!DatasetFieldValueValidator.validateBoundingBox("-180", null, "90", null));
        assertTrue(!DatasetFieldValueValidator.validateBoundingBox(null, "180", null, "90"));
        assertTrue(!DatasetFieldValueValidator.validateBoundingBox("-180", "180", "90", "junk"));
        assertTrue(!DatasetFieldValueValidator.validateBoundingBox("45", "40", "90", "0"));
        assertTrue(!DatasetFieldValueValidator.validateBoundingBox("360", "0", "90", "-90"));
        assertTrue(!DatasetFieldValueValidator.validateBoundingBox("", "", "", ""));
        assertTrue(!DatasetFieldValueValidator.validateBoundingBox(null, null, null, null));
    }
}
