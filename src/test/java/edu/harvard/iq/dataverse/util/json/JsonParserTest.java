/*
 *  (C) Michael Bar-Sinai
 */

package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author michael
 */
public class JsonParserTest {
    
    MockDatasetFieldSvc mockDatasetFieldSvc = null;
    DatasetFieldType keywordType;
    DatasetFieldType descriptionType;
    DatasetFieldType subjectType;
    DatasetFieldType pubIdType;
    
    public JsonParserTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        mockDatasetFieldSvc = new MockDatasetFieldSvc();

        keywordType = mockDatasetFieldSvc.add(new DatasetFieldType("keyword", "primitive", true));
        descriptionType = mockDatasetFieldSvc.add( new DatasetFieldType("description", "primitive", false) );
        
        subjectType = mockDatasetFieldSvc.add(new DatasetFieldType("subject", "controlledVocabulary", true));
        subjectType.setAllowControlledVocabulary(true);
        subjectType.setControlledVocabularyValues( Arrays.asList( 
                new ControlledVocabularyValue(1l, "mgmt", subjectType),
                new ControlledVocabularyValue(2l, "law", subjectType),
                new ControlledVocabularyValue(3l, "cs", subjectType)
        ));
        
        pubIdType = mockDatasetFieldSvc.add(new DatasetFieldType("publicationIdType", "controlledVocabulary", false));
        pubIdType.setAllowControlledVocabulary(true);
        pubIdType.setControlledVocabularyValues( Arrays.asList( 
                new ControlledVocabularyValue(1l, "ark", pubIdType),
                new ControlledVocabularyValue(2l, "doi", pubIdType),
                new ControlledVocabularyValue(3l, "url", pubIdType)
        ));
        
        
    }
    
    @After
    public void tearDown() {
    }
    
    @Test
    public void testParseField_controlled_single() throws JsonParseException {
        JsonObject json = json("{\n" +
"            \"value\": \"ark\",\n" +
"            \"typeClass\": \"controlledVocabulary\",\n" +
"            \"multiple\": false,\n" +
"            \"typeName\": \"publicationIdType\"\n" +
"          }");
        
        JsonParser instance = new JsonParser( mockDatasetFieldSvc);
        
        DatasetField result = instance.parseField(json);
        
        assertEquals( pubIdType, result.getDatasetFieldType() );
        assertEquals( pubIdType.getControlledVocabularyValue("ark"), result.getSingleControlledVocabularyValue() );
        
    }

    @Test
    public void testParseField_controlled_multi() throws JsonParseException {
        JsonObject json = json("{\n" +
"            \"value\": [\n" +
"              \"mgmt\",\n" +
"              \"cs\"\n" +
"            ],\n" +
"            \"typeClass\": \"controlledVocabulary\",\n" +
"            \"multiple\": true,\n" +
"            \"typeName\": \"subject\"\n" +
"          }");
        
        JsonParser instance = new JsonParser( mockDatasetFieldSvc);
        
        DatasetField result = instance.parseField(json);
        
        assertEquals( subjectType, result.getDatasetFieldType() );
        assertEquals( Arrays.asList( subjectType.getControlledVocabularyValue("mgmt"),
                                     subjectType.getControlledVocabularyValue("cs")),
                      result.getControlledVocabularyValues()
        );
        
    }

    
    /**
     * Test of parsePrimitiveField method, of class JsonParser.
     */
    @Test
    public void testParseField_primitive_multi() throws JsonParseException {
        JsonObject json = json("{\n" +
"            \"value\": [\n" +
"              \"data\",\n" +
"              \"set\",\n" +
"              \"sample\"\n" +
"            ],\n" +
"            \"typeClass\": \"primitive\",\n" +
"            \"multiple\": true,\n" +
"            \"typeName\": \"keyword\"\n" +
"          }");
        
        JsonParser instance = new JsonParser( mockDatasetFieldSvc);
        
        DatasetField result = instance.parseField(json);
        
        assertEquals( keywordType, result.getDatasetFieldType() );
        assertEquals( Arrays.asList("data","set","sample"), result.getValues() );
    }
    
    @Test
    public void testParseField_primitive_single() throws JsonParseException {
        JsonObject json = json("{\n" +
"            \"value\": \"data\",\n" +
"            \"typeClass\": \"primitive\",\n" +
"            \"multiple\": false,\n" +
"            \"typeName\": \"description\"\n" +
"          }");
        
        JsonParser instance = new JsonParser( mockDatasetFieldSvc);
        
        DatasetField result = instance.parseField(json);
        
        assertEquals( descriptionType, result.getDatasetFieldType() );
        assertEquals( "data", result.getValue() );
        
    }
    
    @Test(expected=JsonParseException.class)
    public void testParseField_primitive_NEType() throws JsonParseException {
        JsonObject json = json("{\n" +
"            \"value\": \"data\",\n" +
"            \"typeClass\": \"primitive\",\n" +
"            \"multiple\": false,\n" +
"            \"typeName\": \"no-such-type\"\n" +
"          }");
        
        JsonParser instance = new JsonParser( mockDatasetFieldSvc);
        
        DatasetField result = instance.parseField(json);
        
        assertEquals( descriptionType, result.getDatasetFieldType() );
        assertEquals( "data", result.getValue() );
        
    }
    
    JsonObject json( String s ) {
        return Json.createReader( new StringReader(s) ).readObject();
    }
    
    static class MockDatasetFieldSvc extends DatasetFieldServiceBean {
        
        Map<String, DatasetFieldType> fieldTypes = new HashMap<>();
        
        public DatasetFieldType add( DatasetFieldType t ) {
            fieldTypes.put( t.getName(), t);
            return t;
        }
        
        @Override
        public DatasetFieldType findByName( String name ) {
            return fieldTypes.get(name);
        }
    }
}
