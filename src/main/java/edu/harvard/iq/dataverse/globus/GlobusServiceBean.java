package edu.harvard.iq.dataverse.globus;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import edu.harvard.iq.dataverse.*;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.gson.Gson;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.primefaces.PrimeFaces;

import static edu.harvard.iq.dataverse.util.JsfHelper.JH;


@Stateless
@Named("GlobusServiceBean")
public class GlobusServiceBean implements java.io.Serializable{

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

    private static final Logger logger = Logger.getLogger(FeaturedDataverseServiceBean.class.getCanonicalName());

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

    public void onLoad() {
        logger.info("Start Globus " + code);
        logger.info("State " + state);

        String globusEndpoint = settingsSvc.getValueForKey(SettingsServiceBean.Key.GlobusEndpoint, "");
        String basicGlobusToken = settingsSvc.getValueForKey(SettingsServiceBean.Key.BasicGlobusToken, "");
        if (globusEndpoint.equals("") || basicGlobusToken.equals("")) {
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.message.GlobusError"));
            return;
        }
        String datasetId = state;
        logger.info("DatasetId = " + datasetId);

        String directory = getDirectory(datasetId);
        if (directory == null) {
            logger.severe("Cannot find directory");
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.message.GlobusError"));
            return;
        }
        HttpServletRequest origRequest = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();

        logger.info(origRequest.getScheme());
        logger.info(origRequest.getServerName());

        if (code != null ) {

            try {
                AccessToken accessTokenUser = getAccessToken(origRequest, basicGlobusToken);
                if (accessTokenUser == null) {
                    logger.severe("Cannot get access user token for code " + code);
                    JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.message.GlobusError"));
                    return;
                } else {
                    setUserTransferToken(accessTokenUser.getOtherTokens().get(0).getAccessToken());
                }

                UserInfo usr = getUserInfo(accessTokenUser);
                if (usr == null) {
                    logger.severe("Cannot get user info for " + accessTokenUser.getAccessToken());
                    JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.message.GlobusError"));
                    return;
                }
                logger.info(accessTokenUser.getAccessToken());
                logger.info(usr.getEmail());
                //AccessToken clientTokenUser = getClientToken(basicGlobusToken);
                AccessToken clientTokenUser = getClientToken();
                if (clientTokenUser == null) {
                    logger.severe("Cannot get client token ");
                    JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.message.GlobusError"));
                    return;
                }
                logger.info(clientTokenUser.getAccessToken());

                int status = createDirectory(clientTokenUser, directory, globusEndpoint);
                if (status == 202) {
                    int perStatus = givePermission("identity", usr.getSub(), "rw", clientTokenUser, directory, globusEndpoint);
                    if (perStatus != 201 && perStatus != 200) {
                        logger.severe("Cannot get permissions ");
                        JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.message.GlobusError"));
                        return;
                    }
                } else if (status == 502) { //directory already exists
                    int perStatus = givePermission("identity", usr.getSub(), "rw", clientTokenUser, directory, globusEndpoint);
                    if (perStatus == 409) {
                        logger.info("permissions already exist");
                    } else if (perStatus != 201 && perStatus != 200) {
                        logger.severe("Cannot get permissions ");
                        JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.message.GlobusError"));
                        return;
                    }
                } else {
                    logger.severe("Cannot create directory, status code " + status);
                    JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.message.GlobusError"));
                    return;
                }
                // ProcessBuilder processBuilder = new ProcessBuilder();
                // AuthenticatedUser user = (AuthenticatedUser) session.getUser();
                // ApiToken token = authSvc.findApiTokenByUser(user);
                // String command = "curl -H \"X-Dataverse-key:" + token.getTokenString() + "\" -X POST https://" + origRequest.getServerName() + "/api/globus/" + datasetId;
                // logger.info("====command ==== " + command);
                // processBuilder.command("bash", "-c", command);
                // logger.info("=== Start process");
                // Process process = processBuilder.start();
                // logger.info("=== Going globus");
                goGlobusUpload(directory, globusEndpoint);
                logger.info("=== Finished globus");


            } catch (MalformedURLException ex) {
                logger.severe(ex.getMessage());
                logger.severe(ex.getCause().toString());
                JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.message.GlobusError"));
            } catch (UnsupportedEncodingException ex) {
                logger.severe(ex.getMessage());
                logger.severe(ex.getCause().toString());
                JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.message.GlobusError"));
            } catch (IOException ex) {
                logger.severe(ex.getMessage());
                logger.severe(ex.getCause().toString());
                JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.message.GlobusError"));
            }

        }

    }

