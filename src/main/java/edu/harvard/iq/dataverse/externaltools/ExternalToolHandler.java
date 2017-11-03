package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.StringReader;
import javax.json.Json;
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

}
