package edu.harvard.iq.dataverse.provenance;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import jakarta.json.JsonObject;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;

//This has been made a singleton because the schema validator only needs to exist once
//and loads very slowly
public class ProvInvestigator {
   
    private static final Logger logger = Logger.getLogger(ProvInvestigator.class.getCanonicalName());
    private static ProvInvestigator pvSingleton;
    
    private ProvInvestigator() {
        
    }
    
    public HashMap<String,ProvEntityFileData> startRecurseNames(String jsonString) {
        JsonParser parser = new JsonParser();
        HashMap<String,ProvEntityFileData> provJsonParsedEntities = new HashMap<>();
        com.google.gson.JsonObject jsonObject = parser.parse(jsonString).getAsJsonObject();
        recurseNames(jsonObject, null, provJsonParsedEntities, false);
        return provJsonParsedEntities;
    }
    
    static {
        pvSingleton = new ProvInvestigator();
    }
    
    public static ProvInvestigator getInstance() {
        return pvSingleton;
    }
    
    /** Parsing recurser for prov json. Pulls out all names/types inside entity, including the name of each entry inside entity
     * Note that if a later entity is found with the same entity name (not name tag) its parsed contents will replace values that are stored
     * Current parsing code does not parse json arrays. My understanding of the schema is that these do not take place
     * Schema: https://www.w3.org/Submission/2013/SUBM-prov-json-20130424/schema
     */
    protected JsonElement recurseNames(JsonElement element, String outerKey, HashMap<String,ProvEntityFileData> provJsonParsedEntities, boolean atEntity) {
        //we need to know when we are inside of entity 
        //we also need to know when we are inside of each entity so we correctly connect the values
        if(element.isJsonObject()) {
            com.google.gson.JsonObject jsonObject = element.getAsJsonObject();
            Set<Map.Entry<String,JsonElement>> entrySet = jsonObject.entrySet();
            entrySet.forEach((s) -> {
                if(atEntity) {
                    String key = s.getKey();
                    
                    if("name".equals(key) || key.endsWith(":name")) {
                        ProvEntityFileData e = provJsonParsedEntities.get(outerKey);
                        e.fileName = s.getValue().getAsString();
                    } else if("type".equals(key) || key.endsWith(":type")) {
                        if(s.getValue().isJsonObject()) {
                            for ( Map.Entry tEntry : s.getValue().getAsJsonObject().entrySet()) {
                                String tKey = (String) tEntry.getKey();
                                if("type".equals(tKey) || tKey.endsWith(":type")) {
                                    ProvEntityFileData e = provJsonParsedEntities.get(outerKey);

                                    String value = tEntry.getValue().toString();
                                    e.fileType = value;
                                }
                            }                            
                        } else if(s.getValue().isJsonPrimitive()){
                            ProvEntityFileData e = provJsonParsedEntities.get(outerKey);
                            String value = s.getValue().getAsString();
                            e.fileType = value;
                        }
                    }
                } 
                if(null != outerKey && (outerKey.equals("entity") || outerKey.endsWith(":entity"))) {
                    //we are storing the entity name both as the key and in the object, the former for access and the later for ease of use when converted to a list
                    //Also, when we initialize the entity the freeform is set to null, after this recursion
                    provJsonParsedEntities.put(s.getKey(), new ProvEntityFileData(s.getKey(), null, null));
                    recurseNames(s.getValue(),s.getKey(), provJsonParsedEntities, true);
                } else {
                    recurseNames(s.getValue(),s.getKey(), provJsonParsedEntities, false);
                }
                
            });
          
        } 
//// My understanding of the prov standard is there should be no entities in arrays
//// But if that changes the below code should be flushed out --MAD 4.8.6
//        else if(element.isJsonArray()) {
//            JsonArray jsonArray = element.getAsJsonArray();
//            for (JsonElement childElement : jsonArray) {
//                JsonElement result = recurseNames(childElement);
//                if(result != null) {
//                    return result;
//                }
//            }
//        }
        
        return null;
    }
    
    public String getPrettyJsonString(JsonObject jsonObject) {
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(jsonObject.toString());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(je);
    }

