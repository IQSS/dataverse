package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.externaltools.ExternalTool.ReservedWord;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.UrlSignerUtil;
import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.ws.rs.HttpMethod;

/**
 * Handles an operation on a specific file. Requires a file id in order to be
 * instantiated. Applies logic based on an {@link ExternalTool} specification,
 * such as constructing a URL to access that file.
 */
public class ExternalToolHandler {

    /**
     * @return the allowedUrls
     */
    public String getAllowedUrls() {
        return allowedUrls;
    }

    /**
     * @param allowedUrls the allowedUrls to set
     */
    public void setAllowedUrls(String allowedUrls) {
        this.allowedUrls = allowedUrls;
    }

    /**
     * @param user the user to set
     */
    public void setUser(String user) {
        this.user = user;
    }

    private static final Logger logger = Logger.getLogger(ExternalToolHandler.class.getCanonicalName());

    private final ExternalTool externalTool;
    private final DataFile dataFile;
    private final Dataset dataset;
    private final FileMetadata fileMetadata;

    private ApiToken apiToken;
    private String localeCode;
    private String requestMethod;
    private String toolContext;
    private String user;
    private String siteUrl;
    private String allowedUrls;
    
    /**
     * File level tool
     *
     * @param externalTool The database entity.
     * @param dataFile Required.
     * @param apiToken The apiToken can be null because "explore" tools can be
     * used anonymously.
     */
    public ExternalToolHandler(ExternalTool externalTool, DataFile dataFile, ApiToken apiToken, FileMetadata fileMetadata, String localeCode) {
        this.externalTool = externalTool;
        toolContext = externalTool.getToolUrl();
        if (dataFile == null) {
            String error = "A DataFile is required.";
            logger.warning("Error in ExternalToolHandler constructor: " + error);
            throw new IllegalArgumentException(error);
        }
        if (fileMetadata == null) {
            String error = "A FileMetadata is required.";
            logger.warning("Error in ExternalToolHandler constructor: " + error);
            throw new IllegalArgumentException(error);
        }
        this.dataFile = dataFile;
        this.apiToken = apiToken;
        this.fileMetadata = fileMetadata;
        dataset = fileMetadata.getDatasetVersion().getDataset();
        this.localeCode = localeCode;
    }

    /**
     * Dataset level tool
     *
     * @param externalTool The database entity.
     * @param dataset Required.
     * @param apiToken The apiToken can be null because "explore" tools can be
     * used anonymously.
     */
    public ExternalToolHandler(ExternalTool externalTool, Dataset dataset, ApiToken apiToken, String localeCode) {
        this.externalTool = externalTool;
        if (dataset == null) {
            String error = "A Dataset is required.";
            logger.warning("Error in ExternalToolHandler constructor: " + error);
            throw new IllegalArgumentException(error);
        }
        this.dataset = dataset;
        this.apiToken = apiToken;
        this.dataFile = null;
        this.fileMetadata = null;
        this.localeCode = localeCode;
    }

    public DataFile getDataFile() {
        return dataFile;
    }

    public FileMetadata getFileMetadata() {
        return fileMetadata;
    }

    public ApiToken getApiToken() {
        return apiToken;
    }

    public String getLocaleCode() {
        return localeCode;
    }

    // TODO: rename to handleRequest() to someday handle sending headers as well as query parameters.
    public String handleRequest() {
        return handleRequest(false);
    }
    
