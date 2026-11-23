package edu.harvard.iq.dataverse.externalvocabulary;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

final class ExternalVocabularyRetrievalFilter {

    private ExternalVocabularyRetrievalFilter() {
    }

    static JsonObject map(JsonObject config, JsonValue payload, String termUri) {
        JsonObject filtering = config.getJsonObject("retrieval-filtering");
        if (filtering == null || payload == null || payload == JsonValue.NULL) {
            return null;
        }

        JsonObjectBuilder mappedFields = Json.createObjectBuilder();
        for (String filterKey : filtering.keySet()) {
            if ("@context".equals(filterKey)) {
                continue;
            }

            JsonObject filter = filtering.getJsonObject(filterKey);
            if (filter == null) {
                continue;
            }

            JsonValue mappedValue = mapFilter(filter, payload, termUri);
            if (mappedValue != null && mappedValue != JsonValue.NULL) {
                mappedFields.add(filterKey, mappedValue);
            }
        }

        JsonObject result = mappedFields.build();
        return result.isEmpty() ? null : result;
    }

    private static JsonValue mapFilter(JsonObject filter, JsonValue payload, String termUri) {
        String pattern = filter.getString("pattern", "");
        if (pattern.isBlank()) {
            return null;
        }
        if ("@id".equals(pattern)) {
            return Json.createValue(termUri);
        }
        if (!pattern.contains("{")) {
            return Json.createValue(pattern);
        }

        List<JsonValue> values = valuesForParams(filter.get("params"), payload, termUri);
        if (values.isEmpty()) {
            return null;
        }

        if ("{0}".equals(pattern)) {
            return values.get(0);
        }

        Object[] stringValues = values.stream()
                .map(ExternalVocabularyJsonPath::asString)
                .toArray();
        return Json.createValue(MessageFormat.format(pattern, stringValues));
    }

    private static List<JsonValue> valuesForParams(JsonValue params, JsonValue payload, String termUri) {
        if (params == null || params == JsonValue.NULL || params.getValueType() != JsonValue.ValueType.ARRAY) {
            return List.of();
        }

        List<JsonValue> values = new ArrayList<>();
        for (JsonValue paramValue : params.asJsonArray()) {
            if (paramValue.getValueType() != JsonValue.ValueType.STRING) {
                continue;
            }

            JsonValue value = valueForParam(((JsonString) paramValue).getString(), payload, termUri);
            if (value != null && value != JsonValue.NULL) {
                values.add(value);
            }
        }
        return values;
    }

    private static JsonValue valueForParam(String param, JsonValue payload, String termUri) {
        if ("@id".equals(param)) {
            return Json.createValue(termUri);
        }
        if (!param.startsWith("/")) {
            return Json.createValue(param);
        }

        List<JsonValue> matches = ExternalVocabularyJsonPath.valuesAt(payload, param, termUri);
        if (matches.isEmpty()) {
            return null;
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }

        JsonArrayBuilder array = Json.createArrayBuilder();
        matches.forEach(array::add);
        return array.build();
    }
}
