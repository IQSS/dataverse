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
import edu.harvard.iq.dataverse.dataaccess.*;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateGuestbookResponseCommand;
import java.io.File;
import java.io.FileInputStream;
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
            
            
            
            DataFile dataFile = di.getDownloadInfo().getDataFile();
            DataFileIO dataFileIO = DataAccess.getDataFileIO(dataFile, daReq);
                        
            if (dataFileIO != null) {
                dataFileIO.open();
                
                if (di.getConversionParam() != null) {
                    // Image Thumbnail and Tabular data conversion: 
                    // NOTE: only supported on local files, as of 4.0.2!
                    // NOTE: should be supported on all files for which DataFileIO drivers
                    // are available (but not on harvested files1) -- L.A. 4.6.2
                    
                    if (di.getConversionParam().equals("imageThumb") && !dataFile.isHarvested()) { 
                        if ("".equals(di.getConversionParamValue())) {
                            dataFileIO = ImageThumbConverter.getImageThumbnailAsInputStream(dataFileIO, ImageThumbConverter.DEFAULT_THUMBNAIL_SIZE); 
                        } else {
                            try {
                                int size = new Integer(di.getConversionParamValue()).intValue();
                                if (size > 0) {                                    
                                    dataFileIO = ImageThumbConverter.getImageThumbnailAsInputStream(dataFileIO, size);
                                }
                            } catch (java.lang.NumberFormatException ex) {
                                dataFileIO = ImageThumbConverter.getImageThumbnailAsInputStream(dataFileIO, ImageThumbConverter.DEFAULT_THUMBNAIL_SIZE);
                            }
                            
                            // and, since we now have tabular data files that can 
                            // have thumbnail previews... obviously, we don't want to 
                            // add the variable header to the image stream!
                            
                            dataFileIO.setNoVarHeader(Boolean.TRUE);
                            dataFileIO.setVarHeader(null);
                        }
                    } else if (dataFile.isTabularData()) {
                        logger.fine("request for tabular data download;");
                        // We can now generate thumbnails for some tabular data files (specifically, 
                        // tab files tagged as "geospatial"). We are going to assume that you can 
                        // do only ONE thing at a time - request the thumbnail for the file, or 
                        // request any tabular-specific services. 
                        
                        if (di.getConversionParam().equals("noVarHeader")) {
                            logger.fine("tabular data with no var header requested");
                            dataFileIO.setNoVarHeader(Boolean.TRUE);
                            dataFileIO.setVarHeader(null);
                        } else if (di.getConversionParam().equals("format")) {
                            // Conversions, and downloads of "stored originals" are 
                            // now supported on all DataFiles for which DataFileIO 
                            // access drivers are available.
                            
                            if ("original".equals(di.getConversionParamValue())) {
                                logger.fine("stored original of an ingested file requested");
                                dataFileIO = StoredOriginalFile.retreive(dataFileIO);
                            } else {
                                // Other format conversions: 
                                logger.fine("format conversion on a tabular file requested ("+di.getConversionParamValue()+")");
                                String requestedMimeType = di.getServiceFormatType(di.getConversionParam(), di.getConversionParamValue()); 
                                if (requestedMimeType == null) {
                                    // default mime type, in case real type is unknown;
                                    // (this shouldn't happen in real life - but just in case): 
                                    requestedMimeType = "application/octet-stream";
                                } 
                                dataFileIO = 
                                        DataConverter.performFormatConversion(dataFile, 
                                        dataFileIO, 
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
                            
                            if (di.getExtraArguments() != null && di.getExtraArguments().size() > 0) {
                                logger.fine("processing extra arguments list of length "+di.getExtraArguments().size());
                                List <Integer> variablePositionIndex = new ArrayList<>();
                                String subsetVariableHeader = null;
                                for (int i = 0; i < di.getExtraArguments().size(); i++) {
                                    DataVariable variable = (DataVariable)di.getExtraArguments().get(i);
                                    if (variable != null) {
                                        if (variable.getDataTable().getDataFile().getId().equals(dataFile.getId())) {
                                            logger.fine("adding variable id "+variable.getId()+" to the list.");
                                            variablePositionIndex.add(variable.getFileOrder());
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
                                
                                if (variablePositionIndex.size() > 0) {

                                    try {
                                        File tempSubsetFile = File.createTempFile("tempSubsetFile", ".tmp");
                                        TabularSubsetGenerator tabularSubsetGenerator = new TabularSubsetGenerator();
                                        tabularSubsetGenerator.subsetFile(dataFileIO.getInputStream(), tempSubsetFile.getAbsolutePath(), variablePositionIndex, dataFile.getDataTable().getCaseQuantity(), "\t");

                                        if (tempSubsetFile.exists()) {
                                            FileInputStream subsetStream = new FileInputStream(tempSubsetFile);
                                            long subsetSize = tempSubsetFile.length();

                                            InputStreamIO subsetStreamIO = new InputStreamIO(subsetStream, subsetSize);
                                            logger.fine("successfully created subset output stream.");
                                            subsetVariableHeader = subsetVariableHeader.concat("\n");
                                            subsetStreamIO.setVarHeader(subsetVariableHeader);

                                            String tabularFileName = dataFileIO.getFileName();

                                            if (tabularFileName != null && tabularFileName.endsWith(".tab")) {
                                                tabularFileName = tabularFileName.replaceAll("\\.tab$", "-subset.tab");
                                            } else if (tabularFileName != null && !"".equals(tabularFileName)) {
                                                tabularFileName = tabularFileName.concat("-subset.tab");
                                            } else {
                                                tabularFileName = "subset.tab";
                                            }

                                            subsetStreamIO.setFileName(tabularFileName);
                                            subsetStreamIO.setMimeType(dataFileIO.getMimeType());
                                            dataFileIO = subsetStreamIO;
                                        } else {
                                            dataFileIO = null;
                                        }
                                    } catch (IOException ioex) {
                                        dataFileIO = null;
                                    }
                                }
                            } else {
                                logger.fine("empty list of extra arguments.");
                            }
                        } 
                    }
                    
                    
                    if (dataFileIO == null) {
                        throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
                    }
                }
                
                InputStream instream = dataFileIO.getInputStream();
                if (instream != null) {
                    // headers:
                    
                    String fileName = dataFileIO.getFileName(); 
                    String mimeType = dataFileIO.getMimeType(); 
                    
                    // Provide both the "Content-disposition" and "Content-Type" headers,
                    // to satisfy the widest selection of browsers out there. 
                    
                    httpHeaders.add("Content-disposition", "attachment; filename=\"" + fileName + "\"");
                    httpHeaders.add("Content-Type", mimeType + "; name=\"" + fileName + "\"");
                    
                    long contentSize; 
                    boolean useChunkedTransfer = false; 
                    //if ((contentSize = getFileSize(di, dataFileIO.getVarHeader())) > 0) {
                    if ((contentSize = getContentSize(dataFileIO)) > 0) {
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
                    // subsettable files: 
                    
                    if (dataFileIO.getVarHeader() != null) {
                        if (dataFileIO.getVarHeader().getBytes().length > 0) {
                            if (useChunkedTransfer) {
                                String chunkSizeLine = String.format("%x\r\n", dataFileIO.getVarHeader().getBytes().length);
                                outstream.write(chunkSizeLine.getBytes());
                            }
                            outstream.write(dataFileIO.getVarHeader().getBytes());
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
