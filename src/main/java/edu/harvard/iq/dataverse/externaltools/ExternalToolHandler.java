package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.URLTokenUtil;

import edu.harvard.iq.dataverse.util.UrlSignerUtil;
import edu.harvard.iq.dataverse.util.json.JsonUtil;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.ws.rs.HttpMethod;

import org.apache.commons.codec.binary.StringUtils;

import static edu.harvard.iq.dataverse.api.ApiConstants.DS_VERSION_LATEST;

/**
 * Handles an operation on a specific file. Requires a file id in order to be
 * instantiated. Applies logic based on an {@link ExternalTool} specification,
 * such as constructing a URL to access that file.
 */
public class ExternalToolHandler extends URLTokenUtil {

    public final ExternalTool externalTool;

    private String requestMethod;

    

    /**
     * File level tool
     *
     * @param externalTool The database entity.
     * @param dataFile     Required.
     * @param apiToken     The apiToken can be null because "explore" tools can be
     *                     used anonymously.
     */
    public ExternalToolHandler(ExternalTool externalTool, DataFile dataFile, ApiToken apiToken,
            FileMetadata fileMetadata, String localeCode) {
        super(dataFile, apiToken, fileMetadata, localeCode);
        this.externalTool = externalTool;
    }

    /**
     * Dataset level tool
     *
     * @param externalTool The database entity.
     * @param dataset      Required.
     * @param apiToken     The apiToken can be null because "explore" tools can be
     *                     used anonymously.
     */
    public ExternalToolHandler(ExternalTool externalTool, Dataset dataset, ApiToken apiToken, String localeCode) {
        super(dataset, apiToken, localeCode);
        this.externalTool = externalTool;
    }

    public String handleRequest() {
        return handleRequest(false);
    }

    public String handleRequest(boolean preview) {
        JsonObject toolParameters = JsonUtil.getJsonObject(externalTool.getToolParameters());
        JsonString method = toolParameters.getJsonString(HTTP_METHOD);
        requestMethod = method != null ? method.getString() : HttpMethod.GET;
        JsonObject params = getParams(toolParameters);
        logger.fine("Found params: " + JsonUtil.prettyPrint(params));
        if (requestMethod.equals(HttpMethod.GET)) {
            String paramsString = "";
            if (externalTool.getAllowedApiCalls() == null) {
                // Legacy, using apiKey
                logger.fine("Legacy Case");

                for (Entry<String, JsonValue> entry : params.entrySet()) {
                    paramsString = paramsString + (paramsString.isEmpty() ? "?" : "&") + entry.getKey() + "=";
                    JsonValue val = entry.getValue();
                    if (val.getValueType().equals(JsonValue.ValueType.NUMBER)) {
                        paramsString += ((JsonNumber) val).intValue();
                    } else {
                        paramsString += ((JsonString) val).getString();
                    }
                }
            } else {
                //Send a signed callback to get params and signedURLs 
                String callback = null;
                switch (externalTool.getScope()) {
                case DATASET:
                    callback=SystemConfig.getDataverseSiteUrlStatic() + "/api/v1/datasets/"
                            + dataset.getId() + "/versions/" + DS_VERSION_LATEST + "/toolparams/" + externalTool.getId();
                    break;
                case FILE:
                    callback= SystemConfig.getDataverseSiteUrlStatic() + "/api/v1/files/"
                            + dataFile.getId() + "/metadata/" + fileMetadata.getId() + "/toolparams/"
                            + externalTool.getId();
                }
                if (apiToken != null) {
                    callback = UrlSignerUtil.signUrl(callback, 5, apiToken.getAuthenticatedUser().getUserIdentifier(), HttpMethod.GET,
                        JvmSettings.API_SIGNING_SECRET.lookupOptional().orElse("") + apiToken.getTokenString());
                }
                paramsString= "?callback=" + Base64.getEncoder().encodeToString(StringUtils.getBytesUtf8(callback));
                if (getLocaleCode() != null) {
                    paramsString += "&locale=" + getLocaleCode();
                }
            }
            if (preview) {
                paramsString += "&preview=true";
            }
            logger.fine("GET return is: " + paramsString);
            return paramsString;

        } else {
            // ToDo - if the allowedApiCalls() are defined, could/should we send them to
            // tools using POST as well?

            if (requestMethod.equals(HttpMethod.POST)) {
                String body = JsonUtil.prettyPrint(createPostBody(params, null).build());
                try {
                    logger.fine("POST Body: " + body);
                    return postFormData(body);
                } catch (IOException | InterruptedException ex) {
                    Logger.getLogger(ExternalToolHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return null;
    }

    private String postFormData(String allowedApis) throws IOException, InterruptedException {
        String url = null;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(allowedApis))
                .uri(URI.create(externalTool.getToolUrl())).header("Content-Type", "application/json").build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        boolean redirect = false;
        int status = response.statusCode();
        if (status != HttpURLConnection.HTTP_OK) {
            if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER) {
                redirect = true;
            }
        }
        if (redirect == true) {
            String newUrl = response.headers().firstValue("location").get();
//            toolContext = "http://" + response.uri().getAuthority();

            url = newUrl;
        }
        return url;
    }

    public String getToolUrlWithQueryParams() {
        String params = ExternalToolHandler.this.handleRequest();
        return externalTool.getToolUrl() + params;
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

    /**
     * @return Returns Javascript that opens the explore tool in a new browser tab
     *         if the browser allows it.If not, it shows an alert that popups must
     *         be enabled in the browser.
     */
    public String getExploreScript() {
        String toolUrl = this.getToolUrlWithQueryParams();
        logger.fine("Exploring with " + toolUrl);
        return getScriptForUrl(toolUrl);
    }

    // TODO: Consider merging with getExploreScript
    public String getConfigureScript() {
        String toolUrl = this.getToolUrlWithQueryParams();
        logger.fine("Configuring with " + toolUrl);
        return getScriptForUrl(toolUrl);
    }
}
