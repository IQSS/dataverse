package edu.harvard.iq.dataverse.globus;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import edu.harvard.iq.dataverse.*;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonPatch;
import javax.servlet.http.HttpServletRequest;

import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.toJsonArray;

import java.io.*;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
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

import com.google.gson.Gson;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.URLTokenUtil;
import edu.harvard.iq.dataverse.util.json.JsonUtil;

@Stateless
@Named("GlobusServiceBean")
public class GlobusServiceBean implements java.io.Serializable {

    @EJB
    protected DatasetServiceBean datasetSvc;

    @EJB
    protected SettingsServiceBean settingsSvc;

    @Inject
    DataverseSession session;

    @EJB
    protected AuthenticationServiceBean authSvc;

    @EJB
    EjbDataverseEngine commandEngine;

    @EJB
    UserNotificationServiceBean userNotificationService;

    private static final Logger logger = Logger.getLogger(GlobusServiceBean.class.getCanonicalName());
    private static final SimpleDateFormat logFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss");

    private String code;
    private String userTransferToken;
    private String state;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getUserTransferToken() {
        return userTransferToken;
    }

    public void setUserTransferToken(String userTransferToken) {
        this.userTransferToken = userTransferToken;
    }

    ArrayList<String> checkPermisions(AccessToken clientTokenUser, String directory, String globusEndpoint,
            String principalType, String principal) throws MalformedURLException {
        URL url = new URL("https://transfer.api.globusonline.org/v0.10/endpoint/" + globusEndpoint + "/access_list");
        MakeRequestResponse result = makeRequest(url, "Bearer",
                clientTokenUser.getOtherTokens().get(0).getAccessToken(), "GET", null);
        ArrayList<String> ids = new ArrayList<String>();
        if (result.status == 200) {
            AccessList al = parseJson(result.jsonResponse, AccessList.class, false);

            for (int i = 0; i < al.getDATA().size(); i++) {
                Permissions pr = al.getDATA().get(i);
                if ((pr.getPath().equals(directory + "/") || pr.getPath().equals(directory))
                        && pr.getPrincipalType().equals(principalType)
                        && ((principal == null) || (principal != null && pr.getPrincipal().equals(principal)))) {
                    ids.add(pr.getId());
                } else {
                    logger.info(pr.getPath() + " === " + directory + " == " + pr.getPrincipalType());
                    continue;
                }
            }
        }

        return ids;
    }

    public void updatePermision(AccessToken clientTokenUser, String directory, String principalType, String perm)
            throws MalformedURLException {
        if (directory != null && !directory.equals("")) {
            directory = directory + "/";
        }
        logger.info("Start updating permissions." + " Directory is " + directory);
        String globusEndpoint = settingsSvc.getValueForKey(SettingsServiceBean.Key.GlobusEndpoint, "");
        ArrayList<String> rules = checkPermisions(clientTokenUser, directory, globusEndpoint, principalType, null);
        logger.info("Size of rules " + rules.size());
        int count = 0;
        while (count < rules.size()) {
            logger.info("Start removing rules " + rules.get(count));
            Permissions permissions = new Permissions();
            permissions.setDATA_TYPE("access");
            permissions.setPermissions(perm);
            permissions.setPath(directory);

            Gson gson = new GsonBuilder().create();
            URL url = new URL("https://transfer.api.globusonline.org/v0.10/endpoint/" + globusEndpoint + "/access/"
                    + rules.get(count));
            logger.info("https://transfer.api.globusonline.org/v0.10/endpoint/" + globusEndpoint + "/access/"
                    + rules.get(count));
            MakeRequestResponse result = makeRequest(url, "Bearer",
                    clientTokenUser.getOtherTokens().get(0).getAccessToken(), "PUT", gson.toJson(permissions));
            if (result.status != 200) {
                logger.warning("Cannot update access rule " + rules.get(count));
            } else {
                logger.info("Access rule " + rules.get(count) + " was updated");
            }
            count++;
        }
    }

    public void deletePermision(String ruleId, Logger globusLogger) throws MalformedURLException {

        if (ruleId.length() > 0) {
            AccessToken clientTokenUser = getClientToken();
            globusLogger.info("Start deleting permissions.");
            String globusEndpoint = settingsSvc.getValueForKey(SettingsServiceBean.Key.GlobusEndpoint, "");

            URL url = new URL(
                    "https://transfer.api.globusonline.org/v0.10/endpoint/" + globusEndpoint + "/access/" + ruleId);
            MakeRequestResponse result = makeRequest(url, "Bearer",
                    clientTokenUser.getOtherTokens().get(0).getAccessToken(), "DELETE", null);
            if (result.status != 200) {
                globusLogger.warning("Cannot delete access rule " + ruleId);
            } else {
                globusLogger.info("Access rule " + ruleId + " was deleted successfully");
            }
        }

    }

