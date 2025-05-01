package edu.harvard.iq.dataverse;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class DatasetFieldServiceBeanFilterResponseTest {

    private DatasetFieldServiceBean datasetFieldServiceBean;

    @BeforeEach
    void setUp() {
        this.datasetFieldServiceBean = new DatasetFieldServiceBean();

//        Logger rootLogger = Logger.getLogger(DatasetFieldServiceBean.class.getCanonicalName());
//        rootLogger.setLevel(Level.FINE);
//
//        // Ensure all handlers respect the FINE level
//        for (var handler : rootLogger.getHandlers()) {
//            handler.setLevel(Level.FINE);
//        }
//
//        // Add a ConsoleHandler if none exists
//        if (rootLogger.getHandlers().length == 0) {
//            ConsoleHandler consoleHandler = new ConsoleHandler();
//            rootLogger.addHandler(consoleHandler);
//        }
    }

    @AfterEach
    void tearDown() {
      this.datasetFieldServiceBean = null;
    }

    @Test
    void audience() throws Exception {
        String termURI = "https://www.narcis.nl/classification/D13700";
        JsonObject cvocEntry = readObject("src/test/resources/json/cvoc-dans-config/audience.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/audience.json");

        JsonObject result = callFilterResponse(cvocEntry, readObject, termURI);

        Map<String, String> expectedValues = Map.of(
            "en", "Theoretical chemistry, quantum chemistry",
            "nl", "Theoretische chemie, kwantumchemie"
        );
        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertTermNameValues(result, expectedValues);
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName", "vocabularyUri", "vocabularyName");
    }

    @Test
    void collection() throws Exception {
        String termURI = "https://vocabularies.dans.knaw.nl/collections/archaeology/ArcheoDepot";
        JsonObject cvocEntry = readObject("src/test/resources/json/cvoc-dans-config/collection.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/collection.json");

        JsonObject result = callFilterResponse(cvocEntry, readObject, termURI);

        Map<String, String> expectedValues = Map.of(
            "nl", "ArcheoDepot"
        );
        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertTermNameValues(result, expectedValues);
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName", "vocabularyUri", "vocabularyName");
    }

    @Test
    void abrPeriod() throws Exception {
        String termURI = "https://data.cultureelerfgoed.nl/term/id/abr/533f6881-7c2d-49fc-bce6-71a839558c0f";
        JsonObject cvocEntry = readObject("src/test/resources/json/cvoc-dans-config/abrPeriod.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/abrPeriod.json");

        JsonObject result = callFilterResponse(cvocEntry, readObject, termURI);

        Map<String, String> expectedValues = Map.of(
            "nl", "Vroege Middeleeuwen D"
        );
        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertTermNameValues(result, expectedValues);
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName");
    }

    @Test
    void abrArtifact() throws Exception {
        String termURI = "https://data.cultureelerfgoed.nl/term/id/abr/db8feb21-8ddc-432f-8062-a3a15f7f7cf4";
        JsonObject cvocEntry = readObject("src/test/resources/json/cvoc-dans-config/abrArtifact.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/abrArtifact.json");

        JsonObject result = callFilterResponse(cvocEntry, readObject, termURI);

        Map<String, String> expectedValues = Map.of(
            "nl", "steen"
        );
        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertThat(result.getString("vocabularyName")).isEqualTo("ABR artifact");
        assertTermNameValues(result, expectedValues);
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName", "vocabularyName", "vocabularyUri");
    }

    @Test
    void abrVerwervingswijze() throws Exception {
        String termURI = "https://data.cultureelerfgoed.nl/term/id/abr/b7f4fe13-d7b4-4fb9-a7a8-c25ef74b612d";
        JsonObject cvocEntry = readObject("src/test/resources/json/cvoc-dans-config/abrVerwervingswijze.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/abrVerwervingswijze.json");

        JsonObject result = callFilterResponse(cvocEntry, readObject, termURI);

        Map<String, String> expectedValues = Map.of(
            "nl", "archeologisch: booronderzoek"
        );
        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertTermNameValues(result, expectedValues);
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName");
    }

    @Test
    void abrRapportType() throws Exception {
        String termURI = "https://data.cultureelerfgoed.nl/term/id/abr/5701cb3d-0ffd-4663-98e0-fab808448109";
        JsonObject cvocEntry = readObject("src/test/resources/json/cvoc-dans-config/abrRapportType.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/abrRapportType.json");

        JsonObject result = callFilterResponse(cvocEntry, readObject, termURI);

        Map<String, String> expectedValues = Map.of(
            "nl", "Achterhoekse Archeologische Publicaties"
        );
        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertTermNameValues(result, expectedValues);
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName");
    }

    @Test
    void abrComplex() throws Exception {
        String termURI = "https://data.cultureelerfgoed.nl/term/id/abr/1b5b4dd1-f4f8-4e4c-9108-a6fb2c606cde";
        JsonObject cvocEntry = readObject("src/test/resources/json/cvoc-dans-config/abrComplex.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/abrComplex.json");

        JsonObject result = callFilterResponse(cvocEntry, readObject, termURI);

        Map<String, String> expectedValues = Map.of(
            "nl", "(ring)walburg"
        );
        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertTermNameValues(result, expectedValues);
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName");
    }

    private void assertTermNameValues(JsonObject result, Map<String, String> expectedValues) {
        assertThat(termNameValues(result))
            .withFailMessage("Expected result with termName values: %s but got: %s", expectedValues, result)
            .containsExactlyInAnyOrderEntriesOf(expectedValues);
    }

    private Map<String, String> termNameValues(JsonObject result) {
        var termName = result.get("termName");
        if (termName == null) {
            return Map.of();
        }
        if(termName instanceof JsonArray) {
            return ((JsonArray) termName).stream()
                .map(jsonValue -> (JsonObject) jsonValue)
                .collect(Collectors.toMap(
                    jsonObject -> jsonObject.getString("lang"),
                    jsonObject -> jsonObject.getString("value")
                ));
        } else {
            var tn = (JsonObject) termName;
            return Map.of(tn.getString("lang"), tn.getString("value"));
        }
    }

    // TODO add more tests for other CVOC entries

    private JsonObject readObject(String pathname) throws FileNotFoundException {
        var reader = new FileReader(pathname);
        return Json.createReader(reader).readObject();
    }

    private @NotNull JsonObject callFilterResponse(JsonObject cvocEntry, JsonObject readObject, String termURI) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method filterResponseMethod = DatasetFieldServiceBean.class.getDeclaredMethod("filterResponse", JsonObject.class, JsonObject.class, String.class);
        filterResponseMethod.setAccessible(true);
        return (JsonObject) filterResponseMethod.invoke(datasetFieldServiceBean, cvocEntry, readObject, termURI);
    }
}
