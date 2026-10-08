/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.search.SolrField;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author skraffmi
 */
public class DatasetFieldTypeTest {
    
    public DatasetFieldTypeTest() {
    }
    
    @BeforeAll
    public static void setUpClass() {
    }
    
    @AfterAll
    public static void tearDownClass() {
    }
    
    @BeforeEach
    public void setUp() {
    }
    
    @AfterEach
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
        instance.setFieldType(DatasetFieldType.FieldType.TEXT);
        Boolean result = instance.isSanitizeHtml();
        assertFalse(result);
               
        //if textbox then sanitize - allow tags
        instance.setFieldType(DatasetFieldType.FieldType.TEXTBOX);
        result = instance.isSanitizeHtml();
        assertTrue(result);
        
        //if textbox then don't sanitize - allow tags
        instance.setFieldType(DatasetFieldType.FieldType.EMAIL);
        result = instance.isSanitizeHtml();
        assertFalse(result);
        
        //URL, too
        instance.setFieldType(DatasetFieldType.FieldType.URL);
        result = instance.isSanitizeHtml();
        assertTrue(result);
    }
    
    @Test
    public void testIsEscapeOutputText(){
                System.out.println("testIsEscapeOutputText");
        DatasetFieldType instance = new DatasetFieldType();
        instance.setFieldType(DatasetFieldType.FieldType.TEXT);
        assertTrue(instance.isEscapeOutputText());
        assertTrue(instance.isEscapeFieldText());
        
        //if Display Format includes a link or <b> or <br/>, escape the values, but don't escape on the page
        instance.setDisplayFormat("'<a target=\"_blank\" href=\"http://www.rcsb.org/pdb/explore/explore.do?structureId=#VALUE\">PDB (RCSB) #VALUE</a>'");
        assertTrue(instance.isEscapeOutputText());  
        assertFalse(instance.isEscapeFieldText());
        
        //if textbox then sanitize - allow tags
        instance.setFieldType(DatasetFieldType.FieldType.TEXTBOX);
        assertFalse( instance.isEscapeOutputText());
        assertFalse( instance.isEscapeFieldText());

        //if textbox then don't sanitize - allow tags
        instance.setFieldType(DatasetFieldType.FieldType.EMAIL);
        assertTrue(instance.isEscapeOutputText());
        assertTrue(instance.isEscapeFieldText());
        
        //URL, too
        instance.setFieldType(DatasetFieldType.FieldType.URL);
        assertFalse(instance.isEscapeOutputText());
        assertFalse(instance.isEscapeFieldText());
        
    }
    
    @Test
    public void testGetSolrField(){
        
        DatasetFieldType instance = new DatasetFieldType();
        instance.setFieldType(DatasetFieldType.FieldType.DATE);
        SolrField solrField = instance.getSolrField();       
        assertEquals(SolrField.SolrType.DATE, solrField.getSolrType());
        
        instance.setFieldType(DatasetFieldType.FieldType.EMAIL);
        solrField = instance.getSolrField();       
        assertEquals(SolrField.SolrType.EMAIL, solrField.getSolrType());
        DatasetFieldType parent = new DatasetFieldType();
        parent.setAllowMultiples(true);
        instance.setParentDatasetFieldType(parent);
        solrField = instance.getSolrField();
        assertTrue(solrField.isAllowedToBeMultivalued());
        
    }


    
}