    public int givePermission(String principalType, String principal, String perm, AccessToken clientTokenUser,
            String directory, String globusEndpoint) throws MalformedURLException {

        ArrayList<?> rules = checkPermisions(clientTokenUser, directory, globusEndpoint, principalType, principal);

        Permissions permissions = new Permissions();
        permissions.setDATA_TYPE("access");
        permissions.setPrincipalType(principalType);
        permissions.setPrincipal(principal);
        permissions.setPath(directory + "/");
        permissions.setPermissions(perm);

        Gson gson = new GsonBuilder().create();
        MakeRequestResponse result = null;
        if (rules.size() == 0) {
            logger.info("Start creating the rule");
            URL url = new URL("https://transfer.api.globusonline.org/v0.10/endpoint/" + globusEndpoint + "/access");
            result = makeRequest(url, "Bearer", clientTokenUser.getOtherTokens().get(0).getAccessToken(), "POST",
                    gson.toJson(permissions));

            if (result.status == 400) {
                logger.severe("Path " + permissions.getPath() + " is not valid");
            } else if (result.status == 409) {
                logger.warning("ACL already exists or Endpoint ACL already has the maximum number of access rules");
            }

            return result.status;
        } else {
            logger.info("Start Updating the rule");
            URL url = new URL("https://transfer.api.globusonline.org/v0.10/endpoint/" + globusEndpoint + "/access/"
                    + rules.get(0));
            result = makeRequest(url, "Bearer", clientTokenUser.getOtherTokens().get(0).getAccessToken(), "PUT",
                    gson.toJson(permissions));

            if (result.status == 400) {
                logger.severe("Path " + permissions.getPath() + " is not valid");
            } else if (result.status == 409) {
                logger.warning("ACL already exists or Endpoint ACL already has the maximum number of access rules");
            }
            logger.info("Result status " + result.status);
        }

        return result.status;
    }

    public boolean getSuccessfulTransfers(AccessToken clientTokenUser, String taskId) throws MalformedURLException {

        URL url = new URL("https://transfer.api.globusonline.org/v0.10/endpoint_manager/task/" + taskId
                + "/successful_transfers");

        MakeRequestResponse result = makeRequest(url, "Bearer",
                clientTokenUser.getOtherTokens().get(0).getAccessToken(), "GET", null);

        if (result.status == 200) {
            logger.info(" SUCCESS ====== ");
            return true;
        }
        return false;
    }

    public GlobusTask getTask(AccessToken clientTokenUser, String taskId, Logger globusLogger) throws MalformedURLException {

        URL url = new URL("https://transfer.api.globusonline.org/v0.10/endpoint_manager/task/" + taskId);

        MakeRequestResponse result = makeRequest(url, "Bearer",
                clientTokenUser.getOtherTokens().get(0).getAccessToken(), "GET", null);

        GlobusTask task = null;

        if (result.status == 200) {
            task = parseJson(result.jsonResponse, GlobusTask.class, false);
        }
        if (result.status != 200) {
            globusLogger.warning("Cannot find information for the task " + taskId + " : Reason :   "
                    + result.jsonResponse.toString());
        }

        return task;
    }

    public AccessToken getClientToken() throws MalformedURLException {
        String globusBasicToken = settingsSvc.getValueForKey(SettingsServiceBean.Key.GlobusBasicToken, "");
        URL url = new URL(
                "https://auth.globus.org/v2/oauth2/token?scope=openid+email+profile+urn:globus:auth:scope:transfer.api.globus.org:all&grant_type=client_credentials");

        MakeRequestResponse result = makeRequest(url, "Basic", globusBasicToken, "POST", null);
        AccessToken clientTokenUser = null;
        if (result.status == 200) {
            clientTokenUser = parseJson(result.jsonResponse, AccessToken.class, true);
        }
        return clientTokenUser;
    }

    public AccessToken getAccessToken(HttpServletRequest origRequest, String globusBasicToken)
            throws UnsupportedEncodingException, MalformedURLException {
        String serverName = origRequest.getServerName();
        if (serverName.equals("localhost")) {
            logger.severe("Changing localhost to utoronto");
            serverName = "utl-192-123.library.utoronto.ca";
        }

        String redirectURL = "https://" + serverName + "/globus.xhtml";

        redirectURL = URLEncoder.encode(redirectURL, "UTF-8");

        URL url = new URL("https://auth.globus.org/v2/oauth2/token?code=" + code + "&redirect_uri=" + redirectURL
                + "&grant_type=authorization_code");
        logger.info(url.toString());

        MakeRequestResponse result = makeRequest(url, "Basic", globusBasicToken, "POST", null);
        AccessToken accessTokenUser = null;

        if (result.status == 200) {
            logger.info("Access Token: \n" + result.toString());
            accessTokenUser = parseJson(result.jsonResponse, AccessToken.class, true);
            logger.info(accessTokenUser.getAccessToken());
        }

        return accessTokenUser;

    }

