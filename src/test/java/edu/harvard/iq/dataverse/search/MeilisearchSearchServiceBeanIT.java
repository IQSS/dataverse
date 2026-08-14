package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import edu.harvard.iq.dataverse.util.testing.Tags;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.io.StringReader;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(Tags.INTEGRATION_TEST)
@Tag(Tags.USES_TESTCONTAINERS)
@Testcontainers(disabledWithoutDocker = true)
@LocalJvmSettings
@JvmSetting(key = JvmSettings.MEILISEARCH_URL, method = "meilisearchUrl")
@JvmSetting(key = JvmSettings.MEILISEARCH_API_KEY, value = MeilisearchSearchServiceBeanIT.API_KEY)
class MeilisearchSearchServiceBeanIT {

    static final String API_KEY = "dataverse-test-master-key";
    static final int PORT = 7700;

    @Container
    static GenericContainer<?> meilisearch = new GenericContainer<>("getmeili/meilisearch:v1.53.1")
            .withExposedPorts(PORT)
            .withEnv("MEILI_MASTER_KEY", API_KEY)
            .waitingFor(Wait.forHttp("/health").forPort(PORT).withStartupTimeout(Duration.ofMinutes(2)));

    @BeforeAll
    static void addDocuments() throws Exception {
        try (Client client = ClientBuilder.newClient()) {
            JsonArray documents = Json.createArrayBuilder()
                    .add(Json.createObjectBuilder()
                            .add("id", 1)
                            .add("pid", "doi:10.5072/FK2/FINCHES")
                            .add("title", "Darwin's Finches"))
                    .add(Json.createObjectBuilder()
                            .add("id", 2)
                            .add("pid", "doi:10.5072/FK2/SPARROWS")
                            .add("title", "House Sparrows"))
                    .build();
            try (Response response = client.target(meilisearchUrl()).path("indexes/datasets/documents")
                    .queryParam("primaryKey", "id")
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY)
                    .post(Entity.entity(documents.toString(), MediaType.APPLICATION_JSON_TYPE))) {
                assertEquals(202, response.getStatus());
                waitForTask(client, parseObject(response.readEntity(String.class)).getInt("taskUid"));
            }
        }
    }

    @Test
    void queriesRealMeilisearchServer() throws Exception {
        MeilisearchSearchServiceBean service = new MeilisearchSearchServiceBean();

        LinkedHashMap<String, Float> results = service.queryMeilisearch("finches", 10);

        assertEquals(1, results.size());
        assertEquals("doi:10.5072/FK2/FINCHES", results.keySet().iterator().next());
        assertTrue(results.values().iterator().next() > 0F);
    }

    static String meilisearchUrl() {
        return "http://" + meilisearch.getHost() + ":" + meilisearch.getMappedPort(PORT);
    }

    private static void waitForTask(Client client, int taskUid) throws Exception {
        for (int attempt = 0; attempt < 100; attempt++) {
            try (Response response = client.target(meilisearchUrl()).path("tasks").path(Integer.toString(taskUid))
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + API_KEY)
                    .get()) {
                JsonObject task = parseObject(response.readEntity(String.class));
                String status = task.getString("status");
                if ("succeeded".equals(status)) {
                    return;
                }
                if ("failed".equals(status) || "canceled".equals(status)) {
                    throw new IllegalStateException("Meilisearch task did not succeed: " + task);
                }
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("Timed out waiting for Meilisearch task " + taskUid);
    }

    private static JsonObject parseObject(String json) {
        try (var reader = Json.createReader(new StringReader(json))) {
            return reader.readObject();
        }
    }
}
