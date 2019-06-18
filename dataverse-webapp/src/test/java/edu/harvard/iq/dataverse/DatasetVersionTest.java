package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.mocks.MocksFactory;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static edu.harvard.iq.dataverse.mocks.MocksFactory.makeFileMetadata;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author michael
 * @author tjanek
 */
public class DatasetVersionTest {

    public DatasetVersionTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testComparator() {
        DatasetVersion ds1_0 = new DatasetVersion();
        ds1_0.setId(0l);
        ds1_0.setVersionNumber(1l);
        ds1_0.setMinorVersionNumber(0l);
        ds1_0.setVersionState(DatasetVersion.VersionState.RELEASED);

        DatasetVersion ds1_1 = new DatasetVersion();
        ds1_1.setId(1l);
        ds1_1.setVersionNumber(1l);
        ds1_1.setMinorVersionNumber(1l);
        ds1_1.setVersionState(DatasetVersion.VersionState.RELEASED);

        DatasetVersion ds2_0 = new DatasetVersion();
        ds2_0.setId(2l);
        ds2_0.setVersionNumber(2l);
        ds2_0.setMinorVersionNumber(0l);
        ds2_0.setVersionState(DatasetVersion.VersionState.RELEASED);

        DatasetVersion ds_draft = new DatasetVersion();
        ds_draft.setId(3l);
        ds_draft.setVersionState(DatasetVersion.VersionState.DRAFT);

        List<DatasetVersion> expected = Arrays.asList(ds1_0, ds1_1, ds2_0, ds_draft);
        List<DatasetVersion> actual = Arrays.asList(ds2_0, ds1_0, ds_draft, ds1_1);
        Collections.sort(actual, DatasetVersion.compareByVersion);
        assertEquals(expected, actual);
    }

    @Test
    public void testIsInReview() {
        Dataset ds = MocksFactory.makeDataset();

        DatasetVersion draft = ds.getCreateVersion();
        draft.setVersionState(DatasetVersion.VersionState.DRAFT);
        ds.addLock(new DatasetLock(DatasetLock.Reason.InReview, MocksFactory.makeAuthenticatedUser("Lauren", "Ipsumowitch")));
        assertTrue(draft.isInReview());

        DatasetVersion nonDraft = new DatasetVersion();
        nonDraft.setVersionState(DatasetVersion.VersionState.RELEASED);
        assertEquals(false, nonDraft.isInReview());

        ds.addLock(null);
        assertFalse(nonDraft.isInReview());
    }

    /**
     * See also SchemaDotOrgExporterTest.java for more extensive tests.
     */
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
        assertEquals("", datasetVersion.getJsonLd());
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
        terms.setLicense(TermsOfUseAndAccess.License.CC0);
        datasetVersion.setTermsOfUseAndAccess(terms);
        String jsonLd = datasetVersion.getJsonLd();
        System.out.println("jsonLd: " + JsonUtil.prettyPrint(jsonLd));
        JsonReader jsonReader = Json.createReader(new StringReader(jsonLd));
        JsonObject obj = jsonReader.readObject();
        assertEquals("http://schema.org", obj.getString("@context"));
        assertEquals("Dataset", obj.getString("@type"));
        assertEquals("https://doi.org/10.5072/FK2/LK0D1H", obj.getString("@id"));
        assertEquals("https://doi.org/10.5072/FK2/LK0D1H", obj.getString("identifier"));
        assertEquals(null, obj.getString("schemaVersion", null));
        assertEquals("Dataset", obj.getJsonObject("license").getString("@type"));
        assertEquals("CC0", obj.getJsonObject("license").getString("text"));
        assertEquals("https://creativecommons.org/publicdomain/zero/1.0/", obj.getJsonObject("license").getString("url"));
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
        // Only published datasets return any JSON.
        assertEquals("", datasetVersion.getJsonLd());
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
        terms.setLicense(TermsOfUseAndAccess.License.NONE);
        terms.setTermsOfUse("Call me maybe");
        datasetVersion.setTermsOfUseAndAccess(terms);

