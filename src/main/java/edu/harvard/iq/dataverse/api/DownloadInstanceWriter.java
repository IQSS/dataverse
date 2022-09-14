/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.AuxiliaryFile;
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
import edu.harvard.iq.dataverse.globus.GlobusServiceBean;
import edu.harvard.iq.dataverse.makedatacount.MakeDataCountLoggingServiceBean;
import edu.harvard.iq.dataverse.makedatacount.MakeDataCountLoggingServiceBean.MakeDataCountEntry;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.HttpHeaders;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

/**
 *
 * @author Leonid Andreev
 */
@Provider
public class DownloadInstanceWriter implements MessageBodyWriter<DownloadInstance> {

    @Inject
    MakeDataCountLoggingServiceBean mdcLogService;
    @Inject
    SystemConfig systemConfig;
    @Inject
    GlobusServiceBean globusService;

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
            StorageIO<DataFile> storageIO = DataAccess.getStorageIO(dataFile, daReq);

            if (storageIO != null) {
                try {
                    storageIO.open();
                } catch (IOException ioex) {
                    //throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
                    logger.log(Level.INFO, "Datafile {0}: Failed to locate and/or open physical file. Error message: {1}", new Object[]{dataFile.getId(), ioex.getLocalizedMessage()});
                    throw new NotFoundException("Datafile " + dataFile.getId() + ": Failed to locate and/or open physical file.");
                }

                
                boolean redirectSupported = false;
                String auxiliaryTag = null;
                String auxiliaryType = null;
                String auxiliaryFileName = null; 
                // Before we do anything else, check if this download can be handled 
                // by a redirect to remote storage (only supported on S3, as of 5.4):
                if (storageIO.downloadRedirectEnabled()) {

                    // Even if the above is true, there are a few cases where a  
                    // redirect is not applicable. 
                    // For example, for a tabular file, we can redirect a request 
                    // for a saved original; but CANNOT if it is a column subsetting 
                    // request (must be streamed in real time locally); or a format
                    // conversion that hasn't been cached and saved on S3 yet. 
                    redirectSupported = true;


                    if ("imageThumb".equals(di.getConversionParam())) {

                        // Can redirect - but only if already generated and cached.
                        int requestedSize = 0;
                        if (!"".equals(di.getConversionParamValue())) {
                            try {
                                requestedSize = Integer.parseInt(di.getConversionParamValue());
                            } catch (java.lang.NumberFormatException ex) {
                                // it's ok, the default size will be used.
                            }
                        }

                        auxiliaryTag = ImageThumbConverter.THUMBNAIL_SUFFIX + (requestedSize > 0 ? requestedSize : ImageThumbConverter.DEFAULT_THUMBNAIL_SIZE);

                        if (storageIO.downloadRedirectEnabled(auxiliaryTag) && isAuxiliaryObjectCached(storageIO, auxiliaryTag)) {
                            auxiliaryType = ImageThumbConverter.THUMBNAIL_MIME_TYPE;
                            String fileName = storageIO.getFileName();
                            if (fileName != null) {
                                auxiliaryFileName = fileName.replaceAll("\\.[^\\.]*$", ImageThumbConverter.THUMBNAIL_FILE_EXTENSION);
                            }
                        } else {
                            redirectSupported = false;
                        }

                    } else if (di.getAuxiliaryFile() != null) {
                        // We should support redirects to auxiliary files too.
                    
                        auxiliaryTag = di.getAuxiliaryFile().getFormatTag();
                        String auxVersion = di.getAuxiliaryFile().getFormatVersion();
                        if (auxVersion != null) {
                            auxiliaryTag = auxiliaryTag + "_" + auxVersion;
                        }
                    
                        if (storageIO.downloadRedirectEnabled(auxiliaryTag) && isAuxiliaryObjectCached(storageIO, auxiliaryTag)) {
                            String fileExtension = getFileExtension(di.getAuxiliaryFile());
                            auxiliaryFileName = storageIO.getFileName() + "." + auxiliaryTag + fileExtension;
                            auxiliaryType = di.getAuxiliaryFile().getContentType();
                        } else {
                            redirectSupported = false;
                        }

                    } else if (dataFile.isTabularData()) {
                        // Many separate special cases here.

                        if (di.getConversionParam() != null) {
                            if (di.getConversionParam().equals("format")) {

                                if ("original".equals(di.getConversionParamValue())) {
                                    auxiliaryTag = StoredOriginalFile.SAVED_ORIGINAL_FILENAME_EXTENSION;
                                    auxiliaryType = dataFile.getOriginalFileFormat(); 
                                    auxiliaryFileName = dataFile.getOriginalFileName();
                                } else {
                                    // format conversions - can redirect, but only if 
                                    // it has been cached already. 

                                    auxiliaryTag = di.getConversionParamValue();
                                    if (storageIO.downloadRedirectEnabled(auxiliaryTag) && isAuxiliaryObjectCached(storageIO, auxiliaryTag)) {
                                        auxiliaryType = di.getServiceFormatType(di.getConversionParam(), auxiliaryTag);
                                        auxiliaryFileName = FileUtil.replaceExtension(storageIO.getFileName(), auxiliaryTag);
                                    } else {
                                        redirectSupported = false;
                                    }
                                }
                            } else if (!di.getConversionParam().equals("noVarHeader")) {
                                // This is a subset request - can't do. 
                                redirectSupported = false;
                            }
                        } else {
                            redirectSupported = false;
                        }
                    }
                }
                String redirect_url_str=null;

                if (redirectSupported) {
                    // definitely close the (potentially still open) input stream, 
                    // since we are not going to use it. The S3 documentation in particular
                    // emphasizes that it is very important not to leave these
                    // lying around un-closed, since they are going to fill 
                    // up the S3 connection pool!
                    storageIO.closeInputStream();
                    // [attempt to] redirect: 
                    try {
                        redirect_url_str = storageIO.generateTemporaryDownloadUrl(auxiliaryTag, auxiliaryType, auxiliaryFileName);
                    } catch (IOException ioex) {
                        logger.warning("Unable to generate downloadURL for " + dataFile.getId() + ": " + auxiliaryTag);
                        //Setting null will let us try to get the file/aux file w/o redirecting 
                        redirect_url_str = null;
                    }
                }
                
                if (systemConfig.isGlobusFileDownload() && systemConfig.getGlobusStoresList()
                        .contains(DataAccess.getStorageDriverFromIdentifier(dataFile.getStorageIdentifier()))) {
                    if (di.getConversionParam() != null) {
                        if (di.getConversionParam().equals("format")) {

                            if ("GlobusTransfer".equals(di.getConversionParamValue())) {
                                redirect_url_str = globusService.getGlobusAppUrlForDataset(dataFile.getOwner(), false, dataFile);
                            }
                        }
                    }
                    if (redirect_url_str!=null) {

                        logger.fine("Data Access API: redirect url: " + redirect_url_str);
                        URI redirect_uri;

                        try {
                            redirect_uri = new URI(redirect_url_str);
                        } catch (URISyntaxException ex) {
                            logger.info("Data Access API: failed to create redirect url (" + redirect_url_str + ")");
                            redirect_uri = null;
                        }
                        if (redirect_uri != null) {
                            // increment the download count, if necessary:
                            if (di.getGbr() != null && !(isThumbnailDownload(di) || isPreprocessedMetadataDownload(di))) {
                                try {
                                    logger.fine("writing guestbook response, for a download redirect.");
                                    Command<?> cmd = new CreateGuestbookResponseCommand(di.getDataverseRequestService().getDataverseRequest(), di.getGbr(), di.getGbr().getDataFile().getOwner());
                                    di.getCommand().submit(cmd);
                                    MakeDataCountEntry entry = new MakeDataCountEntry(di.getRequestUriInfo(), di.getRequestHttpHeaders(), di.getDataverseRequestService(), di.getGbr().getDataFile());
                                    mdcLogService.logEntry(entry);
                                } catch (CommandException e) {
                                }
                            }

                            // finally, issue the redirect:
                            Response response = Response.seeOther(redirect_uri).build();
                            logger.fine("Issuing redirect to the file location.");
                            throw new RedirectionException(response);
                        }
                        throw new ServiceUnavailableException();
                    }
                }

                if (di.getConversionParam() != null) {
                    // Image Thumbnail and Tabular data conversion: 
                    // NOTE: only supported on local files, as of 4.0.2!
                    // NOTE: should be supported on all files for which StorageIO drivers
                    // are available (but not on harvested files1) -- L.A. 4.6.2

                    if (di.getConversionParam().equals("imageThumb") && !dataFile.isHarvested()) {
                        if ("".equals(di.getConversionParamValue())) {
                            storageIO = ImageThumbConverter.getImageThumbnailAsInputStream(storageIO, ImageThumbConverter.DEFAULT_THUMBNAIL_SIZE);
                        } else {
                            try {
                                int size = new Integer(di.getConversionParamValue());
                                if (size > 0) {
                                    storageIO = ImageThumbConverter.getImageThumbnailAsInputStream(storageIO, size);
                                }
                            } catch (java.lang.NumberFormatException ex) {
                                storageIO = ImageThumbConverter.getImageThumbnailAsInputStream(storageIO, ImageThumbConverter.DEFAULT_THUMBNAIL_SIZE);
                            }

                            // and, since we now have tabular data files that can 
                            // have thumbnail previews... obviously, we don't want to 
                            // add the variable header to the image stream!
                            storageIO.setNoVarHeader(Boolean.TRUE);
                            storageIO.setVarHeader(null);
                        }
                    } else if (dataFile.isTabularData()) {
                        logger.fine("request for tabular data download;");
                        // We can now generate thumbnails for some tabular data files (specifically, 
                        // tab files tagged as "geospatial"). We are going to assume that you can 
                        // do only ONE thing at a time - request the thumbnail for the file, or 
                        // request any tabular-specific services. 

                        if (di.getConversionParam().equals("noVarHeader")) {
                            logger.fine("tabular data with no var header requested");
                            storageIO.setNoVarHeader(Boolean.TRUE);
                            storageIO.setVarHeader(null);
                        } else if (di.getConversionParam().equals("format")) {
                            // Conversions, and downloads of "stored originals" are 
                            // now supported on all DataFiles for which StorageIO 
                            // access drivers are available.

                            if ("original".equals(di.getConversionParamValue())) {
                                logger.fine("stored original of an ingested file requested");
                                storageIO = StoredOriginalFile.retreive(storageIO);
                            } else {
                                // Other format conversions: 
                                logger.fine("format conversion on a tabular file requested (" + di.getConversionParamValue() + ")");
                                String requestedMimeType = di.getServiceFormatType(di.getConversionParam(), di.getConversionParamValue());
                                if (requestedMimeType == null) {
                                    // default mime type, in case real type is unknown;
                                    // (this shouldn't happen in real life - but just in case): 
                                    requestedMimeType = "application/octet-stream";
                                }
                                storageIO
                                        = DataConverter.performFormatConversion(dataFile,
                                                storageIO,
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
                                logger.fine("processing extra arguments list of length " + di.getExtraArguments().size());
                                List<Integer> variablePositionIndex = new ArrayList<>();
                                String subsetVariableHeader = null;
                                for (int i = 0; i < di.getExtraArguments().size(); i++) {
                                    DataVariable variable = (DataVariable) di.getExtraArguments().get(i);
                                    if (variable != null) {
                                        if (variable.getDataTable().getDataFile().getId().equals(dataFile.getId())) {
                                            logger.fine("adding variable id " + variable.getId() + " to the list.");
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
                                        tabularSubsetGenerator.subsetFile(storageIO.getInputStream(), tempSubsetFile.getAbsolutePath(), variablePositionIndex, dataFile.getDataTable().getCaseQuantity(), "\t");

                                        if (tempSubsetFile.exists()) {
                                            FileInputStream subsetStream = new FileInputStream(tempSubsetFile);
                                            long subsetSize = tempSubsetFile.length();

                                            InputStreamIO subsetStreamIO = new InputStreamIO(subsetStream, subsetSize);
                                            logger.fine("successfully created subset output stream.");
                                            subsetVariableHeader = subsetVariableHeader.concat("\n");
                                            subsetStreamIO.setVarHeader(subsetVariableHeader);

                                            String tabularFileName = storageIO.getFileName();

                                            if (tabularFileName != null && tabularFileName.endsWith(".tab")) {
                                                tabularFileName = tabularFileName.replaceAll("\\.tab$", "-subset.tab");
                                            } else if (tabularFileName != null && !"".equals(tabularFileName)) {
                                                tabularFileName = tabularFileName.concat("-subset.tab");
                                            } else {
                                                tabularFileName = "subset.tab";
                                            }

                                            subsetStreamIO.setFileName(tabularFileName);
                                            subsetStreamIO.setMimeType(storageIO.getMimeType());
                                            storageIO = subsetStreamIO;
                                        } else {
                                            storageIO = null;
                                        }
                                    } catch (IOException ioex) {
                                        storageIO = null;
                                    }
                                }
                            } else {
                                logger.fine("empty list of extra arguments.");
                            }
                        }
                    }

                    if (storageIO == null) {
                        //throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
                        // 404/not found may be a better return code option here
                        // (similarly to what the Access API returns when a thumbnail is requested on a text file, etc.)
                        throw new NotFoundException("datafile access error: requested optional service (image scaling, format conversion, etc.) could not be performed on this datafile.");
                    }
                } else if (di.getAuxiliaryFile() != null) {
                    // Make sure to close the InputStream for the main datafile: 
                    storageIO.closeInputStream();
                    String auxTag = di.getAuxiliaryFile().getFormatTag();
                    String auxVersion = di.getAuxiliaryFile().getFormatVersion();
                    if (auxVersion != null) {
                        auxTag = auxTag + "_" + auxVersion;
                    }
                    long auxFileSize = di.getAuxiliaryFile().getFileSize();
                    InputStreamIO auxStreamIO = new InputStreamIO(storageIO.getAuxFileAsInputStream(auxTag), auxFileSize);
                    String fileExtension = getFileExtension(di.getAuxiliaryFile());
                    auxStreamIO.setFileName(storageIO.getFileName() + "." + auxTag + fileExtension);
                    auxStreamIO.setMimeType(di.getAuxiliaryFile().getContentType());
                    storageIO = auxStreamIO;

                } 

                try (InputStream instream = storageIO.getInputStream()) {
                    if (instream != null) {
                        // headers:

                        String fileName = storageIO.getFileName();
                        String mimeType = storageIO.getMimeType();

                        // Provide both the "Content-disposition" and "Content-Type" headers,
                        // to satisfy the widest selection of browsers out there. 
                        // Encode the filename as UTF-8, then deal with spaces. "encode" changes
                        // a space to + so we change it back to a space (%20).
                        String finalFileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
                        httpHeaders.add("Content-disposition", "attachment; filename=\"" + finalFileName + "\"");
                        httpHeaders.add("Content-Type", mimeType + "; name=\"" + finalFileName + "\"");

                        long contentSize;
                        
                        // User may have requested a rangeHeader of bytes.
                        // Ranges are only supported when the size of the content 
                        // stream is known (i.e., it's not a dynamically generated 
                        // stream. 
                        List<Range> ranges = new ArrayList<>();
                        String rangeHeader = null;
                        HttpHeaders headers = di.getRequestHttpHeaders();
                        if (headers != null) {
                            rangeHeader = headers.getHeaderString("Range");
                        }
                        long offset = 0;
                        long leftToRead = -1L; 
                        // Moving the "left to read" var. here; - since we may need 
                        // to start counting our rangeHeader bytes outside the main .write()
                        // loop, if it's a tabular file with a header. 
                        
                        if ((contentSize = getContentSize(storageIO)) > 0) {
                            try {
                                ranges = getRanges(rangeHeader, contentSize);
                            } catch (Exception ex) {
                                logger.fine("Exception caught processing Range header: " + ex.getLocalizedMessage());
                                throw new ClientErrorException("Error due to Range header: " + ex.getLocalizedMessage(), Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE);
                            }
                            
                            if (ranges.isEmpty()) {
                                logger.fine("Content size (retrieved from the AccessObject): " + contentSize);
                                httpHeaders.add("Content-Length", contentSize);
                            } else  {
                                // For now we only support a single rangeHeader.
                                long rangeContentSize = ranges.get(0).getLength();
                                logger.fine("Content size (Range header in use): " + rangeContentSize);
                                httpHeaders.add("Content-Length", rangeContentSize);
                                
                                offset = ranges.get(0).getStart();
                                leftToRead = rangeContentSize;
                                httpHeaders.add("Accept-Ranges", "bytes");
                                httpHeaders.add("Content-Range", "bytes "+offset+"-"+(offset+rangeContentSize-1)+"/"+contentSize);

                            }
                        } else {
                            // Content size unknown, must be a dynamically
                            // generated stream, such as a subsetting request.
                            // We do NOT want to support rangeHeader requests on such streams:
                            if (rangeHeader != null) {
                                throw new NotFoundException("Range headers are not supported on dynamically-generated content, such as tabular subsetting.");
                            }
                            
                        }

                        // (the httpHeaders map must be modified *before* writing any
                        // data in the output stream!)
                        int bufsize;
                        byte[] bffr = new byte[4 * 8192];

                        // Before writing out any bytes from the input stream, write
                        // any extra content, such as the variable header for the 
                        // subsettable files: 
                        if (storageIO.getVarHeader() != null) {
                            logger.fine("storageIO.getVarHeader().getBytes().length: " + storageIO.getVarHeader().getBytes().length);
                            if (storageIO.getVarHeader().getBytes().length > 0) {
                                // If a rangeHeader is not being requested, let's call that the normal case.
                                // Write the entire line of variable headers. Later, the rest of the file
                                // will be written.
                                if (ranges.isEmpty()) {
                                    logger.fine("writing the entire variable header");
                                    outstream.write(storageIO.getVarHeader().getBytes());
                                } else {
                                    // Range requested. Since the output stream of a 
                                    // tabular file is made up of the varHeader and the body of 
                                    // the physical file, we should assume that the requested 
                                    // rangeHeader may span any portion of the combined stream.
                                    // Thus we may or may not have to write the header, or a 
                                    // portion thereof. 
                                    int headerLength = storageIO.getVarHeader().getBytes().length;
                                    if (offset >= headerLength) {
                                        // We can skip the entire header. 
                                        // All we need to do is adjust the byte offset 
                                        // in the physical file; the number of bytes
                                        // left to write stays unchanged, since we haven't
                                        // written anything.
                                        logger.fine("Skipping the variable header completely.");
                                        offset -= headerLength;
                                    } else {
                                        // We need to write some portion of the header; 
                                        // Once we are done, we may or may not still have 
                                        // some bytes left to write from the main physical file.
                                        if (offset + leftToRead <= headerLength) {
                                            // This is a more straightforward case - we just need to 
                                            // write a portion of the header, and then we are done!
                                            logger.fine("Writing this many bytes of the variable header line: " + leftToRead);
                                            outstream.write(Arrays.copyOfRange(storageIO.getVarHeader().getBytes(), (int)offset, (int)offset + (int)leftToRead));
                                            // set "left to read" to zero, indicating that we are done:
                                            leftToRead = 0; 
                                        } else {
                                            // write the requested portion of the header:
                                            logger.fine("Writing this many bytes of the variable header line: " + (headerLength - offset));
                                            outstream.write(Arrays.copyOfRange(storageIO.getVarHeader().getBytes(), (int)offset, headerLength));
                                            // and adjust the file offset and remaining number of bytes accordingly: 
                                            leftToRead -= (headerLength - offset);
                                            offset = 0;
                                        }
                                        
                                    }
                                }
                            }
                        }

                        // Dynamic streams, etc. Normal operation. No leftToRead.
                        if (ranges.isEmpty()) {
                            logger.fine("Normal, non-range request of file id " + dataFile.getId());
                            while ((bufsize = instream.read(bffr)) != -1) {
                                outstream.write(bffr, 0, bufsize);
                            }
                        } else if (leftToRead > 0) {
                            // This is a rangeHeader request, and we still have bytes to read 
                            // (for a tabular file, we may have already written enough
                            // bytes from the variable header!)
                            storageIO.setOffset(offset);
                            // Thinking about it, we could just do instream.skip(offset) 
                            // here... But I would like to have this offset functionality 
                            // in StorageIO, for any future cases where we may not 
                            // be able to do that on the stream directly (?) -- L.A.
                            logger.fine("Range request of file id " + dataFile.getId());
                            // Read a rangeHeader of bytes instead of the whole file. We'll count down as we write.
                            // For now we only support a single rangeHeader.
                            while ((bufsize = instream.read(bffr)) != -1) {
                                if ((leftToRead -= bufsize) > 0) {
                                    // Just do a normal write. Potentially lots to go. Don't break.
                                    outstream.write(bffr, 0, bufsize);
                                } else {
                                    // Get those last bytes or bytes equal to bufsize. Last one. Then break.
                                    outstream.write(bffr, 0, (int) leftToRead + bufsize);
                                    break;
                                }
                            }

                        }

                        logger.fine("di conversion param: " + di.getConversionParam() + ", value: " + di.getConversionParamValue());

                        // Downloads of thumbnail images (scaled down, low-res versions of graphic image files) and 
                        // "preprocessed metadata" records for tabular data files are NOT considered "real" downloads, 
                        // so these should not produce guestbook entries: 
                        if (di.getGbr() != null && !(isThumbnailDownload(di) || isPreprocessedMetadataDownload(di))) {
                            try {
                                logger.fine("writing guestbook response.");
                                Command<?> cmd = new CreateGuestbookResponseCommand(di.getDataverseRequestService().getDataverseRequest(), di.getGbr(), di.getGbr().getDataFile().getOwner());
                                di.getCommand().submit(cmd);
                                MakeDataCountEntry entry = new MakeDataCountEntry(di.getRequestUriInfo(), di.getRequestHttpHeaders(), di.getDataverseRequestService(), di.getGbr().getDataFile());
                                mdcLogService.logEntry(entry);
                            } catch (CommandException e) {
                            }
                        } else {
                            logger.fine("not writing guestbook response");
                        }

                        outstream.close();
                        return;
                    }
                }
            }
        }

        throw new NotFoundException();

    }

