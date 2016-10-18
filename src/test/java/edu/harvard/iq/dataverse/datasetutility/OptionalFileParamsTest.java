/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 
 * @author rmp553
 */
public class OptionalFileParamsTest {
    
    public OptionalFileParamsTest() {
    }
    
    /**
     * Good Json Description
     */
    @Test
    public void test_01_jsonDescriptionGood() throws DataFileTagException {

        msgt("test_01_jsonDescription");

        String val = "A new file";
        String jsonParams = "{\"description\": \"" + val + "\"}";
        
        OptionalFileParams instance = new OptionalFileParams(jsonParams);

        assertEquals(instance.getDescription(), val);
        assertNull(instance.getTags());
        assertNull(instance.getFileDataTags());

    }
    
    /**
     * Good Json Description
     */
    @Test
    public void test_02_jsonDescriptionNumeric() throws DataFileTagException {

        msgt("test_02_jsonDescriptionNumeric");

        String jsonParams = "{\"description\": 250 }";
        
        OptionalFileParams instance = new OptionalFileParams(jsonParams);

        assertEquals(instance.getDescription(), "250");
    
    }
    
    /**
     * Good Json Description
     */
    @Test
    public void test_03_jsonNull() throws DataFileTagException {

        msgt("test_03_jsonNull");

        //String val = "A new file";
        String jsonParams = null;
        
        OptionalFileParams instance = new OptionalFileParams(jsonParams);

        assertEquals(instance.getDescription(), null);
    
    }

    /**
     * Good Json Description
     */
    @Test
    public void test_04_jsonTagsGood() throws DataFileTagException {

        msgt("test_04_jsonTagsGood");

        String val = "A new file";
        String jsonParams = "{\"description\": \"A new file\", \"tags\": [\"dog\", \"cat\", \"mouse\"]}";
        
        OptionalFileParams instance = new OptionalFileParams(jsonParams);

        assertEquals(instance.getDescription(), val);
    
        List<String> expectedTags = Arrays.asList("dog", "cat", "mouse");
        assertEquals(expectedTags, instance.getTags());

        assertNull(instance.getFileDataTags());
        assertTrue(instance.hasTags());
        assertTrue(instance.hasDescription());
        assertFalse(instance.hasFileDataTags());
    
    }
    
    @Test
    public void test_05_jsonTabularTagsGood() throws DataFileTagException {

        msgt("test_05_jsonTabularTagsGood");

        String val = "A new file";
        String jsonParams = "{\"fileDataTags\": [\"Survey\", \"Event\", \"Panel\"], \"description\": \"A new file\"}";
        
        OptionalFileParams instance = new OptionalFileParams(jsonParams);

        assertEquals(instance.getDescription(), val);
    
        List<String> expectedTags = Arrays.asList("Survey", "Event", "Panel");
        assertEquals(expectedTags, instance.getFileDataTags());

        assertNull(instance.getTags());
        assertFalse(instance.hasTags());
        assertTrue(instance.hasDescription());
        assertTrue(instance.hasFileDataTags());
    }
    
    @Test
    public void test_06_jsonTabularTagsBad() throws DataFileTagException {

        msgt("test_06_jsonTabularTagsBad");

        String val = "A new file";
        String jsonParams = "{\"fileDataTags\": [\"Survey\", \"Event\", \"xPanel\"], \"description\": \"A new file\"}";
        
        try{
            OptionalFileParams instance = new OptionalFileParams(jsonParams);
        }catch(DataFileTagException ex){
           // msgt("ex: " + ex.getMessage());
            String errMsg = ResourceBundle.getBundle("Bundle").getString("file.addreplace.error.invalid_datafile_tag");
            msgt("errMsg: " + errMsg);
            assertTrue(ex.getMessage().startsWith(errMsg));
        }
    }
    
    
    @Test
    public void test_07_regularInstanceGood() throws DataFileTagException {

        msgt("test_07_regularInstanceGood");

        String val = "A new file";
        List<String> tags = Arrays.asList("dog", "cat", "mouse");
        List<String> fileDataTags = Arrays.asList("Survey", "Event", "Panel");
        
        OptionalFileParams instance = new OptionalFileParams(val,   
                                tags,
                                fileDataTags);

         assertEquals(val, instance.getDescription());
         assertEquals(tags, instance.getTags());
         assertEquals(fileDataTags, instance.getFileDataTags());
         
    }
    
    @Test
    public void test_08_regularInstanceGoodWithNulls() throws DataFileTagException {

        msgt("test_08_regularInstanceGoodWithNulls");

        String val = null;
        List<String> tags = null;//Arrays.asList("dog", "cat", "mouse");
        List<String> fileDataTags = Arrays.asList("Survey", "Event", "Panel");
        
        OptionalFileParams instance = new OptionalFileParams(val,   
                                tags,
                                fileDataTags);

         assertEquals(val, instance.getDescription());
         assertEquals(tags, instance.getTags());
         assertEquals(fileDataTags, instance.getFileDataTags());
         
    }
    
    @Test
    public void test_08_regularInstanceBadTabularTag() throws DataFileTagException {

        msgt("test_08_regularInstanceGoodWithNulls");

        String val = null;
        List<String> tags = null;//Arrays.asList("dog", "cat", "mouse");
        List<String> fileDataTags = Arrays.asList("Survey", "Event", "Panel");
        
        OptionalFileParams instance = new OptionalFileParams(val,   
                                tags,
                                fileDataTags);

         assertEquals(val, instance.getDescription());
         assertEquals(tags, instance.getTags());
         assertEquals(fileDataTags, instance.getFileDataTags());
         
    }
    
    
    private void msg(String s){
        System.out.println(s);
    }
    
    private void msgt(String s){
        msg("-------------------------------");
        msg(s);
        msg("-------------------------------");
    }
}

/*
Python for creating escaped JSON objects

import json; d = dict(description="A new file",tags=["dog", "cat",  "mouse"]); print json.dumps(json.dumps(d))

# result:
# "{\"description\": \"A new file\", \"tags\": [\"dog\", \"cat\", \"mouse\"]}"



d = dict(description="A new file",
        tabular_tags=["Survey", "Event",  "Panel"])
print json.dumps(json.dumps(d))

# "{\"fileDataTags\": [\"Survey\", \"Event\", \"Panel\"], \"description\": \"A new file\"}"


#import json; d = dict(tags=["dog", "cat",  "mouse"]); print json.dumps(json.dumps(d))

*/