    public MakeRequestResponse makeRequest(URL url, String authType, String authCode, String method,
            String jsonString) {
        String str = null;
        HttpURLConnection connection = null;
        int status = 0;
        try {
            connection = (HttpURLConnection) url.openConnection();
            // Basic
            // NThjMGYxNDQtN2QzMy00ZTYzLTk3MmUtMjljNjY5YzJjNGJiOktzSUVDMDZtTUxlRHNKTDBsTmRibXBIbjZvaWpQNGkwWVVuRmQyVDZRSnc9
            logger.info(authType + " " + authCode);
            connection.setRequestProperty("Authorization", authType + " " + authCode);
            // connection.setRequestProperty("Content-Type",
            // "application/x-www-form-urlencoded");
            connection.setRequestMethod(method);
            if (jsonString != null) {
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                logger.info(jsonString);
                connection.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
                wr.write(jsonString);
                wr.flush();
            }

            status = connection.getResponseCode();
            logger.info("Status now " + status);
            InputStream result = connection.getInputStream();
            if (result != null) {
                logger.info("Result is not null");
                str = readResultJson(result).toString();
                logger.info("str is ");
                logger.info(result.toString());
            } else {
                logger.info("Result is null");
                str = null;
            }

            logger.info("status: " + status);
        } catch (IOException ex) {
            logger.info("IO");
            logger.severe(ex.getMessage());
            logger.info(ex.getCause().toString());
            logger.info(ex.getStackTrace().toString());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        MakeRequestResponse r = new MakeRequestResponse(str, status);
        return r;

    }

    private StringBuilder readResultJson(InputStream in) {
        StringBuilder sb = null;
        try {

            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }
            br.close();
            logger.info(sb.toString());
        } catch (IOException e) {
            sb = null;
            logger.severe(e.getMessage());
        }
        return sb;
    }

