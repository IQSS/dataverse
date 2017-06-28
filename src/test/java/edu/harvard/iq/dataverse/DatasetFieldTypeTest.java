/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.search.SolrField;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.faces.model.SelectItem;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
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
        Boolean expResult = true;
        instance.setFieldType(DatasetFieldType.FieldType.TEXT);
        Boolean result = instance.isSanitizeHtml();
        assertEquals(expResult, result);
        
        //if text then escape
        result = instance.isSanitizeHtml();
        assertEquals(true, result);
        
        //if textbox then don't sanitize - allow tags
        instance.setFieldType(DatasetFieldType.FieldType.TEXTBOX);
        result = instance.isSanitizeHtml();
        assertEquals(false, result);
        
        //if textbox then don't sanitize - allow tags
        instance.setFieldType(DatasetFieldType.FieldType.EMAIL);
        result = instance.isSanitizeHtml();
        assertEquals(true, result);
        
        //URL, too
        instance.setFieldType(DatasetFieldType.FieldType.URL);
        result = instance.isSanitizeHtml();
        assertEquals(false, result);
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
        assertEquals(true, solrField.isAllowedToBeMultivalued());
        
    }


    
}
