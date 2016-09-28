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
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.datasetutility.AddReplaceFileHelper;
import edu.harvard.iq.dataverse.datasetutility.DuplicateFileChecker;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import org.mindrot.jbcrypt.BCrypt;

/**
 *
 * @author rmp553
 */
@Stateless
@Path("upload")
public class FileUpload extends AbstractApiBean {
    
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
    
    private static final Logger logger = Logger.getLogger(FileUpload.class.getName());
    
    // for testing
    private static final String SERVER_UPLOAD_LOCATION_FOLDER = "/Users/rmp553/Documents/iqss-git/dataverse-helper-scripts/src/api_scripts/output/";

    /*
    @POST
    @Path("hello")  //Your Path or URL to call this service
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(
            @DefaultValue("true") @FormDataParam("enabled") boolean enabled,
            @FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail) {
         //Your local disk path where you want to store the file
        String uploadedFileLocation = SERVER_UPLOAD_LOCATION_FOLDER + fileDetail.getFileName();
        System.out.println(uploadedFileLocation);
        // save it
        File  objFile=new File(uploadedFileLocation);
        if(objFile.exists())
        {
            objFile.delete();

        }

        saveToFile(uploadedInputStream, uploadedFileLocation);

        String userMsg = "File uploaded via Jersey based RESTFul Webservice to: " + uploadedFileLocation;

        return okResponse(userMsg);
    }
    
    private void saveToFile(InputStream uploadedInputStream,
        String uploadedFileLocation) {

        try {
            OutputStream out = null;
            int read = 0;
            byte[] bytes = new byte[1024];

            out = new FileOutputStream(new File(uploadedFileLocation));
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            out.flush();
            out.close();
        } catch (IOException e) {

            e.printStackTrace();
        }
    }
    */
    /*
    @POST
    @Path("hello")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(
                    @FormDataParam("file") InputStream fileInputStream,
                    @FormDataParam("file") FormDataContentDisposition contentDispositionHeader) {

            String filePath = SERVER_UPLOAD_LOCATION_FOLDER + contentDispositionHeader.getFileName();

            // save the file to the server
            saveFile(fileInputStream, filePath);

            String output = "File saved to server location : " + filePath;

            return okResponse(output);
            //return Response.status(200).entity(output).build();

    }
    
    // save uploaded file to a defined location on the server
    private void saveFile(InputStream uploadedInputStream,
                    String serverLocation) {

            try {
                    OutputStream outpuStream = new FileOutputStream(new File(serverLocation));
                    int read = 0;
                    byte[] bytes = new byte[1024];

                    outpuStream = new FileOutputStream(new File(serverLocation));
                    while ((read = uploadedInputStream.read(bytes)) != -1) {
                            outpuStream.write(bytes, 0, read);
                    }
                    outpuStream.flush();
                    outpuStream.close();
            } catch (IOException e) {

                    e.printStackTrace();
            }

    }
    */
    
