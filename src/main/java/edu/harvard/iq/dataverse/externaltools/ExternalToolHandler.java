package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
        int numQueryParam = queryParams.size();
        if (numQueryParam == 1) {
            JsonObject jsonObject = queryParams.getJsonObject(0);
            Set<String> firstPair = jsonObject.keySet();
            String key = firstPair.iterator().next();
            String value = jsonObject.getString(key);
            return "?" + getQueryParam(key, value);
        } else {
            List<String> params = new ArrayList<>();
            queryParams.getValuesAs(JsonObject.class).forEach((queryParam) -> {
                queryParam.keySet().forEach((key) -> {
                    String value = queryParam.getString(key);
                    params.add(getQueryParam(key, value));
                });
            });
            return "?" + String.join("&", params);

        }
    }

    private String getQueryParam(String key, String value) {
        DataFile df = getDataFile();
        if (df == null) {
            logger.fine("DataFile was null!");
            // FIXME: Rather than returning the key/value here as-is, enforce the "reserved word required rule" below.
            return key + "=" + value;
        }
        String apiTokenString = null;
        ApiToken theApiToken = getApiToken();
        if (theApiToken != null) {
            apiTokenString = theApiToken.getTokenString();
        }
        // TODO: Put reserved words like "{fileId}" and "{apiToken}" into an enum.
        // TODO: Research if a format like {reservedWord} is easily parse-able or if another format would be better.
        switch (value) {
            case "{fileId}":
                return key + "=" + df.getId();
            case "{apiToken}":
                return key + "=" + apiTokenString;
            default:
                throw new RuntimeException("Only {fileId} and {apiToken} are allowed as reserved words.");
        }
    }

    public String getToolUrlWithQueryParams() {
        return externalTool.getToolUrl() + getQueryParametersForUrl();
    }

}
