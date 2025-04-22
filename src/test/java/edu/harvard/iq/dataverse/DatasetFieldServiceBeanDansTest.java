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
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatasetFieldServiceBeanDansTest {

    private DatasetFieldServiceBean datasetFieldServiceBean;

    static String getCvocJson(String pathToJsonFile) throws IOException {
        final File datasetVersionJson = new File(pathToJsonFile);
        return new String(Files.readAllBytes(Paths.get(datasetVersionJson.getAbsolutePath())));
    }

    @BeforeEach
    void setUp() {
      this.datasetFieldServiceBean = Mockito.spy(new DatasetFieldServiceBean());
    }

    @AfterEach
    void tearDown() {
      this.datasetFieldServiceBean = null;
    }

    @Test
    void getIndexableStringsForAudience() throws Exception {
        String termURI = "https://www.narcis.nl/classification/D13000";
        JsonObject cvocEntry = createMocks("dansAudience", termURI, "audience.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/audience.json");

        String result = reflectFilterResponse().invoke(datasetFieldServiceBean, cvocEntry, readObject, termURI).toString();

        assertTrue(result.contains(termURI));
        assertTrue(result.contains("Exploitation and management of the physical environment"));
        assertTrue(result.contains("Exploitatie en beheer van het fysieke milieu"));
    }

    @Test
    void getIndexableStringsForAbrPeriod() throws Exception {
        String termURI = "https://data.cultureelerfgoed.nl/term/id/abr/02aea074-a4a9-4335-9698-bec9a188964e";
        JsonObject cvocEntry = createMocks("dansAbrPeriod", termURI, "abrPeriod.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/abrPeriod.json");

        String result = reflectFilterResponse().invoke(datasetFieldServiceBean, cvocEntry, readObject, termURI).toString();

        assertTrue(result.contains(termURI));
        assertTrue(result.contains("Vroege Middeleeuwen D"));
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
