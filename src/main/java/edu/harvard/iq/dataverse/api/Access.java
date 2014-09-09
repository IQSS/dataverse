/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.dataaccess.OptionalAccessService;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;

import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;


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
    DatasetVersionServiceBean versionService;

    //@EJB
    
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
        if (df == null) {
            logger.warning("Access: datafile service could not locate a DataFile object for id "+fileId+"!");
            return null; 
        }
        
        
        DownloadInfo dInfo = new DownloadInfo(df);

        /*
         * The only "optional access services" supported as of now (4.0alpha1)
         * are image thumbnail generation and "saved original": 
         * (and yes, this is a hack)
         * TODO: un-hack this. -- L.A. 4.0 alpha 1
         */
        if (df.getContentType() != null && (df.getContentType().startsWith("image/") || df.getContentType().equalsIgnoreCase("application/pdf"))) {
            dInfo.addServiceAvailable(new OptionalAccessService("thumbnail", "image/png", "imageThumb=true", "Image Thumbnail (64x64)"));
        }

        if (df.isTabularData()) {
            String originalMimeType = df.getDataTable().getOriginalFileFormat();
            dInfo.addServiceAvailable(new OptionalAccessService("original", originalMimeType, "format=original","Saved original (" + originalMimeType + ")"));
            
            dInfo.addServiceAvailable(new OptionalAccessService("R", "application/x-rlang-transport", "format=RData", "Data in R format"));
            dInfo.addServiceAvailable(new OptionalAccessService("preprocessed", "application/json", "format=prep", "Preprocessed data in JSON"));
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
    @Produces({"image/png"})
    public InputStream imagethumb(@PathParam("fileSystemId") String fileSystemId, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response) /*throws NotFoundException, ServiceUnavailableException, PermissionDeniedException, AuthorizationRequiredException*/ {
        
        String filesRootDirectory = System.getProperty("dataverse.files.directory");
        if (filesRootDirectory == null || filesRootDirectory.equals("")) {
            filesRootDirectory = "/tmp/files";
        }

        String fileSystemName = filesRootDirectory + "/temp/" + fileSystemId;
        
        String mimeTypeParam = uriInfo.getQueryParameters().getFirst("mimetype");
        String imageThumbFileName = null;
        
        if ("application/pdf".equals(mimeTypeParam)) {
            imageThumbFileName = ImageThumbConverter.generatePDFThumb(fileSystemName);
        } else {
            imageThumbFileName = ImageThumbConverter.generateImageThumb(fileSystemName);
        }
        
        // TODO: 
        // double-check that this temporary preview thumbnail gets deleted 
        // once the file is saved "for real". 
        // (or maybe we shouldn't delete it - but instead move it into the 
        // permanent location... so that it doesn't have to be generated again?)
        // -- L.A. Aug. 21 2014
        
        if (imageThumbFileName == null) {
            imageThumbFileName = getWebappImageResource(DEFAULT_FILE_ICON);
        }

        InputStream in;

        try {
            in = new FileInputStream(imageThumbFileName);
        } catch (Exception ex) {

            return null;
        }
        return in;

    }
    
    
    
    @Path("preview/{fileId}")
    @GET
    @Produces({ "image/png" })
    public InputStream preview(@PathParam("fileId") Long fileId, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response) /*throws NotFoundException, ServiceUnavailableException, PermissionDeniedException, AuthorizationRequiredException*/ {        
        
        
        
        DataFile df = dataFileService.find(fileId);
        
        if (df == null) {
            logger.warning("Preview: datafile service could not locate a DataFile object for id "+fileId+"!");
            return null; 
        }
        
        String imageThumbFileName = null; 
        if (df != null && ("application/pdf".equalsIgnoreCase(df.getContentType()))) {
            imageThumbFileName = ImageThumbConverter.generatePDFThumb(df.getFileSystemLocation().toString(), 48);
        } else if (df != null && df.isImage()) {
            imageThumbFileName = ImageThumbConverter.generateImageThumb(df.getFileSystemLocation().toString(), 48);
        } else {
            imageThumbFileName = getWebappImageResource (DEFAULT_FILE_ICON);
        }
        
        if (imageThumbFileName != null) {
            InputStream in;

            try {
                in = new FileInputStream(imageThumbFileName);
            } catch (Exception ex) {
                return null;
            }
            return in;
        }

        return null; 
    }
    
    @Path("dsPreview/{versionId}")
    @GET
    @Produces({ "image/png" })
    public InputStream dsPreview(@PathParam("versionId") Long versionId, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response) /*throws NotFoundException, ServiceUnavailableException, PermissionDeniedException, AuthorizationRequiredException*/ {        
        
        
        /*
         first version of this API verb was operating on the dataset id, not
         version id: 
        Dataset dataset = datasetService.find(datasetId);
        
        if (dataset == null) {
            logger.warning("Preview: dataset service could not locate a Dataset object for id "+datasetId+"!");
            return null; 
        }
        */
        
        DatasetVersion datasetVersion = versionService.find(versionId);
        
        if (datasetVersion == null) {
            logger.warning("Preview: Version service could not locate a DatasetVersion object for id "+versionId+"!");
            return null; 
        }
        
        String imageThumbFileName = null; 
                
        List<FileMetadata> fileMetadatas = datasetVersion.getFileMetadatas();
            
        for (FileMetadata fileMetadata : fileMetadatas) {
            DataFile dataFile = fileMetadata.getDataFile();
            if ("application/pdf".equalsIgnoreCase(dataFile.getContentType())) {
                imageThumbFileName = ImageThumbConverter.generatePDFThumb(dataFile.getFileSystemLocation().toString(), 48);
                break; 
            } else if (dataFile.isImage()) {
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