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
import edu.harvard.iq.dataverse.datavariable.VariableServiceBean;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;

/**
 *
 * @author Leonid Andreev
 */
@Singleton
@Provider
public class DownloadInstanceWriter implements MessageBodyWriter<DownloadInstance> {
    @EJB
    VariableServiceBean variableService;
    //@PersistenceContext(unitName = "VDCNet-ejbPU")
    //private EntityManager em;
    
    private static final Logger logger = Logger.getLogger(DownloadInstanceWriter.class.getCanonicalName());

    
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
                        if ("".equals(di.getConversionParamValue())) {
                            accessObject = ImageThumbConverter.getImageThumb(sf, (FileAccessObject)accessObject); 
                        } else {
                            try {
                                int size = new Integer(di.getConversionParamValue()).intValue();
                                if (size > 0) {
                                    accessObject = ImageThumbConverter.getImageThumb(sf, (FileAccessObject)accessObject, size);
                                }
                            } catch (java.lang.NumberFormatException ex) {
                                accessObject = ImageThumbConverter.getImageThumb(sf, (FileAccessObject)accessObject);
                            }
                        }
                    }
                    /* the following download services are not supported just yet: 
                    else if (di.getConversionParam().equals("TermsOfUse")) {
                        accessObject = ExportTermsOfUse.export(sf.getStudy());
                    } else if (di.getConversionParam().equals("package")) {
                        if ("WithTermsOfUse".equals(di.getConversionParamValue())) {
                            accessObject = PackageWithTermsOfUse.repackage(sf, (FileAccessObject)accessObject);
                        }
                    }
                    */
                    
                    
                    if (sf.isTabularData()) {
                        if (di.getConversionParam().equals("noVarHeader")) {
                            accessObject.setNoVarHeader(Boolean.TRUE);
                            accessObject.setVarHeader(null);
                        } else if (di.getConversionParam().equals("format")) {
                            
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
                        } else if (di.getConversionParam().equals("subset")) {
                            logger.fine("processing subset request.");
                            
                            // TODO: 
                            // If there are parameters on the list that are 
                            // not valid variable ids, or if the do not belong to 
                            // the datafile referenced - I simply skip them; 
                            // perhaps I should throw an invalid argument exception 
                            // instead. 
                            // -- L.A. 4.0 beta 9
                            
                            if (di.getExtraArguments() != null && di.getExtraArguments().size() > 0) {
                                logger.fine("processing extra arguments list of length "+di.getExtraArguments().size());
                                List <DataVariable> variableList = new ArrayList<>();
                                String subsetVariableHeader = null;
                                for (int i = 0; i < di.getExtraArguments().size(); i++) {
                                    DataVariable variable = (DataVariable)di.getExtraArguments().get(i);
                                    if (variable != null) {
                                        if (variable.getDataTable().getDataFile().getId().equals(sf.getId())) {
                                            logger.fine("adding variable id "+variable.getId()+" to the list.");
                                            variableList.add(variable);
                                            if (subsetVariableHeader == null) {
                                                subsetVariableHeader = variable.getName();
                                            } else {
                                                subsetVariableHeader = subsetVariableHeader.concat("\t");
                                                subsetVariableHeader = subsetVariableHeader.concat(variable.getName());
                                            }
                                        } else {
                                            logger.warning("variable does not belong to this data file.");
                                        }
                                    }  
                                }
                                if (variableList.size() > 0) {
                                    TabularSubsetInputStream subsetInputStream = null; 
                                
                                    try {
                                        subsetInputStream = new TabularSubsetInputStream(sf, variableList);
                                    } catch (IOException ioe) {
                                        subsetInputStream = null; 
                                    }
                                
                                    if (subsetInputStream != null) {
                                        logger.fine("successfully created subset output stream.");
                                        accessObject.closeInputStream();
                                        accessObject.setInputStream(subsetInputStream);
                                        accessObject.setIsLocalFile(true);
                                        // TODO: make noVarHeader option work for subsets too!
                                        subsetVariableHeader = subsetVariableHeader.concat("\n");
                                        accessObject.setVarHeader(subsetVariableHeader);
                                    }
                                }
                            } else {
                                logger.fine("empty list of extra arguments.");
                            }
                        } 
                    }
                    
                    
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
