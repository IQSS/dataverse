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

// for debugging purposes
//
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
        // https://vocabs.datastations.nl/NARCIS/en/page/D13700

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
        // https://vocabs.datastations.nl/rest/v1/DansCollections/data?uri=https://vocabularies.dans.knaw.nl/collections/archaeology/ArcheoDepot&format=application/json

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
        // https://vocabs.datastations.nl/AATC/en/page/300187008

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
        // https://vocabs.datastations.nl/ABR/en/page/?uri=https://data.cultureelerfgoed.nl/term/id/abr/533f6881-7c2d-49fc-bce6-71a839558c0f&clang=nl

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
        // https://vocabs.datastations.nl/ABR/en/page/?uri=https://data.cultureelerfgoed.nl/term/id/abr/db8feb21-8ddc-432f-8062-a3a15f7f7cf4&clang=nl

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
        // https://vocabs.datastations.nl/ABR/en/page/?uri=https://data.cultureelerfgoed.nl/term/id/abr/e06a84fa-62e8-42ff-8f38-0ddfe9485a15&clang=nl

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
        // https://vocabs.datastations.nl/ABR/en/page/?uri=https://data.cultureelerfgoed.nl/term/id/abr/5701cb3d-0ffd-4663-98e0-fab808448109

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
        // https://vocabs.datastations.nl/ABR/en/page/?uri=https://data.cultureelerfgoed.nl/term/id/abr/1b5b4dd1-f4f8-4e4c-9108-a6fb2c606cde

        JsonObject result = callFilterResponse(cvocEntry, readObject, termURI);
        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertThat(result.getString("vocabularyName")).isEqualTo("ABR complextypen");
        assertThat(getValue("nl", result.get("termName"))).isEqualTo("(ring)walburg");
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName", "vocabularyUri", "vocabularyName");
    }

    @Test
    void cessda() throws Exception {
        String termURI = "https://vocabularies.cessda.eu/vocabulary/TopicClassification?v=4.2#TradeIndustryAndMarkets.AgricultureAndRuralIndustry";
        JsonObject cvocEntry = readObject("src/test/resources/json/cvoc-dans-config/cessdaClassification.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/cessdaClassification.json");
        // https://vocabs.datastations.nl/TopicClassification_4_2/en/page/?uri=https://vocabularies.cessda.eu/vocabulary/TopicClassification?v=4.2#TradeIndustryAndRuralIndustry

        JsonObject result = callFilterResponse(cvocEntry, readObject, termURI);
        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertThat(result.getString("vocabularyName")).isEqualTo("CESSDA Topis classification 4.2");
        assertThat(getValue("en", result.get("termName"))).isEqualTo("Agriculture and rural industry");
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName", "vocabularyUri", "vocabularyName");
    }

    @Test
    void collectionMode() throws Exception {
        String termURI = "http://rdf-vocabulary.ddialliance.org/cv/ModeOfCollection/4.0/#AutomatedDataExtraction.WebScraping";
        JsonObject cvocEntry = readObject("src/test/resources/json/cvoc-dans-config/collectionMode.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/collectionMode.json");
        // https://vocabs.datastations.nl/ModeOfCollection_4_0/en/page/?uri=http://rdf-vocabulary.ddialliance.org/cv/ModeOfCollection/4.0/#AutomatedDataExtraction.WebScraping

        JsonObject result = callFilterResponse(cvocEntry, readObject, termURI);
        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertThat(result.getString("vocabularyName")).isEqualTo("DDI Alliance Controlled Vocabulary for Analysis Unit 2.1");
        assertThat(getValue("en", result.get("termName"))).isEqualTo("Automated data extraction: Web scraping");
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName", "vocabularyUri", "vocabularyName");
    }

    @Test
    void elsstClassification() throws Exception {
        String termURI = "https://elsst.cessda.eu/id/4/bf0d664a-e89a-4ec3-80d2-04f664b359ab";
        JsonObject cvocEntry = readObject("src/test/resources/json/cvoc-dans-config/elsstClassification.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/elsstClassification.json");
        // https://vocabs.datastations.nl/ELSST_R4/en/page/bf0d664a-e89a-4ec3-80d2-04f664b359ab

        JsonObject result = callFilterResponse(cvocEntry, readObject, termURI);
        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertThat(result.getString("vocabularyName")).isEqualTo("ELSST Thesaurus Version 4");
        assertThat(getValue("nl", result.get("termName"))).isEqualTo("ACADEMISCHE VAARDIGHEDEN");
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName", "vocabularyUri", "vocabularyName");
    }

    @Test
    void researchInstrument() throws Exception {
        String termURI = "http://rdf-vocabulary.ddialliance.org/cv/TypeOfInstrument/1.1/#Questionnaire.Semistructured";
        JsonObject cvocEntry = readObject("src/test/resources/json/cvoc-dans-config/researchInstrument.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/researchInstrument.json");
        // https://vocabs.datastations.nl/TypeOfInstrument_1_1/en/page/?uri=http://rdf-vocabulary.ddialliance.org/cv/TypeOfInstrument/1.1/#Questionnaire.Semistructured

        JsonObject result = callFilterResponse(cvocEntry, readObject, termURI);
        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertThat(result.getString("vocabularyName")).isEqualTo("DDI Alliance Controlled Vocabulary for Type of Instrument 1.1");
        assertThat(getValue("en", result.get("termName"))).isEqualTo("Semi-structured questionnaire");
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName", "vocabularyUri", "vocabularyName");
    }

    @Test
    void samplingProcedureCV() throws Exception {
        String termURI = "http://rdf-vocabulary.ddialliance.org/cv/SamplingProcedure/1.1/#MixedProbabilityNonprobability";
        JsonObject cvocEntry = readObject("src/test/resources/json/cvoc-dans-config/samplingProcedureCV.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/samplingProcedureCV.json");
        // https://vocabs.datastations.nl/SamplingProcedure_1_1/en/page/?uri=http://rdf-vocabulary.ddialliance.org/cv/SamplingProcedure/1.1/#MixedProbabilityNonprobability

        JsonObject result = callFilterResponse(cvocEntry, readObject, termURI);
        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertThat(result.getString("vocabularyName")).isEqualTo("DDI Alliance Controlled Vocabulary for Sampling Procedure 1.1");
        assertThat(getValue("en", result.get("termName"))).isEqualTo("Mixed probability and non-probability");
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName", "vocabularyUri", "vocabularyName");
    }

    @Test
    void timeMethod() throws Exception {
        String termURI = "http://rdf-vocabulary.ddialliance.org/cv/TimeMethod/1.2/#TimeSeries.Discrete";
        JsonObject cvocEntry = readObject("src/test/resources/json/cvoc-dans-config/timeMethod.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/timeMethod.json");
        // https://vocabs.datastations.nl/TimeMethod_1_2/en/page/?uri=http://rdf-vocabulary.ddialliance.org/cv/TimeMethod/1.2/#TimeSeries.Discrete

        JsonObject result = callFilterResponse(cvocEntry, readObject, termURI);
        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertThat(result.getString("vocabularyName")).isEqualTo("DDI Alliance Controlled Vocabulary for Time Method 1.2");
        assertThat(getValue("en", result.get("termName"))).isEqualTo("Time series: Discrete");
        assertThat(result.keySet()).containsExactlyInAnyOrder("@id", "termName", "vocabularyUri", "vocabularyName");
    }

    @Test
    void unitOfAnalysis() throws Exception {
        String termURI = "http://rdf-vocabulary.ddialliance.org/cv/AnalysisUnit/2.1/#EventOrProcessOrActivity";
        JsonObject cvocEntry = readObject("src/test/resources/json/cvoc-dans-config/unitOfAnalysys.json");
        JsonObject readObject = readObject("src/test/resources/json/cvoc-dans-value/unitOfAnalysis.json");
        // https://vocabs.datastations.nl/AnalysisUnit_2_1/en/page/?uri=http://rdf-vocabulary.ddialliance.org/cv/AnalysisUnit/2.1/#EventOrProcessOrActivity

        JsonObject result = callFilterResponse(cvocEntry, readObject, termURI);
        assertThat(result.getString("@id")).isEqualTo(termURI);
        assertThat(result.getString("vocabularyName")).isEqualTo("DDI Alliance Controlled Vocabulary for Analysis Unit 2.1");
        assertThat(getValue("en", result.get("termName"))).isEqualTo("Event/Process/Activity");
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
        } else if (values instanceof JsonObject obj) {
            if (nl.equals(obj.getString("lang", ""))) {
                return obj.getString("value");
            }
        }
        return null; // Return null if no match is found
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
