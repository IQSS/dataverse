package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.util.URLTokenUtil;

import edu.harvard.iq.dataverse.util.UrlSignerUtil;
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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
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
     * @return the allowedApiCalls
     */
    public String getAllowedApiCalls() {
        return allowedApiCalls;
    }

    /**
     * @param allowedApiCalls the allowedApiCalls to set
     */
    public void setAllowedApiCalls(String allowedApiCalls) {
        this.allowedApiCalls = allowedApiCalls;
    }

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
    private String siteUrl;
    private String allowedApiCalls;
    
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

        StringWriter allowedApiCallsStringWriter = new StringWriter();
        String allowedApis;
        try (JsonWriter jsonWriter = Json.createWriter(allowedApiCallsStringWriter)) {
            JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
            allowedApiCalls = externalTool.getAllowedApiCalls();
            JsonReader jsonReaderApis = Json.createReader(new StringReader(allowedApiCalls));
            JsonObject objApis = jsonReaderApis.readObject();
            JsonArray apis = objApis.getJsonArray("apis");
            apis.getValuesAs(JsonObject.class).forEach(((apiObj) -> {
                String name = apiObj.getJsonString("name").toString();
                String httpmethod = apiObj.getJsonString("method").toString();
                int timeout = apiObj.getInt("timeOut");
                String apiPath = replaceTokensWithValues(apiObj.getJsonString("urlTemplate").toString());
                String url = UrlSignerUtil.signUrl(apiPath, timeout, user,httpmethod, getApiToken().getTokenString());
                jsonArrayBuilder.add(
                        Json.createObjectBuilder().add("name", name)
                                .add("httpMethod", httpmethod)
                                .add("signedUrl", url)
                                .add("timeOut", timeout));
            }));
            JsonArray allowedApiCallsArray = jsonArrayBuilder.build();
            jsonWriter.writeArray(allowedApiCallsArray);
            allowedApis = allowedApiCallsStringWriter.toString();
            try {
                allowedApiCallsStringWriter.close();
            } catch (IOException ex) {
                Logger.getLogger(ExternalToolHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (requestMethod.equals(HttpMethod.POST)){
            try {
                return postFormData(allowedApis);
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
