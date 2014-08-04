package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("files")
public class Files extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Files.class.getCanonicalName());

    @EJB
    DatasetServiceBean datasetService;

    @EJB
    DataFileServiceBean dataFileService;
    
    
    @GET
    @Path("{id}")
    /*
        For WorldMap/GeoConnect Usage
        Return detailed Datafile information including latest Dataset and Dataverse data
        
        !! Does not yet implement permissions/command checks
        !! Change to POST with check for hidden WorldMap key; IP check, etc
    */
    public Response getWorldMapDatafile(@PathParam("id") Long id, @QueryParam("key") String apiKey) {
        
        // Temp: Check if the user exists
        // Change this to WorldMap API check!!
        DataverseUser dv_user = userSvc.findByUserName(apiKey);
        if (dv_user == null) {
            return errorResponse(Response.Status.UNAUTHORIZED, "Invalid apikey '" + apiKey + "'");
        }
        
        // (1) Attempt to retrieve DataFile indicated by id
        DataFile dfile = dataFileService.find(id);
        if (dfile==null){
           return errorResponse(Response.Status.NOT_FOUND, "DataFile not found");
        }
        FileMetadata dfile_meta = dfile.getFileMetadata();
        if (dfile_meta==null){
           return errorResponse(Response.Status.NOT_FOUND, "FileMetadata not found");
        }
        
        // (2) Now get the dataset and the latest DatasetVersion
        Dataset dset = dfile.getOwner();
        if (dset==null){
            return errorResponse(Response.Status.NOT_FOUND, "Owning Dataset for this DataFile not found");
        }
        
        // (2a) latest DatasetVersion
        // !! How do you check if the lastest version has this specific file?
        //
        DatasetVersion dset_version = dset.getLatestVersion();
        if (dset==null){
            return errorResponse(Response.Status.NOT_FOUND, "Latest DatasetVersion for this DataFile not found");
        }
        
        // (3) get Dataverse
        Dataverse dverse = dset.getOwner();
        if (dverse==null){
            return errorResponse(Response.Status.NOT_FOUND, "Dataverse for this DataFile's Dataset not found");
        }
        
        // (4) Roll it all up in a JSON response
        final JsonObjectBuilder dfile_json = Json.createObjectBuilder();
      
        // Dataverse
        dfile_json.add("dv_id", dverse.getId());
        dfile_json.add("dv_name", dverse.getName());
        
        // DatasetVersion Info
        dfile_json.add("dataset_name", dset_version.getTitle());
        dfile_json.add("dataset_description", dset_version.getCitation());
        dfile_json.add("dataset_id", dset_version.getId());
        dfile_json.add("dataset_version_id", dset_version.getVersion());
        
        // DataFile/FileMetaData Info
        dfile_json.add("datafile_id", dfile.getId());
        dfile_json.add("filename", dfile_meta.getLabel());
        dfile_json.add("datafile_label", dfile_meta.getLabel());
        dfile_json.add("datafile_expected_md5_checksum", dfile.getmd5());
        dfile_json.add("filesize", 123456); // Size not available! 
        dfile_json.add("datafile_type", dfile.getContentType());
        dfile_json.add("created", dfile.getCreateDate().toString());
        
        // DataverseUser Info
        dfile_json.add("dv_user_email", dv_user.getEmail());
        dfile_json.add("dv_username", dv_user.getUserName());
        dfile_json.add("dv_user_id", dv_user.getId());
        
        /*       
        "datafile_download_url": "http://127.0.0.1:8090/media/datafile/2014/06/26/boston_census_blocks_1.zip",
        */
        return okResponse(dfile_json);
 
    }
   
    
    @POST
    public String add(DataFile dataFile, @QueryParam("key") String apiKey) {
        Dataset dataset;
        try {
            dataset = datasetService.find(dataFile.getOwner().getId());
        } catch (EJBException ex) {
            return Util.message2ApiError("Couldn't find dataset to save file to. File was " + dataFile);
        }
        List<DataFile> newListOfFiles = dataset.getFiles();
        newListOfFiles.add(dataFile);
        dataset.setFiles(newListOfFiles);
        try {
            DataverseUser u = userSvc.findByUserName(apiKey);
            if (u == null) {
                return error("Invalid apikey '" + apiKey + "'");
            }
            engineSvc.submit(new UpdateDatasetCommand(dataset, u));
            String fileName = "[No name?]";
            if (dataFile.getFileMetadata() != null) {
                fileName = dataFile.getFileMetadata().getLabel(); 
            }
            return "file " + fileName + " created/updated with dataset " + dataset.getId() + " (and probably indexed, check server.log)\n";
        } catch (EJBException ex) {
            Throwable cause = ex;
            StringBuilder sb = new StringBuilder();
            sb.append(ex);
            while (cause.getCause() != null) {
                cause = cause.getCause();
                sb.append(cause.getClass().getCanonicalName() + " ");
                if (cause instanceof ConstraintViolationException) {
                    ConstraintViolationException constraintViolationException = (ConstraintViolationException) cause;
                    for (ConstraintViolation<?> violation : constraintViolationException.getConstraintViolations()) {
                        sb.append("(invalid value: <<<" + violation.getInvalidValue() + ">>> for " + violation.getPropertyPath() + " at " + violation.getLeafBean() + " - " + violation.getMessage() + ")");
                    }
                }
            }
            return Util.message2ApiError("POST failed: " + sb.toString());
        } catch (CommandException ex) {
            return error("Can't update dataset: " + ex.getMessage());
        }
//        return "file " + dataFile.getName() + " indexed dataset " + dataFile.getName() + " files updated (and probably indexed, check server.log)\n";
    }

}
