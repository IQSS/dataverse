package edu.harvard.iq.dataverse.externalvocabulary;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SkosmosExternalVocabularyProvider implements ExternalVocabularyProvider {

    @Override
    public String getProtocol() {
        return "skosmos";
    }

    @Override
    public List<ExternalVocabularyTerm> search(String query, JsonObject config, ExternalVocabularyContext context)
            throws ExternalVocabularyException {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        String vocabulary = firstNonBlank(context.getVocabulary(), getFirstVocabularyKey(config));
        String baseUrl = ExternalVocabularyHttpHelper.withTrailingSlash(config.getString("cvoc-url", ""));
        String parent = config.getString("term-parent-uri", "");

        try (Client client = ClientBuilder.newClient()) {
            Invocation.Builder request = client
                    .target(baseUrl)
                    .path("rest/v1/search")
                    .queryParam("unique", "true")
                    .queryParam("vocab", vocabulary)
                    .queryParam("parent", parent)
                    .queryParam("query", query + "*")
                    .queryParam("lang", context.getLanguage())
                    .request();

            try (Response response = ExternalVocabularyHttpHelper.applyConfiguredHeaders(request, config).get()) {
                ensureSuccessful(response, "search Skosmos terms");
                JsonObject payload = response.readEntity(JsonObject.class);
                JsonArray results = payload.getJsonArray("results");
                if (results == null) {
                    return List.of();
                }

                List<ExternalVocabularyTerm> terms = new ArrayList<>();
                for (JsonValue resultValue : results) {
                    if (resultValue.getValueType() != JsonValue.ValueType.OBJECT) {
                        continue;
                    }
                    JsonObject result = resultValue.asJsonObject();
                    String uri = result.getString("uri", "");
                    String label = result.getString("prefLabel", uri);
                    terms.add(new ExternalVocabularyTerm(uri, label, vocabulary, getVocabularyUri(config, vocabulary), getProtocol()));
                }
                return terms;
            }
        } catch (ExternalVocabularyException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalVocabularyException("Failed to search Skosmos terms.", e);
        }
    }

    @Override
    public Optional<ExternalVocabularyTerm> resolve(String uri, JsonObject config, ExternalVocabularyContext context)
            throws ExternalVocabularyException {
        if (uri == null || uri.trim().isEmpty()) {
            return Optional.empty();
        }

        String baseUrl = ExternalVocabularyHttpHelper.withTrailingSlash(config.getString("cvoc-url", ""));

        try (Client client = ClientBuilder.newClient()) {
            Invocation.Builder request = client
                    .target(baseUrl)
                    .path("rest/v1/data")
                    .queryParam("uri", uri)
                    .request();

            try (Response response = ExternalVocabularyHttpHelper.applyConfiguredHeaders(request, config).get()) {
                ensureSuccessful(response, "resolve Skosmos term");
                JsonObject payload = response.readEntity(JsonObject.class);
                JsonArray graph = payload.getJsonArray("graph");
                if (graph == null) {
                    return Optional.empty();
                }

                String vocabularyName = null;
                String vocabularyUri = null;
                String label = null;

                for (JsonValue graphValue : graph) {
                    if (graphValue.getValueType() != JsonValue.ValueType.OBJECT) {
                        continue;
                    }
                    JsonObject graphEntry = graphValue.asJsonObject();
                    if (uri.equals(graphEntry.getString("uri", null))) {
                        label = labelFromPrefLabel(graphEntry.get("prefLabel"), context.getLanguage());
                    }
                    if ("skos:ConceptScheme".equals(graphEntry.getString("type", ""))) {
                        vocabularyUri = graphEntry.getString("uri", null);
                        vocabularyName = labelFromPrefLabel(graphEntry.get("prefLabel"), context.getLanguage());
                    }
                }

                return Optional.of(new ExternalVocabularyTerm(uri, firstNonBlank(label, uri), vocabularyName, vocabularyUri, getProtocol()));
            }
        } catch (ExternalVocabularyException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalVocabularyException("Failed to resolve Skosmos term.", e);
        }
    }

    private static String getFirstVocabularyKey(JsonObject config) {
        JsonObject vocabs = config.getJsonObject("vocabs");
        if (vocabs == null || vocabs.isEmpty()) {
            return "";
        }
        return vocabs.keySet().iterator().next();
    }

    private static String getVocabularyUri(JsonObject config, String vocabulary) {
        JsonObject vocabs = config.getJsonObject("vocabs");
        if (vocabs == null || !vocabs.containsKey(vocabulary)) {
            return null;
        }
        return vocabs.getJsonObject(vocabulary).getString("vocabularyUri", null);
    }

    private static String labelFromPrefLabel(JsonValue prefLabel, String language) {
        if (prefLabel == null) {
            return null;
        }
        if (prefLabel.getValueType() == JsonValue.ValueType.STRING) {
            return ((JsonString) prefLabel).getString();
        }
        if (prefLabel.getValueType() == JsonValue.ValueType.OBJECT) {
            return prefLabel.asJsonObject().getString("value", null);
        }
        if (prefLabel.getValueType() != JsonValue.ValueType.ARRAY) {
            return null;
        }

        String english = null;
        String first = null;
        for (JsonValue labelValue : prefLabel.asJsonArray()) {
            if (labelValue.getValueType() != JsonValue.ValueType.OBJECT) {
                continue;
            }
            JsonObject label = labelValue.asJsonObject();
            String value = label.getString("value", null);
            String lang = label.getString("lang", "");
            if (first == null) {
                first = value;
            }
            if ("en".equals(lang)) {
                english = value;
            }
            if (language != null && language.equals(lang)) {
                return value;
            }
        }
        return firstNonBlank(english, first);
    }

    private static void ensureSuccessful(Response response, String action) throws ExternalVocabularyException {
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            throw new ExternalVocabularyException("Failed to " + action + ". External service returned HTTP " + response.getStatus() + ".");
        }
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}
