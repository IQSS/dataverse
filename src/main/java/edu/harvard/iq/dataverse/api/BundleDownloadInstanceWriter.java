/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.api;

import java.lang.reflect.Type;
import java.lang.annotation.Annotation;
import java.io.InputStream; 
import java.io.OutputStream;
import java.io.IOException;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.dataaccess.*;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author Leonid Andreev
 */
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
                StorageIO<DataFile> accessObject = DataAccess.getStorageIO(sf, daReq);

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
                    instream = null; 
                    
                    // Now, the original format: 
                    String origFormat = null; 
                    try {
                        StorageIO<DataFile> accessObjectOrig = StoredOriginalFile.retreive(accessObject); //.retrieve(sf, (FileAccessIO) accessObject);
                        if (accessObjectOrig != null) {
                            instream = accessObjectOrig.getInputStream();
                            if (instream != null) {
                                String origFileName = accessObjectOrig.getFileName();
                                origFormat = accessObject.getMimeType();
                                e = new ZipEntry(origFileName);
                                zout.putNextEntry(e);

                                i = 0;
                                while ((i = instream.read(data)) > 0) {
                                    zout.write(data, 0, i);
                                    zout.flush();
                                }
                            }
                        }
                    } catch (IOException ioex) {
                        // ignore; if for whatever reason the original is not
                        // available, we'll just skip it. 
                        logger.warning("failed to retrieve saved original for " + fileName);
                    } finally {
                        if (instream != null) {
                            try {instream.close();} catch (IOException ioex) {}
                            try {zout.closeEntry();} catch (IOException ioex) {}
                        }
                    }
                   
                    instream = null; 
                    
                    // And, if the original format was NOT RData, 
                    // add an RData version: 
                    if (!"application/x-rlang-transport".equals(origFormat)) {
                        try {
                            StorageIO<DataFile> accessObjectRdata = DataConverter.performFormatConversion(sf, accessObject,
                                                                                                           "RData", "application/x-rlang-transport");

                            if (accessObjectRdata != null) {
                                instream = accessObjectRdata.getInputStream();
                                if (instream != null) {
                                    String rdataFileName = accessObjectRdata.getFileName();
                                    e = new ZipEntry(rdataFileName);
                                    zout.putNextEntry(e);

                                    i = 0;
                                    while ((i = instream.read(data)) > 0) {
                                        zout.write(data, 0, i);
                                        zout.flush();
                                    }
                                }
                            }
                        } catch (IOException ioex) {
                            // ignore; if for whatever reason RData conversion is not
                            // available, we'll just skip it.
                            logger.warning("failed to convert tabular data file "+fileName+" to RData.");
                        } finally {
                            if (instream != null) {
                                try{instream.close();}catch(IOException ioex){}
                                try{zout.closeEntry();}catch(IOException ioex){}
                            }
                        }
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
                        e = new ZipEntry(fileName.replaceAll("\\.tab$","citation-ris.ris"));

                        zout.putNextEntry(e);
                        zout.write(di.getFileCitationRIS().getBytes());
                        zout.closeEntry();
                    }

                    if (di.getFileCitationBibtex() != null) {
                        e = new ZipEntry(fileName.replaceAll("\\.tab$","citation-bib.bib"));

                        zout.putNextEntry(e);
                        zout.write(di.getFileCitationBibtex().getBytes());
                        zout.closeEntry();
                    }

                    zout.close();
                    return;
                }
            }
        } catch (IOException ioex) {
            throw new InternalServerErrorException();
        }

        throw new NotFoundException();

    }

}
