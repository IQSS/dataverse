package edu.harvard.iq.dataverse.externalvocabulary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

class ExternalVocabularyRetrievalFilterTest {

    @Test
    void mapsProviderPayloadUsingRetrievalFiltering() {
        JsonObject config = readObject("""
                {
                  "retrieval-filtering": {
                    "@context": {
                      "termName": "https://schema.org/name"
                    },
                    "scheme": {
                      "pattern": "http://www.grid.ac/ontology/"
                    },
                    "termName": {
                      "pattern": "{0}",
                      "params": ["/names/types=ror_display/value"]
                    },
                    "@type": {
                      "pattern": "https://schema.org/Organization"
                    }
                  }
                }
                """);
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

        JsonObject mappedFields = ExternalVocabularyRetrievalFilter.map(config, payload, "https://ror.org/03vek6s52");

        assertEquals("Harvard University", mappedFields.getString("termName"));
        assertEquals("http://www.grid.ac/ontology/", mappedFields.getString("scheme"));
        assertEquals("https://schema.org/Organization", mappedFields.getString("@type"));
        assertNull(mappedFields.get("@context"));
    }

    @Test
    void serializesMappedFieldsOnExternalVocabularyTerm() {
        JsonObject mappedFields = Json.createObjectBuilder()
                .add("termName", "Harvard University")
                .add("@type", "https://schema.org/Organization")
                .build();

        JsonObject term = new ExternalVocabularyTerm(
                "https://ror.org/03vek6s52",
                "Harvard University",
                "ROR",
                "https://ror.org/",
                "ror",
                mappedFields).toJsonObjectBuilder().build();

        assertEquals("Harvard University", term.getJsonObject("mappedFields").getString("termName"));
        assertEquals("https://schema.org/Organization", term.getJsonObject("mappedFields").getString("@type"));
    }

    private static JsonObject readObject(String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readObject();
        }
    }
}
