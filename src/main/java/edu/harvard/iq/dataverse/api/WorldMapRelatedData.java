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
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.MapLayerMetadata;
import edu.harvard.iq.dataverse.MapLayerMetadataServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.UserNotification;
import edu.harvard.iq.dataverse.worldmapauth.TokenApplicationTypeServiceBean;
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.worldmapauth.WorldMapToken;
import edu.harvard.iq.dataverse.worldmapauth.WorldMapTokenServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.inject.Inject;
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
 * 
 * Bean used to communicate information to the WorldMap (currently a Django project)
 * 
 * Within geoconnect, the information sent is validated by the DataverseInfoModel in this project:
 * 
 *  https://github.com/IQSS/shared-dataverse-information
 * 
 */
@Path("worldmap")
public class WorldMapRelatedData extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(WorldMapRelatedData.class.getCanonicalName());

    
    private static final String BASE_PATH = "/api/worldmap/";
    
    public static final String MAP_IT_API_PATH_FRAGMENT =  "map-it/";  
    public static final String MAP_IT_API_PATH = BASE_PATH + MAP_IT_API_PATH_FRAGMENT;
    
    public static final String MAP_IT_API_TOKEN_ONLY_PATH_FRAGMENT =  "map-it-token-only/";  
    public static final String MAP_IT_API_TOKEN_ONLY_PATH = BASE_PATH + MAP_IT_API_TOKEN_ONLY_PATH_FRAGMENT;

    public static final String GET_WORLDMAP_DATAFILE_API_PATH_FRAGMENT =  "datafile/";  
    public static final String GET_WORLDMAP_DATAFILE_API_PATH =  BASE_PATH + GET_WORLDMAP_DATAFILE_API_PATH_FRAGMENT;
    
    
    public static final String UPDATE_MAP_LAYER_DATA_API_PATH_FRAGMENT = "update-layer-metadata/"; 
    public static final String UPDATE_MAP_LAYER_DATA_API_PATH = BASE_PATH + UPDATE_MAP_LAYER_DATA_API_PATH_FRAGMENT;

    public static final String DELETE_MAP_LAYER_DATA_API_PATH_FRAGMENT = "delete-layer-metadata/"; 
    public static final String DELETE_MAP_LAYER_DATA_API_PATH = BASE_PATH + DELETE_MAP_LAYER_DATA_API_PATH_FRAGMENT;
    
    public static final String DELETE_WORLDMAP_TOKEN_PATH_FRAGMENT = "delete-token/"; 
    public static final String DELETE_WORLDMAP_TOKEN_PATH = BASE_PATH + DELETE_WORLDMAP_TOKEN_PATH_FRAGMENT;

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
    AuthenticationServiceBean dataverseUserService;

    @EJB
    PermissionServiceBean permissionService;
    
    @EJB
    SystemConfig systemConfig;
     
    @Inject
    DataverseSession session;
    
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
    
    
    // test call to track down problems
    // http://127.0.0.1:8080/api/worldmap/t/
    @GET
    @Path("t/{identifier}")
    public Response checkWorldMapAPI(@Context HttpServletRequest request
                                    , @PathParam("identifier") int identifier) {

        MapLayerMetadata mapLayerMetadata = this.mapLayerMetadataService.find(new Long(identifier));
        logger.info("mapLayerMetadata retrieved. Try to retrieve image (after 1st time):<br />" + mapLayerMetadata.getMapImageLink());
        
        try {
            this.mapLayerMetadataService.retrieveMapImageForIcon(mapLayerMetadata);
        } catch (IOException ex) {
            logger.info("IOException. Failed to retrieve image. Error:" + ex);
          //  Logger.getLogger(WorldMapRelatedData.class.getName()).log(Level.SEVERE, null, ex);
        }catch(Exception e2){
            logger.info("Failed to retrieve image. Error:" + e2);
        }
        
        return ok( "Looks good " + identifier + " " + mapLayerMetadata.getLayerName());
    }
    
    
    
    
    @GET    
    @Path( MAP_IT_API_PATH_FRAGMENT + "{datafile_id}/{dvuser_id}")
    public Response mapDataFile(@Context HttpServletRequest request
                                , @PathParam("datafile_id") Long datafile_id
                                , @PathParam("dvuser_id") Long dvuser_id){ 
    
        return this.mapDataFileTokenOnlyOption(request, datafile_id, dvuser_id, false);
    }
    
    @GET
    @Path(MAP_IT_API_TOKEN_ONLY_PATH_FRAGMENT + "{datafile_id}/{dvuser_id}")
    public Response getMapDataFileToken(@Context HttpServletRequest request
                                , @PathParam("datafile_id") Long datafile_id
                                , @PathParam("dvuser_id") Long dvuser_id){ 
    
        //request.
        return this.mapDataFileTokenOnlyOption(request, datafile_id, dvuser_id, true);
    }
    
   
    /*
        Link used within Dataverse for MapIt button
        Sends file link to GeoConnect using a Redirect
    
    */
    //@GET    
    //@Path( MAP_IT_API_PATH_FRAGMENT + "token-option/{datafile_id}/{dvuser_id}/{token_only}")
    private Response mapDataFileTokenOnlyOption(@Context HttpServletRequest request
                                ,  Long datafile_id
                                , Long dvuser_id
                                , boolean tokenOnly
    ){ 
        
        logger.log(Level.INFO, "mapDataFile datafile_id: {0}", datafile_id);
        logger.log(Level.INFO, "mapDataFile dvuser_id: {0}", dvuser_id);
        
        AuthenticatedUser user = null;

        if (session != null) {
            if (session.getUser() != null) {
                if (session.getUser().isAuthenticated()) {
                    user = (AuthenticatedUser) session.getUser();
                } 
            }
        } 
        if (user==null){
            return error(Response.Status.FORBIDDEN, "Not logged in");
        }
        
        
        if (true){
            //return okResponse( "Looks good " + datafile_id);
           //tokenAppServiceBean.getGeoConnectApplication();           
           //return okResponse("Currently deactivated (mapDataFile)");
        }

        // Check if the user exists
        AuthenticatedUser dvUser = dataverseUserService.findByID(dvuser_id);
	if ( dvUser == null ){
            return error(Response.Status.FORBIDDEN, "Invalid user");
        }

        // Check if this file exists
        DataFile dfile = dataFileService.find(datafile_id);
        if (dfile==null){
           return error(Response.Status.NOT_FOUND, "DataFile not found for id: " + datafile_id);
        }
        
        /*
            Is the dataset public?
        */
        if (!dfile.getOwner().isReleased()){
           return error(Response.Status.FORBIDDEN, "Mapping is only permitted for public datasets/files");
            
        }
        
        // Does this user have permission to edit metadata for this file?    
        if (!permissionService.request(createDataverseRequest(dvUser)).on(dfile.getOwner()).has(Permission.EditDataset)){
           String errMsg = "The user does not have permission to edit metadata for this file.";
           return error(Response.Status.FORBIDDEN, errMsg);
        }
        
        WorldMapToken token = tokenServiceBean.getNewToken(dfile, dvUser);

        if (tokenOnly){
            // Return only the token in a JSON object
            final JsonObjectBuilder jsonInfo = Json.createObjectBuilder();
            jsonInfo.add(WorldMapToken.GEOCONNECT_TOKEN_KEY, token.getToken()); 
            return ok(jsonInfo);
        }
            
        // Redirect to geoconnect url
 //       String callback_url = this.getServerNamePort(request) + GET_WORLDMAP_DATAFILE_API_PATH + dfile.getId();
        String callback_url = this.getServerNamePort(request) + GET_WORLDMAP_DATAFILE_API_PATH;
        String redirect_url_str = token.getApplication().getMapitLink() + "/" + token.getToken() + "/?cb=" +  URLEncoder.encode(callback_url);
        //String redirect_url_str = TokenApplicationType.LOCAL_DEV_MAPIT_LINK + "/" +  token.getToken() + "/?cb=" +  URLEncoder.encode(callback_url);
        URI redirect_uri;
        
        try {
            redirect_uri = new URI(redirect_url_str);
        } catch (URISyntaxException ex) {
             return error(Response.Status.NOT_FOUND, "Faile to create URI from: " + redirect_url_str);
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
        
        String http_prefix = "https://";
        if (serverName.contains("localhost")){
            http_prefix = "http://";
        }else if(serverName.contains("127.0.0.1")){
            http_prefix = "http://";
        }
        if (portNumber==80){
           return http_prefix + serverName;
        }
        return http_prefix + serverName + ":" + portNumber;
               
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
        logger.info("retrieveTokenValueFromJson.jsonTokenInfo:"+ jsonTokenInfo);
        logger.info("token keys: " + jsonTokenInfo.keySet());
        logger.info("key looked for: " + WorldMapToken.GEOCONNECT_TOKEN_KEY);
        if (!jsonTokenInfo.containsKey(WorldMapToken.GEOCONNECT_TOKEN_KEY)){
            logger.warning("Token not found.  Permission denied.");
            return null;
            //return errorResponse( Response.Status.BAD_REQUEST, "Permission denied");
        }
        
                
        //String worldmapTokenParam = worldmapTokenObject.toString();                
        String worldmapTokenParam = jsonTokenInfo.getString(WorldMapToken.GEOCONNECT_TOKEN_KEY);       
        if (worldmapTokenParam==null){      // shouldn't happen
            logger.warning("worldmapTokenParam is null when .toString() called.  Permission denied.");
            return null;
            //return errorResponse(Response.Status.UNAUTHORIZED, "No access.");
        }
       //logger.info("worldmapTokenParam:"+ worldmapTokenParam);

       //logger.info("worldmapTokenParam length:"+ worldmapTokenParam.length());
        if (!(worldmapTokenParam.length()==64)){
            logger.warning("worldmapTokenParam not length 64.  Permission denied.");
            return null;
           // return errorResponse(Response.Status.UNAUTHORIZED, "No access.");            
        } 
        return worldmapTokenParam;
    }
    
    
    /**
     * Retrieve FileMetadata for Use by WorldMap.
     *  This includes information about the DataFile, Dataset, DatasetVersion, and Dataverse
     * 
     * @param jsonTokenData
     * @param request
     * @return 
     */
    @POST
    @Path(GET_WORLDMAP_DATAFILE_API_PATH_FRAGMENT)// + "{worldmap_token}")
    public Response getWorldMapDatafileInfo(String jsonTokenData, @Context HttpServletRequest request){//, @PathParam("worldmap_token") String worldmapTokenParam) {   
        if (true){
           //return okResponse("Currently deactivated");

           // return okResponse("remote server: " + request.getRemoteAddr());
        }
        logger.info("API call: getWorldMapDatafileInfo");
        //----------------------------------
        // Auth check: Parse the json message and check for a valid GEOCONNECT_TOKEN_KEY and GEOCONNECT_TOKEN_VALUE
        //   -- For testing, the GEOCONNECT_TOKEN_VALUE will be dynamic, found in the db
        //----------------------------------
        logger.info("(1) jsonTokenData: " + jsonTokenData);
        // Parse JSON 
        JsonObject jsonTokenInfo;
        try ( StringReader rdr = new StringReader(jsonTokenData) ) {
            jsonTokenInfo = Json.createReader(rdr).readObject();
        } catch ( JsonParsingException jpe ) {
            logger.log(Level.SEVERE, "Json: " + jsonTokenData);
            return error( Response.Status.BAD_REQUEST, "Error parsing Json: " + jpe.getMessage() );
        }
        logger.info("(1a) jsonTokenInfo: " + jsonTokenInfo);
        
        // Retrieve token string
        String worldmapTokenParam = this.retrieveTokenValueFromJson(jsonTokenInfo);
        logger.info("(1b) token from JSON: " + worldmapTokenParam);
        if (worldmapTokenParam==null){
            return error(Response.Status.BAD_REQUEST, "Token not found in JSON request.");
        }

        // Retrieve WorldMapToken and make sure it is valid
        //
        WorldMapToken wmToken = tokenServiceBean.retrieveAndRefreshValidToken(worldmapTokenParam);
        logger.info("(2) token retrieved from db: " + wmToken);

        if (wmToken==null){
            return error(Response.Status.UNAUTHORIZED, "No access. Invalid token.");
        }
        
        // Make sure the token's User still has permissions to access the file
        //
        logger.info("(3) check permissions");
        if (!(tokenServiceBean.canTokenUserEditFile(wmToken))){
            tokenServiceBean.expireToken(wmToken);
            return error(Response.Status.UNAUTHORIZED, "No access. Invalid token.");
        }


        // (1) Retrieve token connected data: DataverseUser, DataFile
        //
        // Make sure token user and file are still available
        //
        AuthenticatedUser dvUser = wmToken.getDataverseUser();
        if (dvUser == null) {
            return error(Response.Status.NOT_FOUND, "DataverseUser not found for token");
        }
        DataFile dfile = wmToken.getDatafile();
        if (dfile  == null) {
            return error(Response.Status.NOT_FOUND, "DataFile not found for token");
        }
        
        
        // (1a) Retrieve FileMetadata
        FileMetadata dfile_meta = dfile.getFileMetadata();
        if (dfile_meta==null){
           return error(Response.Status.NOT_FOUND, "FileMetadata not found");
        }
        
        // (2) Now get the dataset and the latest DatasetVersion
        Dataset dset = dfile.getOwner();
        if (dset==null){
            return error(Response.Status.NOT_FOUND, "Owning Dataset for this DataFile not found");
        }
        
        // (2a) latest DatasetVersion
        // !! How do you check if the lastest version has this specific file?
        //
        DatasetVersion dset_version = dset.getLatestVersion();
        if (dset_version==null){
            return error(Response.Status.NOT_FOUND, "Latest DatasetVersion for this DataFile not found");
        }
        
        // (3) get Dataverse
        Dataverse dverse = dset.getOwner();
        if (dverse==null){
            return error(Response.Status.NOT_FOUND, "Dataverse for this DataFile's Dataset not found");
        }
        
        // (4) Roll it all up in a JSON response
        final JsonObjectBuilder jsonData = Json.createObjectBuilder();
        
        //------------------------------------
        // Type of file, currently:
        //  - shapefile or 
        //  - tabular file (.tab) with geospatial tag
        //------------------------------------
        if (dfile.isShapefileType()){
            jsonData.add("mapping_type", "shapefile");
        }else if (dfile.isTabularData()){
            jsonData.add("mapping_type", "tabular");        
        }else{
            logger.log(Level.SEVERE, "This was neither a Shapefile nor a Tabular data file.  DataFile id: " + dfile.getId());
            return error( Response.Status.BAD_REQUEST, "Sorry! This file does not have mapping data. Please contact the Dataverse administrator. DataFile id: " + dfile.getId()); 
        }
    
        
        //------------------------------------
        // DataverseUser Info
        //------------------------------------
        jsonData.add("dv_user_id", dvUser.getId());
        jsonData.add("dv_username", dvUser.getUserIdentifier()); 
        jsonData.add("dv_user_email", dvUser.getEmail());
                
        //------------------------------------
        // Dataverse URLs to this server 
        //------------------------------------
        String serverName =  this.getServerNamePort(request);
        jsonData.add("return_to_dataverse_url", dset_version.getReturnToDatasetURL(serverName, dset));
        jsonData.add("datafile_download_url", dfile.getMapItFileDownloadURL(serverName));

        //------------------------------------
        // Dataverse
        //------------------------------------
        //jsonData.add("dataverse_installation_name", "Harvard Dataverse"); // todo / fix
        jsonData.add("dataverse_installation_name",  systemConfig.getDataverseSiteUrl()); // is this enough to distinguish a dataverse installation?

        jsonData.add("dataverse_id", dverse.getId());      
        jsonData.add("dataverse_name", dverse.getName());

        String dataverseDesc = dverse.getDescription();
        if (dataverseDesc == null || dataverseDesc.equalsIgnoreCase("")){
            dataverseDesc = "";
        }        
        jsonData.add("dataverse_description", dataverseDesc);

        //------------------------------------
        // Dataset Info
        //------------------------------------
        jsonData.add("dataset_id", dset.getId());

        //------------------------------------
        // DatasetVersion Info
        //------------------------------------
        jsonData.add("dataset_version_id", dset_version.getId());   // database id
        jsonData.add("dataset_semantic_version", dset_version.getSemanticVersion());  // major/minor version number, e.g. 3.1
        
        jsonData.add("dataset_name", dset_version.getTitle());
        jsonData.add("dataset_citation", dset_version.getCitation(true));
        
        jsonData.add("dataset_description", "");  // Need to fix to/do

        jsonData.add("dataset_is_public", dset_version.isReleased());
                
        //------------------------------------
        // DataFile/FileMetaData Info
        //------------------------------------
        jsonData.add("datafile_id", dfile.getId());
        jsonData.add("datafile_label", dfile_meta.getLabel());
        //jsonData.add("filename", dfile_meta.getLabel());
        jsonData.add("datafile_expected_md5_checksum", dfile.getChecksumValue());
        Long fsize = dfile.getFilesize();
        if (fsize == null){
            fsize= new Long(-1);
        }
            
        jsonData.add("datafile_filesize", fsize); 
        jsonData.add("datafile_content_type", dfile.getContentType());
        jsonData.add("datafile_create_datetime", dfile.getCreateDate().toString());
        
        return ok(jsonData);
 
    }

    
   
    /*
        For WorldMap/GeoConnect Usage
        Create/Updated a MapLayerMetadata object for a given Datafile id
        
        Example of jsonLayerData String:
        {
             "layerName": "geonode:boston_census_blocks_zip_cr9"
            , "layerLink": "http://localhost:8000/data/geonode:boston_census_blocks_zip_cr9"
            , "embedMapLink": "http://localhost:8000/maps/embed/?layer=geonode:boston_census_blocks_zip_cr9"
            , "worldmapUsername": "dv_pete"
        }
    */
    @POST
    @Path(UPDATE_MAP_LAYER_DATA_API_PATH_FRAGMENT)
    public Response updateWorldMapLayerData(String jsonLayerData){
        
         //----------------------------------
        // Auth check: Parse the json message and check for a valid GEOCONNECT_TOKEN_KEY and GEOCONNECT_TOKEN_VALUE
        //   -- For testing, the GEOCONNECT_TOKEN_VALUE will be dynamic, found in the db
        //----------------------------------
        if (jsonLayerData==null){
            logger.log(Level.SEVERE, "jsonLayerData is null");
            return error( Response.Status.BAD_REQUEST, "No JSON data");
        }

        // (1) Parse JSON 
        //
        JsonObject jsonInfo;
        try ( StringReader rdr = new StringReader(jsonLayerData) ) {
            jsonInfo = Json.createReader(rdr).readObject();
        } catch ( JsonParsingException jpe ) {
            logger.log(Level.SEVERE, "Json: " + jsonLayerData);
            return error( Response.Status.BAD_REQUEST, "Error parsing Json: " + jpe.getMessage() );
        }
        
        // Retrieve token string
        String worldmapTokenParam = this.retrieveTokenValueFromJson(jsonInfo);
        if (worldmapTokenParam==null){
            return error(Response.Status.BAD_REQUEST, "Token not found in JSON request.");
        }

        // Retrieve WorldMapToken and make sure it is valid
        //
        WorldMapToken wmToken = this.tokenServiceBean.retrieveAndRefreshValidToken(worldmapTokenParam);
        if (wmToken==null){
            return error(Response.Status.UNAUTHORIZED, "No access. Invalid token.");
        }

        // Make sure the token's User still has permissions to access the file
        //
        if (!(tokenServiceBean.canTokenUserEditFile(wmToken))){
            tokenServiceBean.expireToken(wmToken);
            return error(Response.Status.UNAUTHORIZED, "No access. Invalid token.");
        }

        
        // (2) Make sure the json message has all of the required attributes
        //
        for (String attr : MapLayerMetadata.MANDATORY_JSON_FIELDS ){
            if (!jsonInfo.containsKey(attr)){
                return error( Response.Status.BAD_REQUEST, "Error parsing Json.  Key not found [" + attr + "]\nRequired keys are: " + MapLayerMetadata.MANDATORY_JSON_FIELDS  );
            }
        }
        
        // (3) Attempt to retrieve DataverseUser      
        AuthenticatedUser dvUser = wmToken.getDataverseUser();
        if (dvUser == null) {
            return error(Response.Status.NOT_FOUND, "DataverseUser not found for token");
        }
        
        // (4) Attempt to retrieve DataFile      
        DataFile dfile = wmToken.getDatafile();
        if (dfile==null){
            return error(Response.Status.NOT_FOUND, "DataFile not found for token");
         }

        // check permissions!
        if (!permissionService.request( createDataverseRequest(dvUser) ).on(dfile.getOwner()).has(Permission.EditDataset)){
           String errMsg = "The user does not have permission to edit metadata for this file. (MapLayerMetadata)";
           return error(Response.Status.FORBIDDEN, errMsg);
        }
        
        
        // (5) See if a MapLayerMetadata already exists
        //  
        MapLayerMetadata mapLayerMetadata = this.mapLayerMetadataService.findMetadataByDatafile(dfile);
        if (mapLayerMetadata==null){
            // Create a new mapLayerMetadata object
            mapLayerMetadata = new MapLayerMetadata();
        }

        // Create/Update new MapLayerMetadata object and save it
        mapLayerMetadata.setDataFile(dfile);
        mapLayerMetadata.setDataset(dfile.getOwner());
        mapLayerMetadata.setLayerName(jsonInfo.getString("layerName"));
        mapLayerMetadata.setLayerLink(jsonInfo.getString("layerLink"));
        mapLayerMetadata.setEmbedMapLink(jsonInfo.getString("embedMapLink"));
        mapLayerMetadata.setWorldmapUsername(jsonInfo.getString("worldmapUsername"));
        if (jsonInfo.containsKey("mapImageLink")){
            mapLayerMetadata.setMapImageLink(jsonInfo.getString("mapImageLink"));
        }
      
        // If this was a tabular join set the attributes:
        //  - isJoinLayer
        //  - joinDescription
        //
        String joinDescription = jsonInfo.getString("joinDescription", null);
        if ((joinDescription == null) || (joinDescription.equals(""))){
            mapLayerMetadata.setIsJoinLayer(true);
            mapLayerMetadata.setJoinDescription(joinDescription);
        }else{
            mapLayerMetadata.setIsJoinLayer(false);
            mapLayerMetadata.setJoinDescription(null);            
        }
            
        // Set the mapLayerLinks 
        //
        String mapLayerLinks = jsonInfo.getString("mapLayerLinks", null);
        if ((mapLayerLinks == null) || (mapLayerLinks.equals(""))){
            mapLayerMetadata.setMapLayerLinks(null);                        
        }else{
            mapLayerMetadata.setMapLayerLinks(mapLayerLinks);            
        }
        
        
        //mapLayer.save();
        MapLayerMetadata savedMapLayerMetadata = mapLayerMetadataService.save(mapLayerMetadata);
        if (savedMapLayerMetadata==null){
            logger.log(Level.SEVERE, "Json: " + jsonLayerData);
            return error( Response.Status.BAD_REQUEST, "Failed to save map layer!  Original JSON: ");
        }
        
        
        
        // notify user
        userNotificationService.sendNotification(dvUser, wmToken.getCurrentTimestamp(), UserNotification.Type.MAPLAYERUPDATED, dfile.getOwner().getLatestVersion().getId());

        // ------------------------------------------
        // Retrieve a PNG representation from WorldMap
        // ------------------------------------------
        try {
                logger.info("retrieveMapImageForIcon");
                this.mapLayerMetadataService.retrieveMapImageForIcon(savedMapLayerMetadata);
        } catch (IOException ex) {
                logger.severe("Failed to retrieve image from WorldMap server");
                Logger.getLogger(WorldMapRelatedData.class.getName()).log(Level.SEVERE, null, ex);
        }  
        
        return ok("map layer object saved!");

    }  // end updateWorldMapLayerData
    
    
    
    
    /*
        For WorldMap/GeoConnect Usage
        Delete MayLayerMetadata object for a given Datafile

        POST params
        {
           "GEOCONNECT_TOKEN": "-- some 64 char token which contains a link to the DataFile --"
        }
    */
    @POST
    @Path(DELETE_MAP_LAYER_DATA_API_PATH_FRAGMENT)
    public Response deleteWorldMapLayerData(String jsonData){
        
        /*----------------------------------
            Parse the json message.
            - Auth check: GEOCONNECT_TOKEN
        //----------------------------------*/
        if (jsonData==null){
            logger.log(Level.SEVERE, "jsonData is null");
            return error( Response.Status.BAD_REQUEST, "No JSON data");
        }
        // (1) Parse JSON 
        //
        JsonObject jsonInfo;
        try ( StringReader rdr = new StringReader(jsonData) ) {
            jsonInfo = Json.createReader(rdr).readObject();
        } catch ( JsonParsingException jpe ) {
            logger.log(Level.SEVERE, "Json: " + jsonData);
            return error( Response.Status.BAD_REQUEST, "Error parsing Json: " + jpe.getMessage() );
        }
        
        // (2) Retrieve token string
        String worldmapTokenParam = this.retrieveTokenValueFromJson(jsonInfo);
        if (worldmapTokenParam==null){
            return error(Response.Status.BAD_REQUEST, "Token not found in JSON request.");
        }

        // (3) Retrieve WorldMapToken and make sure it is valid
        //
        WorldMapToken wmToken = this.tokenServiceBean.retrieveAndRefreshValidToken(worldmapTokenParam);
        if (wmToken==null){
            return error(Response.Status.UNAUTHORIZED, "No access. Invalid token.");
        }

        // (4) Make sure the token's User still has permissions to access the file
        //
        if (!(tokenServiceBean.canTokenUserEditFile(wmToken))){
            tokenServiceBean.expireToken(wmToken);
            return error(Response.Status.UNAUTHORIZED, "No access. Invalid token.");
        }

        // (5) Attempt to retrieve DataFile and mapLayerMetadata   
        DataFile dfile = wmToken.getDatafile();
        MapLayerMetadata mapLayerMetadata = this.mapLayerMetadataService.findMetadataByDatafile(dfile);
        if (mapLayerMetadata==null){
            return error(Response.Status.EXPECTATION_FAILED, "No map layer metadata found.");
        }
        
       // (6) Delete the mapLayerMetadata
       //   (note: permissions checked here for a second time by the mapLayerMetadataService call)
       //
       if (!(this.mapLayerMetadataService.deleteMapLayerMetadataObject(mapLayerMetadata, wmToken.getDataverseUser()))){
            return error(Response.Status.PRECONDITION_FAILED, "Failed to delete layer");               
       };
       
       return ok("Map layer metadata deleted.");
        
    }  // end deleteWorldMapLayerData

   /*
        For WorldMap/GeoConnect Usage
        Explicitly expire a WorldMap token, removing token from the database

        POST params
        {
           "GEOCONNECT_TOKEN": "-- some 64 char token which contains a link to the DataFile --"
        }
    */
    @POST
    @Path(DELETE_WORLDMAP_TOKEN_PATH_FRAGMENT)
    public Response deleteWorldMapToken(String jsonData){
        
        /*----------------------------------
            Parse the json message.
            - Auth check: GEOCONNECT_TOKEN
        //----------------------------------*/
        if (jsonData==null){
            logger.log(Level.SEVERE, "jsonData is null");
            return error( Response.Status.BAD_REQUEST, "No JSON data");
        }
        // (1) Parse JSON 
        //
        JsonObject jsonInfo;
        try ( StringReader rdr = new StringReader(jsonData) ) {
            jsonInfo = Json.createReader(rdr).readObject();
        } catch ( JsonParsingException jpe ) {
            logger.log(Level.SEVERE, "Json: " + jsonData);
            return error( Response.Status.BAD_REQUEST, "Error parsing Json: " + jpe.getMessage() );
        }
        
        // (2) Retrieve token string
        String worldmapTokenParam = this.retrieveTokenValueFromJson(jsonInfo);
        if (worldmapTokenParam==null){
            return error(Response.Status.BAD_REQUEST, "Token not found in JSON request.");
        }

        // (3) Retrieve WorldMapToken
        //
        WorldMapToken wmToken = this.tokenServiceBean.findByName(worldmapTokenParam);
        if (wmToken==null){
            return error(Response.Status.NOT_FOUND, "Token not found.");
        }

        // (4) Delete the token
        //
        tokenServiceBean.deleteToken(wmToken);               
        return ok("Token has been deleted.");
        
    }  // end deleteWorldMapLayerData    
    
} // end class
