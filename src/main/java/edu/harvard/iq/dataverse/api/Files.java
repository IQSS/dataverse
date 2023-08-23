package edu.harvard.iq.dataverse.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetLock;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.TermsOfUseAndAccessValidator;
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.datasetutility.AddReplaceFileHelper;
import edu.harvard.iq.dataverse.datasetutility.DataFileTagException;
import edu.harvard.iq.dataverse.datasetutility.NoFilesException;
import edu.harvard.iq.dataverse.datasetutility.OptionalFileParams;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.impl.GetDataFileCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetDraftFileMetadataIfAvailableCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RedetectFileTypeCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RestrictFileCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UningestFileCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.export.ExportService;
import io.gdcc.spi.export.ExportException;
import edu.harvard.iq.dataverse.externaltools.ExternalTool;
import edu.harvard.iq.dataverse.externaltools.ExternalToolHandler;
import edu.harvard.iq.dataverse.ingest.IngestRequest;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.ingest.IngestUtil;
import edu.harvard.iq.dataverse.makedatacount.MakeDataCountLoggingServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import jakarta.ws.rs.core.UriInfo;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Path("files")
public class Files extends AbstractApiBean {
    
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DatasetVersionServiceBean datasetVersionService;
    @EJB
    DataverseServiceBean dataverseService;    
    @EJB
    IngestServiceBean ingestService;
    @Inject
    DataverseRequestServiceBean dvRequestService;
    @EJB
    EjbDataverseEngine commandEngine;
    @EJB
    UserNotificationServiceBean userNotificationService;
    @EJB
    SystemConfig systemConfig;
    @EJB
    SettingsServiceBean settingsService;
    @Inject
    MakeDataCountLoggingServiceBean mdcLogService;
    
    private static final Logger logger = Logger.getLogger(Files.class.getName());
    
    
    
    private void msg(String m){
        System.out.println(m);
    }
    private void dashes(){
        msg("----------------");
    }
    private void msgt(String m){
        dashes(); msg(m); dashes();
    }
    
    /**
     * Restrict or Unrestrict an Existing File
     * @author sarahferry
     * 
     * @param fileToRestrictId
     * @param restrictStr
     * @return
     */
    @PUT
    @AuthRequired
    @Path("{id}/restrict")
    public Response restrictFileInDataset(@Context ContainerRequestContext crc, @PathParam("id") String fileToRestrictId, String restrictStr) {
        //create request
        DataverseRequest dataverseRequest = null;
        //get the datafile
        DataFile dataFile;
        try {
            dataFile = findDataFileOrDie(fileToRestrictId);
        } catch (WrappedResponse ex) {
            return error(BAD_REQUEST, "Could not find datafile with id " + fileToRestrictId);
        }

        boolean restrict = Boolean.valueOf(restrictStr);

        dataverseRequest = createDataverseRequest(getRequestUser(crc));

        // try to restrict the datafile
        try {
            engineSvc.submit(new RestrictFileCommand(dataFile, dataverseRequest, restrict));
        } catch (CommandException ex) {
            return error(BAD_REQUEST, "Problem trying to update restriction status on " + dataFile.getDisplayName() + ": " + ex.getLocalizedMessage());
        }

        // update the dataset
        try {
            engineSvc.submit(new UpdateDatasetVersionCommand(dataFile.getOwner(), dataverseRequest));
        } catch (IllegalCommandException ex) {
            //special case where terms of use are out of compliance   
            if (!TermsOfUseAndAccessValidator.isTOUAValid(dataFile.getOwner().getLatestVersion().getTermsOfUseAndAccess(), null)) {
                return conflict(BundleUtil.getStringFromBundle("dataset.message.toua.invalid"));
            }
            return error(BAD_REQUEST, "Problem saving datafile " + dataFile.getDisplayName() + ": " + ex.getLocalizedMessage());
        } catch (CommandException ex) {
            return error(BAD_REQUEST, "Problem saving datafile " + dataFile.getDisplayName() + ": " + ex.getLocalizedMessage());
        }

        String text =  restrict ? "restricted." : "unrestricted.";
        return ok("File " + dataFile.getDisplayName() + " " + text);
    }
        
    
    //TODO: This api would be improved by reporting the new fileId after replace
    
