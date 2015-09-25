package edu.harvard.iq.dataverse;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test that the DataFileServiceBean classifies DataFiles correctly.
 * @author bencomp
 */
public class DataFileServiceBeanTest {
    
    public DataFileServiceBeanTest() {
    }
    
    /**
     * A DataFile without content type.
     */
    private DataFile fileWoContentType = null;
    /**
     * A DataFile with bogus content type "foo/bar".
     */
    private DataFile fileWithBogusContentType = null;
    /**
     * The Bean Under Test.
     */
    private DataFileServiceBean dataFileServiceBean;
            
    
    @Before
    public void setUp() {
        fileWoContentType = new DataFile();
        fileWithBogusContentType = new DataFile("foo/bar");
        dataFileServiceBean = new DataFileServiceBean();
    }

    /**
     * Expect that {@code null}, a DataFile without content type and a DataFile
     * with bogus content type are not astro files.
     * @throws Exception when the test is in error.
     */
    @Test
    public void testIsFileClassAstro() throws Exception {
        assertFalse(dataFileServiceBean.isFileClassAstro(null));
        assertFalse(dataFileServiceBean.isFileClassAstro(fileWoContentType));
        assertFalse(dataFileServiceBean.isFileClassAstro(fileWithBogusContentType));
    }

    /**
     * Expect that {@code null}, a DataFile without content type and a DataFile
     * with bogus content type are not audio files.
     * @throws Exception when the test is in error.
     */
    @Test
    public void testIsFileClassAudio() throws Exception {
        assertFalse(dataFileServiceBean.isFileClassAudio(null));
        assertFalse(dataFileServiceBean.isFileClassAudio(fileWoContentType));
        assertFalse(dataFileServiceBean.isFileClassAudio(fileWithBogusContentType));
    }

    /**
     * Expect that {@code null}, a DataFile without content type and a DataFile
     * with bogus content type are not code files.
     * @throws Exception when the test is in error.
     */
    @Test
    public void testIsFileClassCode() throws Exception {
        assertFalse(dataFileServiceBean.isFileClassCode(null));
        assertFalse(dataFileServiceBean.isFileClassCode(fileWoContentType));
        assertFalse(dataFileServiceBean.isFileClassCode(fileWithBogusContentType));
    }

    /**
     * Expect that {@code null}, a DataFile without content type and a DataFile
     * with bogus content type are not document files.
     * @throws Exception when the test is in error.
     */
    @Test
    public void testIsFileClassDocument() throws Exception {
        assertFalse(dataFileServiceBean.isFileClassDocument(null));
        assertFalse(dataFileServiceBean.isFileClassDocument(fileWoContentType));
        assertFalse(dataFileServiceBean.isFileClassDocument(fileWithBogusContentType));
    }

    /**
     * Expect that {@code null}, a DataFile without content type and a DataFile
     * with bogus content type are not geo files.
     * @throws Exception when the test is in error.
     */
    @Test
    public void testIsFileClassGeo() throws Exception {
        assertFalse(dataFileServiceBean.isFileClassGeo(null));
        assertFalse(dataFileServiceBean.isFileClassGeo(fileWoContentType));
        assertFalse(dataFileServiceBean.isFileClassGeo(fileWithBogusContentType));
    }

    /**
     * Expect that {@code null}, a DataFile without content type and a DataFile
     * with bogus content type are not image files.
     * @throws Exception when the test is in error.
     */
    @Test
    public void testIsFileClassImage() throws Exception {
        assertFalse(dataFileServiceBean.isFileClassImage(null));
        assertFalse(dataFileServiceBean.isFileClassImage(fileWoContentType));
        assertFalse(dataFileServiceBean.isFileClassImage(fileWithBogusContentType));
    }

