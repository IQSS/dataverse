package edu.harvard.iq.dataverse.util.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.json.JsonException;
import jakarta.json.JsonValue;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class JsonUtilTest {

    @Test
    void testPrettyPrint() {
        String nullString = null;
        assertEquals(null, JsonUtil.prettyPrint(nullString));
        assertEquals("", JsonUtil.prettyPrint(""));
        assertEquals("junk", JsonUtil.prettyPrint("junk"));
        assertEquals("{\n}", JsonUtil.prettyPrint("{}"));
        assertEquals("[\n    \"junk\"\n]", JsonUtil.prettyPrint("[\"junk\"]"));
        assertEquals("{\n" + "    \"foo\": \"bar\"\n" + "}", JsonUtil.prettyPrint("{\"foo\": \"bar\"}"));
    }
    
    @Nested
    class JsonValues {
        @Test
        void testGetJsonValueWithJsonObject() {
            String jsonObject = "{\"key\": \"value\"}";
            JsonValue result = JsonUtil.getJsonValue(jsonObject);
            assertEquals(JsonValue.ValueType.OBJECT, result.getValueType());
            assertEquals("value", result.asJsonObject().getString("key"));
        }
        
        @Test
        void testGetJsonValueWithJsonArray() {
            String jsonArray = "[\"element1\", \"element2\"]";
            JsonValue result = JsonUtil.getJsonValue(jsonArray);
            assertEquals(JsonValue.ValueType.ARRAY, result.getValueType());
            assertEquals("element1", result.asJsonArray().getString(0));
            assertEquals("element2", result.asJsonArray().getString(1));
        }
        
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "  \"\"", "\"primitive\"", "{invalid}", "[invalid]", "[1234, invalid]"})
        void testGetJsonValueWithInvalidJson(String sut) {
            assertThrows(JsonException.class, () -> JsonUtil.getJsonValue(sut));
        }
    }
    
}
