package edu.harvard.iq.dataverse.externalvocabulary;

import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

final class ExternalVocabularyHttpHelper {

    private ExternalVocabularyHttpHelper() {
    }

    static Invocation.Builder applyConfiguredHeaders(Invocation.Builder request, JsonObject config) {
        request.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        if (config != null && config.containsKey("headers") && config.get("headers").getValueType() == jakarta.json.JsonValue.ValueType.OBJECT) {
            JsonObject headers = config.getJsonObject("headers");
            for (String headerName : headers.keySet()) {
                request.header(headerName, headers.getString(headerName, ""));
            }
        }

        return request;
    }

    static String withTrailingSlash(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.endsWith("/") ? value : value + "/";
    }
}
