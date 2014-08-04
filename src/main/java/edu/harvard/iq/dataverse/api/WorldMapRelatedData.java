/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseUser;
import edu.harvard.iq.dataverse.FileMetadata;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 *
 * @author rmp553
 */
@Path("worldmap")
public class WorldMapRelatedData extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Files.class.getCanonicalName());

    @EJB
    DatasetServiceBean datasetService;

    @EJB
    DataFileServiceBean dataFileService;
    
    
    @GET
    //@Path("{id}")
    @Path("datafile/{id}")
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
   
    
    @GET
    @Path("layer-update/{id}")
    public Response updateWorldMapLayerData(@PathParam("id") Long id, @QueryParam("key") String apiKey){
        return okResponse("In process");
    }
}
