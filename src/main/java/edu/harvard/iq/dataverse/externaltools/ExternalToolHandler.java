package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class ExternalToolHandler {

    public static final String toolUrlString = "toolUrl";
    public static final String displayNameBundleKey = "displayNameBundleKey";
    public static final String descriptionNameBundleKey = "descriptionBundleKey";
    public static final String toolParametersString = "toolParameters";

    public static ExternalTool parseAddExternalToolInput(String userInput) {
        try {

            ExternalTool externalTool = new ExternalTool();
            JsonReader jsonReader = Json.createReader(new StringReader(userInput));
            JsonObject jsonObject = jsonReader.readObject();
            // Get display name.
            String displayNameBundleKey = jsonObject.getString(ExternalToolHandler.displayNameBundleKey);
            System.out.println("displayNameBundleKey: " + displayNameBundleKey);
            externalTool.setDisplayNameBundleKey(displayNameBundleKey);
            String displayName = BundleUtil.getStringFromBundle(externalTool.getDisplayNameBundleKey());
            System.out.println("displayName: " + displayName);
            // Get description.
            String descriptionBundleKey = jsonObject.getString(ExternalToolHandler.descriptionNameBundleKey);
            externalTool.setDescriptionBundleKey(descriptionBundleKey);
            String description = BundleUtil.getStringFromBundle(externalTool.getDescriptionBundleKey());
            System.out.println("description: " + description);
            // Get URL
            String toolUrl = jsonObject.getString(ExternalToolHandler.toolUrlString);
            externalTool.setToolUrl(toolUrl);
            // Get parameters
            JsonObject toolParameters = jsonObject.getJsonObject(ExternalToolHandler.toolParametersString);
            String toolParametersAsString = toolParameters.toString();
            System.out.println("toolParametersAsString: " + toolParametersAsString);
            externalTool.setToolParameters(toolParametersAsString);
            return externalTool;
        } catch (Exception ex) {
            System.out.println("ex: " + ex);
            return null;
        }
    }

    public static String getQueryParametersForUrl(ExternalTool externalTool) {
        DataFile nullDataFile = null;
        ApiToken nullApiToken = null;
        return getQueryParametersForUrl(externalTool, nullDataFile, nullApiToken);
    }

    // FIXME: Do we really need two methods?
    public static String getQueryParametersForUrl(ExternalTool externalTool, DataFile dataFile, ApiToken apiToken) {
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

}
