package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import static edu.harvard.iq.dataverse.api.AbstractApiBean.error;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.externaltools.ExternalTool;
import edu.harvard.iq.dataverse.externaltools.ExternalToolHandler;
import edu.harvard.iq.dataverse.externaltools.ExternalToolServiceBean;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

@Path("admin/externalTools")
public class ExternalTools extends AbstractApiBean {

    @GET
    public Response getExternalTools() {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        externalToolService.findAll().forEach((externalTool) -> {
            // FIXME: Show more than the ID in the output.
            jab.add(externalTool.getId());
        });
        return ok(jab);
    }

    @GET
    @Path("file/{id}")
    public Response getExternalToolsByFile(@PathParam("id") Long fileIdFromUser) {
        DataFile dataFile = fileSvc.find(fileIdFromUser);
        if (dataFile == null) {
            return error(BAD_REQUEST, "Could not find datafile with id " + fileIdFromUser);
        }
        JsonArrayBuilder tools = Json.createArrayBuilder();
        ApiToken apiToken = new ApiToken();
        String apiTokenString = getRequestApiKey();
        apiToken.setTokenString(apiTokenString);
        externalToolService.findAll(dataFile, apiToken)
                .forEach((externalToolHandler) -> {
                    tools.add(externalToolHandler.toJson());
                });
        return ok(tools);
    }

    @POST
    public Response addExternalTool(String userInput) {
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolInput(userInput);
            ExternalTool saved = externalToolService.save(externalTool);
            Long toolId = saved.getId();
            actionLogSvc.log(new ActionLogRecord(ActionLogRecord.ActionType.ExternalTool, "addExternalTool").setInfo("External tool added with id " + toolId + "."));
            JsonObjectBuilder tool = Json.createObjectBuilder();
            tool.add("id", toolId);
            tool.add(ExternalToolHandler.DISPLAY_NAME, saved.getDisplayName());
            return ok(tool);
        } catch (Exception ex) {
            return error(BAD_REQUEST, ex.getMessage());
        }

    }

}