    private <T> T parseJson(String sb, Class<T> jsonParserClass, boolean namingPolicy) {
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

    public String getDirectory(String datasetId) {
        Dataset dataset = null;
        String directory = null;
        try {
            dataset = datasetSvc.find(Long.parseLong(datasetId));
            if (dataset == null) {
                logger.severe("Dataset not found " + datasetId);
                return null;
            }
            String storeId = dataset.getStorageIdentifier();
            storeId.substring(storeId.indexOf("//") + 1);
            directory = storeId.substring(storeId.indexOf("//") + 1);
            logger.info(storeId);
            logger.info(directory);
            logger.info("Storage identifier:" + dataset.getIdentifierForFileStorage());
            return directory;

        } catch (NumberFormatException nfe) {
            logger.severe(nfe.getMessage());

            return null;
        }

    }

    class MakeRequestResponse {
        public String jsonResponse;
        public int status;

        MakeRequestResponse(String jsonResponse, int status) {
            this.jsonResponse = jsonResponse;
            this.status = status;
        }

    }

    private MakeRequestResponse findDirectory(String directory, AccessToken clientTokenUser, String globusEndpoint)
            throws MalformedURLException {
        URL url = new URL(" https://transfer.api.globusonline.org/v0.10/endpoint/" + globusEndpoint + "/ls?path="
                + directory + "/");

        MakeRequestResponse result = makeRequest(url, "Bearer",
                clientTokenUser.getOtherTokens().get(0).getAccessToken(), "GET", null);
        logger.info("find directory status:" + result.status);

        return result;
    }

    public boolean giveGlobusPublicPermissions(String datasetId)
            throws UnsupportedEncodingException, MalformedURLException {

        String globusEndpoint = settingsSvc.getValueForKey(SettingsServiceBean.Key.GlobusEndpoint, "");
        String globusBasicToken = settingsSvc.getValueForKey(SettingsServiceBean.Key.GlobusBasicToken, "");
        if (globusEndpoint.equals("") || globusBasicToken.equals("")) {
            return false;
        }
        AccessToken clientTokenUser = getClientToken();
        if (clientTokenUser == null) {
            logger.severe("Cannot get client token ");
            return false;
        }

        String directory = getDirectory(datasetId);
        logger.info(directory);

        MakeRequestResponse status = findDirectory(directory, clientTokenUser, globusEndpoint);

        if (status.status == 200) {

            /*
             * FilesList fl = parseJson(status.jsonResponse, FilesList.class, false);
             * ArrayList<FileG> files = fl.getDATA(); if (files != null) { for (FileG file:
             * files) { if (!file.getName().contains("cached") &&
             * !file.getName().contains(".thumb")) { int perStatus =
             * givePermission("all_authenticated_users", "", "r", clientTokenUser, directory
             * + "/" + file.getName(), globusEndpoint); logger.info("givePermission status "
             * + perStatus + " for " + file.getName()); if (perStatus == 409) {
             * logger.info("Permissions already exist or limit was reached for " +
             * file.getName()); } else if (perStatus == 400) {
             * logger.info("No file in Globus " + file.getName()); } else if (perStatus !=
             * 201) { logger.info("Cannot get permission for " + file.getName()); } } } }
             */

            int perStatus = givePermission("all_authenticated_users", "", "r", clientTokenUser, directory,
                    globusEndpoint);
            logger.info("givePermission status " + perStatus);
            if (perStatus == 409) {
                logger.info("Permissions already exist or limit was reached");
            } else if (perStatus == 400) {
                logger.info("No directory in Globus");
            } else if (perStatus != 201 && perStatus != 200) {
                logger.info("Cannot give read permission");
                return false;
            }

        } else if (status.status == 404) {
            logger.info("There is no globus directory");
        } else {
            logger.severe("Cannot find directory in globus, status " + status);
            return false;
        }

        return true;
    }

    // Generates the URL to launch the Globus app
    public String getGlobusAppUrlForDataset(Dataset d) {
        return getGlobusAppUrlForDataset(d, true, null);
    }

    public String getGlobusAppUrlForDataset(Dataset d, boolean upload, DataFile df) {
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
        String storePrefix = "";
        String driverId = d.getEffectiveStorageDriverId();
        try {
            storePrefix = DataAccess.getDriverPrefix(driverId);
        } catch (Exception e) {
            logger.warning("GlobusAppUrlForDataset: Failed to get storePrefix for " + driverId);
        }
        //Use URLTokenUtil for params currently in common with external tools. 
        URLTokenUtil tokenUtil = new URLTokenUtil(d, df, apiToken, localeCode);
        String appUrl;
        if (upload) {
            appUrl = settingsSvc.getValueForKey(SettingsServiceBean.Key.GlobusAppUrl, "http://localhost")
                    + "/upload?datasetPid={datasetPid}&siteUrl={siteUrl}&apiToken={apiToken}&datasetId={datasetId}&datasetVersion={datasetVersion}&dvLocale={localeCode}";
        } else {
            if (df == null) {
                appUrl = settingsSvc.getValueForKey(SettingsServiceBean.Key.GlobusAppUrl, "http://localhost")
                        + "/download?datasetPid={datasetPid}&siteUrl={siteUrl}"
                        + ((apiToken != null) ? "&apiToken={apiToken}" : "")
                        + "&datasetId={datasetId}&datasetVersion={datasetVersion}&dvLocale={localeCode}";
            } else {
                String rawStorageId = df.getStorageIdentifier();
                rawStorageId=rawStorageId.substring(rawStorageId.lastIndexOf(":")+1);
                appUrl = settingsSvc.getValueForKey(SettingsServiceBean.Key.GlobusAppUrl, "http://localhost")
                        + "/download-file?datasetPid={datasetPid}&siteUrl={siteUrl}"
                        + ((apiToken != null) ? "&apiToken={apiToken}" : "")
                        + "&datasetId={datasetId}&datasetVersion={datasetVersion}&dvLocale={localeCode}&fileId={fileId}&storageIdentifier="
                        + rawStorageId + "&fileName=" + df.getCurrentName();
            }
        }
        return tokenUtil.replaceTokensWithValues(appUrl) + "&storePrefix=" + storePrefix;
    }

    public String getGlobusDownloadScript(Dataset dataset, ApiToken apiToken) {
        return URLTokenUtil.getScriptForUrl(getGlobusAppUrlForDataset(dataset, false, null));
        
    }
    
    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void globusUpload(String jsonData, ApiToken token, Dataset dataset, String httpRequestUrl,
            AuthenticatedUser authUser) throws ExecutionException, InterruptedException, MalformedURLException {

        Integer countAll = 0;
        Integer countSuccess = 0;
        Integer countError = 0;
        String logTimestamp = logFormatter.format(new Date());
        Logger globusLogger = Logger.getLogger(
                "edu.harvard.iq.dataverse.upload.client.DatasetServiceBean." + "GlobusUpload" + logTimestamp);
        String logFileName = "../logs" + File.separator + "globusUpload_id_" + dataset.getId() + "_" + logTimestamp
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

        globusLogger.info("Starting an globusUpload ");

        String datasetIdentifier = dataset.getStorageIdentifier();

        // ToDo - use DataAccess methods?
        String storageType = datasetIdentifier.substring(0, datasetIdentifier.indexOf("://") + 3);
        datasetIdentifier = datasetIdentifier.substring(datasetIdentifier.indexOf("://") + 3);

        Thread.sleep(5000);

        JsonObject jsonObject = null;
        try (StringReader rdr = new StringReader(jsonData)) {
            jsonObject = Json.createReader(rdr).readObject();
        } catch (Exception jpe) {
            jpe.printStackTrace();
            logger.log(Level.SEVERE, "Error parsing dataset json. Json: {0}");
        }
        logger.info("json: " + JsonUtil.prettyPrint(jsonObject));

        String taskIdentifier = jsonObject.getString("taskIdentifier");

        String ruleId = "";
        try {
            ruleId = jsonObject.getString("ruleId");
        } catch (NullPointerException npe) {
            logger.warning("NPE for jsonData object");
        }

        // globus task status check
        GlobusTask task = globusStatusCheck(taskIdentifier, globusLogger);
        String taskStatus = getTaskStatus(task);

        if (ruleId.length() > 0) {
            deletePermision(ruleId, globusLogger);
        }

        // If success, switch to an EditInProgress lock - do this before removing the
        // GlobusUpload lock
        // Keeping a lock through the add datafiles API call avoids a conflicting edit
        // and keeps any open dataset page refreshing until the datafile appears
        if (!(taskStatus.startsWith("FAILED") || taskStatus.startsWith("INACTIVE"))) {
            datasetSvc.addDatasetLock(dataset,
                    new DatasetLock(DatasetLock.Reason.EditInProgress, authUser, "Completing Globus Upload"));
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
             */
            datasetSvc.removeDatasetLocks(dataset, DatasetLock.Reason.GlobusUpload);
        }

        if (taskStatus.startsWith("FAILED") || taskStatus.startsWith("INACTIVE")) {
            String comment = "Reason : " + taskStatus.split("#")[1] + "<br> Short Description : "
                    + taskStatus.split("#")[2];
            userNotificationService.sendNotification((AuthenticatedUser) authUser, new Timestamp(new Date().getTime()),
                    UserNotification.Type.GLOBUSUPLOADCOMPLETEDWITHERRORS, dataset.getId(), comment, true);
            globusLogger.info("Globus task failed ");

        } else {
            try {
                //

                List<String> inputList = new ArrayList<String>();
                JsonArray filesJsonArray = jsonObject.getJsonArray("files");

                if (filesJsonArray != null) {

                    for (JsonObject fileJsonObject : filesJsonArray.getValuesAs(JsonObject.class)) {

                        // storageIdentifier s3://gcs5-bucket1:1781cfeb8a7-748c270a227c from
                        // externalTool
                        String storageIdentifier = fileJsonObject.getString("storageIdentifier");
                        String[] bits = storageIdentifier.split(":");
                        String bucketName = bits[1].replace("/", "");
                        String fileId = bits[bits.length - 1];

                        // fullpath s3://gcs5-bucket1/10.5072/FK2/3S6G2E/1781cfeb8a7-4ad9418a5873
                        String fullPath = storageType + bucketName + "/" + datasetIdentifier + "/" + fileId;
                        String fileName = fileJsonObject.getString("fileName");

                        inputList.add(fileId + "IDsplit" + fullPath + "IDsplit" + fileName);
                    }

                    // calculateMissingMetadataFields: checksum, mimetype
                    JsonObject newfilesJsonObject = calculateMissingMetadataFields(inputList, globusLogger);
                    JsonArray newfilesJsonArray = newfilesJsonObject.getJsonArray("files");

                    JsonArrayBuilder jsonDataSecondAPI = Json.createArrayBuilder();

                    for (JsonObject fileJsonObject : filesJsonArray.getValuesAs(JsonObject.class)) {

                        countAll++;
                        String storageIdentifier = fileJsonObject.getString("storageIdentifier");
                        String fileName = fileJsonObject.getString("fileName");
                        String directoryLabel = fileJsonObject.getString("directoryLabel");
                        String[] bits = storageIdentifier.split(":");
                        String fileId = bits[bits.length - 1];

                        List<JsonObject> newfileJsonObject = IntStream.range(0, newfilesJsonArray.size())
                                .mapToObj(index -> ((JsonObject) newfilesJsonArray.get(index)).getJsonObject(fileId))
                                .filter(Objects::nonNull).collect(Collectors.toList());

                        if (newfileJsonObject != null) {
                            if (!newfileJsonObject.get(0).getString("hash").equalsIgnoreCase("null")) {
                                JsonPatch path = Json.createPatchBuilder()
                                        .add("/md5Hash", newfileJsonObject.get(0).getString("hash")).build();
                                fileJsonObject = path.apply(fileJsonObject);
                                path = Json.createPatchBuilder()
                                        .add("/mimeType", newfileJsonObject.get(0).getString("mime")).build();
                                fileJsonObject = path.apply(fileJsonObject);
                                jsonDataSecondAPI.add(fileJsonObject);
                                countSuccess++;
                            } else {
                                globusLogger.info(fileName
                                        + " will be skipped from adding to dataset by second API due to missing values ");
                                countError++;
                            }
                        } else {
                            globusLogger.info(fileName
                                    + " will be skipped from adding to dataset by second API due to missing values ");
                            countError++;
                        }
                    }

                    String newjsonData = jsonDataSecondAPI.build().toString();

                    globusLogger.info("Successfully generated new JsonData for Second API call");

                    String command = "curl -H \"X-Dataverse-key:" + token.getTokenString() + "\" -X POST "
                            + httpRequestUrl + "/api/datasets/:persistentId/addFiles?persistentId=doi:"
                            + datasetIdentifier + " -F jsonData='" + newjsonData + "'";
                    System.out.println("*******====command ==== " + command);

                    String output = addFilesAsync(command, globusLogger);
                    if (output.equalsIgnoreCase("ok")) {
                        // if(!taskSkippedFiles)
                        if (countError == 0) {
                            userNotificationService.sendNotification((AuthenticatedUser) authUser,
                                    new Timestamp(new Date().getTime()), UserNotification.Type.GLOBUSUPLOADCOMPLETED,
                                    dataset.getId(), countSuccess + " files added out of " + countAll, true);
                        } else {
                            userNotificationService.sendNotification((AuthenticatedUser) authUser,
                                    new Timestamp(new Date().getTime()),
                                    UserNotification.Type.GLOBUSUPLOADCOMPLETEDWITHERRORS, dataset.getId(),
                                    countSuccess + " files added out of " + countAll, true);
                        }
                        globusLogger.info("Successfully completed api/datasets/:persistentId/addFiles call ");
                    } else {
                        globusLogger.log(Level.SEVERE,
                                "******* Error while executing api/datasets/:persistentId/add call ", command);
                    }

                }

                globusLogger.info("Files processed: " + countAll.toString());
                globusLogger.info("Files added successfully: " + countSuccess.toString());
                globusLogger.info("Files failures: " + countError.toString());
                globusLogger.info("Finished upload via Globus job.");

                if (fileHandlerSuceeded) {
                    fileHandler.close();
                }

            } catch (Exception e) {
                logger.info("Exception from globusUpload call ");
                e.printStackTrace();
                globusLogger.info("Exception from globusUpload call " + e.getMessage());
                datasetSvc.removeDatasetLocks(dataset, DatasetLock.Reason.EditInProgress);
            }
        }
    }

    public String addFilesAsync(String curlCommand, Logger globusLogger)
            throws ExecutionException, InterruptedException {
        CompletableFuture<String> addFilesFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return (addFiles(curlCommand, globusLogger));
        }, executor).exceptionally(ex -> {
            globusLogger.fine("Something went wrong : " + ex.getLocalizedMessage());
            ex.printStackTrace();
            return null;
        });

