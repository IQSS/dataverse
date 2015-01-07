package edu.harvard.iq.dataverse.util.json;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseContact;
import edu.harvard.iq.dataverse.MetadataBlockServiceBean;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddressRange;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

/**
 * Parses JSON objects into domain objects.
 * @author michael
 */
public class JsonParser {
    
    private final DateFormat timeFormat = new SimpleDateFormat( JsonPrinter.TIME_FORMAT_STRING );
    private final DateFormat dateFormat = new SimpleDateFormat( JsonPrinter.DATE_FORMAT_STRING );
  
    DatasetFieldServiceBean datasetFieldSvc;
    MetadataBlockServiceBean blockService;

    public JsonParser(DatasetFieldServiceBean datasetFieldSvc, MetadataBlockServiceBean blockService) {
        this.datasetFieldSvc = datasetFieldSvc;
        this.blockService = blockService;
    }
    
    public static Dataverse parseDataverse( JsonObject jobj ) throws JsonParseException {
        Dataverse dv = new Dataverse();

        dv.setAlias( getMandatoryString(jobj, "alias") );
        dv.setName( getMandatoryString(jobj, "name") );
        dv.setDescription( jobj.getString("description", null));
        dv.setPermissionRoot( jobj.getBoolean("permissionRoot", false) );
        dv.setFacetRoot( jobj.getBoolean("facetRoot", false) );
        if ( jobj.containsKey("dataverseContacts") ) {
            JsonArray dvContacts = jobj.getJsonArray("dataverseContacts");
            int i=0;
            List<DataverseContact> dvContactList = new LinkedList<>();
            for ( JsonValue jsv : dvContacts ) {
                DataverseContact dvc = new DataverseContact(dv);
                dvc.setContactEmail( getMandatoryString((JsonObject)jsv,"contactEmail") );
                dvc.setDisplayOrder(i++);
                dvContactList.add( dvc );
            }
            dv.setDataverseContacts(dvContactList);
        }
        
        return dv;
    }
    
    private static String getMandatoryString( JsonObject jobj, String name ) throws JsonParseException {
        if ( jobj.containsKey(name) ) {
            return jobj.getString(name);
        }
        throw new JsonParseException("Field " + name + " is mandatory");
    }
    
    public IpGroup parseIpGroup( JsonObject obj ) {
        IpGroup retVal = new IpGroup();
        
        if ( obj.containsKey("id") ) {
            retVal.setId( Long.valueOf(obj.getString("id")) );
        }
        retVal.setName( obj.getString("name",null) );
        retVal.setDescription( obj.getString("description",null) );
        retVal.setAlias( obj.getString("alias", null) );
        
        JsonArray rangeArray = obj.getJsonArray("ranges");
        for ( JsonValue range : rangeArray ) {
            if ( range.getValueType() == JsonValue.ValueType.ARRAY ) {
                JsonArray rr = (JsonArray)range;
                retVal.add( IpAddressRange.make(IpAddress.valueOf(rr.getString(0)),
                                            IpAddress.valueOf(rr.getString(1))) );
                
            }
        }
        
        return retVal;
    }
    
    public DatasetVersion parseDatasetVersion(JsonObject obj) throws JsonParseException {
        return parseDatasetVersion(obj, new DatasetVersion());
    }
    
    public Dataset parseDataset(JsonObject obj) throws JsonParseException {
        Dataset dataset = new Dataset();
        // EMK TODO: 
        // set datasetvalues from obj; Do we need to set anything else?
        dataset.setAuthority(obj.getString("authority", null));
        dataset.setIdentifier(obj.getString("identifier", null));
        dataset.setProtocol(obj.getString("protocol", null));
        dataset.setDoiSeparator(obj.getString("doiSeparator",null));
        DatasetVersion dsv = parseDatasetVersion(obj.getJsonObject("datasetVersion"));
        LinkedList<DatasetVersion> versions = new LinkedList<>();
        versions.add(dsv);
        dsv.setDataset(dataset);

        dataset.setVersions(versions);
        return dataset;
    }
    
