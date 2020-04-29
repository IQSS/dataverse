package edu.harvard.iq.dataverse.util.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class TestJsonCreator {

    public static JsonElement stringAsJsonElement(String jsonString) {
        JsonParser parser = new JsonParser();

        return parser.parse(jsonString.replace('\'', '"'));
    }
}
