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
import edu.harvard.iq.dataverse.MapLayerMetadata;
import edu.harvard.iq.dataverse.MapLayerMetadataServiceBean;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.stream.JsonParsingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import org.atmosphere.config.service.Post;
import org.atmosphere.config.service.Put;
import org.primefaces.json.JSONObject;

/**
 *
 * @author rmp553
 */
@Path("worldmap")
public class WorldMapRelatedData extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Files.class.getCanonicalName());

    
    private static final String BASE_PATH = "/api/worldmap/";
    
    public static final String MAP_IT_API_PATH_FRAGMENT =  "map-it/";  
    public static final String MAP_IT_API_PATH = BASE_PATH + MAP_IT_API_PATH_FRAGMENT;
    
    public static final String GET_WORLDMAP_DATAFILE_API_PATH_FRAGMENT =  "datafile/";  
    public static final String GET_WORLDMAP_DATAFILE_API_PATH =  BASE_PATH + GET_WORLDMAP_DATAFILE_API_PATH_FRAGMENT;
    
    
    public static final String UPDATE_MAP_LAYER_DATA_API_PATH_FRAGMENT = "update-layer-metadata"; 
    public static final String UPDATE_MAP_LAYER_DATA_API_PATH = BASE_PATH + UPDATE_MAP_LAYER_DATA_API_PATH_FRAGMENT;

    // for testing, move to config file
    //
    private static final String GEOCONNECT_URL = "http://127.0.0.1:8070/shapefile/map-it";
    private static final String GEOCONNECT_TOKEN_KEY = "GEOCONNECT_TOKEN";
    
    private static final String GEOCONNECT_TOKEN_VALUE = "howdy";  // for testing

    @EJB
    MapLayerMetadataServiceBean mapLayerMetadataService;

    @EJB
    DatasetServiceBean datasetService;

    @EJB
    DataFileServiceBean dataFileService;
    

    /*
        Link used within Dataverse for MapIt button
        Sends file link to GeoConnect using a Redirect
    
    */
    @GET
    @Path(MAP_IT_API_PATH_FRAGMENT + "{datafile_id}")
    public Response mapDataFile(@Context HttpServletRequest request, @PathParam("datafile_id") Long datafile_id){
        
        // Check if this file exists
        DataFile dfile = dataFileService.find(datafile_id);
        if (dfile==null){
           return errorResponse(Response.Status.NOT_FOUND, "DataFile not found for md5: " + datafile_id);
        }
        
        // Redirect to geoconnect url
        String callback_url = this.getServerNamePort(request) + GET_WORLDMAP_DATAFILE_API_PATH + dfile.getId();
        String redirect_url_str = WorldMapRelatedData.GEOCONNECT_URL + "?cb=" +  URLEncoder.encode(callback_url);
        URI redirect_uri;
        try {
            redirect_uri = new URI(redirect_url_str);
        } catch (URISyntaxException ex) {
             return errorResponse(Response.Status.NOT_FOUND, "Faile to create URI from: " + redirect_url_str);
        }
//        Response.
        return Response.seeOther(redirect_uri).build();
        
    }
    
    /*
        For WorldMap/GeoConnect Usage
        Return detailed Datafile information including latest Dataset and Dataverse data
        
        e.g. http://localhost:8080/api/worldmap/datafile/33?key=some-key

        !! Does not yet implement permissions/command checks
        !! Change to POST with check for hidden WorldMap key; IP check, etc
    */
    @POST
    @Path(GET_WORLDMAP_DATAFILE_API_PATH_FRAGMENT + "{datafile_id}")
    public Response getWorldMapDatafile(String jsonTokenData, @Context HttpServletRequest request, @PathParam("datafile_id") Long datafile_id, @QueryParam("key") String apiKey) {
        
        //            + "<br /> getRemoteAddr: " + request.getRemoteAddr()         
        //            + "<br /> X-FORWARDED-FOR: " + request.getHeader("X-FORWARDED-FOR")
        
        // Temp: Check if the user exists
        // Change this to WorldMap API check!!
        DataverseUser dv_user = userSvc.findByUserName(apiKey);
        if (dv_user == null) {
            return errorResponse(Response.Status.UNAUTHORIZED, "Invalid apikey '" + apiKey + "'");
        }
        //----------------------------------
        // Auth check: Parse the json message and check for a valid GEOCONNECT_TOKEN_KEY and GEOCONNECT_TOKEN_VALUE
        //   -- For testing, the GEOCONNECT_TOKEN_VALUE will be dynamic, found in the db, etc.
        //----------------------------------

        JsonObject json_token_info;
        try ( StringReader rdr = new StringReader(jsonTokenData) ) {
            json_token_info = Json.createReader(rdr).readObject();
        } catch ( JsonParsingException jpe ) {
            logger.log(Level.SEVERE, "Json: " + jsonTokenData);
            return errorResponse( Response.Status.BAD_REQUEST, "Error parsing Json: " + jpe.getMessage() );
        }
        
        if (!json_token_info.containsKey(GEOCONNECT_TOKEN_KEY)){
            return errorResponse( Response.Status.BAD_REQUEST, "Permission denied (1)");
            //return errorResponse( Response.Status.BAD_REQUEST, "Error parsing Json.  Key not found [" + GEOCONNECT_TOKEN_KEY + "]");
        }
        if (!(json_token_info.getString(GEOCONNECT_TOKEN_KEY).equalsIgnoreCase(GEOCONNECT_TOKEN_VALUE))){
            return errorResponse( Response.Status.BAD_REQUEST, "Permission denied (2)");
        }
        //-----------------------------------

        // (1) Attempt to retrieve DataFile indicated by id
        DataFile dfile = dataFileService.find(datafile_id);
        if (dfile==null){
           return errorResponse(Response.Status.NOT_FOUND, "DataFile not found for id: " + datafile_id);
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
        dfile_json.add("filesize", dfile.getFilesize()); 
        dfile_json.add("datafile_type", dfile.getContentType());
        dfile_json.add("created", dfile.getCreateDate().toString());
                      
        String server_name =  this.getServerNamePort(request);
        dfile_json.add("datafile_download_url", dfile.getMapItFileDownloadURL(server_name));
       
        
        // DataverseUser Info
        dfile_json.add("dv_user_email", dv_user.getEmail());
        dfile_json.add("dv_username", dv_user.getUserName());
        dfile_json.add("dv_user_id", dv_user.getId());
                
        
        return okResponse(dfile_json);
 
    }
   
    
    private String getServerNamePort(HttpServletRequest request){
        if (request == null){
            return "";
        }
        String serverName = request.getServerName();
        if (serverName==null){
             return "";
        }
        int portNumber = request.getServerPort();
        if (portNumber==80){
           return "http://" + serverName;
        }
        return "http://" + serverName + ":" + portNumber;
               
    }
    
    /*
        For WorldMap/GeoConnect Usage
        Create a MayLayerMetadata object for a given Datafile id
        
        !! Does not yet implement permissions/command checks
        !! Change to check for hidden WorldMap key; IP check, etc
    
        Example of jsonLayerData String:
        {
             "layerName": "geonode:boston_census_blocks_zip_cr9"
            , "layerLink": "http://localhost:8000/data/geonode:boston_census_blocks_zip_cr9"
            , "embedMapLink": "http://localhost:8000/maps/embed/?layer=geonode:boston_census_blocks_zip_cr9"
            , "worldmapUsername": "dv_pete"
        }
    */
    @POST
    @Path(UPDATE_MAP_LAYER_DATA_API_PATH_FRAGMENT) // + "{datafile_id}")
    //public Response updateWorldMapLayerData(String jsonLayerData, @PathParam("datafile_id") Long datafile_id, @QueryParam("key") String apiKey){
    public Response updateWorldMapLayerData(String jsonLayerData, @QueryParam("key") String apiKey){
        
        // Temp: Check if the user exists
        // Change this to WorldMap API check!!
        DataverseUser dv_user = userSvc.findByUserName(apiKey);
        if (dv_user == null) {
            return errorResponse(Response.Status.UNAUTHORIZED, "Invalid apikey '" + apiKey + "'");
        }
        
        
        // (1) Parse the json message
        JsonObject json_info;
        try ( StringReader rdr = new StringReader(jsonLayerData) ) {
            json_info = Json.createReader(rdr).readObject();
        } catch ( JsonParsingException jpe ) {
            logger.log(Level.SEVERE, "Json: " + jsonLayerData);
            return errorResponse( Response.Status.BAD_REQUEST, "Error parsing Json: " + jpe.getMessage() );
        }
        
        // (1a) Check for the correct token
        // Next step: dynamic token, etc.
        /*
        if (!json_info.containsKey(GEOCONNECT_TOKEN_KEY)){
            return errorResponse( Response.Status.BAD_REQUEST, "Permission denied (1)");
            //return errorResponse( Response.Status.BAD_REQUEST, "Error parsing Json.  Key not found [" + GEOCONNECT_TOKEN_KEY + "]");
        }
        if (!(json_info.getString(GEOCONNECT_TOKEN_KEY).equalsIgnoreCase(GEOCONNECT_TOKEN_VALUE))){
            return errorResponse( Response.Status.BAD_REQUEST, "Permission denied (2)");
        }
        */
        
        
        
        // (2) Make sure the json message has all of the required attributes
        for (String attr : MapLayerMetadata.MANDATORY_JSON_FIELDS ){
            if (!json_info.containsKey(attr)){
                return errorResponse( Response.Status.BAD_REQUEST, "Error parsing Json.  Key not found [" + attr + "]\nRequired keys are: " + MapLayerMetadata.MANDATORY_JSON_FIELDS  );
            }
        }
        
        // (2) Attempt to retrieve DataFile indicated by id
        Integer datafileID = json_info.getInt("datafileID");
        if (datafileID==null){
           return errorResponse(Response.Status.NOT_FOUND, "DataFile id not found in JSON: " + json_info);
        }
        
        DataFile dfile = dataFileService.find(datafileID.longValue());
        if (dfile==null){
           return errorResponse(Response.Status.NOT_FOUND, "DataFile not found for id: " + datafileID);
        }

        
        MapLayerMetadata mapLayer;
        // See if a MapLayerMetadata already exists
        mapLayer = mapLayerMetadataService.findMetadataByLayerNameAndDatafile(json_info.getString("layerName"));//, dfile);
        if (mapLayer == null){
            mapLayer = new MapLayerMetadata();
        }

        // Create/Update new MapLayerMetadata object and save it
        mapLayer.setDataFile(dfile);
        mapLayer.setDataset(dfile.getOwner());
        mapLayer.setLayerName(json_info.getString("layerName"));
        mapLayer.setLayerLink(json_info.getString("layerLink"));
        mapLayer.setEmbedMapLink(json_info.getString("embedMapLink"));
        mapLayer.setWorldmapUsername(json_info.getString("worldmapUsername"));

        //mapLayer.save();
        MapLayerMetadata saved_map_layer = mapLayerMetadataService.save(mapLayer);
        if (saved_map_layer==null){
            logger.log(Level.SEVERE, "Json: " + jsonLayerData);
            return errorResponse( Response.Status.BAD_REQUEST, "Failed to save map layer!  Original JSON: ");
        }
        return okResponse("map layer object saved!");

        
//        return okResponse("In process");
    }
}
