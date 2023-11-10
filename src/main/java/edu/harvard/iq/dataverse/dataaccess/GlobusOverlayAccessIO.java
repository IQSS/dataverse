package edu.harvard.iq.dataverse.dataaccess;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.globus.AccessToken;
import edu.harvard.iq.dataverse.globus.GlobusServiceBean;
import edu.harvard.iq.dataverse.util.UrlSignerUtil;
import edu.harvard.iq.dataverse.util.json.JsonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Logger;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

/**
 * @author qqmyers
 */
/*
 * Globus Overlay Driver
 * 
 * Remote: StorageIdentifier format:
 * <globusDriverId>://<baseStorageIdentifier>//<relativePath> Storage location:
 * <globusendpointId/basepath>/<relPath> Internal StorageIdentifier format:
 * <globusDriverId>://<baseStorageIdentifier> Storage location:
 * <globusEndpointId/basepath>/<dataset authority>/<dataset
 * identifier>/<baseStorageIdentifier>
 *
 * transfer and reference endpoint formats: <globusEndpointId/basePath>
 * reference endpoints separated by a comma
 * 
 */
public class GlobusOverlayAccessIO<T extends DvObject> extends RemoteOverlayAccessIO<T> implements GlobusAccessibleStore {
    private static final Logger logger = Logger.getLogger("edu.harvard.iq.dataverse.dataaccess.GlobusOverlayAccessIO");

    /*
     * If this is set to true, the store supports Globus transfer in and
     * Dataverse/the globus app manage file locations, access controls, deletion,
     * etc.
     */
    private Boolean dataverseManaged = null;

    private String relativeDirectoryPath;
    
    private String endpointPath;
    
    private String filename;

    private String[] allowedEndpoints;
    private String endpoint;

    public GlobusOverlayAccessIO(T dvObject, DataAccessRequest req, String driverId) throws IOException {
        super(dvObject, req, driverId);
    }


    public GlobusOverlayAccessIO(String storageLocation, String driverId) throws IOException {
        this.driverId = driverId;
        configureStores(null, driverId, storageLocation);
        if (isManaged()) {
            String[] parts = DataAccess.getDriverIdAndStorageLocation(storageLocation);
            path = parts[1];
        } else {
            this.setIsLocalFile(false);
            path = storageLocation.substring(storageLocation.lastIndexOf("//") + 2);
            validatePath(path);
            logger.fine("Referenced path: " + path);
        }
    }
    private boolean isManaged() {
        if(dataverseManaged==null) {
            dataverseManaged = GlobusAccessibleStore.isDataverseManaged(this.driverId);
        }
        return dataverseManaged;
    }
    
    private String retrieveGlobusAccessToken() {
        String globusToken = getConfigParam(GlobusAccessibleStore.GLOBUS_TOKEN);
        

        AccessToken accessToken = GlobusServiceBean.getClientToken(globusToken);
        return accessToken.getOtherTokens().get(0).getAccessToken();
    }


    private void parsePath() {
        int filenameStart = path.lastIndexOf("/") + 1;
        String endpointWithBasePath = null;
        if (!isManaged()) {
            endpointWithBasePath = findMatchingEndpoint(path, allowedEndpoints);
        } else {
            endpointWithBasePath = allowedEndpoints[0];
        }
        //String endpointWithBasePath = baseEndpointPath.substring(baseEndpointPath.lastIndexOf(DataAccess.SEPARATOR) + 3);
        int pathStart = endpointWithBasePath.indexOf("/");
        logger.info("endpointWithBasePath: " + endpointWithBasePath);
        endpointPath = "/" + (pathStart > 0 ? endpointWithBasePath.substring(pathStart + 1) : "");
        logger.info("endpointPath: " + endpointPath);
        

        if (isManaged() && (dvObject!=null)) {
            
            Dataset ds = null;
            if (dvObject instanceof Dataset) {
                ds = (Dataset) dvObject;
            } else if (dvObject instanceof DataFile) {
                ds = ((DataFile) dvObject).getOwner();
            }
            relativeDirectoryPath = "/" + ds.getAuthority() + "/" + ds.getIdentifier();
        } else {
            relativeDirectoryPath = "";
        }
        if (filenameStart > 0) {
            relativeDirectoryPath = relativeDirectoryPath + path.substring(0, filenameStart);
        }
        logger.info("relativeDirectoryPath finally: " + relativeDirectoryPath);
        filename = path.substring(filenameStart);
        endpoint = pathStart > 0 ? endpointWithBasePath.substring(0, pathStart) : endpointWithBasePath;

        
    }

