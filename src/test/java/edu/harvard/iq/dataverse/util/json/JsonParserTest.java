/*
 *  (C) Michael Bar-Sinai
 */

package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldType.FieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author michael
 */
public class JsonParserTest {
    
    MockDatasetFieldSvc datasetFieldTypeSvc = null;
    MockSettingsSvc settingsSvc = null;
    DatasetFieldType keywordType;
    DatasetFieldType descriptionType;
    DatasetFieldType subjectType;
    DatasetFieldType pubIdType;
    DatasetFieldType compoundSingleType;
    JsonParser sut;
    
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
        datasetFieldTypeSvc = new MockDatasetFieldSvc();

        keywordType = datasetFieldTypeSvc.add(new DatasetFieldType("keyword", FieldType.TEXT, true));
        descriptionType = datasetFieldTypeSvc.add( new DatasetFieldType("description", FieldType.TEXTBOX, false) );
        
        subjectType = datasetFieldTypeSvc.add(new DatasetFieldType("subject", FieldType.TEXT, true));
        subjectType.setAllowControlledVocabulary(true);
        subjectType.setControlledVocabularyValues( Arrays.asList( 
                new ControlledVocabularyValue(1l, "mgmt", subjectType),
                new ControlledVocabularyValue(2l, "law", subjectType),
                new ControlledVocabularyValue(3l, "cs", subjectType)
        ));
        
        pubIdType = datasetFieldTypeSvc.add(new DatasetFieldType("publicationIdType", FieldType.TEXT, false));
        pubIdType.setAllowControlledVocabulary(true);
        pubIdType.setControlledVocabularyValues( Arrays.asList( 
                new ControlledVocabularyValue(1l, "ark", pubIdType),
                new ControlledVocabularyValue(2l, "doi", pubIdType),
                new ControlledVocabularyValue(3l, "url", pubIdType)
        ));
        
        compoundSingleType = datasetFieldTypeSvc.add(new DatasetFieldType("coordinate", FieldType.TEXT, true));
        Set<DatasetFieldType> childTypes = new HashSet<>();
        childTypes.add( datasetFieldTypeSvc.add(new DatasetFieldType("lat", FieldType.TEXT, false)) );
        childTypes.add( datasetFieldTypeSvc.add(new DatasetFieldType("lon", FieldType.TEXT, false)) );
        