    public DatasetVersion parseDatasetVersion( JsonObject obj, DatasetVersion dsv ) throws JsonParseException {
        try {
           
            String archiveNote = obj.getString("archiveNote", null);
            if ( archiveNote != null ) dsv.setArchiveNote( archiveNote );
            
            dsv.setDeaccessionLink( obj.getString("deaccessionLink", null) );
            dsv.setVersionNumber( parseLong(obj.getString("versionNumber", null)) );
            dsv.setMinorVersionNumber( parseLong(obj.getString("minorVersionNumber", null)) );
            dsv.setId( parseLong(obj.getString("id", null)) );
            
            String versionStateStr = obj.getString("versionState", null);
            if ( versionStateStr != null ) {
                dsv.setVersionState( DatasetVersion.VersionState.valueOf(versionStateStr) );
            }
            
            dsv.setReleaseTime( parseDate(obj.getString("releaseDate", null)) );
            dsv.setLastUpdateTime( parseTime(obj.getString("lastUpdateTime", null)) );
            dsv.setCreateTime( parseTime(obj.getString("createTime", null)) );
            dsv.setArchiveTime( parseTime(obj.getString("archiveTime", null)) );
            
            dsv.setDatasetFields( parseMetadataBlocks(obj.getJsonObject("metadataBlocks")) );
            
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
        DatasetFieldType type = datasetFieldSvc.findByNameOpt(json.getString("typeName",""));

        if ( type == null ) {
            throw new JsonParseException("Can't find type '" + json.getString("typeName","") +"'");
        }
        if (type.isAllowMultiples() != json.getBoolean("multiple")) {
            throw new JsonParseException("incorrect multiple   for field " +json.getString("typeName",""));
        }
        if (type.isCompound() && !json.getString("typeClass").equals("compound")) {
                    throw new JsonParseException("incorrect  typeClass for field " +json.getString("typeName","")+", should be compound.");  
        }      
        if (!type.isControlledVocabulary() && type.isPrimitive() && !json.getString("typeClass").equals("primitive")) {
                    throw new JsonParseException("incorrect  typeClass for field: " +json.getString("typeName","")+", should be primitive");  
        } 
        if (type.isControlledVocabulary() && !json.getString("typeClass").equals("controlledVocabulary")) {
                    throw new JsonParseException("incorrect  typeClass for field " +json.getString("typeName","")+", should be controlledVocabulary");  
        }   
        ret.setDatasetFieldType(type);
        
        try {
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
        } catch ( Exception e ) {
            throw new JsonParseException("Exception while parsing field of type: " + ret.getDatasetFieldType().getName(), e );
        }
        return ret;
    }
    
    public List<DatasetFieldCompoundValue> parseCompoundValue( DatasetFieldType compoundType, JsonObject json ) throws JsonParseException {
        
        if ( json.getBoolean("multiple") ) {
            List<DatasetFieldCompoundValue> vals = new LinkedList<>();
            
            for ( JsonObject obj : json.getJsonArray("value").getValuesAs(JsonObject.class) ) {
                DatasetFieldCompoundValue cv = new DatasetFieldCompoundValue();
                List<DatasetField> fields = new LinkedList<>();
                for ( String fieldName: obj.keySet() ) {
                    JsonObject childFieldJson = obj.getJsonObject(fieldName);
                    DatasetField f = parseField( childFieldJson );
                    if (!compoundType.getChildDatasetFieldTypes().contains(f.getDatasetFieldType())) {
                        throw new JsonParseException("field "+f.getDatasetFieldType().getName()+" is not a child of "+ compoundType.getName());
                    }
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
            JsonObject value = json.getJsonObject("value");
            for ( String key : value.keySet()  ) {
                JsonObject childFieldJson = value.getJsonObject(key);
                DatasetField f = parseField( childFieldJson );
                f.setParentDatasetFieldCompoundValue(cv);
                fields.add( f );
            }
            cv.setChildDatasetFields(fields);
            List<DatasetFieldCompoundValue> vals = new LinkedList<>();
            vals.add(cv);
            return vals;
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
            datasetFieldValue.setValue( json.getString("value", "") );
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
            String strValue = json.getString("value", "");
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
    Date parseTime( String str ) throws ParseException {
        return str==null ? null : timeFormat.parse(str);
    }
    
    Long parseLong( String str ) throws NumberFormatException {
        return (str==null) ? null : Long.valueOf(str);
    }
    
    int parsePrimitiveInt( String str, int defaultValue ) {
        return str==null ? defaultValue : Integer.parseInt(str);
    }
    
}
