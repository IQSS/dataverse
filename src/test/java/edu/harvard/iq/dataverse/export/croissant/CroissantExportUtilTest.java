package edu.harvard.iq.dataverse.export.croissant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.util.json.JsonUtil;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;

public class CroissantExportUtilTest {

    @Test
    void testGetReviews() {
        // TODO consider reading this instead: doc/sphinx-guides/source/_static/api/list-reviews.json
        String content = """
{
  "reviews": [
    {
      "title": "Review of Pediatric Asthma",
      "authors": [
        "Wazowski, Mike"
      ],
      "persistentId": "doi:10.5072/FK2/UWVWPY",
      "persistentIdUrl": "https://doi.org/10.5072/FK2/UWVWPY",
      "id": 243,
      "citation": "Wazowski, Mike, 2026, \\\"Review of Pediatric Asthma\\\", https://doi.org/10.5072/FK2/UWVWPY, Root, V1",
      "citationHtml": "Wazowski, Mike, 2026, \\\"Review of Pediatric Asthma\\\", <a href=\\\"https://doi.org/10.5072/FK2/UWVWPY\\\" target=\\\"_blank\\\">https://doi.org/10.5072/FK2/UWVWPY</a>, Root, V1",
      "datePublished": "2026-05-27T19:45:20Z",
      "description": "This is a review of a dataset.",
      "rubricMetadataBlocks": [
        {
          "name": "rubric_trusteddatadimensionsintensities",
          "displayName": "Trusted Data Dimensions and Intensities",
          "fields": [
            {
              "typeName": "biasEquityAndRepresentativeness",
              "value": "Low"
            },
            {
              "typeName": "authorAndProvenance",
              "value": "Medium"
            },
            {
              "typeName": "fitnessForScopeAndContextualRelevance",
              "value": "Medium"
            },
            {
              "typeName": "integrityAndUsability",
              "value": "High"
            },
            {
              "typeName": "transparencyOfMethodsAndDocumentation",
              "value": "Low"
            },
            {
              "typeName": "licensingAndLegalClarity",
              "value": "High"
            }
          ]
        }
      ]
    }
  ]
}
                        """;
        JsonObject croissantJson = JsonUtil.getJsonObject(content);
        JsonObjectBuilder job = Json.createObjectBuilder(croissantJson);
        JsonObject result = CroissantExportUtil.getReviews(job).build();
        System.out.println(prettyPrint(result));
        assertTrue(result.getJsonArray("reviews").size() == 1);
        assertEquals("CriticReview", result.getJsonArray("reviews").get(0).asJsonObject().getString("@type"));
    }

    public static String prettyPrint(JsonObject jsonObject) {
        Map<String, Boolean> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory jsonWriterFactory = Json.createWriterFactory(config);
        StringWriter stringWriter = new StringWriter();
        try (JsonWriter jsonWriter = jsonWriterFactory.createWriter(stringWriter)) {
            jsonWriter.writeObject(jsonObject);
        }
        return stringWriter.toString();
    }

}
