package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.branding.BrandingUtilTest;
import edu.harvard.iq.dataverse.license.License;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import java.io.StringReader;
import java.net.URI;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

public class DatasetVersionTest {
    
    private static final Logger logger = Logger.getLogger(DatasetVersion.class.getCanonicalName());
    
    @BeforeAll
    public static void setUp() {
        BrandingUtilTest.setupMocks();
    }
    
    @AfterAll
    public static void tearDown() {
        BrandingUtilTest.setupMocks();
    }
    
    @Test
    public void testComparator() {
        DatasetVersion ds1_0 = new DatasetVersion();
        ds1_0.setId(0l);
        ds1_0.setVersionNumber( 1l );
        ds1_0.setMinorVersionNumber( 0l );
        ds1_0.setVersionState(DatasetVersion.VersionState.RELEASED);
        
        DatasetVersion ds1_1 = new DatasetVersion();
        ds1_1.setId(1l);
        ds1_1.setVersionNumber( 1l );
        ds1_1.setMinorVersionNumber( 1l );
        ds1_1.setVersionState(DatasetVersion.VersionState.RELEASED);
        
        DatasetVersion ds2_0 = new DatasetVersion();
        ds2_0.setId(2l);
        ds2_0.setVersionNumber( 2l );
        ds2_0.setMinorVersionNumber( 0l );
        ds2_0.setVersionState(DatasetVersion.VersionState.RELEASED);
        
        DatasetVersion ds_draft = new DatasetVersion();
        ds_draft.setId(3l);
        ds_draft.setVersionState(DatasetVersion.VersionState.DRAFT);
        
        List<DatasetVersion> expected = Arrays.asList( ds1_0, ds1_1, ds2_0, ds_draft );
        List<DatasetVersion> actual = Arrays.asList( ds2_0, ds1_0, ds_draft, ds1_1 );
        Collections.sort(actual, DatasetVersion.compareByVersion);
        assertEquals( expected, actual );
    }

    @Test
    public void testIsInReview() {
        Dataset ds = MocksFactory.makeDataset();
        
        DatasetVersion draft = ds.getCreateVersion(null);
        draft.setVersionState(DatasetVersion.VersionState.DRAFT);
        ds.addLock(new DatasetLock(DatasetLock.Reason.InReview, MocksFactory.makeAuthenticatedUser("Lauren", "Ipsumowitch")));
        assertTrue(draft.isInReview());

        DatasetVersion nonDraft = new DatasetVersion();
        nonDraft.setVersionState(DatasetVersion.VersionState.RELEASED);
        assertFalse(nonDraft.isInReview());
        
        ds.addLock(null);
        assertFalse(nonDraft.isInReview());
    }

