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

    public FileAccessIOTest() {
    }

    @Before
    public void setUpClass() {
        dataverse = MocksFactory.makeDataverse();
        dataset = MocksFactory.makeDataset();
        dataset.setOwner(dataverse);
        dataFile = MocksFactory.makeDataFile();
        dataFile.setOwner(dataset);
        datasetAccess = new FileAccessIO<>(dataset);
        dataFileAccess = new FileAccessIO<>(dataFile);
        //dataverseAccess = new FileAccessIO<>(new Dataverse());
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
     */
    @Test
    public void testOpen() {
        DataAccessOption[] options = null;
        try {
            datasetAccess.open(options);
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Test of savePath method, of class FileAccessIO.
     */
    @Test
    public void testSavePath() {
        Path fileSystemPath = null;
        try {
            datasetAccess.savePath(fileSystemPath);
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Test of saveInputStream method, of class FileAccessIO.
     */
    @Test
    public void testSaveInputStream() {
        InputStream inputStream = null;
        try {
            datasetAccess.saveInputStream(inputStream);
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Test of openAuxChannel method, of class FileAccessIO.
     */
    @Test
    public void testOpenAuxChannel() {
        String auxItemTag = "";
        DataAccessOption[] options = null;
        Channel expResult = null;
        Channel result;
        try {
            result = datasetAccess.openAuxChannel(auxItemTag, options);
            assertEquals(expResult, result);
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Test of isAuxObjectCached method, of class FileAccessIO.
     */
    @Test
    public void testIsAuxObjectCached() {
        String auxItemTag = "";
        boolean expResult = false;
        boolean result;
        try {
            result = datasetAccess.isAuxObjectCached(auxItemTag);
            assertEquals(expResult, result);
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Test of getAuxObjectSize method, of class FileAccessIO.
     */
    @Test
    public void testGetAuxObjectSize() {
        String auxItemTag = "";
        long expResult = 0L;
        long result;
        try {
            result = datasetAccess.getAuxObjectSize(auxItemTag);
            assertEquals(expResult, result);
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Test of getAuxObjectAsPath method, of class FileAccessIO.
     */
    @Test
    public void testGetAuxObjectAsPath() {
        String auxItemTag = "";
        Path expResult = null;
        Path result;
        try {
            result = datasetAccess.getAuxObjectAsPath(auxItemTag);
            assertEquals(expResult, result);
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Test of backupAsAux method, of class FileAccessIO.
     */
    @Test
    public void testBackupAsAux() {
        String auxItemTag = "";
        try {
            datasetAccess.backupAsAux(auxItemTag);
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Test of savePathAsAux method, of class FileAccessIO.
     */
    @Test
    public void testSavePathAsAux() {
        Path fileSystemPath = null;
        String auxItemTag = "";
        try {
            datasetAccess.savePathAsAux(fileSystemPath, auxItemTag);
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Test of saveInputStreamAsAux method, of class FileAccessIO.
     */
    @Test
    public void testSaveInputStreamAsAux() {
        InputStream inputStream = null;
        String auxItemTag = "";
        try {
            datasetAccess.saveInputStreamAsAux(inputStream, auxItemTag);
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Test of listAuxObjects method, of class FileAccessIO.
     */
    @Test
    public void testListAuxObjects() {
        List<String> expResult = null;
        List<String> result;
        try {
            result = dataFileAccess.listAuxObjects();
            assertEquals(expResult, result);
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Test of deleteAuxObject method, of class FileAccessIO.
     */
    @Test
    public void testDeleteAuxObject() {
        String auxItemTag = "";
        try {
            datasetAccess.deleteAuxObject(auxItemTag);
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Test of deleteAllAuxObjects method, of class FileAccessIO.
     */
    @Test
    public void testDeleteAllAuxObjects() {
        try {
            datasetAccess.deleteAllAuxObjects();
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Test of getStorageLocation method, of class FileAccessIO.
     */
    @Test
    public void testGetStorageLocation() {
        String expResult = null;
        String result = datasetAccess.getStorageLocation();
        assertEquals(expResult, result);
    }

    /**
     * Test of getFileSystemPath method, of class FileAccessIO.
     */
    @Test
    public void testGetFileSystemPath() {
        try {
            Path expResult = null;
            Path result = datasetAccess.getFileSystemPath();
            assertEquals(expResult, result);
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Test of exists method, of class FileAccessIO.
     */
    @Test
    public void testExists() {
        try {
            boolean expResult = false;
            boolean result = datasetAccess.exists();
            assertEquals(expResult, result);
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Test of delete method, of class FileAccessIO.
     */
    @Test
    public void testDelete() {
        try {
            datasetAccess.delete();
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Test of openLocalFileAsInputStream method, of class FileAccessIO.
     */
    @Test
    public void testOpenLocalFileAsInputStream() {
        FileInputStream expResult = null;
        FileInputStream result = datasetAccess.openLocalFileAsInputStream();
        assertEquals(expResult, result);
    }

    /**
     * Test of openLocalFileAsOutputStream method, of class FileAccessIO.
     */
    @Test
    public void testOpenLocalFileAsOutputStream() {
        FileOutputStream expResult = null;
    }

    /**
     * Test of getAuxFileAsInputStream method, of class FileAccessIO.
     */
    @Test
    public void testGetAuxFileAsInputStream() {
        InputStream result = null;
        try {
            String auxItemTag = "";
            InputStream expResult = null;
            result = datasetAccess.getAuxFileAsInputStream(auxItemTag);
            assertEquals(expResult, result);
        } catch (IOException ex) {
            fail(ex.getMessage());
        } finally {
            try {
                result.close();
            } catch (IOException ex) {
                fail(ex.getMessage());
            }
        }
    }
}
