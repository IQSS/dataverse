/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.util.regex.Pattern;
import javax.validation.ConstraintValidatorContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mockito.Mockito;

/**
 *
 * @author skraffmi
 */
public class DatasetFieldValueValidatorTest {
    
    
    public DatasetFieldValueValidatorTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }


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
        assertEquals(false, result);
        
        //Make string too long - should fail.
        value.setValue("asdf");
        result = instance.isValid(value, ctx);
        assertEquals(false, result);
        
        //Now lets try Dates
        dft.setFieldType(DatasetFieldType.FieldType.DATE);   
        dft.setValidationFormat(null);
        value.setValue("1999AD");
        result = instance.isValid(value, ctx);
        assertEquals(true, result); 
        
        value.setValue("44BCE");
        result = instance.isValid(value, ctx);
        assertEquals(true, result); 
        
        value.setValue("2004-10-27");
        result = instance.isValid(value, ctx);
        assertEquals(true, result); 
        
        value.setValue("2002-08");
        result = instance.isValid(value, ctx);
        assertEquals(true, result);  
        
        value.setValue("[1999?]");
        result = instance.isValid(value, ctx);
        assertEquals(true, result); 
        
        value.setValue("Blergh");
        result = instance.isValid(value, ctx);
        assertEquals(false, result);  
        
        //Float
        dft.setFieldType(DatasetFieldType.FieldType.FLOAT); 
        value.setValue("44");
        result = instance.isValid(value, ctx);
        assertEquals(true, result);
        
        value.setValue("44 1/2");
        result = instance.isValid(value, ctx);
        assertEquals(false, result);
        
        //Integer
        dft.setFieldType(DatasetFieldType.FieldType.INT); 
        value.setValue("44");
        result = instance.isValid(value, ctx);
        assertEquals(true, result);
        
        value.setValue("-44");
        result = instance.isValid(value, ctx);
        assertEquals(true, result);
        
        value.setValue("12.14");
        result = instance.isValid(value, ctx);
        assertEquals(false, result);
        
        //URL
        dft.setFieldType(DatasetFieldType.FieldType.URL); 
        value.setValue("http://cnn.com");
        result = instance.isValid(value, ctx);
        assertEquals(true, result);
        
        
        value.setValue("espn.com");
        result = instance.isValid(value, ctx);
        assertEquals(false, result);
        
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

}
