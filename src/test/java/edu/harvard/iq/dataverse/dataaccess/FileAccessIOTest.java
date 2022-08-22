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
import edu.harvard.iq.dataverse.util.FileUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.Before;

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

    private String dummyDriverId = "dummmy";
    
    private Path fileSystemPath = new File("/tmp/files/tmp/dataset/Dataset").toPath();

    public FileAccessIOTest() {
    }

    @Before
    public void setUpClass() throws IOException {
        dataverse = MocksFactory.makeDataverse();
        dataset = MocksFactory.makeDataset();
        dataset.setOwner(dataverse);
        dataset.setAuthority("tmp");
        dataset.setIdentifier("dataset");
        dataset.setStorageIdentifier("Dataset");

        dataFile = MocksFactory.makeDataFile();
        dataFile.setOwner(dataset);
        dataFile.setStorageIdentifier("DataFile");
        
        datasetAccess = new FileAccessIO<>(dataset,null, dummyDriverId);
        dataFileAccess = new FileAccessIO<>(dataFile, null, dummyDriverId);
        dataverseAccess = new FileAccessIO<>(dataverse, null, dummyDriverId);

        File file = new File("/tmp/files/tmp/dataset/Dataset");
        file.getParentFile().mkdirs();
        file.createNewFile();
        new File("/tmp/files/tmp/dataset/DataFile").createNewFile();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write("This is a test string");
        }
    }

    @After
    public void tearDownClass() throws IOException {
        FileUtils.deleteDirectory(new File("/tmp/files/"));
    }

    /**
     * Test of canRead, canWrite, and open method, of class FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testOpen() throws IOException {
        assertEquals(false, datasetAccess.canRead());
        assertEquals(false, datasetAccess.canWrite());

        datasetAccess.open(DataAccessOption.READ_ACCESS);
        assertEquals(true, datasetAccess.canRead());
        assertEquals(false, datasetAccess.canWrite());

        datasetAccess.open(DataAccessOption.WRITE_ACCESS);
        assertEquals(false, datasetAccess.canRead());
        assertEquals(true, datasetAccess.canWrite());

        dataFileAccess.open(DataAccessOption.READ_ACCESS);
        assertEquals(true, dataFileAccess.canRead());
        assertEquals(false, dataFileAccess.canWrite());
    }

    /**
     * Test of savePath method, of class FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testSavePath() throws IOException {
        datasetAccess.savePath(fileSystemPath);
        InputStream inputStream = new ByteArrayInputStream("Hello".getBytes());
        datasetAccess.saveInputStream(inputStream);
    }

    /**
     * Test of openAuxChannel method, of class FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testOpenAuxChannel() throws IOException {
        assertNotNull(datasetAccess.openAuxChannel("Dataset", DataAccessOption.READ_ACCESS));
    }

    /**
     * Test of isAuxObjectCached method, of class FileAccessIO.
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testIsAuxObjectCached() throws IOException {
        assertEquals(true, datasetAccess.isAuxObjectCached("Dataset"));
    }

    /**
     * Test of getAuxObjectSize method, of class FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testGetAuxObjectSize() throws IOException {
        assertEquals(21, datasetAccess.getAuxObjectSize("Dataset"));
        
    }

    /**
     * Test of getAuxObjectAsPath method, of class FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testGetAuxObjectAsPath() throws IOException {
        assertEquals(fileSystemPath, datasetAccess.getAuxObjectAsPath("Dataset"));
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
        InputStream inputStream = new ByteArrayInputStream("Hello".getBytes());;
        datasetAccess.saveInputStreamAsAux(inputStream, "Dataset");
    }

    /**
     * Test of listAuxObjects method, of class FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testListAuxObjects() throws IOException {
        List<String> result = dataFileAccess.listAuxObjects();
        assertEquals(new ArrayList<>(), result);
    }

    /**
     * Test of delete and deleteAllAuxObject method, of class FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testDeleteAuxObject() throws IOException {
        datasetAccess.deleteAuxObject("Dataset");
        dataFileAccess.deleteAllAuxObjects();
    }

    /**
     * Test of getStorageLocation method, of class FileAccessIO.
     *
     */
    @Test
    public void testGetStorageLocation() {
        String expResult = dummyDriverId + ":///tmp/files/tmp/dataset/Dataset";
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
        boolean expResult = true;
        boolean result = datasetAccess.exists();
        assertEquals(expResult, result);
    }

    /**
     * Test of delete method, of class FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    /* dataAccess no longer supports deleting of the main physical file.
    @Test
    public void testDelete() throws IOException {
        datasetAccess.delete();
    }*/

    /**
     * Test of openLocalFileAsInputStream and openLocalFileAsOutputStream method, of class
     * FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testOpenLocalFileAsInputStream() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(datasetAccess.openLocalFileAsInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append('\n');
        }
        assertEquals("This is a test string\n", sb.toString());
        assertNotNull(datasetAccess.openLocalFileAsOutputStream());
    }

    /**
     * Test of getAuxFileAsInputStream method, of class FileAccessIO.
     *
     * @throws java.io.IOException if test is broken
     */
    @Test
    public void testGetAuxFileAsInputStream() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(datasetAccess.getAuxFileAsInputStream("Dataset")));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append('\n');
        }
        assertEquals("This is a test string\n", sb.toString());
    }
    
    @Test
    public void testFileIdentifierFormats() throws IOException {
        System.setProperty("dataverse.files.filetest.type", "file");
        System.setProperty("dataverse.files.filetest.label", "Ftest");
        System.setProperty("dataverse.files.filetest.directory", "/tmp/mydata");

        FileUtil.generateStorageIdentifier();
        assertTrue(DataAccess.isValidDirectStorageIdentifier("filetest://" + FileUtil.generateStorageIdentifier()));
        //The tests here don't use a valid identifier string
        assertFalse(DataAccess.isValidDirectStorageIdentifier(dataFile.getStorageIdentifier()));
        //bad store id
        String defaultType = System.getProperty("dataverse.files.file.type");
        //Assure file isn't a defined store before test and reset afterwards if it was
        System.clearProperty("dataverse.files.file.type");
        assertFalse(DataAccess.isValidDirectStorageIdentifier("file://" + FileUtil.generateStorageIdentifier()));
        if(defaultType!=null) {
            System.out.println("dataverse.files.file.type reset to " + defaultType);
            System.setProperty("dataverse.files.file.type", defaultType);
        }
        //breakout
        assertFalse(DataAccess.isValidDirectStorageIdentifier("filetest://../" + FileUtil.generateStorageIdentifier()));
        
        System.clearProperty("dataverse.files.filetest.type");
        System.clearProperty("dataverse.files.filetest.label");
        System.clearProperty("dataverse.files.filetest.directory");
    }
}
