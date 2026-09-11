package edu.harvard.iq.dataverse.externalvocabulary;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.JsonReader;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HttpJsonExternalVocabularyProvider implements ExternalVocabularyProvider {

    static final String TYPE = "http-json";

    @Override
    public String getProtocol() {
        return TYPE;
    }

    public boolean supports(JsonObject config) {
        JsonObject providerConfig = getProviderConfig(config);
        return TYPE.equals(providerConfig.getString("type", ""))
                || providerConfig.containsKey("search-uri")
                || providerConfig.containsKey("search-url");
    }

    @Override
    public List<ExternalVocabularyTerm> search(String query, JsonObject config, ExternalVocabularyContext context)
            throws ExternalVocabularyException {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        JsonObject providerConfig = getProviderConfig(config);
        String searchUri = firstNonBlank(
                providerConfig.getString("search-uri", null),
                providerConfig.getString("search-url", null));
        if (searchUri == null) {
            throw new ExternalVocabularyException("Runtime external vocabulary provider requires search-uri.");
        }

        String requestUri = replaceTemplateValues(searchUri, query, query, context);
        JsonValue payload = getJson(requestUri, config);
        List<JsonValue> resultValues = ExternalVocabularyJsonPath.valuesAt(
                payload,
                providerConfig.getString("results-path", "/results"),
                query);

        List<ExternalVocabularyTerm> terms = new ArrayList<>();
        int limit = providerConfig.getInt("limit", 10);
        for (JsonValue resultValue : resultValues) {
            if (resultValue == null || resultValue == JsonValue.NULL) {
                continue;
            }

            String uri = firstStringAt(resultValue, query, providerConfig, config, "uri-path", "/uri", "/id", "/@id");
            String label = firstStringAt(resultValue, uri, providerConfig, config, "label-path", "/label", "/name", "/prefLabel");
            if (uri == null || uri.isBlank() || label == null || label.isBlank()) {
                continue;
            }

            terms.add(new ExternalVocabularyTerm(
                    uri,
                    label,
                    firstConfiguredOrPathValue(resultValue, uri, providerConfig, "vocabulary-name", "vocabulary-name-path"),
                    firstConfiguredOrPathValue(resultValue, uri, providerConfig, "vocabulary-uri", "vocabulary-uri-path"),
                    config.getString("protocol", getProtocol()),
                    ExternalVocabularyRetrievalFilter.map(config, resultValue, uri)));

            if (terms.size() >= limit) {
                break;
            }
        }

        return terms;
    }

    @Override
    public Optional<ExternalVocabularyTerm> resolve(String uri, JsonObject config, ExternalVocabularyContext context)
            throws ExternalVocabularyException {
        if (uri == null || uri.trim().isEmpty()) {
            return Optional.empty();
        }

        JsonObject providerConfig = getProviderConfig(config);
        String resolveUri = firstNonBlank(
                providerConfig.getString("resolve-uri", null),
                providerConfig.getString("resolve-url", null),
                config.getString("retrieval-uri", null));
        if (resolveUri == null) {
            return Optional.empty();
        }

        String termId = stripConfiguredPrefix(uri, config);
        String requestUri = replaceTemplateValues(resolveUri, termId, uri, context);
        JsonValue payload = getJson(requestUri, config);
        JsonValue resultValue = firstValueAt(payload, providerConfig.getString("resolve-result-path", ""), uri)
                .orElse(payload);

        String resolvedUri = firstStringAt(resultValue, uri, providerConfig, config, "uri-path", "/uri", "/id", "/@id");
        String label = firstStringAt(resultValue, uri, providerConfig, config, "label-path", "/label", "/name", "/prefLabel");
        return Optional.of(new ExternalVocabularyTerm(
                firstNonBlank(resolvedUri, uri),
                firstNonBlank(label, uri),
                firstConfiguredOrPathValue(resultValue, uri, providerConfig, "vocabulary-name", "vocabulary-name-path"),
                firstConfiguredOrPathValue(resultValue, uri, providerConfig, "vocabulary-uri", "vocabulary-uri-path"),
                config.getString("protocol", getProtocol()),
                ExternalVocabularyRetrievalFilter.map(config, resultValue, firstNonBlank(resolvedUri, uri))));
    }

    private JsonValue getJson(String requestUri, JsonObject config) throws ExternalVocabularyException {
        try (Client client = ClientBuilder.newClient()) {
            Invocation.Builder request = client.target(requestUri).request();
            try (Response response = ExternalVocabularyHttpHelper.applyConfiguredHeaders(request, config).get()) {
                if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                    throw new ExternalVocabularyException("External service returned HTTP " + response.getStatus() + ".");
                }
                try (JsonReader reader = Json.createReader(new StringReader(response.readEntity(String.class)))) {
                    return reader.readValue();
                }
            }
        } catch (ExternalVocabularyException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalVocabularyException("Failed to call runtime external vocabulary provider.", e);
        }
    }

    private static JsonObject getProviderConfig(JsonObject config) {
        JsonObject providerConfig = config.getJsonObject("provider");
        return providerConfig == null ? config : providerConfig;
    }

    private static String firstStringAt(JsonValue value, String termUri, JsonObject providerConfig, JsonObject cvocConfig,
            String configuredPathKey, String... fallbackPaths) {
        for (String configuredPath : strings(providerConfig.get(configuredPathKey))) {
            String match = ExternalVocabularyJsonPath.firstStringAt(value, configuredPath, termUri);
            if (match != null) {
                return match;
            }
        }

        for (String configuredPath : filteringPaths(cvocConfig, configuredPathKey)) {
            String match = ExternalVocabularyJsonPath.firstStringAt(value, configuredPath, termUri);
            if (match != null) {
                return match;
            }
        }

        for (String fallbackPath : fallbackPaths) {
            String match = ExternalVocabularyJsonPath.firstStringAt(value, fallbackPath, termUri);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private static List<String> filteringPaths(JsonObject config, String configuredPathKey) {
        String filteringKey = "label-path".equals(configuredPathKey) ? "termName" : "";
        if (filteringKey.isBlank()) {
            return List.of();
        }

        JsonObject filtering = config.getJsonObject("retrieval-filtering");
        if (filtering == null || !filtering.containsKey(filteringKey)) {
            return List.of();
        }

        JsonObject filter = filtering.getJsonObject(filteringKey);
        if (filter == null || !"{0}".equals(filter.getString("pattern", ""))) {
            return List.of();
        }

        return strings(filter.get("params"));
    }

    private static String firstConfiguredOrPathValue(JsonValue value, String termUri, JsonObject providerConfig,
            String configuredValueKey, String configuredPathKey) {
        String configuredValue = providerConfig.getString(configuredValueKey, null);
        if (configuredValue != null && !configuredValue.isBlank()) {
            return configuredValue;
        }

        for (String path : strings(providerConfig.get(configuredPathKey))) {
            String match = ExternalVocabularyJsonPath.firstStringAt(value, path, termUri);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private static Optional<JsonValue> firstValueAt(JsonValue root, String path, String termUri) {
        return ExternalVocabularyJsonPath.valuesAt(root, path, termUri).stream()
                .filter(value -> value != null && value != JsonValue.NULL)
                .findFirst();
    }

    private static List<String> strings(JsonValue value) {
        if (value == null || value == JsonValue.NULL) {
            return List.of();
        }
        if (value.getValueType() == JsonValue.ValueType.STRING) {
            return List.of(((JsonString) value).getString());
        }
        if (value.getValueType() == JsonValue.ValueType.ARRAY) {
            List<String> values = new ArrayList<>();
            JsonArray array = value.asJsonArray();
            for (JsonValue arrayValue : array) {
                if (arrayValue.getValueType() == JsonValue.ValueType.STRING) {
                    values.add(((JsonString) arrayValue).getString());
                }
            }
            return values;
        }
        return List.of();
    }

    private static String replaceTemplateValues(String template, String zeroValue, String termUri,
            ExternalVocabularyContext context) {
        String result = template;
        result = replace(result, "0", zeroValue);
        result = replace(result, "query", zeroValue);
        result = replace(result, "termId", zeroValue);
        result = replace(result, "uri", termUri);
        result = replace(result, "termUri", termUri);
        result = replace(result, "vocabulary", context.getVocabulary());
        result = replace(result, "language", context.getLanguage());
        return result;
    }

    private static String replace(String template, String name, String value) {
        if (value == null) {
            value = "";
        }
        return template
                .replace("{" + name + "}", value)
                .replace("{encodeUrl:" + name + "}", URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    private static String stripConfiguredPrefix(String uri, JsonObject config) {
        String prefix = config.getString("prefix", null);
        if (prefix != null && uri.startsWith(prefix)) {
            return uri.substring(prefix.length());
        }
        return uri;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
