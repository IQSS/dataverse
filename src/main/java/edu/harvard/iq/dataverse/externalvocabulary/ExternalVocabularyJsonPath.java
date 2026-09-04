package edu.harvard.iq.dataverse.externalvocabulary;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.math.NumberUtils;

final class ExternalVocabularyJsonPath {

    private ExternalVocabularyJsonPath() {
    }

    static List<JsonValue> valuesAt(JsonValue root, String path, String termUri) {
        if (root == null || path == null || path.isBlank()) {
            return List.of(root);
        }

        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        if (normalizedPath.isEmpty()) {
            return List.of(root);
        }

        List<JsonValue> currentValues = List.of(root);
        for (String segment : normalizedPath.split("/")) {
            List<JsonValue> nextValues = new ArrayList<>();
            for (JsonValue currentValue : currentValues) {
                nextValues.addAll(valuesForSegment(currentValue, segment, termUri));
            }
            currentValues = nextValues;
            if (currentValues.isEmpty()) {
                return List.of();
            }
        }

        return currentValues;
    }

    static String firstStringAt(JsonValue root, String path, String termUri) {
        for (JsonValue value : valuesAt(root, path, termUri)) {
            String stringValue = asString(value);
            if (stringValue != null && !stringValue.isBlank()) {
                return stringValue;
            }
        }
        return null;
    }

    static String asString(JsonValue value) {
        if (value == null || value == JsonValue.NULL) {
            return null;
        }
        if (value.getValueType() == JsonValue.ValueType.STRING) {
            return ((JsonString) value).getString();
        }
        return value.toString();
    }

    private static List<JsonValue> valuesForSegment(JsonValue value, String segment, String termUri) {
        if (value == null || value == JsonValue.NULL) {
            return List.of();
        }
        if ("*".equals(segment) && value.getValueType() == JsonValue.ValueType.ARRAY) {
            return new ArrayList<>(value.asJsonArray());
        }
        if (segment.contains("=") && value.getValueType() == JsonValue.ValueType.ARRAY) {
            return valuesMatchingFilter(value.asJsonArray(), segment, termUri);
        }
        if (value.getValueType() == JsonValue.ValueType.ARRAY && NumberUtils.isCreatable(segment)) {
            return valueAtIndex(value.asJsonArray(), segment);
        }
        if (value.getValueType() == JsonValue.ValueType.OBJECT) {
            JsonValue nextValue = value.asJsonObject().get(segment);
            return nextValue == null ? List.of() : List.of(nextValue);
        }
        return List.of();
    }

    private static List<JsonValue> valuesMatchingFilter(JsonArray array, String segment, String termUri) {
        String[] keyValue = segment.split("=", 2);
        if (keyValue.length != 2 || keyValue[0].isBlank()) {
            return List.of();
        }

        String key = keyValue[0];
        String expected = "@id".equals(keyValue[1]) ? termUri : keyValue[1];
        List<JsonValue> matches = new ArrayList<>();

        for (JsonValue arrayValue : array) {
            if (arrayValue.getValueType() != JsonValue.ValueType.OBJECT) {
                continue;
            }
            JsonObject object = arrayValue.asJsonObject();
            JsonValue candidate = object.get(key);
            if (matchesExpected(candidate, expected)) {
                matches.add(object);
            }
        }

        return matches;
    }

    private static boolean matchesExpected(JsonValue candidate, String expected) {
        if (candidate == null || candidate == JsonValue.NULL) {
            return false;
        }
        if ("*".equals(expected)) {
            return true;
        }
        if (candidate.getValueType() == JsonValue.ValueType.STRING) {
            return ((JsonString) candidate).getString().equals(expected);
        }
        if (candidate.getValueType() == JsonValue.ValueType.ARRAY) {
            for (JsonValue arrayValue : candidate.asJsonArray()) {
                if (arrayValue.getValueType() == JsonValue.ValueType.STRING
                        && ((JsonString) arrayValue).getString().equals(expected)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<JsonValue> valueAtIndex(JsonArray array, String segment) {
        try {
            int index = Integer.parseInt(segment);
            if (index >= 0 && index < array.size()) {
                return List.of(array.get(index));
            }
        } catch (NumberFormatException ignored) {
            return List.of();
        }
        return List.of();
    }
}
