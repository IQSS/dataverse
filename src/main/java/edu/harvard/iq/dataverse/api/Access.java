/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.dataaccess.OptionalAccessService;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;

import java.lang.reflect.Type;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.util.Properties;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import javax.servlet.http.HttpServletResponse;

/*
    Custom API exceptions [NOT YET IMPLEMENTED]
import edu.harvard.iq.dataverse.api.exceptions.NotFoundException;
import edu.harvard.iq.dataverse.api.exceptions.ServiceUnavailableException;
import edu.harvard.iq.dataverse.api.exceptions.PermissionDeniedException;
import edu.harvard.iq.dataverse.api.exceptions.AuthorizationRequiredException;
*/

/**
 *
 * @author Leonid Andreev
 * 
 * The data (file) access API is based on the DVN access API v.1.0 (that came 
 * with the v.3.* of the DVN app) and extended for DVN 4.0 to include some
 * extra fancy functionality, such as subsetting individual columns in tabular
 * data files and more.
 */

@Path("access")
public class Access {
    private static final Logger logger = Logger.getLogger(Access.class.getCanonicalName());
    
    private static final String DEFAULT_FILE_ICON = "icon_file.png";
    private static final String DEFAULT_DATASET_ICON = "icon_dataset.png";
    
    @EJB
    DataFileServiceBean dataFileService;
    @EJB 
    DatasetServiceBean datasetService; 

    @EJB
    
    @Path("datafile/{fileId}")
    @GET
    @Produces({ "application/xml" })
    public DownloadInstance datafile(@PathParam("fileId") Long fileId, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response) /*throws NotFoundException, ServiceUnavailableException, PermissionDeniedException, AuthorizationRequiredException*/ {        
        ByteArrayOutputStream outStream = null;
        

        DataFile df = dataFileService.find(fileId);
        /* TODO: 
         * Throw a meaningful exception if file not found!
         * -- L.A. 4.0alpha1
         */
        
        DownloadInfo dInfo = new DownloadInfo(df);

        /*
         * The only "optional access services" supported as of now (4.0alpha1)
         * are image thumbnail generation and "saved original": 
         * (and yes, this is a hack)
         * TODO: un-hack this. -- L.A. 4.0 alpha 1
         */
        if (df.getContentType() != null && df.getContentType().startsWith("image/")) {
            dInfo.addServiceAvailable(new OptionalAccessService("thumbnail", "image/png", "imageThumb=true", "Image Thumbnail (64x64)"));
        }

        if (df.isTabularData()) {
            String originalMimeType = df.getDataTable().getOriginalFileFormat();
            dInfo.addServiceAvailable(new OptionalAccessService("original", originalMimeType, "fileFormat=original","Saved original (" + originalMimeType + ")"));
            
            dInfo.addServiceAvailable(new OptionalAccessService("R", "application/x-rlang-transport", "fileFormat=RData", "Data in R format"));
        }
        DownloadInstance downloadInstance = new DownloadInstance(dInfo);
        
        for (String key : uriInfo.getQueryParameters().keySet()) {
            String value = uriInfo.getQueryParameters().getFirst(key);
            
            if (downloadInstance.isDownloadServiceSupported(key, value)) {
                // this automatically sets the conversion parameters in 
                // the download instance to key and value;
                // TODO: I should probably set these explicitly instead. 
                break;
            } else {
                // Service unknown/not supported/bad arguments, etc.:
                // TODO: throw new ServiceUnavailableException(); 
            }
            
        }
        /* 
         * Provide content type header:
         * (this will be done by the InstanceWriter class - ?)
         */
         
        /* Provide "Access-Control-Allow-Origin" header:
         * (may not be needed here... - that header was added specifically
         * to get the data exploration app to be able to access the metadata
         * API; may have been something specific to Vito's installation too
         * -- L.A.)
         */
        response.setHeader("Access-Control-Allow-Origin", "*");
                
        /* 
         * Provide some browser-friendly headers: (?)
         */
        //return retValue; 
        return downloadInstance;
    }
    
