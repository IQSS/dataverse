package edu.harvard.iq.dataverse.externaltools;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObjectBuilder;

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
        jab.add(ExternalToolHandler.DISPLAY_NAME, externalTool.getDisplayName());
        jab.add(ExternalToolHandler.DESCRIPTION, externalTool.getDescription());
        jab.add(ExternalToolHandler.TOOL_URL, ExternalToolUtil.getToolUrlWithQueryParams(this, externalTool));
        jab.add(ExternalToolHandler.TOOL_PARAMETERS, externalTool.getToolParameters());
        return jab;
    }

}
