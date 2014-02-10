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

/**
 *
 * @author Leonid Andreev
 */
@Singleton
@Provider
public class DownloadInstanceWriter implements MessageBodyWriter<DownloadInstance> {

    @Override
    public boolean isWriteable(Class<?> clazz, Type type, Annotation[] annotation, MediaType mediaType) {
        return clazz == DownloadInstance.class;
    }

    @Override
    public long getSize(DownloadInstance di, Class<?> clazz, Type type, Annotation[] annotation, MediaType mediaType) {
        return -1;
    }

    /*
    @Override
    public void writeTo(DownloadInstance di, Class<?> clazz, Type type, Annotation[] annotation, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream outstream) throws IOException, WebApplicationException {

        ByteArrayOutputStream generatedStream = di.getOutStream();
        byte[] generatedBytes = generatedStream.toByteArray();

        outstream.write(generatedBytes, 0, generatedBytes.length);
            // in prod. we'll want to use the 
        // outstream.write(byte[], offset, lenght) version
        //
        // do i need to close outstream?
    }
    */
    
    @Override
    public void writeTo(DownloadInstance di, Class<?> clazz, Type type, Annotation[] annotation, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream outstream) throws IOException, WebApplicationException {

        if (di.getDownloadInfo() != null && di.getDownloadInfo().getDataFile() != null) {
            DataAccessRequest daReq = new DataAccessRequest();
            
            
            
            DataFile sf = di.getDownloadInfo().getDataFile();
            DataAccessObject accessObject = DataAccess.createDataAccessObject(sf, daReq);
                        
            if (accessObject != null) {
                accessObject.open();
                
                if (di.getConversionParam() != null) {
                    // Image Thumbnail conversion: 
                    
                    if (di.getConversionParam().equals("imageThumb")) {
                        accessObject = ImageThumbConverter.getImageThumb(sf, (FileAccessObject)accessObject); 
                    }
                    /* No other download services are supported just yet. 
                    else if (di.getConversionParam().equals("TermsOfUse")) {
                        accessObject = ExportTermsOfUse.export(sf.getStudy());
                    } else if (di.getConversionParam().equals("package")) {
                        if ("WithTermsOfUse".equals(di.getConversionParamValue())) {
                            accessObject = PackageWithTermsOfUse.repackage(sf, (FileAccessObject)accessObject);
                        }
                    }
                    */
                    
                    /* No special services for "Subsettable" files just yet:
                    
                    if (sf.isTabularData()) {
                        if (di.getConversionParam().equals("noVarHeader")) {
                            accessObject.setNoVarHeader(Boolean.TRUE);
                            accessObject.setVarHeader(null);
                        } else if (di.getConversionParam().equals("fileFormat")) {
                            
                            if ("original".equals(di.getConversionParamValue())) {
                                accessObject = StoredOriginalFile.retrieve(sf, (FileAccessObject)accessObject);
                            } else {
                                // Other format conversions: 
                                String requestedMimeType = di.getServiceFormatType(di.getConversionParam(), di.getConversionParamValue()); 
                                if (requestedMimeType == null) {
                                    // default mime type, in case real type is unknown;
                                    // (this shouldn't happen in real life - but just in case): 
                                    requestedMimeType = "application/octet-stream";
                                } 
                                accessObject = 
                                        DataFileConverter.performFormatConversion(
                                        sf, 
                                        (FileAccessObject)accessObject, 
                                        di.getConversionParamValue(), requestedMimeType);
                            }
                        }
                    }
                    */
                    
                    if (accessObject == null) {
                        throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
                    }
                }
                
                InputStream instream = accessObject.getInputStream();
                if (instream != null) {
                    // headers:
                    
                    String fileName = accessObject.getFileName(); 
                    String mimeType = accessObject.getMimeType(); 
                    
                    // Provide both the "Content-disposition" and "Content-Type" headers,
                    // to satisfy the widest selection of browsers out there. 
                    
                    httpHeaders.add("Content-disposition", "attachment; filename=\"" + fileName + "\"");
                    httpHeaders.add("Content-Type", mimeType + "; name=\"" + fileName);
                    
                    // (the httpHeaders map must be modified *before* writing any
                    // data in the output stream! 
                                                              
                    int bufsize;
                    byte [] bffr = new byte[4*8192];
                    
                    // before writing out any bytes from the input stream, flush
                    // any extra content, such as the variable header for the 
                    // subsettable files:
                    
                    if (accessObject.getVarHeader() != null) {
                        outstream.write(accessObject.getVarHeader().getBytes());
                    }

                    while ((bufsize = instream.read(bffr)) != -1) {
                        outstream.write(bffr, 0, bufsize);
                    }

                    instream.close();
                    return;
                }
            }
        }
        
        throw new WebApplicationException(Response.Status.NOT_FOUND);

    }

}