    private void goGlobusUpload(String directory, String globusEndpoint ) {

        String httpString = "window.location.replace('" + "https://app.globus.org/file-manager?destination_id=" + globusEndpoint + "&destination_path=" + directory + "'" +")";
        PrimeFaces.current().executeScript(httpString);
    }

    public void goGlobusDownload(String datasetId) {

        String directory = getDirectory(datasetId);
        String globusEndpoint = settingsSvc.getValueForKey(SettingsServiceBean.Key.GlobusEndpoint, "");
        String httpString = "window.location.replace('" + "https://app.globus.org/file-manager?origin_id=" + globusEndpoint + "&origin_path=" + directory + "'" +")";
        PrimeFaces.current().executeScript(httpString);
    }
/*
    public void removeGlobusPermission() throws MalformedURLException {
        //taskId and ruleId
        String basicGlobusToken = settingsSvc.getValueForKey(SettingsServiceBean.Key.BasicGlobusToken, "");
        AccessToken clientTokenUser =  getClientToken(basicGlobusToken);
        String directory =  getDirectory( dataset.getId()+"" );
        updatePermision(clientTokenUser, directory, "identity", "r");
    }

 */

    ArrayList<String>  checkPermisions( AccessToken clientTokenUser, String directory, String globusEndpoint, String principalType, String principal) throws MalformedURLException {
        URL url = new URL("https://transfer.api.globusonline.org/v0.10/endpoint/" + globusEndpoint + "/access_list");
        MakeRequestResponse result = makeRequest(url, "Bearer",
                clientTokenUser.getOtherTokens().get(0).getAccessToken(),"GET",  null);
        ArrayList<String> ids = new ArrayList<String>();
        if (result.status == 200) {
            AccessList al = parseJson(result.jsonResponse, AccessList.class, false);

            for (int i = 0; i< al.getDATA().size(); i++) {
                Permissions pr = al.getDATA().get(i);
                if ((pr.getPath().equals(directory + "/") || pr.getPath().equals(directory )) && pr.getPrincipalType().equals(principalType) &&
                        ((principal == null) || (principal != null && pr.getPrincipal().equals(principal))) ) {
                    ids.add(pr.getId());
                } else {
                    logger.info(pr.getPath() + " === " + directory + " == " + pr.getPrincipalType());
                    continue;
                }
            }
        }

        return ids;
    }

    public void updatePermision(AccessToken clientTokenUser, String directory, String principalType, String perm) throws MalformedURLException {
        if (directory != null && !directory.equals("")) {
            directory =  directory + "/";
        }
        logger.info("Start updating permissions." + " Directory is " + directory);
        String globusEndpoint = settingsSvc.getValueForKey(SettingsServiceBean.Key.GlobusEndpoint, "");
        ArrayList<String> rules = checkPermisions( clientTokenUser, directory, globusEndpoint, principalType, null);
        logger.info("Size of rules " + rules.size());
        int count = 0;
        while (count < rules.size()) {
            logger.info("Start removing rules " + rules.get(count) );
            Permissions permissions = new Permissions();
            permissions.setDATA_TYPE("access");
            permissions.setPermissions(perm);
            permissions.setPath(directory);

            Gson gson = new GsonBuilder().create();
            URL url = new URL("https://transfer.api.globusonline.org/v0.10/endpoint/" + globusEndpoint + "/access/" + rules.get(count));
            logger.info("https://transfer.api.globusonline.org/v0.10/endpoint/" + globusEndpoint + "/access/" + rules.get(count));
            MakeRequestResponse result = makeRequest(url, "Bearer",
                    clientTokenUser.getOtherTokens().get(0).getAccessToken(),"PUT",  gson.toJson(permissions));
            if (result.status != 200) {
                logger.warning("Cannot update access rule " + rules.get(count));
            } else {
                logger.info("Access rule " + rules.get(count) + " was updated");
            }
            count++;
        }
    }

