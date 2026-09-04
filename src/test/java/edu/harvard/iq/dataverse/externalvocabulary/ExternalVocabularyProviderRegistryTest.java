package edu.harvard.iq.dataverse.externalvocabulary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

class ExternalVocabularyProviderRegistryTest {

    @Test
    void findsBuiltInProviderByProtocol() {
        ExternalVocabularyProviderRegistry registry = new ExternalVocabularyProviderRegistry();

        assertInstanceOf(SkosmosExternalVocabularyProvider.class, registry.find("skosmos").orElseThrow());
    }

    @Test
    void fallsBackToRuntimeHttpJsonProvider() {
        ExternalVocabularyProviderRegistry registry = new ExternalVocabularyProviderRegistry();
        JsonObject config = readObject("""
                {
                  "protocol": "ror",
                  "provider": {
                    "type": "http-json",
                    "search-uri": "https://api.ror.org/v2/organizations?query={encodeUrl:query}"
                  }
                }
                """);

        assertInstanceOf(HttpJsonExternalVocabularyProvider.class, registry.find(config).orElseThrow());
    }

    @Test
    void readsFilteredPathThroughArray() {
        JsonObject payload = readObject("""
                {
                  "id": "https://ror.org/03vek6s52",
                  "names": [
                    {
                      "types": ["acronym"],
                      "value": "HU"
                    },
                    {
                      "types": ["ror_display", "label"],
                      "value": "Harvard University"
                    }
                  ]
                }
                """);

        String value = ExternalVocabularyJsonPath.firstStringAt(payload, "/names/types=ror_display/value", "");

        assertEquals("Harvard University", value);
    }

    @Test
    void doesNotResolveUnknownRuntimeProviderWithoutHttpJsonConfiguration() {
        ExternalVocabularyProviderRegistry registry = new ExternalVocabularyProviderRegistry();
        JsonObject config = readObject("""
                {
                  "protocol": "ror"
                }
                """);

        assertTrue(registry.find(config).isEmpty());
    }

    private static JsonObject readObject(String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readObject();
        }
    }
}
