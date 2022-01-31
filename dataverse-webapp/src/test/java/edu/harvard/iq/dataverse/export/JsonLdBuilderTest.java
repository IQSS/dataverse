package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.harvard.iq.dataverse.persistence.MocksFactory.create;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JsonLdBuilderTest {

    @InjectMocks
    private JsonLdBuilder jsonLdBuilder;

    @Mock
    private DataFileServiceBean dataFileService;

    @Mock
    private SettingsServiceBean settingsService;

    @Mock
    private SystemConfig systemConfig;

    /**
     * See also SchemaDotOrgExporterTest.java for more extensive tests.
     */

    @BeforeEach
    void beforeEach() {
        Dataverse dv = new Dataverse();
        dv.setName("RepOD");

        when(dataFileService.isSameTermsOfUse(any(), any())).thenReturn(true);
        when(systemConfig.getDataverseSiteUrl()).thenReturn("localhost");
        when(settingsService.getValueForKey(SettingsServiceBean.Key.HideSchemaDotOrgDownloadUrls)).thenReturn("true");

        jsonLdBuilder = new JsonLdBuilder(dataFileService, settingsService, systemConfig);
    }

    // -------------------- TESTS --------------------

    @Test
    @DisplayName("Should return empty string for unpublished dataset")
    void getJsonLd__unpublished() {
        // given
        Dataset dataset = new Dataset();
        dataset.setProtocol("doi");
        dataset.setAuthority("10.5072/FK2");
        dataset.setIdentifier("LK0D1H");

        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setDataset(dataset);
        datasetVersion.setVersionState(DatasetVersion.VersionState.DRAFT);

        // when
        String json = jsonLdBuilder.buildJsonLd(datasetVersion);

        // then
        assertThat(json).isEmpty();
    }

    @Test
    void testGetJsonLd() throws ParseException {
        // given
        Dataverse dataverse = new Dataverse();
        dataverse.setName("LibraScholar");

        Dataset dataset = new Dataset();
        dataset.setProtocol("doi");
        dataset.setAuthority("10.5072/FK2");
        dataset.setIdentifier("LK0D1H");
        dataset.setOwner(dataverse);

        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setDataset(dataset);
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);
        datasetVersion.setVersionNumber(1L);

        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");
        Date publicationDate = dateFmt.parse("19551105");
        datasetVersion.setReleaseTime(publicationDate);
        dataset.setPublicationDate(new Timestamp(publicationDate.getTime()));

        // when
        String jsonLd = jsonLdBuilder.buildJsonLd(datasetVersion);

        // then
        JsonReader jsonReader = Json.createReader(new StringReader(jsonLd));
        JsonObject obj = jsonReader.readObject();
        assertEquals("http://schema.org", obj.getString("@context"));
        assertEquals("Dataset", obj.getString("@type"));
        assertEquals("https://doi.org/10.5072/FK2/LK0D1H", obj.getString("@id"));
        assertEquals("https://doi.org/10.5072/FK2/LK0D1H", obj.getString("identifier"));
        assertNull(obj.getString("schemaVersion", null));
        assertEquals("CreativeWork", obj.getJsonObject("license").getString("@type"));
        assertEquals("Different licenses or terms for individual files", obj.getJsonObject("license").getString("name"));
        assertEquals("1955-11-05", obj.getString("dateModified"));
        assertEquals("1955-11-05", obj.getString("datePublished"));
        assertEquals("1", obj.getString("version"));
        assertEquals("", obj.getString("name"));
        JsonArray emptyArray = Json.createArrayBuilder().build();
        assertEquals(emptyArray, obj.getJsonArray("creator"));
        assertEquals(emptyArray, obj.getJsonArray("keywords"));
        assertEquals("Organization", obj.getJsonObject("publisher").getString("@type"));
        assertEquals("LibraScholar", obj.getJsonObject("publisher").getString("name"));
        assertEquals("Organization", obj.getJsonObject("provider").getString("@type"));
        assertEquals("LibraScholar", obj.getJsonObject("provider").getString("name"));
        assertEquals("LibraScholar", obj.getJsonObject("includedInDataCatalog").getString("name"));
    }

    @Test
    void getFunders() {
        // given
        DatasetVersion version = new DatasetVersion();
        version.setDatasetFields(IntStream.range(1, 10)
                .mapToObj(i -> {
                    switch(i % 3) {
                        case 0:
                            return create(DatasetFieldConstant.contributor, "",
                                    create(DatasetFieldConstant.contributorName, "funder-" + i),
                                    create(DatasetFieldConstant.contributorType, "Funder"));
                        case 1:
                            return create(DatasetFieldConstant.contributor, "",
                                    create(DatasetFieldConstant.contributorName, "editor-" + i),
                                    create(DatasetFieldConstant.contributorType, "Editor"));
                        case 2:
                            return create(DatasetFieldConstant.grantNumber, "",
                                    create(DatasetFieldConstant.grantNumberAgency, "agency-" + i));
                        default:
                            throw new RuntimeException("Unexpected");
                    }
                }).collect(Collectors.toList()));
        // when
        List<String> funders = jsonLdBuilder.getFunders(version);

        // then
        assertThat(funders)
                .containsExactlyInAnyOrder("funder-3", "funder-6", "funder-9", "agency-2", "agency-5", "agency-8");
    }

    @Test
    void getTimePeriodCovered() {
        // given
        DatasetVersion version = new DatasetVersion();
        version.setDatasetFields(Arrays.asList(
                createTimePeriod(null, null),
                createTimePeriod(null, "07-01-2022"),
                createTimePeriod("01-01-2022", null),
                createTimePeriod("01-01-2022", "07-01-2022"),
                createTimePeriod("08-01-2022", "13-01-2022")));

        // when
        List<String> timePeriodsCovered = jsonLdBuilder.getTimePeriodsCovered(version);

        // then
        assertThat(timePeriodsCovered)
                .containsExactlyInAnyOrder("01-01-2022/07-01-2022", "08-01-2022/13-01-2022");
    }

    @Test
    void getSpatialCoverages() {
        // given
        DatasetVersion version = new DatasetVersion();
        version.setDatasetFields(Arrays.asList(
                createSpatialCoverage("Warsaw", null, "Poland", null),
                createSpatialCoverage("Albany", "NY", "USA", "other"),
                createSpatialCoverage(null, null, null, null)));

        // when
        List<String> spatialCoverages = jsonLdBuilder.getSpatialCoverages(version);

        // then
        assertThat(spatialCoverages)
                .containsExactlyInAnyOrder("Warsaw, Poland", "Albany, NY, USA, other");
    }

    @Test
    void getTopicsClassification() {
        // given
        DatasetVersion version = new DatasetVersion();
        version.setDatasetFields(Arrays.asList(
                create("topicClassification", ""),
                create("topicClassification", "", create("topicClassValue", "")),
                create("topicClassification", "", create("topicClassValue", null)),
                create("topicClassification", "", create("topicClassValue", "classification-value-1")),
                create("topicClassification", "", create("topicClassValue", "classification-value-2"))));

        // when
        List<String> classifications = jsonLdBuilder.getTopicClassifications(version);

        // then
        assertThat(classifications)
                .containsExactlyInAnyOrder("classification-value-1", "classification-value-2");
    }

    // -------------------- PRIVATE --------------------

    private DatasetField createTimePeriod(String start, String end) {
        return create(DatasetFieldConstant.timePeriodCovered, "",
                create(DatasetFieldConstant.timePeriodCoveredStart, start),
                create(DatasetFieldConstant.timePeriodCoveredEnd, end));
    }

    private DatasetField createSpatialCoverage(String city, String state, String country, String other) {
        return create(DatasetFieldConstant.geographicCoverage, "",
                create(DatasetFieldConstant.city, city),
                create(DatasetFieldConstant.state, state),
                create(DatasetFieldConstant.country, country),
                create(DatasetFieldConstant.otherGeographicCoverage, other));
    }
}
