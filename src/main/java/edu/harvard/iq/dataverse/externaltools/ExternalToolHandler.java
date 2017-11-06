package edu.harvard.iq.dataverse.externaltools;

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
            String firstKey = firstPair.iterator().next();
            return "?" + firstKey + "=" + jsonObject.getString(firstKey);
        } else {
            List<String> params = new ArrayList<>();
            queryParams.getValuesAs(JsonObject.class).forEach((queryParam) -> {
                queryParam.keySet().forEach((key) -> {
                    params.add(key + "=" + queryParam.getString(key));
                });
            });
            return "?" + String.join("&", params);

        }
    }

}
