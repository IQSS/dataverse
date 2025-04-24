package edu.harvard.iq.dataverse.api;

import com.google.api.client.util.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.datasetutility.AddReplaceFileHelper;
import edu.harvard.iq.dataverse.datasetutility.DataFileTagException;
import edu.harvard.iq.dataverse.datasetutility.NoFilesException;
import edu.harvard.iq.dataverse.datasetutility.OptionalFileParams;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.impl.*;
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
import edu.harvard.iq.dataverse.util.URLTokenUtil;

import static edu.harvard.iq.dataverse.api.ApiConstants.*;
import static edu.harvard.iq.dataverse.api.Datasets.handleVersion;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.inject.Inject;
import jakarta.json.*;
import jakarta.json.stream.JsonParsingException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static edu.harvard.iq.dataverse.util.json.JsonPrinter.jsonDT;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;

import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
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
    @Inject
    GuestbookResponseServiceBean guestbookResponseService;
    @Inject
    DataFileServiceBean dataFileServiceBean;
    @Inject
    FileMetadataVersionsHelper fileMetadataVersionsHelper;

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

        Boolean restrict = null;
        Boolean enableAccessRequest = null;
        String termsOfAccess = null;
        String returnMessage = " ";
        // Backward comparability - allow true/false in string(old) or json(new)
        if (restrictStr != null && restrictStr.trim().startsWith("{")) {
            // process as json
            jakarta.json.JsonObject jsonObject;
            try (StringReader stringReader = new StringReader(restrictStr)) {
                jsonObject = Json.createReader(stringReader).readObject();
                if (jsonObject.containsKey("restrict")) {
                    restrict = Boolean.valueOf(jsonObject.getBoolean("restrict"));
                    returnMessage += restrict ? "restricted." : "unrestricted.";
                } else {
                    return badRequest("Error parsing Json: 'restrict' is required.");
                }
                if (jsonObject.containsKey("enableAccessRequest")) {
                    enableAccessRequest = Boolean.valueOf(jsonObject.getBoolean("enableAccessRequest"));
                    returnMessage += " Access Request is " + (enableAccessRequest ? "enabled." : "disabled.");
                }
                if (jsonObject.containsKey("termsOfAccess")) {
                    termsOfAccess = jsonObject.getString("termsOfAccess");
                    returnMessage += " Terms of Access for restricted files: " + termsOfAccess;
                }
            } catch (JsonParsingException jpe) {
                return badRequest("Error parsing Json: " + jpe.getMessage());
            }
        } else {
            restrict = Boolean.valueOf(restrictStr);
            returnMessage += restrict ? "restricted." : "unrestricted.";
        }

        dataverseRequest = createDataverseRequest(getRequestUser(crc));

        // try to restrict the datafile
        try {
            engineSvc.submit(new RestrictFileCommand(dataFile, dataverseRequest, restrict, enableAccessRequest, termsOfAccess));
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

        return ok("File " + dataFile.getDisplayName() + returnMessage);
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
    @Produces("application/json")
    @Operation(summary = "Replace a file on a dataset", 
               description = "Replace a file to a dataset")
    @APIResponse(responseCode = "200",
               description = "File replaced successfully on the dataset")
    @Tag(name = "replaceFilesInDataset", 
         description = "Replace a file to a dataset")
    @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA)) 
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
                    return error(BAD_REQUEST, ex.getMessage());
                }
            } catch (ClassCastException | com.google.gson.JsonParseException ex) {
                return error(BAD_REQUEST, BundleUtil.getStringFromBundle("file.addreplace.error.parsing"));
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
                return error(BAD_REQUEST, "NoFileException!  Serious Error! See administrator!");

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
            dataset.getOrCreateEditVersion();
            deletePhysicalFile = !dataFile.isReleased();

            UpdateDatasetVersionCommand update_cmd = new UpdateDatasetVersionCommand(dataset, dvRequest,  Arrays.asList(fileToDelete));
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
                    @PathParam("id") String fileIdOrPersistentId, @QueryParam("sourceInternalVersionNumber") Integer sourceInternalVersionNumber
        ) throws CommandException {
        
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

            if (sourceInternalVersionNumber != null) {
                try {
                    validateInternalVersionNumberIsNotOutdated(df, sourceInternalVersionNumber);
                } catch (WrappedResponse wr) {
                    return wr.getResponse();
                }
            }

            //You shouldn't be trying to edit a datafile that has been replaced
            List<Long> result = em.createNamedQuery("DataFile.findDataFileThatReplacedId", Long.class)
            .setParameter("identifier", df.getId())
                    .getResultList();
            //There will be either 0 or 1 returned dataFile Id. If there is 1 this file is replaced and we need to error.
            if(null != result && result.size() > 0) {
                //we get the data file to do a permissions check, if this fails it'll go to the WrappedResponse below for an ugly unpermitted error
                execCommand(new GetDataFileCommand(req, findDataFileOrDie(result.get(0).toString())));

                return error(BAD_REQUEST, "You cannot edit metadata on a dataFile that has been replaced. Please try again with the newest file id.");
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
                        return error(BAD_REQUEST, ex.getMessage());
                    }
                } catch (ClassCastException | com.google.gson.JsonParseException ex) {
                    return error(BAD_REQUEST, BundleUtil.getStringFromBundle("file.addreplace.error.parsing"));
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
                    return error(BAD_REQUEST, "An error has occurred attempting to update the requested DataFile. It is not part of the current version of the Dataset.");
                }

                jakarta.json.JsonObject jsonObject = JsonUtil.getJsonObject(jsonData);
                String incomingLabel = jsonObject.getString("label", null);
                String incomingDirectoryLabel = jsonObject.getString("directoryLabel", null);
                String existingLabel = df.getFileMetadata().getLabel();
                String existingDirectoryLabel = df.getFileMetadata().getDirectoryLabel();
                String pathPlusFilename = IngestUtil.getPathAndFileNameToCheck(incomingLabel, incomingDirectoryLabel, existingLabel, existingDirectoryLabel);
                // We remove the current file from the list we'll check for duplicates.
                // Instead, the current file is passed in as pathPlusFilename.
                // the original test fails for published datasets/new draft because the filemetadata
                // lacks an id for the "equals" test. Changing test to datafile for #11208
                List<FileMetadata> fmdListMinusCurrentFile = new ArrayList<>();
                
                for (FileMetadata fileMetadata : fmdList) {
                    if (!fileMetadata.getDataFile().equals(df)) {
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
            return error(BAD_REQUEST, "An error has occurred attempting to update the requested DataFile, likely related to permissions.");
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
    @Path("{id}")
    public Response getFileData(@Context ContainerRequestContext crc,
                                @PathParam("id") String fileIdOrPersistentId,
                                @QueryParam("includeDeaccessioned") boolean includeDeaccessioned,
                                @QueryParam("returnDatasetVersion") boolean returnDatasetVersion,
                                @QueryParam("returnOwners") boolean returnOwners,
                                @Context UriInfo uriInfo,
                                @Context HttpHeaders headers) {
        return response( req -> getFileDataResponse(req, fileIdOrPersistentId, DS_VERSION_LATEST, includeDeaccessioned, returnDatasetVersion, returnOwners, uriInfo, headers), getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("{id}/versions/{datasetVersionId}")
    public Response getFileDataForVersion(@Context ContainerRequestContext crc,
                                @PathParam("id") String fileIdOrPersistentId,
                                @PathParam("datasetVersionId") String datasetVersionId,
                                @QueryParam("includeDeaccessioned") boolean includeDeaccessioned,
                                @QueryParam("returnDatasetVersion") boolean returnDatasetVersion,
                                @QueryParam("returnOwners") boolean returnOwners,
                                @Context UriInfo uriInfo,
                                @Context HttpHeaders headers) {
        return response( req -> getFileDataResponse(req, fileIdOrPersistentId, datasetVersionId, includeDeaccessioned, returnDatasetVersion, returnOwners, uriInfo, headers), getRequestUser(crc));
    }

    private Response getFileDataResponse(final DataverseRequest req,
                                         String fileIdOrPersistentId,
                                         String datasetVersionId,
                                         boolean includeDeaccessioned,
                                         boolean returnDatasetVersion,
                                         boolean returnOwners,
                                         UriInfo uriInfo,
                                         HttpHeaders headers) throws WrappedResponse {
        final DataFile dataFile = execCommand(new GetDataFileCommand(req, findDataFileOrDie(fileIdOrPersistentId)));
        FileMetadata fileMetadata = execCommand(handleVersion(datasetVersionId, new Datasets.DsVersionHandler<>() {
            @Override
            public Command<FileMetadata> handleLatest() {
                return new GetLatestAccessibleFileMetadataCommand(req, dataFile, includeDeaccessioned);
            }

            @Override
            public Command<FileMetadata> handleDraft() {
                return new GetDraftFileMetadataIfAvailableCommand(req, dataFile);
            }

            @Override
            public Command<FileMetadata> handleSpecific(long major, long minor) {
                return new GetSpecificPublishedFileMetadataByDatasetVersionCommand(req, dataFile, major, minor, includeDeaccessioned);
            }

            @Override
            public Command<FileMetadata> handleLatestPublished() {
                return new GetLatestPublishedFileMetadataCommand(req, dataFile, includeDeaccessioned);
            }
        }));

        if (fileMetadata == null) {
            throw new WrappedResponse(notFound(BundleUtil.getStringFromBundle("files.api.notFoundInVersion", Arrays.asList(fileIdOrPersistentId, datasetVersionId))));
        }

        if (fileMetadata.getDatasetVersion().isReleased()) {
            MakeDataCountLoggingServiceBean.MakeDataCountEntry entry = new MakeDataCountLoggingServiceBean.MakeDataCountEntry(uriInfo, headers, dvRequestService, dataFile);
            mdcLogService.logEntry(entry);
        } 
                    
        return Response.ok(Json.createObjectBuilder()
                .add("status", ApiConstants.STATUS_OK)
                .add("data", json(fileMetadata, returnOwners, returnDatasetVersion)).build())
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
            // Ingest never succeeded, either there was a failure or this is not a tabular
            // data file
            // We allow anyone who can publish to uningest in order to clear a problem
            if (dataFile.isIngestProblem()) {
                try {
                    AuthenticatedUser au = getRequestAuthenticatedUserOrDie(crc);
                    if (!(permissionSvc.permissionsFor(au, dataFile).contains(Permission.PublishDataset))) {
                        return forbidden(
                                "Uningesting to remove an ingest problem can only be done by those who can publish the dataset");
                    }
                } catch (WrappedResponse wr) {
                    return wr.getResponse();
                }
                dataFile.setIngestDone();
                dataFile.setIngestReport(null);
                fileService.save(dataFile);
                return ok("Datafile " + dataFile.getId() + " uningested.");
            } else {
                return error(BAD_REQUEST,
                        BundleUtil.getStringFromBundle("Cannot uningest non-tabular file."));
            }
        } else {
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
                return error(FORBIDDEN, "This API call can be used by superusers only");
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
            return error(BAD_REQUEST, "Failed to locate the parent dataset for the datafile.");
        }
        
        if (dataFile.isTabularData()) {
            return error(BAD_REQUEST, "The datafile is already ingested as Tabular.");
        }
        
        boolean ingestLock = dataset.isLockedFor(DatasetLock.Reason.Ingest);
        
        if (ingestLock) {
            return error(FORBIDDEN, "Dataset already locked with an Ingest lock");
        }
        
        if (!FileUtil.canIngestAsTabular(dataFile)) {
            return error(BAD_REQUEST, "Tabular ingest is not supported for this file type (id: "+id+", type: "+dataFile.getContentType()+")");
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
            // Ingested Files have mimetype = text/tab-separated-values
            // No need to redetect
            if (dataFileIn.isTabularData()) {
                return error(BAD_REQUEST, "The file is an ingested tabular file.");
            }
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
                return error(FORBIDDEN, "This API call can be used by superusers only");
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
        User user = getRequestUser(crc);
        apiToken = authSvc.getValidApiTokenForUser(user);
        FileMetadata target = fileSvc.findFileMetadata(fmid);
        if (target == null) {
            return error(BAD_REQUEST, "FileMetadata not found.");
        }

        URLTokenUtil eth = null;

        eth = new ExternalToolHandler(externalTool, target.getDataFile(), apiToken, target, locale);
        return ok(eth.createPostBody(eth.getParams(JsonUtil.getJsonObject(externalTool.getToolParameters())), JsonUtil.getJsonArray(externalTool.getAllowedApiCalls())));
    }
    
    @GET
    @Path("fixityAlgorithm")
    public Response getFixityAlgorithm() {
        return ok(systemConfig.getFileFixityChecksumAlgorithm().toString());
    }

    @GET
    @AuthRequired
    @Path("{id}/downloadCount")
    public Response getFileDownloadCount(@Context ContainerRequestContext crc, @PathParam("id") String dataFileId) {
        return response(req -> {
            DataFile dataFile = execCommand(new GetDataFileCommand(req, findDataFileOrDie(dataFileId)));
            return ok(guestbookResponseService.getDownloadCountByDataFileId(dataFile.getId()).toString());
        }, getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("{id}/dataTables")
    public Response getFileDataTables(@Context ContainerRequestContext crc, @PathParam("id") String dataFileId) {
        DataFile dataFile;
        try {
            dataFile = findDataFileOrDie(dataFileId);
        } catch (WrappedResponse e) {
            return notFound("File not found for given id.");
        }
        if (dataFile.isRestricted() || FileUtil.isActivelyEmbargoed(dataFile)) {
            DataverseRequest dataverseRequest = createDataverseRequest(getRequestUser(crc));
            boolean hasPermissionToDownloadFile = permissionSvc.requestOn(dataverseRequest, dataFile).has(Permission.DownloadFile);
            if (!hasPermissionToDownloadFile) {
                return forbidden("Insufficient permissions to access the requested information.");
            }
        }
        if (!dataFile.isTabularData()) {
            return badRequest(BundleUtil.getStringFromBundle("files.api.only.tabular.supported"));
        }
        return ok(jsonDT(dataFile.getDataTables()));
    }

    @POST
    @AuthRequired
    @Path("{id}/metadata/categories")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setFileCategories(@Context ContainerRequestContext crc, @PathParam("id") String dataFileId, String jsonBody, @QueryParam("replace") boolean replaceData) {
        return response(req -> {
            DataFile dataFile = execCommand(new GetDataFileCommand(req, findDataFileOrDie(dataFileId)));
            jakarta.json.JsonObject jsonObject;
            try (StringReader stringReader = new StringReader(jsonBody)) {
                jsonObject = Json.createReader(stringReader).readObject();
                JsonArray requestedCategoriesJson = jsonObject.getJsonArray("categories");
                FileMetadata fileMetadata = dataFile.getFileMetadata();
                if (replaceData) {
                    fileMetadata.setCategories(Lists.newArrayList());
                }
                for (JsonValue jsonValue : requestedCategoriesJson) {
                    JsonString jsonString = (JsonString) jsonValue;
                    fileMetadata.addCategoryByName(jsonString.getString());
                }
                execCommand(new UpdateDatasetVersionCommand(fileMetadata.getDataFile().getOwner(), req));
                return ok("Categories of file " + dataFileId + " updated.");
            } catch (JsonParsingException jpe) {
                return badRequest("Error parsing Json: " + jpe.getMessage());
            }
        }, getRequestUser(crc));
    }

    @POST
    @AuthRequired
    @Path("{id}/metadata/tabularTags")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setFileTabularTags(@Context ContainerRequestContext crc, @PathParam("id") String dataFileId, String jsonBody, @QueryParam("replace") boolean replaceData) {
        return response(req -> {
            DataFile dataFile = execCommand(new GetDataFileCommand(req, findDataFileOrDie(dataFileId)));
            if (!dataFile.isTabularData()) {
                return badRequest(BundleUtil.getStringFromBundle("files.api.only.tabular.supported"));
            }
            jakarta.json.JsonObject jsonObject;
            try (StringReader stringReader = new StringReader(jsonBody)) {
                jsonObject = Json.createReader(stringReader).readObject();
                JsonArray requestedTabularTagsJson = jsonObject.getJsonArray("tabularTags");
                if (replaceData) {
                    dataFile.setTags(Lists.newArrayList());
                }
                for (JsonValue jsonValue : requestedTabularTagsJson) {
                    JsonString jsonString = (JsonString) jsonValue;
                    try {
                        dataFile.addUniqueTagByLabel(jsonString.getString());
                    } catch (IllegalArgumentException iax){
                        return badRequest(iax.getMessage());
                    }
                }
                execCommand(new UpdateDatasetVersionCommand(dataFile.getOwner(), req));
                return ok("Tabular tags of file " + dataFileId + " updated.");
            } catch (JsonParsingException jpe) {
                return badRequest("Error parsing Json: " + jpe.getMessage());
            }
        }, getRequestUser(crc));
    }

    @GET
    @AuthRequired
    @Path("{id}/hasBeenDeleted")
    public Response getHasBeenDeleted(@Context ContainerRequestContext crc, @PathParam("id") String dataFileId) {
        return response(req -> {
            DataFile dataFile = execCommand(new GetDataFileCommand(req, findDataFileOrDie(dataFileId)));
            return ok(dataFileServiceBean.hasBeenDeleted(dataFile));
        }, getRequestUser(crc));
    }

    /**
     * @param fileIdOrPersistentId Database ID or PID of the data file.
     * @param versionNumber The version of the dataset, such as 1.0, :draft,
     * :latest-published, etc.
     * @param includeDeaccessioned Defaults to false.
     */
    @GET
    @AuthRequired
    @Path("{id}/versions/{dsVersionString}/citation")
    public Response getFileCitationByVersion(@Context ContainerRequestContext crc, @PathParam("id") String fileIdOrPersistentId, @PathParam("dsVersionString") String versionNumber, @QueryParam("includeDeaccessioned") boolean includeDeaccessioned) {
        try {
            DataverseRequest req = createDataverseRequest(getRequestUser(crc));
            final DataFile df = execCommand(new GetDataFileCommand(req, findDataFileOrDie(fileIdOrPersistentId)));
            Dataset ds = df.getOwner();
            DatasetVersion dsv = findDatasetVersionOrDie(req, versionNumber, ds, includeDeaccessioned, true);
            if (dsv == null) {
                return unauthorized(BundleUtil.getStringFromBundle("files.api.no.draftOrUnauth"));
            }

            Long getDatasetVersionID = dsv.getId();
            FileMetadata fm = dataFileServiceBean.findFileMetadataByDatasetVersionIdAndDataFileId(getDatasetVersionID, df.getId());
            if (fm == null) {
                return notFound(BundleUtil.getStringFromBundle("files.api.fileNotFound"));
            }
            boolean direct = df.isIdentifierRegistered();
            DataCitation citation = new DataCitation(fm, direct);
            return ok(citation.toString(true));
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    @GET
    @AuthRequired
    @Path("{id}/versionDifferences")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFileVersionsList(@Context ContainerRequestContext crc, @PathParam("id") String fileIdOrPersistentId) {
        try {
            DataverseRequest req = createDataverseRequest(getRequestUser(crc));
            final DataFile df = execCommand(new GetDataFileCommand(req, findDataFileOrDie(fileIdOrPersistentId)));
            FileMetadata fm = df.getFileMetadata();
            if (fm == null) {
                return notFound(BundleUtil.getStringFromBundle("files.api.fileNotFound"));
            }
            List<FileMetadata> fileMetadataList = fileMetadataVersionsHelper.loadFileVersionList(req, fm);
            JsonArrayBuilder jab = Json.createArrayBuilder();
            for (FileMetadata fileMetadata : fileMetadataList) {
                jab.add(fileMetadataVersionsHelper.jsonDataFileVersions(fileMetadata).build());
            }
            return Response.ok()
                    .entity(Json.createObjectBuilder()
                            .add("status", STATUS_OK)
                            .add("data", jab.build()).build()
                    ).build();
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
}
