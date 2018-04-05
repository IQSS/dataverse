package edu.harvard.iq.dataverse;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.json.JsonObject;


public class ProvUtilFragmentBean extends AbstractApiBean implements java.io.Serializable{
   
    HashMap<String,ProvEntityFileData> provJsonParsedEntities = new HashMap<>();
    JsonParser parser = new JsonParser();
    
    public HashMap<String,ProvEntityFileData> startRecurseNames(String jsonString) {
        //TODO: Make this take a string and make the jsonObject ourselves
        com.google.gson.JsonObject jsonObject = parser.parse(jsonString).getAsJsonObject();
        recurseNames(jsonObject, null, false);
        return provJsonParsedEntities;
    }
    
    /** Parsing recurser for prov json. Pulls out all names/types inside entity, including the name of each entry inside entity
     * Note that if a later entity is found with the same entity name (not name tag) its parsed contents will replace values that are stored
     * Current parsing code does not parse json arrays. My understanding of the schema is that these do not take place
     * Schema: https://www.w3.org/Submission/2013/SUBM-prov-json-20130424/schema
     */
    protected JsonElement recurseNames(JsonElement element, String outerKey, boolean atEntity) {
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
                    recurseNames(s.getValue(),s.getKey(),true);
                } else {
                    recurseNames(s.getValue(),s.getKey(),false);
                }
                
            });
          
        } 
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
    
   
}
