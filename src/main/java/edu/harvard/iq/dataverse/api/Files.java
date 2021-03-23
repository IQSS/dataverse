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
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import static edu.harvard.iq.dataverse.api.AbstractApiBean.error;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.datasetutility.AddReplaceFileHelper;
import edu.harvard.iq.dataverse.datasetutility.DataFileTagException;
import edu.harvard.iq.dataverse.datasetutility.NoFilesException;
import edu.harvard.iq.dataverse.datasetutility.OptionalFileParams;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.GetDataFileCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetDraftFileMetadataIfAvailableCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RedetectFileTypeCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RestrictFileCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UningestFileCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.export.ExportException;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.ingest.IngestRequest;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.ingest.IngestUtil;
import edu.harvard.iq.dataverse.makedatacount.MakeDataCountLoggingServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonReader;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import javax.ws.rs.core.UriInfo;
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
    @Path("{id}/restrict")
    public Response restrictFileInDataset(@PathParam("id") String fileToRestrictId, String restrictStr) {
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
  
        try {
            dataverseRequest = createDataverseRequest(findUserOrDie());
        } catch (WrappedResponse wr) {
            return error(BAD_REQUEST, "Couldn't find user to execute command: " + wr.getLocalizedMessage());
        }

        // try to restrict the datafile
        try {
            engineSvc.submit(new RestrictFileCommand(dataFile, dataverseRequest, restrict));
        } catch (CommandException ex) {
            return error(BAD_REQUEST, "Problem trying to update restriction status on " + dataFile.getDisplayName() + ": " + ex.getLocalizedMessage());
        }

        // update the dataset
        try {
            engineSvc.submit(new UpdateDatasetVersionCommand(dataFile.getOwner(), dataverseRequest));
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
    @Path("{id}/replace")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response replaceFileInDataset(
                    @PathParam("id") String fileIdOrPersistentId,
                    @FormDataParam("jsonData") String jsonData,
                    @FormDataParam("file") InputStream testFileInputStream,
                    @FormDataParam("file") FormDataContentDisposition contentDispositionHeader,
                    @FormDataParam("file") final FormDataBodyPart formDataBodyPart
                    ){

        if (!systemConfig.isHTTPUpload()) {
            return error(Response.Status.SERVICE_UNAVAILABLE, BundleUtil.getStringFromBundle("file.api.httpDisabled"));
        }
        // (1) Get the user from the API key
        User authUser;
        try {
            authUser = findUserOrDie();
        } catch (AbstractApiBean.WrappedResponse ex) {
            return error(Response.Status.FORBIDDEN, 
                    BundleUtil.getStringFromBundle("file.addreplace.error.auth")
                    );
        }

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
        if(null == contentDispositionHeader) {
             return error(BAD_REQUEST, "You must upload a file.");
        }
        String newFilename = contentDispositionHeader.getFileName();
        String newFileContentType = formDataBodyPart.getMediaType().toString();
        
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
                return error(Response.Status.SERVICE_UNAVAILABLE, BundleUtil.getStringFromBundle("file.api.alreadyHasPackageFile"));
            }
        } catch (WrappedResponse ex) {
            String error = BundleUtil.getStringFromBundle("file.addreplace.error.existing_file_to_replace_not_found_by_id", Arrays.asList(fileIdOrPersistentId));
            // TODO: Some day, return ex.getResponse() instead. Also run FilesIT and updated expected status code and message.
            return error(BAD_REQUEST, error);
        }
        if (forceReplace){
            addFileHelper.runForceReplaceFile(fileToReplaceId,
                                    newFilename,
                                    newFileContentType,
                                    testFileInputStream,
                                    optionalFileParams);
        }else{
            addFileHelper.runReplaceFile(fileToReplaceId,
                                    newFilename,
                                    newFileContentType,
                                    testFileInputStream,
                                    optionalFileParams);            
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
    
    //Much of this code is taken from the replace command, 
    //simplified as we aren't actually switching files
    @POST
    @Path("{id}/metadata")
    public Response updateFileMetadata(@FormDataParam("jsonData") String jsonData,
                    @PathParam("id") String fileIdOrPersistentId
        ) throws DataFileTagException, CommandException {
        
        FileMetadata upFmd = null;
        
        try {
            DataverseRequest req;
            try {
                req = createDataverseRequest(findUserOrDie());
            } catch (Exception e) {
                return error(BAD_REQUEST, "Error attempting to request information. Maybe a bad API token?");
            }
            final DataFile df;
            try {
                df = execCommand(new GetDataFileCommand(req, findDataFileOrDie(fileIdOrPersistentId)));
            } catch (Exception e) {
                return error(BAD_REQUEST, "Error attempting get the requested data file.");
            }

            
            User authUser = findUserOrDie();

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
                DatasetVersion editVersion = df.getOwner().getEditVersion();

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

                JsonReader jsonReader = Json.createReader(new StringReader(jsonData));
                javax.json.JsonObject jsonObject = jsonReader.readObject();
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
    @Path("{id}/metadata")
    public Response getFileMetadata(@PathParam("id") String fileIdOrPersistentId, @PathParam("versionId") String versionId, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response, Boolean getDraft) throws WrappedResponse, Exception {
            DataverseRequest req;
            try {
                req = createDataverseRequest(findUserOrDie());
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
                    return error(BAD_REQUEST, "No draft availabile for this dataset");
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
    @Path("{id}/metadata/draft")
    public Response getFileMetadataDraft(@PathParam("id") String fileIdOrPersistentId, @PathParam("versionId") String versionId, @Context UriInfo uriInfo, @Context HttpHeaders headers, @Context HttpServletResponse response, Boolean getDraft) throws WrappedResponse, Exception {
        return getFileMetadata(fileIdOrPersistentId, versionId, uriInfo, headers, response, true);
    }

    @Path("{id}/uningest")
    @POST
    public Response uningestDatafile(@PathParam("id") String id) {

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
            DataverseRequest req = createDataverseRequest(findUserOrDie());
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
    
    @Path("{id}/reingest")
    @POST
    public Response reingest(@PathParam("id") String id) {

        AuthenticatedUser u;
        try {
            u = findAuthenticatedUserOrDie();
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
        String status = ingestService.startIngestJobs(new ArrayList<>(Arrays.asList(dataFile)), u);
        
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

    @Path("{id}/redetect")
    @POST
    public Response redetectDatafile(@PathParam("id") String id, @QueryParam("dryRun") boolean dryRun) {
        try {
            DataFile dataFileIn = findDataFileOrDie(id);
            String originalContentType = dataFileIn.getContentType();
            DataFile dataFileOut = execCommand(new RedetectFileTypeCommand(createDataverseRequest(findUserOrDie()), dataFileIn, dryRun));
            NullSafeJsonBuilder result = NullSafeJsonBuilder.jsonObjectBuilder()
                    .add("dryRun", dryRun)
                    .add("oldContentType", originalContentType)
                    .add("newContentType", dataFileOut.getContentType());
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

}
