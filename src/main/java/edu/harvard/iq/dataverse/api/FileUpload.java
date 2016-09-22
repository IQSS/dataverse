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
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
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
    
    @GET
    @Path("hi")
    public Response hi(){
        
        InputStream testFile = getSampleFile();
        if (testFile == null){
            return okResponse("Couldn't find the file!!");
        }
        DatasetVersion workingVersion = datasetVersionService.find(new Long(3));
        Dataset dataset = workingVersion.getDataset(); //datasetService.find(new Long(26));

        int cnt = 0;
        for (FileMetadata fm : workingVersion.getFileMetadatas()){
            cnt++;
            msg("File " + cnt + ": " + fm.getLabel());
        }
        dashes();
        //DatasetVersion workingVersion = null;
                
        /*
        ------------------------------------------
        ------------------------------------------
            Set up the workingVersion for editing
            - copied from DatasetPage*
                * undisputed king of tech debt...
        ------------------------------------------
        */
        List<Template> dataverseTemplates = new ArrayList();
        Long ownerId = dataset.getOwner().getId();
        Template defaultTemplate = null;
        Template selectedTemplate = null;
        dataverseTemplates = dataverseService.find(ownerId).getTemplates();
        
        if (!dataverseService.find(ownerId).isTemplateRoot()) {
            dataverseTemplates.addAll(dataverseService.find(ownerId).getParentTemplates());
        }
        
        defaultTemplate = dataverseService.find(ownerId).getDefaultTemplate();
        if (defaultTemplate != null) {
            selectedTemplate = defaultTemplate;
            for (Template testT : dataverseTemplates) {
                if (defaultTemplate.getId().equals(testT.getId())) {
                    selectedTemplate = testT;
                }
            }
            workingVersion = dataset.getEditVersion(selectedTemplate);
        } 
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
  //      testDataset = 
                
        return okResponse("hi");

    }

}