    /**
     * Expect that {@code null}, a DataFile without content type and a DataFile
     * with bogus content type are not network files.
     * @throws Exception when the test is in error.
     */
    @Test
    public void testIsFileClassNetwork() throws Exception {
        assertFalse(dataFileServiceBean.isFileClassNetwork(null));
        assertFalse(dataFileServiceBean.isFileClassNetwork(fileWoContentType));
        assertFalse(dataFileServiceBean.isFileClassNetwork(fileWithBogusContentType));
    }

    /**
     * Expect that {@code null}, a DataFile without content type and a DataFile
     * with bogus content type are not tabular files.
     * @throws Exception when the test is in error.
     */
    @Test
    public void testIsFileClassTabularData() throws Exception {
        assertFalse(dataFileServiceBean.isFileClassTabularData(null));
        assertFalse(dataFileServiceBean.isFileClassTabularData(fileWoContentType));
        assertFalse(dataFileServiceBean.isFileClassTabularData(fileWithBogusContentType));
    }

    /**
     * Expect that {@code null}, a DataFile without content type and a DataFile
     * with bogus content type are not video files.
     * @throws Exception when the test is in error.
     */
    @Test
    public void testIsFileClassVideo() throws Exception {
        assertFalse(dataFileServiceBean.isFileClassVideo(null));
        assertFalse(dataFileServiceBean.isFileClassVideo(fileWoContentType));
        assertFalse(dataFileServiceBean.isFileClassVideo(fileWithBogusContentType));
    }

    /**
     * Expect that {@code null}, a DataFile without content type and a DataFile
     * with bogus content type are not SPSS portable files.
     * @throws Exception when the test is in error.
     */
    @Test
    public void testIsSpssPorFile() throws Exception {
        assertFalse(dataFileServiceBean.isSpssPorFile(null));
        assertFalse(dataFileServiceBean.isSpssPorFile(fileWoContentType));
        assertFalse(dataFileServiceBean.isSpssPorFile(fileWithBogusContentType));
    }

    /**
     * Expect that {@code null}, a DataFile without content type and a DataFile
     * with bogus content type are not SPSS .sav files.
     * @throws Exception when the test is in error.
     */
    @Test
    public void testIsSpssSavFile() throws Exception {
        assertFalse(dataFileServiceBean.isSpssSavFile(null));
        assertFalse(dataFileServiceBean.isSpssSavFile(fileWoContentType));
        assertFalse(dataFileServiceBean.isSpssSavFile(fileWithBogusContentType));
    }

    /**
     * Expect that {@code null}, a DataFile without content type and a DataFile
     * with bogus content type are not files that thumbnails can be created for.
     * @throws Exception when the test is in error.
     */
    @Test
    public void testIsThumbnailSupported() throws Exception {
        assertFalse(dataFileServiceBean.thumbnailSupported(null));
        assertFalse(dataFileServiceBean.thumbnailSupported(fileWoContentType));
        assertFalse(dataFileServiceBean.thumbnailSupported(fileWithBogusContentType));
    }

    /**
     * Expect that {@code null}, a DataFile without content type and a DataFile
     * with bogus content type are not files that thumbnails can be created for.
     * @throws Exception when the test is in error.
     */
    @Test
    public void testIsThumbnailSupportedForSize() throws Exception {
        assertFalse(dataFileServiceBean.isThumbnailAvailableForSize(null));
        assertFalse(dataFileServiceBean.isThumbnailAvailableForSize(fileWoContentType));
        assertFalse(dataFileServiceBean.isThumbnailAvailableForSize(fileWithBogusContentType));
    }
    
    /**
     * Expect that files without content type or with a bogus content type are
     * classed as "other". Note that the file classes are not coded as constants!
     * @throws Exception when the test is in error.
     */
    @Test
    public void testGetFileClass() throws Exception {
        assertEquals("other", dataFileServiceBean.getFileClass(fileWoContentType));
        assertEquals("other", dataFileServiceBean.getFileClass(fileWithBogusContentType));
    }
    
}