    public void deletePermision(String ruleId) throws MalformedURLException {

        AccessToken clientTokenUser = getClientToken();
        logger.info("Start updating permissions."  );
        String globusEndpoint = settingsSvc.getValueForKey(SettingsServiceBean.Key.GlobusEndpoint, "");

        URL url = new URL("https://transfer.api.globusonline.org/v0.10/endpoint/" + globusEndpoint + "/access/" + ruleId);
        logger.info("https://transfer.api.globusonline.org/v0.10/endpoint/" + globusEndpoint + "/access/" + ruleId);
        MakeRequestResponse result = makeRequest(url, "Bearer",
                clientTokenUser.getOtherTokens().get(0).getAccessToken(),"DELETE",  null);
        if (result.status != 200) {
            logger.warning("Cannot update access rule " + ruleId);
        } else {
            logger.info("Access rule " + ruleId + " was updated");
        }

    }

    public int givePermission(String principalType, String principal, String perm, AccessToken clientTokenUser, String directory, String globusEndpoint) throws MalformedURLException {

        ArrayList rules = checkPermisions( clientTokenUser, directory, globusEndpoint, principalType, principal);



        Permissions permissions = new Permissions();
        permissions.setDATA_TYPE("access");
        permissions.setPrincipalType(principalType);
        permissions.setPrincipal(principal);
        permissions.setPath(directory + "/" );
        permissions.setPermissions(perm);

        Gson gson = new GsonBuilder().create();
        MakeRequestResponse result = null;
        if (rules.size() == 0) {
            logger.info("Start creating the rule");
            URL url = new URL("https://transfer.api.globusonline.org/v0.10/endpoint/"+ globusEndpoint + "/access");
            result = makeRequest(url, "Bearer",
                    clientTokenUser.getOtherTokens().get(0).getAccessToken(), "POST", gson.toJson(permissions));

            if (result.status == 400) {
                logger.severe("Path " + permissions.getPath() + " is not valid");
            } else if (result.status == 409) {
                logger.warning("ACL already exists or Endpoint ACL already has the maximum number of access rules");
            }

            return result.status;
        } else {
            logger.info("Start Updating the rule");
            URL url = new URL("https://transfer.api.globusonline.org/v0.10/endpoint/"+ globusEndpoint + "/access/" + rules.get(0));
            result = makeRequest(url, "Bearer",
                    clientTokenUser.getOtherTokens().get(0).getAccessToken(), "PUT", gson.toJson(permissions));

            if (result.status == 400) {
                logger.severe("Path " + permissions.getPath() + " is not valid");
            } else if (result.status == 409) {
                logger.warning("ACL already exists or Endpoint ACL already has the maximum number of access rules");
            }
            logger.info("Result status " + result.status);
        }

        return result.status;
    }

    private int createDirectory(AccessToken clientTokenUser, String directory, String globusEndpoint) throws MalformedURLException {
        URL url = new URL("https://transfer.api.globusonline.org/v0.10/operation/endpoint/" + globusEndpoint + "/mkdir");

        MkDir mkDir = new MkDir();
        mkDir.setDataType("mkdir");
        mkDir.setPath(directory);
        Gson gson = new GsonBuilder().create();

        MakeRequestResponse result = makeRequest(url, "Bearer",
                clientTokenUser.getOtherTokens().get(0).getAccessToken(),"POST",  gson.toJson(mkDir));
        logger.info(result.toString());

        if (result.status == 502) {
            logger.warning("Cannot create directory " + mkDir.getPath() + ", it already exists");
        } else if (result.status == 403) {
            logger.severe("Cannot create directory " + mkDir.getPath() + ", permission denied");
        } else if  (result.status == 202) {
            logger.info("Directory created " + mkDir.getPath());
        }

        return result.status;

    }

