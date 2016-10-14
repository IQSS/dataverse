/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api;

//import com.sun.jersey.core.header.FormDataContentDisposition;
//import com.sun.jersey.multipart.FormDataParam;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetFieldValidator;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import static edu.harvard.iq.dataverse.api.AbstractApiBean.errorResponse;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.datasetutility.AddReplaceFileHelper;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Iterator;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.omnifaces.util.Faces;

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
    
    private static final Logger logger = Logger.getLogger(Files.class.getName());
    
    // for testing
    private static final String SERVER_UPLOAD_LOCATION_FOLDER = "/Users/rmp553/Documents/iqss-git/dataverse-helper-scripts/src/api_scripts/output/";

    
    
    /**
     * get existing test file from this directory:
     *  "scripts/search/data/replace_test/"
     * 
     * @param existingFileName
     * @return 
     */
    
    private InputStream getExistingFileInputStream(String existingFileName){
        if (existingFileName == null){
            return null;
        }
        InputStream inputStream = null;

        //System.out.println("Current path: " + Paths.get(".").toAbsolutePath().normalize().toString());
        String pathToFileName = "(some path)/scripts/search/data/replace_test/" + existingFileName;
        
        try {
            inputStream = new FileInputStream(pathToFileName);
            //is.close(); 
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }

        return inputStream;
    }
    
    private InputStream getSampleFile(){
        
        InputStream inputStream = null;
        String testFileInputStreamName = "/Users/rmp553/Documents/iqss-git/dataverse-helper-scripts/src/api_scripts/input/howdy3.txt";
        //testFileInputStreamName = "/Users/rmp553/NetBeansProjects/dataverse/src/main/java/edu/harvard/iq/dataverse/datasetutility/howdy.txt";
        try {
            inputStream = new FileInputStream(testFileInputStreamName);
            //is.close(); 
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }

        return inputStream;

    }
    
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
                    @FormDataParam("file") InputStream testFileInputStream,
                    @FormDataParam("file") FormDataContentDisposition contentDispositionHeader,
                    @FormDataParam("file") final FormDataBodyPart formDataBodyPart,
                    @FormDataParam("forceReplace") Boolean forceReplace            
                    ){
        
        if (forceReplace==null){
            forceReplace = false;
        }
        
        // -------------------------------------
        // (1) Get the file name and content type
        // -------------------------------------
        String newFilename = contentDispositionHeader.getFileName();
        String newFileContentType = formDataBodyPart.getMediaType().toString();
        
        // -------------------------------------
        // (2) Get the user from the API key
        // -------------------------------------
        User authUser;
        try {
            authUser = this.findUserOrDie();
        } catch (AbstractApiBean.WrappedResponse ex) {
            return errorResponse(Response.Status.FORBIDDEN, "Couldn't find a user from the API key");
        }
        
        //-------------------
        // (3) Create the AddReplaceFileHelper object
        //-------------------
        msg("REPLACE!");

        DataverseRequest dvRequest2 = createDataverseRequest(authUser);
        AddReplaceFileHelper addFileHelper = new AddReplaceFileHelper(dvRequest2,
                                                this.ingestService,
                                                this.datasetService,
                                                this.fileService,
                                                this.permissionSvc,
                                                this.commandEngine);


        //-------------------
        // (4) Run "runReplaceFileByDatasetId"
        //-------------------
        
        
        if (forceReplace){
            addFileHelper.runForceReplaceFile(fileToReplaceId,
                                    newFilename,
                                    newFileContentType,
                                    testFileInputStream);
        }else{
            addFileHelper.runReplaceFile(fileToReplaceId,
                                    newFilename,
                                    newFileContentType,
                                    testFileInputStream);            
        }    
            
        msg("we're back.....");
        if (addFileHelper.hasError()){
            msg("yes, has error");
            return errorResponse(Response.Status.BAD_REQUEST, addFileHelper.getErrorMessagesAsString("\n"));
        }else{
            msg("no error");
         
            return okResponseGsonObject("File successfully replaced!",
                    addFileHelper.getSuccessResultAsGsonObject());
            //"Look at that!  You added a file! (hey hey, it may have worked)");
        }
            
    } // end: replaceFileInDataset



}


