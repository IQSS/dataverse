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

import javax.ws.rs.WebApplicationException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.dataaccess.*;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateGuestbookResponseCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Leonid Andreev
 */
@Provider
public class DownloadInstanceWriter implements MessageBodyWriter<DownloadInstance> {
    
    private static final Logger logger = Logger.getLogger(DownloadInstanceWriter.class.getCanonicalName());

    
    @Override
    public boolean isWriteable(Class<?> clazz, Type type, Annotation[] annotation, MediaType mediaType) {
        return clazz == DownloadInstance.class;
    }

    @Override
    public long getSize(DownloadInstance di, Class<?> clazz, Type type, Annotation[] annotation, MediaType mediaType) {
        return -1;
        //return getFileSize(di);
    }
    
    @Override
    public void writeTo(DownloadInstance di, Class<?> clazz, Type type, Annotation[] annotation, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream outstream) throws IOException, WebApplicationException {

        
        if (di.getDownloadInfo() != null && di.getDownloadInfo().getDataFile() != null) {
            DataAccessRequest daReq = new DataAccessRequest();
            
            
            
            DataFile sf = di.getDownloadInfo().getDataFile();
            DataFileIO accessObject = DataAccess.createDataAccessObject(sf, daReq);
                        
            if (accessObject != null) {
                accessObject.open();
                
                if (di.getConversionParam() != null) {
                    // Image Thumbnail and Tabular data conversion: 
                    // NOTE: only supported on local files, as of 4.0.2!
                    
                    if (di.getConversionParam().equals("imageThumb") && accessObject.isLocalFile()) {
                        if ("".equals(di.getConversionParamValue())) {
                            accessObject = ImageThumbConverter.getImageThumb((FileAccessIO)accessObject); 
                        } else {
                            try {
                                int size = new Integer(di.getConversionParamValue()).intValue();
                                if (size > 0) {
                                    accessObject = ImageThumbConverter.getImageThumb((FileAccessIO)accessObject, size);
                                }
                            } catch (java.lang.NumberFormatException ex) {
                                accessObject = ImageThumbConverter.getImageThumb((FileAccessIO)accessObject);
                            }
                        }
                    }
                    
                    
                    if (sf.isTabularData()) {
                        if (di.getConversionParam().equals("noVarHeader")) {
                            accessObject.setNoVarHeader(Boolean.TRUE);
                            accessObject.setVarHeader(null);
                        } else if (di.getConversionParam().equals("format")  && accessObject.isLocalFile()) {
                            
                            if ("original".equals(di.getConversionParamValue())) {
                                accessObject = StoredOriginalFile.retreive(accessObject);
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
                                        (FileAccessIO)accessObject, 
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
                                        
                                        String tabularFileName = accessObject.getFileName();
                                        
                                        if (tabularFileName != null && tabularFileName.endsWith(".tab")) {
                                            tabularFileName = tabularFileName.replaceAll("\\.tab$", "-subset.tab");
                                        } else if (tabularFileName != null && !"".equals(tabularFileName)) {
                                            tabularFileName = tabularFileName.concat("-subset.tab");
                                        } else {
                                            tabularFileName = "subset.tab";
                                        }
                                        
                                        accessObject.setFileName(tabularFileName);
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
                    httpHeaders.add("Content-Type", mimeType + "; name=\"" + fileName + "\"");
                    
                    long contentSize; 
                    boolean useChunkedTransfer = false; 
                    //if ((contentSize = getFileSize(di, accessObject.getVarHeader())) > 0) {
                    if ((contentSize = getContentSize(accessObject)) > 0) {
                        logger.fine("Content size (retrieved from the AccessObject): "+contentSize);
                        httpHeaders.add("Content-Length", contentSize); 
                    } else {
                        //httpHeaders.add("Transfer-encoding", "chunked");
                        //useChunkedTransfer = true;
                    }
                    
                    // (the httpHeaders map must be modified *before* writing any
                    // data in the output stream!)
                                                              
                    int bufsize;
                    byte [] bffr = new byte[4*8192];
                    byte [] chunkClose = "\r\n".getBytes();
                    
                    // before writing out any bytes from the input stream, flush
                    // any extra content, such as the variable header for the 
                    // subsettable files: (??)4
                    
                    if (accessObject.getVarHeader() != null) {
                        if (accessObject.getVarHeader().getBytes().length > 0) {
                            if (useChunkedTransfer) {
                                String chunkSizeLine = String.format("%x\r\n", accessObject.getVarHeader().getBytes().length);
                                outstream.write(chunkSizeLine.getBytes());
                            }
                            outstream.write(accessObject.getVarHeader().getBytes());
                            if (useChunkedTransfer) {
                                outstream.write(chunkClose);
                            }
                        }
                    }

                    while ((bufsize = instream.read(bffr)) != -1) {
                        if (useChunkedTransfer) {
                            String chunkSizeLine = String.format("%x\r\n", bufsize);
                            outstream.write(chunkSizeLine.getBytes());
                        }
                        outstream.write(bffr, 0, bufsize);
                        if (useChunkedTransfer) {
                            outstream.write(chunkClose);
                        }
                    }

                    if (useChunkedTransfer) {
                        String chunkClosing = "0\r\n\r\n";
                        outstream.write(chunkClosing.getBytes());
                    }
                    
                    
                    logger.fine("di conversion param: "+di.getConversionParam()+", value: "+di.getConversionParamValue());
                    
                    // Downloads of thumbnail images (scaled down, low-res versions of graphic image files) and 
                    // "preprocessed metadata" records for tabular data files are NOT considered "real" downloads, 
                    // so these should not produce guestbook entries: 
                    
                    if (di.getGbr() != null && !(isThumbnailDownload(di) || isPreprocessedMetadataDownload(di))) {
                        try {
                            logger.fine("writing guestbook response.");
                            Command cmd = new CreateGuestbookResponseCommand(di.getDataverseRequestService().getDataverseRequest(), di.getGbr(), di.getGbr().getDataFile().getOwner());
                            di.getCommand().submit(cmd);
                        } catch (CommandException e) {
                            //if an error occurs here then download won't happen no need for response recs...
                        }
                    } else {
                        logger.fine("not writing guestbook response");
                    } 
                        
                    
                    instream.close();
                    outstream.close(); 
                    return;
                }
            }
        }
        
        throw new WebApplicationException(Response.Status.NOT_FOUND);

    }
    
    private boolean isThumbnailDownload(DownloadInstance downloadInstance) {
        if (downloadInstance == null) return false; 
        
        if (downloadInstance.getConversionParam() == null) return false; 
        
        return downloadInstance.getConversionParam().equals("imageThumb");
    }
    
    private boolean isPreprocessedMetadataDownload(DownloadInstance downloadInstance) {
        if (downloadInstance == null) return false; 
        
        if (downloadInstance.getConversionParam() == null) return false; 
        
        if (downloadInstance.getConversionParamValue() == null) return false; 
        
        return downloadInstance.getConversionParam().equals("format") && downloadInstance.getConversionParamValue().equals("prep");
    }
    
    private long getContentSize(DataFileIO accessObject) {
        long contentSize = 0; 
        
        if (accessObject.getSize() > -1) {
            contentSize+=accessObject.getSize();
            if (accessObject.getVarHeader() != null) {
                if (accessObject.getVarHeader().getBytes().length > 0) {
                    contentSize+=accessObject.getVarHeader().getBytes().length;
                }
            }
            return contentSize;
        }
        return -1;
    }
    
    private long getFileSize(DownloadInstance di) {
        return getFileSize(di, null);
    }
    
    private long getFileSize(DownloadInstance di, String extraHeader) {
        if (di.getDownloadInfo() != null && di.getDownloadInfo().getDataFile() != null) {           
            DataFile df = di.getDownloadInfo().getDataFile();
            
            // For non-tabular files, we probably know the file size: 
            // (except for when this is a thumbNail rquest on an image file - 
            // because the size will obviously be different... can still be 
            // figured out - but perhaps we shouldn't bother; since thumbnails 
            // are essentially guaranteed to be small)
            if (!df.isTabularData() && (di.getConversionParam() == null || "".equals(di.getConversionParam()))) {
                if (df.getFilesize() > 0) {
                    return df.getFilesize();
                }
            }
            
            // For Tabular files:
            // If it's just a straight file download, it's pretty easy - we 
            // already know the size of the file on disk (just like in the 
            // fragment above); we just need to make sure if we are also supplying
            // the additional variable name header - then we need to add its 
            // size to the total... But the cases when it's a format conversion 
            // and, especially, subsets are of course trickier. (these are not
            // supported yet).
            
            if (df.isTabularData() && (di.getConversionParam() == null || "".equals(di.getConversionParam()))) {
                long fileSize = df.getFilesize();
                if (fileSize > 0) {
                    if (extraHeader != null) {
                        fileSize += extraHeader.getBytes().length;
                    }
                    return fileSize;
                }
            }
        }
        return -1;
    }

}
