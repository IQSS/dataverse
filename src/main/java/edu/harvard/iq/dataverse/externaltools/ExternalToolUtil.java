package edu.harvard.iq.dataverse.externaltools;

import static edu.harvard.iq.dataverse.externaltools.ExternalToolHandler.DESCRIPTION;
import static edu.harvard.iq.dataverse.externaltools.ExternalToolHandler.DISPLAY_NAME;
import static edu.harvard.iq.dataverse.externaltools.ExternalToolHandler.TOOL_PARAMETERS;
import static edu.harvard.iq.dataverse.externaltools.ExternalToolHandler.TOOL_URL;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class ExternalToolUtil {

    public static ExternalTool parseAddExternalToolInput(String userInput) {
        try {
            ExternalTool externalTool = new ExternalTool();
            JsonReader jsonReader = Json.createReader(new StringReader(userInput));
            JsonObject jsonObject = jsonReader.readObject();
            externalTool.setDisplayName(jsonObject.getString(DISPLAY_NAME));
            externalTool.setDescription(jsonObject.getString(DESCRIPTION));
            externalTool.setToolUrl(jsonObject.getString(TOOL_URL));
            // Get parameters
            JsonObject toolParameters = jsonObject.getJsonObject(TOOL_PARAMETERS);
            String toolParametersAsString = toolParameters.toString();
//            System.out.println("toolParametersAsString: " + toolParametersAsString);
            externalTool.setToolParameters(toolParametersAsString);
            return externalTool;
        } catch (Exception ex) {
//            System.out.println("ex: " + ex);
            return null;
        }
    }

}
