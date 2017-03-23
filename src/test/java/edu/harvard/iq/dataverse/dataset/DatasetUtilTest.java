package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
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

    /**
     * Test of getThumbnail method, of class DatasetUtil.
     */
    @Test
    public void testGetThumbnail() {
        System.out.println("getThumbnail");
        Dataset dataset = null;
        DatasetThumbnail expResult = null;
        DatasetThumbnail result = DatasetUtil.getThumbnail(dataset);
        assertEquals(expResult, result);
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
