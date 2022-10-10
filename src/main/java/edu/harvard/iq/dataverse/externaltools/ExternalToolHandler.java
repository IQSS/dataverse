package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.util.URLTokenUtil;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

/**
 * Handles an operation on a specific file. Requires a file id in order to be
 * instantiated. Applies logic based on an {@link ExternalTool} specification,
 * such as constructing a URL to access that file.
 */
public class ExternalToolHandler extends URLTokenUtil {

    private final ExternalTool externalTool;
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
    public String getQueryParametersForUrl() {
        return getQueryParametersForUrl(false);
    }
    
    // TODO: rename to handleRequest() to someday handle sending headers as well as query parameters.
    public String getQueryParametersForUrl(boolean preview) {
        String toolParameters = externalTool.getToolParameters();
        JsonReader jsonReader = Json.createReader(new StringReader(toolParameters));
        JsonObject obj = jsonReader.readObject();
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
        if (!preview) {
            return "?" + String.join("&", params);
        } else {
            return "?" + String.join("&", params) + "&preview=true";
        }
    }

    public String getToolUrlWithQueryParams() {
        return externalTool.getToolUrl() + getQueryParametersForUrl();
    }
    
    public String getToolUrlForPreviewMode() {
        return externalTool.getToolUrl() + getQueryParametersForUrl(true);
    }

    public ExternalTool getExternalTool() {
        return externalTool;
    }

    public void setApiToken(ApiToken apiToken) {
        this.apiToken = apiToken;
    }

    /**
     * @return Returns Javascript that opens the explore tool in a new browser
     * tab if the browser allows it.If not, it shows an alert that popups must
     * be enabled in the browser.
     */
    public String getExploreScript() {
        String toolUrl = this.getToolUrlWithQueryParams();
        logger.fine("Exploring with " + toolUrl);
        return getScriptForUrl(toolUrl);
    }
}