    private InputStream getSampleFile(){
        
        InputStream is = null;
        String testFileInputStreamName = "/Users/rmp553/Documents/iqss-git/dataverse-helper-scripts/src/api_scripts/input/howdy3.txt";
        //testFileInputStreamName = "/Users/rmp553/NetBeansProjects/dataverse/src/main/java/edu/harvard/iq/dataverse/datasetutility/howdy.txt";
        try {
            is = new FileInputStream(testFileInputStreamName);
            //is.close(); 
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }

        return is;

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
    
    
    private void removeLinkedFileFromDataset(Dataset dataset, DataFile dataFileToRemove){
        
         // remove the file from the dataset (since createDataFiles has already linked
        // it to the dataset!
        // first, through the filemetadata list, then through tht datafiles list:
        Iterator<FileMetadata> fmIt = dataset.getEditVersion().getFileMetadatas().iterator();
        msgt("Clear FileMetadatas");
        while (fmIt.hasNext()) {
        FileMetadata fm = fmIt.next();
            msg("Check: " + fm);
            if (fm.getId() == null && dataFileToRemove.getStorageIdentifier().equals(fm.getDataFile().getStorageIdentifier())) {
                msg("Got It! ");
                fmIt.remove();
                break;
            }
        }
        
        
        Iterator<DataFile> dfIt = dataset.getFiles().iterator();
        msgt("Clear Files");
        while (dfIt.hasNext()) {
            DataFile dfn = dfIt.next();
            msg("Check: " + dfn);
            if (dfn.getId() == null && dataFileToRemove.getStorageIdentifier().equals(dfn.getStorageIdentifier())) {
                msg("Got It! try to remove from iterator");
                
                dfIt.remove();
                msg("...didn't work");
                
                break;
            }else{
                msg("...ok");
            }
        }
    }
    
    /**
     *
     * @param fileId
     * @return
     */
    @GET
    @Path("resave/{fileId}")
    public Response hiReSave(@PathParam("fileId") Long fileId){
        msgt("hiReSave: " + fileId);
        DataFile df = fileService.find(fileId);
        
        if (df ==null){
            return okResponse("file not found: " + fileId);
        }
        df = fileService.save(df);
        
        return okResponse("saved: " + df);
    }    
        
    @GET
    @Path("add")
    public Response hi_add(){
        
        // -------------------------------------
        msgt("(1) getSampleFile()");
        // -------------------------------------

        InputStream testFileInputStream = getSampleFile();
        if (testFileInputStream == null){
            return okResponse("Couldn't find the file!!");
        }
        
        // -------------------------------------
        msgt("(1a) Get User from API token");
        // -------------------------------------
        User authUser;
        try {
            authUser = this.findUserOrDie();
        } catch (WrappedResponse ex) {
            return okResponse("Couldn't find a user from the API key");
        }
        //authSvc.findByID(new Long(1));        
        msg("authUser: " + authUser);
        msg("getUserIdentifier: " + authUser.getIdentifier());

        
        // -------------------------------------
        msgt("(1b) Get the selected Dataset");
        // -------------------------------------        
        int dataset_id = 10;
        Dataset selectedDataset = datasetService.find(new Long(dataset_id));

        
        //-------------------
        // ADD
        //-------------------
        msg("ADD!");

        DataverseRequest dvRequest2 = createDataverseRequest(authUser);
        AddReplaceFileHelper addFileHelper = new AddReplaceFileHelper(dvRequest2,
                                                this.ingestService,
                                                this.datasetService,
                                                this.fileService,
                                                this.permissionSvc,
                                                this.commandEngine);


        addFileHelper.runAddFile(selectedDataset,
                                "blackbox.txt",
                                "text/plain",
                                testFileInputStream);


        if (addFileHelper.hasError()){
            return okResponse(addFileHelper.getErrorMessagesAsString("\n"));
        }else{
            return okResponse("hey hey, it may have worked");
        }
            

    } // end call to "hi"


    @GET
    @Path("replace/{oldFileId}")
    public Response hi_replace(@PathParam("oldFileId") Long oldFileId){
        
        // -------------------------------------
        msgt("(1) getSampleFile()");
        // -------------------------------------

        InputStream testFileInputStream = getSampleFile();
        if (testFileInputStream == null){
            return okResponse("Couldn't find the file!!");
        }
        
        // -------------------------------------
        msgt("(1a) Get User from API token");
        // -------------------------------------
        User authUser;
        try {
            authUser = this.findUserOrDie();
        } catch (WrappedResponse ex) {
            return okResponse("Couldn't find a user from the API key");
        }
        //authSvc.findByID(new Long(1));        
        msg("authUser: " + authUser);
        msg("getUserIdentifier: " + authUser.getIdentifier());

        
        // -------------------------------------
        msgt("(1b) Get the selected Dataset");
        // -------------------------------------        
        int dataset_id = 10;
        Dataset selectedDataset = datasetService.find(new Long(dataset_id));

        
        //-------------------
        // REPLACE
        //-------------------

        msg("REPLACE!");


        DataverseRequest dvRequest2 = createDataverseRequest(authUser);
        AddReplaceFileHelper addFileHelper = new AddReplaceFileHelper(dvRequest2,
                                                this.ingestService,
                                                this.datasetService,
                                                this.fileService,
                                                this.permissionSvc,
                                                this.commandEngine);

        //Long oldFileId =  oldFileId;
        addFileHelper.runReplaceFile(selectedDataset,
                                "replace_" + oldFileId.toString() + ".txt",
                                "text/plain", 
                                testFileInputStream,
                                oldFileId
                                );


        if (addFileHelper.hasError()){
            return okResponse(addFileHelper.getErrorMessagesAsString("\n"));
        }else{
            return okResponse("hey hey, it may have worked");
        }
           
        
    } // end call to "hi"

}
