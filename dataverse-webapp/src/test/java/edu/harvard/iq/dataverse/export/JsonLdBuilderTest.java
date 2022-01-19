package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
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
import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class JsonLdBuilderTest {

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
    public void beforeEach() {
        Dataverse dv = new Dataverse();
        dv.setName("RepOD");

        when(dataFileService.isSameTermsOfUse(any(), any())).thenReturn(true);
        when(systemConfig.getDataverseSiteUrl()).thenReturn("localhost");
        when(settingsService.getValueForKey(SettingsServiceBean.Key.HideSchemaDotOrgDownloadUrls)).thenReturn("true");

        jsonLdBuilder = new JsonLdBuilder(dataFileService, settingsService, systemConfig);
    }

    @Test
    public void testGetJsonLd() throws ParseException {
        Dataset dataset = new Dataset();
        dataset.setProtocol("doi");
        dataset.setAuthority("10.5072/FK2");
        dataset.setIdentifier("LK0D1H");
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setDataset(dataset);
        datasetVersion.setVersionState(DatasetVersion.VersionState.DRAFT);
        assertEquals("", datasetVersion.getPublicationDateAsString());
        // Only published datasets return any JSON.
        assertEquals("", jsonLdBuilder.buildJsonLd(datasetVersion));
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);
        datasetVersion.setVersionNumber(1L);
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");
        Date publicationDate = dateFmt.parse("19551105");
        datasetVersion.setReleaseTime(publicationDate);
        dataset.setPublicationDate(new Timestamp(publicationDate.getTime()));
        Dataverse dataverse = new Dataverse();
        dataverse.setName("LibraScholar");
        dataset.setOwner(dataverse);
        String jsonLd = jsonLdBuilder.buildJsonLd(datasetVersion);
        System.out.println("jsonLd: " + JsonUtil.prettyPrint(jsonLd));
        JsonReader jsonReader = Json.createReader(new StringReader(jsonLd));
        JsonObject obj = jsonReader.readObject();
        assertEquals("http://schema.org", obj.getString("@context"));
        assertEquals("Dataset", obj.getString("@type"));
        assertEquals("https://doi.org/10.5072/FK2/LK0D1H", obj.getString("@id"));
        assertEquals("https://doi.org/10.5072/FK2/LK0D1H", obj.getString("identifier"));
        assertEquals(null, obj.getString("schemaVersion", null));
        assertEquals("CreativeWork", obj.getJsonObject("license").getString("@type"));
        assertEquals("Different licenses or terms for individual files", obj.getJsonObject("license").getString("name"));
        assertEquals("1955-11-05", obj.getString("dateModified"));
        assertEquals("1955-11-05", obj.getString("datePublished"));
        assertEquals("1", obj.getString("version"));
        // TODO: if it ever becomes easier to mock a dataset title, test it.
        assertEquals("", obj.getString("name"));
        // TODO: If it ever becomes easier to mock authors, test them.
        JsonArray emptyArray = Json.createArrayBuilder().build();
        assertEquals(emptyArray, obj.getJsonArray("creator"));
        // TODO: If it ever becomes easier to mock subjects, test them.
        assertEquals(emptyArray, obj.getJsonArray("keywords"));
        assertEquals("Organization", obj.getJsonObject("publisher").getString("@type"));
        assertEquals("LibraScholar", obj.getJsonObject("publisher").getString("name"));
        assertEquals("Organization", obj.getJsonObject("provider").getString("@type"));
        assertEquals("LibraScholar", obj.getJsonObject("provider").getString("name"));
        assertEquals("LibraScholar", obj.getJsonObject("includedInDataCatalog").getString("name"));
    }

    @Test
    public void testGetJsonLdNonCC0License() throws ParseException {
        Dataset dataset = new Dataset();
        dataset.setProtocol("doi");
        dataset.setAuthority("10.5072/FK2");
        dataset.setIdentifier("LK0D1H");
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setDataset(dataset);
        datasetVersion.setVersionState(DatasetVersion.VersionState.DRAFT);
        assertEquals("", datasetVersion.getPublicationDateAsString());
        // Only published datasets return any JSON.
        assertEquals("", jsonLdBuilder.buildJsonLd(datasetVersion));
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);
        datasetVersion.setVersionNumber(1L);
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");
        Date publicationDate = dateFmt.parse("19551105");
        datasetVersion.setReleaseTime(publicationDate);
        dataset.setPublicationDate(new Timestamp(publicationDate.getTime()));
        Dataverse dataverse = new Dataverse();
        dataverse.setName("LibraScholar");
        dataset.setOwner(dataverse);

        String jsonLd = jsonLdBuilder.buildJsonLd(datasetVersion);
        System.out.println("jsonLd: " + JsonUtil.prettyPrint(jsonLd));
        JsonReader jsonReader = Json.createReader(new StringReader(jsonLd));
        JsonObject obj = jsonReader.readObject();
        assertEquals("http://schema.org", obj.getString("@context"));
        assertEquals("Dataset", obj.getString("@type"));
        assertEquals("https://doi.org/10.5072/FK2/LK0D1H", obj.getString("@id"));
        assertEquals("https://doi.org/10.5072/FK2/LK0D1H", obj.getString("identifier"));
        assertEquals(null, obj.getString("schemaVersion", null));
        assertEquals("CreativeWork", obj.getJsonObject("license").getString("@type"));
        assertEquals("Different licenses or terms for individual files", obj.getJsonObject("license").getString("name"));
        assertEquals("1955-11-05", obj.getString("dateModified"));
        assertEquals("1955-11-05", obj.getString("datePublished"));
        assertEquals("1", obj.getString("version"));
        // TODO: if it ever becomes easier to mock a dataset title, test it.
        assertEquals("", obj.getString("name"));
        // TODO: If it ever becomes easier to mock authors, test them.
        JsonArray emptyArray = Json.createArrayBuilder().build();
        assertEquals(emptyArray, obj.getJsonArray("creator"));
        // TODO: If it ever becomes easier to mock subjects, test them.
        assertEquals(emptyArray, obj.getJsonArray("keywords"));
        assertEquals("Organization", obj.getJsonObject("publisher").getString("@type"));
        assertEquals("LibraScholar", obj.getJsonObject("publisher").getString("name"));
        assertEquals("Organization", obj.getJsonObject("provider").getString("@type"));
        assertEquals("LibraScholar", obj.getJsonObject("provider").getString("name"));
        assertEquals("LibraScholar", obj.getJsonObject("includedInDataCatalog").getString("name"));
    }

}
