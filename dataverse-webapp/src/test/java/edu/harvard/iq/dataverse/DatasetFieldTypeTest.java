/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author skraffmi
 */
public class DatasetFieldTypeTest {

    public DatasetFieldTypeTest() {
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
     * Test of setInclude method, of class DatasetFieldType.
     */


    /**
     * Test of isSanitizeHtml method, of class DatasetFieldType.
     */
    @Test
    public void testIsSanitizeHtml() {
        System.out.println("isSanitizeHtml");
        DatasetFieldType instance = new DatasetFieldType();
        instance.setFieldType(FieldType.TEXT);
        Boolean result = instance.isSanitizeHtml();
        assertFalse(result);

        //if textbox then sanitize - allow tags
        instance.setFieldType(FieldType.TEXTBOX);
        result = instance.isSanitizeHtml();
        assertEquals(true, result);

        //if textbox then don't sanitize - allow tags
        instance.setFieldType(FieldType.EMAIL);
        result = instance.isSanitizeHtml();
        assertEquals(false, result);

        //URL, too
        instance.setFieldType(FieldType.URL);
        result = instance.isSanitizeHtml();
        assertEquals(true, result);
    }

    @Test
    public void testIsEscapeOutputText() {
        System.out.println("testIsEscapeOutputText");
        DatasetFieldType instance = new DatasetFieldType();
        instance.setFieldType(FieldType.TEXT);
        Boolean result = instance.isEscapeOutputText();
        assertTrue(result);

        //if Disaplay Format includes a link then don't escape
        instance.setDisplayFormat("'<a target=\"_blank\" href=\"http://www.rcsb.org/pdb/explore/explore.do?structureId=#VALUE\">PDB (RCSB) #VALUE</a>'");
        result = instance.isEscapeOutputText();
        assertFalse(result);

        //if textbox then sanitize - allow tags
        instance.setFieldType(FieldType.TEXTBOX);
        result = instance.isEscapeOutputText();
        assertFalse(result);

        //if textbox then don't sanitize - allow tags
        instance.setFieldType(FieldType.EMAIL);
        result = instance.isEscapeOutputText();
        assertTrue(result);

        //URL, too
        instance.setFieldType(FieldType.URL);
        result = instance.isEscapeOutputText();
        assertEquals(false, result);

    }

    @Test
    public void isParentAllowsMutlipleValues_parentReturnsTrue() {
        //given
        DatasetFieldType parentDsf = new DatasetFieldType();
        parentDsf.setAllowMultiples(true);

        DatasetFieldType datasetFieldType = new DatasetFieldType();
        datasetFieldType.setParentDatasetFieldType(parentDsf);

        //when
        boolean parentAllowsMutlipleValues = datasetFieldType.isThisOrParentAllowsMultipleValues();

        //then
        Assert.assertTrue(parentAllowsMutlipleValues);
    }

    @Test
    public void isParentAllowsMutlipleValues_parentReturnsFalse() {
        //given
        DatasetFieldType parentDsf = new DatasetFieldType();
        parentDsf.setAllowMultiples(false);

        DatasetFieldType datasetFieldType = new DatasetFieldType();
        datasetFieldType.setParentDatasetFieldType(parentDsf);

        //when
        boolean parentAllowsMutlipleValues = datasetFieldType.isThisOrParentAllowsMultipleValues();

        //then
        Assert.assertFalse(parentAllowsMutlipleValues);
    }

}
