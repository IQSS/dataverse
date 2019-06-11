package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import static edu.harvard.iq.dataverse.api.AbstractApiBean.error;
import edu.harvard.iq.dataverse.externaltools.ExternalTool;
import edu.harvard.iq.dataverse.externaltools.ExternalToolServiceBean;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
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

    // TODO: Rather than only supporting looking up files by their database IDs, consider supporting persistent identifiers.
    @GET
    @Path("file/{id}")
    public Response getExternalToolsByFile(@PathParam("id") Long fileIdFromUser) {
        DataFile dataFile = fileSvc.find(fileIdFromUser);
        if (dataFile == null) {
            return error(BAD_REQUEST, "Could not find datafile with id " + fileIdFromUser);
        }
        JsonArrayBuilder tools = Json.createArrayBuilder();

        List<ExternalTool> allExternalTools = externalToolService.findAll();
        List<ExternalTool> toolsByFile = ExternalToolServiceBean.findExternalToolsByFile(allExternalTools, dataFile);
        for (ExternalTool tool : toolsByFile) {
            tools.add(tool.toJson());
        }

        return ok(tools);
    }

}
