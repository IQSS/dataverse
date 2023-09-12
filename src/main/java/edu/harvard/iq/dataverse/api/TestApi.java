package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.externaltools.ExternalTool;
import edu.harvard.iq.dataverse.externaltools.ExternalToolHandler;
import java.util.List;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

@Path("admin/test")
public class TestApi extends AbstractApiBean {

    @GET
    @Path("datasets/{id}/externalTools")
    public Response getExternalToolsforFile(@PathParam("id") String idSupplied, @QueryParam("type") String typeSupplied) {
        ExternalTool.Type type;
        try {
            type = ExternalTool.Type.fromString(typeSupplied);
        } catch (IllegalArgumentException ex) {
            return error(BAD_REQUEST, ex.getLocalizedMessage());
        }
        Dataset dataset;
        try {
            dataset = findDatasetOrDie(idSupplied);
            JsonArrayBuilder tools = Json.createArrayBuilder();
            List<ExternalTool> datasetTools = externalToolService.findDatasetToolsByType(type);
            for (ExternalTool tool : datasetTools) {
                ApiToken apiToken = externalToolService.getApiToken(getRequestApiKey());
                ExternalToolHandler externalToolHandler = new ExternalToolHandler(tool, dataset, apiToken, null);
                JsonObjectBuilder toolToJson = externalToolService.getToolAsJsonWithQueryParameters(externalToolHandler);
                tools.add(toolToJson);
            }
            return ok(tools);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    @Path("files/{id}/externalTools")
    @GET
    public Response getExternalToolsForFile(@PathParam("id") String idSupplied, @QueryParam("type") String typeSupplied) {
        ExternalTool.Type type;
        try {
            type = ExternalTool.Type.fromString(typeSupplied);
        } catch (IllegalArgumentException ex) {
            return error(BAD_REQUEST, ex.getLocalizedMessage());
        }
        try {
            DataFile dataFile = findDataFileOrDie(idSupplied);
            JsonArrayBuilder tools = Json.createArrayBuilder();
            List<ExternalTool> datasetTools = externalToolService.findFileToolsByTypeAndContentType(type, dataFile.getContentType());
            for (ExternalTool tool : datasetTools) {
                ApiToken apiToken = externalToolService.getApiToken(getRequestApiKey());
                ExternalToolHandler externalToolHandler = new ExternalToolHandler(tool, dataFile, apiToken, dataFile.getFileMetadata(), null);
                JsonObjectBuilder toolToJson = externalToolService.getToolAsJsonWithQueryParameters(externalToolHandler);
                if (externalToolService.meetsRequirements(tool, dataFile)) {
                    tools.add(toolToJson);
                }
            }
            return ok(tools);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

}
