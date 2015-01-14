package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

public class Util {

    private static final Logger logger = Logger.getLogger(Util.class.getCanonicalName());

    static final Set<String> booleanValues;
    static final Set<String> booleanTrueValues;
    
    static {
        booleanTrueValues = new TreeSet<>();
        booleanTrueValues.add("true");
        booleanTrueValues.add("yes");
        booleanTrueValues.add("1");
        
        booleanValues = new TreeSet<>();
        booleanValues.addAll( booleanTrueValues );
        booleanValues.add("no");
        booleanValues.add("false");
        booleanValues.add("0");
    }

    @Deprecated
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

    @Deprecated
    static String message2ApiError(String message) {
        JsonObject error = Json.createObjectBuilder()
                .add("message", message)
                .add("documentation_url", "http://thedata.org")
                .build();
        return jsonObject2prettyString(error);

    }
	
    static JsonArray asJsonArray( String str ) {
        try ( JsonReader rdr = Json.createReader(new StringReader(str)) ) {
            return rdr.readArray();
        }
    }
    
	static String ok( JsonObject obj ) {
		JsonObjectBuilder response = Json.createObjectBuilder();
		response.add("status", "OK");
		response.add("data", obj);
		
		return jsonObject2prettyString(response.build());
	}
	
	static String ok( String msg ) {
		JsonObjectBuilder response = Json.createObjectBuilder();
		response.add("status", "OK");
		response.add("data", Json.createObjectBuilder().add("message", msg));
		
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
	
    static boolean isBoolean( String s ) {
        return booleanValues.contains(s.toLowerCase());
    }
    
    static boolean isTrue( String s ) {
        return booleanTrueValues.contains(s.toLowerCase());
    }
    
	static boolean isNumeric( String s ) {
		for ( char c : s.toCharArray() ) {
			if ( ! Character.isDigit(c) ) return false;
		}
		return true;
	}

    /**
     * @param date The Date object to convert.
     * @return A ISO 8601 date with UTC time zone or null.
     *
     * <p>
     *
     * "Law #1: Use ISO-8601 for your dates"
     * http://apiux.com/2013/03/20/5-laws-api-dates-and-times/
     *
     * <p>
     * "All timestamps are returned in ISO 8601 format: YYYY-MM-DDTHH:MM:SSZ"
     * https://developer.github.com/v3/#schema
     *
     * <p>
     *
     * "Law #4: Return it in UTC"
     * http://apiux.com/2013/03/20/5-laws-api-dates-and-times/
     *
     */
    public static String getDateTimeFormatToReturnIn(Date date) {
        if (date == null) {
            return null;
        }
        String otherFormatString = JsonPrinter.TIME_FORMAT_STRING;
        String dateTimeFormatString = "yyyy-MM-dd'T'HH:mm'Z'";
        if (!dateTimeFormatString.equals(otherFormatString)) {
            logger.info("Warning. Two different date/time format strings in use: " + dateTimeFormatString + " and " + otherFormatString);
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat(dateTimeFormatString);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

}
