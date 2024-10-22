package edu.harvard.iq.dataverse.globus;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import edu.harvard.iq.dataverse.*;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonPatch;
import jakarta.json.JsonString;
import jakarta.json.JsonValue.ValueType;
import jakarta.json.stream.JsonParsingException;
import jakarta.ws.rs.HttpMethod;

import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.toJsonArray;

import java.io.*;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.codec.binary.StringUtils;
import org.primefaces.PrimeFaces;

import com.google.gson.Gson;
import edu.harvard.iq.dataverse.api.ApiConstants;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.GlobusAccessibleStore;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.datasetutility.AddReplaceFileHelper;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlServiceBean;
import edu.harvard.iq.dataverse.settings.FeatureFlags;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.URLTokenUtil;
import edu.harvard.iq.dataverse.util.UrlSignerUtil;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import jakarta.json.JsonReader;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import org.apache.http.util.EntityUtils;

@Stateless
@Named("GlobusServiceBean")
public class GlobusServiceBean implements java.io.Serializable {

    @EJB
    protected DatasetServiceBean datasetSvc;
    @EJB
    protected SettingsServiceBean settingsSvc;
    @Inject
    DataverseSession session;
    @Inject
    DataverseRequestServiceBean dataverseRequestSvc;
    @EJB
    protected AuthenticationServiceBean authSvc;
    @EJB
    EjbDataverseEngine commandEngine;
    @EJB
    UserNotificationServiceBean userNotificationService;
    @EJB
    PrivateUrlServiceBean privateUrlService;
    @EJB
    FileDownloadServiceBean fileDownloadService;
    @EJB
    DataFileServiceBean dataFileSvc;
    @EJB
    PermissionServiceBean permissionSvc;
    @EJB 
    IngestServiceBean ingestSvc;
    @EJB
    SystemConfig systemConfig;
    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    private static final Logger logger = Logger.getLogger(GlobusServiceBean.class.getCanonicalName());
    private static final SimpleDateFormat logFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss");

    private String getRuleId(GlobusEndpoint endpoint, String principal, String permissions)
            throws MalformedURLException {

        String principalType = "identity";

        URL url = new URL("https://transfer.api.globusonline.org/v0.10/endpoint/" + endpoint.getId() + "/access_list");
        MakeRequestResponse result = makeRequest(url, "Bearer", endpoint.getClientToken(), "GET", null);
        if (result.status == 200) {
            AccessList al = parseJson(result.jsonResponse, AccessList.class, false);

            for (int i = 0; i < al.getDATA().size(); i++) {
                Permissions pr = al.getDATA().get(i);

                if ((pr.getPath().equals(endpoint.getBasePath() + "/") || pr.getPath().equals(endpoint.getBasePath()))
                        && pr.getPrincipalType().equals(principalType)
                        && ((principal == null) || (principal != null && pr.getPrincipal().equals(principal)))
                        && pr.getPermissions().equals(permissions)) {
                    return pr.getId();
                } else {
                    logger.fine(pr.getPath() + " === " + endpoint.getBasePath() + " == " + pr.getPrincipalType());
                    continue;
                }
            }
        }
        return null;
    }

    /**
     * Call to delete a globus rule related to the specified dataset.
     * 
     * @param ruleId       - Globus rule id - assumed to be associated with the
     *                     dataset's file path (should not be called with a user
     *                     specified rule id w/o further checking)
     * @param dataset    - the dataset associated with the rule
     * @param globusLogger - a separate logger instance, may be null
     */
    public void deletePermission(String ruleId, Dataset dataset, Logger globusLogger) {
        globusLogger.fine("Start deleting rule " + ruleId + " for dataset " + dataset.getId());
        if (ruleId.length() > 0) {
            if (dataset != null) {
                GlobusEndpoint endpoint = getGlobusEndpoint(dataset);
                if (endpoint != null) {
                    String accessToken = endpoint.getClientToken();
                    globusLogger.info("Start deleting permissions.");
                    try {
                        URL url = new URL("https://transfer.api.globusonline.org/v0.10/endpoint/" + endpoint.getId()
                                + "/access/" + ruleId);
                        MakeRequestResponse result = makeRequest(url, "Bearer", accessToken, "DELETE", null);
                        if (result.status != 200) {
                            globusLogger.warning("Cannot delete access rule " + ruleId);
                        } else {
                            globusLogger.info("Access rule " + ruleId + " was deleted successfully");
                        }
                    } catch (MalformedURLException ex) {
                        logger.log(Level.WARNING,
                                "Failed to delete access rule " + ruleId + " on endpoint " + endpoint.getId(), ex);
                    }
                }
            }
        }
    }

    /**
     * Request read/write access for the specified principal and generate a list of
     * accessible paths for new files for the specified dataset.
     * 
     * @param principal     - the id of the Globus principal doing the transfer
     * @param dataset
     * @param numberOfPaths - how many files are to be transferred
     * @return
     */
    public JsonObject requestAccessiblePaths(String principal, Dataset dataset, int numberOfPaths) {

        GlobusEndpoint endpoint = getGlobusEndpoint(dataset);
        String principalType = "identity";

        Permissions permissions = new Permissions();
        permissions.setDATA_TYPE("access");
        permissions.setPrincipalType(principalType);
        permissions.setPrincipal(principal);
        permissions.setPath(endpoint.getBasePath() + "/");
        permissions.setPermissions("rw");
        
        JsonObjectBuilder response = Json.createObjectBuilder();
        //Try to create the directory (202 status) if it does not exist (502-already exists)
        int mkDirStatus = makeDirs(endpoint, dataset);
        if (!(mkDirStatus== 202 || mkDirStatus == 502)) {
            return response.add("status", mkDirStatus).build();
        }
        //The dir for the dataset's data exists, so try to request permission for the principal
        int requestPermStatus = requestPermission(endpoint, dataset, permissions);
        response.add("status", requestPermStatus);
        if (requestPermStatus == 201) {
            String driverId = dataset.getEffectiveStorageDriverId();
            JsonObjectBuilder paths = Json.createObjectBuilder();
            for (int i = 0; i < numberOfPaths; i++) {
                String storageIdentifier = DataAccess.getNewStorageIdentifier(driverId);
                int lastIndex = Math.max(storageIdentifier.lastIndexOf("/"), storageIdentifier.lastIndexOf(":"));
                paths.add(storageIdentifier, endpoint.getBasePath() + "/" + storageIdentifier.substring(lastIndex + 1));

            }
            response.add("paths", paths.build());
        }
        return response.build();
    }

    /**
     * Call to create the directories for the specified dataset.
     * 
     * @param dataset
     * @return - an error status at whichever subdir the process fails at or the
     *         final success status
     */
    private int makeDirs(GlobusEndpoint endpoint, Dataset dataset) {
        logger.fine("Creating dirs: " + endpoint.getBasePath());
        int index = endpoint.getBasePath().lastIndexOf(dataset.getAuthorityForFileStorage())
                + dataset.getAuthorityForFileStorage().length();
        String nextDir = endpoint.getBasePath().substring(0, index);
        int response = makeDir(endpoint, nextDir);
        String identifier = dataset.getIdentifierForFileStorage();
        //Usually identifiers will have 0 or 1 slashes (e.g. FK2/ABCDEF) but the while loop will handle any that could have more
        //Will skip if the first makeDir above failed
        while ((identifier.length() > 0) && ((response == 202 || response == 502))) {
            index = identifier.indexOf('/');
            if (index == -1) {
                //Last dir to create
                response = makeDir(endpoint, nextDir + "/" + identifier);
                identifier = "";
            } else {
                //The next dir to create
                nextDir = nextDir + "/" + identifier.substring(0, index);
                response = makeDir(endpoint, nextDir);
                //The rest of the identifier
                identifier = identifier.substring(index + 1);
            }
        }
        return response;
    }
    
