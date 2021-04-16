/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.dataaccess;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
//import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channel;
import java.util.Arrays;
import java.util.List;
//import org.apache.commons.httpclient.Header;
//import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author oscardssmith
 */
public class StorageIOTest {
    StorageIO<Dataset> instance = new FileAccessIO<>();

    @Test
    public void testGetChannel() throws IOException {
        assertEquals(null, instance.getChannel());
        Channel c = new RandomAccessFile("src/main/java/propertyFiles/Bundle.properties", "r").getChannel();
        instance.setChannel(c);
        assertEquals(c, instance.getChannel());
    }

    @Test
    public void testGetWriteChannel() throws Exception {
        try {
            instance.getWriteChannel();
            fail("Should not have write access");
        } catch (IOException ex) {
            assertEquals("No NIO write access in this DataAccessObject.", ex.getMessage());
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
        Dataset d= new Dataset();
        instance.setDvObject(d);
        //assertSame uses == rather than the .equals() method which would (currently) be true for any two Datasets 
        assertSame(d, instance.getDataset());        try {
            instance.getDataFile();
            fail("This should have thrown");
        } catch (ClassCastException ex) {
            //Test succeeds
        }
        try {
            instance.getDataverse();
            fail("This should have thrown");
        } catch (ClassCastException ex) {
            //Test succeeds
        }
        // null driver defaults to 'file'
        DataFile f= new DataFile();
        Dataverse dv = new Dataverse();
        assertSame(f, new FileAccessIO<>(f, null, null).getDataFile());
        assertSame(dv, new FileAccessIO<>(dv, null, null).getDataverse());
    }

    @Test
    public void testRequest() {
        assertNotNull(instance.getRequest());
        DataAccessRequest req = new DataAccessRequest();
        instance.setRequest(req);
        assertEquals(req, instance.getRequest());
    }

    /*@Test
    public void testStatus() {
        assertEquals(0, instance.getStatus());
        instance.setStatus(1);
        assertEquals(1, instance.getStatus());
    }*/

    @Test
    public void testSize() {
        assertEquals(0, instance.getSize());
        instance.setSize(1);
        assertEquals(1, instance.getSize());
    }

    @Test
    public void testInputStream() throws IOException {
        assertEquals(null, instance.getInputStream());
        InputStream is = new ByteArrayInputStream("Test".getBytes());
        instance.setInputStream(is);
        assertEquals(is, instance.getInputStream());
        instance.closeInputStream();
        assertEquals(null, instance.getErrorMessage());
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
    public void testErrorMessage() {
        assertEquals(null, instance.getErrorMessage());
        instance.setErrorMessage("Test");
        assertEquals("Test", instance.getErrorMessage());
    }

    @Test
    public void testRemoteUrl() {
        assertEquals(null, instance.getRemoteUrl());
        instance.setRemoteUrl("Test");
        assertEquals("Test", instance.getRemoteUrl());
    }

    @Test
    public void testSwiftContainerName() {
        assertEquals(null, instance.getSwiftContainerName());
        instance.setSwiftContainerName("Swift");
        assertEquals("Swift", instance.getSwiftContainerName());
    }

    /*
    @Test
    public void testHTTPMethod() {
        assertEquals(null, instance.getHTTPMethod());
        GetMethod method = new GetMethod();
        instance.setHTTPMethod(method);
        assertEquals(method, instance.getHTTPMethod());
        instance.releaseConnection();
    }

    @Test
    public void testResponseHeaders() {
        assertArrayEquals(null, instance.getResponseHeaders());
        Header[] headers = new Header[]{new Header("Test", ""), new Header()};
        instance.setResponseHeaders(headers);
        assertArrayEquals(headers, instance.getResponseHeaders());
    }

    @Test
    public void testFileLocation() {
        assertEquals(true, instance.isLocalFile());
        instance.setIsLocalFile(false);
        assertEquals(false, instance.isLocalFile());

        assertEquals(false, instance.isRemoteAccess());
        instance.setIsRemoteAccess(true);
        assertEquals(true, instance.isRemoteAccess());
    }

    @Test
    public void testHttpAccess() {
        assertEquals(false, instance.isHttpAccess());
        instance.setIsHttpAccess(true);
        assertEquals(true, instance.isHttpAccess());
    }*/

    @Test
    public void testDownloadSupported() {
        assertEquals(true, instance.isDownloadSupported());
        instance.setIsDownloadSupported(false);
        assertEquals(false, instance.isDownloadSupported());
    }

    @Test
    public void testSubsetSupported() {
        assertEquals(false, instance.isSubsetSupported());
        instance.setIsSubsetSupported(true);
        assertEquals(true, instance.isSubsetSupported());
    }

    @Test
    public void testZippedStream() {
        assertEquals(false, instance.isZippedStream());
        instance.setIsZippedStream(true);
        assertEquals(true, instance.isZippedStream());
    }

    @Test
    public void testNoVarHeader() {
        assertEquals(false, instance.noVarHeader());
        instance.setNoVarHeader(true);
        assertEquals(true, instance.noVarHeader());
    }

    @Test
    public void testGenerateVariableHeader() {
        DataVariable var = new DataVariable(0,null);
        var.setName("Random");

        @SuppressWarnings("unchecked")
        List<DataVariable> dvs = Arrays.asList(new DataVariable[]{var, var});
        assertEquals("Random	Random\n", instance.generateVariableHeader(dvs));
        assertEquals(null, instance.generateVariableHeader(null));
    }
}
