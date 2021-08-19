package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.api.errorhandlers.ApiErrorResponse;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.DataConverter;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.dataaccess.InputStreamIO;
import edu.harvard.iq.dataverse.dataaccess.S3AccessIO;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.dataaccess.StoredOriginalFile;
import edu.harvard.iq.dataverse.dataaccess.TabularSubsetGenerator;
import edu.harvard.iq.dataverse.datafile.page.WholeDatasetDownloadLogger;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateGuestbookResponseCommand;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.datavariable.DataVariable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * @author Leonid Andreev
 */
@Provider
public class DownloadInstanceWriter implements MessageBodyWriter<DownloadInstance> {

    private static final Logger logger = LoggerFactory.getLogger(DownloadInstanceWriter.class);

    @Inject
    private DataConverter dataConverter;

    @Inject
    private WholeDatasetDownloadLogger datasetDownloadLogger;

    @Inject
    private ImageThumbConverter imageThumbConverter;

    private DataAccess dataAccess = DataAccess.dataAccess();

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
        if (di.getDownloadInfo() == null || di.getDownloadInfo().getDataFile() == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        DataFile dataFile = di.getDownloadInfo().getDataFile();
        StorageIO<DataFile> storageIO = dataAccess.getStorageIO(dataFile);


        try {
            storageIO.open();
        } catch (IOException ioex) {
            //throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
            logger.info("Datafile {}: Failed to locate and/or open physical file. Error message: {}", dataFile.getId(), ioex.getLocalizedMessage());
            throw new NotFoundException("Datafile " + dataFile.getId() + ": Failed to locate and/or open physical file.");
        }

        if (StringUtils.equals("imageThumb", di.getConversionParam())) {
            int thumbnailSize = NumberUtils.toInt(di.getConversionParamValue(), ImageThumbConverter.DEFAULT_THUMBNAIL_SIZE);
            InputStreamIO thumbnailStorageIO = Optional
                    .ofNullable(imageThumbConverter.getImageThumbnailAsInputStream(dataFile, thumbnailSize))
                    .orElseThrow(() -> new WebApplicationException(ApiErrorResponse.errorResponse(404, "Image thumbnail not found").asJaxRsResponse()));

            // and, since we now have tabular data files that can 
            // have thumbnail previews... obviously, we don't want to 
            // add the variable header to the image stream!
            thumbnailStorageIO.setNoVarHeader(Boolean.TRUE);
            thumbnailStorageIO.setVarHeader(null);

            writeStorageIOToOutputStream(thumbnailStorageIO, outstream, httpHeaders);
            return;
        }
        if (StringUtils.equals("noVarHeader", di.getConversionParam()) && dataFile.isTabularData()) {
            logger.debug("tabular data with no var header requested");
            storageIO.setNoVarHeader(Boolean.TRUE);
            storageIO.setVarHeader(null);

            writeStorageIOWithGuesbookAndWholeDatasetDownloadSave(storageIO, outstream, httpHeaders, di, dataFile);
            return;
        }
        if (StringUtils.equals("format", di.getConversionParam()) && dataFile.isTabularData()) {
            // Conversions, and downloads of "stored originals" are 
            // now supported on all DataFiles for which StorageIO 
            // access drivers are available.

            if ("original".equals(di.getConversionParamValue())) {
                logger.debug("stored original of an ingested file requested");
                storageIO = StoredOriginalFile.retreive(storageIO, dataFile.getDataTable());
                if (storageIO == null) {
                    throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
                }
            } else {
                // Other format conversions:
                logger.debug("format conversion on a tabular file requested (" + di.getConversionParamValue() + ")");
                String requestedMimeType = di.getServiceFormatType(di.getConversionParam(), di.getConversionParamValue());
                if (requestedMimeType == null) {
                    // default mime type, in case real type is unknown;
                    // (this shouldn't happen in real life - but just in case): 
                    requestedMimeType = "application/octet-stream";
                }
                storageIO = Optional.ofNullable(dataConverter.performFormatConversion(dataFile, storageIO, 
                                                                                      di.getConversionParamValue(), requestedMimeType))
                                    .orElseThrow(() -> new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE));
            }
            
            if (StringUtils.equals("prep", di.getConversionParamValue())) {
                writeStorageIOToOutputStream(storageIO, outstream, httpHeaders);
            } else {
                writeStorageIOWithGuesbookAndWholeDatasetDownloadSave(storageIO, outstream, httpHeaders, di, dataFile);
            }
            return;
        }
        if (StringUtils.equals("subset", di.getConversionParam()) && dataFile.isTabularData()) {
            logger.debug("processing subset request.");

            // TODO: 
            // If there are parameters on the list that are 
            // not valid variable ids, or if they do not belong to
            // the datafile referenced - I simply skip them; 
            // perhaps I should throw an invalid argument exception 
            // instead. 

            List<DataVariable> filteredVariables = filterToVariablesOfDataFile(di, dataFile);

            if (filteredVariables.isEmpty()) {
                writeStorageIOWithGuesbookAndWholeDatasetDownloadSave(storageIO, outstream, httpHeaders, di, dataFile);
                return;
            }


            List<Integer> variablePositionIndex = filteredVariables.stream().map(DataVariable::getFileOrder).collect(toList());
            String subsetVariableHeader = filteredVariables.stream().map(DataVariable::getName).collect(joining("\t"));
            subsetVariableHeader = subsetVariableHeader.concat("\n");

            Optional<File> tempSubsetFile = Optional.empty();
            try {
                tempSubsetFile = Optional.of(File.createTempFile("tempSubsetFile", ".tmp"));
                TabularSubsetGenerator tabularSubsetGenerator = new TabularSubsetGenerator();
                tabularSubsetGenerator.subsetFile(storageIO.getInputStream(), tempSubsetFile.get().getAbsolutePath(), variablePositionIndex, dataFile.getDataTable().getCaseQuantity(), "\t");

                long subsetSize = tempSubsetFile.get().length();

                String tabularFileName = storageIO.getFileName();

                if (tabularFileName != null && tabularFileName.endsWith(".tab")) {
                    tabularFileName = tabularFileName.replaceAll("\\.tab$", "-subset.tab");
                } else if (tabularFileName != null && !"".equals(tabularFileName)) {
                    tabularFileName = tabularFileName.concat("-subset.tab");
                } else {
                    tabularFileName = "subset.tab";
                }

                InputStreamIO subsetStreamIO = new InputStreamIO(new FileInputStream(tempSubsetFile.get()), subsetSize, tabularFileName, storageIO.getMimeType());
                logger.debug("successfully created subset output stream.");

                subsetStreamIO.setVarHeader(subsetVariableHeader);

                storageIO = subsetStreamIO;
                writeStorageIOToOutputStream(storageIO, outstream, httpHeaders);
                writeGuestbookResponse(di);

                return;
            } catch (IOException ioex) {
                throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
            } finally {
                tempSubsetFile.ifPresent(File::delete);
            }
        }

