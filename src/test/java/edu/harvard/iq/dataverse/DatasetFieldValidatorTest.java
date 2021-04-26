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
        
        Dataverse dataverse = new Dataverse();
        Dataset dataset = new Dataset();
        dataset.setOwner(dataverse);
        DatasetVersion datasetVersion = new DatasetVersion();        
        datasetVersion.setDataset(dataset);        
           
        testPrimitiveDatasetField("test isValid() on template field", true, "", null, true);
        testPrimitiveDatasetField("test isValid() for required primitive with empty value", true, "", datasetVersion, false);
        testPrimitiveDatasetField("test isValid() for required primitive with nonempty value",  true, "test", datasetVersion, true);
        testPrimitiveDatasetField("test isValid() for not required primitive", false, "", datasetVersion, true);

        // the compound tests are defined by parent/child1 required and child1/child2 values
        testCompoundDatasetField("test isValid() for false/false compound with empty/empty children", false, false, "", "", datasetVersion, true);
        testCompoundDatasetField("test isValid() for true/true compound with empty/empty children", true, true, "", "", datasetVersion, false);
        testCompoundDatasetField("test isValid() for true/true compound with empty/nonempty children", true, true, "", "test", datasetVersion, false);
        testCompoundDatasetField("test isValid() for true/true compound with nonempty/empty children", true, true, "test", "", datasetVersion, true);
        testCompoundDatasetField("test isValid() for false/true compound with empty/empty children", false, true, "", "", datasetVersion, true);
        testCompoundDatasetField("test isValid() for false/true compound with empty/nonempty children", false, true, "", "test", datasetVersion, false);
        testCompoundDatasetField("test isValid() for false/true compound with nonempty/empty children", false, true, "test", "", datasetVersion, true);        
    }
    
    private void testPrimitiveDatasetField(String test, boolean required, String value,
        DatasetVersion dsv, boolean expectedOutcome) {
        DatasetFieldType primitiveDSFType = new DatasetFieldType("primitive", DatasetFieldType.FieldType.TEXT, false);
        primitiveDSFType.setRequired(required); 
        
        DatasetField testDatasetField = new DatasetField();
        if (dsv != null) {
            testDatasetField.setDatasetVersion(dsv);
        } else {
            testDatasetField.setTemplate(new Template());
        }
        testDatasetField.setDatasetFieldType(primitiveDSFType);
        testDatasetField.setSingleValue(value);
        
        DatasetFieldValidator datasetFieldValidator = new DatasetFieldValidator();
        assertEquals( test, expectedOutcome, datasetFieldValidator.isValid(testDatasetField, constraintValidatorContext));
       
    }
      
    private void testCompoundDatasetField(String test, boolean requiredParent, boolean requiredChild1,
            String valueChild1, String valueChild2, DatasetVersion dsv, boolean expectedOutcome) {    

        DatasetFieldType parentDSFType = new DatasetFieldType("parent", DatasetFieldType.FieldType.TEXT, false);
        DatasetFieldType child1DSFType = new DatasetFieldType("child1", DatasetFieldType.FieldType.TEXT, false);
        DatasetFieldType child2DSFType = new DatasetFieldType("child2", DatasetFieldType.FieldType.TEXT, false);

        parentDSFType.setRequired(requiredParent); 
        child1DSFType.setRequired(requiredChild1); 
        child2DSFType.setRequired(false);        
        
        DatasetField parentDatasetField = new DatasetField();
        parentDatasetField.setDatasetVersion(dsv);
        parentDatasetField.setDatasetFieldType(parentDSFType);
        
        DatasetFieldCompoundValue compound = new DatasetFieldCompoundValue();
        compound.setParentDatasetField(parentDatasetField);

        DatasetField child1DatasetField = new DatasetField();
        child1DatasetField.setParentDatasetFieldCompoundValue(compound);
        compound.getChildDatasetFields().add(child1DatasetField);
        child1DatasetField.setDatasetFieldType(child1DSFType);
        child1DatasetField.setSingleValue(valueChild1);    

        DatasetField child2DatasetField = new DatasetField();
        child2DatasetField.setParentDatasetFieldCompoundValue(compound);
        compound.getChildDatasetFields().add(child2DatasetField);        
        child2DatasetField.setDatasetFieldType(child2DSFType);
        child2DatasetField.setSingleValue(valueChild2);   
        

        DatasetFieldValidator datasetFieldValidator = new DatasetFieldValidator();
        assertEquals( test, expectedOutcome, datasetFieldValidator.isValid(child1DatasetField, constraintValidatorContext));
    }
    
}
