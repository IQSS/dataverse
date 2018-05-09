package edu.harvard.iq.dataverse.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import static edu.harvard.iq.dataverse.api.AbstractApiBean.error;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.datasetutility.AddReplaceFileHelper;
import edu.harvard.iq.dataverse.datasetutility.DataFileTagException;
import edu.harvard.iq.dataverse.datasetutility.NoFilesException;
import edu.harvard.iq.dataverse.datasetutility.OptionalFileParams;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteMapLayerMetadataCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RestrictFileCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UningestFileCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.InputStream;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 *
 * @author rmp553
 */
@Path("files")
public class Files extends AbstractApiBean {
    
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DataFileServiceBean fileService;
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
    public Response restrictFileInDataset(
                           @PathParam("id") Long fileToRestrictId,
                           String restrictStr
                           ){
        //create request
        DataverseRequest dataverseRequest = null;
        //get the datafile
        DataFile dataFile = fileService.find(fileToRestrictId);
        if (dataFile == null) {
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
            engineSvc.submit(new UpdateDatasetCommand(dataFile.getOwner(), dataverseRequest));
        } catch (CommandException ex) {
            return error(BAD_REQUEST, "Problem saving datafile " + dataFile.getDisplayName() + ": " + ex.getLocalizedMessage());
        }

        String text =  restrict ? "restricted." : "unrestricted.";
        return ok("File " + dataFile.getDisplayName() + " " + text);
    }
        
    
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
                    @PathParam("id") Long fileToReplaceId,
                    @FormDataParam("jsonData") String jsonData,
                    @FormDataParam("file") InputStream testFileInputStream,
                    @FormDataParam("file") FormDataContentDisposition contentDispositionHeader,
                    @FormDataParam("file") final FormDataBodyPart formDataBodyPart
                    ){
        
        // -------------------------------------
        // (1) Get the user from the API key
        // -------------------------------------
        User authUser;
        try {
            authUser = this.findUserOrDie();
        } catch (AbstractApiBean.WrappedResponse ex) {
            return error(Response.Status.FORBIDDEN, 
                    ResourceBundle.getBundle("Bundle").getString("file.addreplace.error.auth")
                    );
        }

        // -------------------------------------
        // (2) Check/Parse the JSON (if uploaded)
        // -------------------------------------        
        Boolean forceReplace = false;
        OptionalFileParams optionalFileParams = null;
        if (jsonData != null) {
            JsonObject jsonObj = null;
            try {
                jsonObj = new Gson().fromJson(jsonData, JsonObject.class);
                // (2a) Check for optional "forceReplace"
                // -------------------------------------
                if ((jsonObj.has("forceReplace")) && (!jsonObj.get("forceReplace").isJsonNull())) {
                    forceReplace = jsonObj.get("forceReplace").getAsBoolean();
                    if (forceReplace == null) {
                        forceReplace = false;
                    }
                }
                try {
                    // (2b) Load up optional params via JSON
                    //  - Will skip extra attributes which includes fileToReplaceId and forceReplace
                    //---------------------------------------
                    optionalFileParams = new OptionalFileParams(jsonData);
                } catch (DataFileTagException ex) {
                    return error(Response.Status.BAD_REQUEST, ex.getMessage());
                }
            } catch (ClassCastException ex) {
                logger.info("Exception parsing string '" + jsonData + "': " + ex);
            }
        }

        // -------------------------------------
        // (3) Get the file name and content type
        // -------------------------------------
        String newFilename = contentDispositionHeader.getFileName();
        String newFileContentType = formDataBodyPart.getMediaType().toString();
        
        
        //-------------------
        // (4) Create the AddReplaceFileHelper object
        //-------------------
        msg("REPLACE!");

        DataverseRequest dvRequest2 = createDataverseRequest(authUser);
        AddReplaceFileHelper addFileHelper = new AddReplaceFileHelper(dvRequest2,
                                                this.ingestService,
                                                this.datasetService,
                                                this.fileService,
                                                this.permissionSvc,
                                                this.commandEngine,
                                                this.systemConfig);

        //-------------------
        // (5) Run "runReplaceFileByDatasetId"
        //-------------------
        
        
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
            String successMsg = ResourceBundle.getBundle("Bundle").getString("file.addreplace.success.replace");        

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
    
    @DELETE
    @Path("{id}/map")
    public Response getMapLayerMetadatas(@PathParam("id") Long idSupplied) {
        DataverseRequest dataverseRequest = null;
        try {
            dataverseRequest = createDataverseRequest(findUserOrDie());
        } catch (WrappedResponse wr) {
            return error(BAD_REQUEST, "Couldn't find user to execute command: " + wr.getLocalizedMessage());
        }
        DataFile dataFile = fileService.find(idSupplied);
        try {
            boolean deleted = engineSvc.submit(new DeleteMapLayerMetadataCommand(dataverseRequest, dataFile));
            if (deleted) {
                return ok("Map deleted from file id " + dataFile.getId());
            } else {
                return error(BAD_REQUEST, "Could not delete map from file id " + dataFile.getId());
            }
        } catch (CommandException ex) {
            return error(BAD_REQUEST, "Problem trying to delete map from file id " + dataFile.getId() + ": " + ex.getLocalizedMessage());
        }
    }
    
    @Path("{id}/uningest")
    @POST
    public Response uningestDatafile(@PathParam("id") Long idSupplied) {

        DataFile dataFile = fileService.find(idSupplied);

        if (dataFile == null) {
            return error(Response.Status.NOT_FOUND, "File not found for given id.");
        }

        if (!dataFile.isTabularData()) {
            return error(Response.Status.BAD_REQUEST, "Cannot uningest non-tabular file.");
        }

        try {
            DataverseRequest req = createDataverseRequest(findUserOrDie());
            execCommand(new UningestFileCommand(req, dataFile));
            return ok("Datafile " + idSupplied + " uningested.");

        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }

    }

}
