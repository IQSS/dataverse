package edu.harvard.iq.dataverse;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.Mockito;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import jakarta.json.Json;
import jakarta.json.JsonObject;

public class DatasetFieldServiceBeanTest {

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
    void getIndexableStringsByTermUriSkosmos() throws IOException {
        String fieldName = "keyword";
        String termURI = "http://aims.fao.org/aos/agrovoc/c_2389";

        JsonObject cvocEntry = prepare(fieldName, "src/test/resources/json/cvoc-skosmos.json");

        JsonObject getExtVocabValueReturnedValue = Json.createObjectBuilder()
                .add("@id", termURI)
                .add("termName", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("lang", "fr")
                                .add("value", "faux bourdon"))
                        .add(Json.createObjectBuilder()
                                .add("lang", "en")
                                .add("value", "drone (insects)")))
                .add("vocabularyUri", "http://aims.fao.org/aos/agrovoc")
                .add("synonyms", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("lang", "fr")
                                .add("value", "Abeille mâle"))
                        .add(Json.createObjectBuilder()
                                .add("lang", "en")
                                .add("value", "drone honey bees")))
                .add("genericTerm", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("lang", "fr")
                                .add("value", "Colonie d'abeilles"))
                        .add(Json.createObjectBuilder()
                                .add("lang", "en")
                                .add("value", "bee colonies")))
                .build();
        Mockito.doReturn(getExtVocabValueReturnedValue).when(datasetFieldServiceBean).getExternalVocabularyValue(termURI);
        Mockito.doReturn(null).when(datasetFieldServiceBean).getExternalVocabularyValue(AdditionalMatchers.not(Mockito.eq(termURI)));

        // keywordTermURL
        Set<String> result = datasetFieldServiceBean.getIndexableStringsByTermUri(termURI, cvocEntry, "keywordTermURL");
        assertEquals(Set.of("faux bourdon", "drone (insects)"), result);

        // keywordValue
        result = datasetFieldServiceBean.getIndexableStringsByTermUri(termURI, cvocEntry, "keywordValue");
        assertEquals(Collections.emptySet(), result, "Only 'keywordTermURL' must return values for Skosmos");

        // Any others field
        result = datasetFieldServiceBean.getIndexableStringsByTermUri(termURI, cvocEntry, "");
        assertEquals(Collections.emptySet(), result, "Only 'keywordTermURL' must return values for Skosmos");

        // Another termURI not in database
        result = datasetFieldServiceBean.getIndexableStringsByTermUri("http://example.org/uuid", cvocEntry, "keywordTermURL");
        assertEquals(Collections.emptySet(), result);
    }

    @Test
    void getIndexableStringsByTermUriAgroportal() throws IOException {
        String fieldName = "keyword";
        String termURI = "http://aims.fao.org/aos/agrovoc/c_50265";

        JsonObject cvocEntry = prepare(fieldName, "src/test/resources/json/cvoc-agroportal.json");

        JsonObject getExtVocabValueReturnedValue = Json.createObjectBuilder()
                .add("@id", termURI)
                .add("termName", Json.createObjectBuilder()
                        .add("fr", "association de quartier")
                        .add("en", "neighborhood associations"))
                .add("vocabularyName", "https://data.agroportal.lirmm.fr/ontologies/AGROVOC")
                .add("vocabularyUri", "https://data.agroportal.lirmm.fr/ontologies/AGROVOC")
                .add("synonyms", Json.createObjectBuilder()
                        .add("en", Json.createArrayBuilder().add("neighborhood societies")))
                .build();
        Mockito.doReturn(getExtVocabValueReturnedValue).when(datasetFieldServiceBean).getExternalVocabularyValue(termURI);
        Mockito.doReturn(null).when(datasetFieldServiceBean).getExternalVocabularyValue(AdditionalMatchers.not(Mockito.eq(termURI)));

        // keywordValue
        Set<String> result = datasetFieldServiceBean.getIndexableStringsByTermUri(termURI, cvocEntry, "keywordValue");
        assertEquals(Set.of("association de quartier", "neighborhood associations", "neighborhood societies"), result);

        // keywordTermURL
        result = datasetFieldServiceBean.getIndexableStringsByTermUri(termURI, cvocEntry, "keywordTermURL");
        assertEquals(Collections.emptySet(), result, "Only 'keywordValue' must return values for Agroportal");

        // Any others field
        result = datasetFieldServiceBean.getIndexableStringsByTermUri(termURI, cvocEntry, "");
        assertEquals(Collections.emptySet(), result, "Only 'keywordValue' must return values for Agroportal");

        // Another termURI not in database
        result = datasetFieldServiceBean.getIndexableStringsByTermUri("http://example.org/uuid", cvocEntry, "keywordValue");
        assertEquals(Collections.emptySet(), result);
    }

    @Test
    void getIndexableStringsByTermUriOrcid() throws IOException {
        String fieldName = "creator";
        String termURI = "https://orcid.org/0000-0003-4217-153X";

        JsonObject cvocEntry = prepare(fieldName, "src/test/resources/json/cvoc-orcid.json");

        JsonObject getExtVocabValueReturnedValue = Json.createObjectBuilder()
                .add("@id", termURI)
                .add("scheme", "ORCID")
                .add("@type", "https://schema.org/Person")
                .add("personName", "Doe, John")
                .build();
        Mockito.doReturn(getExtVocabValueReturnedValue).when(datasetFieldServiceBean).getExternalVocabularyValue(termURI);
        Mockito.doReturn(null).when(datasetFieldServiceBean).getExternalVocabularyValue(AdditionalMatchers.not(Mockito.eq(termURI)));

        // ORCID match with "personName" field into "getIndexableStringsByTermUri" method
        Set<String> result = datasetFieldServiceBean.getIndexableStringsByTermUri(termURI, cvocEntry, "");
        assertEquals(Set.of("Doe, John"), result);

        // Another termURI not in database
        result = datasetFieldServiceBean.getIndexableStringsByTermUri("http://example.org/uuid", cvocEntry, fieldName);
        assertEquals(Collections.emptySet(), result);
    }

    /**
     * Prepare unit tests with mock methods.
     *
     * @param fieldName "field-name" into cvoc configuration file
     * @param jsonPath path of the JSON configuration file: src/test/resources/json/...
     * @return {@link JsonObject} representing the configuration file
     * @throws IOException in case on read error on configuration file
     */
    JsonObject prepare(String fieldName, String jsonPath) throws IOException {
        Long dftId = Long.parseLong("1");
        // DatasetFieldType name corresponding to "field-name" into cvoc configuration file
        DatasetFieldType dft = new DatasetFieldType(fieldName, DatasetFieldType.FieldType.NONE, true);
        dft.setId(dftId);

        Mockito.doReturn(dft).when(datasetFieldServiceBean).findByNameOpt(fieldName);
        Mockito.doReturn(null).when(datasetFieldServiceBean).findByNameOpt(AdditionalMatchers.not(Mockito.eq(fieldName)));

        SettingsServiceBean settingsService = Mockito.mock(SettingsServiceBean.class);
        Mockito.when(settingsService.getValueForKey(SettingsServiceBean.Key.CVocConf)).thenReturn(getCvocJson(jsonPath));
        datasetFieldServiceBean.settingsService = settingsService;

        return datasetFieldServiceBean.getCVocConf(false).get(dftId);
    }

}
