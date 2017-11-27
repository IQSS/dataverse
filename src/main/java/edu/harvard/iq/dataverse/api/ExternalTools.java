package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import static edu.harvard.iq.dataverse.api.AbstractApiBean.error;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.externaltools.ExternalTool;
import edu.harvard.iq.dataverse.externaltools.ExternalToolHandler;
import edu.harvard.iq.dataverse.externaltools.ExternalToolServiceBean;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.DELETE;
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
            jab.add(externalTool.toJson());
        });
        return ok(jab);
    }

    @POST
    public Response addExternalTool(String manifest) {
        try {
            ExternalTool externalTool = ExternalToolServiceBean.parseAddExternalToolManifest(manifest);
            ExternalTool saved = externalToolService.save(externalTool);
            Long toolId = saved.getId();
            actionLogSvc.log(new ActionLogRecord(ActionLogRecord.ActionType.ExternalTool, "addExternalTool").setInfo("External tool added with id " + toolId + "."));
            return ok(saved.toJson());
        } catch (Exception ex) {
            return error(BAD_REQUEST, ex.getMessage());
        }

    }

    @DELETE
    @Path("{id}")
    public Response deleteExternalTool(@PathParam("id") long externalToolIdFromUser) {
        boolean deleted = externalToolService.delete(externalToolIdFromUser);
        if (deleted) {
            return ok("Deleted external tool with id of " + externalToolIdFromUser);
        } else {
            return error(BAD_REQUEST, "Could not not delete external tool with id of " + externalToolIdFromUser);
        }
    }

    /**
     * For testing only. For each of the external tools in the database we are
     * testing the JSON format and that any reserved words within the JSON are
     * properly replaced with the data the user supplies such as a file id
     * and/or API token.
     */
    @GET
    @Path("test/file/{id}")
    public Response getExternalToolHandlersByFile(@PathParam("id") Long fileIdFromUser) {
        DataFile dataFile = fileSvc.find(fileIdFromUser);
        if (dataFile == null) {
            return error(BAD_REQUEST, "Could not find datafile with id " + fileIdFromUser);
        }
        JsonArrayBuilder tools = Json.createArrayBuilder();
        ApiToken apiToken = new ApiToken();
        String apiTokenString = getRequestApiKey();
        apiToken.setTokenString(apiTokenString);

        List<ExternalTool> allExternalTools = externalToolService.findAll();
        List<ExternalTool> toolsByFile = ExternalToolServiceBean.findExternalToolsByFile(allExternalTools, dataFile);
        for (ExternalTool tool : toolsByFile) {
            tools.add(tool.toJson());
        }

        return ok(tools);
    }

}