    /**
     * Replace an Existing File 
     * 
     * @param datasetId
     * @param testFileInputStream
     * @param contentDispositionHeader
     * @param formDataBodyPart
     * @return 
     */
    @POST
    @AuthRequired
    @Path("{id}/replace")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response replaceFileInDataset(
                    @Context ContainerRequestContext crc,
                    @PathParam("id") String fileIdOrPersistentId,
                    @FormDataParam("jsonData") String jsonData,
                    @FormDataParam("file") InputStream testFileInputStream,
                    @FormDataParam("file") FormDataContentDisposition contentDispositionHeader,
                    @FormDataParam("file") final FormDataBodyPart formDataBodyPart
                    ){

        if (!systemConfig.isHTTPUpload()) {
            return error(Response.Status.SERVICE_UNAVAILABLE, BundleUtil.getStringFromBundle("file.api.httpDisabled"));
        }
        // (1) Get the user from the ContainerRequestContext
        User authUser = getRequestUser(crc);

        // (2) Check/Parse the JSON (if uploaded)  
        Boolean forceReplace = false;
        OptionalFileParams optionalFileParams = null;
        if (jsonData != null) {
            JsonObject jsonObj = null;
            try {
                jsonObj = new Gson().fromJson(jsonData, JsonObject.class);
                // (2a) Check for optional "forceReplace"
                if ((jsonObj.has("forceReplace")) && (!jsonObj.get("forceReplace").isJsonNull())) {
                    forceReplace = jsonObj.get("forceReplace").getAsBoolean();
                    if (forceReplace == null) {
                        forceReplace = false;
                    }
                }
                try {
                    // (2b) Load up optional params via JSON
                    //  - Will skip extra attributes which includes fileToReplaceId and forceReplace
                    optionalFileParams = new OptionalFileParams(jsonData);
                } catch (DataFileTagException ex) {
                    return error(Response.Status.BAD_REQUEST, ex.getMessage());
                }
            } catch (ClassCastException | com.google.gson.JsonParseException ex) {
                return error(Response.Status.BAD_REQUEST, BundleUtil.getStringFromBundle("file.addreplace.error.parsing"));
            }
        }

        // (3) Get the file name and content type
        String newFilename = null;
        String newFileContentType = null;
        String newStorageIdentifier = null;
        if (null == contentDispositionHeader) {
            if (optionalFileParams.hasStorageIdentifier()) {
                newStorageIdentifier = optionalFileParams.getStorageIdentifier();
                if (optionalFileParams.hasFileName()) {
                    newFilename = optionalFileParams.getFileName();
                    if (optionalFileParams.hasMimetype()) {
                        newFileContentType = optionalFileParams.getMimeType();
                    }
                }
            } else {
                return error(BAD_REQUEST,
                        "You must upload a file or provide a valid storageidentifier, filename, and mimetype.");
            }
        } else {
            newFilename = contentDispositionHeader.getFileName();
            newFileContentType = formDataBodyPart.getMediaType().toString();
        }
        
        // (4) Create the AddReplaceFileHelper object
        msg("REPLACE!");

        DataverseRequest dvRequest2 = createDataverseRequest(authUser);
        AddReplaceFileHelper addFileHelper = new AddReplaceFileHelper(dvRequest2,
                                                this.ingestService,
                                                this.datasetService,
                                                this.fileService,
                                                this.permissionSvc,
                                                this.commandEngine,
                                                this.systemConfig);

        // (5) Run "runReplaceFileByDatasetId"
        long fileToReplaceId = 0;
        try {
            DataFile dataFile = findDataFileOrDie(fileIdOrPersistentId);
            fileToReplaceId = dataFile.getId();

            if (dataFile.isFilePackage()) {
                return error(Response.Status.SERVICE_UNAVAILABLE,
                        BundleUtil.getStringFromBundle("file.api.alreadyHasPackageFile"));
            }

            if (forceReplace) {
                addFileHelper.runForceReplaceFile(fileToReplaceId, newFilename, newFileContentType,
                        newStorageIdentifier, testFileInputStream, dataFile.getOwner(), optionalFileParams);
            } else {
                addFileHelper.runReplaceFile(fileToReplaceId, newFilename, newFileContentType, newStorageIdentifier,
                        testFileInputStream, dataFile.getOwner(), optionalFileParams);
            }
        } catch (WrappedResponse ex) {
            String error = BundleUtil.getStringFromBundle(
                    "file.addreplace.error.existing_file_to_replace_not_found_by_id",
                    Arrays.asList(fileIdOrPersistentId));
            // TODO: Some day, return ex.getResponse() instead. Also run FilesIT and updated
            // expected status code and message.
            return error(BAD_REQUEST, error);
        }
        msg("we're back.....");
        if (addFileHelper.hasError()){
            msg("yes, has error");          
            return error(addFileHelper.getHttpErrorCode(), addFileHelper.getErrorMessagesAsString("\n"));
        
        }else{
            msg("no error");
            String successMsg = BundleUtil.getStringFromBundle("file.addreplace.success.replace");

            try {
                msgt("as String: " + addFileHelper.getSuccessResult());
                /**
                 * @todo We need a consistent, sane way to communicate a human
                 * readable message to an API client suitable for human
                 * consumption. Imagine if the UI were built in Angular or React
                 * and we want to return a message from the API as-is to the
                 * user. Human readable.
                 */
                logger.fine("successMsg: " + successMsg);
                return ok(addFileHelper.getSuccessResultAsJsonObjectBuilder());
                //return okResponseGsonObject(successMsg,
                //        addFileHelper.getSuccessResultAsGsonObject());
                //"Look at that!  You added a file! (hey hey, it may have worked)");
            } catch (NoFilesException ex) {
                Logger.getLogger(Files.class.getName()).log(Level.SEVERE, null, ex);
                return error(Response.Status.BAD_REQUEST, "NoFileException!  Serious Error! See administrator!");

            }
        }
            
    } // end: replaceFileInDataset