        String result = addFilesFuture.get();

        return result;
    }

    private String addFiles(String curlCommand, Logger globusLogger) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        Process process = null;
        String line;
        String status = "";

        try {
            globusLogger.info("Call to :  " + curlCommand);
            processBuilder.command("bash", "-c", curlCommand);
            process = processBuilder.start();
            process.waitFor();

            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));

            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null)
                sb.append(line);
            globusLogger.info(" API Output :  " + sb.toString());
            JsonObject jsonObject = null;
            try (StringReader rdr = new StringReader(sb.toString())) {
                jsonObject = Json.createReader(rdr).readObject();
            } catch (Exception jpe) {
                jpe.printStackTrace();
                globusLogger.log(Level.SEVERE, "Error parsing dataset json.");
            }

            status = jsonObject.getString("status");
        } catch (Exception ex) {
            globusLogger.log(Level.SEVERE,
                    "******* Unexpected Exception while executing api/datasets/:persistentId/add call ", ex);
        }

        return status;
    }

    @Asynchronous
    public void globusDownload(String jsonData, Dataset dataset, User authUser) throws MalformedURLException {

        String logTimestamp = logFormatter.format(new Date());
        Logger globusLogger = Logger.getLogger(
                "edu.harvard.iq.dataverse.upload.client.DatasetServiceBean." + "GlobusDownload" + logTimestamp);

        String logFileName = "../logs" + File.separator + "globusDownload_id_" + dataset.getId() + "_" + logTimestamp
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

        globusLogger.info("Starting an globusDownload ");

        JsonObject jsonObject = null;
        try (StringReader rdr = new StringReader(jsonData)) {
            jsonObject = Json.createReader(rdr).readObject();
        } catch (Exception jpe) {
            jpe.printStackTrace();
            globusLogger.log(Level.SEVERE, "Error parsing dataset json. Json: {0}");
        }

        String taskIdentifier = jsonObject.getString("taskIdentifier");
        String ruleId = "";

        try {
            jsonObject.getString("ruleId");
        } catch (NullPointerException npe) {

        }

        // globus task status check
        GlobusTask task = globusStatusCheck(taskIdentifier, globusLogger);
        String taskStatus = getTaskStatus(task);

        if (ruleId.length() > 0) {
            deletePermision(ruleId, globusLogger);
        }

        if (taskStatus.startsWith("FAILED") || taskStatus.startsWith("INACTIVE")) {
            String comment = "Reason : " + taskStatus.split("#")[1] + "<br> Short Description : "
                    + taskStatus.split("#")[2];
            userNotificationService.sendNotification((AuthenticatedUser) authUser, new Timestamp(new Date().getTime()),
                    UserNotification.Type.GLOBUSDOWNLOADCOMPLETEDWITHERRORS, dataset.getId(), comment, true);
            globusLogger.info("Globus task failed during download process");
        } else {
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

    private GlobusTask globusStatusCheck(String taskId, Logger globusLogger) throws MalformedURLException {
        boolean taskCompletion = false;
        String status = "";
        GlobusTask task = null;
        int pollingInterval = SystemConfig.getIntLimitFromStringOrDefault(settingsSvc.getValueForKey(SettingsServiceBean.Key.GlobusPollingInterval), 50);
        do {
            try {
                globusLogger.info("checking globus transfer task   " + taskId);
                Thread.sleep(pollingInterval * 1000);
                AccessToken clientTokenUser = getClientToken();
                // success = globusServiceBean.getSuccessfulTransfers(clientTokenUser, taskId);
                task = getTask(clientTokenUser, taskId, globusLogger);
                if (task != null) {
                    status = task.getStatus();
                    if (status != null) {
                        // The task is in progress.
                        if (status.equalsIgnoreCase("ACTIVE")) {
                            if (task.getNice_status().equalsIgnoreCase("ok")
                                    || task.getNice_status().equalsIgnoreCase("queued")) {
                                taskCompletion = false;
                            } else {
                                taskCompletion = true;
                                // status = "FAILED" + "#" + task.getNice_status() + "#" +
                                // task.getNice_status_short_description();
                            }
                        } else {
                            // The task is either succeeded, failed or inactive.
                            taskCompletion = true;
                            // status = status + "#" + task.getNice_status() + "#" +
                            // task.getNice_status_short_description();
                        }
                    } else {
                        // status = "FAILED";
                        taskCompletion = true;
                    }
                } else {
                    // status = "FAILED";
                    taskCompletion = true;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        } while (!taskCompletion);

        globusLogger.info("globus transfer task completed successfully");
        return task;
    }

    private String getTaskStatus(GlobusTask task) {
        String status = null;
        if (task != null) {
            status = task.getStatus();
            if (status != null) {
                // The task is in progress.
                if (status.equalsIgnoreCase("ACTIVE")) {
                    status = "FAILED" + "#" + task.getNice_status() + "#" + task.getNice_status_short_description();
                } else {
                    // The task is either succeeded, failed or inactive.
                    status = status + "#" + task.getNice_status() + "#" + task.getNice_status_short_description();
                }
            } else {
                status = "FAILED";
            }
        } else {
            status = "FAILED";
        }
        return status;
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

        CompletableFuture completableFuture = allCompletableFuture.thenApply(files -> {
            return files.stream().map(d -> json(d)).collect(toJsonArray());
        });

        JsonArrayBuilder filesObject = (JsonArrayBuilder) completableFuture.get();

        JsonObject output = Json.createObjectBuilder().add("files", filesObject).build();

        return output;

    }

    private CompletableFuture<FileDetailsHolder> calculateDetailsAsync(String id, Logger globusLogger) {
        // logger.info(" calcualte additional details for these globus id ==== " + id);

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

        // ToDo: what if the file doesnot exists in s3
        // ToDo: what if checksum calculation failed

        do {
            try {
                StorageIO<DvObject> dataFileStorageIO = DataAccess.getDirectStorageIO(fullPath);
                in = dataFileStorageIO.getInputStream();
                checksumVal = FileUtil.calculateChecksum(in, DataFile.ChecksumType.MD5);
                count = 3;
            } catch (IOException ioex) {
                count = 3;
                logger.info(ioex.getMessage());
                globusLogger.info("S3AccessIO: DataFile (fullPAth " + fullPath
                        + ") does not appear to be an S3 object associated with driver: ");
            } catch (Exception ex) {
                count = count + 1;
                ex.printStackTrace();
                logger.info(ex.getMessage());
                Thread.sleep(5000);
            }

        } while (count < 3);

        if (checksumVal.length() == 0) {
            checksumVal = "NULL";
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

        if (type!=null && !type.isBlank()) {
            if (FileUtil.useRecognizedType(finalType, type)) {
                finalType = type;
            }
        }

        return finalType;
    }
    /*
     * public boolean globusFinishTransfer(Dataset dataset, AuthenticatedUser user)
     * throws MalformedURLException {
     * 
     * logger.info("=====Tasklist == dataset id :" + dataset.getId()); String
     * directory = null;
     * 
     * try {
     * 
     * List<FileMetadata> fileMetadatas = new ArrayList<>();
     * 
     * StorageIO<Dataset> datasetSIO = DataAccess.getStorageIO(dataset);
     * 
     * 
     * 
     * DatasetVersion workingVersion = dataset.getEditVersion();
     * 
     * if (workingVersion.getCreateTime() != null) {
     * workingVersion.setCreateTime(new Timestamp(new Date().getTime())); }
     * 
     * directory = dataset.getAuthorityForFileStorage() + "/" +
     * dataset.getIdentifierForFileStorage();
     * 
     * System.out.println("======= directory ==== " + directory +
     * " ====  datasetId :" + dataset.getId()); Map<String, Integer> checksumMapOld
     * = new HashMap<>();
     * 
     * Iterator<FileMetadata> fmIt = workingVersion.getFileMetadatas().iterator();
     * 
     * while (fmIt.hasNext()) { FileMetadata fm = fmIt.next(); if (fm.getDataFile()
     * != null && fm.getDataFile().getId() != null) { String chksum =
     * fm.getDataFile().getChecksumValue(); if (chksum != null) {
     * checksumMapOld.put(chksum, 1); } } }
     * 
     * List<DataFile> dFileList = new ArrayList<>(); boolean update = false; for
     * (S3ObjectSummary s3ObjectSummary : datasetSIO.listAuxObjects("")) {
     * 
     * String s3ObjectKey = s3ObjectSummary.getKey();
     * 
     * 
     * String t = s3ObjectKey.replace(directory, "");
     * 
     * if (t.indexOf(".") > 0) { long totalSize = s3ObjectSummary.getSize(); String
     * filePath = s3ObjectKey; String fileName =
     * filePath.split("/")[filePath.split("/").length - 1]; String fullPath =
     * datasetSIO.getStorageLocation() + "/" + fileName;
     * 
     * logger.info("Full path " + fullPath); StorageIO<DvObject> dataFileStorageIO =
     * DataAccess.getDirectStorageIO(fullPath); InputStream in =
     * dataFileStorageIO.getInputStream();
     * 
     * String checksumVal = FileUtil.calculateChecksum(in,
     * DataFile.ChecksumType.MD5); //String checksumVal = s3ObjectSummary.getETag();
     * logger.info("The checksum is " + checksumVal); if
     * ((checksumMapOld.get(checksumVal) != null)) { logger.info("datasetId :" +
     * dataset.getId() + "======= filename ==== " + filePath +
     * " == file already exists "); } else if (filePath.contains("cached") ||
     * filePath.contains(".thumb")) { logger.info(filePath + " is ignored"); } else
     * { update = true; logger.info("datasetId :" + dataset.getId() +
     * "======= filename ==== " + filePath + " == new file   "); try {
     * 
     * DataFile datafile = new DataFile(DataFileServiceBean.MIME_TYPE_GLOBUS_FILE);
     * //MIME_TYPE_GLOBUS datafile.setModificationTime(new Timestamp(new
     * Date().getTime())); datafile.setCreateDate(new Timestamp(new
     * Date().getTime())); datafile.setPermissionModificationTime(new Timestamp(new
     * Date().getTime()));
     * 
     * FileMetadata fmd = new FileMetadata();
     * 
     * 
     * fmd.setLabel(fileName); fmd.setDirectoryLabel(filePath.replace(directory,
     * "").replace(File.separator + fileName, ""));
     * 
     * fmd.setDataFile(datafile);
     * 
     * datafile.getFileMetadatas().add(fmd);
     * 
     * FileUtil.generateS3PackageStorageIdentifierForGlobus(datafile);
     * logger.info("====  datasetId :" + dataset.getId() + "======= filename ==== "
     * + filePath + " == added to datafile, filemetadata   ");
     * 
     * try { // We persist "SHA1" rather than "SHA-1".
     * //datafile.setChecksumType(DataFile.ChecksumType.SHA1);
     * datafile.setChecksumType(DataFile.ChecksumType.MD5);
     * datafile.setChecksumValue(checksumVal); } catch (Exception cksumEx) {
     * logger.info("====  datasetId :" + dataset.getId() +
     * "======Could not calculate  checksumType signature for the new file "); }
     * 
     * datafile.setFilesize(totalSize);
     * 
     * dFileList.add(datafile);
     * 
     * } catch (Exception ioex) { logger.info("datasetId :" + dataset.getId() +
     * "======Failed to process and/or save the file " + ioex.getMessage()); return
     * false;
     * 
     * } } } } if (update) {
     * 
     * List<DataFile> filesAdded = new ArrayList<>();
     * 
     * if (dFileList != null && dFileList.size() > 0) {
     * 
     * // Dataset dataset = version.getDataset();
     * 
     * for (DataFile dataFile : dFileList) {
     * 
     * if (dataFile.getOwner() == null) { dataFile.setOwner(dataset);
     * 
     * workingVersion.getFileMetadatas().add(dataFile.getFileMetadata());
     * dataFile.getFileMetadata().setDatasetVersion(workingVersion);
     * dataset.getFiles().add(dataFile);
     * 
     * }
     * 
     * filesAdded.add(dataFile);
     * 
     * }
     * 
     * logger.info("====  datasetId :" + dataset.getId() +
     * " ===== Done! Finished saving new files to the dataset."); }
     * 
     * fileMetadatas.clear(); for (DataFile addedFile : filesAdded) {
     * fileMetadatas.add(addedFile.getFileMetadata()); } filesAdded = null;
     * 
     * if (workingVersion.isDraft()) {
     * 
     * logger.info("Async: ====  datasetId :" + dataset.getId() +
     * " ==== inside draft version ");
     * 
     * Timestamp updateTime = new Timestamp(new Date().getTime());
     * 
     * workingVersion.setLastUpdateTime(updateTime);
     * dataset.setModificationTime(updateTime);
     * 
     * 
     * for (FileMetadata fileMetadata : fileMetadatas) {
     * 
     * if (fileMetadata.getDataFile().getCreateDate() == null) {
     * fileMetadata.getDataFile().setCreateDate(updateTime);
     * fileMetadata.getDataFile().setCreator((AuthenticatedUser) user); }
     * fileMetadata.getDataFile().setModificationTime(updateTime); }
     * 
     * 
     * } else { logger.info("datasetId :" + dataset.getId() +
     * " ==== inside released version ");
     * 
     * for (int i = 0; i < workingVersion.getFileMetadatas().size(); i++) { for
     * (FileMetadata fileMetadata : fileMetadatas) { if
     * (fileMetadata.getDataFile().getStorageIdentifier() != null) {
     * 
     * if (fileMetadata.getDataFile().getStorageIdentifier().equals(workingVersion.
     * getFileMetadatas().get(i).getDataFile().getStorageIdentifier())) {
     * workingVersion.getFileMetadatas().set(i, fileMetadata); } } } }
     * 
     * 
     * }
     * 
     * 
     * try { Command<Dataset> cmd; logger.info("Async: ====  datasetId :" +
     * dataset.getId() +
     * " ======= UpdateDatasetVersionCommand START in globus function "); cmd = new
     * UpdateDatasetVersionCommand(dataset, new DataverseRequest(user,
     * (HttpServletRequest) null)); ((UpdateDatasetVersionCommand)
     * cmd).setValidateLenient(true); //new DataverseRequest(authenticatedUser,
     * (HttpServletRequest) null) //dvRequestService.getDataverseRequest()
     * commandEngine.submit(cmd); } catch (CommandException ex) {
     * logger.log(Level.WARNING, "====  datasetId :" + dataset.getId() +
     * "======CommandException updating DatasetVersion from batch job: " +
     * ex.getMessage()); return false; }
     * 
     * logger.info("====  datasetId :" + dataset.getId() +
     * " ======= GLOBUS  CALL COMPLETED SUCCESSFULLY ");
     * 
     * //return true; }
     * 
     * } catch (Exception e) { String message = e.getMessage();
     * 
     * logger.info("====  datasetId :" + dataset.getId() +
     * " ======= GLOBUS  CALL Exception ============== " + message);
     * e.printStackTrace(); return false; //return
     * error(Response.Status.INTERNAL_SERVER_ERROR,
     * "Uploaded files have passed checksum validation but something went wrong while attempting to move the files into Dataverse. Message was '"
     * + message + "'."); }
     * 
     * String globusBasicToken =
     * settingsSvc.getValueForKey(SettingsServiceBean.Key.GlobusBasicToken, "");
     * AccessToken clientTokenUser = getClientToken(globusBasicToken);
     * updatePermision(clientTokenUser, directory, "identity", "r"); return true; }
     * 
     */
}