        // There's no conversion etc., so we should enable check
        //                    checkForWholeDatasetDownload = true;

        if (storageIO instanceof S3AccessIO && !(dataFile.isTabularData()) && isRedirectToS3()) {
            // definitely close the (still open) S3 input stream, 
            // since we are not going to use it. The S3 documentation
            // emphasizes that it is very important not to leave these
            // lying around un-closed, since they are going to fill 
            // up the S3 connection pool!
            try {
                storageIO.getInputStream().close();
            } catch (IOException ioex) {
                logger.warn("Exception during closing input stream: ", ioex);
            }
            URI redirectUri = generateTemporaryS3URI((S3AccessIO<DataFile>) storageIO);

            // increment the download count, if necessary:
            writeGuestbookResponse(di);
            datasetDownloadLogger.incrementLogIfDownloadingWholeDataset(Collections.singletonList(dataFile));

            // finally, issue the redirect:
            Response response = Response.seeOther(redirectUri).build();
            logger.info("Issuing redirect to the file location on S3.");
            throw new RedirectionException(response);
        }

        writeStorageIOWithGuesbookAndWholeDatasetDownloadSave(storageIO, outstream, httpHeaders, di, dataFile);
    }

    private void writeStorageIOWithGuesbookAndWholeDatasetDownloadSave(StorageIO<DataFile> storageIO, OutputStream outstream,
            MultivaluedMap<String, Object> httpHeaders, DownloadInstance di, DataFile dataFile) throws IOException {

        writeStorageIOToOutputStream(storageIO, outstream, httpHeaders);
        writeGuestbookResponse(di);
        datasetDownloadLogger.incrementLogIfDownloadingWholeDataset(Collections.singletonList(dataFile));
    }
    
    private List<DataVariable> filterToVariablesOfDataFile(DownloadInstance di, DataFile dataFile) {
        List<DataVariable> dataVariables = new ArrayList<>();
        if (di.getExtraArguments() == null || di.getExtraArguments().size() == 0) {
            logger.debug("empty list of extra arguments.");
            return dataVariables;
        }
        
        logger.debug("processing extra arguments list of length " + di.getExtraArguments().size());
        for (int i = 0; i < di.getExtraArguments().size(); i++) {
            DataVariable variable = (DataVariable) di.getExtraArguments().get(i);
            if (variable != null) {
                if (variable.getDataTable().getDataFile().getId().equals(dataFile.getId())) {
                    logger.debug("adding variable id " + variable.getId() + " to the list.");
                    dataVariables.add(variable);
                } else {
                    logger.warn("variable does not belong to this data file.");
                }
            }
        }
        return dataVariables;
    }
    
    private URI generateTemporaryS3URI(S3AccessIO<DataFile> s3StorageIO) {
        String redirectUrlStr;
        try {
            redirectUrlStr = s3StorageIO.generateTemporaryS3Url();
        } catch (IOException ioex) {
            throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
        }

        if (redirectUrlStr == null) {
            throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
        }

        logger.info("Data Access API: direct S3 url: " + redirectUrlStr);

        try {
            return new URI(redirectUrlStr);
        } catch (URISyntaxException ex) {
            logger.info("Data Access API: failed to create S3 redirect url (" + redirectUrlStr + ")");
            throw new WebApplicationException(Response.Status.SERVICE_UNAVAILABLE);
        }
    }
    
    private void writeGuestbookResponse(DownloadInstance di) {
        if (di.getGbr() != null) {
            try {
                logger.debug("writing guestbook response.");
                Command<?> cmd = new CreateGuestbookResponseCommand(di.getDataverseRequestService().getDataverseRequest(), di.getGbr(), di.getGbr().getDataFile().getOwner());
                di.getCommand().submit(cmd);
            } catch (CommandException ce) {
                logger.warn("Exception while writing into guestbook: ", ce);
            }
        } else {
            logger.debug("not writing guestbook response");
        }
    }
    
    private void writeStorageIOToOutputStream(StorageIO<DataFile> storageIO, OutputStream outstream, MultivaluedMap<String, Object> httpHeaders) throws IOException {
        
        try (InputStream instream = storageIO.getInputStream()) {

            if (instream == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            // headers:

            String fileName = storageIO.getFileName();
            String mimeType = storageIO.getMimeType();

            // Provide both the "Content-disposition" and "Content-Type" headers,
            // to satisfy the widest selection of browsers out there.

            fileName = encodeURI(fileName);
            httpHeaders.add("Content-Disposition", "attachment; filename*=utf-8''"+ fileName + "; filename="+fileName);
            httpHeaders.add("Content-Type", mimeType + "; name=\"" + fileName + "\"");

            long contentSize = getContentSize(storageIO);
            //if ((contentSize = getFileSize(di, storageIO.getVarHeader())) > 0) {
            if (contentSize > 0) {
                logger.debug("Content size (retrieved from the AccessObject): " + contentSize);
                httpHeaders.add("Content-Length", contentSize);
            }

            // (the httpHeaders map must be modified *before* writing any
            // data in the output stream!)

            int bufsize;
            byte[] bffr = new byte[4 * 8192];

            // before writing out any bytes from the input stream, flush
            // any extra content, such as the variable header for the 
            // subsettable files: 

            if (storageIO.getVarHeader() != null) {
                if (storageIO.getVarHeader().getBytes().length > 0) {
                    outstream.write(storageIO.getVarHeader().getBytes());
                }
            }

            while ((bufsize = instream.read(bffr)) != -1) {
                outstream.write(bffr, 0, bufsize);
            }

        } finally {
            outstream.close();
        }
    }

    /**
     * Fix for RFC6266 Content-Disposition encoding taken from primefaces:
     * https://github.com/primefaces/primefaces/pull/2368
     */
    private static String encodeURI(String string) throws UnsupportedEncodingException {
        if (string == null) {
            return null;
        }

        return URLEncoder.encode(string, "UTF-8")
                .replace("+", "%20")
                .replace("%21", "!")
                .replace("%27", "'")
                .replace("%28", "(")
                .replace("%29", ")")
                .replace("%7E", "~");
    }

    private long getContentSize(StorageIO<?> accessObject) {

        try {
            long contentSize = accessObject.getSize();

            if (accessObject.getVarHeader() != null) {
                contentSize += accessObject.getVarHeader().getBytes().length;
            }
            return contentSize;

        } catch(IOException e) {
            logger.warn("Unable to obtain content size", e);
        }
        return -1;
    }

    private boolean isRedirectToS3() {
        String optionValue = System.getProperty("dataverse.files.s3-download-redirect");
        return "true".equalsIgnoreCase(optionValue);
    }

}
