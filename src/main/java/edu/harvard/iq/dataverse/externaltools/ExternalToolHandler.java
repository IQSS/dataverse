package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.URLTokenUtil;

import edu.harvard.iq.dataverse.util.UrlSignerUtil;
import edu.harvard.iq.dataverse.util.json.JsonUtil;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonWriter;
import javax.ws.rs.HttpMethod;

/**
 * Handles an operation on a specific file. Requires a file id in order to be
 * instantiated. Applies logic based on an {@link ExternalTool} specification,
 * such as constructing a URL to access that file.
 */
public class ExternalToolHandler extends URLTokenUtil {

    /**
     * @param user the user to set
     */
    public void setUser(String user) {
        this.user = user;
    }

    private final ExternalTool externalTool;

    private String requestMethod;
    private String toolContext;
    private String user;

    
    /**
     * File level tool
     *
     * @param externalTool The database entity.
     * @param dataFile Required.
     * @param apiToken The apiToken can be null because "explore" tools can be
     * used anonymously.
     */
    public ExternalToolHandler(ExternalTool externalTool, DataFile dataFile, ApiToken apiToken, FileMetadata fileMetadata, String localeCode) {
        super(dataFile, apiToken, fileMetadata, localeCode);
        this.externalTool = externalTool;
        toolContext = externalTool.getToolUrl();
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
        super(dataset, apiToken, localeCode);
        this.externalTool = externalTool;
    }

    // TODO: rename to handleRequest() to someday handle sending headers as well as query parameters.
    public String handleRequest() {
        return handleRequest(false);
    }
    
    // TODO: rename to handleRequest() to someday handle sending headers as well as query parameters.
    public String handleRequest(boolean preview) {
        JsonObject obj = JsonUtil.getJsonObject(externalTool.getToolParameters());
        JsonString method = obj.getJsonString("httpMethod");
        requestMethod = method!=null?method.getString():HttpMethod.GET;
        JsonArray queryParams = obj.getJsonArray("queryParameters");
        List<String> params = new ArrayList<>();
        if (requestMethod.equals(HttpMethod.GET)) {
            if (queryParams == null || queryParams.isEmpty()) {
                return "";
            }
            queryParams.getValuesAs(JsonObject.class).forEach((queryParam) -> {
                queryParam.keySet().forEach((key) -> {
                    String value = queryParam.getString(key);
                    String param = getQueryParam(key, value);
                    if (param != null && !param.isEmpty()) {
                        params.add(param);
                    }
                });
            });
        }

        //ToDo - if the allowedApiCalls() are defined, could/should we send them to tools using GET as well?
        
        if (requestMethod.equals(HttpMethod.POST)) {
            try {
                JsonObjectBuilder bodyBuilder =  Json.createObjectBuilder();
                queryParams.getValuesAs(JsonObject.class).forEach((queryParam) -> {
                    queryParam.keySet().forEach((key) -> {
                        String value = queryParam.getString(key);
                        String param = getPostBodyParam(key, value);
                        if (param != null && !param.isEmpty()) {
                            params.add(param);
                        }
                    });
                });
                String addVal = String.join(",", params);
                bodyBuilder.add("queryParameters", addVal);
                String allowedApis;
                JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
                JsonObject objApis = JsonUtil.getJsonObject(externalTool.getAllowedApiCalls());
                JsonArray apis = objApis.getJsonArray("apis");
                apis.getValuesAs(JsonObject.class).forEach(((apiObj) -> {
                    String name = apiObj.getJsonString("name").getString();
                    String httpmethod = apiObj.getJsonString("method").getString();
                    int timeout = apiObj.getInt("timeOut");
                    String urlTemplate = apiObj.getJsonString("urlTemplate").getString();
                    logger.fine("URL Template: " + urlTemplate);
                    String apiPath = replaceTokensWithValues(urlTemplate);
                    logger.fine("URL WithTokens: " + apiPath);
                    String url = UrlSignerUtil.signUrl(apiPath, timeout, user, httpmethod, System.getProperty(SystemConfig.API_SIGNING_SECRET, "") + getApiToken().getTokenString());
                    logger.fine("Signed URL: " + url);
                    jsonArrayBuilder.add(Json.createObjectBuilder().add("name", name).add("httpMethod", httpmethod)
                            .add("signedUrl", url).add("timeOut", timeout));
                }));
                JsonArray allowedApiCallsArray = jsonArrayBuilder.build();
                bodyBuilder.add("signedUrls", allowedApiCallsArray);
                JsonObject body = bodyBuilder.build();
                logger.info(body.toString());
                return postFormData(body.toString());
            } catch (IOException ex) {
                Logger.getLogger(ExternalToolHandler.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(ExternalToolHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (!preview) {
            return "?" + String.join("&", params);
        } else {
            return "?" + String.join("&", params) + "&preview=true";
        }
    }

    
    private String postFormData(String allowedApis ) throws IOException, InterruptedException{
        String url = null;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(allowedApis)).uri(URI.create(externalTool.getToolUrl()))
                .header("Content-Type", "application/json")
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
        if (redirect==true){
            String newUrl = response.headers().firstValue("location").get();
//            toolContext = "http://" + response.uri().getAuthority();
            
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
