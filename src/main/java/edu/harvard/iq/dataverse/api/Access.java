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
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DataverseTheme;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.dataaccess.OptionalAccessService;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.datavariable.DataVariable;
import edu.harvard.iq.dataverse.datavariable.VariableServiceBean;
import edu.harvard.iq.dataverse.export.DDIExportServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.worldmapauth.WorldMapTokenServiceBean;

import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Properties;
import javax.inject.Inject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;


import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

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
public class Access extends AbstractApiBean {
    private static final Logger logger = Logger.getLogger(Access.class.getCanonicalName());
        
    @EJB
    DataFileServiceBean dataFileService;
    @EJB 
    DatasetServiceBean datasetService; 
    @EJB
    DatasetVersionServiceBean versionService;
    @EJB
    DataverseServiceBean dataverseService; 
    @EJB
    VariableServiceBean variableService;
    @EJB
    SettingsServiceBean settingsService; 
    @EJB
    DDIExportServiceBean ddiExportService;
    @EJB
    PermissionServiceBean permissionService;
    @Inject
    DataverseSession session;
    @EJB
    WorldMapTokenServiceBean worldMapTokenServiceBean;

    //@EJB
    
    // TODO: 
    // versions? -- L.A. 4.0 beta 10
    @Path("datafile/bundle/{fileId}")
    @GET
    @Produces({"application/zip"})
    public BundleDownloadInstance datafileBundle(@PathParam("fileId") Long fileId, @QueryParam("key") String apiToken, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response) /*throws NotFoundException, ServiceUnavailableException, PermissionDeniedException, AuthorizationRequiredException*/ {
 
        DataFile df = dataFileService.find(fileId);
        
        if (df == null) {
            logger.warning("Access: datafile service could not locate a DataFile object for id "+fileId+"!");
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        // This will throw a WebApplicationException, with the correct 
        // exit code, if access isn't authorized: 
        checkAuthorization(df, apiToken);
        
        DownloadInfo dInfo = new DownloadInfo(df);
        BundleDownloadInstance downloadInstance = new BundleDownloadInstance(dInfo);
        
        FileMetadata fileMetadata = df.getFileMetadata();
        DatasetVersion datasetVersion = df.getOwner().getLatestVersion();
        
        downloadInstance.setFileCitationEndNote(datasetService.createCitationXML(datasetVersion, fileMetadata));
        downloadInstance.setFileCitationRIS(datasetService.createCitationRIS(datasetVersion, fileMetadata));
        
        ByteArrayOutputStream outStream = null;
        outStream = new ByteArrayOutputStream();

        try {
            ddiExportService.exportDataFile(
                    fileId,
                    outStream,
                    null,
                    null);

            downloadInstance.setFileDDIXML(outStream.toString());

        } catch (Exception ex) {
            // if we can't generate the DDI, it's ok; 
            // we'll just generate the bundle without it. 
        }
        
        return downloadInstance; 
    }
    
    @Path("datafile/{fileId}")
    @GET
    @Produces({ "application/xml" })
    public DownloadInstance datafile(@PathParam("fileId") Long fileId, @QueryParam("key") String apiToken, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response) /*throws NotFoundException, ServiceUnavailableException, PermissionDeniedException, AuthorizationRequiredException*/ {                

        DataFile df = dataFileService.find(fileId);
        
        if (df == null) {
            logger.warning("Access: datafile service could not locate a DataFile object for id "+fileId+"!");
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        
        // This will throw a WebApplicationException, with the correct 
        // exit code, if access isn't authorized: 
        checkAuthorization(df, apiToken);
        
        DownloadInfo dInfo = new DownloadInfo(df);

        if (dataFileService.thumbnailSupported(df)) {
            dInfo.addServiceAvailable(new OptionalAccessService("thumbnail", "image/png", "imageThumb=true", "Image Thumbnail (64x64)"));
        }

        if (df.isTabularData()) {
            String originalMimeType = df.getDataTable().getOriginalFileFormat();
            dInfo.addServiceAvailable(new OptionalAccessService("original", originalMimeType, "format=original","Saved original (" + originalMimeType + ")"));
            
            dInfo.addServiceAvailable(new OptionalAccessService("R", "application/x-rlang-transport", "format=RData", "Data in R format"));
            dInfo.addServiceAvailable(new OptionalAccessService("preprocessed", "application/json", "format=prep", "Preprocessed data in JSON"));
            dInfo.addServiceAvailable(new OptionalAccessService("subset", "text/tab-separated-values", "variables=&lt;LIST&gt;", "Column-wise Subsetting"));
        }
        DownloadInstance downloadInstance = new DownloadInstance(dInfo);
        
        for (String key : uriInfo.getQueryParameters().keySet()) {
            String value = uriInfo.getQueryParameters().getFirst(key);
            
            if (downloadInstance.isDownloadServiceSupported(key, value)) {
                // this automatically sets the conversion parameters in 
                // the download instance to key and value;
                // TODO: I should probably set these explicitly instead. 
                
                if (downloadInstance.getConversionParam().equals("subset")) {
                    String subsetParam = downloadInstance.getConversionParamValue();
                    String variableIdParams[] = subsetParam.split(",");
                    if (variableIdParams != null && variableIdParams.length > 0) {
                        logger.fine(variableIdParams.length + " tokens;");
                        for (int i = 0; i < variableIdParams.length; i++) {
                            logger.fine("token: " + variableIdParams[i]);
                            String token = variableIdParams[i].replaceFirst("^v", "");
                            Long variableId = null;
                            try {
                                variableId = new Long(token);
                            } catch (NumberFormatException nfe) {
                                variableId = null;
                            }
                            if (variableId != null) {
                                logger.fine("attempting to look up variable id " + variableId);
                                if (variableService != null) {
                                    DataVariable variable = variableService.find(variableId);
                                    if (variable != null) {
                                        if (downloadInstance.getExtraArguments() == null) {
                                            downloadInstance.setExtraArguments(new ArrayList<Object>());
                                        }
                                        logger.fine("putting variable id "+variable.getId()+" on the parameters list of the download instance.");
                                        downloadInstance.getExtraArguments().add(variable);
                                        
                                        //if (!variable.getDataTable().getDataFile().getId().equals(sf.getId())) {
                                        //variableList.add(variable);
                                        //}
                                    }
                                } else {
                                    logger.fine("variable service is null.");
                                }
                            }
                        }
                    }
                }

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
    
    
    /* 
     * Variants of the Access API calls for retrieving datafile-level 
     * Metadata.
    */
    
    
    // Metadata format defaults to DDI:
    @Path("datafile/{fileId}/metadata")
    @GET
    @Produces({"text/xml"})
    public String tabularDatafileMetadata(@PathParam("fileId") Long fileId, @QueryParam("exclude") String exclude, @QueryParam("include") String include, @Context HttpHeaders header, @Context HttpServletResponse response) throws NotFoundException, ServiceUnavailableException /*, PermissionDeniedException, AuthorizationRequiredException*/ { 
        return tabularDatafileMetadataDDI(fileId, exclude, include, header, response);
    }
    
    /* 
     * This has been moved here, under /api/access, from the /api/meta hierarchy
     * which we are going to retire.
     */
    @Path("datafile/{fileId}/metadata/ddi")
    @GET
    @Produces({"text/xml"})
    public String tabularDatafileMetadataDDI(@PathParam("fileId") Long fileId, @QueryParam("exclude") String exclude, @QueryParam("include") String include, @Context HttpHeaders header, @Context HttpServletResponse response) throws NotFoundException, ServiceUnavailableException /*, PermissionDeniedException, AuthorizationRequiredException*/ {
        String retValue = "";

        DataFile dataFile = null; 
        
        //httpHeaders.add("Content-disposition", "attachment; filename=\"dataverse_files.zip\"");
        //httpHeaders.add("Content-Type", "application/zip; name=\"dataverse_files.zip\"");
        response.setHeader("Content-disposition", "attachment; filename=\"dataverse_files.zip\"");
        
        dataFile = dataFileService.find(fileId);
        if (dataFile == null) {
            throw new NotFoundException();
        }
        
        String fileName = dataFile.getFileMetadata().getLabel().replaceAll("\\.tab$", "-ddi.xml");
        response.setHeader("Content-disposition", "attachment; filename=\""+fileName+"\"");
        response.setHeader("Content-Type", "application/xml; name=\""+fileName+"\"");
        
        ByteArrayOutputStream outStream = null;
        outStream = new ByteArrayOutputStream();

        try {
            ddiExportService.exportDataFile(
                    fileId,
                    outStream,
                    exclude,
                    include);

            retValue = outStream.toString();

        } catch (Exception e) {
            // For whatever reason we've failed to generate a partial 
            // metadata record requested. 
            // We return Service Unavailable.
            throw new ServiceUnavailableException();
        }

        response.setHeader("Access-Control-Allow-Origin", "*");

        return retValue;
    }
    
    @Path("variable/{varId}/metadata/ddi")
    @GET
    @Produces({ "application/xml" })

    public String dataVariableMetadataDDI(@PathParam("varId") Long varId, @QueryParam("exclude") String exclude, @QueryParam("include") String include, @Context HttpHeaders header, @Context HttpServletResponse response) /*throws NotFoundException, ServiceUnavailableException, PermissionDeniedException, AuthorizationRequiredException*/ {
        String retValue = "";
        
        ByteArrayOutputStream outStream = null;
        try {
            outStream = new ByteArrayOutputStream();

            ddiExportService.exportDataVariable(
                    varId,
                    outStream,
                    exclude,
                    include);
        } catch (Exception e) {
            // For whatever reason we've failed to generate a partial 
            // metadata record requested. We simply return an empty string.
            return retValue;
        }

        retValue = outStream.toString();
        
        response.setHeader("Access-Control-Allow-Origin", "*");
        
        return retValue; 
    }
    
    /*
     * "Preprocessed data" metadata format:
     * (this was previously provided as a "format conversion" option of the 
     * file download form of the access API call)
     */
    
    @Path("datafile/{fileId}/metadata/preprocessed")
    @GET
    @Produces({"text/xml"})
    
    public DownloadInstance tabularDatafileMetadataPreprocessed(@PathParam("fileId") Long fileId, @QueryParam("key") String apiToken, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response) {
        uriInfo.getQueryParameters().clear();
        uriInfo.getQueryParameters().add("format", "prep");
        
        return datafile(fileId, apiToken, uriInfo, headers, response);
    }
    
    /* 
     * API method for downloading zipped bundles of multiple files:
    */
    
    
    @Path("datafiles/{fileIds}")
    @GET
    @Produces({"application/zip"})
    public ZippedDownloadInstance datafiles(@PathParam("fileIds") String fileIds, @QueryParam("key") String apiToken, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response) throws WebApplicationException /*throws NotFoundException, ServiceUnavailableException, PermissionDeniedException, AuthorizationRequiredException*/ {
        ByteArrayOutputStream outStream = null;
        // create a Download Instance without, without a primary Download Info object:
        ZippedDownloadInstance downloadInstance = new ZippedDownloadInstance();

        if (fileIds == null || fileIds.equals("")) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        String fileIdParams[] = fileIds.split(",");
        if (fileIdParams != null && fileIdParams.length > 0) {
            logger.fine(fileIdParams.length + " tokens;");
            for (int i = 0; i < fileIdParams.length; i++) {
                logger.fine("token: " + fileIdParams[i]);
                Long fileId = null;
                try {
                    fileId = new Long(fileIdParams[i]);
                } catch (NumberFormatException nfe) {
                    fileId = null;
                }
                logger.fine("attempting to look up file id " + fileId);
                DataFile file = dataFileService.find(fileId);
                if (file != null) {
                    if (isAccessAuthorized(file, apiToken)) { 
                        logger.fine("adding datafile (id=" + file.getId() + ") to the download list of the ZippedDownloadInstance.");
                        downloadInstance.addDataFile(file);
                    } else {
                        downloadInstance.setManifest(downloadInstance.getManifest() + 
                                file.getFileMetadata().getLabel() + " IS RESTRICTED AND CANNOT BE DOWNLOADED\r\n");
                    }

                } else {
                    // Or should we just drop it and make a note in the Manifest?    
                    throw new WebApplicationException(Response.Status.NOT_FOUND);
                }
            }
        } else {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        if (downloadInstance.getDataFiles().size() < 1) {
            // This means the file ids supplied were valid, but none were 
            // accessible for this user:
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }
        

        return downloadInstance;
    }
    
    @Path("tempPreview/{fileSystemId}")
    @GET
    @Produces({"image/png"})
    public InputStream tempPreview(@PathParam("fileSystemId") String fileSystemId, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response) /*throws NotFoundException, ServiceUnavailableException, PermissionDeniedException, AuthorizationRequiredException*/ {
        
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
        // Update: 
        // the temporary thumbnail file does get cleaned up now; 
        // but yeay, maybe we should be saving it permanently instead, as 
        // the above suggested...
        // -- L.A. Feb. 28 2015
        
        
        if (imageThumbFileName == null) {
            return null; 
        }
        /* 
         removing the old, non-vector default icon: 
            imageThumbFileName = getWebappImageResource(DEFAULT_FILE_ICON);
        }
        */

        InputStream in;

        try {
            in = new FileInputStream(imageThumbFileName);
        } catch (Exception ex) {

            return null;
        }
        return in;

    }
    
    
    
    @Path("fileCardImage/{fileId}")
    @GET
    @Produces({ "image/png" })
    public InputStream fileCardImage(@PathParam("fileId") Long fileId, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response) /*throws NotFoundException, ServiceUnavailableException, PermissionDeniedException, AuthorizationRequiredException*/ {        
        
        
        
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
        } else if ("application/zipped-shapefile".equalsIgnoreCase(df.getContentType())) {
            imageThumbFileName = ImageThumbConverter.generateWorldMapThumb(df.getFileSystemLocation().toString(), 48);
        } 
        /* 
         * Removing the old, non-vector default icon: 
        else {
            imageThumbFileName = getWebappImageResource (DEFAULT_FILE_ICON);
        }
        */
        
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
    
    @Path("dsCardImage/{versionId}")
    @GET
    @Produces({ "image/png" })
    public InputStream dsCardImage(@PathParam("versionId") Long versionId, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response) /*throws NotFoundException, ServiceUnavailableException, PermissionDeniedException, AuthorizationRequiredException*/ {        
        
        
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
        
        // First, check if this dataset has a designated thumbnail image: 
        
        if (datasetVersion.getDataset() != null) {
            DataFile dataFile = datasetVersion.getDataset().getThumbnailFile();
            if (dataFile != null) {
                if ("application/pdf".equalsIgnoreCase(dataFile.getContentType())) {
                    imageThumbFileName = ImageThumbConverter.generatePDFThumb(dataFile.getFileSystemLocation().toString(), 48);
                } else if (dataFile.isImage()) {
                    imageThumbFileName = ImageThumbConverter.generateImageThumb(dataFile.getFileSystemLocation().toString(), 48);
                } 
            }
        }
        
        // If not, we'll try to use one of the files in this dataset version:
        if (!datasetVersion.getDataset().isHarvested()) {
            if (imageThumbFileName == null) {
                List<FileMetadata> fileMetadatas = datasetVersion.getFileMetadatas();

                for (FileMetadata fileMetadata : fileMetadatas) {
                    DataFile dataFile = fileMetadata.getDataFile();
                    if ("application/pdf".equalsIgnoreCase(dataFile.getContentType())) {
                        imageThumbFileName = ImageThumbConverter.generatePDFThumb(dataFile.getFileSystemLocation().toString(), 48);
                        break;
                    } else if (dataFile.isImage()) {
                        imageThumbFileName = ImageThumbConverter.generateImageThumb(dataFile.getFileSystemLocation().toString(), 48);
                        break;
                    } else if ("application/zipped-shapefile".equalsIgnoreCase(dataFile.getContentType())) {
                        imageThumbFileName = ImageThumbConverter.generateWorldMapThumb(dataFile.getFileSystemLocation().toString(), 48);
                        break;
                    }
                }
            }
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
    
    @Path("dvCardImage/{dataverseId}")
    @GET
    @Produces({ "image/png" })
    public InputStream dvCardImage(@PathParam("dataverseId") Long dataverseId, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response) /*throws NotFoundException, ServiceUnavailableException, PermissionDeniedException, AuthorizationRequiredException*/ {        
        
        
        Dataverse dataverse = dataverseService.find(dataverseId);
        
        if (dataverse == null) {
            logger.warning("Preview: Version service could not locate a DatasetVersion object for id "+dataverseId+"!");
            return null; 
        }
        
        String imageThumbFileName = null; 
        
        // First, check if the dataverse has a defined logo: 
        
        if (dataverse.getDataverseTheme()!=null && dataverse.getDataverseTheme().getLogo() != null && !dataverse.getDataverseTheme().getLogo().equals("")) {
            File dataverseLogoFile = getLogo(dataverse);
            if (dataverseLogoFile != null) {
                String logoThumbNailPath = null;
                InputStream in = null;

                try {
                    if (dataverseLogoFile.exists()) {
                        logoThumbNailPath =  ImageThumbConverter.generateImageThumb(dataverseLogoFile.getAbsolutePath(), 48);
                        if (logoThumbNailPath != null) {
                            in = new FileInputStream(logoThumbNailPath);
                        }
                    }
                } catch (Exception ex) {
                    in = null; 
                }
                if (in != null) {
                    return in;
                }    
            }
        }
        
        // If there's no uploaded logo for this dataverse, go through its 
        // [released] datasets and see if any of them have card images:
        
        // TODO: figure out if we want to be doing this! 
        // (efficiency considerations...) -- L.A. 4.0 
        // And we definitely don't want to be doing this for harvested 
        // dataverses:
        
        if (!dataverse.isHarvested()) {
            for (Dataset dataset : datasetService.findPublishedByOwnerId(dataverseId)) {
                if (dataset != null) {
                    DatasetVersion releasedVersion = dataset.getReleasedVersion();
                // TODO: 
                    // put the Version-related code below away in its own method, 
                    // share it between this and the "dataset card image" method 
                    // above. 
                    // -- L.A. 4.0 beta 8
                    // TODO: 
                    // yeah, this needs to be cleaned up - after 4.0. 
                    // -- L.A. 4.0 beta 11
                    if (releasedVersion != null) {
                        for (FileMetadata fileMetadata : releasedVersion.getFileMetadatas()) {
                            DataFile dataFile = fileMetadata.getDataFile();
                            if ("application/pdf".equalsIgnoreCase(dataFile.getContentType())) {
                                imageThumbFileName = ImageThumbConverter.generatePDFThumb(dataFile.getFileSystemLocation().toString(), 48);
                                break;
                            } else if (dataFile.isImage()) {
                                imageThumbFileName = ImageThumbConverter.generateImageThumb(dataFile.getFileSystemLocation().toString(), 48);
                                break;
                            } else if ("application/zipped-shapefile".equalsIgnoreCase(dataFile.getContentType())) {
                                imageThumbFileName = ImageThumbConverter.generateWorldMapThumb(dataFile.getFileSystemLocation().toString(), 48);
                                break;
                            }
                        }
                    }
                    if (imageThumbFileName != null) {
                        break;
                    }
                }
            }
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
    
    // TODO: 
    // put this method into the dataverseservice; use it there
    // -- L.A. 4.0 beta14
    
    private File getLogo(Dataverse dataverse) {
        if (dataverse.getId() == null) {
            return null; 
        }
        
        DataverseTheme theme = dataverse.getDataverseTheme(); 
        if (theme != null && theme.getLogo() != null && !theme.getLogo().equals("")) {
            Properties p = System.getProperties();
            String domainRoot = p.getProperty("com.sun.aas.instanceRoot");
  
            if (domainRoot != null && !"".equals(domainRoot)) {
                return new File (domainRoot + File.separator + 
                    "docroot" + File.separator + 
                    "logos" + File.separator + 
                    dataverse.getLogoOwnerId() + File.separator + 
                    theme.getLogo());
            }
        }
            
        return null;         
    }
    
    /* 
        removing: 
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
    */
    
    
    // TODO: 
    // duplicated code in the 2 methods below. 
    // -- L.A. 4.0, beta11
    
    private void checkAuthorization(DataFile df, String apiToken) throws WebApplicationException {
        // New as of beta15: 
        // Either a session, or an API token is *always* required. 
        // Even if it's a totally public object. 
        // So, checking for that first:
        logger.info("checking if either a session or a token supplied.");
        if (session == null || session.getUser() == null) { // || !session.getUser().isAuthenticated()) {
            logger.info("session is null, or unauthenticated.");
            if (apiToken == null || findUserByApiToken(apiToken) == null) {
                logger.info("token null or not supplied.");
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            }
        } else {
            logger.info("session not null.");
        }

        // We don't even need to check permissions on files that are 
        // from released Dataset versions and not restricted: 
        
        //logger.info("checking if file is restricted:");
        if (!df.isRestricted()) {
            //logger.info("nope.");
            if (df.getOwner().getReleasedVersion() != null) {
                //logger.info("file belongs to a dataset with a released version.");
                if (df.getOwner().getReleasedVersion().getFileMetadatas() != null) {
                    //logger.info("going through the list of filemetadatas that belong to the released version.");
                    for (FileMetadata fm : df.getOwner().getReleasedVersion().getFileMetadatas()) {
                        if (df.equals(fm.getDataFile())) {
                            //logger.info("found a match!");
                            return;
                        }
                    }
                }
            }
        }
        
        AuthenticatedUser user = null;
       
        /** 
         * Authentication/authorization:
         * 
         * note that the fragment below - that retrieves the session object
         * and tries to find the user associated with the session - is really
         * for logging/debugging purposes only; for practical purposes, it 
         * would be enough to just call "permissionService.on(df).has(Permission.DownloadFile)"
         * and the method does just that, tries to authorize for the user in 
         * the current session (or guest user, if no session user is available):
         */
        
        if (session != null) {
            if (session.getUser() != null) {
                if (session.getUser().isAuthenticated()) {
                    user = (AuthenticatedUser) session.getUser();
                } else {
                    logger.fine("User associated with the session is not an authenticated user. (Guest access will be assumed).");
                    if (session.getUser() instanceof GuestUser) {
                        logger.fine("User associated with the session is indeed a guest user.");
                    }
                }
            } else {
                logger.fine("No user associated with the session.");
            }
        } else {
            logger.fine("Session is null.");
        } 
        
        /**
         * TODO: remove all the auth logging, once the functionality is tested. 
         * -- L.A. 4.0, beta 10
         */
        
        if (permissionService.on(df).has(Permission.DownloadFile)) {
            // Note: PermissionServiceBean.on(Datafile df) will obtain the 
            // User from the Session object, just like in the code fragment 
            // above. That's why it's not passed along as an argument.
            if (user != null) {
                logger.fine("Session-based auth: user "+user.getName()+" has access rights on the requested datafile.");
            } else {
                logger.fine("Session-based auth: guest user is granted access to the datafile.");
            }
        } else if ((apiToken != null)&&(apiToken.length()==64)){
            /* 
                WorldMap token check
                - WorldMap tokens are 64 chars in length
            
                - Use the worldMapTokenServiceBean to verify token 
                    and check permissions against the requested DataFile
            */
            if (!(this.worldMapTokenServiceBean.isWorldMapTokenAuthorizedForDataFileDownload(apiToken, df))){
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }
            
            // Yes! User may access file
            //
            logger.fine("WorldMap token-based auth: Token is valid for the requested datafile");
            
        } else if ((apiToken != null)&&(apiToken.length()!=64)) {
            // Will try to obtain the user information from the API token, 
            // if supplied: 
        
            user = findUserByApiToken(apiToken);
            
            if (user == null) {
                logger.warning("API token-based auth: Unable to find a user with the API token provided.");
                throw new WebApplicationException(Response.Status.FORBIDDEN);
                
            } 
            
            if (!permissionService.userOn(user, df).has(Permission.DownloadFile)) { 
                logger.fine("API token-based auth: User "+user.getName()+" is not authorized to access the datafile.");
                throw new WebApplicationException(Response.Status.FORBIDDEN);
            }
            
            logger.fine("API token-based auth: User "+user.getName()+" has rights to access the datafile.");
        } else {
            logger.fine("Unauthenticated access: No guest access to the datafile.");
            // throwing "authorization required" (401) instead of "access denied" (403) here:
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
    }
    
    
    
    
    private boolean isAccessAuthorized(DataFile df, String apiToken) {
        AuthenticatedUser user = null;
       
        // We don't even need to check permissions on files that are 
        // from released Dataset versions and not restricted: 
        
        //logger.info("checking if file is restricted:");
        if (!df.isRestricted()) {
            //logger.info("nope.");
            if (df.getOwner().getReleasedVersion() != null) {
                //logger.info("file belongs to a dataset with a released version.");
                if (df.getOwner().getReleasedVersion().getFileMetadatas() != null) {
                    //logger.info("going through the list of filemetadatas that belong to the released version.");
                    for (FileMetadata fm : df.getOwner().getReleasedVersion().getFileMetadatas()) {
                        if (df.equals(fm.getDataFile())) {
                            //logger.info("found a match!");
                            return true;
                        }
                    }
                }
            }
        }
        
        if (session != null) {
            if (session.getUser() != null) {
                if (session.getUser().isAuthenticated()) {
                    user = (AuthenticatedUser) session.getUser();
                } else {
                    logger.fine("User associated with the session is not an authenticated user. (Guest access will be assumed).");
                    if (session.getUser() instanceof GuestUser) {
                        logger.fine("User associated with the session is indeed a guest user.");
                    }
                }
            } else {
                logger.fine("No user associated with the session.");
            }
        } else {
            logger.fine("Session is null.");
        } 
        
        /**
         * TODO: remove all the auth logging, once the functionality is tested. 
         * -- L.A. 4.0, beta 10
         */
        
        if (permissionService.on(df).has(Permission.DownloadFile)) {
            // Note: PermissionServiceBean.on(Datafile df) will obtain the 
            // User from the Session object, just like in the code fragment 
            // above. That's why it's not passed along as an argument.
            if (user != null) {
                logger.fine("Session-based auth: user "+user.getName()+" has access rights on the requested datafile.");
            } else {
                logger.fine("Session-based auth: guest user is granted access to the datafile.");
            }
        } else if (apiToken != null) {
            // Will try to obtain the user information from the API token, 
            // if supplied: 
        
            user = findUserByApiToken(apiToken);
            
            if (user == null) {
                logger.warning("API token-based auth: Unable to find a user with the API token provided.");
                return false; 
                
            } 
            
            if (!permissionService.userOn(user, df).has(Permission.DownloadFile)) { 
                logger.fine("API token-based auth: User "+user.getName()+" is not authorized to access the datafile.");
                return false; 
            }
            
            logger.fine("API token-based auth: User "+user.getName()+" has rights to access the datafile.");
        } else {
            logger.fine("Unauthenticated access: No guest access to the datafile.");
            // throwing "authorization required" (401) instead of "access denied" (403) here:
            return false; 
        }
        
        return true;
    }
}