    private int makeDir(GlobusEndpoint endpoint, String dir) {
        MakeRequestResponse result = null;
        String body = "{\"DATA_TYPE\":\"mkdir\",\"path\":\"" + dir + "\"}";
        try {
            logger.fine(body);
            URL url = new URL(
                    "https://transfer.api.globusonline.org/v0.10/operation/endpoint/" + endpoint.getId() + "/mkdir");
            result = makeRequest(url, "Bearer", endpoint.getClientToken(), "POST", body);

            switch (result.status) {
            case 202:
                logger.fine("Dir " + dir + " was created successfully.");
                break;
            case 502:
                logger.fine("Dir " + dir + " already exists.");
                break;
            default:
                logger.warning("Status " + result.status + " received when creating dir " + dir);
                logger.fine("Response: " + result.jsonResponse);
            }
        } catch (MalformedURLException ex) {
            // Misconfiguration
            logger.warning("Failed to create dir on " + endpoint.getId());
            return 500;
        }
        return result.status;
    }
    
    private int requestPermission(GlobusEndpoint endpoint, Dataset dataset, Permissions permissions) {
        Gson gson = new GsonBuilder().create();
        MakeRequestResponse result = null;
        logger.fine("Start creating the rule");

        try {
            URL url = new URL("https://transfer.api.globusonline.org/v0.10/endpoint/" + endpoint.getId() + "/access");
            result = makeRequest(url, "Bearer", endpoint.getClientToken(), "POST", gson.toJson(permissions));

            switch (result.status) {
            case 404:
                logger.severe("Endpoint " + endpoint.getId() + " was not found");
                break;
            case 400:
                logger.severe("Path " + permissions.getPath() + " is not valid");
                break;
            case 409:
                logger.warning("ACL already exists or Endpoint ACL already has the maximum number of access rules");
                break;
            case 201:
                JsonObject globusResponse = JsonUtil.getJsonObject(result.jsonResponse);
                if (globusResponse != null && globusResponse.containsKey("access_id")) {
                    permissions.setId(globusResponse.getString("access_id"));
                    monitorTemporaryPermissions(permissions.getId(), dataset.getId());
                    logger.fine("Access rule " + permissions.getId() + " was created successfully");
                } else {
                    // Shouldn't happen!
                    logger.warning("Access rule id not returned for dataset " + dataset.getId());
                }
            }
            return result.status;
        } catch (MalformedURLException ex) {
            // Misconfiguration
            logger.warning("Failed to create access rule URL for " + endpoint.getId());
            return 500;
        }
    }

    /**
     * Given an array of remote files to be referenced in the dataset, create a set
     * of valid storage identifiers and return a map of the remote file paths to
     * storage identifiers.
     * 
     * @param dataset
     * @param referencedFiles - a JSON array of remote files to be referenced in the
     *                        dataset - each should be a string with the <Globus
     *                        endpoint>/path/to/file
     * @return - a map of supplied paths to valid storage identifiers
     */
    public JsonObject requestReferenceFileIdentifiers(Dataset dataset, JsonArray referencedFiles) {
        String driverId = dataset.getEffectiveStorageDriverId();
        JsonArray endpoints = GlobusAccessibleStore.getReferenceEndpointsWithPaths(driverId);

        JsonObjectBuilder fileMap = Json.createObjectBuilder();
        referencedFiles.forEach(value -> {
            if (value.getValueType() != ValueType.STRING) {
                throw new JsonParsingException("ReferencedFiles must be strings", null);
            }
            String referencedFile = ((JsonString) value).getString();
            boolean valid = false;
            for (int i = 0; i < endpoints.size(); i++) {
                if (referencedFile.startsWith(((JsonString) endpoints.get(i)).getString())) {
                    valid = true;
                }
            }
            if (!valid) {
                throw new IllegalArgumentException(
                        "Referenced file " + referencedFile + " is not in an allowed endpoint/path");
            }
            String storageIdentifier = DataAccess.getNewStorageIdentifier(driverId);
            fileMap.add(referencedFile, storageIdentifier + "//" + referencedFile);
        });
        return fileMap.build();
    }