    // TODO: rename to handleRequest() to someday handle sending headers as well as query parameters.
    public String handleRequest(boolean preview) {
        String toolParameters = externalTool.getToolParameters();
        JsonReader jsonReader = Json.createReader(new StringReader(toolParameters));
        JsonObject obj = jsonReader.readObject();
        JsonString method = obj.getJsonString("httpMethod");
        requestMethod = method!=null?method.getString():HttpMethod.GET;
        JsonArray queryParams = obj.getJsonArray("queryParameters");
        if (queryParams == null || queryParams.isEmpty()) {
            return "";
        }
        List<String> params = new ArrayList<>();
        queryParams.getValuesAs(JsonObject.class).forEach((queryParam) -> {
            queryParam.keySet().forEach((key) -> {
                String value = queryParam.getString(key);
                String param = getQueryParam(key, value);
                if (param != null && !param.isEmpty()) {
                    params.add(param);
                }
            });
        });        
        if (requestMethod.equals(HttpMethod.POST)){
            try {
                return postFormData(obj.getJsonNumber("timeOut").intValue(), params);
            } catch (IOException | InterruptedException ex) {
                Logger.getLogger(ExternalToolHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (!preview) {
            return "?" + String.join("&", params);
        } else {
            return "?" + String.join("&", params) + "&preview=true";
        }
    }

    private String getQueryParam(String key, String value) {
        ReservedWord reservedWord = ReservedWord.fromString(value);
        switch (reservedWord) {
            case FILE_ID:
                // getDataFile is never null for file tools because of the constructor
                return key + "=" + getDataFile().getId();
            case FILE_PID:
                GlobalId filePid = getDataFile().getGlobalId();
                if (filePid != null) {
                    return key + "=" + getDataFile().getGlobalId();
                }
                break;
            case SITE_URL:
                siteUrl = SystemConfig.getDataverseSiteUrlStatic();
                return key + "=" + siteUrl;
            case API_TOKEN:
                String apiTokenString = null;
                ApiToken theApiToken = getApiToken();
                if (theApiToken != null) {
                    apiTokenString = theApiToken.getTokenString();
                    return key + "=" + apiTokenString;
                }
                break;
            case DATASET_ID:
                return key + "=" + dataset.getId();
            case DATASET_PID:
                return key + "=" + dataset.getGlobalId().asString();
            case DATASET_VERSION:
                String versionString = null;
                if(fileMetadata!=null) { //true for file case
                    versionString = fileMetadata.getDatasetVersion().getFriendlyVersionNumber();
                } else { //Dataset case - return the latest visible version (unless/until the dataset case allows specifying a version)
                    if (getApiToken() != null) {
                        versionString = dataset.getLatestVersion().getFriendlyVersionNumber();
                    } else {
                        versionString = dataset.getLatestVersionForCopy().getFriendlyVersionNumber();
                    }
                }
                if (("DRAFT").equals(versionString)) {
                    versionString = ":draft"; // send the token needed in api calls that can be substituted for a numeric
                                              // version.
                }
                return key + "=" + versionString;
            case FILE_METADATA_ID:
                if(fileMetadata!=null) { //true for file case
                    return key + "=" + fileMetadata.getId();
                }
            case LOCALE_CODE:
                return key + "=" + getLocaleCode();
            case ALLOWED_URLS:
                return key + "=" + getAllowedUrls();                    
            default:
                break;
        }
        return null;
    }
    
    
    private String postFormData(Integer timeout,List<String> params ) throws IOException, InterruptedException{
        String url = "";
//        Integer timeout = obj.getJsonNumber("timeOut").intValue();
        url = UrlSignerUtil.signUrl(siteUrl, timeout, user, HttpMethod.POST, getApiToken().getTokenString());
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(String.join("&", params))).uri(URI.create(externalTool.getToolUrl()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("signedUrl", url)
                .build();        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        boolean redirect=false;
        int status = response.statusCode();
        if (status != HttpURLConnection.HTTP_OK) {
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER) {
                redirect = true;
            }
        }
        if (redirect=true){
            String newUrl = response.headers().firstValue("location").get();
            toolContext = "http://" + response.uri().getAuthority();
            
            url = newUrl;
        }
        return url;
    }
            
    public String getToolUrlWithQueryParams() {
        String params = ExternalToolHandler.this.handleRequest();
        return toolContext + params;
    }
    
    public String getToolUrlForPreviewMode() {
        return externalTool.getToolUrl() + handleRequest(true);
    }

    public ExternalTool getExternalTool() {
        return externalTool;
    }

    public void setApiToken(ApiToken apiToken) {
        this.apiToken = apiToken;
    }

}