    private boolean isAuxiliaryObjectCached(StorageIO storageIO, String auxiliaryTag) {
        try {
            return storageIO.isAuxObjectCached(auxiliaryTag);
        } catch (IOException cachedIOE) {
            return false; 
        }
    }
    
    // TODO: Return ".md" for "text/markdown" as well as other extensions in MimeTypeDetectionByFileExtension.properties
    private String getFileExtension(AuxiliaryFile auxFile) {
        String fileExtension = "";
        if (auxFile == null) {
            return fileExtension;
        }
        String contentType = auxFile.getContentType();
        if (contentType != null) {
            MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
            try {
                MimeType mimeType = allTypes.forName(contentType);
                fileExtension = mimeType.getExtension();
            } catch (MimeTypeException ex) {
            }
        }
        return fileExtension;
    }

    private boolean isThumbnailDownload(DownloadInstance downloadInstance) {
        if (downloadInstance == null) {
            return false;
        }

        if (downloadInstance.getConversionParam() == null) {
            return false;
        }

        return downloadInstance.getConversionParam().equals("imageThumb");
    }

    private boolean isPreprocessedMetadataDownload(DownloadInstance downloadInstance) {
        if (downloadInstance == null) {
            return false;
        }

        if (downloadInstance.getConversionParam() == null) {
            return false;
        }

        if (downloadInstance.getConversionParamValue() == null) {
            return false;
        }

        return downloadInstance.getConversionParam().equals("format") && downloadInstance.getConversionParamValue().equals("prep");
    }

