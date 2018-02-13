package edu.harvard.iq.dataverse.provenance;

import edu.harvard.iq.dataverse.util.SystemConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonObjectBuilder;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import java.io.StringReader;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.json.JSONObject;

//see: https://github.com/ProvTools/prov-cpl/blob/master/bindings/python/RestAPI/rest-docs.txt
@Stateless
public class ProvenanceRestServiceBean {

    private static final Logger logger = Logger.getLogger(ProvenanceRestServiceBean.class.getCanonicalName());

    //MAD: when 4346 is combined with this code, use the ip being set there.
    public String provBaseUrl;

    @Inject
    SystemConfig systemConfig;

    public void init() {
        provBaseUrl = systemConfig.getProvServiceUrl();//"http://10.252.76.172:7777";
    }

    //for testing purposes, should be deleted I think.
    public void setProvBaseUrl(String url) {
        provBaseUrl = url;
    }

    //MAD: I may just want to delete this
    //nothing in the docs
    //curl http://host:port/provapi/version
    public String getVersion() throws UnirestException {
        HttpResponse<JsonNode> response = Unirest.get(provBaseUrl + "/provapi/version").asJson();
        return response.getBody().getObject().getString("version");
    }

    //all API calls return 1 of the following status codes:
    //200 - success
    //400 - invalid arguments
    //404 - lookup failed
    //500 - internal error, check logs
    //
    //GET /provapi/bundle/<id>
    //gets bundle <id> information
    //returns:
    //{
    //	'name': String,
    //	'creation_time': Timestamp,
    //	'creation_session': Long
    //}
    public Map<String, String> getBundleId(Long bundleId) throws UnirestException {
        HttpResponse<JsonNode> response = Unirest.get(provBaseUrl + "/provapi/bundle/" + bundleId).asJson();
        if (response.getStatus() == 404) {
            return null;
        }
        logger.info(response.getStatusText());
        Map returnMap = new HashMap<String, String>();
        returnMap.put("name", response.getBody().getObject().getString("name"));
        // FIXME: Return standard javax.json.JsonObject (JSON-P, JRS 353) like we do in getBundleJson.
        return returnMap;
    }

    //POST /provapi/bundle
    //creates a new bundle
    //params:
    //{
    //	'name': String
    //}
    //returns:
    //{
    //	'id': Long
    //}
    // curl -X POST -H 'Content-type: application/json' -d '{"name":"testName"}' http://localhost:7777/provapi/bundle
    /**
     * Use cases:
     *
     * - Uploading DataFile provenance (PROV-JSON file supplied by user).
     *
     * - Replace on DataFile with another ("file replace")
     *
     * - (Future) Deaccession.
     *
     * @param bundleName A concatenation of a datafile database id (or maybe
     * persistent id, in the future) and a unique action being taken (newUpload,
     * replaceFile, deaccession), separated by a dash (i.e. 42-newUpload).
     */
    public Long createEmptyBundleFromName(String bundleName) throws UnirestException {
        HttpResponse<JsonNode> response = Unirest.post(provBaseUrl + "/provapi/bundle")
                .header("Content-Type", "application/json")
                .body("{\"name\":\"" + bundleName + "\"}")
                .asJson();
        if (response.getStatus() != 200) {
            throw new RuntimeException("Response code for provenance postBundle: " + response.getStatus() + ", " + response.getStatusText());
        }
        return response.getBody().getObject().getLong("id");
    }

    public boolean deleteBundle(long bundleId) throws UnirestException {
        HttpResponse<JsonNode> deleteResponse = Unirest.delete(provBaseUrl + "/provapi/bundle/" + bundleId).asJson();
        logger.info(deleteResponse.getBody().toString());
        int responseCode = deleteResponse.getStatus();
        if (responseCode == 200) {
            return true;
        } else {
            return false;
        }
    }

    //GET /provapi/bundle/<id>/json
    //exports provenance from bundle <id> as a document
    //returns:
    //{
    //	'JSON': {PROV-JSON Doc}
    //}
    public JsonObject getBundleJson(long bundleId) throws UnirestException {
        HttpResponse<JsonNode> response = Unirest.get(provBaseUrl + "/provapi/bundle/" + bundleId + "/json").asJson();
        JSONObject orgJsonObject = response.getBody().getObject();
        /**
         * Yes, there is some inefficiency in converting this to the standard
         * (JRS 353) JSON-P format but it's worth using the standard to avoid
         * having to deal with a proliferation of Java implementations for JSON.
         */
        JsonReader jsonReader = Json.createReader(new StringReader(orgJsonObject.toString()));
        return jsonReader.readObject();
    }

