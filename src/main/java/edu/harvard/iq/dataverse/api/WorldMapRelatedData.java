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
import edu.harvard.iq.dataverse.DataverseUserServiceBean;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.MapLayerMetadata;
import edu.harvard.iq.dataverse.MapLayerMetadataServiceBean;
import edu.harvard.iq.dataverse.worldmapauth.TokenApplicationTypeServiceBean;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import edu.harvard.iq.dataverse.worldmapauth.WorldMapToken;
import edu.harvard.iq.dataverse.worldmapauth.WorldMapTokenServiceBean;
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
import javax.json.stream.JsonParsingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 *
 * @author raprasad
 */
@Path("worldmap")
public class WorldMapRelatedData extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Files.class.getCanonicalName());

    
    private static final String BASE_PATH = "/api/worldmap/";
    
    public static final String MAP_IT_API_PATH_FRAGMENT =  "map-it/";  
    public static final String MAP_IT_API_PATH = BASE_PATH + MAP_IT_API_PATH_FRAGMENT;
    
    public static final String GET_WORLDMAP_DATAFILE_API_PATH_FRAGMENT =  "datafile/";  
    public static final String GET_WORLDMAP_DATAFILE_API_PATH =  BASE_PATH + GET_WORLDMAP_DATAFILE_API_PATH_FRAGMENT;
    
    
    public static final String UPDATE_MAP_LAYER_DATA_API_PATH_FRAGMENT = "update-layer-metadata/"; 
    public static final String UPDATE_MAP_LAYER_DATA_API_PATH = BASE_PATH + UPDATE_MAP_LAYER_DATA_API_PATH_FRAGMENT;

    
    @EJB
    MapLayerMetadataServiceBean mapLayerMetadataService;

    @EJB
    DatasetServiceBean datasetService;

    @EJB
    DataFileServiceBean dataFileService;
    
    @EJB
    UserNotificationServiceBean userNotificationService;
    
    @EJB
    WorldMapTokenServiceBean tokenServiceBean;

    @EJB
    TokenApplicationTypeServiceBean tokenAppServiceBean;

    @EJB
    DataverseUserServiceBean dataverseUserService;

    
    /**
     *  Create URL for API call to WorldMapRelatedData.mapDataFile(...)   
     * 
     * @param dataFileID
     * @param dataverseUserID
     * @return 
     */
    public static String getMapItURL(Long dataFileID, Long dataverseUserID){
        if ((dataverseUserID==null)||(dataFileID==null)){
            return null;
        }
        //test, see if it gets created
        return WorldMapRelatedData.MAP_IT_API_PATH + dataFileID + "/" + dataverseUserID;
    }
    
    
    /*
        Link used within Dataverse for MapIt button
        Sends file link to GeoConnect using a Redirect
    
    */
    @GET
    @Path(MAP_IT_API_PATH_FRAGMENT + "{datafile_id}" + "/" + "{dvuser_id}") ///{dvuser_id}")
    public Response mapDataFile(@Context HttpServletRequest request
                                , @PathParam("datafile_id") Long datafile_id
                                , @PathParam("dvuser_id") Long dvuser_id){ 
        
         logger.info("mapDataFile datafile_id: " + datafile_id );
        logger.info("mapDataFile dvuser_id: " + dvuser_id );
        if (true){
           //tokenAppServiceBean.getGeoConnectApplication();           
           //return okResponse("Currently deactivated (mapDataFile)");
        }

        // Check if the user exists
        DataverseUser dvUser = dataverseUserService.find(dvuser_id);
	if ( dvUser == null ){
            return errorResponse(Response.Status.FORBIDDEN, "Invalid user");
        }

        // Check if this file exists
        DataFile dfile = dataFileService.find(datafile_id);
        if (dfile==null){
           return errorResponse(Response.Status.NOT_FOUND, "DataFile not found for md5: " + datafile_id);
        }
        
        // TO ADD WHEN PERMISSIONS ARE READY
        // Does this user have permission to edit metadata for this file?
        WorldMapToken token = tokenServiceBean.getNewToken(dfile, dvUser);

        // Redirect to geoconnect url
 //       String callback_url = this.getServerNamePort(request) + GET_WORLDMAP_DATAFILE_API_PATH + dfile.getId();
        String callback_url = this.getServerNamePort(request) + GET_WORLDMAP_DATAFILE_API_PATH;
        String redirect_url_str = token.getApplication().getMapitLink() + "/" + token.getToken() + "/?cb=" +  URLEncoder.encode(callback_url);
        //String redirect_url_str = TokenApplicationType.DEV_MAPIT_LINK + "/" +  token.getToken() + "/?cb=" +  URLEncoder.encode(callback_url);
        URI redirect_uri;
        
        try {
            redirect_uri = new URI(redirect_url_str);
        } catch (URISyntaxException ex) {
             return errorResponse(Response.Status.NOT_FOUND, "Faile to create URI from: " + redirect_url_str);
        }
//        Response.
        return Response.seeOther(redirect_uri).build();
        
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
    
    /**
     * Parse json looking for the GEOCONNECT_TOKEN_KEY.
     * Make sure that the string itself is not null and 64 chars
     * 
     * @param jsonTokenInfo
     * @return 
     */
    private String retrieveTokenValueFromJson(JsonObject jsonTokenInfo){
        if (jsonTokenInfo==null){
            return null;
        }
        if (!jsonTokenInfo.containsKey(WorldMapToken.GEOCONNECT_TOKEN_KEY)){
            logger.warning("Token not found.  Permission denied.");
            return null;
            //return errorResponse( Response.Status.BAD_REQUEST, "Permission denied");
        }
        Object worldmapTokenObject = jsonTokenInfo.get(WorldMapToken.GEOCONNECT_TOKEN_KEY);
        if (worldmapTokenObject==null){
            logger.warning("Token is null found.  Permission denied.");
            return null;
            //return errorResponse( Response.Status.BAD_REQUEST, "Token value not found");
        }
                
        String worldmapTokenParam = worldmapTokenObject.toString();                
        if (worldmapTokenParam==null){      // shouldn't happen
            logger.warning("worldmapTokenParam is null when .toString() called.  Permission denied.");
            return null;
            //return errorResponse(Response.Status.UNAUTHORIZED, "No access.");
        }
        if (!(worldmapTokenParam.length()==64)){
            logger.warning("worldmapTokenParam not length 64.  Permission denied.");
            return null;
           // return errorResponse(Response.Status.UNAUTHORIZED, "No access.");            
        } 
        return worldmapTokenParam;
    }
    
    /**
     * Given a string token, retrieve the related WorldMapToken object
     * 
     * @param worldmapTokenParam
     * @return WorldMapToken object (if it hasn't expired)
     */
    private WorldMapToken retrieveAndRefreshValidToken(String worldmapTokenParam){
        if (worldmapTokenParam==null){
            logger.warning("worldmapTokenParam is null.  Permission denied.");
            return null;
        }
        WorldMapToken wmToken = this.tokenServiceBean.findByName(worldmapTokenParam);
        if (wmToken==null){
            logger.warning("WorldMapToken not found for '" + worldmapTokenParam + "'.  Permission denied.");
            return null;
        }
        if (wmToken.hasTokenExpired()){
            logger.warning("WorldMapToken has expired.  Permission denied.");
            return null;
        }
        wmToken.refreshToken();
        logger.info("WorldMapToken refreshed.");
        tokenServiceBean.save(wmToken);
        
        return wmToken;
    }

    @POST
    @Path(GET_WORLDMAP_DATAFILE_API_PATH_FRAGMENT)// + "{worldmap_token}")
    public Response getWorldMapDatafileInfo(String jsonTokenData, @Context HttpServletRequest request){//, @PathParam("worldmap_token") String worldmapTokenParam) {
        if (true){
           //return okResponse("Currently deactivated");

           // return okResponse("remote server: " + request.getRemoteAddr());
        }
        //----------------------------------
        // Auth check: Parse the json message and check for a valid GEOCONNECT_TOKEN_KEY and GEOCONNECT_TOKEN_VALUE
        //   -- For testing, the GEOCONNECT_TOKEN_VALUE will be dynamic, found in the db
        //----------------------------------

        // Parse JSON 
        JsonObject jsonTokenInfo;
        try ( StringReader rdr = new StringReader(jsonTokenData) ) {
            jsonTokenInfo = Json.createReader(rdr).readObject();
        } catch ( JsonParsingException jpe ) {
            logger.log(Level.SEVERE, "Json: " + jsonTokenData);
            return errorResponse( Response.Status.BAD_REQUEST, "Error parsing Json: " + jpe.getMessage() );
        }
        
        // Retrieve token string
        String worldmapTokenParam = this.retrieveTokenValueFromJson(jsonTokenInfo);
        if (worldmapTokenParam==null){
            return errorResponse(Response.Status.UNAUTHORIZED, "Permission denied.");
        }

        // Retrieve WorldMapToken and make sure it is valid
        //
        WorldMapToken wmToken = this.retrieveAndRefreshValidToken(worldmapTokenParam);
        if (wmToken==null){
            return errorResponse(Response.Status.UNAUTHORIZED, "No access. Invalid token.");
        }

        // (1) Retrieve token connected data: DataverseUser, DataFile
        //
        // Make sure token user and file are still available
        //
        DataverseUser dv_user = wmToken.getDataverseUser();
        if (dv_user == null) {
            return errorResponse(Response.Status.NOT_FOUND, "DataverseUser not found for token");
        }
        DataFile dfile = wmToken.getDatafile();
        if (dfile  == null) {
            return errorResponse(Response.Status.NOT_FOUND, "DataFile not found for token");
        }
                
        // (1a) Retrieve FileMetadata
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
        if (dset_version==null){
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
        Long fsize = dfile.getFilesize();
        if (fsize == null){
            fsize= new Long(-1);
        }
            
        dfile_json.add("filesize", fsize); 
        dfile_json.add("datafile_type", dfile.getContentType());
        dfile_json.add("created", dfile.getCreateDate().toString());
                      
        /* Dataverse URLs to this server */
        String serverName =  this.getServerNamePort(request);
        dfile_json.add("datafile_download_url", dfile.getMapItFileDownloadURL(serverName));
        dfile_json.add("return_to_dataverse_url", dset_version.getReturnToDatasetURL(serverName, dset));

        
        // DataverseUser Info
        dfile_json.add("dv_user_email", dv_user.getEmail());
        dfile_json.add("dv_username", dv_user.getUserName());
        dfile_json.add("dv_user_id", dv_user.getId());
                
        
        return okResponse(dfile_json);
 
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
    public Response updateWorldMapLayerData(String jsonLayerData){//, @QueryParam("key") String apiKey){
        
         //----------------------------------
        // Auth check: Parse the json message and check for a valid GEOCONNECT_TOKEN_KEY and GEOCONNECT_TOKEN_VALUE
        //   -- For testing, the GEOCONNECT_TOKEN_VALUE will be dynamic, found in the db
        //----------------------------------

        // (1) Parse JSON 
        //
        JsonObject jsonInfo;
        try ( StringReader rdr = new StringReader(jsonLayerData) ) {
            jsonInfo = Json.createReader(rdr).readObject();
        } catch ( JsonParsingException jpe ) {
            logger.log(Level.SEVERE, "Json: " + jsonLayerData);
            return errorResponse( Response.Status.BAD_REQUEST, "Error parsing Json: " + jpe.getMessage() );
        }
        
        // Retrieve token string
        String worldmapTokenParam = this.retrieveTokenValueFromJson(jsonInfo);
        if (worldmapTokenParam==null){
            return errorResponse(Response.Status.UNAUTHORIZED, "Permission denied.");
        }

        // Retrieve WorldMapToken and make sure it is valid
        //
        WorldMapToken wmToken = this.retrieveAndRefreshValidToken(worldmapTokenParam);
        if (wmToken==null){
            return errorResponse(Response.Status.UNAUTHORIZED, "No access. Invalid token.");
        }

        
        // (2) Make sure the json message has all of the required attributes
        //
        for (String attr : MapLayerMetadata.MANDATORY_JSON_FIELDS ){
            if (!jsonInfo.containsKey(attr)){
                return errorResponse( Response.Status.BAD_REQUEST, "Error parsing Json.  Key not found [" + attr + "]\nRequired keys are: " + MapLayerMetadata.MANDATORY_JSON_FIELDS  );
            }
        }
        
        // (3) Attempt to retrieve DataverseUser      
        DataverseUser dv_user = wmToken.getDataverseUser();
        if (dv_user == null) {
            return errorResponse(Response.Status.NOT_FOUND, "DataverseUser not found for token");
        }
        
        // (4) Attempt to retrieve DataFile      
        DataFile dfile = wmToken.getDatafile();
        if (dfile==null){
            return errorResponse(Response.Status.NOT_FOUND, "DataFile not found for token");
         }

       
        
        MapLayerMetadata mapLayer;
        // (5) See if a MapLayerMetadata already exists
        mapLayer = mapLayerMetadataService.findMetadataByLayerNameAndDatafile(jsonInfo.getString("layerName"));//, dfile);
        if (mapLayer == null){
            mapLayer = new MapLayerMetadata();
        }

        // Create/Update new MapLayerMetadata object and save it
        mapLayer.setDataFile(dfile);
        mapLayer.setDataset(dfile.getOwner());
        mapLayer.setLayerName(jsonInfo.getString("layerName"));
        mapLayer.setLayerLink(jsonInfo.getString("layerLink"));
        mapLayer.setEmbedMapLink(jsonInfo.getString("embedMapLink"));
        mapLayer.setWorldmapUsername(jsonInfo.getString("worldmapUsername"));

        //mapLayer.save();
        MapLayerMetadata saved_map_layer = mapLayerMetadataService.save(mapLayer);
        if (saved_map_layer==null){
            logger.log(Level.SEVERE, "Json: " + jsonLayerData);
            return errorResponse( Response.Status.BAD_REQUEST, "Failed to save map layer!  Original JSON: ");
        }
        
        // notify user
        userNotificationService.sendNotification(dv_user, wmToken.getCurrentTimestamp(), UserNotification.Type.MAPLAYERUPDATED, dfile.getOwner().getLatestVersion().getId());

        
        return okResponse("map layer object saved!");

        
//        return okResponse("In process");
    }
}
