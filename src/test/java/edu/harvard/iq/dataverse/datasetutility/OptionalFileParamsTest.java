/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import java.util.Arrays;
import java.util.List;
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
        assertNull(instance.getTabularTags());

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

        assertNull(instance.getTabularTags());

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

import json
d = dict(description="A new file",
        tags=["dog", "cat",  "mouse"])
print json.dumps(json.dumps(d))

# result:
# "{\"description\": \"A new file\", \"tags\": [\"dog\", \"cat\", \"mouse\"]}"
*/