    //POST /provapi/json
    //uploads a PROV-JSON file
    //params:
    //{
    //	'JSON': {PROV-JSON Doc},
    //	'bundle_name': String,
    //	'anchor_objects': [{
    //		'id': Long (internal CPL ID)
    //		'name': String (name of object in document)
    //	}]
    //}
    //
    public JsonObject uploadProvJsonForBundle(JsonObject provJson, String bundleName) throws UnirestException {
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("bundle_name", bundleName);
        job.add("JSON", provJson);
        JsonObject jsonToSend = job.build();
        logger.info("to send: " + JsonUtil.prettyPrint(jsonToSend.toString()));
        HttpResponse<JsonNode> response = Unirest.post(provBaseUrl + "/provapi/json")
                .header("Content-Type", "application/json")
                .body(jsonToSend.toString())
                .asJson();
        if (response.getStatus() != 200) {
            throw new RuntimeException("Response code for provenance postBundle: " + response.getStatus() + ", " + response.getStatusText());
        }
        JSONObject orgJsonObject = response.getBody().getObject();
        /**
         * Yes, there is some inefficiency in converting this to the standard
         * (JRS 353) JSON-P format but it's worth using the standard to avoid
         * having to deal with a proliferation of Java implementations for JSON.
         */
        JsonReader jsonReader = Json.createReader(new StringReader(orgJsonObject.toString()));
        return jsonReader.readObject();
    }

//POST /provapi/lookup/bundles
//looks up multiple bundles
//params:
//{
//	'name': String
//}
//returns:
//{
//	'ids': [Long]
//}
//
//
//GET /provapi/bundle/<id>/prefix
//	/provapi/bundle/<id>/prefix/<string:prefix>
//gets prefixes from bundle <id> 
//(optionally with prefix name)
//returns:
//{
//	'prefixes': [{
//		'prefix': String,
//		'iri': String
//	}]
//}
//
//
//POST /provapi/bundle/<id>/prefix
//creates a prefix for bundle <id>
//params:
//{
//	'prefix': String,
//	'iri': String
//}
//returns:
//
//
//GET /provapi/bundle/<id>/property
//	/provapi/bundle/<id>/property/<string:prefix>:<string:name>
//gets properties from bundle <id> 
//(optionally with property prefix:name)
//returns:
//{
//	'properties': [{
//		'prefix': String,
//		'name': String,
//		'value': String
//	}]
//}
//
//POST /provapi/bundle/<id>/property
//creates a property for bundle <id>
//params:
//{
//	'prefix': String,
//	'name': String,
//	'value': String
//}
//returns:
//
//GET /provapi/bundle/<id>/objects
//gets all objects from bundle <id>
//returns:
//{
//	objects: [{
//        'id': Long, 
//        'creation_time': Timestamp,
//        'prefix': String,
//        'name': String,
//        'type': enum,
//        'bundle': Long
//	}]
//}
//
//GET /provapi/bundle/<id>/relations
//gets all relations from bundle <id>
//returns:
//{
//	relations: [{
//		'id': Long,
//		'ancestor': Long,
//		'descendant': Long,
//		'type': enum,
//		'bundle': Long,
//		'base': Long,
//		'other': Long
//	}]
//}
//
//GET /provapi/object/<id>
//gets object <id> information
//returns:
//{
//    'id': Long, 
//    'creation_time': Timestamp,
//    'prefix': String,
//    'name': String,
//    'type': enum,
//    'bundle': Long
//}
//
//POST /provapi/object
//creates a new object
//params:
//{
//	'prefix': String,
//	'name': String,
//	'type': enum,
//	'bundle': Long
//}
//returns:
//{
//	'id': Long
//}
    /**
     * Need IRI per Dataverse-installation.
     *
     * @param prefix Short, three characters
     */
    public static void createObject(String prefix, String name, String type, String bundleId) {
        // FIXME: Implement me. Add function for addBundlePrefix too.
    }

//
//POST /provapi/lookup/object
//looks up an object
//params:
//{
//	'prefix': String,
//	'name': String,
//	'type': enum, (optional)
//	'bundle': Long (optional)
//}
//returns:
//{
//	'id': Long
//}
//
//POST /provapi/lookup/object
//looks up multiple objects
//params:
//{
//	'prefix': String,
//	'name': String,
//	'type': enum, (optional)
//	'bundle': Long (optional)
//}
//returns:
//{
//	'id': [Long]
//}
//
//GET /provapi/object/<id>/property
//	/provapi/object/<id>/property/<string:prefix>:<string:property>
//gets properties from object <id> 
//(optionally with property prefix:name)
//returns:
//{
//	'properties': [{
//		'prefix': String,
//		'name': String,
//		'value': String
//	}]
//}
//
//POST /provapi/object/<id>/property
//creates a property for object <id>
//params:
//{
//	'prefix': String,
//	'name': String,
//	'value': String
//}
//returns:
//
//POST /provapi/lookup/object/property
//looks up objects by property
//param:
//{
//	'prefix': String,
//	'name': String,
//	'value': String (optional)
//}
//returns:
//{
//	ids: [Long]
//}
//
//GET /provapi/object/<id>/ancestors
//gets all ancestral relations from object <id>
//returns:
//{
//	relations: [{
//		'id': Long,
//		'ancestor': Long,
//		'descendant': Long,
//		'type': enum,
//		'bundle': Long,
//		'base': Long,
//		'other': Long
//	}]
//}
//
//GET /provapi/object/<id>/descendants
//gets all descendent relations to object <id>
//returns:
//{
//	relations: [{
//		'id': Long,
//		'ancestor': Long,
//		'descendant': Long,
//		'type': enum,
//		'bundle': Long,
//		'base': Long,
//		'other': Long
//	}]
//}
//
//POST /provapi/object/<id>/relation
//creates a relation to/from object <id>
//params:
//{
//	'dest' OR 'src': Long,
//	'type': enum,
//	'bundle': Long 
//}
//returns:
//{
//	'id': Long
//}
//
//GET /provapi/relation/<id>/property
//	/provapi/relation/<id>/property/<string:prefix>:<string:name>
//gets properties from relation <id> 
//(optionally with property prefix:name)
//returns:
//{
//	'properties': [{
//		'prefix': String,
//		'name': String,
//		'value': String
//	}]
//}
//
//POST /provapi/relation/<id>/property
//creates a property for relation <id>
//params:
//{
//	'prefix': String,
//	'name': String,
//	'value': String
//}
//returns:
//
}
