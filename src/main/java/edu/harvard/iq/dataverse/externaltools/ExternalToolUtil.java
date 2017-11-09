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
import java.util.logging.Logger;
import javax.json.Json;
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


}
