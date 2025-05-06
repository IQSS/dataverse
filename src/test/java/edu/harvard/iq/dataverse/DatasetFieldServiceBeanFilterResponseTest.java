package edu.harvard.iq.dataverse;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
        // https://vocabs.datastations.nl/rest/v1/NARCIS/data&format=application/json&uri=https://www.narcis.nl/classification/D13700

        JsonObject result = callFilterResponse(cvocEntry, readObject, termURI);

        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertThat(getValue("nl", result.get("vocabularyName"))).startsWith("Classificatiecodes van de wetenschapsportal Narcis (www.narcis.nl).");
        assertThat(getValue("nl", result.get("termName"))).startsWith("Theoretische chemie, kwantumchemie");
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName", "vocabularyUri", "vocabularyName");
    }

    @Test
    void collection() throws Exception {
        String termURI = "https://vocabularies.dans.knaw.nl/collections/archaeology/ArcheoDepot";
        JsonObject cvocEntry = readObject("src/test/resources/json/cvoc-dans-config/collection.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/collection.json");

        JsonObject result = callFilterResponse(cvocEntry, readObject, termURI);

        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertThat(getValue("nl", result.get("vocabularyName"))).isEqualTo("DansCollections");
        assertThat(getValue("nl", result.get("termName"))).isEqualTo("ArcheoDepot");
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName", "vocabularyUri", "vocabularyName");
    }

    @Test
    void aatClassification() throws Exception {
        String termURI = "http://vocab.getty.edu/aat/300187008";
        JsonObject cvocEntry = readObject("src/test/resources/json/cvoc-dans-config/AATClassification.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/AATClassification.json");

        JsonObject result = callFilterResponse(cvocEntry, readObject, termURI);

        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertThat(getValue("en", result.get("vocabularyName"))).isEqualTo("The Art and Architecture Thesaurus Concepts");
        assertThat(getValue("nl", result.get("termName"))).isEqualTo("Abnakee kleden");
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName", "vocabularyUri", "vocabularyName");
    }

    @Test
    void abrPeriod() throws Exception {
        String termURI = "https://data.cultureelerfgoed.nl/term/id/abr/533f6881-7c2d-49fc-bce6-71a839558c0f";
        JsonObject cvocEntry = readObject("src/test/resources/json/cvoc-dans-config/abrPeriod.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/abrPeriod.json");

        JsonObject result = callFilterResponse(cvocEntry, readObject, termURI);

        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertThat(result.getString("vocabularyName")).isEqualTo("ABR perioden");
        assertThat(getValue("nl", result.get("termName"))).isEqualTo("Vroege Middeleeuwen D");
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName", "vocabularyUri", "vocabularyName");
    }

    @Test
    void abrArtifact() throws Exception {
        String termURI = "https://data.cultureelerfgoed.nl/term/id/abr/db8feb21-8ddc-432f-8062-a3a15f7f7cf4";
        JsonObject cvocEntry = readObject("src/test/resources/json/cvoc-dans-config/abrArtifact.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/abrArtifact.json");

        JsonObject result = callFilterResponse(cvocEntry, readObject, termURI);

        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertThat(result.getString("vocabularyName")).isEqualTo("ABR artifact");
        assertThat(getValue("nl", result.get("termName"))).isEqualTo("steen");
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName", "vocabularyUri", "vocabularyName");
    }

    @Test
    void abrVerwervingswijze() throws Exception {
        String termURI = "https://data.cultureelerfgoed.nl/term/id/abr/b7f4fe13-d7b4-4fb9-a7a8-c25ef74b612d";
        JsonObject cvocEntry = readObject("src/test/resources/json/cvoc-dans-config/abrVerwervingswijze.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/abrVerwervingswijze.json");

        JsonObject result = callFilterResponse(cvocEntry, readObject, termURI);

        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertThat(result.getString("vocabularyName")).isEqualTo("ABR verwervingswijzen");
        assertThat(getValue("nl", result.get("termName"))).isEqualTo("archeologisch: booronderzoek");
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName", "vocabularyUri", "vocabularyName");
    }

    @Test
    void abrRapportType() throws Exception {
        String termURI = "https://data.cultureelerfgoed.nl/term/id/abr/5701cb3d-0ffd-4663-98e0-fab808448109";
        JsonObject cvocEntry = readObject("src/test/resources/json/cvoc-dans-config/abrRapportType.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/abrRapportType.json");
        // https://vocabs.datastations.nl/rest/v1/ABR/data&format=application/json&uri=https://data.cultureelerfgoed.nl/term/id/abr/5701cb3d-0ffd-4663-98e0-fab808448109

        JsonObject result = callFilterResponse(cvocEntry, readObject, termURI);

        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertThat(result.getString("vocabularyName")).isEqualTo("ABR rapporten");
        assertThat(getValue("nl", result.get("termName"))).isEqualTo("Achterhoekse Archeologische Publicaties");
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName", "vocabularyUri", "vocabularyName");
    }

    @Test
    void abrComplex() throws Exception {
        String termURI = "https://data.cultureelerfgoed.nl/term/id/abr/1b5b4dd1-f4f8-4e4c-9108-a6fb2c606cde";
        JsonObject cvocEntry = readObject("src/test/resources/json/cvoc-dans-config/abrComplex.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/abrComplex.json");

        JsonObject result = callFilterResponse(cvocEntry, readObject, termURI);
        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertThat(result.getString("vocabularyName")).isEqualTo("ABR complextypen");
        assertThat(getValue("nl", result.get("termName"))).isEqualTo("(ring)walburg");
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName", "vocabularyUri", "vocabularyName");
    }
    private String getValue(String nl, JsonValue values) {
        if (values instanceof JsonArray) {
            for (var item : (JsonArray) values) {
                JsonObject obj = item.asJsonObject();
                if (nl.equals(obj.getString("lang", ""))) {
                    return obj.getString("value");
                }
            }
        } else if (values instanceof JsonObject) {
            JsonObject obj = (JsonObject) values;
            if (nl.equals(obj.getString("lang", ""))) {
                return obj.getString("value");
            }
        }
        return null; // Return null if no match is found
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
