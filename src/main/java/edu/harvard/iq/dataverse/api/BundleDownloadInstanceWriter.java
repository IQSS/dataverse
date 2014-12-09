/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.api;

import java.lang.reflect.Type;
import java.lang.annotation.Annotation;
import javax.ejb.Singleton;
import java.io.InputStream; 
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.ws.rs.WebApplicationException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.dataaccess.*;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author Leonid Andreev
 */
@Singleton
@Provider
public class BundleDownloadInstanceWriter implements MessageBodyWriter<BundleDownloadInstance> {
    
    private static final Logger logger = Logger.getLogger(BundleDownloadInstanceWriter.class.getCanonicalName());

    
    @Override
    public boolean isWriteable(Class<?> clazz, Type type, Annotation[] annotation, MediaType mediaType) {
        return clazz == BundleDownloadInstance.class;
    }

    @Override
    public long getSize(BundleDownloadInstance di, Class<?> clazz, Type type, Annotation[] annotation, MediaType mediaType) {
        return -1;
    }

    
    
    @Override
    public void writeTo(BundleDownloadInstance di, Class<?> clazz, Type type, Annotation[] annotation, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream outstream) throws IOException, WebApplicationException {

        try {
            if (di.getDownloadInfo() != null && di.getDownloadInfo().getDataFile() != null) {
                DataAccessRequest daReq = new DataAccessRequest();
                DataFile sf = di.getDownloadInfo().getDataFile();
                DataAccessObject accessObject = DataAccess.createDataAccessObject(sf, daReq);

                if (accessObject != null) {
                    accessObject.open();

                    ZipOutputStream zout = new ZipOutputStream(outstream);

                    /* First, the tab file itself: */
                    String fileName = accessObject.getFileName();
                    String zipFileName = fileName.replaceAll("\\.tab$", "-bundle.zip");

                    httpHeaders.add("Content-disposition", "attachment; filename=\"" + zipFileName + "\"");
                    httpHeaders.add("Content-Type", "application/zip; name=\"" + zipFileName + "\"");

                    InputStream instream = accessObject.getInputStream();

                    ZipEntry e = new ZipEntry(fileName);
                    zout.putNextEntry(e);

                    String varHeaderLine = accessObject.getVarHeader();
                    if (varHeaderLine != null) {
                        zout.write(varHeaderLine.getBytes());
                    }

                    byte[] data = new byte[8192];

                    int i = 0;
                    while ((i = instream.read(data)) > 0) {
                        zout.write(data, 0, i);
                        zout.flush();
                    }
                    instream.close();
                    zout.closeEntry();

                // Now, the original format: 
                    try {
                        accessObject = StoredOriginalFile.retrieve(sf, (FileAccessObject) accessObject);
                        if (accessObject != null) {
                            instream = accessObject.getInputStream();
                        }
                        String origFileName = accessObject.getFileName();
                        e = new ZipEntry(origFileName);
                        zout.putNextEntry(e);

                        i = 0;
                        while ((i = instream.read(data)) > 0) {
                            zout.write(data, 0, i);
                            zout.flush();
                        }
                        instream.close();
                        zout.closeEntry();
                    } catch (IOException ioex) {
                    // ignore; if for whatever reason the original is not
                        // available, we'll just skip it. 
                    }
                    
                // And the variable metadata (DDI/XML), if available: 
                    if (di.getFileDDIXML() != null) {
                        e = new ZipEntry(fileName.replaceAll("\\.tab$", "-ddi.xml"));

                        zout.putNextEntry(e);
                        zout.write(di.getFileDDIXML().getBytes());
                        zout.closeEntry();
                    }

                // And now the citations: 
                    if (di.getFileCitationEndNote() != null) {
                        e = new ZipEntry(fileName.replaceAll("\\.tab$","citation-endnote.xml"));

                        zout.putNextEntry(e);
                        zout.write(di.getFileCitationEndNote().getBytes());
                        zout.closeEntry();

                    }

                    if (di.getFileCitationRIS() != null) {
                        e = new ZipEntry(fileName.replaceAll("\\.tab$","citation-ris.txt"));

                        zout.putNextEntry(e);
                        zout.write(di.getFileCitationRIS().getBytes());
                        zout.closeEntry();
                    }

                    zout.close();
                    return;
                }
            }
        } catch (IOException ioex) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }

        throw new WebApplicationException(Response.Status.NOT_FOUND);

    }

}
