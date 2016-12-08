/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api;

//import com.sun.jersey.core.header.FormDataContentDisposition;
//import com.sun.jersey.multipart.FormDataParam;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.datasetutility.AddReplaceFileHelper;
import edu.harvard.iq.dataverse.datasetutility.DataFileTagException;
import edu.harvard.iq.dataverse.datasetutility.NoFilesException;
import edu.harvard.iq.dataverse.datasetutility.OptionalFileParams;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.InputStream;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
        // (2) Check/Parse the JSON
        // -------------------------------------        
        if (jsonData == null){
            logger.log(Level.SEVERE, "jsonData is null");
            return error( Response.Status.BAD_REQUEST, "No JSON data");
        }
        JsonObject jsonObj = new Gson().fromJson(jsonData, JsonObject.class);

        // (2a) Check for required "fileToReplaceId"
        // -------------------------------------        
        /*if ((!jsonObj.has("fileToReplaceId")) || jsonObj.get("fileToReplaceId").isJsonNull()){
            return error( Response.Status.BAD_REQUEST, "'fileToReplaceId' NOT found in the JSON Request");
        }
        
        Long fileToReplaceId;
        
        try {
            fileToReplaceId = Long.parseLong(jsonObj.get("fileToReplaceId").toString());        
        } catch (Exception e) {
            return error( Response.Status.BAD_REQUEST, "'fileToReplaceId' in the JSON Request must be a number.");            
        }
        */
        
        // (2b) Check for optional "forceReplace"
        // -------------------------------------        
        Boolean forceReplace = false;
        if ((jsonObj.has("forceReplace")) && (!jsonObj.get("forceReplace").isJsonNull())){
            forceReplace = jsonObj.get("forceReplace").getAsBoolean();
            if (forceReplace == null){
                forceReplace = false;
            }
        }
        
        
        // (2d) Load up optional params via JSON
        //  - Will skip extra attributes which includes fileToReplaceId and forceReplace
        //---------------------------------------
        OptionalFileParams optionalFileParams = null;
        try {
            optionalFileParams = new OptionalFileParams(jsonData);
        } catch (DataFileTagException ex) {
            return error( Response.Status.BAD_REQUEST, ex.getMessage());            
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
                return ok(successMsg,
                        addFileHelper.getSuccessResultAsJsonObjectBuilder());
                //return okResponseGsonObject(successMsg,
                //        addFileHelper.getSuccessResultAsGsonObject());
                //"Look at that!  You added a file! (hey hey, it may have worked)");
            } catch (NoFilesException ex) {
                Logger.getLogger(Files.class.getName()).log(Level.SEVERE, null, ex);
                return error(Response.Status.BAD_REQUEST, "NoFileException!  Serious Error! See administrator!");

            }
        }
            
    } // end: replaceFileInDataset



}