    public String getTaskList(String basicGlobusToken, String identifierForFileStorage, String timeWhenAsyncStarted) throws MalformedURLException  {
        try
        {
            logger.info("1.getTaskList ====== timeWhenAsyncStarted = " + timeWhenAsyncStarted + "    ====== identifierForFileStorage ====== " + identifierForFileStorage);

            String globusEndpoint = settingsSvc.getValueForKey(SettingsServiceBean.Key.GlobusEndpoint, "");
            //AccessToken clientTokenUser = getClientToken(basicGlobusToken);
            AccessToken clientTokenUser = getClientToken( );

            URL url = new URL("https://transfer.api.globusonline.org/v0.10/endpoint_manager/task_list?filter_endpoint="+globusEndpoint+"&filter_status=SUCCEEDED&filter_completion_time="+timeWhenAsyncStarted);

            //AccessToken accessTokenUser
            //accessTokenUser.getOtherTokens().get(0).getAccessToken()
            MakeRequestResponse result = makeRequest(url, "Bearer", clientTokenUser.getOtherTokens().get(0).getAccessToken(),"GET",  null);
            //logger.info("==TEST ==" + result.toString());



            //2019-12-01 18:34:37+00:00
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //SimpleDateFormat task_sdf = new SimpleDateFormat("yyyy-MM-ddTHH:mm:ss");

            Calendar cal1 = Calendar.getInstance();
            cal1.setTime(sdf.parse(timeWhenAsyncStarted));

            Calendar cal2 = Calendar.getInstance();

            Tasklist tasklist = null;
            //2019-12-01 18:34:37+00:00

            if (result.status == 200) {
                tasklist = parseJson(result.jsonResponse, Tasklist.class, false);
                for (int i = 0; i< tasklist.getDATA().size(); i++) {
                    Task task = tasklist.getDATA().get(i);
                    Date tastTime = sdf.parse(task.getRequest_time().replace("T" , " "));
                    cal2.setTime(tastTime);


                    if ( cal1.before(cal2))  {

                        // get /task/<task_id>/successful_transfers
                        // verify datasetid in "destination_path": "/~/test_godata_copy/file1.txt",
                        // go to aws and get files and write to database tables

                        logger.info("====== timeWhenAsyncStarted = " + timeWhenAsyncStarted + "    ====== task.getRequest_time().toString() ====== " + task.getRequest_time());

                        boolean success = getSuccessfulTransfers(clientTokenUser, task.getTask_id() , identifierForFileStorage) ;

                        if(success)
                        {
                            logger.info("SUCCESS ====== " + timeWhenAsyncStarted + " timeWhenAsyncStarted is before tastTime  =  TASK time =  " + task.getTask_id());
                            return task.getTask_id();
                        }
                    }
                    else
                    {
                        //logger.info("====== " + timeWhenAsyncStarted + " timeWhenAsyncStarted is after tastTime =  TASK time = " + task.getTask_id());
                        //return task.getTask_id();
                    }
                }
            }
        } catch (MalformedURLException ex) {
            logger.severe(ex.getMessage());
            logger.severe(ex.getCause().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean getSuccessfulTransfers(AccessToken clientTokenUser, String taskId, String identifierForFileStorage) throws MalformedURLException {

        URL url = new URL("https://transfer.api.globusonline.org/v0.10/endpoint_manager/task/"+taskId+"/successful_transfers");

        MakeRequestResponse result = makeRequest(url, "Bearer",clientTokenUser.getOtherTokens().get(0).getAccessToken(),
                "GET",  null);

        Transferlist transferlist = null;

        if (result.status == 200) {
            transferlist = parseJson(result.jsonResponse, Transferlist.class, false);
            for (int i = 0; i < transferlist.getDATA().size(); i++) {
                SuccessfulTransfer successfulTransfer = transferlist.getDATA().get(i);
                String pathToVerify = successfulTransfer.getDestination_path();
                logger.info("getSuccessfulTransfers : ======pathToVerify ===  " + pathToVerify + " ====identifierForFileStorage ===  " + identifierForFileStorage);
                if(pathToVerify.contains(identifierForFileStorage))
                {
                    logger.info(" SUCCESS ====== " + pathToVerify + " ====  " + identifierForFileStorage);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean getSuccessfulTransfers(AccessToken clientTokenUser, String taskId ) throws MalformedURLException {

        URL url = new URL("https://transfer.api.globusonline.org/v0.10/endpoint_manager/task/"+taskId+"/successful_transfers");

        MakeRequestResponse result = makeRequest(url, "Bearer",clientTokenUser.getOtherTokens().get(0).getAccessToken(),
                "GET",  null);

        Transferlist transferlist = null;

        if (result.status == 200) {
            logger.info(" SUCCESS ====== " );
            return true;
        }
        return false;
    }



    public AccessToken getClientToken() throws MalformedURLException {
        String basicGlobusToken = settingsSvc.getValueForKey(SettingsServiceBean.Key.BasicGlobusToken, "");
        URL url = new URL("https://auth.globus.org/v2/oauth2/token?scope=openid+email+profile+urn:globus:auth:scope:transfer.api.globus.org:all&grant_type=client_credentials");

        MakeRequestResponse result = makeRequest(url, "Basic",
                basicGlobusToken,"POST",   null);
        AccessToken clientTokenUser = null;
        if (result.status == 200) {
            clientTokenUser = parseJson(result.jsonResponse, AccessToken.class, true);
        }
        return clientTokenUser;
    }

    public AccessToken getAccessToken(HttpServletRequest origRequest, String basicGlobusToken ) throws UnsupportedEncodingException, MalformedURLException {
        String serverName = origRequest.getServerName();
        if (serverName.equals("localhost")) {
            serverName = "utl-192-123.library.utoronto.ca";
        }

        String redirectURL = "https://" + serverName + "/globus.xhtml";

        redirectURL = URLEncoder.encode(redirectURL, "UTF-8");

        URL url = new URL("https://auth.globus.org/v2/oauth2/token?code=" + code + "&redirect_uri=" + redirectURL
                + "&grant_type=authorization_code");
        logger.info(url.toString());

        MakeRequestResponse result = makeRequest(url, "Basic", basicGlobusToken,"POST",   null);
        AccessToken accessTokenUser = null;

        if (result.status == 200) {
            logger.info("Access Token: \n" + result.toString());
            accessTokenUser = parseJson(result.jsonResponse, AccessToken.class, true);
            logger.info(accessTokenUser.getAccessToken());
        }

        return accessTokenUser;

    }

    public UserInfo getUserInfo(AccessToken accessTokenUser) throws MalformedURLException {

        URL url = new URL("https://auth.globus.org/v2/oauth2/userinfo");
        MakeRequestResponse result = makeRequest(url, "Bearer" , accessTokenUser.getAccessToken() , "GET",  null);
        UserInfo usr = null;
        if (result.status == 200) {
            usr = parseJson(result.jsonResponse, UserInfo.class, true);
        }

        return usr;
    }

    public MakeRequestResponse  makeRequest(URL url, String authType, String authCode, String method, String jsonString) {
        String str = null;
        HttpURLConnection connection = null;
        int status = 0;
        try {
            connection = (HttpURLConnection) url.openConnection();
            //Basic NThjMGYxNDQtN2QzMy00ZTYzLTk3MmUtMjljNjY5YzJjNGJiOktzSUVDMDZtTUxlRHNKTDBsTmRibXBIbjZvaWpQNGkwWVVuRmQyVDZRSnc9
            logger.info(authType + " " + authCode);
            connection.setRequestProperty("Authorization", authType + " " + authCode);
            //connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
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

    private MakeRequestResponse findDirectory(String directory, AccessToken clientTokenUser, String globusEndpoint) throws MalformedURLException {
        URL url = new URL(" https://transfer.api.globusonline.org/v0.10/endpoint/" + globusEndpoint +"/ls?path=" + directory + "/");

        MakeRequestResponse result = makeRequest(url, "Bearer",
                clientTokenUser.getOtherTokens().get(0).getAccessToken(),"GET", null);
        logger.info("find directory status:" + result.status);

        return result;
    }

    public boolean giveGlobusPublicPermissions(String datasetId) throws UnsupportedEncodingException, MalformedURLException {

        String globusEndpoint = settingsSvc.getValueForKey(SettingsServiceBean.Key.GlobusEndpoint, "");
        String basicGlobusToken = settingsSvc.getValueForKey(SettingsServiceBean.Key.BasicGlobusToken, "");
        if (globusEndpoint.equals("") || basicGlobusToken.equals("")) {
            return false;
        }
        //AccessToken clientTokenUser = getClientToken(basicGlobusToken);
        AccessToken clientTokenUser = getClientToken( );
        if (clientTokenUser == null) {
            logger.severe("Cannot get client token ");
            return false;
        }

        String directory = getDirectory(datasetId);
        logger.info(directory);

        MakeRequestResponse status = findDirectory(directory, clientTokenUser, globusEndpoint);

        if (status.status == 200) {

           /* FilesList fl = parseJson(status.jsonResponse, FilesList.class, false);
            ArrayList<FileG> files = fl.getDATA();
            if (files != null) {
                for (FileG file: files) {
                    if (!file.getName().contains("cached") && !file.getName().contains(".thumb")) {
                        int perStatus = givePermission("all_authenticated_users", "", "r", clientTokenUser,
                                directory + "/" + file.getName(), globusEndpoint);
                        logger.info("givePermission status " + perStatus + " for " + file.getName());
                        if (perStatus == 409) {
                            logger.info("Permissions already exist or limit was reached for " + file.getName());
                        } else if (perStatus == 400) {
                            logger.info("No file in Globus " + file.getName());
                        } else if (perStatus != 201) {
                            logger.info("Cannot get permission for " + file.getName());
                        }
                    }
                }
            }*/

            int perStatus = givePermission("all_authenticated_users", "", "r", clientTokenUser, directory, globusEndpoint);
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
        }else {
            logger.severe("Cannot find directory in globus, status " + status );
            return false;
        }

        return true;
    }
/*
    public boolean globusFinishTransfer(Dataset dataset,  AuthenticatedUser user) throws MalformedURLException {

        logger.info("=====Tasklist == dataset id :" + dataset.getId());
        String directory = null;

        try {

            List<FileMetadata> fileMetadatas = new ArrayList<>();

            StorageIO<Dataset> datasetSIO = DataAccess.getStorageIO(dataset);



            DatasetVersion workingVersion = dataset.getEditVersion();

            if (workingVersion.getCreateTime() != null) {
                workingVersion.setCreateTime(new Timestamp(new Date().getTime()));
            }

            directory = dataset.getAuthorityForFileStorage() + "/" + dataset.getIdentifierForFileStorage();

            System.out.println("======= directory ==== " + directory + " ====  datasetId :" + dataset.getId());
            Map<String, Integer> checksumMapOld = new HashMap<>();

            Iterator<FileMetadata> fmIt = workingVersion.getFileMetadatas().iterator();

            while (fmIt.hasNext()) {
                FileMetadata fm = fmIt.next();
                if (fm.getDataFile() != null && fm.getDataFile().getId() != null) {
                    String chksum = fm.getDataFile().getChecksumValue();
                    if (chksum != null) {
                        checksumMapOld.put(chksum, 1);
                    }
                }
            }

            List<DataFile> dFileList = new ArrayList<>();
            boolean update = false;
            for (S3ObjectSummary s3ObjectSummary : datasetSIO.listAuxObjects("")) {

                String s3ObjectKey = s3ObjectSummary.getKey();


                String t = s3ObjectKey.replace(directory, "");

                if (t.indexOf(".") > 0) {
                    long totalSize = s3ObjectSummary.getSize();
                    String filePath = s3ObjectKey;
                    String fileName = filePath.split("/")[filePath.split("/").length - 1];
                    String fullPath = datasetSIO.getStorageLocation() + "/" + fileName;

                    logger.info("Full path " + fullPath);
                    StorageIO<DvObject> dataFileStorageIO = DataAccess.getDirectStorageIO(fullPath);
                    InputStream in = dataFileStorageIO.getInputStream();

                    String checksumVal = FileUtil.calculateChecksum(in, DataFile.ChecksumType.MD5);
                    //String checksumVal = s3ObjectSummary.getETag();
                    logger.info("The checksum is " + checksumVal);
                    if ((checksumMapOld.get(checksumVal) != null)) {
                        logger.info("datasetId :" + dataset.getId() + "======= filename ==== " + filePath + " == file already exists ");
                    } else if (filePath.contains("cached") || filePath.contains(".thumb")) {
                        logger.info(filePath + " is ignored");
                    } else {
                        update = true;
                        logger.info("datasetId :" + dataset.getId() + "======= filename ==== " + filePath + " == new file   ");
                        try {

                            DataFile datafile = new DataFile(DataFileServiceBean.MIME_TYPE_GLOBUS_FILE);  //MIME_TYPE_GLOBUS
                            datafile.setModificationTime(new Timestamp(new Date().getTime()));
                            datafile.setCreateDate(new Timestamp(new Date().getTime()));
                            datafile.setPermissionModificationTime(new Timestamp(new Date().getTime()));

                            FileMetadata fmd = new FileMetadata();


                            fmd.setLabel(fileName);
                            fmd.setDirectoryLabel(filePath.replace(directory, "").replace(File.separator + fileName, ""));

                            fmd.setDataFile(datafile);

                            datafile.getFileMetadatas().add(fmd);

                            FileUtil.generateS3PackageStorageIdentifierForGlobus(datafile);
                            logger.info("====  datasetId :" + dataset.getId() + "======= filename ==== " + filePath + " == added to datafile, filemetadata   ");

                            try {
                                // We persist "SHA1" rather than "SHA-1".
                                //datafile.setChecksumType(DataFile.ChecksumType.SHA1);
                                datafile.setChecksumType(DataFile.ChecksumType.MD5);
                                datafile.setChecksumValue(checksumVal);
                            } catch (Exception cksumEx) {
                                logger.info("====  datasetId :" + dataset.getId() + "======Could not calculate  checksumType signature for the new file ");
                            }

                            datafile.setFilesize(totalSize);

                            dFileList.add(datafile);

                        } catch (Exception ioex) {
                            logger.info("datasetId :" + dataset.getId() + "======Failed to process and/or save the file " + ioex.getMessage());
                            return false;

                        }
                    }
                }
            }
            if (update) {

                List<DataFile> filesAdded = new ArrayList<>();

                if (dFileList != null && dFileList.size() > 0) {

                    // Dataset dataset = version.getDataset();

                    for (DataFile dataFile : dFileList) {

                        if (dataFile.getOwner() == null) {
                            dataFile.setOwner(dataset);

                            workingVersion.getFileMetadatas().add(dataFile.getFileMetadata());
                            dataFile.getFileMetadata().setDatasetVersion(workingVersion);
                            dataset.getFiles().add(dataFile);

                        }

                        filesAdded.add(dataFile);

                    }

                    logger.info("====  datasetId :" + dataset.getId() + " ===== Done! Finished saving new files to the dataset.");
                }

                fileMetadatas.clear();
                for (DataFile addedFile : filesAdded) {
                    fileMetadatas.add(addedFile.getFileMetadata());
                }
                filesAdded = null;

                if (workingVersion.isDraft()) {

                    logger.info("Async: ====  datasetId :" + dataset.getId() + " ==== inside draft version ");

                    Timestamp updateTime = new Timestamp(new Date().getTime());

                    workingVersion.setLastUpdateTime(updateTime);
                    dataset.setModificationTime(updateTime);


                    for (FileMetadata fileMetadata : fileMetadatas) {

                        if (fileMetadata.getDataFile().getCreateDate() == null) {
                            fileMetadata.getDataFile().setCreateDate(updateTime);
                            fileMetadata.getDataFile().setCreator((AuthenticatedUser) user);
                        }
                        fileMetadata.getDataFile().setModificationTime(updateTime);
                    }


                } else {
                    logger.info("datasetId :" + dataset.getId() + " ==== inside released version ");

                    for (int i = 0; i < workingVersion.getFileMetadatas().size(); i++) {
                        for (FileMetadata fileMetadata : fileMetadatas) {
                            if (fileMetadata.getDataFile().getStorageIdentifier() != null) {

                                if (fileMetadata.getDataFile().getStorageIdentifier().equals(workingVersion.getFileMetadatas().get(i).getDataFile().getStorageIdentifier())) {
                                    workingVersion.getFileMetadatas().set(i, fileMetadata);
                                }
                            }
                        }
                    }


                }


                try {
                    Command<Dataset> cmd;
                    logger.info("Async: ====  datasetId :" + dataset.getId() + " ======= UpdateDatasetVersionCommand START in globus function ");
                    cmd = new UpdateDatasetVersionCommand(dataset, new DataverseRequest(user, (HttpServletRequest) null));
                    ((UpdateDatasetVersionCommand) cmd).setValidateLenient(true);
                    //new DataverseRequest(authenticatedUser, (HttpServletRequest) null)
                    //dvRequestService.getDataverseRequest()
                    commandEngine.submit(cmd);
                } catch (CommandException ex) {
                    logger.log(Level.WARNING, "====  datasetId :" + dataset.getId() + "======CommandException updating DatasetVersion from batch job: " + ex.getMessage());
                    return false;
                }

                logger.info("====  datasetId :" + dataset.getId() + " ======= GLOBUS  CALL COMPLETED SUCCESSFULLY ");

                //return true;
            }

        } catch (Exception e) {
            String message = e.getMessage();

            logger.info("====  datasetId :" + dataset.getId() + " ======= GLOBUS  CALL Exception ============== " + message);
            e.printStackTrace();
            return false;
            //return error(Response.Status.INTERNAL_SERVER_ERROR, "Uploaded files have passed checksum validation but something went wrong while attempting to move the files into Dataverse. Message was '" + message + "'.");
        }

        String basicGlobusToken = settingsSvc.getValueForKey(SettingsServiceBean.Key.BasicGlobusToken, "");
        AccessToken clientTokenUser = getClientToken(basicGlobusToken);
        updatePermision(clientTokenUser, directory, "identity", "r");
        return true;
    }

*/
}
