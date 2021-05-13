/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.dataaccess;

import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author oscardssmith
 */
public class FileAccessIOTest {

    private FileAccessIO<Dataset> datasetAccess;
    private FileAccessIO<DataFile> dataFileAccess;
    private Dataverse dataverse;
    private Dataset dataset;
    private DataFile dataFile;

    private byte[] datasetAuxFileBytes = "This is a test string".getBytes();
    private String datasetAuxObjectName = "Dataset";
    private byte[] datafileBytes = "datafile content".getBytes();

    private byte[] dataToBeSaved = "tobesaved".getBytes();
    private File fileToBeSaved = new File("/tmp/files/fileToBeSaved");


    @Before
    public void setUpClass() throws IOException {
        dataverse = MocksFactory.makeDataverse();
        dataset = MocksFactory.makeDataset();
        dataset.setOwner(dataverse);
        dataset.setStorageIdentifier("file://10.1010/FK2/DATASET");

        dataFile = MocksFactory.makeDataFile();
        dataFile.setOwner(dataset);
        dataFile.setStorageIdentifier("DataFile");

        datasetAccess = new FileAccessIO<>(dataset, "/tmp/files");
        dataFileAccess = new FileAccessIO<>(dataFile, "/tmp/files");

        FileUtils.writeByteArrayToFile(new File("/tmp/files/10.1010/FK2/DATASET/Dataset"), datasetAuxFileBytes);
        FileUtils.writeByteArrayToFile(new File("/tmp/files/10.1010/FK2/DATASET/DataFile"), datafileBytes);
        
        FileUtils.writeByteArrayToFile(new File("/tmp/files/fileToBeSaved"), dataToBeSaved);
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

    @Test
    public void testOpenAuxChannel() throws IOException {
        assertNotNull(datasetAccess.openAuxChannel(datasetAuxObjectName, DataAccessOption.READ_ACCESS));
    }

    @Test
    public void testIsAuxObjectCached() throws IOException {
        assertThat(datasetAccess.isAuxObjectCached(datasetAuxObjectName)).isTrue();
    }

    @Test
    public void testGetAuxObjectSize() throws IOException {
        assertEquals(21, datasetAccess.getAuxObjectSize(datasetAuxObjectName));

    }

    @Test
    public void testGetAuxObjectAsPath() throws IOException {
        assertThat(datasetAccess.getAuxObjectAsPath(datasetAuxObjectName))
            .isEqualTo(new File("/tmp/files/10.1010/FK2/DATASET/" + datasetAuxObjectName).toPath());
    }

    @Test
    public void testBackupAsAux() throws IOException {
        dataFileAccess.backupAsAux("auxFileBackup");
        
        assertThat(new File("/tmp/files/10.1010/FK2/DATASET/DataFile.auxFileBackup")).hasBinaryContent(datafileBytes);
    }


    @Test
    public void testSavePathAsAux() throws IOException {
        datasetAccess.savePathAsAux(fileToBeSaved.toPath(), "Dataset");
    }


    @Test
    public void testSaveInputStreamAsAux() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("Hello".getBytes());
        datasetAccess.saveInputStreamAsAux(inputStream, "Dataset");
    }

    @Test
    public void testListAuxObjects() throws IOException {
        List<String> result = dataFileAccess.listAuxObjects();
        assertEquals(new ArrayList<>(), result);
    }

    @Test
    public void testDeleteAuxObject() throws IOException {
        datasetAccess.deleteAuxObject("Dataset");
        dataFileAccess.deleteAllAuxObjects();
    }

    @Test
    public void testGetStorageLocation() {
        assertThat(dataFileAccess.getStorageLocation()).isEqualTo("file:///tmp/files/10.1010/FK2/DATASET/DataFile");
    }

    @Test
    public void testGetFileSystemPath() throws IOException {
        assertThat(dataFileAccess.getFileSystemPath()).isEqualTo(new File("/tmp/files/10.1010/FK2/DATASET/DataFile").toPath());
    }

    @Test
    public void testExists() throws IOException {
        assertThat(dataFileAccess.exists()).isTrue();
    }

    @Test
    public void testGetAuxFileAsInputStream() throws IOException {
        assertThat(datasetAccess.getAuxFileAsInputStream("Dataset")).hasBinaryContent(datasetAuxFileBytes);
    }
}
