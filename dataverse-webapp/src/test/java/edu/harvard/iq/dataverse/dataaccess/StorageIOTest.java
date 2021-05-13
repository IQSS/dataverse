package edu.harvard.iq.dataverse.dataaccess;

import com.google.common.io.Resources;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.DataVariable;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import org.junit.Before;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @author oscardssmith
 */
public class StorageIOTest {
    StorageIO<Dataset> instance;


    @Before
    public void setUpClass() throws IOException {
        Dataset dataset = new Dataset();
        dataset.setStorageIdentifier("storageId");
        instance = new FileAccessIO<>(dataset, "/tmp/files/tmp/dataset/Dataset");
    }

    @Test
    public void constructor_not_supported_dv_object() {
        // when & then
        assertThatThrownBy(() -> new FileAccessIO<>(new Dataverse(), "/tmp"))
            .isInstanceOf(IllegalStateException.class);
    }

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