        for ( DatasetFieldType t : childTypes ) {
            t.setParentDatasetFieldType(compoundSingleType);
        }
        compoundSingleType.setChildDatasetFieldTypes(childTypes);
        sut = new JsonParser(datasetFieldTypeSvc, null, settingsSvc);
    }
    
    @Test 
    public void testCompoundRepeatsRoundtrip() throws JsonParseException {
        DatasetField expected = new DatasetField();
        expected.setDatasetFieldType( datasetFieldTypeSvc.findByName("coordinate") );
        List<DatasetFieldCompoundValue> vals = new LinkedList<>();
        for ( int i=0; i<5; i++ ) {
            DatasetFieldCompoundValue val = new DatasetFieldCompoundValue();
            val.setParentDatasetField(expected);
            val.setChildDatasetFields( Arrays.asList(latLonField("lat", Integer.toString(i*10)), latLonField("lon", Integer.toString(3+i*10))));
            vals.add( val );
        }
        expected.setDatasetFieldCompoundValues(vals);
        
        JsonObject json = JsonPrinter.json(expected);
        
        System.out.println("json = " + json);
        
        DatasetField actual = sut.parseField(json);
        
        assertFieldsEqual(expected, actual);
    }
    
    DatasetField latLonField( String latLon, String value ) {
        DatasetField retVal = new DatasetField();
        retVal.setDatasetFieldType( datasetFieldTypeSvc.findByName(latLon));
        retVal.setDatasetFieldValues( Collections.singletonList( new DatasetFieldValue(retVal, value)));
        return retVal;
    }
    
    @Test 
    public void testControlledVocalNoRepeatsRoundTrip() throws JsonParseException {
        DatasetField expected = new DatasetField();
        DatasetFieldType fieldType = datasetFieldTypeSvc.findByName("publicationIdType");
        expected.setDatasetFieldType( fieldType );
        expected.setControlledVocabularyValues( Collections.singletonList( fieldType.getControlledVocabularyValue("ark")));
        JsonObject json = JsonPrinter.json(expected);
        
        DatasetField actual = sut.parseField(json);
        assertFieldsEqual(expected, actual);
        
    }
    
    @Test 
    public void testControlledVocalRepeatsRoundTrip() throws JsonParseException {
        DatasetField expected = new DatasetField();
        DatasetFieldType fieldType = datasetFieldTypeSvc.findByName("subject");
        expected.setDatasetFieldType( fieldType );
        expected.setControlledVocabularyValues( Arrays.asList( fieldType.getControlledVocabularyValue("mgmt"),
                 fieldType.getControlledVocabularyValue("law"),
                 fieldType.getControlledVocabularyValue("cs")));
        
        JsonObject json = JsonPrinter.json(expected);      
        DatasetField actual = sut.parseField(json);
        assertFieldsEqual(expected, actual);
        
    }
    
    
    @Test(expected=JsonParseException.class)
     public void testChildValidation() throws JsonParseException {
        // This Json String is a compound field that contains the wrong
        // fieldType as a child ("description" is not a child of "coordinate").
        // It should throw a JsonParseException when it encounters the invalid child.
        String compoundString = "{ " +
"            \"typeClass\": \"compound\"," +
"            \"multiple\": true," +
"            \"typeName\": \"coordinate\"," +
"            \"value\": [" +
"              {" +
"                \"description\": {" +
"                  \"value\": \"0\"," +
"                  \"typeClass\": \"primitive\"," +
"                  \"multiple\": false," +
"                  \"typeName\": \"description\"" +
"                }" +
"              }" +
"            ]" +
"            " +
"          }"; 
   
        String text = compoundString;
        JsonReader jsonReader = Json.createReader(new StringReader(text));
        JsonObject obj = jsonReader.readObject();

        sut.parseField(obj);
       }
    
    
    @Test
    public void testPrimitiveNoRepeatesFieldRoundTrip() throws JsonParseException {
        DatasetField expected = new DatasetField();
        expected.setDatasetFieldType( datasetFieldTypeSvc.findByName("description") );
        expected.setDatasetFieldValues( Collections.singletonList(new DatasetFieldValue(expected, "This is a description value")) );
        JsonObject json = JsonPrinter.json(expected);
        
        DatasetField actual = sut.parseField(json);
        
        assertFieldsEqual(actual, expected);
    }
    
    @Test
    public void testPrimitiveRepeatesFieldRoundTrip() throws JsonParseException {
        DatasetField expected = new DatasetField();
        expected.setDatasetFieldType( datasetFieldTypeSvc.findByName("keyword") );
        expected.setDatasetFieldValues( Arrays.asList(new DatasetFieldValue(expected, "kw1"),
                new DatasetFieldValue(expected, "kw2"),
                new DatasetFieldValue(expected, "kw3")) );
        JsonObject json = JsonPrinter.json(expected);
        
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
            
        } else if ( ex.getDatasetFieldType().isControlledVocabulary() ) {
            List<ControlledVocabularyValue> exVals = ex.getControlledVocabularyValues();
            List<ControlledVocabularyValue> actVals = act.getControlledVocabularyValues();
            if ( exVals.size() != actVals.size() ) return false;
            Iterator<ControlledVocabularyValue> exItr = exVals.iterator();
            for ( ControlledVocabularyValue actVal : actVals ) {
                ControlledVocabularyValue exVal = exItr.next();
                if ( ! exVal.getId().equals(actVal.getId()) ) {
                    return false;
                }
            }
            return true;
            
        } else if ( ex.getDatasetFieldType().isCompound() ) {
            List<DatasetFieldCompoundValue> exVals = ex.getDatasetFieldCompoundValues();
            List<DatasetFieldCompoundValue> actVals = act.getDatasetFieldCompoundValues();
            if ( exVals.size() != actVals.size() ) return false;
            Iterator<DatasetFieldCompoundValue> exItr = exVals.iterator();
            for ( DatasetFieldCompoundValue actVal : actVals ) {
                DatasetFieldCompoundValue exVal = exItr.next();
                Iterator<DatasetField> exChildItr = exVal.getChildDatasetFields().iterator();
                Iterator<DatasetField> actChildItr = actVal.getChildDatasetFields().iterator();
                while( exChildItr.hasNext() ) {
                    assertFieldsEqual(exChildItr.next(), actChildItr.next());
                }
            }
            return true;
            
        }
        
        throw new IllegalArgumentException("Unknown dataset field type '" + ex.getDatasetFieldType() + "'");
    }
    
    static class MockSettingsSvc extends SettingsServiceBean {
        @Override
        public String getValueForKey( Key key, String defaultValue ) {
            if (key.equals(SettingsServiceBean.Key.Authority)) {
                return "10.5072/FK2";
            } else if (key.equals(SettingsServiceBean.Key.Protocol)) {
                return "doi";
            } else if( key.equals(SettingsServiceBean.Key.DoiSeparator)) {
                return "/";
            }
             return null;
        }
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
        public DatasetFieldType findByNameOpt(String name) {
           return findByName(name);
        }
        
        @Override
        public ControlledVocabularyValue findControlledVocabularyValueByDatasetFieldTypeAndStrValue(DatasetFieldType dsft, String strValue, boolean lenient) {
            ControlledVocabularyValue cvv = new ControlledVocabularyValue();
            cvv.setDatasetFieldType(dsft);
            cvv.setStrValue(strValue);
            return cvv;
        }
 
    }
}
