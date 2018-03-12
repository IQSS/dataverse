/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

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
public class DatasetFieldValidatorTest {
    
    public DatasetFieldValidatorTest() {
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
     * Test of isValid method, of class DatasetFieldValidator.
     */
    @Test
    public void testIsValid() {
        System.out.println("isValid");
        DatasetField value = new DatasetField();

        final ConstraintValidatorContext ctx =
            Mockito.mock(ConstraintValidatorContext.class);
        DatasetFieldValidator instance = new DatasetFieldValidator();
        //If its a template field it is always valid
        value.setTemplate(new Template());
        boolean expResult = true;
        boolean result = instance.isValid(value, ctx);
        assertEquals(expResult, result);
        
        
        //if not template and required
        value.setTemplate(null);
        DatasetVersion datasetVersion = new DatasetVersion();
        Dataset dataset = new Dataset();
        Dataverse dataverse = new Dataverse();
        dataset.setOwner(dataverse);
        datasetVersion.setDataset(dataset);
        value.setDatasetVersion(datasetVersion);
        
        DatasetFieldValue dfv = new DatasetFieldValue();
        DatasetFieldType dft = new DatasetFieldType("test", DatasetFieldType.FieldType.TEXT, false);
        dft.setRequired(true);
        value.setDatasetFieldType(dft);
        value.setSingleValue("");
        dfv.setValue("");
        result = instance.isValid(value, ctx);
        assertEquals(false, result);
        
        //Fill in a value - should be valid now....
        value.setSingleValue("value");
        result = instance.isValid(value, ctx);
        assertEquals(true, result);
        
        //if not required - can be blank
        dft.setRequired(false);
        value.setSingleValue("");
        result = instance.isValid(value, ctx);
        assertEquals(true, result);
    }
    
}
