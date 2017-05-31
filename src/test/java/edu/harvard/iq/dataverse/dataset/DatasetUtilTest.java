package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.FileMetadata;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class DatasetUtilTest {

    public DatasetUtilTest() {
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

    /**
     * Test of getThumbnailCandidates method, of class DatasetUtil.
     */
    @Test
    public void testGetThumbnailCandidates() {
        System.out.println("getThumbnailCandidates");
        Dataset dataset = null;
        boolean considerDatasetLogoAsCandidate = false;
        List<DatasetThumbnail> expResult = new ArrayList<>();
        List<DatasetThumbnail> result = DatasetUtil.getThumbnailCandidates(dataset, considerDatasetLogoAsCandidate);
        assertEquals(expResult, result);
    }

    @Test
    public void testGetThumbnailNull() {
        System.out.println("getThumbnail");
        Dataset dataset = null;
        DatasetThumbnail expResult = null;
        DatasetThumbnail result = DatasetUtil.getThumbnail(dataset);
        assertEquals(expResult, result);
    }

    @Test
    public void testGetThumbnailUseGeneric() {
        System.out.println("testGetThumbnailUseGeneric");
        Dataset dataset = new Dataset();
        dataset.setUseGenericThumbnail(true);
        DatasetThumbnail result = DatasetUtil.getThumbnail(dataset);
        assertNull(result);
    }

    @Test
    public void testGetThumbnailRestricted() {
        System.out.println("testGetThumbnailRestricted");
        Dataset dataset = new Dataset();
        DataFile thumbnailFile = new DataFile();
        thumbnailFile.setId(42l);
        thumbnailFile.setRestricted(true);
        dataset.setThumbnailFile(thumbnailFile);
        DatasetThumbnail result = DatasetUtil.getThumbnail(dataset);
        assertNull(result);
    }

    @Test
    public void testGetThumbnailAutoSelected() {
        System.out.println("testGetThumbnailAutoSelected");
        Dataset dataset = new Dataset();
        dataset.setId(42l);

        DataFile datafile1NotValidThumbnailContentType = new DataFile();
        datafile1NotValidThumbnailContentType.setId(1l);
        datafile1NotValidThumbnailContentType.setStorageIdentifier("storageIdentifier1");
        datafile1NotValidThumbnailContentType.setContentType("text/plain");
        FileMetadata fileMetadata1 = new FileMetadata();
        fileMetadata1.setId(1l);
        fileMetadata1.setDataFile(datafile1NotValidThumbnailContentType);

        DataFile datafile2HasValidThumbnailContentType = new DataFile();
        datafile2HasValidThumbnailContentType.setId(2l);
        datafile2HasValidThumbnailContentType.setStorageIdentifier("storageIdentifier2");
        datafile2HasValidThumbnailContentType.setContentType("image/png");
        FileMetadata fileMetadata2 = new FileMetadata();
        fileMetadata2.setId(2l);
        fileMetadata2.setDataFile(datafile2HasValidThumbnailContentType);

        DatasetVersion datasetVersion = new DatasetVersion();
        List<DatasetVersion> versions = new ArrayList<>();

        List<FileMetadata> fileMetadatas = new ArrayList<>();
        fileMetadatas.add(fileMetadata1);
        fileMetadatas.add(fileMetadata2);

        versions.add(datasetVersion);
        dataset.setVersions(versions);
        datasetVersion.setFileMetadatas(fileMetadatas);
        DatasetThumbnail result = DatasetUtil.getThumbnail(dataset);
        // FIXME: get this working.
//        assertEquals(datafile2HasValidThumbnailContentType.getId(), result.getDataFile().getId());
    }

    /**
     * Test of deleteDatasetLogo method, of class DatasetUtil.
     */
    @Test
    public void testDeleteDatasetLogo() {
        System.out.println("deleteDatasetLogo");
        Dataset dataset = null;
        boolean expResult = false;
        boolean result = DatasetUtil.deleteDatasetLogo(dataset);
        assertEquals(expResult, result);
    }

    /**
     * Test of getDefaultThumbnailFile method, of class DatasetUtil.
     */
    @Test
    public void testGetDefaultThumbnailFile() {
        System.out.println("getDefaultThumbnailFile");
        Dataset dataset = null;
        DataFile expResult = null;
        DataFile result = DatasetUtil.attemptToAutomaticallySelectThumbnailFromDataFiles(dataset);
        assertEquals(expResult, result);
    }

    /**
     * Test of persistDatasetLogoToDiskAndCreateThumbnail method, of class
     * DatasetUtil.
     */
    @Test
    public void testPersistDatasetLogoToDiskAndCreateThumbnail() {
        System.out.println("persistDatasetLogoToDiskAndCreateThumbnail");
        Dataset dataset = null;
        InputStream inputStream = null;
        Dataset expResult = null;
        Dataset result = DatasetUtil.persistDatasetLogoToDiskAndCreateThumbnail(dataset, inputStream);
        assertEquals(expResult, result);
    }

    /**
     * Test of getThumbnailAsInputStream method, of class DatasetUtil.
     */
    @Test
    public void testGetThumbnailAsInputStream() {
        System.out.println("getThumbnailAsInputStream");
        Dataset dataset = null;
        InputStream expResult = null;
        InputStream result = DatasetUtil.getThumbnailAsInputStream(dataset);
        assertEquals(expResult, result);
    }

    /**
     * Test of isDatasetLogoPresent method, of class DatasetUtil.
     */
    @Test
    public void testIsDatasetLogoPresent() {
        System.out.println("isDatasetLogoPresent");
        Dataset dataset = null;
        boolean expResult = false;
        boolean result = DatasetUtil.isDatasetLogoPresent(dataset);
        assertEquals(expResult, result);
    }

}