    private static String findMatchingEndpoint(String path, String[] allowedEndpoints) {
        for(int i=0;i<allowedEndpoints.length;i++) {
            if (path.startsWith(allowedEndpoints[i])) {
                return allowedEndpoints[i];
            }
        }
        logger.warning("Could not find matching endpoint for path: " + path);
        return null;
    }

    @Override
    protected void validatePath(String relPath) throws IOException {
        if (isManaged()) {
            if (!usesStandardNamePattern(relPath)) {
                throw new IOException("Unacceptable identifier pattern in submitted identifier: " + relPath);
            }
        } else {
            try {
                String endpoint = findMatchingEndpoint(relPath, allowedEndpoints);
                logger.info(endpoint + "  " + relPath);

                if (endpoint == null || !Paths.get(endpoint, relPath).normalize().startsWith(endpoint)) {
                    throw new IOException(
                            "storageidentifier doesn't start with one of " + this.driverId + "'s allowed endpoints");
                }
            } catch (InvalidPathException e) {
                throw new IOException("Could not interpret storageidentifier in globus store " + this.driverId);
            }
        }

    }

    // Call the Globus API to get the file size
    @Override
    public long retrieveSizeFromMedia() {
        parsePath();
        String globusAccessToken = retrieveGlobusAccessToken();
        logger.info("GAT2: " + globusAccessToken);
        // Construct Globus URL
        URI absoluteURI = null;
        try {

            absoluteURI = new URI("https://transfer.api.globusonline.org/v0.10/operation/endpoint/" + endpoint
                    + "/ls?path=" + endpointPath + relativeDirectoryPath + "&filter=name:" + filename);
            HttpGet get = new HttpGet(absoluteURI);

            logger.info("Token is " + globusAccessToken);
            get.addHeader("Authorization", "Bearer " + globusAccessToken);
            CloseableHttpResponse response = getSharedHttpClient().execute(get, localContext);
            if (response.getStatusLine().getStatusCode() == 200) {
                // Get reponse as string
                String responseString = EntityUtils.toString(response.getEntity());
                logger.info("Response from " + get.getURI().toString() + " is: " + responseString);
                JsonObject responseJson = JsonUtil.getJsonObject(responseString);
                JsonArray dataArray = responseJson.getJsonArray("DATA");
                if (dataArray != null && dataArray.size() != 0) {
                    //File found
                    return (long) responseJson.getJsonArray("DATA").getJsonObject(0).getInt("size");
                }
            } else {
                logger.warning("Response from " + get.getURI().toString() + " was "
                        + response.getStatusLine().getStatusCode());
                logger.info(EntityUtils.toString(response.getEntity()));
            }
        } catch (URISyntaxException e) {
            // Should have been caught in validatePath
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return -1;
    }

    
    @Override
    public InputStream getInputStream() throws IOException {
        if(Boolean.parseBoolean(getConfigParam("endpoint-maps-to-base-store"))) {
            return baseStore.getInputStream();
        } else {
            throw new IOException("Not implemented");
        }
    }
    
    @Override
    public void delete() throws IOException {

        parsePath();
        // Delete is best-effort - we tell the endpoint to delete don't monitor whether
        // it succeeds
        if (!isDirectAccess()) {
            throw new IOException("Direct Access IO must be used to permanently delete stored file objects");
        }
        String globusAccessToken = retrieveGlobusAccessToken();
        // Construct Globus URL
        URI absoluteURI = null;
        try {

            absoluteURI = new URI("https://transfer.api.globusonline.org/v0.10/submission_id");
            HttpGet get = new HttpGet(absoluteURI);

            logger.info("Token is " + globusAccessToken);
            get.addHeader("Authorization", "Bearer " + globusAccessToken);
            CloseableHttpResponse response = getSharedHttpClient().execute(get, localContext);
            if (response.getStatusLine().getStatusCode() == 200) {
                // Get reponse as string
                String responseString = EntityUtils.toString(response.getEntity());
                logger.info("Response from " + get.getURI().toString() + " is: " + responseString);
                JsonObject responseJson = JsonUtil.getJsonObject(responseString);
                String submissionId = responseJson.getString("value");
                logger.info("submission_id for delete is: " + submissionId);
                absoluteURI = new URI("https://transfer.api.globusonline.org/v0.10/delete");
                HttpPost post = new HttpPost(absoluteURI);
                JsonObjectBuilder taskJsonBuilder = Json.createObjectBuilder();
                taskJsonBuilder.add("submission_id", submissionId).add("DATA_TYPE", "delete").add("endpoint", endpoint)
                        .add("DATA", Json.createArrayBuilder().add(Json.createObjectBuilder().add("DATA_TYPE", "delete_item").add("path",
                                endpointPath + relativeDirectoryPath + "/" + filename)));
                post.setHeader("Content-Type", "application/json");
                post.addHeader("Authorization", "Bearer " + globusAccessToken);
                String taskJson= JsonUtil.prettyPrint(taskJsonBuilder.build());
                logger.info("Sending: " + taskJson);
                post.setEntity(new StringEntity(taskJson, "utf-8"));
                CloseableHttpResponse postResponse = getSharedHttpClient().execute(post, localContext);
                int statusCode=postResponse.getStatusLine().getStatusCode();
                logger.info("Response :" + statusCode + ": " +postResponse.getStatusLine().getReasonPhrase());
                switch (statusCode) {
                case 202:
                    // ~Success - delete task was accepted
                    logger.info("Globus delete initiated: " + EntityUtils.toString(postResponse.getEntity()));
                    break;
                case 200:
                    // Duplicate - delete task was already accepted
                    logger.info("Duplicate Globus delete: " + EntityUtils.toString(postResponse.getEntity()));
                    break;
                default:
                    logger.warning("Response from " + post.getURI().toString() + " was "
                            + postResponse.getStatusLine().getStatusCode());
                    logger.info(EntityUtils.toString(postResponse.getEntity()));
                }

            } else {
                logger.warning("Response from " + get.getURI().toString() + " was "
                        + response.getStatusLine().getStatusCode());
                logger.info(EntityUtils.toString(response.getEntity()));
            }
        } catch (Exception e) {
            logger.warning(e.getMessage());
            throw new IOException("Error deleting: " + endpoint + "/" + path);

        }

        // Delete all the cached aux files as well:
        deleteAllAuxObjects();

    }

    @Override
    public String generateTemporaryDownloadUrl(String auxiliaryTag, String auxiliaryType, String auxiliaryFileName)
            throws IOException {
//Fix
        parsePath();
        // ToDo - support remote auxiliary Files
        if (auxiliaryTag == null) {
            String secretKey = getConfigParam(SECRET_KEY);
            if (secretKey == null) {
                return endpoint + "/" + path;
            } else {
                return UrlSignerUtil.signUrl(endpoint + "/" + path, getUrlExpirationMinutes(), null, "GET", secretKey);
            }
        } else {
            return baseStore.generateTemporaryDownloadUrl(auxiliaryTag, auxiliaryType, auxiliaryFileName);
        }
    }

    static boolean isValidIdentifier(String driverId, String storageId) {
        String baseIdentifier = storageId.substring(storageId.lastIndexOf("//") + 2);
        try {
            
        String[] allowedEndpoints =getAllowedEndpoints(driverId);
            
        // Internally managed endpoints require standard name pattern (submitted via
        // /addFile(s) api)
        if (GlobusAccessibleStore.isDataverseManaged(driverId)) {
            boolean hasStandardName = usesStandardNamePattern(baseIdentifier);
            if (hasStandardName) {
                return true;
            } else {
                logger.warning("Unacceptable identifier pattern in submitted identifier: " + baseIdentifier);
                return false;
            }
        }
        // Remote endpoints require a valid URI within the baseUrl
        try {
            String endpoint = findMatchingEndpoint(baseIdentifier, allowedEndpoints);
            
            if(endpoint==null || !Paths.get(endpoint, baseIdentifier).normalize().startsWith(endpoint)) {
                logger.warning("storageidentifier doesn't start with one of " + driverId + "'s allowed endpoints");
                return false;
            }
        } catch (InvalidPathException e) {
            logger.warning("Could not interpret storageidentifier in globus store " + driverId);
            return false;
        }
        return true;
        } catch (IOException e) {
            return false;
        }
        
    }

    @Override
    public String getStorageLocation() throws IOException {
        parsePath();
        if (isManaged()) {
            return this.driverId + DataAccess.SEPARATOR + relativeDirectoryPath + "/" + filename;
        } else {
            return super.getStorageLocation();
        }
    }
    
    /** This endpoint configures all the endpoints the store is allowed to reference data from. At present, the RemoteOverlayAccessIO only supports a single endpoint but
     * the derived GlobusOverlayAccessIO can support multiple endpoints.
     * @throws IOException
     */
    @Override
    protected void configureEndpoints() throws IOException {
        allowedEndpoints = getAllowedEndpoints(this.driverId);
        logger.info("Set allowed endpoints: " + Arrays.toString(allowedEndpoints));
    }
    
    private static String[] getAllowedEndpoints(String driverId) throws IOException {
        String[] allowedEndpoints = null;
        if (GlobusAccessibleStore.isDataverseManaged(driverId)) {
            allowedEndpoints = new String[1];
            allowedEndpoints[0] = getConfigParamForDriver(driverId, TRANSFER_ENDPOINT_WITH_BASEPATH);
            if (allowedEndpoints[0] == null) {
                throw new IOException(
                        "dataverse.files." + driverId + "." + TRANSFER_ENDPOINT_WITH_BASEPATH + " is required");
            }
        } else {
            String rawEndpoints = getConfigParamForDriver(driverId, REFERENCE_ENDPOINTS_WITH_BASEPATHS);
            if (rawEndpoints != null) {
                allowedEndpoints = getConfigParamForDriver(driverId, REFERENCE_ENDPOINTS_WITH_BASEPATHS).split("\\s*,\\s*");
            }
            if (rawEndpoints == null || allowedEndpoints.length == 0) {
                throw new IOException("dataverse.files." + driverId + ".base-url is required");
            }
        }
        return allowedEndpoints;
    }


    public static void main(String[] args) {
        System.out.println("Running the main method");
        if (args.length > 0) {
            System.out.printf("List of arguments: {}", Arrays.toString(args));
        }
        System.setProperty("dataverse.files.globus.base-url", "globus://d8c42580-6528-4605-9ad8-116a61982644");
        System.out.println("NotValid: " + isValidIdentifier("globus", "globus://localid//../of/the/hill"));
        System.out.println("ValidRemote: " + isValidIdentifier("globus", "globus://localid//of/the/hill"));
        System.setProperty("dataverse.files.globus.managed", "true");

        System.out.println("ValidLocal: " + isValidIdentifier("globus", "globus://176e28068b0-1c3f80357c42"));
        System.setProperty("dataverse.files.globus.globus-token",
                "");
        System.setProperty("dataverse.files.globus.base-store", "file");
        System.setProperty("dataverse.files.file.type", DataAccess.DEFAULT_STORAGE_DRIVER_IDENTIFIER);
        System.setProperty("dataverse.files.file.directory", "/tmp/files");
        // logger.info(JvmSettings.BASE_URL.lookup("globus"));
        // logger.info(JvmSettings.GLOBUS_TOKEN.lookup("globus"));

        try {
            GlobusOverlayAccessIO<DvObject> gsio = new GlobusOverlayAccessIO<DvObject>(
                    "globus://1234///hdc1/image001.mrc", "globus");
            logger.info("Size is " + gsio.retrieveSizeFromMedia());

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            DataFile df = new DataFile();
            Dataset ds = new Dataset();
            ds.setAuthority("10.5072");
            ds.setIdentifier("FK21234");
            df.setOwner(ds);
            df.setStorageIdentifier("globus://1234///hdc1/image001.mrc");
            GlobusOverlayAccessIO<DvObject> gsio = new GlobusOverlayAccessIO<DvObject>(df, null, "globus");
            logger.info("Size2 is " + gsio.retrieveSizeFromMedia());

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    
}
