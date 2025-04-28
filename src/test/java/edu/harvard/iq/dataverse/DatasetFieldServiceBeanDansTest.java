package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class DatasetFieldServiceBeanDansTest {

    private DatasetFieldServiceBean datasetFieldServiceBean;

    static String getCvocJson(String pathToJsonFile) throws IOException {
        final File datasetVersionJson = new File(pathToJsonFile);
        return new String(Files.readAllBytes(Paths.get(datasetVersionJson.getAbsolutePath())));
    }

    @BeforeEach
    void setUp() {
      this.datasetFieldServiceBean = Mockito.spy(new DatasetFieldServiceBean());

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
        List<String> expectedValues = List.of(
            "Exploitation and management of the physical environment",
            "Exploitatie en beheer van het fysieke milieu"
        );
        assertThat(result.getString("@id")).isEqualTo("https://www.narcis.nl/classification/D18100");
        assertTermNameValues(result, expectedValues);
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName", "vocabularyUri");
        // TODO shouldn't we also get the vocabularyName?
    }

    @Test
    void getIndexableStringsForAudience() throws Exception {
        String termURI = "https://www.narcis.nl/classification/D18100";
        JsonObject cvocEntry = createMocks("dansAudience", termURI, "audience.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/audience.json");

        JsonObject result = (JsonObject) reflectFilterResponse().invoke(datasetFieldServiceBean, cvocEntry, readObject, termURI);

        List<String> expectedValues = List.of(
            "Exploitation and management of the physical environment",
            "Exploitatie en beheer van het fysieke milieu"
        );
        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertTermNameValues(result, expectedValues);
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName", "vocabularyUri");
    }

    @Test
    void getIndexableStringsForAbrPeriod1() throws Exception {
        String termURI = "https://data.cultureelerfgoed.nl/term/id/abr/19679187-0ac4-4127-b4cd-09a348400585";
        JsonObject cvocEntry = createMocks("dansAbrPeriod", termURI, "abrPeriod.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/abrPeriod-1.json");

        JsonObject result = (JsonObject) reflectFilterResponse().invoke(datasetFieldServiceBean, cvocEntry, readObject, termURI);

        List<String> expectedValues = List.of(
            "Vroege Middeleeuwen D"
        );
        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertTermNameValues(result, expectedValues);
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName", "vocabularyUri");
    }

    @Test
    void getIndexableStringsForAbrPeriod2() throws Exception {
        String termURI = "https://data.cultureelerfgoed.nl/term/id/abr/19679187-0ac4-4127-b4cd-09a348400585";
        JsonObject cvocEntry = createMocks("dansAbrPeriod", termURI, "abrPeriod.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/abrPeriod-2.json");

        JsonObject result = (JsonObject) reflectFilterResponse().invoke(datasetFieldServiceBean, cvocEntry, readObject, termURI);

        List<String> expectedValues = List.of(
            "Eerste Wereldoorlog"
        );
        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertTermNameValues(result, expectedValues);
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName", "vocabularyUri");
    }

    private void assertTermNameValues(JsonObject result, List<String> expectedValues) {
        assertThat(termNameValues(result))
            .withFailMessage("Expected result with termName values: %s but got: %s", expectedValues, result)
            .containsExactlyInAnyOrderElementsOf(expectedValues);
    }

    private @NotNull List<String> termNameValues(JsonObject result) {
        var termName = result.getJsonArray("termName");
        if (termName == null) {
            return List.of();
        }
        return termName.stream()
            .map(jsonValue -> ((JsonObject) jsonValue).getString("value"))
            .collect(Collectors.toList());
    }

    private JsonObject readObject(String pathname) throws FileNotFoundException {
        var reader = new FileReader(pathname);
        return Json.createReader(reader).readObject();
    }

    private @NotNull Method reflectFilterResponse() throws NoSuchMethodException {
        Method filterResponseMethod = DatasetFieldServiceBean.class.getDeclaredMethod("filterResponse", JsonObject.class, JsonObject.class, String.class);
        filterResponseMethod.setAccessible(true);
        return filterResponseMethod;
    }

    /**
     * Prepare unit tests with mock methods.
     *
     * @param fieldName "field-termUri" into cvoc configuration file
     * @param termURI
     * @param jsonFileName    name of the JSON configuration/value files in: src/test/resources/json/cvoc-dans-config/ respectively src/test/resources/json/cvoc-dans-value/
     * @return {@link JsonObject} representing the configuration file
     * @throws IOException in case on read error on one of the files file
     */
    JsonObject createMocks(String fieldName, String termURI, String jsonFileName) throws IOException {
        Long dftId = Long.parseLong("1");
        // DatasetFieldType termUri corresponding to "field-termUri" into cvoc configuration file
        DatasetFieldType dft = new DatasetFieldType(fieldName, DatasetFieldType.FieldType.NONE, true);
        dft.setId(dftId);

        Mockito.doReturn(dft).when(datasetFieldServiceBean).findByNameOpt(fieldName);
        Mockito.doReturn(null).when(datasetFieldServiceBean).findByNameOpt(AdditionalMatchers.not(Mockito.eq(fieldName)));

        SettingsServiceBean settingsService = Mockito.mock(SettingsServiceBean.class);
        fieldName = getCvocJson("src/test/resources/json/cvoc-dans-config/" + jsonFileName);
        Mockito.when(settingsService.getValueForKey(SettingsServiceBean.Key.CVocConf)).thenReturn(fieldName);
        datasetFieldServiceBean.settingsService = settingsService;

        return datasetFieldServiceBean.getCVocConf(false).get(dftId);
    }
}
