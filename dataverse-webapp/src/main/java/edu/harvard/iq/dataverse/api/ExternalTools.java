package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.api.annotations.ApiWriteOperation;
import edu.harvard.iq.dataverse.externaltools.ExternalToolServiceBean;
import edu.harvard.iq.dataverse.persistence.ActionLogRecord;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool.Type;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("admin/externalTools")
public class ExternalTools extends AbstractApiBean {

    private ActionLogServiceBean actionLogSvc;
    private ExternalToolServiceBean externalToolService;
    private DataFileServiceBean fileSvc;

    // -------------------- CONSTRUCTORS --------------------

    public ExternalTools() { }

    @Inject
    public ExternalTools(ActionLogServiceBean actionLogSvc, ExternalToolServiceBean externalToolService, DataFileServiceBean fileSvc) {
        this.actionLogSvc = actionLogSvc;
        this.externalToolService = externalToolService;
        this.fileSvc = fileSvc;
    }

    // -------------------- LOGIC --------------------

    @GET
    public Response getExternalTools() {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        for (ExternalTool externalTool : externalToolService.findAll()) {
            jab.add(externalTool.toJson());
        }
        return ok(jab);
    }

    @POST
    @ApiWriteOperation
    public Response addExternalTool(String manifest) {
        try {
            ExternalTool externalTool = externalToolService.parseAddExternalToolManifest(manifest);
            if (previewerOfSameContentAlreadyRegistered(externalTool)) {
                return badRequest("There's already a previewer for content type of " + externalTool.getContentType() + ". It must be removed before adding new.");
            }
            ExternalTool saved = externalToolService.save(externalTool);
            Long toolId = saved.getId();
            actionLogSvc.log(new ActionLogRecord(ActionLogRecord.ActionType.ExternalTool, "addExternalTool").setInfo("External tool added with id " + toolId + "."));
            return ok(saved.toJson());
        } catch (Exception ex) {
            return badRequest(ex.getMessage());
        }
    }

    @DELETE
    @ApiWriteOperation
    @Path("{id}")
    public Response deleteExternalTool(@PathParam("id") long externalToolIdFromUser) {
        boolean deleted = externalToolService.delete(externalToolIdFromUser);
        return deleted
                ? ok("Deleted external tool with id of " + externalToolIdFromUser)
                : badRequest("Could not not delete external tool with id of " + externalToolIdFromUser);
    }

    // TODO: Rather than only supporting looking up files by their database IDs, consider supporting persistent identifiers.
    @GET
    @Path("file/{id}")
    public Response getExternalToolsByFile(@PathParam("id") Long fileIdFromUser) {
        DataFile dataFile = fileSvc.find(fileIdFromUser);
        if (dataFile == null) {
            return badRequest("Could not find datafile with id " + fileIdFromUser);
        }
        JsonArrayBuilder tools = Json.createArrayBuilder();

        List<ExternalTool> allExternalTools = externalToolService.findAll();
        List<ExternalTool> toolsByFile = ExternalToolServiceBean.findExternalToolsByFile(allExternalTools, dataFile);
        for (ExternalTool tool : toolsByFile) {
            tools.add(tool.toJson());
        }
        return ok(tools);
    }

    // -------------------- PRIVATE --------------------

    private boolean previewerOfSameContentAlreadyRegistered(ExternalTool externalTool) {
        return externalTool.getType() == Type.PREVIEW
                && externalToolService.findByType(Type.PREVIEW)
                    .stream()
                    .anyMatch(p -> p.getContentType().equals(externalTool.getContentType()));
    }
}
