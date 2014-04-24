package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

/**
 * Parses JSON objects into domain objects.
 * @author michael
 */
public class JsonParser {
    
    DatasetFieldServiceBean datasetFieldSvc;

    public JsonParser(DatasetFieldServiceBean datasetFieldSvc) {
        this.datasetFieldSvc = datasetFieldSvc;
    }
    
    public DatasetField parseField( JsonObject json ) throws JsonParseException {
        DatasetField ret = new DatasetField();
        DatasetFieldType type = datasetFieldSvc.findByName(json.getString("typeName"));
        if ( type == null ) {
            throw new JsonParseException("Can't find field type named '" + json.getString("typeName") + "'");
        }
        ret.setDatasetFieldType(type);
        
        
        if ( type.isCompound() ) {
            List<DatasetFieldCompoundValue> vals = parseCompoundValue(type, json);
            for ( DatasetFieldCompoundValue dsfcv : vals ) {
                dsfcv.setParentDatasetField(ret);
            }
            ret.setDatasetFieldCompoundValues(vals);
            
        } else if ( type.isControlledVocabulary() ) {
            List<ControlledVocabularyValue> vals = parseControlledVocabularyValue(type, json);
            for ( ControlledVocabularyValue cvv : vals ) {
                cvv.setDatasetFieldType(type);
            }
            ret.setControlledVocabularyValues(vals);
            
        } else {
            // primitive
            List<DatasetFieldValue> values = parsePrimitiveValue( json );
            for ( DatasetFieldValue val : values ) {
                val.setDatasetField(ret);
            }
            ret.setDatasetFieldValues(values);
        }
        
        return ret;
    }
    
    public List<DatasetFieldCompoundValue> parseCompoundValue( DatasetFieldType compoundType, JsonObject json ) throws JsonParseException {
        
        if ( json.getBoolean("multiple") ) {
            List<DatasetFieldCompoundValue> vals = new LinkedList<>();
            
            for ( JsonArray arr : json.getJsonArray("value").getValuesAs(JsonArray.class) ) {
                DatasetFieldCompoundValue cv = new DatasetFieldCompoundValue();
                List<DatasetField> fields = new LinkedList<>();
                for ( JsonObject childFieldJson : arr.getValuesAs(JsonObject.class) ) {
                    DatasetField f = parseField( childFieldJson );
                    f.setParentDatasetFieldCompoundValue(cv);
                    fields.add( f );
                }
                cv.setChildDatasetFields(fields);
                vals.add(cv);
            }
            
            return vals;
            
        } else {
            
            DatasetFieldCompoundValue cv = new DatasetFieldCompoundValue();
            List<DatasetField> fields = new LinkedList<>();
            for ( JsonObject childFieldJson : json.getJsonArray("value").getValuesAs(JsonObject.class) ) {
                DatasetField f = parseField( childFieldJson );
                f.setParentDatasetFieldCompoundValue(cv);
                fields.add( f );
            }
            cv.setChildDatasetFields(fields);
            return Collections.singletonList(cv);
        }
    }
    
    public List<DatasetFieldValue> parsePrimitiveValue( JsonObject json ) throws JsonParseException {
        
        List<DatasetFieldValue> vals = new LinkedList<>();
        if ( json.getBoolean("multiple") ) {
            for ( JsonString val : json.getJsonArray("value").getValuesAs(JsonString.class) ) {
                DatasetFieldValue datasetFieldValue = new DatasetFieldValue();
                datasetFieldValue.setDisplayOrder( vals.size()-1 );
                datasetFieldValue.setValue( val.getString() );
                vals.add( datasetFieldValue );
            }
            
        } else {
            DatasetFieldValue datasetFieldValue = new DatasetFieldValue();
            datasetFieldValue.setValue( json.getString("value") );
            vals.add(datasetFieldValue);
        }

        return vals;
    }
    
    public List<ControlledVocabularyValue> parseControlledVocabularyValue( DatasetFieldType cvvType, JsonObject json ) throws JsonParseException {
        if ( json.getBoolean("multiple") ) {
            List<ControlledVocabularyValue> vals = new LinkedList<>();
            for ( JsonString strVal : json.getJsonArray("value").getValuesAs(JsonString.class) ) {
                String strValue = strVal.getString();
                ControlledVocabularyValue cvv = cvvType.getControlledVocabularyValue( strValue );
                if ( cvv==null ) {
                    throw new JsonParseException("Value '" + strValue + "' does not exist in type '" + cvvType.getName() + "'");
                }
                vals.add(cvv);
            }
            return vals;
            
        } else {
            String strValue = json.getString("value");
            ControlledVocabularyValue cvv = cvvType.getControlledVocabularyValue( strValue );
            if ( cvv==null ) {
                throw new JsonParseException("Value '" + strValue + "' does not exist in type '" + cvvType.getName() + "'");
            }
            return Collections.singletonList(cvv);
        }
    }
    
}
