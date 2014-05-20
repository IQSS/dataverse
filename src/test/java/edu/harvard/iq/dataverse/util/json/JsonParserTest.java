/*
 *  (C) Michael Bar-Sinai
 */

package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author michael
 */
public class JsonParserTest {
    
    MockDatasetFieldSvc datasetFieldSvc = null;
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
        datasetFieldSvc = new MockDatasetFieldSvc();

        keywordType = datasetFieldSvc.add(new DatasetFieldType("keyword", "primitive", true));
        descriptionType = datasetFieldSvc.add( new DatasetFieldType("description", "primitive", false) );
        
        subjectType = datasetFieldSvc.add(new DatasetFieldType("subject", "controlledVocabulary", true));
        subjectType.setAllowControlledVocabulary(true);
        subjectType.setControlledVocabularyValues( Arrays.asList( 
                new ControlledVocabularyValue(1l, "mgmt", subjectType),
                new ControlledVocabularyValue(2l, "law", subjectType),
                new ControlledVocabularyValue(3l, "cs", subjectType)
        ));
        
        pubIdType = datasetFieldSvc.add(new DatasetFieldType("publicationIdType", "controlledVocabulary", false));
        pubIdType.setAllowControlledVocabulary(true);
        pubIdType.setControlledVocabularyValues( Arrays.asList( 
                new ControlledVocabularyValue(1l, "ark", pubIdType),
                new ControlledVocabularyValue(2l, "doi", pubIdType),
                new ControlledVocabularyValue(3l, "url", pubIdType)
        ));
        
        compoundSingleType = datasetFieldSvc.add(new DatasetFieldType("coordinate", "compound", false));
        Set<DatasetFieldType> childTypes = new HashSet<>();
        childTypes.add( datasetFieldSvc.add(new DatasetFieldType("lat", "primitive", false)) );
        childTypes.add( datasetFieldSvc.add(new DatasetFieldType("lon", "primitive", false)) );
        
        for ( DatasetFieldType t : childTypes ) {
            t.setParentDatasetFieldType(compoundSingleType);
        }
        compoundSingleType.setChildDatasetFieldTypes(childTypes);
        
    }
    
    @Test
    public void testPrimitiveNoRepeatesFieldRoundTrip() throws JsonParseException {
        DatasetField expected = new DatasetField();
        expected.setDatasetFieldType( datasetFieldSvc.findByName("description") );
        expected.setDatasetFieldValues( Collections.singletonList(new DatasetFieldValue(expected, "This is a description value")) );
        JsonObject json = JsonPrinter.json(expected);
        JsonParser sut = new JsonParser(datasetFieldSvc, null);
        System.out.println("json = " + json);
        DatasetField actual = sut.parseField(json);
        
        assertFieldsEqual(actual, expected);
    }
    
    @Test
    public void testPrimitiveRepeatesFieldRoundTrip() throws JsonParseException {
        DatasetField expected = new DatasetField();
        expected.setDatasetFieldType( datasetFieldSvc.findByName("keyword") );
        expected.setDatasetFieldValues( Arrays.asList(new DatasetFieldValue(expected, "kw1"),
                new DatasetFieldValue(expected, "kw2"),
                new DatasetFieldValue(expected, "kw3")) );
        JsonObject json = JsonPrinter.json(expected);
        JsonParser sut = new JsonParser(datasetFieldSvc, null);
        System.out.println("json = " + json);
        DatasetField actual = sut.parseField(json);
        
        assertFieldsEqual(actual, expected);
    }
    
    JsonObject json( String s ) {
        return Json.createReader( new StringReader(s) ).readObject();
    }
    
    public boolean assertFieldsEqual( DatasetField ex, DatasetField act ) {
        if ( ex == act ) return true;
        if ( (ex == null) ^ (act==null) ) return false;
        
        // type
        if ( ! ex.getDatasetFieldType().equals(act.getDatasetFieldType()) ) return false;
        if ( ex.getDatasetFieldType().isPrimitive() ) {
            List<DatasetFieldValue> exVals = ex.getDatasetFieldValues();
            List<DatasetFieldValue> actVals = act.getDatasetFieldValues();
            if ( exVals.size() != actVals.size() ) return false;
            Iterator<DatasetFieldValue> exItr = exVals.iterator();
            for ( DatasetFieldValue actVal : actVals ) {
                DatasetFieldValue exVal = exItr.next();
                if ( ! exVal.getValue().equals(actVal.getValue()) ) {
                    return false;
                }
            }
            return true;
        }
        return false; // not implemented, really.
    }
    
    static class MockDatasetFieldSvc extends DatasetFieldServiceBean {
        
        Map<String, DatasetFieldType> fieldTypes = new HashMap<>();
        long nextId = 1;
        public DatasetFieldType add( DatasetFieldType t ) {
            if ( t.getId()==null ) {
                t.setId( nextId++ );
            }
            fieldTypes.put( t.getName(), t);
            return t;
        }
        
        @Override
        public DatasetFieldType findByName( String name ) {
            return fieldTypes.get(name);
        }
        
        @Override
        public DatasetFieldType findByNameOpt( String name ) {
            return findByName( name );
        }
    }
}