    /**
     * A cache of temporary permission requests - for upload (rw) and download (r)
     * access. When a temporary permission request is created, it is added to the
     * cache. After GLOBUS_CACHE_MAXAGE minutes, if a transfer has not been started,
     * the permission will be revoked/deleted. (If a transfer has been started, the
     * permission will not be revoked/deleted until the transfer is complete. This
     * is handled in other methods.)
     */
    // ToDo - nominally this doesn't need to be as long as the allowed time for the
    // downloadCache so there could be two separate settings.
    // Single cache of open rules/permission requests
    private final Cache<String, Long> rulesCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.of(JvmSettings.GLOBUS_CACHE_MAXAGE.lookup(Integer.class), ChronoUnit.MINUTES))
            .scheduler(Scheduler.systemScheduler()).evictionListener((ruleId, datasetId, cause) -> {
                // Delete rules that expire
                logger.fine("Rule " + ruleId + " expired");
                Dataset dataset = datasetSvc.find(datasetId);
                deletePermission((String) ruleId, dataset, logger);
            })

            .build();

    // Convenience method to add a temporary permission request to the cache -
    // allows logging of temporary permission requests
    private void monitorTemporaryPermissions(String ruleId, long datasetId) {
        logger.fine("Adding rule " + ruleId + " for dataset " + datasetId);
        rulesCache.put(ruleId, datasetId);
    }

    /**
     * Call the Globus API to get info about the transfer.
     * 
     * @param accessToken
     * @param taskId       - the Globus task id supplied by the user
     * @param globusLogger - the transaction-specific logger to use (separate log
     *                     files are created in general, some calls may use the
     *                     class logger)
     * @return
     * @throws MalformedURLException
     */
    public GlobusTaskState getTask(String accessToken, String taskId, Logger globusLogger) {

        Logger myLogger = globusLogger != null ? globusLogger : logger;

        URL url;
        try {
            url = new URL("https://transfer.api.globusonline.org/v0.10/endpoint_manager/task/" + taskId);
        } catch (MalformedURLException mue) {
            myLogger.warning("Malformed URL exception when trying to contact Globus. Globus API url: "
                    + "https://transfer.api.globusonline.org/v0.10/endpoint_manager/task/"
                    + taskId);
            return null;
        }

        MakeRequestResponse result = makeRequest(url, "Bearer", accessToken, "GET", null);

        GlobusTaskState task = null;

        if (result.status == 200) {
            task = parseJson(result.jsonResponse, GlobusTaskState.class, false);
        }
        if (result.status != 200) {
            // @todo It should probably retry it 2-3 times before giving up;
            // similarly, it should probably differentiate between a "no such task" 
            // response and something intermittent like a server/network error or 
            // an expired token... i.e. something that's recoverable (?)
            myLogger.warning("Cannot find information for the task " + taskId + " : Reason :   "
                    + result.jsonResponse.toString());
        }

        return task;
    }

    /**
     * Globus call to get an access token for the user using the long-term token we
     * hold.
     * 
     * @param globusBasicToken - the base64 encoded Globus Basic token comprised of
     *                         the <Globus user id>:<key>
     * @return - a valid Globus access token
     */
    public static AccessToken getClientToken(String globusBasicToken) {
        URL url;
        AccessToken clientTokenUser = null;

        try {
            url = new URL(
                    "https://auth.globus.org/v2/oauth2/token?scope=openid+email+profile+urn:globus:auth:scope:transfer.api.globus.org:all&grant_type=client_credentials");

            MakeRequestResponse result = makeRequest(url, "Basic", globusBasicToken, "POST", null);
            if (result.status == 200) {
                clientTokenUser = parseJson(result.jsonResponse, AccessToken.class, true);
            }
        } catch (MalformedURLException e) {
            // On a statically defined URL...
            e.printStackTrace();
        }
        return clientTokenUser;
    }

    private static MakeRequestResponse makeRequest(URL url, String authType, String authCode, String method,
            String jsonString) {
        String str = null;
        HttpURLConnection connection = null;
        int status = 0;
        try {
            connection = (HttpURLConnection) url.openConnection();
            // Basic
            logger.fine("For URL: " + url.toString());
            connection.setRequestProperty("Authorization", authType + " " + authCode);
            // connection.setRequestProperty("Content-Type",
            // "application/x-www-form-urlencoded");
            connection.setRequestMethod(method);
            if (jsonString != null) {
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                logger.fine(jsonString);
                connection.setDoOutput(true);

                OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
                wr.write(jsonString);
                wr.flush();
            }

            status = connection.getResponseCode();
            logger.fine("Status now " + status);
            InputStream result = connection.getInputStream();
            if (result != null) {
                str = readResultJson(result).toString();
                logger.fine("str is " + result.toString());
            } else {
                logger.fine("Result is null");
                str = null;
            }

            logger.fine("status: " + status);
        } catch (IOException ex) {
            logger.severe(ex.getMessage());
            logger.fine(ex.getCause().toString());
            logger.fine(ex.getStackTrace().toString());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        MakeRequestResponse r = new MakeRequestResponse(str, status);
        return r;

    }

    private static StringBuilder readResultJson(InputStream in) {
        StringBuilder sb = null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }
            br.close();
            logger.fine(sb.toString());
        } catch (IOException e) {
            sb = null;
            logger.severe(e.getMessage());
        }
        return sb;
    }

    private static <T> T parseJson(String sb, Class<T> jsonParserClass, boolean namingPolicy) {
        if (sb != null) {
            Gson gson = null;
            if (namingPolicy) {
                gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

            } else {
                gson = new GsonBuilder().create();
            }
            T jsonClass = gson.fromJson(sb, jsonParserClass);
            return jsonClass;
        } else {
            logger.severe("Bad respond from token rquest");
            return null;
        }
    }

    static class MakeRequestResponse {
        public String jsonResponse;
        public int status;

        MakeRequestResponse(String jsonResponse, int status) {
            this.jsonResponse = jsonResponse;
            this.status = status;
        }

    }

    /**
     * Cache of open download Requests This cache keeps track of the set of files
     * selected for transfer out (download) via Globus. It is a means of
     * transferring the list from the DatasetPage, where it is generated via user UI
     * actions, and the Datasets/globusDownloadParameters API.
     * 
     * Nominally, the dataverse-globus app will call that API endpoint and then
     * /requestGlobusDownload, at which point the cached info is sent to the app. If
     * the app doesn't call within 5 minutes (the time allowed to call
     * /globusDownloadParameters) + GLOBUS_CACHE_MAXAGE minutes (a ~longer period
     * giving the user time to make choices in the app), the cached info is deleted.
     * 
     */
    private final Cache<String, JsonObject> downloadCache = Caffeine.newBuilder()
            .expireAfterWrite(
                    Duration.of(JvmSettings.GLOBUS_CACHE_MAXAGE.lookup(Integer.class) + 5, ChronoUnit.MINUTES))
            .scheduler(Scheduler.systemScheduler()).evictionListener((downloadId, datasetId, cause) -> {
                // Delete downloads that expire
                logger.fine("Download for " + downloadId + " expired");
            })

            .build();

    public JsonObject getFilesForDownload(String downloadId) {
        return downloadCache.getIfPresent(downloadId);
    }

    public int setPermissionForDownload(Dataset dataset, String principal) {
        GlobusEndpoint endpoint = getGlobusEndpoint(dataset);
        String principalType = "identity";

        Permissions permissions = new Permissions();
        permissions.setDATA_TYPE("access");
        permissions.setPrincipalType(principalType);
        permissions.setPrincipal(principal);
        permissions.setPath(endpoint.getBasePath() + "/");
        permissions.setPermissions("r");

        return requestPermission(endpoint, dataset, permissions);
    }

    // Generates the URL to launch the Globus app for upload
    public String getGlobusAppUrlForDataset(Dataset d) {
        return getGlobusAppUrlForDataset(d, true, null);
    }

    /**
     * Generated the App URl for upload (in) or download (out)
     * 
     * @param d         - the dataset involved
     * @param upload    - boolean, true for upload, false for download
     * @param dataFiles - a list of the DataFiles to be downloaded
     * @return
     */
    public String getGlobusAppUrlForDataset(Dataset d, boolean upload, List<DataFile> dataFiles) {
        String localeCode = session.getLocaleCode();
        ApiToken apiToken = null;
        User user = session.getUser();

        if (user instanceof AuthenticatedUser) {
            apiToken = authSvc.findApiTokenByUser((AuthenticatedUser) user);

            if ((apiToken == null) || (apiToken.getExpireTime().before(new Date()))) {
                logger.fine("Created apiToken for user: " + user.getIdentifier());
                apiToken = authSvc.generateApiTokenForUser((AuthenticatedUser) user);
            }
        }
        String driverId = d.getEffectiveStorageDriverId();
        try {
        } catch (Exception e) {
            logger.warning("GlobusAppUrlForDataset: Failed to get storePrefix for " + driverId);
        }

        // Use URLTokenUtil for params currently in common with external tools.
        URLTokenUtil tokenUtil = new URLTokenUtil(d, null, apiToken, localeCode);
        String appUrl = settingsSvc.getValueForKey(SettingsServiceBean.Key.GlobusAppUrl, "http://localhost");
        String callback = null;
        if (upload) {
            appUrl = appUrl + "/upload?dvLocale={localeCode}";
            callback = SystemConfig.getDataverseSiteUrlStatic() + "/api/v1/datasets/" + d.getId()
                    + "/globusUploadParameters?locale=" + localeCode;
        } else {
            // Download
            JsonObject files = GlobusUtil.getFilesMap(dataFiles, d);

            String downloadId = UUID.randomUUID().toString();
            downloadCache.put(downloadId, files);
            appUrl = appUrl + "/download?dvLocale={localeCode}";
            callback = SystemConfig.getDataverseSiteUrlStatic() + "/api/v1/datasets/" + d.getId()
                    + "/globusDownloadParameters?locale=" + localeCode + "&downloadId=" + downloadId;

        }
        if (apiToken != null) {
            callback = UrlSignerUtil.signUrl(callback, 5, apiToken.getAuthenticatedUser().getUserIdentifier(),
                    HttpMethod.GET,
                    JvmSettings.API_SIGNING_SECRET.lookupOptional().orElse("") + apiToken.getTokenString());
        } else {
            // Shouldn't happen
            logger.warning("Unable to get api token for user: " + user.getIdentifier());
        }
        appUrl = appUrl + "&callback=" + Base64.getEncoder().encodeToString(StringUtils.getBytesUtf8(callback));

        String finalUrl = tokenUtil.replaceTokensWithValues(appUrl);
        logger.fine("Calling app: " + finalUrl);
        return finalUrl;
    }

    private String getGlobusDownloadScript(Dataset dataset, ApiToken apiToken, List<DataFile> downloadDFList) {
        return URLTokenUtil.getScriptForUrl(getGlobusAppUrlForDataset(dataset, false, downloadDFList));
    }

    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void globusUpload(JsonObject jsonData, Dataset dataset, String httpRequestUrl,
            AuthenticatedUser authUser) throws IllegalArgumentException, ExecutionException, InterruptedException, MalformedURLException {

        // Before we do anything else, let's do some basic validation of what
        // we've been passed:
        
        JsonArray filesJsonArray = jsonData.getJsonArray("files");

        if (filesJsonArray == null || filesJsonArray.size() < 1) {
            throw new IllegalArgumentException("No valid json entries supplied for the files being uploaded");
        }
        
        Date startDate = new Date();
        
        String logTimestamp = logFormatter.format(startDate);
        Logger globusLogger = Logger.getLogger(
                "edu.harvard.iq.dataverse.upload.client.DatasetServiceBean." + "GlobusUpload" + logTimestamp);
        String logFileName = System.getProperty("com.sun.aas.instanceRoot") + File.separator + "logs" + File.separator + "globusUpload_" + dataset.getId() + "_" + logTimestamp
                + ".log";
        FileHandler fileHandler;

        try {
            fileHandler = new FileHandler(logFileName);
            globusLogger.setUseParentHandlers(false);
        } catch (IOException | SecurityException ex) {
            Logger.getLogger(DatasetServiceBean.class.getName()).log(Level.SEVERE, null, ex);
            fileHandler = null;
        }

        if (fileHandler != null) {
            globusLogger.addHandler(fileHandler);
        } else {
            globusLogger = logger;
        }

        logger.fine("json: " + JsonUtil.prettyPrint(jsonData));
        
        globusLogger.info("Globus upload initiated");

        String taskIdentifier = jsonData.getString("taskIdentifier");

        GlobusEndpoint endpoint = getGlobusEndpoint(dataset);
        GlobusTaskState taskState = getTask(endpoint.getClientToken(), taskIdentifier, globusLogger);
        String ruleId = getRuleId(endpoint, taskState.getOwner_id(), "rw");
        logger.fine("Found rule: " + ruleId);
        if (ruleId != null) {
            Long datasetId = rulesCache.getIfPresent(ruleId);
            if (datasetId != null) {
                // Will not delete rule
                rulesCache.invalidate(ruleId);
            }
        }
        
        // Wait before first check
        Thread.sleep(5000);
        
        if (FeatureFlags.GLOBUS_USE_EXPERIMENTAL_ASYNC_FRAMEWORK.enabled()) {
            
            // Save the task information in the database so that the Globus monitoring
            // service can continue checking on its progress.
            
            GlobusTaskInProgress taskInProgress = new GlobusTaskInProgress(taskIdentifier, GlobusTaskInProgress.TaskType.UPLOAD, dataset, endpoint.getClientToken(), authUser, ruleId, new Timestamp(startDate.getTime()));
            em.persist(taskInProgress);
            
            // Save the metadata entries that define the files that are being uploaded
            // in the database. These entries will be used once/if the uploads
            // completes successfully to add the files to the dataset. 

            for (JsonObject fileJsonObject : filesJsonArray.getValuesAs(JsonObject.class)) {
                ExternalFileUploadInProgress fileUploadRecord = new ExternalFileUploadInProgress(taskIdentifier, fileJsonObject.toString());

                em.persist(fileUploadRecord);
            }
            
            if (fileHandler != null) {
                fileHandler.close();
            }

            // return and forget
            return;
        }
        
        
        // the old implementation that relies on looping continuosly, 
        // sleeping-then-checking the task status repeatedly:
        
        // globus task status check
        // (the following method performs continuous looped checks of the remote
        // Globus API, monitoring it for as long as it takes for the task to 
        // finish one way or another!)
        taskState = globusStatusCheck(endpoint, taskIdentifier, globusLogger);
        // @todo null check, or make sure it's never null
        String taskStatus = GlobusUtil.getTaskStatus(taskState);

        boolean taskSuccess = GlobusUtil.isTaskCompleted(taskState);
        
        processCompletedUploadTask(dataset, filesJsonArray, authUser, ruleId, globusLogger, taskSuccess, taskStatus);
        
        if (fileHandler != null) {
            fileHandler.close();
        }
    }
    /**
     * As the name suggests, the method completes and finalizes an upload task, 
     * whether it completed successfully or failed. (In the latter case, it 
     * simply sends a failure notification and does some cleanup). 
     * The method is called in both task monitoring scenarios: the old method, 
     * that relies on continuous looping, and the new, implemented on the basis
     * of timer-like monitoring from a dedicated monitoring Singleton service.
     * @param dataset           the dataset
     * @param filesJsonArray    JsonArray containing files metadata entries as passed to /addGlobusFiles
     * @param authUser          the user that should be be performing the addFiles call 
     *                          finalizing adding the files to the Dataset. Note that this 
     *                          user will need to be obtained from the saved api token, when this
     *                          method is called via the TaskMonitoringService
     * @param ruleId            Globus rule/permission id associated with the task
     * @param myLogger          the Logger; if null, the main logger of the service bean will be used
     * @param fileHandler       FileHandler associated with the Logger, when not null
     * @param taskSuccess       boolean task status of the completed task
     * @param taskState         human-readable task status label as reported by the Globus API
     * the method should not throw any exceptions; all the exceptions thrown 
     * by the methods within are expected to be intercepted. 
     */
    private void processCompletedUploadTask(Dataset dataset, 
            JsonArray filesJsonArray, 
            AuthenticatedUser authUser, 
            String ruleId, 
            Logger globusLogger,
            boolean taskSuccess, 
            String taskStatus) {
        
        Logger myLogger = globusLogger == null ? logger : globusLogger;
        
        if (ruleId != null) {
            // Transfer is complete, so delete rule
            deletePermission(ruleId, dataset, myLogger);
        }
        
        // If success, switch to an EditInProgress lock - do this before removing the
        // GlobusUpload lock
        // Keeping a lock through the add datafiles API call avoids a conflicting edit
        // and keeps any open dataset page refreshing until the datafile appears.
        
        if (taskSuccess) {
            myLogger.info("Finished upload via Globus job.");

            DatasetLock editLock = datasetSvc.addDatasetLock(dataset.getId(), 
                    DatasetLock.Reason.EditInProgress, 
                    (authUser).getId(), 
                    "Completing Globus Upload");
            if (editLock != null) {
                dataset.addLock(editLock);
            } else {
                myLogger.log(Level.WARNING, "Failed to lock the dataset (dataset id={0})", dataset.getId());
            }
        }

        DatasetLock gLock = dataset.getLockFor(DatasetLock.Reason.GlobusUpload);
        if (gLock == null) {
            logger.log(Level.WARNING, "No lock found for dataset");
        } else {
            logger.log(Level.FINE, "Removing GlobusUpload lock " + gLock.getId());
            /*
             * Note: This call to remove a lock only works immediately because it is in
             * another service bean. Despite the removeDatasetLocks method having the
             * REQUIRES_NEW transaction annotation, when the globusUpload method and that
             * method were in the same bean (globusUpload was in the DatasetServiceBean to
             * start), the globus lock was still seen in the API call initiated in the
             * addFilesAsync method called within the globusUpload method. I.e. it appeared
             * that the lock removal was not committed/visible outside this method until
             * globusUpload itself ended.
             * (from @landreev:) If I understand the comment above correctly - annotations 
             * like "@TransactionAttribute(REQUIRES_NEW) do NOT work when you call a method 
             * directly within the same service bean. Strictly speaking, it's not the 
             * "within the same bean" part that is the key, rather, these annotations
             * only apply when calling a method via an @EJB-defined service. So it 
             * is generally possible to call another method within FooServiceBean 
             * with the REQUIRES_NEW transaction taking effect - but then it would need
             * to define *itself* as an @EJB - 
             * @EJB FooServiceBean fooSvc; 
             * ...
             * fooSvc.doSomethingInNewTransaction(...); 
             * etc. 
             */
            datasetSvc.removeDatasetLocks(dataset, DatasetLock.Reason.GlobusUpload);
        }
        
        if (!taskSuccess) {
            String comment; 
            if (taskStatus != null) {
                comment = "Reason : " + taskStatus.split("#")[1] + "<br> Short Description : "
                        + taskStatus.split("#")[2];
            } else {
                comment = "No further information available";
            }
            
            myLogger.info("Globus Upload task failed ");
            userNotificationService.sendNotification((AuthenticatedUser) authUser, new Timestamp(new Date().getTime()),
                    UserNotification.Type.GLOBUSUPLOADREMOTEFAILURE, dataset.getId(), comment, true);

        } else {
            try {
                processUploadedFiles(filesJsonArray, dataset, authUser, myLogger);
            } catch (Exception e) {
                logger.info("Exception from processUploadedFiles call ");
                e.printStackTrace();
                myLogger.info("Exception from processUploadedFiles call " + e.getMessage());
                datasetSvc.removeDatasetLocks(dataset, DatasetLock.Reason.EditInProgress);
            }
        }
        if (ruleId != null) {
            deletePermission(ruleId, dataset, myLogger);
            myLogger.info("Removed upload permission: " + ruleId);
        }
        //if (fileHandler != null) {
        //    fileHandler.close();
        //}
        
    }
    
    
    /**
     * The code in this method is copy-and-pasted from the previous Borealis 
     * implemenation. 
     * @todo see if it can be refactored and simplified a bit, the json manipulation 
     *       specifically (?)
     * @param filesJsonArray    JsonArray containing files metadata entries as passed to /addGlobusFiles
     * @param dataset           the dataset
     * @param authUser          the user that should be be performing the addFiles call 
     *                          finalizing adding the files to the Dataset. Note that this 
     *                          user will need to be obtained from the saved api token, when this
     *                          method is called via the TaskMonitoringService
     * @param myLogger          the Logger; if null, the main logger of the service bean will be used
     * @throws IOException, InterruptedException, ExecutionException @todo may need to throw more exceptions (?)
     */
    private void processUploadedFiles(JsonArray filesJsonArray, Dataset dataset, AuthenticatedUser authUser, Logger myLogger) throws IOException, InterruptedException, ExecutionException {
        myLogger = myLogger != null ? myLogger : logger; 
        
        Integer countAll = 0;
        Integer countSuccess = 0;
        Integer countError = 0;
        Integer countAddFilesSuccess = 0;
        String notificationErrorMessage = ""; 
        
        List<String> inputList = new ArrayList<String>();

        String datasetIdentifier = dataset.getAuthorityForFileStorage() + "/"
                + dataset.getIdentifierForFileStorage();

        for (JsonObject fileJsonObject : filesJsonArray.getValuesAs(JsonObject.class)) {

            // storageIdentifier s3://gcs5-bucket1:1781cfeb8a7-748c270a227c from
            // externalTool
            String storageIdentifier = fileJsonObject.getString("storageIdentifier");
            String[] parts = DataAccess.getDriverIdAndStorageLocation(storageIdentifier);
            String storeId = parts[0];
            // If this is an S3 store, we need to split out the bucket name
            String[] bits = parts[1].split(":");
            String bucketName = "";
            if (bits.length > 1) {
                bucketName = bits[0];
            }
            String fileId = bits[bits.length - 1];

            // fullpath s3://gcs5-bucket1/10.5072/FK2/3S6G2E/1781cfeb8a7-4ad9418a5873
            // or globus:///10.5072/FK2/3S6G2E/1781cfeb8a7-4ad9418a5873
            String fullPath = storeId + "://" + bucketName + "/" + datasetIdentifier + "/" + fileId;
            String fileName = fileJsonObject.getString("fileName");

            inputList.add(fileId + "IDsplit" + fullPath + "IDsplit" + fileName);
        }

        // calculateMissingMetadataFields: checksum, mimetype
        JsonObject newfilesJsonObject = calculateMissingMetadataFields(inputList, myLogger);
        JsonArray newfilesJsonArray = newfilesJsonObject.getJsonArray("files");
        logger.fine("Size: " + newfilesJsonArray.size());
        logger.fine("Val: " + JsonUtil.prettyPrint(newfilesJsonArray.getJsonObject(0)));
        JsonArrayBuilder addFilesJsonData = Json.createArrayBuilder();

        for (JsonObject fileJsonObject : filesJsonArray.getValuesAs(JsonObject.class)) {

            countAll++;
            String storageIdentifier = fileJsonObject.getString("storageIdentifier");
            String fileName = fileJsonObject.getString("fileName");
            String[] parts = DataAccess.getDriverIdAndStorageLocation(storageIdentifier);
            // If this is an S3 store, we need to split out the bucket name
            String[] bits = parts[1].split(":");
            if (bits.length > 1) {
            }
            String fileId = bits[bits.length - 1];

            List<JsonObject> newfileJsonObject = IntStream.range(0, newfilesJsonArray.size())
                    .mapToObj(index -> ((JsonObject) newfilesJsonArray.get(index)).getJsonObject(fileId))
                    .filter(Objects::nonNull).collect(Collectors.toList());
            if (newfileJsonObject != null) {
                logger.fine("List Size: " + newfileJsonObject.size());
                // if (!newfileJsonObject.get(0).getString("hash").equalsIgnoreCase("null")) {
                JsonPatch path = Json.createPatchBuilder()
                        .add("/md5Hash", newfileJsonObject.get(0).getString("hash")).build();
                fileJsonObject = path.apply(fileJsonObject);
                path = Json.createPatchBuilder()
                        .add("/mimeType", newfileJsonObject.get(0).getString("mime")).build();
                fileJsonObject = path.apply(fileJsonObject);
                addFilesJsonData.add(fileJsonObject);
                countSuccess++;
                // } else {
                // globusLogger.info(fileName
                // + " will be skipped from adding to dataset by second API due to missing
                // values ");
                // countError++;
                // }
            } else {
                myLogger.info(fileName
                        + " will be skipped from adding to dataset in the final AddReplaceFileHelper.addFiles() call. ");
                countError++;
            }
        }

        String newjsonData = addFilesJsonData.build().toString();

        myLogger.info("Successfully generated new JsonData for addFiles call");

        myLogger.info("Files passed to /addGlobusFiles: " + countAll);
        myLogger.info("Files processed successfully: " + countSuccess);
        myLogger.info("Files failures to process: " + countError);

        if (countSuccess < 1) {
            // We don't have any valid entries to call addFiles() for; so, no 
            // need to proceed. 
            notificationErrorMessage = "Failed to successfully process any of the file entries, "
                    + "out of the " + countAll + " total as submitted to Dataverse";
            userNotificationService.sendNotification((AuthenticatedUser) authUser,
                        new Timestamp(new Date().getTime()), UserNotification.Type.GLOBUSUPLOADREMOTEFAILURE,
                        dataset.getId(), notificationErrorMessage, true);
            return;
        } else if (countSuccess < countAll) {
            notificationErrorMessage = "Out of the " + countAll + " file entries submitted to /addGlobusFiles " 
                    + "only " + countSuccess + " could be successfully parsed and processed. ";
        }
        
        // A new AddReplaceFileHelper implementation, replacing the old one that 
        // was relying on calling /addFiles api via curl: 
        
        // Passing null for the HttpServletRequest to make a new DataverseRequest. 
        // The parent method is always executed asynchronously, so the real request 
        // that was associated with the original API call that triggered this upload
        // cannot be obtained. 
        DataverseRequest dataverseRequest = new DataverseRequest(authUser, (HttpServletRequest)null);
        
        AddReplaceFileHelper addFileHelper = new AddReplaceFileHelper(
                dataverseRequest,
                this.ingestSvc,
                this.datasetSvc,
                this.dataFileSvc,
                this.permissionSvc,
                this.commandEngine,
                this.systemConfig
        );
                
        // The old code had 2 sec. of sleep, so ...
        Thread.sleep(2000);

        Response addFilesResponse = addFileHelper.addFiles(newjsonData, dataset, authUser);

        if (addFilesResponse == null) {
            logger.info("null response from addFiles call");
            //@todo add this case to the user notification in case of error
            return;
        }
        
        JsonObject addFilesJsonObject = JsonUtil.getJsonObject(addFilesResponse.getEntity().toString());
        
        // @todo null check?
        String addFilesStatus = addFilesJsonObject.getString("status", null);
        myLogger.info("addFilesResponse status: " + addFilesStatus);
        
        if (ApiConstants.STATUS_OK.equalsIgnoreCase(addFilesStatus)) {
            if (addFilesJsonObject.containsKey("data") && addFilesJsonObject.getJsonObject("data").containsKey("Result")) {

                //Integer countAddFilesTotal = addFilesJsonObject.getJsonObject("data").getJsonObject("Result").getInt(ApiConstants.API_ADD_FILES_COUNT_PROCESSED, -1);
                countAddFilesSuccess = addFilesJsonObject.getJsonObject("data").getJsonObject("Result").getInt(ApiConstants.API_ADD_FILES_COUNT_SUCCESSFUL, -1);
                myLogger.info("Files successfully added by addFiles(): " + countAddFilesSuccess);

            } else {
                myLogger.warning("Malformed addFiles response json: " + addFilesJsonObject.toString());
                notificationErrorMessage = "Malformed response received when attempting to add the files to the dataset. ";
            }

            myLogger.info("Completed addFiles call ");
        } else if (ApiConstants.STATUS_ERROR.equalsIgnoreCase(addFilesStatus)) {
            String addFilesMessage = addFilesJsonObject.getString("message", null);

            myLogger.log(Level.SEVERE,
                    "******* Error while executing addFiles ", newjsonData);
            myLogger.log(Level.SEVERE, "****** Output from addFiles: ", addFilesMessage);
            notificationErrorMessage += "Error response received when attempting to add the files to the dataset: " + addFilesMessage + " "; 

        } else {
            myLogger.log(Level.SEVERE,
                    "******* Error while executing addFiles ", newjsonData);
            notificationErrorMessage += "Unexpected error encountered when attemptingh to add the files to the dataset.";
        }
        
        // if(!taskSkippedFiles)
        if (countAddFilesSuccess == countAll) {
            userNotificationService.sendNotification((AuthenticatedUser) authUser,
                    new Timestamp(new Date().getTime()), UserNotification.Type.GLOBUSUPLOADCOMPLETED,
                    dataset.getId(), countSuccess + " files added out of " + countAll, true);
        } else if (countAddFilesSuccess > 0) {
            // success, but partial:
            userNotificationService.sendNotification((AuthenticatedUser) authUser,
                    new Timestamp(new Date().getTime()),
                    UserNotification.Type.GLOBUSUPLOADCOMPLETEDWITHERRORS, dataset.getId(),
                    countSuccess + " files added out of " + countAll + notificationErrorMessage, true);
        } else {
            notificationErrorMessage = "".equals(notificationErrorMessage) 
                    ? " No additional information is available." : notificationErrorMessage;
            userNotificationService.sendNotification((AuthenticatedUser) authUser,
                    new Timestamp(new Date().getTime()),
                    UserNotification.Type.GLOBUSUPLOADLOCALFAILURE, dataset.getId(),
                    notificationErrorMessage, true);
        }

    }
    
    @Asynchronous
    public void globusDownload(String jsonData, Dataset dataset, User authUser) throws MalformedURLException {

        String logTimestamp = logFormatter.format(new Date());
        Logger globusLogger = Logger.getLogger(
                "edu.harvard.iq.dataverse.upload.client.DatasetServiceBean." + "GlobusDownload" + logTimestamp);

        String logFileName = System.getProperty("com.sun.aas.instanceRoot") + File.separator + "logs" + File.separator + "globusDownload_id_" + dataset.getId() + "_" + logTimestamp
                + ".log";
        FileHandler fileHandler;
        boolean fileHandlerSuceeded;
        try {
            fileHandler = new FileHandler(logFileName);
            globusLogger.setUseParentHandlers(false);
            fileHandlerSuceeded = true;
        } catch (IOException | SecurityException ex) {
            Logger.getLogger(DatasetServiceBean.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        if (fileHandlerSuceeded) {
            globusLogger.addHandler(fileHandler);
        } else {
            globusLogger = logger;
        }

        globusLogger.info("Starting a globusDownload ");

        JsonObject jsonObject = null;
        try {
            jsonObject = JsonUtil.getJsonObject(jsonData);
        } catch (Exception jpe) {
            jpe.printStackTrace();
            globusLogger.log(Level.SEVERE, "Error parsing dataset json. Json: {0}", jsonData);
            // TODO: stop the process after this parsing exception.
        }

        String taskIdentifier = jsonObject.getString("taskIdentifier");

        GlobusEndpoint endpoint = getGlobusEndpoint(dataset);
        logger.fine("Endpoint path: " + endpoint.getBasePath());

        // If the rules_cache times out, the permission will be deleted. Presumably that
        // doesn't affect a
        // globus task status check
        GlobusTaskState task = getTask(endpoint.getClientToken(), taskIdentifier, globusLogger);
        String ruleId = getRuleId(endpoint, task.getOwner_id(), "r");
        if (ruleId != null) {
            logger.fine("Found rule: " + ruleId);
            Long datasetId = rulesCache.getIfPresent(ruleId);
            if (datasetId != null) {
                logger.fine("Deleting from cache: rule: " + ruleId);
                // Will not delete rule
                rulesCache.invalidate(ruleId);
            }
        } else {
            // Something is wrong - the rule should be there (a race with the cache timing
            // out?)
            logger.warning("ruleId not found for taskId: " + taskIdentifier);
        }
        task = globusStatusCheck(endpoint, taskIdentifier, globusLogger);
        // @todo null check?
        String taskStatus = GlobusUtil.getTaskStatus(task);

        // Transfer is done (success or failure) so delete the rule
        if (ruleId != null) {
            logger.fine("Deleting: rule: " + ruleId);
            deletePermission(ruleId, dataset, globusLogger);
        }

        if (taskStatus.startsWith("FAILED") || taskStatus.startsWith("INACTIVE")) {
            String comment = "Reason : " + taskStatus.split("#")[1] + "<br> Short Description : "
                    + taskStatus.split("#")[2];
            if (authUser != null && authUser instanceof AuthenticatedUser) {
                userNotificationService.sendNotification((AuthenticatedUser) authUser, new Timestamp(new Date().getTime()),
                        UserNotification.Type.GLOBUSDOWNLOADCOMPLETEDWITHERRORS, dataset.getId(), comment, true);
            }
            
            globusLogger.info("Globus task failed during download process: "+comment);
        } else if (authUser != null && authUser instanceof AuthenticatedUser) {
        
            boolean taskSkippedFiles = (task.getSkip_source_errors() == null) ? false : task.getSkip_source_errors();
            if (!taskSkippedFiles) {
                userNotificationService.sendNotification((AuthenticatedUser) authUser,
                        new Timestamp(new Date().getTime()), UserNotification.Type.GLOBUSDOWNLOADCOMPLETED,
                        dataset.getId());
            } else {
                userNotificationService.sendNotification((AuthenticatedUser) authUser,
                        new Timestamp(new Date().getTime()), UserNotification.Type.GLOBUSDOWNLOADCOMPLETEDWITHERRORS,
                        dataset.getId(), "");
            }
        }
    }

    Executor executor = Executors.newFixedThreadPool(10);

    private GlobusTaskState globusStatusCheck(GlobusEndpoint endpoint, String taskId, Logger globusLogger)
            throws MalformedURLException {
        boolean taskCompleted = false;
        GlobusTaskState task = null;
        int pollingInterval = SystemConfig.getIntLimitFromStringOrDefault(
                settingsSvc.getValueForKey(SettingsServiceBean.Key.GlobusPollingInterval), 50);
        do {
            try {
                globusLogger.info("checking globus transfer task   " + taskId);
                Thread.sleep(pollingInterval * 1000);
                // Call the (centralized) Globus API to check on the task state/status:
                task = getTask(endpoint.getClientToken(), taskId, globusLogger);
                taskCompleted = GlobusUtil.isTaskCompleted(task);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        } while (!taskCompleted);

        globusLogger.info("globus transfer task completed successfully");
        return task;
    }
    
    public JsonObject calculateMissingMetadataFields(List<String> inputList, Logger globusLogger)
            throws InterruptedException, ExecutionException, IOException {

        List<CompletableFuture<FileDetailsHolder>> hashvalueCompletableFutures = inputList.stream()
                .map(iD -> calculateDetailsAsync(iD, globusLogger)).collect(Collectors.toList());

        CompletableFuture<Void> allFutures = CompletableFuture
                .allOf(hashvalueCompletableFutures.toArray(new CompletableFuture[hashvalueCompletableFutures.size()]));

        CompletableFuture<List<FileDetailsHolder>> allCompletableFuture = allFutures.thenApply(future -> {
            return hashvalueCompletableFutures.stream().map(completableFuture -> completableFuture.join())
                    .collect(Collectors.toList());
        });

        CompletableFuture<?> completableFuture = allCompletableFuture.thenApply(files -> {
            return files.stream().map(d -> json(d)).collect(toJsonArray());
        });

        JsonArrayBuilder filesObject = (JsonArrayBuilder) completableFuture.get();

        JsonObject output = Json.createObjectBuilder().add("files", filesObject).build();

        return output;

    }

    private CompletableFuture<FileDetailsHolder> calculateDetailsAsync(String id, Logger globusLogger) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                return (calculateDetails(id, globusLogger));
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
            return null;
        }, executor).exceptionally(ex -> {
            return null;
        });
    }

    private FileDetailsHolder calculateDetails(String id, Logger globusLogger)
            throws InterruptedException, IOException {
        int count = 0;
        String checksumVal = "";
        InputStream in = null;
        String fileId = id.split("IDsplit")[0];
        String fullPath = id.split("IDsplit")[1];
        String fileName = id.split("IDsplit")[2];

        // ToDo: what if the file does not exist in s3
        // (L.A.) - good question. maybe it should call .open and .exists() here? 
        //          otherwise, there doesn't seem to be any diagnostics as to which 
        //          files uploaded successfully and which failed (?)
        //          ... however, any partially successful upload cases should be 
        //          properly handled later, during the .addFiles() call - only 
        //          the files that actually exists in storage remotely will be 
        //          added to the dataset permanently then. 
        // ToDo: what if checksum calculation failed
        // (L.A.) - this appears to have been addressed - by using "Not available in Dataverse"
        //          in place of a checksum. 

        String storageDriverId = DataAccess.getDriverIdAndStorageLocation(fullPath)[0];

        if (StorageIO.isDataverseAccessible(storageDriverId)) {
            do {
                try {
                    StorageIO<DvObject> dataFileStorageIO = DataAccess.getDirectStorageIO(fullPath);
                        in = dataFileStorageIO.getInputStream();
                    checksumVal = FileUtil.calculateChecksum(in, DataFile.ChecksumType.MD5);
                    count = 3;
                } catch (IOException ioex) {
                    count = 3;
                    logger.fine(ioex.getMessage());
                    globusLogger.info(
                            "DataFile (fullPath " + fullPath + ") does not appear to be accessible within Dataverse: ");
                } catch (Exception ex) {
                    count = count + 1;
                    ex.printStackTrace();
                    logger.info(ex.getMessage());
                    Thread.sleep(5000);
                }
            } while (count < 3);
        }

        if (checksumVal.length() == 0) {
            checksumVal = "Not available in Dataverse";
        }

        String mimeType = calculatemime(fileName);
        globusLogger.info(" File Name " + fileName + "  File Details " + fileId + " checksum = " + checksumVal
                + " mimeType = " + mimeType);
        return new FileDetailsHolder(fileId, checksumVal, mimeType);
        // getBytes(in)+"" );
        // calculatemime(fileName));
    }

    public String calculatemime(String fileName) throws InterruptedException {

        String finalType = FileUtil.MIME_TYPE_UNDETERMINED_DEFAULT;
        String type = FileUtil.determineFileTypeByNameAndExtension(fileName);

        if (type != null && !type.isBlank()) {
            if (FileUtil.useRecognizedType(finalType, type)) {
                finalType = type;
            }
        }

        return finalType;
    }

    private GlobusEndpoint getGlobusEndpoint(DvObject dvObject) {
        Dataset dataset = null;
        if (dvObject instanceof Dataset) {
            dataset = (Dataset) dvObject;
        } else if (dvObject instanceof DataFile) {
            dataset = (Dataset) dvObject.getOwner();
        } else {
            throw new IllegalArgumentException("Unsupported DvObject type: " + dvObject.getClass().getName());
        }
        String driverId = dataset.getEffectiveStorageDriverId();
        GlobusEndpoint endpoint = null;

        String directoryPath = GlobusAccessibleStore.getTransferPath(driverId);

        if (GlobusAccessibleStore.isDataverseManaged(driverId) && (dataset != null)) {
            directoryPath = directoryPath + "/" + dataset.getAuthorityForFileStorage() + "/"
                    + dataset.getIdentifierForFileStorage();
        } else {
            // remote store - may have path in file storageidentifier
            String relPath = dvObject.getStorageIdentifier()
                    .substring(dvObject.getStorageIdentifier().lastIndexOf("//") + 2);
            int filenameStart = relPath.lastIndexOf("/") + 1;
            if (filenameStart > 0) {
                directoryPath = directoryPath + relPath.substring(0, filenameStart);
            }
        }
        logger.fine("directoryPath finally: " + directoryPath);

        String endpointId = GlobusAccessibleStore.getTransferEndpointId(driverId);

        logger.fine("endpointId: " + endpointId);

        String globusToken = GlobusAccessibleStore.getGlobusToken(driverId);

        AccessToken accessToken = GlobusServiceBean.getClientToken(globusToken);
        String clientToken = accessToken.getOtherTokens().get(0).getAccessToken();
        endpoint = new GlobusEndpoint(endpointId, clientToken, directoryPath);

        return endpoint;
    }

    // This helper method is called from the Download terms/guestbook/etc. popup,
    // when the user clicks the "ok" button. We use it, instead of calling
    // downloadServiceBean directly, in order to differentiate between single
    // file downloads and multiple (batch) downloads - since both use the same
    // terms/etc. popup.
    public void writeGuestbookAndStartTransfer(GuestbookResponse guestbookResponse,
            boolean doNotSaveGuestbookResponse) {
        PrimeFaces.current().executeScript("PF('guestbookAndTermsPopup').hide()");
        guestbookResponse.setEventType(GuestbookResponse.DOWNLOAD);

        ApiToken apiToken = null;
        User user = session.getUser();
        if (user instanceof AuthenticatedUser) {
            apiToken = authSvc.findApiTokenByUser((AuthenticatedUser) user);
        } else if (user instanceof PrivateUrlUser) {
            PrivateUrlUser privateUrlUser = (PrivateUrlUser) user;
            PrivateUrl privUrl = privateUrlService.getPrivateUrlFromDatasetId(privateUrlUser.getDatasetId());
            apiToken = new ApiToken();
            apiToken.setTokenString(privUrl.getToken());
        }

        DataFile df = guestbookResponse.getDataFile();
        if (df != null) {
            logger.fine("Single datafile case for writeGuestbookAndStartTransfer");
            List<DataFile> downloadDFList = new ArrayList<DataFile>(1);
            downloadDFList.add(df);
            if (!doNotSaveGuestbookResponse) {
                fileDownloadService.writeGuestbookResponseRecord(guestbookResponse);
            }
            PrimeFaces.current().executeScript(getGlobusDownloadScript(df.getOwner(), apiToken, downloadDFList));
        } else {
            // Following FileDownloadServiceBean writeGuestbookAndStartBatchDownload
            List<String> list = new ArrayList<>(Arrays.asList(guestbookResponse.getSelectedFileIds().split(",")));
            List<DataFile> selectedFiles = new ArrayList<DataFile>();
            for (String idAsString : list) {
                try {
                    Long fileId = Long.parseLong(idAsString);
                    // If we need to create a GuestBookResponse record, we have to
                    // look up the DataFile object for this file:
                    df = dataFileSvc.findCheapAndEasy(fileId);
                    selectedFiles.add(df);
                    if (!doNotSaveGuestbookResponse) {
                        guestbookResponse.setDataFile(df);
                        fileDownloadService.writeGuestbookResponseRecord(guestbookResponse);
                    }
                } catch (NumberFormatException nfe) {
                    logger.warning(
                            "A file id passed to the writeGuestbookAndStartTransfer method as a string could not be converted back to Long: "
                                    + idAsString);
                    return;
                }

            }
            if (!selectedFiles.isEmpty()) {
                // Use dataset from one file - files should all be from the same dataset
                PrimeFaces.current().executeScript(getGlobusDownloadScript(df.getOwner(), apiToken, selectedFiles));
            }
        }
    }
    
    public List<GlobusTaskInProgress> findAllOngoingTasks() {
        return em.createQuery("select object(o) from GlobusTaskInProgress as o order by o.startTime", GlobusTaskInProgress.class).getResultList();
    }
    
    public void deleteTask(GlobusTaskInProgress task) {
        GlobusTaskInProgress mergedTask = em.merge(task);
        em.remove(mergedTask);
    }
    
    public List<ExternalFileUploadInProgress> findExternalUploadsByTaskId(String taskId) {
        return em.createNamedQuery("ExternalFileUploadInProgress.findByTaskId").setParameter("taskId", taskId).getResultList();    
    }
    
    public void processCompletedTask(GlobusTaskInProgress globusTask, boolean taskSuccess, String taskStatus, Logger taskLogger) {
        String ruleId = globusTask.getRuleId();
        Dataset dataset = globusTask.getDataset();
        AuthenticatedUser authUser = globusTask.getLocalUser();
        if (authUser == null) {
            // @todo log error message; do nothing 
            return;
        }

        if (GlobusTaskInProgress.TaskType.UPLOAD.equals(globusTask.getTaskType())) {
            List<ExternalFileUploadInProgress> fileUploadsInProgress = findExternalUploadsByTaskId(globusTask.getTaskId());

            if (fileUploadsInProgress == null || fileUploadsInProgress.size() < 1) {
                // @todo log error message; do nothing
                // (will this ever happen though?)
                return;
            }

            JsonArrayBuilder filesJsonArrayBuilder = Json.createArrayBuilder();

            for (ExternalFileUploadInProgress pendingFile : fileUploadsInProgress) {
                String jsonInfoString = pendingFile.getFileInfo();
                JsonObject fileObject = JsonUtil.getJsonObject(jsonInfoString);
                filesJsonArrayBuilder.add(fileObject);
            }

            JsonArray filesJsonArray = filesJsonArrayBuilder.build();

            processCompletedUploadTask(dataset, filesJsonArray, authUser, ruleId, taskLogger, taskSuccess, taskStatus);
        } else {
            // @todo eventually, extend this async. framework to handle Glonus downloads as well
        }

    }
            
    public void deleteExternalUploadRecords(String taskId) {
        em.createNamedQuery("ExternalFileUploadInProgress.deleteByTaskId")
                .setParameter("taskId", taskId)
                .executeUpdate();
    }
}
