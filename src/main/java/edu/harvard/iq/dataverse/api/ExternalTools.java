package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.externaltools.ExternalTool;
import edu.harvard.iq.dataverse.externaltools.ExternalToolServiceBean;
import java.util.logging.Logger;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("admin/externalTools")
@Tag(name = "External Tools", description = "External tool registration and lookup operations.")
public class ExternalTools extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(ExternalTools.class.getCanonicalName());

    @GET
    @Operation(summary = "Lists external tools",
            description = "Returns all registered external tools as JSON.")
    public Response getExternalTools() {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        externalToolService.findAll().forEach((externalTool) -> {
            jab.add(externalTool.toJson());
        });
        return ok(jab);
    }

    @GET
    @Path("{id}")
    @Operation(summary = "Returns an external tool",
            description = "Returns the registered external tool with the specified numeric id.")
    public Response getExternalTool(
            @Parameter(description = "Numeric id of the external tool to return.", required = true)
            @PathParam("id") long externalToolIdFromUser) {
        ExternalTool externalTool = externalToolService.findById(externalToolIdFromUser);
        if (externalTool != null) {
            return ok(externalTool.toJson());
        } else {
            return error(BAD_REQUEST, "Could not find external tool with id of " + externalToolIdFromUser);
        }
    }

    @POST
    @Operation(summary = "Registers an external tool",
            description = "Parses an external tool manifest, saves the tool definition, and returns the saved tool as JSON.")
    public Response addExternalTool(
            @RequestBody(description = "External tool manifest JSON to parse and register.")
            String manifest) {
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
    @Operation(summary = "Deletes an external tool",
            description = "Deletes the registered external tool with the specified numeric id.")
    public Response deleteExternalTool(
            @Parameter(description = "Numeric id of the external tool to delete.", required = true)
            @PathParam("id") long externalToolIdFromUser) {
        boolean deleted = externalToolService.delete(externalToolIdFromUser);
        if (deleted) {
            return ok("Deleted external tool with id of " + externalToolIdFromUser);
        } else {
            return error(BAD_REQUEST, "Could not not delete external tool with id of " + externalToolIdFromUser);
        }
    }

}
