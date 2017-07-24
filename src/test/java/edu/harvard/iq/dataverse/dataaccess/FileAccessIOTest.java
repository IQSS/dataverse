/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.dataaccess;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.mocks.MocksFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channel;
import java.nio.file.Path;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author oscardssmith
 */
public class FileAccessIOTest {

    private FileAccessIO<Dataset> datasetAccess;
    private FileAccessIO<DataFile> dataFileAccess;
    private FileAccessIO<Dataverse> dataverseAccess;
    private Dataverse dataverse;
    private Dataset dataset;
    private DataFile dataFile;

    private Path fileSystemPath = new File("/tmp/files/tmp/dataset/Dataset").toPath();

    public FileAccessIOTest() {
    }

    @Before
    public void setUpClass() {
        dataverse = MocksFactory.makeDataverse();
        dataset = MocksFactory.makeDataset();
        dataset.setOwner(dataverse);
        dataset.setAuthority("tmp");
        dataset.setIdentifier("dataset");
        dataset.setStorageIdentifier("dataSet");

        dataFile = MocksFactory.makeDataFile();
        dataFile.setOwner(dataset);
        dataFile.setStorageIdentifier("dataFile");

        datasetAccess = new FileAccessIO<>(dataset);
        dataFileAccess = new FileAccessIO<>(dataFile);
        dataverseAccess = new FileAccessIO<>(dataverse);
    }

    /**
     * Test of canRead method, of class FileAccessIO.
     */
    @Test
    public void testCanRead() {
        boolean expResult = false;
        boolean result = datasetAccess.canRead();
        assertEquals(expResult, result);
    }

    /**
     * Test of canWrite method, of class FileAccessIO.
     */
    @Test
    public void testCanWrite() {
        boolean expResult = false;
        boolean result = datasetAccess.canWrite();
        assertEquals(expResult, result);
    }

    /**
     * Test of open method, of class FileAccessIO.
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testOpen() throws IOException {
        DataAccessOption options = DataAccessOption.READ_ACCESS;
        datasetAccess.open(options);
    }

    /**
     * Test of savePath method, of class FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testSavePath() throws IOException {
        datasetAccess.savePath(fileSystemPath);
    }

    /**
     * Test of saveInputStream method, of class FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testSaveInputStream() throws IOException {
        InputStream inputStream = null;
        datasetAccess.saveInputStream(inputStream);
    }

    /**
     * Test of openAuxChannel method, of class FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testOpenAuxChannel() throws IOException {
        DataAccessOption options = DataAccessOption.READ_ACCESS;
        Channel expResult = null;
        Channel result = datasetAccess.openAuxChannel("Dataset", options);
        assertEquals(expResult, result);
    }

    /**
     * Test of isAuxObjectCached method, of class FileAccessIO.
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testIsAuxObjectCached() throws IOException {
        boolean expResult = false;
        boolean result = datasetAccess.isAuxObjectCached("Dataset");
        assertEquals(expResult, result);
    }

    /**
     * Test of getAuxObjectSize method, of class FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testGetAuxObjectSize() throws IOException {
        long expResult = 0L;
        long result;
        result = datasetAccess.getAuxObjectSize("Dataset");
        assertEquals(expResult, result);
        
    }

    /**
     * Test of getAuxObjectAsPath method, of class FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testGetAuxObjectAsPath() throws IOException {
        Path result = datasetAccess.getAuxObjectAsPath("Dataset");
        assertEquals(fileSystemPath, result);
    }

    /**
     * Test of backupAsAux method, of class FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testBackupAsAux() throws IOException {
        datasetAccess.backupAsAux("Dataset");
    }

    /**
     * Test of savePathAsAux method, of class FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testSavePathAsAux() throws IOException {
        datasetAccess.savePathAsAux(fileSystemPath, "Dataset");
    }

    /**
     * Test of saveInputStreamAsAux method, of class FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testSaveInputStreamAsAux() throws IOException {
        InputStream inputStream = null;
        datasetAccess.saveInputStreamAsAux(inputStream, "Dataset");
    }

    /**
     * Test of listAuxObjects method, of class FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testListAuxObjects() throws IOException {
        List<String> expResult = null;
        List<String> result;
        result = dataFileAccess.listAuxObjects();
        assertEquals(expResult, result);
    }

    /**
     * Test of deleteAuxObject method, of class FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testDeleteAuxObject() throws IOException {
        datasetAccess.deleteAuxObject("Dataset");
    }

    /**
     * Test of deleteAllAuxObjects method, of class FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testDeleteAllAuxObjects() throws IOException {
        dataFileAccess.deleteAllAuxObjects();
    }

    /**
     * Test of getStorageLocation method, of class FileAccessIO.
     *
     */
    @Test
    public void testGetStorageLocation() {
        String expResult = "file:///tmp/files/tmp/dataset/dataSet";
        String result = datasetAccess.getStorageLocation();
        assertEquals(expResult, result);
    }

    /**
     * Test of getFileSystemPath method, of class FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testGetFileSystemPath() throws IOException {
        Path result = datasetAccess.getFileSystemPath();
        assertEquals(fileSystemPath, result);
    }

    /**
     * Test of exists method, of class FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testExists() throws IOException {
        boolean expResult = false;
        boolean result = datasetAccess.exists();
        assertEquals(expResult, result);
    }

    /**
     * Test of delete method, of class FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testDelete() throws IOException {
        datasetAccess.delete();
    }

    /**
     * Test of openLocalFileAsInputStream method, of class FileAccessIO.
     *
     */
    @Test
    public void testOpenLocalFileAsInputStream() {
        FileInputStream expResult = null;
        FileInputStream result = datasetAccess.openLocalFileAsInputStream();
        assertEquals(expResult, result);
    }

    /**
     * Test of openLocalFileAsOutputStream method, of class FileAccessIO.
     *
     */
    @Test
    public void testOpenLocalFileAsOutputStream() {
        FileOutputStream expResult = null;
    }

    /**
     * Test of getAuxFileAsInputStream method, of class FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testGetAuxFileAsInputStream() throws IOException {
        InputStream expResult = null;
        InputStream result = datasetAccess.getAuxFileAsInputStream("Dataset");
        assertEquals(expResult, result);
    }
}
