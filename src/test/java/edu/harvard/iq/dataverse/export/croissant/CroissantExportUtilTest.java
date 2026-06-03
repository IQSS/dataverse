package edu.harvard.iq.dataverse.export.croissant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
    void testGetReviews() throws IOException {
        String content = Files.readString(Path.of("doc/sphinx-guides/source/_static/api/list-reviews.json"), StandardCharsets.UTF_8);
        JsonObject apiResponseJson = JsonUtil.getJsonObject(content);
        JsonObjectBuilder job = Json.createObjectBuilder(apiResponseJson.getJsonObject("data"));
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
