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
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.datasetutility.DuplicateFileChecker;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetCommand;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
import javax.validation.ConstraintViolation;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

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
        String testFileName = "/Users/rmp553/Documents/iqss-git/dataverse-helper-scripts/src/api_scripts/input/howdy.txt";
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
    
    @GET
    @Path("hi")
    public Response hi(){
        
        // -------------------------------------
        msgt("(1) getSampleFile() + workingVersion");
        // -------------------------------------

        InputStream testFile = getSampleFile();
        if (testFile == null){
            return okResponse("Couldn't find the file!!");
        }
        DatasetVersion workingVersion = datasetVersionService.find(new Long(3));
        
        if (workingVersion.getVersionState()!=DatasetVersion.VersionState.DRAFT){
            return okResponse("For testing, making the sure the state is DRAFT.  This workingVersion is: " + workingVersion.getVersionState());
        }
        
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
        msg("But ok, we can continue now...");

        // -------------------------------------
        msgt("3 Duplicate check");
        // -------------------------------------
        List<DataFile> newFiles = new ArrayList();

        DuplicateFileChecker dfc = new DuplicateFileChecker(datasetVersionService);
        for (DataFile df : dFileList){
            //if (dfc.isFileInSavedDatasetVersion(workingVersion, df.getmd5())){               
            //    return okResponse("This file has a dupe md5! " + df.getFileMetadata().getLabel());
            if (DuplicateFileChecker.isDuplicateOriginalWay(workingVersion, df.getFileMetadata())){
               return okResponse("This file has a dupe md5! " + df.getFileMetadata().getLabel());
            }else{
                newFiles.add(df);
            }
        }
        
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
        AuthenticatedUser authUser = authSvc.findByID(new Long(1));        
        msg("authUser: " + authUser);
        DataverseRequest dvRequest = createDataverseRequest(authUser);
        msg("dvRequest: " + dvRequest);

        if (dvRequest == null){
            return okResponse("Failed, dvRequest is null");
        }
        CreateDatasetCommand cmd = new CreateDatasetCommand(workingVersion.getDataset(), 
                                dvRequest);

        // -------------------------------------
        msgt("7 Run the command!");
        // -------------------------------------
        try {
            Dataset newDataset = commandEngine.submit(cmd);
        } catch (CommandException ex) {
            //ex.getMessage()
            msgt("Bombed: " + ex.getMessage());
            //Logger.getLogger(FileUpload.class.getName()).log(Level.SEVERE, null, ex);
        }catch (EJBException ex) {
            msgt("Bombed2: " + ex.getMessage());
        } 
        
        return okResponse("hi");

    }

}
