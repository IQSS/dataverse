package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import static edu.harvard.iq.dataverse.externaltools.ExternalToolHandler.DESCRIPTION;
import static edu.harvard.iq.dataverse.externaltools.ExternalToolHandler.DISPLAY_NAME;
import static edu.harvard.iq.dataverse.externaltools.ExternalToolHandler.TOOL_PARAMETERS;
import static edu.harvard.iq.dataverse.externaltools.ExternalToolHandler.TOOL_URL;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class ExternalToolUtil {

    private static final Logger logger = Logger.getLogger(ExternalToolUtil.class.getCanonicalName());

    // Perhaps this could use a better name than "findAll". This method takes a list of tools from the database and
    // returns "handlers" that have inserted parameters in the right places.
    public static List<ExternalToolHandler> findAll(List<ExternalTool> externalTools, DataFile file, ApiToken apiToken) {
        List<ExternalToolHandler> externalToolHandlers = new ArrayList<>();
        externalTools.forEach((externalTool) -> {
            ExternalToolHandler externalToolHandler = new ExternalToolHandler(externalTool, file, apiToken);
            externalToolHandlers.add(externalToolHandler);
        });
        return externalToolHandlers;

    }

    public static ExternalTool parseAddExternalToolInput(String userInput) {
        try {
            JsonReader jsonReader = Json.createReader(new StringReader(userInput));
            JsonObject jsonObject = jsonReader.readObject();
            String displayName = jsonObject.getString(DISPLAY_NAME);
            String description = jsonObject.getString(DESCRIPTION);
            String toolUrl = jsonObject.getString(TOOL_URL);
            JsonObject toolParametersObj = jsonObject.getJsonObject(TOOL_PARAMETERS);
            String toolParameters = toolParametersObj.toString();
            return new ExternalTool(displayName, description, toolUrl, toolParameters);
        } catch (Exception ex) {
            System.out.println("ex: " + ex);
            return null;
        }
    }

    // FIXME: rename to handleRequest() to someday handle sending headers as well as query parameters.
    public static String getQueryParametersForUrl(ExternalToolHandler externalToolHandler, ExternalTool externalTool) {
        DataFile dataFile = externalToolHandler.getDataFile();
        ApiToken apiToken = externalToolHandler.getApiToken();
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
            return "?" + getQueryParam(key, value, dataFile, apiToken);
        } else {
            List<String> params = new ArrayList<>();
            queryParams.getValuesAs(JsonObject.class).forEach((queryParam) -> {
                queryParam.keySet().forEach((key) -> {
                    String value = queryParam.getString(key);
                    params.add(getQueryParam(key, value, dataFile, apiToken));
                });
            });
            return "?" + String.join("&", params);

        }
    }

    private static String getQueryParam(String key, String value, DataFile dataFile, ApiToken apiToken) {
        if (dataFile == null) {
            logger.info("DataFile was null!");
            return key + "=" + value;
        }
        String apiTokenString = null;
        if (apiToken != null) {
            apiTokenString = apiToken.getTokenString();
        }
        // TODO: Put reserved words like "{fileId}" and "{apiToken}" into an enum.
        switch (value) {
            case "{fileId}":
                return key + "=" + dataFile.getId();
            case "{apiToken}":
                return key + "=" + apiTokenString;
            default:
                return key + "=" + value;
        }
    }

    public static String getToolUrlWithQueryParams(ExternalToolHandler externalToolHandler, ExternalTool externalTool) {
        return externalTool.getToolUrl() + getQueryParametersForUrl(externalToolHandler, externalTool);
    }

}
