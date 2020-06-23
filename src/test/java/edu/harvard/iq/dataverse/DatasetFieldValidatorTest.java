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

    final ConstraintValidatorContext constraintValidatorContext = Mockito.mock(ConstraintValidatorContext.class);
    
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
        DatasetField dataSetField = new DatasetField();
        DatasetFieldValidator datasetFieldValidator = new DatasetFieldValidator();

        // if it is a template field it is always valid
        dataSetField.setTemplate(new Template());
        boolean expResult = true;
        boolean result = datasetFieldValidator.isValid(dataSetField, constraintValidatorContext);
        assertEquals("test isValid() on template field", expResult, result);

        // if not a template field and required
        dataSetField.setTemplate(null);
        DatasetVersion datasetVersion = new DatasetVersion();
        Dataset dataset = new Dataset();
        Dataverse dataverse = new Dataverse();
        dataset.setOwner(dataverse);
        datasetVersion.setDataset(dataset);
        dataSetField.setDatasetVersion(datasetVersion);

        DatasetFieldValue datasetFieldValue = new DatasetFieldValue();
        DatasetFieldType datasetFieldType = new DatasetFieldType("test", DatasetFieldType.FieldType.TEXT, false);
        datasetFieldType.setRequired(true);
        dataSetField.setDatasetFieldType(datasetFieldType);
        dataSetField.setSingleValue("");
        datasetFieldValue.setValue("");
        result = datasetFieldValidator.isValid(dataSetField, constraintValidatorContext);
        assertEquals("test isValid() if not template field and required with empty DataSetField", false, result);

        // fill in a value - the required constraint is satisfied now
        dataSetField.setSingleValue("value");
        result = datasetFieldValidator.isValid(dataSetField, constraintValidatorContext);
        assertEquals("test isValid() if not template field and required with non-empty DataSetField", true, result);

        // if not required the field can be blank
        datasetFieldType.setRequired(false);
        dataSetField.setSingleValue("");
        result = datasetFieldValidator.isValid(dataSetField, constraintValidatorContext);
        assertEquals("test isValid() if not template field and not required", true, result);
    }
    
}
