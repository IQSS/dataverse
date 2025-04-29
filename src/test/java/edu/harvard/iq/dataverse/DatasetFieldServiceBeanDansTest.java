package edu.harvard.iq.dataverse;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class DatasetFieldServiceBeanDansTest {

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
    void interceptedResultForAudience() { // TODO remove when assertTermNameValues succeeds in another test
        JsonObject result = Json.createReader(new StringReader("""
            {
               "@id": "https://www.narcis.nl/classification/D18100",
               "termName": [
                 {
                   "lang": "nl",
                   "value": "Exploitatie en beheer van het fysieke milieu"
                 },
                 {
                   "lang": "en",
                   "value": "Exploitation and management of the physical environment"
                 }
               ],
               "vocabularyUri": "https://www.narcis.nl/classification/"
            }
            """)).readObject();
        Map<String, String> expectedValues = Map.of(
            "en", "Exploitation and management of the physical environment",
            "nl", "Exploitatie en beheer van het fysieke milieu"
        );
        assertThat(result.getString("@id")).isEqualTo("https://www.narcis.nl/classification/D18100");
        assertTermNameValues(result, expectedValues);
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName", "vocabularyUri", "vocabularyName");
    }

    @Test
    void filterResponseForAudience() throws Exception {
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
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName", "vocabularyUri");
    }

    @Test
    void filterResponseForAbrPeriod() throws Exception {
        String termURI = "https://data.cultureelerfgoed.nl/term/id/abr/19679187-0ac4-4127-b4cd-09a348400585";
        JsonObject cvocEntry = readObject("src/test/resources/json/cvoc-dans-config/abrPeriod.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/abrPeriod.json");

        JsonObject result = callFilterResponse(cvocEntry, readObject, termURI);

        Map<String, String> expectedValues = Map.of(
            "nl", "Vroege Middeleeuwen D"
        );
        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertTermNameValues(result, expectedValues);
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName", "vocabularyUri");
    }

    private void assertTermNameValues(JsonObject result, Map<String, String> expectedValues) {
        assertThat(termNameValues(result))
            .withFailMessage("Expected result with termName values: %s but got: %s", expectedValues, result)
            .containsExactlyInAnyOrderEntriesOf(expectedValues);
    }

    private Map<String, String> termNameValues(JsonObject result) {
        var termName = result.getJsonArray("termName");
        if (termName == null) {
            return Map.of();
        }
        return termName.stream()
            .map(jsonValue -> (JsonObject) jsonValue)
            .collect(Collectors.toMap(
                jsonObject -> jsonObject.getString("lang"),
                jsonObject -> jsonObject.getString("value")
            ));
    }

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
