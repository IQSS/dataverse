package edu.harvard.iq.dataverse.api;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

public class Util {

    static String jsonObject2prettyString(JsonObject jsonObject) {
        Map<String, Boolean> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory jsonWriterFactory = Json.createWriterFactory(config);
        StringWriter stringWriter = new StringWriter();
        try (JsonWriter jsonWriter = jsonWriterFactory.createWriter(stringWriter)) {
            jsonWriter.writeObject(jsonObject);
        }
        return stringWriter.toString();
    }

    static String jsonArray2prettyString(JsonArray jsonArray) {
        Map<String, Boolean> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory jsonWriterFactory = Json.createWriterFactory(config);

        StringWriter stringWriter = new StringWriter();
        try (JsonWriter jsonWriter = jsonWriterFactory.createWriter(stringWriter)) {
            jsonWriter.writeArray(jsonArray);
        }
        return stringWriter.toString();
    }

    static String message2ApiError(String message) {
        JsonObject error = Json.createObjectBuilder()
                .add("message", message)
                .add("documentation_url", "http://thedata.org")
                .build();
        return jsonObject2prettyString(error);

    }
	
	static String ok( JsonObject obj ) {
		JsonObjectBuilder response = Json.createObjectBuilder();
		response.add("status", "OK");
		response.add("data", obj);
		
		return jsonObject2prettyString(response.build());
	}
	
	static String ok( JsonArray arr ) {
		JsonObjectBuilder response = Json.createObjectBuilder();
		response.add("status", "OK");
		response.add("data", arr);
		
		return jsonObject2prettyString(response.build());
	}
	
	static String error( String message ) {
		JsonObjectBuilder response = Json.createObjectBuilder();
		response.add("status", "ERROR");
		response.add("message", message);
		
		return jsonObject2prettyString(response.build());
	}
	
	static boolean isNumeric( String s ) {
		for ( char c : s.toCharArray() ) {
			if ( ! Character.isDigit(c) ) return false;
		}
		return true;
	}
}
