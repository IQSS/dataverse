package edu.harvard.iq.dataverse.externalvocabulary;

import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldType;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Stateless
public class ExternalVocabularyServiceBean {

    @EJB
    DatasetFieldServiceBean datasetFieldService;

    private final ExternalVocabularyProviderRegistry providerRegistry = new ExternalVocabularyProviderRegistry();

    public JsonArrayBuilder getConfiguredExternalVocabularies() {
        JsonArrayBuilder configs = Json.createArrayBuilder();
        Map<Long, JsonObject> cvocConf = datasetFieldService.getCVocConf(false);
        for (JsonObject config : cvocConf.values()) {
            configs.add(toSanitizedConfig(config));
        }
        return configs;
    }

    public Optional<JsonObject> findConfigByFieldName(String fieldName) {
        DatasetFieldType fieldType = datasetFieldService.findByNameOpt(fieldName);
        if (fieldType == null) {
            return Optional.empty();
        }

        JsonObject byTermField = datasetFieldService.getCVocConf(true).get(fieldType.getId());
        if (byTermField != null) {
            return Optional.of(byTermField);
        }

        JsonObject byParentField = datasetFieldService.getCVocConf(false).get(fieldType.getId());
        if (byParentField != null) {
            return Optional.of(byParentField);
        }

        return Optional.empty();
    }

    public JsonObjectBuilder toSanitizedConfig(JsonObject config) {
        return NullSafeJsonBuilder.jsonObjectBuilder()
                .add("fieldName", config.getString("field-name", ""))
                .add("termUriField", config.getString("term-uri-field", config.getString("field-name", "")))
                .add("protocol", config.getString("protocol", ""))
                .add("allowFreeText", config.getBoolean("allow-free-text", false))
                .add("languages", config.getString("languages", ""))
                .add("termParentUri", config.getString("term-parent-uri", ""))
                .add("vocabs", config.containsKey("vocabs") ? config.getJsonObject("vocabs") : Json.createObjectBuilder().build())
                .add("managedFields", config.containsKey("managed-fields") ? config.getJsonObject("managed-fields") : Json.createObjectBuilder().build());
    }

    public List<ExternalVocabularyTerm> search(String fieldName, String query, String vocabulary, String language)
            throws ExternalVocabularyException {
        JsonObject config = configOrThrow(fieldName);
        ExternalVocabularyProvider provider = providerOrThrow(config);
        return provider.search(query, config, new ExternalVocabularyContext(vocabulary, language));
    }

    public Optional<ExternalVocabularyTerm> resolve(String fieldName, String uri, String language)
            throws ExternalVocabularyException {
        JsonObject config = configOrThrow(fieldName);
        ExternalVocabularyProvider provider = providerOrThrow(config);
        return provider.resolve(uri, config, new ExternalVocabularyContext(null, language));
    }

    public boolean validate(String fieldName, String value) throws ExternalVocabularyException {
        JsonObject config = configOrThrow(fieldName);
        JsonObject vocabs = config.getJsonObject("vocabs");
        if (value == null || value.isBlank()) {
            return true;
        }
        if (vocabs == null) {
            return false;
        }

        boolean valid = false;
        boolean couldBeFreeText = true;
        boolean freeTextAllowed = config.getBoolean("allow-free-text", false);
        for (String vocabName : vocabs.keySet()) {
            JsonObject vocab = vocabs.getJsonObject(vocabName);
            String baseUri = vocab.getString("uriSpace", "");
            if (!baseUri.isEmpty() && value.startsWith(baseUri)) {
                valid = true;
                break;
            }
            if (baseUri.contains("://")) {
                String protocol = baseUri.substring(baseUri.indexOf("://") + 3);
                if (value.startsWith(protocol)) {
                    couldBeFreeText = false;
                }
            }
        }
        return valid || (freeTextAllowed && couldBeFreeText);
    }

    private JsonObject configOrThrow(String fieldName) throws ExternalVocabularyException {
        return findConfigByFieldName(fieldName)
                .orElseThrow(() -> new ExternalVocabularyException("No external vocabulary is configured for field " + fieldName + "."));
    }

    private ExternalVocabularyProvider providerOrThrow(JsonObject config) throws ExternalVocabularyException {
        String protocol = config.getString("protocol", "");
        return providerRegistry.find(config)
                .orElseThrow(() -> new ExternalVocabularyException(
                        "No external vocabulary provider is available for protocol " + protocol
                                + ". Configure provider.type=http-json or add a built-in adapter."));
    }
}
