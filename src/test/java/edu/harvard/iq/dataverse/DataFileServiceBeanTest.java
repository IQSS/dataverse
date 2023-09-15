package edu.harvard.iq.dataverse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
            
    
    @BeforeEach
    public void setUp() {
        fileWoContentType = createDataFile(null);
        fileWithBogusContentType = createDataFile("foo/bar");
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
     (this method has been removed from datafileservicebean)
    @Test
    public void testIsThumbnailSupportedForSize() throws Exception {
        assertFalse(dataFileServiceBean.isThumbnailAvailableForSize(null));
        assertFalse(dataFileServiceBean.isThumbnailAvailableForSize(fileWoContentType));
        assertFalse(dataFileServiceBean.isThumbnailAvailableForSize(fileWithBogusContentType));
    }
    */
    
    /**
     * Expect that files without content type or with a bogus content type are
     * classed as "other". Note that the file classes are not coded as constants!
     * @throws Exception when the test is in error.
     */
    @Test
    public void testGetFileClass() throws Exception {
        assertEquals("other", dataFileServiceBean.getFileThumbnailClass(fileWoContentType));
        assertEquals("other", dataFileServiceBean.getFileThumbnailClass(fileWithBogusContentType));
    }
    
    /**
     * Create a DataFile with properties.
     * @param contentType the content media type as a string
     * @param storageIdentifier an identifier that signifies the location of the
     * file in storage. Must not be null, but may be empty.
     * @return a DataFile with the given content type and storage identifier
     *
     * @see #createDataFile(java.lang.String)
     */
    private DataFile createDataFile(String contentType, String storageIdentifier) {
        DataFile file = new DataFile(contentType);
        file.setStorageIdentifier(storageIdentifier);
        return file;
    }

    /**
     * Create a DataFile with the given content type and empty storage identifier.
     * @param contentType the content type of the DataFile (may be {@code null})
     * @return a DataFile with content type and empty storage identifier
     *
     * @see #createDataFile(java.lang.String, java.lang.String)
     */
    private DataFile createDataFile(String contentType) {
        return createDataFile(contentType, "");
    }

    @Test
    public void testfindMostRecentVersionFileIsIn() {
        assertEquals(null, dataFileServiceBean.findMostRecentVersionFileIsIn(null));
    }

}
