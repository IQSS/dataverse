package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.externaltools.ExternalToolServiceBean.ReservedWord;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

public class ExternalToolHandler {

    private static final Logger logger = Logger.getLogger(ExternalToolHandler.class.getCanonicalName());

    public static final String DISPLAY_NAME = "displayName";
    public static final String DESCRIPTION = "description";
    public static final String TOOL_URL = "toolUrl";
    public static final String TOOL_PARAMETERS = "toolParameters";

    private final ExternalTool externalTool;
    private final DataFile dataFile;

    private final ApiToken apiToken;

    /**
     * @param externalTool The database entity.
     * @param dataFile Required.
     * @param apiToken The apiToken can be null because in the future, "explore"
     * tools can be used anonymously.
     */
    public ExternalToolHandler(ExternalTool externalTool, DataFile dataFile, ApiToken apiToken) {
        this.externalTool = externalTool;
        if (dataFile == null) {
            String error = "A DataFile is required.";
            logger.warning("Error in ExternalToolHandler constructor: " + error);
            throw new IllegalArgumentException(error);
        }
        this.dataFile = dataFile;
        this.apiToken = apiToken;
    }

    public DataFile getDataFile() {
        return dataFile;
    }

    public ApiToken getApiToken() {
        return apiToken;
    }

    public JsonObjectBuilder toJson() {
        JsonObjectBuilder jab = Json.createObjectBuilder();
        jab.add("id", externalTool.getId());
        jab.add(DISPLAY_NAME, externalTool.getDisplayName());
        jab.add(DESCRIPTION, externalTool.getDescription());
        jab.add(TOOL_URL, getToolUrlWithQueryParams());
        jab.add(TOOL_PARAMETERS, externalTool.getToolParameters());
        return jab;
    }

    // TODO: rename to handleRequest() to someday handle sending headers as well as query parameters.
    public String getQueryParametersForUrl() {
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
                params.add(getQueryParam(key, value));
            });
        });
        return "?" + String.join("&", params);
    }

    // TODO: Make this parser much smarter in the future. Tools like Data Explorer will want
    // to use reserved words within a value like this:
    // "uri": "{siteUrl}/api/access/datafile/{fileId}/metadata/ddi"
    private String getQueryParam(String key, String value) {
        ReservedWord reservedWord = ReservedWord.fromString(value);
        switch (reservedWord) {
            case FILE_ID:
                // getDataFile is never null because of the constructor
                return key + "=" + getDataFile().getId();
            case API_TOKEN:
                String apiTokenString = null;
                ApiToken theApiToken = getApiToken();
                if (theApiToken != null) {
                    apiTokenString = theApiToken.getTokenString();
                }
                return key + "=" + apiTokenString;
            default:
                // We should never reach here.
                return null;
        }
    }

    public String getToolUrlWithQueryParams() {
        return externalTool.getToolUrl() + getQueryParametersForUrl();
    }

    public ExternalTool getExternalTool() {
        return externalTool;
    }

}