    /**
     * Delete an Existing File 
     * 
     * @param id file ID or peristent ID
     */
    @DELETE
    @AuthRequired
    @Path("{id}")
    public Response deleteFileInDataset(@Context ContainerRequestContext crc, @PathParam("id") String fileIdOrPersistentId){
        // (1) Get the user from the API key and create request
        User authUser = getRequestUser(crc);
        DataverseRequest dvRequest = createDataverseRequest(authUser);

        // (2) Delete
        boolean deletePhysicalFile = false;
        try {
            DataFile dataFile = findDataFileOrDie(fileIdOrPersistentId);
            FileMetadata fileToDelete = dataFile.getLatestFileMetadata();
            Dataset dataset = dataFile.getOwner();
            DatasetVersion v = dataset.getOrCreateEditVersion();
            deletePhysicalFile = !dataFile.isReleased();

            UpdateDatasetVersionCommand update_cmd = new UpdateDatasetVersionCommand(dataset, dvRequest,  Arrays.asList(fileToDelete), v);
            update_cmd.setValidateLenient(true);

            try {
                commandEngine.submit(update_cmd);
            } catch (CommandException ex) {
                return error(BAD_REQUEST, "Delete failed for file ID " + fileIdOrPersistentId + " (CommandException): " + ex.getMessage());
            } catch (EJBException ex) {
                return error(BAD_REQUEST, "Delete failed for file ID " + fileIdOrPersistentId + "(EJBException): " + ex.getMessage());
            }
    
            if (deletePhysicalFile) {
                try {
                    fileService.finalizeFileDelete(dataFile.getId(), fileService.getPhysicalFileToDelete(dataFile));
                } catch (IOException ioex) {
                    logger.warning("Failed to delete the physical file associated with the deleted datafile id="
                            + dataFile.getId() + ", storage location: " + fileService.getPhysicalFileToDelete(dataFile));
                }
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }

        return ok(deletePhysicalFile);
    }
    
    //Much of this code is taken from the replace command, 
    //simplified as we aren't actually switching files
    @POST
    @AuthRequired
    @Path("{id}/metadata")
    public Response updateFileMetadata(@Context ContainerRequestContext crc, @FormDataParam("jsonData") String jsonData,
                    @PathParam("id") String fileIdOrPersistentId
        ) throws DataFileTagException, CommandException {
        
        FileMetadata upFmd = null;
        
        try {
            DataverseRequest req;
            try {
                req = createDataverseRequest(getRequestUser(crc));
            } catch (Exception e) {
                return error(BAD_REQUEST, "Error attempting to request information. Maybe a bad API token?");
            }
            final DataFile df;
            try {
                df = execCommand(new GetDataFileCommand(req, findDataFileOrDie(fileIdOrPersistentId)));
            } catch (Exception e) {
                return error(BAD_REQUEST, "Error attempting get the requested data file.");
            }


            //You shouldn't be trying to edit a datafile that has been replaced
            List<Long> result = em.createNamedQuery("DataFile.findDataFileThatReplacedId", Long.class)
            .setParameter("identifier", df.getId())
                    .getResultList();
            //There will be either 0 or 1 returned dataFile Id. If there is 1 this file is replaced and we need to error.
            if(null != result && result.size() > 0) {
                //we get the data file to do a permissions check, if this fails it'll go to the WrappedResponse below for an ugly unpermitted error
                execCommand(new GetDataFileCommand(req, findDataFileOrDie(result.get(0).toString())));

                return error(Response.Status.BAD_REQUEST, "You cannot edit metadata on a dataFile that has been replaced. Please try again with the newest file id.");
            }

            // (2) Check/Parse the JSON (if uploaded)  
            OptionalFileParams optionalFileParams = null;

            if (jsonData != null) {
                JsonObject jsonObj = null;
                try {
                    jsonObj = new Gson().fromJson(jsonData, JsonObject.class);
                    if ((jsonObj.has("restrict")) && (!jsonObj.get("restrict").isJsonNull())) { 
                        Boolean restrict = jsonObj.get("restrict").getAsBoolean();

                        if (restrict != df.getFileMetadata().isRestricted()) {
                            commandEngine.submit(new RestrictFileCommand(df, req, restrict));
                        }
                    }
                    try {
                        // (2b) Load up optional params via JSON
                        //  - Will skip extra attributes which includes fileToReplaceId and forceReplace
                        optionalFileParams = new OptionalFileParams(jsonData);
                    } catch (DataFileTagException ex) {
                        return error(Response.Status.BAD_REQUEST, ex.getMessage());
                    }
                } catch (ClassCastException | com.google.gson.JsonParseException ex) {
                    return error(Response.Status.BAD_REQUEST, BundleUtil.getStringFromBundle("file.addreplace.error.parsing"));
                }
            }

            try {
                DatasetVersion editVersion = df.getOwner().getOrCreateEditVersion();

                //We get the new fileMetadata from the new version
                //This is because after generating the draft with getEditVersion,
                //the updated fileMetadata is not populated to the DataFile object where its easily accessible.
                //Due to this we have to find the FileMetadata inside the DatasetVersion by comparing files info.
                List<FileMetadata> fmdList = editVersion.getFileMetadatas();
                for(FileMetadata testFmd : fmdList) {
                    DataFile daf = testFmd.getDataFile();
                    if(daf.equals(df)){
                       upFmd = testFmd;
                       break;
                    }
                }
                
                if (upFmd == null){
                    return error(Response.Status.BAD_REQUEST, "An error has occurred attempting to update the requested DataFile. It is not part of the current version of the Dataset.");
                }

                jakarta.json.JsonObject jsonObject = JsonUtil.getJsonObject(jsonData);
                String incomingLabel = jsonObject.getString("label", null);
                String incomingDirectoryLabel = jsonObject.getString("directoryLabel", null);
                String existingLabel = df.getFileMetadata().getLabel();
                String existingDirectoryLabel = df.getFileMetadata().getDirectoryLabel();
                String pathPlusFilename = IngestUtil.getPathAndFileNameToCheck(incomingLabel, incomingDirectoryLabel, existingLabel, existingDirectoryLabel);
                // We remove the current file from the list we'll check for duplicates.
                // Instead, the current file is passed in as pathPlusFilename.
                List<FileMetadata> fmdListMinusCurrentFile = new ArrayList<>();
                for (FileMetadata fileMetadata : fmdList) {
                    if (!fileMetadata.equals(df.getFileMetadata())) {
                        fmdListMinusCurrentFile.add(fileMetadata);
                    }
                }
                if (IngestUtil.conflictsWithExistingFilenames(pathPlusFilename, fmdListMinusCurrentFile)) {
                    return error(BAD_REQUEST, BundleUtil.getStringFromBundle("files.api.metadata.update.duplicateFile", Arrays.asList(pathPlusFilename)));
                }

                optionalFileParams.addOptionalParams(upFmd);

                Dataset upDS = execCommand(new UpdateDatasetVersionCommand(upFmd.getDataFile().getOwner(), req));

            } catch (Exception e) {
                logger.log(Level.WARNING, "Dataset publication finalization: exception while exporting:{0}", e);
                return error(Response.Status.INTERNAL_SERVER_ERROR, "Error adding metadata to DataFile: " + e);
            }

        } catch (WrappedResponse wr) {
            return error(Response.Status.BAD_REQUEST, "An error has occurred attempting to update the requested DataFile, likely related to permissions.");
        }

        String jsonString = upFmd.asGsonObject(true).toString();

        return Response
                .status(Response.Status.OK)
                .entity("File Metadata update has been completed: " + jsonString)
                .type(MediaType.TEXT_PLAIN) //Our plain text string is already json
                .build();
    }
    
    @GET
    @AuthRequired
    @Path("{id}/draft")
    public Response getFileDataDraft(@Context ContainerRequestContext crc, @PathParam("id") String fileIdOrPersistentId, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response) throws WrappedResponse, Exception {
        return getFileDataResponse(getRequestUser(crc), fileIdOrPersistentId, uriInfo, headers, response, true);
    }
    
    @GET
    @AuthRequired
    @Path("{id}")
    public Response getFileData(@Context ContainerRequestContext crc, @PathParam("id") String fileIdOrPersistentId, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response) throws WrappedResponse, Exception {
          return getFileDataResponse(getRequestUser(crc), fileIdOrPersistentId, uriInfo, headers, response, false);
    }
    
    private Response getFileDataResponse(User user, String fileIdOrPersistentId, UriInfo uriInfo, HttpHeaders headers, HttpServletResponse response, boolean draft ){
        
        DataverseRequest req;
        try {
            req = createDataverseRequest(user);
        } catch (Exception e) {
            return error(BAD_REQUEST, "Error attempting to request information. Maybe a bad API token?");
        }
        final DataFile df;
        try {
            df = execCommand(new GetDataFileCommand(req, findDataFileOrDie(fileIdOrPersistentId)));
        } catch (Exception e) {
            return error(BAD_REQUEST, "Error attempting get the requested data file.");
        }

        FileMetadata fm;

        if (draft) {
            try {
                fm = execCommand(new GetDraftFileMetadataIfAvailableCommand(req, df));
            } catch (WrappedResponse w) {
                return error(BAD_REQUEST, "An error occurred getting a draft version, you may not have permission to access unpublished data on this dataset.");
            }
            if (null == fm) {
                return error(BAD_REQUEST, BundleUtil.getStringFromBundle("files.api.no.draft"));
            }
        } else {
            //first get latest published
            //if not available get draft if permissible

            try {
                fm = df.getLatestPublishedFileMetadata();
                
            } catch (UnsupportedOperationException e) {
                try {
                    fm = execCommand(new GetDraftFileMetadataIfAvailableCommand(req, df));
                } catch (WrappedResponse w) {
                    return error(BAD_REQUEST, "An error occurred getting a draft version, you may not have permission to access unpublished data on this dataset.");
                }
                if (null == fm) {
                    return error(BAD_REQUEST, BundleUtil.getStringFromBundle("files.api.no.draft"));
                }
            }

        }
        
        if (fm.getDatasetVersion().isReleased()) {
            MakeDataCountLoggingServiceBean.MakeDataCountEntry entry = new MakeDataCountLoggingServiceBean.MakeDataCountEntry(uriInfo, headers, dvRequestService, df);
            mdcLogService.logEntry(entry);
        } 
        
        return Response.ok(Json.createObjectBuilder()
                .add("status", ApiConstants.STATUS_OK)
                .add("data", json(fm)).build())
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
    
    @GET
    @AuthRequired
    @Path("{id}/metadata")
    public Response getFileMetadata(@Context ContainerRequestContext crc, @PathParam("id") String fileIdOrPersistentId, @PathParam("versionId") String versionId, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response, Boolean getDraft) throws WrappedResponse, Exception {
        //ToDo - versionId is not used - can't get metadata for earlier versions
        DataverseRequest req;
            try {
                req = createDataverseRequest(getRequestUser(crc));
            } catch (Exception e) {
                return error(BAD_REQUEST, "Error attempting to request information. Maybe a bad API token?");
            }
            final DataFile df;
            try {
                df = execCommand(new GetDataFileCommand(req, findDataFileOrDie(fileIdOrPersistentId)));
            } catch (Exception e) {
                return error(BAD_REQUEST, "Error attempting get the requested data file.");
            }
            FileMetadata fm;
            
            if(null != getDraft && getDraft) { 
                try {
                    fm = execCommand(new GetDraftFileMetadataIfAvailableCommand(req, findDataFileOrDie(fileIdOrPersistentId)));
                } catch (WrappedResponse w) {
                    return error(BAD_REQUEST, "An error occurred getting a draft version, you may not have permission to access unpublished data on this dataset." );
                }
                if(null == fm) {
                    return error(BAD_REQUEST, BundleUtil.getStringFromBundle("files.api.no.draft"));
                }
            } else {
                fm = df.getLatestPublishedFileMetadata();
                MakeDataCountLoggingServiceBean.MakeDataCountEntry entry = new MakeDataCountLoggingServiceBean.MakeDataCountEntry(uriInfo, headers, dvRequestService, df);
                mdcLogService.logEntry(entry);
            }
            
            String jsonString = fm.asGsonObject(true).toString();
            
            return Response
                .status(Response.Status.OK)
                .entity(jsonString)
                .type(MediaType.TEXT_PLAIN) //Our plain text string is already json
                .build();
    }
    
    @GET
    @AuthRequired
    @Path("{id}/metadata/draft")
    public Response getFileMetadataDraft(@Context ContainerRequestContext crc, @PathParam("id") String fileIdOrPersistentId, @PathParam("versionId") String versionId, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response, Boolean getDraft) throws WrappedResponse, Exception {
        return getFileMetadata(crc, fileIdOrPersistentId, versionId, uriInfo, headers, response, true);
    }

    @POST
    @AuthRequired
    @Path("{id}/uningest")
    public Response uningestDatafile(@Context ContainerRequestContext crc, @PathParam("id") String id) {

        DataFile dataFile;
        try {
            dataFile = findDataFileOrDie(id);
        } catch (WrappedResponse ex) {
            return error(BAD_REQUEST, "Could not find datafile with id " + id);
        }
        if (dataFile == null) {
            return error(Response.Status.NOT_FOUND, "File not found for given id.");
        }

        if (!dataFile.isTabularData()) {
            return error(Response.Status.BAD_REQUEST, "Cannot uningest non-tabular file.");
        }

        try {
            DataverseRequest req = createDataverseRequest(getRequestUser(crc));
            execCommand(new UningestFileCommand(req, dataFile));
            Long dataFileId = dataFile.getId();
            dataFile = fileService.find(dataFileId);
            Dataset theDataset = dataFile.getOwner();
            exportDatasetMetadata(settingsService, theDataset);
            return ok("Datafile " + dataFileId + " uningested.");
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }

    }

    // reingest attempts to queue an *existing* DataFile 
    // for tabular ingest. It can be used on non-tabular datafiles; to try to 
    // ingest a file that has previously failed ingest, or to ingest a file of a
    // type for which ingest was not previously supported. 
    // We are considering making it possible, in the future, to reingest 
    // a datafile that's already ingested as Tabular; for example, to address a 
    // bug that has been found in an ingest plugin.
    @POST
    @AuthRequired
    @Path("{id}/reingest")
    public Response reingest(@Context ContainerRequestContext crc, @PathParam("id") String id) {

        AuthenticatedUser u;
        try {
            u = getRequestAuthenticatedUserOrDie(crc);
            if (!u.isSuperuser()) {
                return error(Response.Status.FORBIDDEN, "This API call can be used by superusers only");
            }
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
        
        DataFile dataFile;
        try {
            dataFile = findDataFileOrDie(id);
        } catch (WrappedResponse ex) {
            return error(Response.Status.NOT_FOUND, "File not found for given id.");
        }

        Dataset dataset = dataFile.getOwner();
        
        if (dataset == null) {
            return error(Response.Status.BAD_REQUEST, "Failed to locate the parent dataset for the datafile.");
        }
        
        if (dataFile.isTabularData()) {
            return error(Response.Status.BAD_REQUEST, "The datafile is already ingested as Tabular.");
        }
        
        boolean ingestLock = dataset.isLockedFor(DatasetLock.Reason.Ingest);
        
        if (ingestLock) {
            return error(Response.Status.FORBIDDEN, "Dataset already locked with an Ingest lock");
        }
        
        if (!FileUtil.canIngestAsTabular(dataFile)) {
            return error(Response.Status.BAD_REQUEST, "Tabular ingest is not supported for this file type (id: "+id+", type: "+dataFile.getContentType()+")");
        }
        
        dataFile.SetIngestScheduled();
                
        if (dataFile.getIngestRequest() == null) {
            dataFile.setIngestRequest(new IngestRequest(dataFile));
        }

        dataFile.getIngestRequest().setForceTypeCheck(true);
        
        // update the datafile, to save the newIngest request in the database:
        dataFile = fileService.save(dataFile);
        
        // queue the data ingest job for asynchronous execution: 
        String status = ingestService.startIngestJobs(dataset.getId(), new ArrayList<>(Arrays.asList(dataFile)), u);
        
        if (!StringUtil.isEmpty(status)) {
            // This most likely indicates some sort of a problem (for example, 
            // the ingest job was not put on the JMS queue because of the size
            // of the file). But we are still returning the OK status - because
            // from the point of view of the API, it's a success - we have 
            // successfully gone through the process of trying to schedule the 
            // ingest job...
            
            return ok(status);
        }
        return ok("Datafile " + id + " queued for ingest");

    }

    @POST
    @AuthRequired
    @Path("{id}/redetect")
    public Response redetectDatafile(@Context ContainerRequestContext crc, @PathParam("id") String id, @QueryParam("dryRun") boolean dryRun) {
        try {
            DataFile dataFileIn = findDataFileOrDie(id);
            String originalContentType = dataFileIn.getContentType();
            DataFile dataFileOut = execCommand(new RedetectFileTypeCommand(createDataverseRequest(getRequestUser(crc)), dataFileIn, dryRun));
            NullSafeJsonBuilder result = NullSafeJsonBuilder.jsonObjectBuilder()
                    .add("dryRun", dryRun)
                    .add("oldContentType", originalContentType)
                    .add("newContentType", dataFileOut.getContentType());
            return ok(result);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @POST
    @AuthRequired
    @Path("{id}/extractNcml")
    public Response extractNcml(@Context ContainerRequestContext crc, @PathParam("id") String id) {
        try {
            AuthenticatedUser au = getRequestAuthenticatedUserOrDie(crc);
            if (!au.isSuperuser()) {
                // We can always make a command in the future if there's a need
                // for non-superusers to call this API.
                return error(Response.Status.FORBIDDEN, "This API call can be used by superusers only");
            }
            DataFile dataFileIn = findDataFileOrDie(id);
            java.nio.file.Path tempLocationPath = null;
            boolean successOrFail = ingestService.extractMetadataNcml(dataFileIn, tempLocationPath);
            NullSafeJsonBuilder result = NullSafeJsonBuilder.jsonObjectBuilder()
                    .add("result", successOrFail);
            return ok(result);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    /**
     * Attempting to run metadata export, for all the formats for which we have
     * metadata Exporters.
     */
    private void exportDatasetMetadata(SettingsServiceBean settingsServiceBean, Dataset theDataset) {

        try {
            ExportService instance = ExportService.getInstance();
            instance.exportAllFormats(theDataset);

        } catch (ExportException ex) {
            // Something went wrong!
            // Just like with indexing, a failure to export is not a fatal
            // condition. We'll just log the error as a warning and keep
            // going:
            logger.log(Level.WARNING, "Dataset publication finalization: exception while exporting:{0}", ex.getMessage());
        }
    }

    // This method provides a callback for an external tool to retrieve it's
    // parameters/api URLs. If the request is authenticated, e.g. by it being
    // signed, the api URLs will be signed. If a guest request is made, the URLs
    // will be plain/unsigned.
    // This supports the cases where a tool is accessing a restricted resource (e.g.
    // preview of a draft file), or public case.
    @GET
    @AuthRequired
    @Path("{id}/metadata/{fmid}/toolparams/{tid}")
    public Response getExternalToolFMParams(@Context ContainerRequestContext crc, @PathParam("tid") long externalToolId,
            @PathParam("id") String fileId, @PathParam("fmid") long fmid, @QueryParam(value = "locale") String locale) {
        ExternalTool externalTool = externalToolService.findById(externalToolId);
        if(externalTool == null) {
            return error(BAD_REQUEST, "External tool not found.");
        }
        if (!ExternalTool.Scope.FILE.equals(externalTool.getScope())) {
            return error(BAD_REQUEST, "External tool does not have file scope.");
        }
        ApiToken apiToken = null;
        User u = getRequestUser(crc);
        if (u instanceof AuthenticatedUser) {
            apiToken = authSvc.findApiTokenByUser((AuthenticatedUser) u);
        }
        FileMetadata target = fileSvc.findFileMetadata(fmid);
        if (target == null) {
            return error(BAD_REQUEST, "FileMetadata not found.");
        }

        ExternalToolHandler eth = null;

        eth = new ExternalToolHandler(externalTool, target.getDataFile(), apiToken, target, locale);
        return ok(eth.createPostBody(eth.getParams(JsonUtil.getJsonObject(externalTool.getToolParameters()))));
    }
    
    @GET
    @Path("fixityAlgorithm")
    public Response getFixityAlgorithm() {
        return ok(systemConfig.getFileFixityChecksumAlgorithm().toString());
    }
}