        String jsonLd = datasetVersion.getJsonLd();
        System.out.println("jsonLd: " + JsonUtil.prettyPrint(jsonLd));
        JsonReader jsonReader = Json.createReader(new StringReader(jsonLd));
        JsonObject obj = jsonReader.readObject();
        assertEquals("http://schema.org", obj.getString("@context"));
        assertEquals("Dataset", obj.getString("@type"));
        assertEquals("https://doi.org/10.5072/FK2/LK0D1H", obj.getString("@id"));
        assertEquals("https://doi.org/10.5072/FK2/LK0D1H", obj.getString("identifier"));
        assertEquals(null, obj.getString("schemaVersion", null));
        assertEquals("Dataset", obj.getJsonObject("license").getString("@type"));
        assertEquals("Call me maybe", obj.getJsonObject("license").getString("text"));
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
    public void shouldSortFileMetadataByDisplayOrder() {
        // given
        DatasetVersion version = withUnSortedFiles();

        // when
        List<FileMetadata> orderedMetadatas = version.getFileMetadatasSorted();

        // then
        verifySortOrder(orderedMetadatas, "file4.png", 0);
        verifySortOrder(orderedMetadatas, "file3.png", 1);
        verifySortOrder(orderedMetadatas, "file5.png", 2);
        verifySortOrder(orderedMetadatas, "file2.png", 3);
        verifySortOrder(orderedMetadatas, "file6.png", 4);
        verifySortOrder(orderedMetadatas, "file1.png", 5);
    }

    @Test
    public void shouldAddNewFileMetadataWithProperDisplayOrder() {
        // given
        DatasetVersion version = withFilesAndCustomDisplayOrder();
        FileMetadata toAdd = makeFileMetadata(40L, "file4.png", 0);

        // when
        version.addFileMetadata(toAdd);

        // then
        verifyDisplayOrder(version.getFileMetadatas(), 0, "file1.png", 1);
        verifyDisplayOrder(version.getFileMetadatas(), 1, "file2.png", 6);
        verifyDisplayOrder(version.getFileMetadatas(), 2, "file3.png", 8);
        verifyDisplayOrder(version.getFileMetadatas(), 3, "file4.png", 9);
    }

    @Test
    public void shouldAddNewFileMetadataOnEmptyMetadatasWithZeroIndex() {
        // given
        DatasetVersion version = new DatasetVersion();
        FileMetadata toAdd = makeFileMetadata(40L, "file1.png", -5); // fake -5 displayOrder

        // when
        version.addFileMetadata(toAdd);

        // then
        verifyDisplayOrder(version.getFileMetadatas(), 0, "file1.png", 0);
    }

    private void verifySortOrder(List<FileMetadata> metadatas, String label, int expectedOrderIndex) {
        assertEquals(label, metadatas.get(expectedOrderIndex).getLabel());
    }

    private void verifyDisplayOrder(List<FileMetadata> metadatas, int index, String label, int displayOrder) {
        assertEquals(label, metadatas.get(index).getLabel());
        assertEquals(displayOrder, metadatas.get(index).getDisplayOrder());
    }

    private DatasetVersion withUnSortedFiles() {
        DatasetVersion datasetVersion = new DatasetVersion();

        datasetVersion.setFileMetadatas(newArrayList(
                makeFileMetadata(10L, "file2.png", 3),
                makeFileMetadata(20L, "file1.png", 5),
                makeFileMetadata(30L, "file3.png", 1),
                makeFileMetadata(40L, "file4.png", 0),
                makeFileMetadata(50L, "file5.png", 2),
                makeFileMetadata(60L, "file6.png", 4)
        ));

        return datasetVersion;
    }

    private DatasetVersion withFilesAndCustomDisplayOrder() {
        DatasetVersion datasetVersion = new DatasetVersion();

        datasetVersion.setFileMetadatas(newArrayList(
                makeFileMetadata(10L, "file1.png", 1),
                makeFileMetadata(20L, "file2.png", 6),
                makeFileMetadata(30L, "file3.png", 8)
        ));

        return datasetVersion;
    }
}
