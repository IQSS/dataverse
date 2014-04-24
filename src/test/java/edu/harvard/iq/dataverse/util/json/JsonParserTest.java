/*
 *  (C) Michael Bar-Sinai
 */

package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    DatasetFieldType compoundSingleType;
    
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
        
        compoundSingleType = mockDatasetFieldSvc.add(new DatasetFieldType("coordinate", "compound", false));
        Set<DatasetFieldType> childTypes = new HashSet<>();
        childTypes.add( mockDatasetFieldSvc.add(new DatasetFieldType("lat", "primitive", false)) );
        childTypes.add( mockDatasetFieldSvc.add(new DatasetFieldType("lon", "primitive", false)) );
        
        for ( DatasetFieldType t : childTypes ) {
            t.setParentDatasetFieldType(compoundSingleType);
        }
        compoundSingleType.setChildDatasetFieldTypes(childTypes);
        
    }
    
    @After
    public void tearDown() {
    }
 
    @Test
    public void testParseField_compound_single() throws JsonParseException {
        JsonObject json = json("{\n" +
                "  \"value\": [\n" +
                "  {\n" +
                "    \"value\": \"500.3\",\n" +
                "    \"typeClass\": \"primitive\",\n" +
                "    \"multiple\": false,\n" +
                "    \"typeName\": \"lon\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"value\": \"300.3\",\n" +
                "      \"typeClass\": \"primitive\",\n" +
                "      \"multiple\": false,\n" +
                "      \"typeName\": \"lat\"\n" +
                "    }\n" +
                "    ],\n" +
                "    \"typeClass\": \"compound\",\n" +
                "    \"multiple\": false,\n" +
                "    \"typeName\": \"coordinate\"\n" +
                "}");
        
        JsonParser instance = new JsonParser( mockDatasetFieldSvc, null );
        
        DatasetField result = instance.parseField(json);
        
        assertEquals( compoundSingleType, result.getDatasetFieldType() );
        DatasetFieldCompoundValue val = result.getDatasetFieldCompoundValues().get(0);
        DatasetField lon = new DatasetField();
        lon.setDatasetFieldType( mockDatasetFieldSvc.findByName("lon"));
        lon.setDatasetFieldValues( Collections.singletonList(new DatasetFieldValue(lon, "500.3")) );

        DatasetField lat = new DatasetField();
        lat.setDatasetFieldType( mockDatasetFieldSvc.findByName("lat"));
        lat.setDatasetFieldValues( Collections.singletonList(new DatasetFieldValue(lat, "300.3")) );
        
        List<DatasetField> actual = val.getChildDatasetFields();
        assertEquals( lon, actual.get(0) );
        assertEquals( lat, actual.get(1) );
    }
    
    @Test
    public void testParseField_controlled_single() throws JsonParseException {
        JsonObject json = json("{\n" +
"            \"value\": \"ark\",\n" +
"            \"typeClass\": \"controlledVocabulary\",\n" +
"            \"multiple\": false,\n" +
"            \"typeName\": \"publicationIdType\"\n" +
"          }");
        
        JsonParser instance = new JsonParser( mockDatasetFieldSvc, null );
        
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
        
        JsonParser instance = new JsonParser( mockDatasetFieldSvc, null );
        
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
        
        JsonParser instance = new JsonParser( mockDatasetFieldSvc, null );
        
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
        
        JsonParser instance = new JsonParser( mockDatasetFieldSvc, null );
        
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
        
        JsonParser instance = new JsonParser( mockDatasetFieldSvc, null );
        
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
