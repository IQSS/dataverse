package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DatasetAuthor;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

/**
 * Parses JSON objects into domain objects.
 * @author michael
 */
public class JsonParser {
    
    private final DateFormat dateFormat = new SimpleDateFormat( JsonPrinter.TIME_FORMAT_STRING );
    
    DatasetFieldServiceBean datasetFieldSvc;
    MetadataBlockServiceBean blockService;

    public JsonParser(DatasetFieldServiceBean datasetFieldSvc, MetadataBlockServiceBean blockService) {
        this.datasetFieldSvc = datasetFieldSvc;
        this.blockService = blockService;
    }
    
    public DatasetVersion parseDatasetVersion( JsonObject obj ) throws JsonParseException {
        try {
            DatasetVersion dsv = new DatasetVersion();
            
            dsv.setArchiveNote( obj.getString("archiveNote", null) );
            dsv.setDeaccessionLink( obj.getString("deaccessionLink", null) );
            dsv.setVersion( parseLong(obj.getString("version")) );
            dsv.setVersionNumber( parseLong(obj.getString("versionNumber")) );
            dsv.setMinorVersionNumber( parseLong(obj.getString("minorVersionNumber")) );
            dsv.setId( parseLong(obj.getString("id")) );
            
            String versionStateStr = obj.getString("versionState");
            if ( versionStateStr != null ) {
                dsv.setVersionState( DatasetVersion.VersionState.valueOf(versionStateStr) );
            }
            
            dsv.setReleaseTime( parseDate(obj.getString("releaseDate")) );
            dsv.setLastUpdateTime( parseDate(obj.getString("lastUpdateTime")) );
            dsv.setCreateTime( parseDate(obj.getString("createTime")) );
            dsv.setArchiveTime( parseDate(obj.getString("archiveTime")) );
            
            dsv.setDatasetFields( parseMetadataBlocks(obj.getJsonObject("metadataBlocks")) );
            
            // parse authors
            JsonArray authorsJson = obj.getJsonArray("authors");
            List<DatasetAuthor> authors = new ArrayList<>( authorsJson.size() );
            for ( JsonObject authorJson : authorsJson.getValuesAs(JsonObject.class) ) {
                DatasetAuthor author = new DatasetAuthor();
                author.setAffiliation( parseField( authorJson.getJsonObject("affiliation")) );
                author.setIdType( authorJson.getString("idType") );
                author.setIdValue( authorJson.getString("idValue"));
                author.setDisplayOrder( parsePrimitiveInt(authorJson.getString("displayOrder"), 0) );
                author.setName( parseField( authorJson.getJsonObject("name")) );
                
                authors.add( author );
                author.setDatasetVersion(dsv);
            }
            dsv.setDatasetAuthors(authors);
            
            // parse distributors
            // CONTPOINT 
            
            return dsv;
            
        } catch (ParseException ex) {
            throw new JsonParseException("Error parsing date:" + ex.getMessage(), ex);
        } catch ( NumberFormatException ex ) {
            throw new JsonParseException("Error parsing number:" + ex.getMessage(), ex);
        }
    }
    
    public List<DatasetField> parseMetadataBlocks( JsonObject json ) throws JsonParseException {
        Set<String> keys = json.keySet();
        List<DatasetField> fields = new LinkedList<>();
        
        for ( String blockName : keys ) {
            blockService.findByName(blockName);
            JsonObject blockJson = json.getJsonObject(blockName);
            JsonArray fieldsJson = blockJson.getJsonArray("fields");
            for ( JsonObject fieldJson : fieldsJson.getValuesAs(JsonObject.class) ) {
                fields.add( parseField(fieldJson) );
            }
        }
        
        return fields;
    }
    
    public DatasetField parseField( JsonObject json ) throws JsonParseException {
        if ( json == null ) return null;
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
    
    Date parseDate( String str ) throws ParseException {
        return str==null ? null : dateFormat.parse(str);
    }
    
    Long parseLong( String str ) throws NumberFormatException {
        return (str==null) ? null : Long.valueOf(str);
    }
    
    int parsePrimitiveInt( String str, int defaultValue ) {
        return str==null ? defaultValue : Integer.parseInt(str);
    }
    
}