    public boolean isProvValid(String jsonInput) {
        try { 
            schema.validate(new JSONObject(jsonInput)); // throws a ValidationException if this object is invalid
        } catch (ValidationException vx) {
            logger.info("Prov schema error : " + vx); //without classLoader is blows up in actual deployment
            return false;
        } catch (Exception ex) {
            logger.info("Prov file error : " + ex);
            return false;
        } 

        return true;
    }
    
    //Pulled from https://www.w3.org/Submission/2013/SUBM-prov-json-20130424/schema
    //Not the prettiest way of accessing the schema, but loading the .json file as an external resource
    //turned out to be very painful, especially when also trying to exercise it via unit tests
    //
    //To solve https://github.com/IQSS/dataverse/issues/5154 , the provenance schema
    //here was updated to include the "core schema" values being downloaded by the "id" tag. 
    //If this schema needs to be updated (as of 2018, it hadn't been since 2013) this will need
    //to be done manually again or we'll need to pull both files and store them on disk.
    //The later option was not done previously because we couldn't get the same files to be
    //referenced by the code and our junit tests.
    private static final String provSchema = 
        "{\n" +
        "    \"id\": \"\",\n" +
        "    \"$schema\": \"http://json-schema.org/draft-04/schema#\",\n" +
        "    \"description\": \"Schema for a PROV-JSON document\",\n" +
        "    \"type\": \"object\",\n" +
        "    \"additionalProperties\": false,\n" +
        "    \"dependencies\": {\n" +
        "        \"exclusiveMaximum\": [ \"maximum\" ],\n" +
        "        \"exclusiveMinimum\": [ \"minimum\" ]\n" +
        "    },"+
        "    \"default\": {},\n" +
        "    \"properties\": {\n" +
        "        \"id\": {\n" +
        "            \"type\": \"string\"\n" +
        "        },\n" +
        "        \"$schema\": {\n" +
        "            \"type\": \"string\"\n" +
        "        },\n" +
        "        \"title\": {\n" +
        "            \"type\": \"string\"\n" +
        "        },\n" +
        "        \"description\": {\n" +
        "            \"type\": \"string\"\n" +
        "        },\n" +
        "        \"default\": {},\n" +
        "        \"multipleOf\": {\n" +
        "            \"type\": \"number\",\n" +
        "            \"minimum\": 0,\n" +
        "            \"exclusiveMinimum\": true\n" +
        "        },\n" +
        "        \"maximum\": {\n" +
        "            \"type\": \"number\"\n" +
        "        },\n" +
        "        \"exclusiveMaximum\": {\n" +
        "            \"type\": \"boolean\",\n" +
        "            \"default\": false\n" +
        "        },\n" +
        "        \"minimum\": {\n" +
        "            \"type\": \"number\"\n" +
        "        },\n" +
        "        \"exclusiveMinimum\": {\n" +
        "            \"type\": \"boolean\",\n" +
        "            \"default\": false\n" +
        "        },\n" +
        "        \"maxLength\": { \"$ref\": \"#/definitions/positiveInteger\" },\n" +
        "        \"minLength\": { \"$ref\": \"#/definitions/positiveIntegerDefault0\" },\n" +
        "        \"pattern\": {\n" +
        "            \"type\": \"string\",\n" +
        "            \"format\": \"regex\"\n" +
        "        },\n" +
        "        \"additionalItems\": {\n" +
        "            \"anyOf\": [\n" +
        "                { \"type\": \"boolean\" },\n" +
        "                { \"$ref\": \"#\" }\n" +
        "            ],\n" +
        "            \"default\": {}\n" +
        "        },\n" +
        "        \"items\": {\n" +
        "            \"anyOf\": [\n" +
        "                { \"$ref\": \"#\" },\n" +
        "                { \"$ref\": \"#/definitions/schemaArray\" }\n" +
        "            ],\n" +
        "            \"default\": {}\n" +
        "        },\n" +
        "        \"maxItems\": { \"$ref\": \"#/definitions/positiveInteger\" },\n" +
        "        \"minItems\": { \"$ref\": \"#/definitions/positiveIntegerDefault0\" },\n" +
        "        \"uniqueItems\": {\n" +
        "            \"type\": \"boolean\",\n" +
        "            \"default\": false\n" +
        "        },\n" +
        "        \"maxProperties\": { \"$ref\": \"#/definitions/positiveInteger\" },\n" +
        "        \"minProperties\": { \"$ref\": \"#/definitions/positiveIntegerDefault0\" },\n" +
        "        \"required\": { \"$ref\": \"#/definitions/stringArray\" },\n" +
        "        \"additionalProperties\": {\n" +
        "            \"anyOf\": [\n" +
        "                { \"type\": \"boolean\" },\n" +
        "                { \"$ref\": \"#\" }\n" +
        "            ],\n" +
        "            \"default\": {}\n" +
        "        },\n" +
        "        \"definitions\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\": \"#\" },\n" +
        "            \"default\": {}\n" +
        "        },\n" +
        "        \"properties\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\": \"#\" },\n" +
        "            \"default\": {}\n" +
        "        },\n" +
        "        \"patternProperties\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\": \"#\" },\n" +
        "            \"default\": {}\n" +
        "        },\n" +
        "        \"dependencies\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": {\n" +
        "                \"anyOf\": [\n" +
        "                    { \"$ref\": \"#\" },\n" +
        "                    { \"$ref\": \"#/definitions/stringArray\" }\n" +
        "                ]\n" +
        "            }\n" +
        "        },\n" +
        "        \"enum\": {\n" +
        "            \"type\": \"array\",\n" +
        "            \"minItems\": 1,\n" +
        "            \"uniqueItems\": true\n" +
        "        },\n" +
        "        \"type\": {\n" +
        "            \"anyOf\": [\n" +
        "                { \"$ref\": \"#/definitions/simpleTypes\" },\n" +
        "                {\n" +
        "                    \"type\": \"array\",\n" +
        "                    \"items\": { \"$ref\": \"#/definitions/simpleTypes\" },\n" +
        "                    \"minItems\": 1,\n" +
        "                    \"uniqueItems\": true\n" +
        "                }\n" +
        "            ]\n" +
        "        },\n" +
        "        \"format\": { \"type\": \"string\" },\n" +
        "        \"allOf\": { \"$ref\": \"#/definitions/schemaArray\" },\n" +
        "        \"anyOf\": { \"$ref\": \"#/definitions/schemaArray\" },\n" +
        "        \"oneOf\": { \"$ref\": \"#/definitions/schemaArray\" },\n" +
        "        \"not\": { \"$ref\": \"#\" },\n" +
        "        \"prefix\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"patternProperties\": {\n" +
        "                        \"^[a-zA-Z0-9_\\\\-]+$\": { \"type\" : \"string\", \"format\": \"uri\" }\n" +
        "            }\n" +
        "        },\n" +
        "        \"entity\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\":\"#/definitions/entity\" }\n" +
        "        },\n" +
        "        \"activity\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\":\"#/definitions/activity\" }\n" +
        "        },\n" +
        "        \"agent\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\":\"#/definitions/agent\" }\n" +
        "        },\n" +
        "        \"wasGeneratedBy\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\":\"#/definitions/generation\" }\n" +
        "        },\n" +
        "        \"used\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\":\"#/definitions/usage\" }\n" +
        "        },\n" +
        "        \"wasInformedBy\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\":\"#/definitions/communication\" }\n" +
        "        },\n" +
        "        \"wasStartedBy\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\":\"#/definitions/start\" }\n" +
        "        },\n" +
        "        \"wasEndedby\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\":\"#/definitions/end\" }\n" +
        "        },\n" +
        "        \"wasInvalidatedBy\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\":\"#/definitions/invalidation\" }\n" +
        "        },\n" +
        "        \"wasDerivedFrom\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\":\"#/definitions/derivation\" }\n" +
        "        },\n" +
        "        \"wasAttributedTo\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\":\"#/definitions/attribution\" }\n" +
        "        },\n" +
        "        \"wasAssociatedWith\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\":\"#/definitions/association\" }\n" +
        "        },\n" +
        "        \"actedOnBehalfOf\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\":\"#/definitions/delegation\" }\n" +
        "        },\n" +
        "        \"wasInfluencedBy\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\":\"#/definitions/influence\" }\n" +
        "        },\n" +
        "        \"specializationOf\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\":\"#/definitions/specialization\" }\n" +
        "        },\n" +
        "        \"alternateOf\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\":\"#/definitions/alternate\" }\n" +
        "        },\n" +
        "        \"hadMember\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\":\"#/definitions/membership\" }\n" +
        "        },\n" +
        "        \"bundle\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"additionalProperties\": { \"$ref\":\"#/definitions/bundle\" }\n" +
        "        }\n" +
        "    },\n" +
        "    \"definitions\": {\n" +
        "        \"schemaArray\": {\n" +
        "            \"type\": \"array\",\n" +
        "            \"minItems\": 1,\n" +
        "            \"items\": { \"$ref\": \"#\" }\n" +
        "        },\n" +
        "        \"positiveInteger\": {\n" +
        "            \"type\": \"integer\",\n" +
        "            \"minimum\": 0\n" +
        "        },\n" +
        "        \"positiveIntegerDefault0\": {\n" +
        "            \"allOf\": [ { \"$ref\": \"#/definitions/positiveInteger\" }, { \"default\": 0 } ]\n" +
        "        },\n" +
        "        \"simpleTypes\": {\n" +
        "            \"enum\": [ \"array\", \"boolean\", \"integer\", \"null\", \"number\", \"object\", \"string\" ]\n" +
        "        },\n" +
        "        \"stringArray\": {\n" +
        "            \"type\": \"array\",\n" +
        "            \"items\": { \"type\": \"string\" },\n" +
        "            \"minItems\": 1,\n" +
        "            \"uniqueItems\": true\n" +
        "        },\n"+
        "        \"typedLiteral\": {\n" +
        "            \"title\": \"PROV-JSON Typed Literal\",\n" +
        "            \"type\": \"object\",\n" +
        "            \"properties\": {\n" +
        "                \"$\": { \"type\": \"string\" },\n" +
        "                \"type\": { \"type\": \"string\", \"format\": \"uri\" },\n" +
        "                \"lang\": { \"type\": \"string\" }\n" +
        "            },\n" +
        "            \"required\": [\"$\"],\n" +
        "            \"additionalProperties\": false\n" +
        "        },\n" +
        "        \"stringLiteral\": {\"type\": \"string\"},\n" +
        "        \"numberLiteral\": {\"type\": \"number\"},\n" +
        "        \"booleanLiteral\": {\"type\": \"boolean\"},\n" +
        "        \"literalArray\": {\n" +
        "            \"type\": \"array\",\n" +
        "            \"minItems\": 1,\n" +
        "            \"items\": {\n" +
        "                \"anyOf\": [\n" +
        "                    { \"$ref\": \"#/definitions/stringLiteral\" },\n" +
        "                    { \"$ref\": \"#/definitions/numberLiteral\" },\n" +
        "                    { \"$ref\": \"#/definitions/booleanLiteral\" },\n" +
        "                    { \"$ref\": \"#/definitions/typedLiteral\" }\n" +
        "                ]\n" +
        "            }\n" +
        "        },\n" +
        "        \"attributeValues\": {\n" +
        "            \"anyOf\": [\n" +
        "                { \"$ref\": \"#/definitions/stringLiteral\" },\n" +
        "                { \"$ref\": \"#/definitions/numberLiteral\" },\n" +
        "                { \"$ref\": \"#/definitions/booleanLiteral\" },\n" +
        "                { \"$ref\": \"#/definitions/typedLiteral\" },\n" +
        "                { \"$ref\": \"#/definitions/literalArray\" }\n" +
        "            ]\n" +
        "        },\n" +
        "        \"entity\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"title\": \"entity\",\n" +
        "            \"additionalProperties\": { \"$ref\": \"#/definitions/attributeValues\" }\n" +
        "        },\n" +
        "        \"agent\": { \"$ref\": \"#/definitions/entity\" },\n" +
        "        \"activity\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"title\": \"activity\",\n" +
        "            \"prov:startTime\": { \"type\": \"string\", \"format\": \"date-time\" },\n" +
        "            \"prov:endTime\": { \"type\": \"string\", \"format\": \"date-time\" },\n" +
        "            \"additionalProperties\": { \"$ref\": \"#/definitions/attributeValues\" }\n" +
        "        },\n" +
        "        \"generation\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"title\": \"generation/usage\",\n" +
        "            \"properties\": {\n" +
        "                \"prov:entity\": { \"type\": \"string\", \"format\": \"uri\" },\n" +
        "                \"prov:activity\": { \"type\": \"string\", \"format\": \"uri\" },\n" +
        "                \"prov:time\": { \"type\": \"string\", \"format\": \"date-time\" }\n" +
        "            },\n" +
        "            \"required\": [\"prov:entity\"],\n" +
        "            \"additionalProperties\": { \"$ref\": \"#/definitions/attributeValues\" }\n" +
        "        },\n" +
        "        \"usage\": {\"$ref\":\"#/definitions/generation\"},\n" +
        "        \"communication\":{\n" +
        "            \"type\": \"object\",\n" +
        "            \"title\": \"communication\",\n" +
        "            \"properties\": {\n" +
        "                \"prov:informant\": {\"type\": \"string\", \"format\": \"uri\"},\n" +
        "                \"prov:informed\": {\"type\": \"string\", \"format\": \"uri\"}\n" +
        "            },\n" +
        "            \"required\": [\"prov:informant\", \"prov:informed\"],\n" +
        "            \"additionalProperties\": { \"$ref\": \"#/definitions/attributeValues\" }\n" +
        "        },\n" +
        "        \"start\":{\n" +
        "            \"type\": \"object\",\n" +
        "            \"title\": \"start/end\",\n" +
        "            \"properties\": {\n" +
        "                \"prov:activity\": {\"type\": \"string\", \"format\": \"uri\"},\n" +
        "                \"prov:time\": {\"type\": \"string\", \"format\": \"date-time\"},\n" +
        "                \"prov:trigger\": {\"type\": \"string\", \"format\": \"uri\"}\n" +
        "            },\n" +
        "            \"required\": [\"prov:activity\"],\n" +
        "            \"additionalProperties\": { \"$ref\": \"#/definitions/attributeValues\" }\n" +
        "        },\n" +
        "        \"end\": {\"$ref\":\"#/definitions/start\"},\n" +
        "        \"invalidation\":{\n" +
        "            \"type\": \"object\",\n" +
        "            \"title\": \"invalidation\",\n" +
        "            \"properties\": {\n" +
        "                \"prov:entity\": { \"type\": \"string\", \"format\": \"uri\" },\n" +
        "                \"prov:time\": { \"type\": \"string\", \"format\": \"date-time\" },\n" +
        "                \"prov:activity\": { \"type\": \"string\", \"format\": \"uri\" }\n" +
        "            },\n" +
        "            \"required\": [\"prov:entity\"],\n" +
        "            \"additionalProperties\": { \"$ref\": \"#/definitions/attributeValues\" }\n" +
        "        },\n" +
        "        \"derivation\":{\n" +
        "            \"type\": \"object\",\n" +
        "            \"title\": \"derivation\",\n" +
        "            \"properties\": {\n" +
        "                \"prov:generatedEntity\": { \"type\": \"string\", \"format\": \"uri\" },\n" +
        "                \"prov:usedEntity\": { \"type\": \"string\", \"format\": \"uri\" },\n" +
        "                \"prov:activity\": { \"type\": \"string\", \"format\": \"uri\" },\n" +
        "                \"prov:generation\": { \"type\": \"string\", \"format\": \"uri\" },\n" +
        "                \"prov:usage\": { \"type\": \"string\", \"format\": \"uri\" }\n" +
        "            },\n" +
        "            \"required\": [\"prov:generatedEntity\", \"prov:usedEntity\"],\n" +
        "            \"additionalProperties\": { \"$ref\": \"#/definitions/attributeValues\" }\n" +
        "        },\n" +
        "        \"attribution\":{\n" +
        "            \"type\": \"object\",\n" +
        "            \"title\": \"attribution\",\n" +
        "            \"properties\": {\n" +
        "                \"prov:entity\": { \"type\": \"string\", \"format\": \"uri\" },\n" +
        "                \"prov:agent\": { \"type\": \"string\", \"format\": \"uri\" }\n" +
        "            },\n" +
        "            \"required\": [\"prov:entity\", \"prov:agent\"],\n" +
        "            \"additionalProperties\": { \"$ref\": \"#/definitions/attributeValues\" }\n" +
        "        },\n" +
        "        \"association\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"title\": \"association\",\n" +
        "            \"properties\": {\n" +
        "                \"prov:activity\": { \"type\": \"string\", \"format\": \"uri\" },\n" +
        "                \"prov:agent\": { \"type\": \"string\", \"format\": \"uri\" },\n" +
        "                \"prov:plan\": { \"type\": \"string\", \"format\": \"uri\" }\n" +
        "            },\n" +
        "            \"required\": [\"prov:activity\"],\n" +
        "            \"additionalProperties\": { \"$ref\": \"#/definitions/attributeValues\" }\n" +
        "        },\n" +
        "        \"delegation\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"title\": \"delegation\",\n" +
        "            \"properties\": {\n" +
        "                \"prov:delegate\": { \"type\": \"string\", \"format\": \"uri\" },\n" +
        "                \"prov:responsible\": { \"type\": \"string\", \"format\": \"uri\" },\n" +
        "                \"prov:activity\": { \"type\": \"string\", \"format\": \"uri\" }\n" +
        "            },\n" +
        "            \"required\": [\"prov:delegate\", \"prov:responsible\"],\n" +
        "            \"additionalProperties\": { \"$ref\": \"#/definitions/attributeValues\" }\n" +
        "        },\n" +
        "        \"influence\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"title\": \"\",\n" +
        "            \"properties\": {\n" +
        "                \"prov:influencer\": { \"type\": \"string\", \"format\": \"uri\" },\n" +
        "                \"prov:influencee\": { \"type\": \"string\", \"format\": \"uri\" }\n" +
        "            },\n" +
        "            \"required\": [\"prov:influencer\", \"prov:influencee\"],\n" +
        "            \"additionalProperties\": { \"$ref\": \"#/definitions/attributeValues\" }\n" +
        "        },\n" +
        "        \"specialization\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"title\": \"specialization\",\n" +
        "            \"properties\": {\n" +
        "                \"prov:generalEntity\": { \"type\": \"string\", \"format\": \"uri\" },\n" +
        "                \"prov:specificEntity\": { \"type\": \"string\", \"format\": \"uri\" }\n" +
        "            },\n" +
        "            \"required\": [\"prov:generalEntity\", \"prov:specificEntity\"],\n" +
        "            \"additionalProperties\": { \"$ref\": \"#/definitions/attributeValues\" }\n" +
        "        },\n" +
        "        \"alternate\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"title\": \"alternate\",\n" +
        "            \"properties\": {\n" +
        "                \"prov:alternate1\": { \"type\": \"string\", \"format\": \"uri\" },\n" +
        "                \"prov:alternate2\": { \"type\": \"string\", \"format\": \"uri\" }\n" +
        "            },\n" +
        "            \"required\": [\"prov:alternate1\", \"prov:alternate2\"],\n" +
        "            \"additionalProperties\": { \"$ref\": \"#/definitions/attributeValues\" }\n" +
        "        },\n" +
        "        \"membership\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"title\": \"membership\",\n" +
        "            \"properties\": {\n" +
        "                \"prov:collection\": { \"type\": \"string\", \"format\": \"uri\" },\n" +
        "                \"prov:entity\": { \"type\": \"string\", \"format\": \"uri\" }\n" +
        "            },\n" +
        "            \"required\": [\"prov:collection\", \"prov:entity\"],\n" +
        "            \"additionalProperties\": { \"$ref\": \"#/definitions/attributeValues\" }\n" +
        "        },\n" +
        "        \"bundle\": {\n" +
        "            \"type\": \"object\",\n" +
        "            \"title\": \"bundle\",\n" +
        "            \"properties\":{\n" +
        "                \"prefix\": {\n" +
        "                    \"type\": \"object\",\n" +
        "                    \"patternProperties\": {\n" +
        "                        \"^[a-zA-Z0-9_\\\\-]+$\": { \"type\" : \"string\", \"format\": \"uri\" }\n" +
        "                    }\n" +
        "                },\n" +
        "                \"entity\": {\n" +
        "                    \"type\": \"object\",\n" +
        "                    \"additionalProperties\": { \"$ref\":\"#/definitions/entity\" }\n" +
        "                },\n" +
        "                \"activity\": {\n" +
        "                    \"type\": \"object\",\n" +
        "                    \"additionalProperties\": { \"$ref\":\"#/definitions/activity\" }\n" +
        "                },\n" +
        "                \"agent\": {\n" +
        "                    \"type\": \"object\",\n" +
        "                    \"additionalProperties\": { \"$ref\":\"#/definitions/agent\" }\n" +
        "                },\n" +
        "                \"wasGeneratedBy\": {\n" +
        "                    \"type\": \"object\",\n" +
        "                    \"additionalProperties\": { \"$ref\":\"#/definitions/generation\" }\n" +
        "                },\n" +
        "                \"used\": {\n" +
        "                    \"type\": \"object\",\n" +
        "                    \"additionalProperties\": { \"$ref\":\"#/definitions/usage\" }\n" +
        "                },\n" +
        "                \"wasInformedBy\": {\n" +
        "                    \"type\": \"object\",\n" +
        "                    \"additionalProperties\": { \"$ref\":\"#/definitions/communication\" }\n" +
        "                },\n" +
        "                \"wasStartedBy\": {\n" +
        "                    \"type\": \"object\",\n" +
        "                    \"additionalProperties\": { \"$ref\":\"#/definitions/start\" }\n" +
        "                },\n" +
        "                \"wasEndedby\": {\n" +
        "                    \"type\": \"object\",\n" +
        "                    \"additionalProperties\": { \"$ref\":\"#/definitions/end\" }\n" +
        "                },\n" +
        "                \"wasInvalidatedBy\": {\n" +
        "                    \"type\": \"object\",\n" +
        "                    \"additionalProperties\": { \"$ref\":\"#/definitions/invalidation\" }\n" +
        "                },\n" +
        "                \"wasDerivedFrom\": {\n" +
        "                    \"type\": \"object\",\n" +
        "                    \"additionalProperties\": { \"$ref\":\"#/definitions/derivation\" }\n" +
        "                },\n" +
        "                \"wasAttributedTo\": {\n" +
        "                    \"type\": \"object\",\n" +
        "                    \"additionalProperties\": { \"$ref\":\"#/definitions/attribution\" }\n" +
        "                },\n" +
        "                \"wasAssociatedWith\": {\n" +
        "                    \"type\": \"object\",\n" +
        "                    \"additionalProperties\": { \"$ref\":\"#/definitions/association\" }\n" +
        "                },\n" +
        "                \"actedOnBehalfOf\": {\n" +
        "                    \"type\": \"object\",\n" +
        "                    \"additionalProperties\": { \"$ref\":\"#/definitions/delegation\" }\n" +
        "                },\n" +
        "                \"wasInfluencedBy\": {\n" +
        "                    \"type\": \"object\",\n" +
        "                    \"additionalProperties\": { \"$ref\":\"#/definitions/influence\" }\n" +
        "                },\n" +
        "                \"specializationOf\": {\n" +
        "                    \"type\": \"object\",\n" +
        "                    \"additionalProperties\": { \"$ref\":\"#/definitions/specialization\" }\n" +
        "                },\n" +
        "                \"alternateOf\": {\n" +
        "                    \"type\": \"object\",\n" +
        "                    \"additionalProperties\": { \"$ref\":\"#/definitions/alternate\" }\n" +
        "                },\n" +
        "                \"hadMember\": {\n" +
        "                    \"type\": \"object\",\n" +
        "                    \"additionalProperties\": { \"$ref\":\"#/definitions/membership\" }\n" +
        "                }\n" +
        "            }\n" +
        "        }\n" +
        "    }\n" +
        "}"; 
    
    private static final JSONObject rawSchema = new JSONObject(new JSONTokener(provSchema));
    private static final Schema schema = SchemaLoader.load(rawSchema);

}
