/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.api;

//import com.sun.jersey.core.header.FormDataContentDisposition;
//import com.sun.jersey.multipart.FormDataParam;
import edu.harvard.iq.dataverse.DataFile;
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
        String testFileName = "/Users/rmp553/Documents/iqss-git/dataverse-helper-scripts/src/api_scripts/input/howdy2.txt";
        //testFileName = "/Users/rmp553/NetBeansProjects/dataverse/src/main/java/edu/harvard/iq/dataverse/datasetutility/howdy.txt";
        try {
            is = new FileInputStream(testFileName);
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
    
    @GET
    @Path("hi")
    public Response hi(){
        
        // -------------------------------------
        msgt("(1) getSampleFile()");
        // -------------------------------------

        InputStream testFile = getSampleFile();
        if (testFile == null){
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
        
        
        // -------------------------------------
        msgt("(1c) Get the edit version of the Dataset");
        // -------------------------------------        
        DatasetVersion workingVersion = selectedDataset.getEditVersion();
        msg("new workingVersion: " + workingVersion + "\n   url:" +  selectedDataset.getPersistentURL());
                
        // -------------------------------------
        msgt("(1d) List the dataset version files");
        // -------------------------------------
      
        // List the current files
        //
        int cnt = 0;
        for (FileMetadata fm : workingVersion.getFileMetadatas()){
            cnt++;
            msg("File " + cnt + ": " + fm.getLabel());
        }
        dashes();
        
          
        // -------------------------------------
        msgt("(2) ingestService.createDataFiles");
        // -------------------------------------

        List<DataFile> dFileList = null; 
        msg("state of the workingVersion: " + workingVersion.getVersionState());
        try {
            msg("The starting bell rings....");
            dFileList = ingestService.createDataFiles(workingVersion,
                    testFile,
                    "hullo.txt",
                    "text/plain");
            msg("Almost there....");
        } catch (IOException ex) {
            msg("Not happy...:" + ex.toString());
            logger.severe(ex.toString());
            return okResponse("IOException when trying to ingest: " + testFile.toString());
        }
        
             
        // -------------------------------------
        msgt("(2A) we should have an additional file");
        // -------------------------------------
        // List the current files
        //
        cnt = 0;
        for (FileMetadata fm : workingVersion.getFileMetadatas()){
            cnt++;
            msg("File " + cnt + ": " + fm.getLabel());
        }
        dashes();
 
        
        

        // -------------------------------------
        msgt("3 Duplicate check");
        // -------------------------------------
        List<DataFile> newFiles = new ArrayList();
        msg("dFileList: " + dFileList.toString());
        String warningMessage  = null;
        for (DataFile df : dFileList){
            
            
             // -----------------------------------------------------------
            // Check for ingest warnings
            // -----------------------------------------------------------
            if (df.isIngestProblem()) {
                if (df.getIngestReportMessage() != null) {
                    if (warningMessage == null) {
                        warningMessage = df.getIngestReportMessage();
                    } else {
                        warningMessage = warningMessage.concat("; " + df.getIngestReportMessage());
                    }
                }
                df.setIngestDone();
            }
            if (warningMessage != null){
                return okResponse(warningMessage);
            }

            
            msg("Checking file: " + df.getFileMetadata().getLabel());
            //if (dfc.isFileInSavedDatasetVersion(workingVersion, df.getmd5())){               
            //    return okResponse("This file has a dupe md5! " + df.getFileMetadata().getLabel());
            if (DuplicateFileChecker.isDuplicateOriginalWay(workingVersion, df.getFileMetadata())){
                msg("has a dupe:");
                // Shut things down!
                try {
                    testFile.close();
                } catch (IOException ex) {
                    Logger.getLogger(FileUpload.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                String dupeName = df.getFileMetadata().getLabel();
                removeLinkedFileFromDataset(selectedDataset, df);
                
                return okResponse("This file has a dupe md5! " + dupeName + " checksum: " + df.getmd5());
            }else{
                //df.save();
                newFiles.add(df);
            }
        }
        
        
        // -------------------------------------
        msgt("(3a) List the new files");
        // -------------------------------------
        // List the current files
        //
        cnt = 0;
        for (DataFile df : newFiles){
            cnt++;
            msg("File " + cnt + ": " + df.getFileMetadata().getLabel());
        }
        dashes();

        
        // -------------------------------------
        msgt("4 Check constraints");
        // -------------------------------------
        Set<ConstraintViolation> constraintViolations = workingVersion.validate();    
        List<String> errMsgs = new ArrayList<>();
        for (ConstraintViolation violation : constraintViolations){
            msg("Violation found! :" + violation.getMessage());
            errMsgs.add(violation.getMessage());
        }
        if (errMsgs.size() > 0){
            return okResponse("Constraint violations found! " + String.join("<br />\n", errMsgs));
        }
        
        // -------------------------------------
        msgt("5 Add the files!");
        // -------------------------------------
        ingestService.addFiles(workingVersion, newFiles);


        // -------------------------------------
        msgt("6 Make the command!");
        // -------------------------------------
        /*
        
            execCommand(new SetDatasetCitationDateCommand(createDataverseRequest(findUserOrDie()), findDatasetOrDie(id), dsfType));

        */

        DataverseRequest dvRequest = createDataverseRequest(authUser);
        msg("dvRequest: " + dvRequest);

        if (dvRequest == null){
            return okResponse("Failed, dvRequest is null");
        }
        //CreateDatasetCommand cmd = new CreateDatasetCommand(workingVersion.getDataset(), 
        //                      dvRequest);
        Command<Dataset> update_cmd;
        update_cmd = new UpdateDatasetCommand(selectedDataset, dvRequest);
        ((UpdateDatasetCommand) update_cmd).setValidateLenient(true);  

        // -------------------------------------
        msgt("7 Run the command!");
        // -------------------------------------
        try {
            commandEngine.submit(update_cmd);
        } catch (CommandException ex) {
            //ex.getMessage()
            msgt("Bombed: " + ex.getMessage());
            return okResponse("bombed....");
            //Logger.getLogger(FileUpload.class.getName()).log(Level.SEVERE, null, ex);
        }catch (EJBException ex) {
            msgt("Bombed2: " + ex.getMessage());
            return okResponse("bombed 2....");
        } 
        
        // -------------------------------------
        msgt("8 userNotificationService");
        // -------------------------------------
        
        userNotificationService.sendNotification((AuthenticatedUser) authUser, selectedDataset.getCreateDate(), UserNotification.Type.CREATEDS, selectedDataset.getLatestVersion().getId());

        // -------------------------------------
        msgt("9 start Ingest jobs");
        // -------------------------------------
        newFiles.clear();
        
        
        ingestService.startIngestJobs(selectedDataset, (AuthenticatedUser) authUser);

        
        return okResponse("hi. maybe it worked!");

    }

}