    /**
     * See also SchemaDotOrgExporterTest.java for more extensive tests.
     */
    @Test
    public void testGetJsonLd() throws ParseException {
        Dataset dataset = new Dataset();
        License license = new License("CC0 1.0", "You can copy, modify, distribute and perform the work, even for commercial purposes, all without asking permission.", URI.create("http://creativecommons.org/publicdomain/zero/1.0"), URI.create("/resources/images/cc0.png"), true, 1l);
        license.setDefault(true);
        dataset.setProtocol("doi");
        dataset.setAuthority("10.5072/FK2");
        dataset.setIdentifier("LK0D1H");
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setDataset(dataset);
        datasetVersion.setVersionState(DatasetVersion.VersionState.DRAFT);
        assertEquals("", datasetVersion.getPublicationDateAsString());
        // TODO make some assertions on drafts
//        String jsonLdDraft = datasetVersion.getJsonLd();
//        logger.fine("jsonLdDraft: " + JsonUtil.prettyPrint(jsonLdDraft));
//        JsonReader jsonReaderDraft = Json.createReader(new StringReader(jsonLdDraft));
//        JsonObject objDraft = jsonReaderDraft.readObject();
//        assertEquals("http://schema.org", objDraft.getString("@context"));
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);
        datasetVersion.setVersionNumber(1L);
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");
        Date publicationDate = dateFmt.parse("19551105");
        datasetVersion.setReleaseTime(publicationDate);
        dataset.setPublicationDate(new Timestamp(publicationDate.getTime()));
        Dataverse dataverse = new Dataverse();
        dataverse.setName("LibraScholar");
        dataset.setOwner(dataverse);
        TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
        terms.setLicense(license);
        datasetVersion.setTermsOfUseAndAccess(terms);
        String jsonLd = datasetVersion.getJsonLd();
        logger.fine("jsonLd: " + JsonUtil.prettyPrint(jsonLd));
        JsonReader jsonReader = Json.createReader(new StringReader(jsonLd));
        JsonObject obj = jsonReader.readObject();
        assertEquals("http://schema.org", obj.getString("@context"));
        assertEquals("Dataset", obj.getString("@type"));
        assertEquals("https://doi.org/10.5072/FK2/LK0D1H", obj.getString("@id"));
        assertEquals("https://doi.org/10.5072/FK2/LK0D1H", obj.getString("identifier"));
        assertEquals(null, obj.getString("schemaVersion", null));
        assertEquals("http://creativecommons.org/publicdomain/zero/1.0", obj.getString("license"));
        assertEquals("1955-11-05", obj.getString("dateModified"));
        assertEquals("1955-11-05", obj.getString("datePublished"));
        assertEquals("1", obj.getString("version"));
        // TODO: if it ever becomes easier to mock a dataset title, test it.
        assertEquals("", obj.getString("name"));
        // TODO: If it ever becomes easier to mock authors, test them.
        JsonArray emptyArray = Json.createArrayBuilder().build();
        assertEquals(emptyArray, obj.getJsonArray("creator"));
        assertEquals(emptyArray, obj.getJsonArray("author"));
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
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);
        datasetVersion.setVersionNumber(1L);
        datasetVersion.setMinorVersionNumber(0L);
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");
        Date publicationDate = dateFmt.parse("19551105");
        datasetVersion.setReleaseTime(publicationDate);
        dataset.setPublicationDate(new Timestamp(publicationDate.getTime()));
        Dataverse dataverse = new Dataverse();
        dataverse.setName("LibraScholar");
        dataset.setOwner(dataverse);

        TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
        terms.setLicense(null);
        terms.setTermsOfUse("Call me maybe");
        datasetVersion.setTermsOfUseAndAccess(terms);

        String jsonLd = datasetVersion.getJsonLd();
        logger.fine("jsonLd: " + JsonUtil.prettyPrint(jsonLd));
        JsonReader jsonReader = Json.createReader(new StringReader(jsonLd));
        JsonObject obj = jsonReader.readObject();
        assertEquals("http://schema.org", obj.getString("@context"));
        assertEquals("Dataset", obj.getString("@type"));
        assertEquals("https://doi.org/10.5072/FK2/LK0D1H", obj.getString("@id"));
        assertEquals("https://doi.org/10.5072/FK2/LK0D1H", obj.getString("identifier"));
        assertEquals(null, obj.getString("schemaVersion", null));
        assertTrue(obj.getString("license").contains("/api/datasets/:persistentId/versions/1.0/customlicense?persistentId=doi:10.5072/FK2/LK0D1H"));
        assertEquals("1955-11-05", obj.getString("dateModified"));
        assertEquals("1955-11-05", obj.getString("datePublished"));
        assertEquals("1", obj.getString("version"));
        // TODO: if it ever becomes easier to mock a dataset title, test it.
        assertEquals("", obj.getString("name"));
        // TODO: If it ever becomes easier to mock authors, test them.
        JsonArray emptyArray = Json.createArrayBuilder().build();
        assertEquals(emptyArray, obj.getJsonArray("creator"));
        assertEquals(emptyArray, obj.getJsonArray("author"));
        // TODO: If it ever becomes easier to mock subjects, test them.
        assertEquals(emptyArray, obj.getJsonArray("keywords"));
        assertEquals("Organization", obj.getJsonObject("publisher").getString("@type"));
        assertEquals("LibraScholar", obj.getJsonObject("publisher").getString("name"));
        assertEquals("Organization", obj.getJsonObject("provider").getString("@type"));
        assertEquals("LibraScholar", obj.getJsonObject("provider").getString("name"));
        assertEquals("LibraScholar", obj.getJsonObject("includedInDataCatalog").getString("name"));
    }

}