    private long getContentSize(StorageIO<?> accessObject) {
        long contentSize = 0;

        if (accessObject.getSize() > -1) {
            contentSize += accessObject.getSize();
            if (accessObject.getVarHeader() != null) {
                if (accessObject.getVarHeader().getBytes().length > 0) {
                    contentSize += accessObject.getVarHeader().getBytes().length;
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

    /**
     * @param range "bytes 0-10" for example. Found in the "Range" HTTP header.
     * @param fileSize File size in bytes.
     * @throws RunTimeException on any problems processing the Range header.
     */
    public List<Range> getRanges(String range, long fileSize) {
        // Inspired by https://gist.github.com/davinkevin/b97e39d7ce89198774b4
        // via https://stackoverflow.com/questions/28427339/how-to-implement-http-byte-rangeHeader-requests-in-spring-mvc/28479001#28479001
        List<Range> ranges = new ArrayList<>();

        if (range != null) {
            logger.fine("Range header supplied: " + range);

            // Technically this regex supports multiple ranges.
            // Below we have a check to enforce a single range.
            if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
                throw new RuntimeException("The format is bytes=<range-start>-<range-end> where start and end are optional.");
            }

            // The 6 is to remove "bytes="
            String[] parts = range.substring(6).split(",");
            if (parts.length > 1) {
                // Only allow a single range.
                throw new RuntimeException("Only one range is allowed.");
            }
            // This loop is here in case we ever want to support multiple ranges.
            for (String part : parts) {

                long start = getRangeStart(part);
                long end = getRangeEnd(part);

                if (start == -1) {
                    // start does not exist. Base start off of how many bytes from end.
                    start = fileSize - end;
                    end = fileSize - 1;
                } else if (end == -1 || end > fileSize - 1) {
                    // Set end when it doesn't exist.
                    // Also, automatically set end to size of file if end is beyond
                    // the file size (rather than throwing an error).
                    end = fileSize - 1;
                }

                if (start > end) {
                    throw new RuntimeException("Start is larger than end or size of file.");
                }

                ranges.add(new Range(start, end));

            }
        }

        return ranges;
    }

    /**
     * @return Return a positive long or -1 if start does not exist.
     */
    public static long getRangeStart(String part) {
        // Get everything before the "-".
        String start = part.substring(0, part.indexOf("-"));
        return (start.length() > 0) ? Long.parseLong(start) : -1;
    }

    /**
     * @return Return a positive long or -1 if end does not exist.
     */
    public static long getRangeEnd(String part) {
        // Get everything after the "-".
        String end = part.substring(part.indexOf("-") + 1, part.length());
        return (end.length() > 0) ? Long.parseLong(end) : -1;
    }

}