    @Path("imagethumb/{fileSystemId}")
    @GET
    @Produces({ "image/png" })
    public InputStream imagethumb(@PathParam("fileSystemId") Long fileSystemId, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response) /*throws NotFoundException, ServiceUnavailableException, PermissionDeniedException, AuthorizationRequiredException*/ {        
        String filesRootDirectory = System.getProperty("dataverse.files.directory");
        if (filesRootDirectory == null || filesRootDirectory.equals("")) {
            filesRootDirectory = "/tmp/files";
        }
        
        String fileSystemName = filesRootDirectory + "/temp/" + fileSystemId;
        String imageThumbFileName = ImageThumbConverter.generateImageThumb(fileSystemName);
        if (imageThumbFileName != null) {
            InputStream in;

            try {
                in = new FileInputStream(imageThumbFileName);
            } catch (Exception ex) {
                // We don't particularly care what the reason why we have
                // failed to access the file was.
                // From the point of view of the download subsystem, it's a
                // binary operation -- it's either successfull or not.
                // If we can't access it for whatever reason, we are saying
                // it's 404 NOT FOUND in our HTTP response.
                return null;
            }
            return in;
        }

        return null; 
    }
    
    @Path("preview/{fileId}")
    @GET
    @Produces({ "image/png" })
    public InputStream preview(@PathParam("fileId") Long fileId, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response) /*throws NotFoundException, ServiceUnavailableException, PermissionDeniedException, AuthorizationRequiredException*/ {        
        
        
        
        DataFile df = dataFileService.find(fileId);
        String imageThumbFileName = null; 
        if (df != null && df.isImage()) {
            imageThumbFileName = ImageThumbConverter.generateImageThumb(df.getFileSystemLocation().toString(), 48);
        } else {
            imageThumbFileName = getWebappImageResource (DEFAULT_FILE_ICON);
        }
        
        if (imageThumbFileName != null) {
            InputStream in;

            try {
                in = new FileInputStream(imageThumbFileName);
            } catch (Exception ex) {
                // We don't particularly care what the reason why we have
                // failed to access the file was.
                // From the point of view of the download subsystem, it's a
                // binary operation -- it's either successfull or not.
                // If we can't access it for whatever reason, we are saying
                // it's 404 NOT FOUND in our HTTP response.
                return null;
            }
            return in;
        }

        return null; 
    }
    
    @Path("dsPreview/{datasetId}")
    @GET
    @Produces({ "image/png" })
    public InputStream dsPreview(@PathParam("datasetId") Long datasetId, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response) /*throws NotFoundException, ServiceUnavailableException, PermissionDeniedException, AuthorizationRequiredException*/ {        
        
        
        
        Dataset dataset = datasetService.find(datasetId);
        String imageThumbFileName = null; 
        
        List<DataFile> dataFiles = dataset.getFiles();
        for (DataFile dataFile : dataFiles) {
            if (dataFile.isImage()) {
                imageThumbFileName = ImageThumbConverter.generateImageThumb(dataFile.getFileSystemLocation().toString(), 48);
                break;
            } 
        }
        
        if (imageThumbFileName == null) {
            imageThumbFileName = getWebappImageResource (DEFAULT_DATASET_ICON);
        }
        
        if (imageThumbFileName != null) {
            InputStream in;

            try {
                in = new FileInputStream(imageThumbFileName);
            } catch (Exception ex) {
                // We don't particularly care what the reason why we have
                // failed to access the file was.
                // From the point of view of the download subsystem, it's a
                // binary operation -- it's either successfull or not.
                // If we can't access it for whatever reason, we are saying
                // it's 404 NOT FOUND in our HTTP response.
                return null;
            }
            return in;
        }

        return null; 
    }
    
    private String getWebappImageResource(String imageName) {
        String imageFilePath = null;
        String persistenceFilePath = null;
        java.net.URL persistenceFileUrl = Thread.currentThread().getContextClassLoader().getResource("META-INF/persistence.xml");
        
        if (persistenceFileUrl != null) {
            persistenceFilePath = persistenceFileUrl.getFile();
            if (persistenceFilePath != null) {
                persistenceFilePath = persistenceFilePath.replaceFirst("/[^/]*$", "/");
                imageFilePath = persistenceFilePath + "../../../resources/images/" + imageName;
                return imageFilePath; 
            }
            logger.warning("Null file path representation of the location of persistence.xml in the webapp root directory!"); 
        } else {
            logger.warning("Could not find the location of persistence.xml in the webapp root directory!");
        }

        return null;
    }
}