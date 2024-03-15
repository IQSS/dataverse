/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileCategory;
import edu.harvard.iq.dataverse.DataFileTag;
import edu.harvard.iq.dataverse.FileMetadata;

import java.util.Arrays;
import java.util.List;

import edu.harvard.iq.dataverse.util.BundleUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
        assertNull(instance.getCategories());
        assertNull(instance.getDataFileTags());

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

        assertNull(instance.getDescription());
    
    }

    /**
     * Good Json Description
     */
    @Test
    public void test_04_jsonTagsGood() throws DataFileTagException {

        msgt("test_04_jsonTagsGood");

        String val = "A new file";
        String jsonParams = "{\"description\": \"A new file\", \"categories\": [\"dog\", \"cat\", \"mouse\"]}";
        
        OptionalFileParams instance = new OptionalFileParams(jsonParams);

        assertEquals(instance.getDescription(), val);
    
        List<String> expectedCategories = Arrays.asList("dog", "cat", "mouse");
        assertEquals(expectedCategories, instance.getCategories());

        assertNull(instance.getDataFileTags());
        assertTrue(instance.hasCategories());
        assertTrue(instance.hasDescription());
        assertFalse(instance.hasFileDataTags());
    
    }
    
    @Test
    public void test_05_jsonTabularTagsGood() throws DataFileTagException {

        msgt("test_05_jsonTabularTagsGood");

        String val = "A new file";
        String jsonParams = "{\"dataFileTags\": [\"Survey\", \"Event\", \"Panel\"], \"description\": \"A new file\"}";
        
        OptionalFileParams instance = new OptionalFileParams(jsonParams);

        assertEquals(instance.getDescription(), val);
    
        List<String> expectedTags = Arrays.asList("Survey", "Event", "Panel");
        assertEquals(expectedTags, instance.getDataFileTags());

        assertNull(instance.getCategories());
        assertFalse(instance.hasCategories());
        assertTrue(instance.hasDescription());
        assertTrue(instance.hasFileDataTags());
    }
    
    @Test
    public void test_06_jsonTabularTagsBad() throws DataFileTagException {

        msgt("test_06_jsonTabularTagsBad");

        String val = "A new file";
        String jsonParams = "{\"dataFileTags\": [\"Survey\", \"Event\", \"xPanel\"], \"description\": \"A new file\"}";
        
        try{
            OptionalFileParams instance = new OptionalFileParams(jsonParams);
        }catch(DataFileTagException ex){
           // msgt("ex: " + ex.getMessage());
            String errMsg = BundleUtil.getStringFromBundle("file.addreplace.error.invalid_datafile_tag");
            msgt("errMsg: " + errMsg);
            assertTrue(ex.getMessage().startsWith(errMsg));
        }
    }
    
    
    @Test
    public void test_07_regularInstanceGood() throws DataFileTagException {

        msgt("test_07_regularInstanceGood");

        String val = "A new file";
        List<String> categories = Arrays.asList("dog", " dog ", "cat", "mouse", "dog ");
        List<String> dataFileTags = Arrays.asList("Survey", "Event", "Panel");
        
        OptionalFileParams instance = new OptionalFileParams(val,   
                                categories,
                                dataFileTags);

         assertEquals(val, instance.getDescription());
         assertEquals( Arrays.asList("dog", "cat", "mouse"), instance.getCategories());
         assertEquals(dataFileTags, instance.getDataFileTags());
         
    }
    
    @Test
    public void test_08_regularInstanceGoodWithNulls() throws DataFileTagException {

        msgt("test_08_regularInstanceGoodWithNulls");

        String val = null;
        List<String> categories = null;//Arrays.asList("dog", "cat", "mouse");
        List<String> dataFileTags = Arrays.asList("Survey", "Survey", "Event", "Panel", "Survey", " ");
        
        OptionalFileParams instance = new OptionalFileParams(val,   
                                categories,
                                dataFileTags);

         assertEquals(val, instance.getDescription());
         assertEquals(categories, instance.getCategories());
         assertEquals(Arrays.asList("Survey", "Event", "Panel"), instance.getDataFileTags());
         
    }
    
    @Test
    public void test_09_unusedParamsGood() throws DataFileTagException {

        msgt("test_08_regularInstanceGoodWithNulls");
      
        String jsonParams = "{\"forceReplace\": \"unused within OptionalFileParams\", \"oldFileId\": \"unused within OptionalFileParams\", \"description\": null, \"unusedParam1\": \"haha\", \"categories\": []}";
        
        OptionalFileParams instance = new OptionalFileParams(jsonParams);
        
        assertNull(instance.getDescription());
        assertFalse(instance.hasDescription());

        assertNull(instance.getCategories());
        assertFalse(instance.hasCategories());

        assertNull(instance.getDataFileTags());
        assertFalse(instance.hasFileDataTags());

    }

    @Test
    public void test_10_emptyString() throws DataFileTagException {

        msgt("test_10_emptyString");

        String jsonParams = "";

        OptionalFileParams instance = new OptionalFileParams(jsonParams);

        assertNull(instance.getDescription());
        assertFalse(instance.hasDescription());

        assertNull(instance.getCategories());
        assertFalse(instance.hasCategories());

        assertNull(instance.getDataFileTags());
        assertFalse(instance.hasFileDataTags());

    }
    
    @Test
    public void testGetOptionalFileParamsFromJson() throws DataFileTagException {
        FileMetadata fm = new FileMetadata();
        DataFile df = new DataFile();
        DataFileTag dft = new DataFileTag();
        dft.setType(DataFileTag.TagType.Panel);
        df.addTag(dft);
        fm.setDataFile(df);
        fm.setDescription("description");
        fm.setDirectoryLabel("/foo/bar");
        fm.setLabel("testFileName");
        DataFileCategory fmc = new DataFileCategory();
        fmc.setName("category");
        fm.addCategory(fmc);
        
        JsonObject fmJson = fm.asGsonObject(true);
        
        OptionalFileParams instance = new OptionalFileParams(fmJson.toString());
        assertEquals(fm.getDescription(), instance.getDescription());
        assertEquals(fm.getDirectoryLabel(), instance.getDirectoryLabel());
        assertEquals(fm.getLabel(), instance.getLabel());
        assertEquals(dft.getTypeLabel(), instance.getDataFileTags().get(0));
        assertEquals(fmc.getName(), instance.getCategories().get(0));
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
d = dict(description="A new file"
        ,categories=["dog", "cat",  "mouse"])
print json.dumps(json.dumps(d))

# result:
# "{\"description\": \"A new file\", \"categories\": [\"dog\", \"cat\", \"mouse\"]}"



d = dict(description="A new file",
        tabular_tags=["Survey", "Event",  "Panel"])
print json.dumps(json.dumps(d))

# "{\"fileDataTags\": [\"Survey\", \"Event\", \"Panel\"], \"description\": \"A new file\"}"


#import json; d = dict(tags=["dog", "cat",  "mouse"]); print json.dumps(json.dumps(d))


import json
d = dict(description="A new file",
    categories=["dog", "cat",  "mouse"],
    unusedParam1="haha",
    forceReplace="unused within OptionalFileParams", 
    oldFileId="unused within OptionalFileParams"
)
print json.dumps(json.dumps(d))


*/