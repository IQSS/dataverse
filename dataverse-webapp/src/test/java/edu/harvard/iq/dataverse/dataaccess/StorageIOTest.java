package edu.harvard.iq.dataverse.dataaccess;

import com.google.common.io.Resources;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.DataVariable;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channel;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @author oscardssmith
 */
public class StorageIOTest {
    StorageIO<Dataset> instance = new FileAccessIO<>("/tmp/files/tmp/dataset/Dataset");

    @Test
    public void testGetChannel() throws IOException {
        assertNull(instance.getChannel());
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(Resources.getResource("images/banner.png").getPath(), "r")) {
            Channel c = randomAccessFile.getChannel();
            instance.setChannel(c);
            assertEquals(c, instance.getChannel());
        }
    }

    @Test
    public void testGetReadChannel() throws Exception {
        try {
            instance.getReadChannel();
            fail("Should not have read access");
        } catch (IOException ex) {
            assertEquals("No NIO read access in this DataAccessObject.", ex.getMessage());
        }
    }

    @Test
    public void testGetDvObject() {
        assertEquals(null, instance.getDvObject());
        instance.setDvObject(new Dataset());
        assertEquals(new Dataset(), instance.getDataset());

        try {
            instance.getDataFile();
            fail("This should have thrown");
        } catch (ClassCastException ex) {
            assertEquals("edu.harvard.iq.dataverse.persistence.dataset.Dataset cannot be cast to edu.harvard.iq.dataverse.persistence.datafile.DataFile", ex.getMessage());
        }
        try {
            instance.getDataverse();
            fail("This should have thrown");
        } catch (ClassCastException ex) {
            assertEquals("edu.harvard.iq.dataverse.persistence.dataset.Dataset cannot be cast to edu.harvard.iq.dataverse.persistence.dataverse.Dataverse", ex.getMessage());
        }
        assertEquals(new DataFile(), new FileAccessIO<>(new DataFile(), null).getDataFile());
        assertEquals(new Dataverse(), new FileAccessIO<>(new Dataverse(), null).getDataverse());
    }

    @Test
    public void testInputStream() throws IOException {
        assertEquals(null, instance.getInputStream());
        InputStream is = new ByteArrayInputStream("Test".getBytes());
        instance.setInputStream(is);
        assertEquals(is, instance.getInputStream());
        instance.closeInputStream();
    }

    @Test
    public void testOutputStream() throws Exception {
        assertEquals(null, instance.getOutputStream());
        OutputStream os = new ByteArrayOutputStream();
        instance.setOutputStream(os);
        assertEquals(os, instance.getOutputStream());
    }

    @Test
    public void testMimeType() {
        assertEquals(null, instance.getMimeType());
        instance.setMimeType("Test");
        assertEquals("Test", instance.getMimeType());
    }

    @Test
    public void testFileName() {
        assertEquals(null, instance.getFileName());
        instance.setFileName("Test");
        assertEquals("Test", instance.getFileName());
    }

    @Test
    public void testVarHeader() {
        assertEquals(null, instance.getVarHeader());
        instance.setVarHeader("Test");
        assertEquals("Test", instance.getVarHeader());
    }

    @Test
    public void testNoVarHeader() {
        assertEquals(false, instance.noVarHeader());
        instance.setNoVarHeader(true);
        assertEquals(true, instance.noVarHeader());
    }

    @Test
    public void testGenerateVariableHeader() {
        DataVariable var = new DataVariable(0, null);
        var.setName("Random");

        @SuppressWarnings("unchecked")
        List<DataVariable> dvs = Arrays.asList(var, var);
        assertEquals("Random	Random\n", instance.generateVariableHeader(dvs));
        assertEquals(null, instance.generateVariableHeader(null));
    }
}
