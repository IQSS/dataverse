package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.MapLayerMetadataServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.api.annotations.ApiWriteOperation;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.citation.CitationFactory;
import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.notification.UserNotificationService;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.MapLayerMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.TermsOfUseType;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.worldmap.WorldMapToken;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.worldmapauth.WorldMapTokenServiceBean;
import org.apache.commons.lang3.StringUtils;

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
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author raprasad
 * <p>
 * Bean used to communicate information to the WorldMap (currently a Django project)
 * <p>
 * Within geoconnect, the information sent is validated by the DataverseInfoModel in this project:
 * <p>
 * https://github.com/IQSS/shared-dataverse-information
 * @todo Audit these methods to ensure they don't pose a security risk. Consider
 * changing the installer so that like "admin" this "worldmap" endpoint is
 * blocked out of the box. See
 * http://guides.dataverse.org/en/4.6.1/installation/config.html#blocking-api-endpoints
 */
@Path("worldmap")
public class WorldMapRelatedData extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(WorldMapRelatedData.class.getCanonicalName());


    private static final String BASE_PATH = "/api/worldmap/";

    public static final String MAP_IT_API_PATH_FRAGMENT = "map-it/";

    public static final String MAP_IT_API_TOKEN_ONLY_PATH_FRAGMENT = "map-it-token-only/";

    public static final String GET_WORLDMAP_DATAFILE_API_PATH_FRAGMENT = "datafile/";
    public static final String GET_WORLDMAP_DATAFILE_API_PATH = BASE_PATH + GET_WORLDMAP_DATAFILE_API_PATH_FRAGMENT;


    public static final String UPDATE_MAP_LAYER_DATA_API_PATH_FRAGMENT = "update-layer-metadata/";

    public static final String DELETE_MAP_LAYER_DATA_API_PATH_FRAGMENT = "delete-layer-metadata/";

    public static final String DELETE_WORLDMAP_TOKEN_PATH_FRAGMENT = "delete-token/";

    @EJB
    MapLayerMetadataServiceBean mapLayerMetadataService;

    @EJB
    DataFileServiceBean dataFileService;

    @EJB
    UserNotificationService userNotificationService;

    @EJB
    WorldMapTokenServiceBean tokenServiceBean;

    @EJB
    AuthenticationServiceBean dataverseUserService;

    @EJB
    PermissionServiceBean permissionService;

    @EJB
    SystemConfig systemConfig;

    @Inject
    DataverseSession session;

    @Inject
    CitationFactory citationFactory;

    // -------------------- LOGIC --------------------

    // test call to track down problems
    // http://127.0.0.1:8080/api/worldmap/t/
    @GET
    @Path("t/{identifier}")
    public Response checkWorldMapAPI(@Context HttpServletRequest request, @PathParam("identifier") int identifier) {

        MapLayerMetadata mapLayerMetadata = mapLayerMetadataService.find((long) identifier);
        logger.info("mapLayerMetadata retrieved. Try to retrieve image (after 1st time):<br />"
                + mapLayerMetadata.getMapImageLink());

        try {
            mapLayerMetadataService.retrieveMapImageForIcon(mapLayerMetadata);
        } catch (IOException ioe) {
            logger.info("IOException. Failed to retrieve image. Error:" + ioe);
        } catch (Exception e) {
            logger.info("Failed to retrieve image. Error:" + e);
        }

        return ok("Looks good " + identifier + " " + mapLayerMetadata.getLayerName());
    }

    @GET
    @Path(MAP_IT_API_PATH_FRAGMENT + "{datafile_id}/{dvuser_id}")
    public Response mapDataFile(@Context HttpServletRequest request, @PathParam("datafile_id") Long datafileId,
                                @PathParam("dvuser_id") Long dvUserId) {
        return mapDataFileTokenOnlyOption(request, datafileId, dvUserId, false);
    }

    @GET
    @Path(MAP_IT_API_TOKEN_ONLY_PATH_FRAGMENT + "{datafile_id}/{dvuser_id}")
    public Response getMapDataFileToken(@Context HttpServletRequest request, @PathParam("datafile_id") Long datafileId,
                                        @PathParam("dvuser_id") Long dvUserId) {
        return mapDataFileTokenOnlyOption(request, datafileId, dvUserId, true);
    }

    /**
     * Parse json looking for the GEOCONNECT_TOKEN_KEY.
     * Make sure that the string itself is not null and 64 chars
     */
    private String retrieveTokenValueFromJson(JsonObject jsonTokenInfo) {
        if (jsonTokenInfo == null) {
            return null;
        }
        logger.info("retrieveTokenValueFromJson.jsonTokenInfo:" + jsonTokenInfo);
        logger.info("token keys: " + jsonTokenInfo.keySet());
        logger.info("key looked for: " + WorldMapToken.GEOCONNECT_TOKEN_KEY);
        if (!jsonTokenInfo.containsKey(WorldMapToken.GEOCONNECT_TOKEN_KEY)) {
            logger.warning("Token not found. Permission denied.");
            return null;
        }

        String worldmapTokenParam = jsonTokenInfo.getString(WorldMapToken.GEOCONNECT_TOKEN_KEY);
        if (worldmapTokenParam == null) {      // shouldn't happen
            logger.warning("worldmapTokenParam is null when .toString() called. Permission denied.");
            return null;
        }
        if (!(worldmapTokenParam.length() == 64)) {
            logger.warning("worldmapTokenParam not length 64. Permission denied.");
            return null;
        }
        return worldmapTokenParam;
    }

    /**
     * Retrieve FileMetadata for Use by WorldMap.
     * This includes information about the DataFile, Dataset, DatasetVersion, and Dataverse
     */
    @POST
    @Path(GET_WORLDMAP_DATAFILE_API_PATH_FRAGMENT)
    public Response getWorldMapDatafileInfo(String jsonTokenData, @Context HttpServletRequest request) {
        logger.info("API call: getWorldMapDatafileInfo");
        //----------------------------------
        // Auth check: Parse the json message and check for a valid GEOCONNECT_TOKEN_KEY and GEOCONNECT_TOKEN_VALUE
        //   -- For testing, the GEOCONNECT_TOKEN_VALUE will be dynamic, found in the db
        //----------------------------------
        logger.info("(1) jsonTokenData: " + jsonTokenData);

        JsonObject jsonTokenInfo;
        try (StringReader rdr = new StringReader(jsonTokenData)) {
            jsonTokenInfo = Json.createReader(rdr).readObject();
        } catch (JsonParsingException jpe) {
            logger.log(Level.SEVERE, "Json: " + jsonTokenData);
            return badRequest("Error parsing Json: " + jpe.getMessage());
        }
        logger.info("(1a) jsonTokenInfo: " + jsonTokenInfo);

        // Retrieve token string
        String worldmapTokenParam = retrieveTokenValueFromJson(jsonTokenInfo);
        logger.info("(1b) token from JSON: " + worldmapTokenParam);
        if (worldmapTokenParam == null) {
            return badRequest("Token not found in JSON request.");
        }

        // Retrieve WorldMapToken and make sure it is valid
        WorldMapToken wmToken = tokenServiceBean.retrieveAndRefreshValidToken(worldmapTokenParam);
        logger.info("(2) token retrieved from db: " + wmToken);

        if (wmToken == null) {
            return unauthorized("No access. Invalid token.");
        }

        // Make sure the token's User still has permissions to access the file
        logger.info("(3) check permissions");
        if (!(tokenServiceBean.canTokenUserEditFile(wmToken))) {
            tokenServiceBean.expireToken(wmToken);
            return unauthorized("No access. Invalid token.");
        }

        // (1) Retrieve token connected data: DataverseUser, DataFile
        //
        // Make sure token user and file are still available
        AuthenticatedUser dvUser = wmToken.getDataverseUser();
        if (dvUser == null) {
            return notFound("DataverseUser not found for token");
        }
        DataFile dfile = wmToken.getDatafile();
        if (dfile == null) {
            return notFound("DataFile not found for token");
        }

        // (1a) Retrieve FileMetadata
        FileMetadata dfileMeta = dfile.getFileMetadata();
        if (dfileMeta == null) {
            return notFound("FileMetadata not found");
        }

        // (2) Now get the dataset and the latest DatasetVersion
        Dataset dset = dfile.getOwner();
        if (dset == null) {
            return notFound("Owning Dataset for this DataFile not found");
        }

        // (2a) latest DatasetVersion
        // !! How do you check if the lastest version has this specific file?
        DatasetVersion dset_version = dset.getLatestVersion();
        if (dset_version == null) {
            return notFound("Latest DatasetVersion for this DataFile not found");
        }

        // (3) get Dataverse
        Dataverse dverse = dset.getOwner();
        if (dverse == null) {
            return notFound("Dataverse for this DataFile's Dataset not found");
        }

        // (4) Roll it all up in a JSON response
        final JsonObjectBuilder jsonData = Json.createObjectBuilder();

        // Type of file, currently:
        //  - shapefile or
        //  - tabular file (.tab) with geospatial tag
        if (dfile.isShapefileType()) {
            jsonData.add("mapping_type", "shapefile");
        } else if (dfile.isTabularData()) {
            jsonData.add("mapping_type", "tabular");
        } else {
            logger.log(Level.SEVERE, "This was neither a Shapefile nor a Tabular data file.  DataFile id: " + dfile.getId());
            return badRequest("Sorry! This file does not have mapping data. Please contact the Dataverse " +
                    "administrator. DataFile id: " + dfile.getId());
        }

        // DataverseUser Info
        jsonData.add("dv_user_id", dvUser.getId());
        jsonData.add("dv_username", dvUser.getUserIdentifier());
        jsonData.add("dv_user_email", dvUser.getEmail());

        // Dataverse URLs to this server
        String serverName = systemConfig.getDataverseSiteUrl();
        jsonData.add("return_to_dataverse_url", getReturnToFilePageURL(serverName, dset, dfile, dset_version));
        jsonData.add("datafile_download_url", dfile.getMapItFileDownloadURL(serverName));

        // Dataverse – is this enough to distinguish a dataverse installation?
        jsonData.add("dataverse_installation_name", systemConfig.getDataverseSiteUrl());

        jsonData.add("dataverse_id", dverse.getId());
        jsonData.add("dataverse_name", dverse.getName());

        String dataverseDesc = dverse.getDescription();
        jsonData.add("dataverse_description", StringUtils.isNotEmpty(dataverseDesc) ? dataverseDesc : StringUtils.EMPTY);

        // Dataset Info
        jsonData.add("dataset_id", dset.getId());

        // DatasetVersion Info
        jsonData.add("dataset_version_id", dset_version.getId()); // database id
        jsonData.add("dataset_semantic_version", dset_version.getSemanticVersion()); // major/minor ver. num., e.g. 3.1

        jsonData.add("dataset_name", dset_version.getParsedTitle());
        jsonData.add("dataset_citation", citationFactory.create(dset_version).toString(true));

        jsonData.add("dataset_description", ""); // Need to fix to/do

        jsonData.add("dataset_is_public", dset_version.isReleased());

        // DataFile/FileMetaData Info
        jsonData.add("datafile_id", dfile.getId());
        jsonData.add("datafile_label", dfileMeta.getLabel());
        jsonData.add("datafile_expected_md5_checksum", dfile.getChecksumValue());

        long fsize = dfile.getFilesize();
        jsonData.add("datafile_filesize", fsize);
        jsonData.add("datafile_content_type", dfile.getContentType());
        jsonData.add("datafile_create_datetime", dfile.getCreateDate().toString());

        // restriction status of the DataFile
        jsonData.add("datafile_is_restricted", dfileMeta.getTermsOfUse().getTermsOfUseType() == TermsOfUseType.RESTRICTED);

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
    @ApiWriteOperation
    @Path(UPDATE_MAP_LAYER_DATA_API_PATH_FRAGMENT)
    public Response updateWorldMapLayerData(String jsonLayerData) {

        // Auth check: Parse the json message and check for a valid GEOCONNECT_TOKEN_KEY and GEOCONNECT_TOKEN_VALUE
        //   -- For testing, the GEOCONNECT_TOKEN_VALUE will be dynamic, found in the db
        if (jsonLayerData == null) {
            logger.log(Level.SEVERE, "jsonLayerData is null");
            return badRequest("No JSON data");
        }

        // (1) Parse JSON
        JsonObject jsonInfo;
        try (StringReader rdr = new StringReader(jsonLayerData)) {
            jsonInfo = Json.createReader(rdr).readObject();
        } catch (JsonParsingException jpe) {
            logger.log(Level.SEVERE, "Json: " + jsonLayerData);
            return badRequest("Error parsing Json: " + jpe.getMessage());
        }

        // Retrieve token string
        String worldmapTokenParam = retrieveTokenValueFromJson(jsonInfo);
        if (worldmapTokenParam == null) {
            return badRequest("Token not found in JSON request.");
        }

        // Retrieve WorldMapToken and make sure it is valid
        WorldMapToken wmToken = tokenServiceBean.retrieveAndRefreshValidToken(worldmapTokenParam);
        if (wmToken == null) {
            return unauthorized("No access. Invalid token.");
        }

        // Make sure the token's User still has permissions to access the file
        if (!tokenServiceBean.canTokenUserEditFile(wmToken)) {
            tokenServiceBean.expireToken(wmToken);
            return unauthorized("No access. Invalid token.");
        }

        // (2) Make sure the json message has all of the required attributes
        for (String attr : MapLayerMetadata.MANDATORY_JSON_FIELDS) {
            if (!jsonInfo.containsKey(attr)) {
                return badRequest("Error parsing Json.  Key not found [" + attr + "]\nRequired keys are: "
                        + MapLayerMetadata.MANDATORY_JSON_FIELDS);
            }
        }

        // (3) Attempt to retrieve DataverseUser
        AuthenticatedUser dvUser = wmToken.getDataverseUser();
        if (dvUser == null) {
            return notFound("DataverseUser not found for token");
        }

        // (4) Attempt to retrieve DataFile
        DataFile dfile = wmToken.getDatafile();
        if (dfile == null) {
            return notFound("DataFile not found for token");
        }

        // check permissions!
        if (!permissionService.requestOn(createDataverseRequest(dvUser), dfile.getOwner()).has(Permission.EditDataset)) {
            String errMsg = "The user does not have permission to edit metadata for this file. (MapLayerMetadata)";
            return forbidden(errMsg);
        }

        // (5) See if a MapLayerMetadata already exists
        //
        MapLayerMetadata mapLayerMetadata = mapLayerMetadataService.findMetadataByDatafile(dfile);
        if (mapLayerMetadata == null) {
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
        if (jsonInfo.containsKey("mapImageLink")) {
            mapLayerMetadata.setMapImageLink(jsonInfo.getString("mapImageLink"));
        }

        // If this was a tabular join set the attributes:
        //  - isJoinLayer
        //  - joinDescription
        String joinDescription = jsonInfo.getString("joinDescription", null);
        if (StringUtils.isNotEmpty(joinDescription)) {
            mapLayerMetadata.setIsJoinLayer(true);
            mapLayerMetadata.setJoinDescription(joinDescription);
        } else {
            mapLayerMetadata.setIsJoinLayer(false);
            mapLayerMetadata.setJoinDescription(null);
        }

        // Set the mapLayerLinks
        String mapLayerLinks = jsonInfo.getString("mapLayerLinks", null);
        if (StringUtils.isNotEmpty(mapLayerLinks)) {
            mapLayerMetadata.setMapLayerLinks(mapLayerLinks);
        } else {
            mapLayerMetadata.setMapLayerLinks(null);
        }

        MapLayerMetadata savedMapLayerMetadata = mapLayerMetadataService.save(mapLayerMetadata);
        if (savedMapLayerMetadata == null) {
            logger.log(Level.SEVERE, "Json: " + jsonLayerData);
            return badRequest("Failed to save map layer!  Original JSON: ");
        }

        // notify user
        userNotificationService.sendNotificationWithEmail(dvUser, wmToken.getCurrentTimestamp(), NotificationType.MAPLAYERUPDATED,
                dfile.getOwner().getLatestVersion().getId(), NotificationObjectType.DATASET_VERSION);
        // Retrieve a PNG representation from WorldMap
        try {
            logger.info("retrieveMapImageForIcon");
            mapLayerMetadataService.retrieveMapImageForIcon(savedMapLayerMetadata);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Failed to retrieve image from WorldMap server", ex);
        }

        return ok("map layer object saved!");
    }

    /*
        For WorldMap/GeoConnect Usage
        Delete MayLayerMetadata object for a given Datafile

        POST params
        {
           "GEOCONNECT_TOKEN": "-- some 64 char token which contains a link to the DataFile --"
        }
    */
    @POST
    @ApiWriteOperation
    @Path(DELETE_MAP_LAYER_DATA_API_PATH_FRAGMENT)
    public Response deleteWorldMapLayerData(String jsonData) {

        /*----------------------------------
            Parse the json message.
            - Auth check: GEOCONNECT_TOKEN
        //----------------------------------*/
        if (jsonData == null) {
            logger.log(Level.SEVERE, "jsonData is null");
            return badRequest("No JSON data");
        }
        // (1) Parse JSON
        JsonObject jsonInfo;
        try (StringReader rdr = new StringReader(jsonData)) {
            jsonInfo = Json.createReader(rdr).readObject();
        } catch (JsonParsingException jpe) {
            logger.log(Level.SEVERE, "Json: " + jsonData);
            return badRequest("Error parsing Json: " + jpe.getMessage());
        }

        // (2) Retrieve token string
        String worldmapTokenParam = retrieveTokenValueFromJson(jsonInfo);
        if (worldmapTokenParam == null) {
            return badRequest("Token not found in JSON request.");
        }

        // (3) Retrieve WorldMapToken and make sure it is valid
        WorldMapToken wmToken = tokenServiceBean.retrieveAndRefreshValidToken(worldmapTokenParam);
        if (wmToken == null) {
            return unauthorized("No access. Invalid token.");
        }

        // (4) Make sure the token's User still has permissions to access the file
        if (!(tokenServiceBean.canTokenUserEditFile(wmToken))) {
            tokenServiceBean.expireToken(wmToken);
            return unauthorized("No access. Invalid token.");
        }

        // (5) Attempt to retrieve DataFile and mapLayerMetadata
        DataFile dfile = wmToken.getDatafile();
        MapLayerMetadata mapLayerMetadata = mapLayerMetadataService.findMetadataByDatafile(dfile);
        if (mapLayerMetadata == null) {
            return error(Response.Status.EXPECTATION_FAILED, "No map layer metadata found.");
        }

        // (6) Delete the mapLayerMetadata
        //   (note: permissions checked here for a second time by the mapLayerMetadataService call)
        if (!mapLayerMetadataService.deleteMapLayerMetadataObject(mapLayerMetadata, wmToken.getDataverseUser())) {
            return error(Response.Status.PRECONDITION_FAILED, "Failed to delete layer");
        }

        return ok("Map layer metadata deleted.");
    }

    /*
         For WorldMap/GeoConnect Usage
         Explicitly expire a WorldMap token, removing token from the database

         POST params
         {
            "GEOCONNECT_TOKEN": "-- some 64 char token which contains a link to the DataFile --"
         }
     */
    @POST
    @ApiWriteOperation
    @Path(DELETE_WORLDMAP_TOKEN_PATH_FRAGMENT)
    public Response deleteWorldMapToken(String jsonData) {

        /*----------------------------------
            Parse the json message.
            - Auth check: GEOCONNECT_TOKEN
        //----------------------------------*/
        if (jsonData == null) {
            logger.log(Level.SEVERE, "jsonData is null");
            return badRequest("No JSON data");
        }
        // (1) Parse JSON
        //
        JsonObject jsonInfo;
        try (StringReader rdr = new StringReader(jsonData)) {
            jsonInfo = Json.createReader(rdr).readObject();
        } catch (JsonParsingException jpe) {
            logger.log(Level.SEVERE, "Json: " + jsonData);
            return badRequest("Error parsing Json: " + jpe.getMessage());
        }

        // (2) Retrieve token string
        String worldmapTokenParam = retrieveTokenValueFromJson(jsonInfo);
        if (worldmapTokenParam == null) {
            return badRequest("Token not found in JSON request.");
        }

        // (3) Retrieve WorldMapToken
        WorldMapToken wmToken = tokenServiceBean.findByName(worldmapTokenParam);
        if (wmToken == null) {
            return notFound("Token not found.");
        }

        // (4) Delete the token
        tokenServiceBean.deleteToken(wmToken);
        return ok("Token has been deleted.");
    }

    // -------------------- PRIVATE --------------------

    /*
        Link used within Dataverse for MapIt button
        Sends file link to GeoConnect using a Redirect
    */
    private Response mapDataFileTokenOnlyOption(@Context HttpServletRequest request, Long datafileId, Long dvuserId,
                                                boolean tokenOnly) {

        logger.log(Level.INFO, "mapDataFile datafileId: {0}", datafileId);
        logger.log(Level.INFO, "mapDataFile dvuserId: {0}", dvuserId);

        AuthenticatedUser user = null;

        if (session != null && session.getUser() != null && session.getUser().isAuthenticated()) {
            user = (AuthenticatedUser) session.getUser();
        }
        if (user == null) {
            return forbidden("Not logged in");
        }

        // Check if the user exists
        AuthenticatedUser dvUser = dataverseUserService.findByID(dvuserId);
        if (dvUser == null) {
            return forbidden("Invalid user");
        }

        // Check if this file exists
        DataFile dfile = dataFileService.find(datafileId);
        if (dfile == null) {
            return notFound("DataFile not found for id: " + datafileId);
        }

        // Is the dataset public?
        if (!dfile.getOwner().isReleased()) {
            return forbidden("Mapping is only permitted for public datasets/files");
        }

        // Does this user have permission to edit metadata for this file?
        if (!permissionService.requestOn(createDataverseRequest(dvUser), dfile.getOwner()).has(Permission.EditDataset)) {
            String errMsg = "The user does not have permission to edit metadata for this file.";
            return forbidden(errMsg);
        }

        WorldMapToken token = tokenServiceBean.getNewToken(dfile, dvUser);

        if (tokenOnly) {
            // Return only the token in a JSON object
            final JsonObjectBuilder jsonInfo = Json.createObjectBuilder();
            jsonInfo.add(WorldMapToken.GEOCONNECT_TOKEN_KEY, token.getToken());
            return ok(jsonInfo);
        }

        // Redirect to geoconnect url
        String callbackUrl = systemConfig.getDataverseSiteUrl() + GET_WORLDMAP_DATAFILE_API_PATH;
        String redirectUrlStr = token.getApplication().getMapitLink() + "/" + token.getToken() + "/?cb="
                + URLEncoder.encode(callbackUrl);

        URI redirectUri;
        try {
            redirectUri = new URI(redirectUrlStr);
        } catch (URISyntaxException ex) {
            return notFound("Failed to create URI from: " + redirectUrlStr);
        }
        return Response.seeOther(redirectUri).build();
    }

    private String getReturnToFilePageURL(String serverName, Dataset dset, DataFile dataFile, DatasetVersion datasetVersion) {
        return serverName + "/file.xhtml?fileId=" + dataFile.getId() + "&version=" + datasetVersion.getSemanticVersion();
    }
}
