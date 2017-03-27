package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.MapLayerMetadata;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CheckMapLayerMetadataCommand;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

/**
 * @todo consolidate with WorldMapRelatedData (/api/worldmap)?
 */
@Path("admin/geoconnect")
public class Geoconnect extends AbstractApiBean {

    @GET
    @Path("mapLayerMetadatas")
    public Response getMapLayerMetadatas() {
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        mapLayerMetadataSrv.findAll().stream().forEach((mapLayerMetadata) -> {
            jsonArrayBuilder.add(json(mapLayerMetadata));
        });
        return ok(jsonArrayBuilder);
    }

    @POST
    @Path("mapLayerMetadatas/check")
    public Response checkMapLayerMetadatas() {
        DataverseRequest dataverseRequest = null;
        try {
            dataverseRequest = createDataverseRequest(findUserOrDie());
        } catch (WrappedResponse wr) {
            return error(BAD_REQUEST, "Couldn't find user to execute command: " + wr.getLocalizedMessage());
        }
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        for (MapLayerMetadata unmodified : mapLayerMetadataSrv.findAll()) {
            JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
            jsonObjectBuilder.add("fileId", unmodified.getDataFile().getId());
            jsonObjectBuilder.add("mapLayerMetadataId", unmodified.getId());
            jsonObjectBuilder.add("layerLink", unmodified.getLayerLink());
            jsonObjectBuilder.add("fileLandingPage", systemConfig.getDataverseSiteUrl() + "/file.xhtml?fileId=" + unmodified.getDataFile().getId());
            try {
                MapLayerMetadata modified = engineSvc.submit(new CheckMapLayerMetadataCommand(dataverseRequest, unmodified.getDataFile()));
                jsonObjectBuilder.add("lastVerifiedStatus", modified.getLastVerifiedStatus());
            } catch (CommandException ex) {
                jsonObjectBuilder.add("Could not check status: ", ex.getLocalizedMessage());
            }
            jsonArrayBuilder.add(jsonObjectBuilder);
        }
        return ok(jsonArrayBuilder);
    }

}
