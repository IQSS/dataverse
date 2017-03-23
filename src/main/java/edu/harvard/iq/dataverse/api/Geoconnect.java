package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.MapLayerMetadata;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * @todo consolidate with WorldMapRelatedData (/api/worldmap)
 */
@Path("admin/geoconnect")
public class Geoconnect extends AbstractApiBean {

    @GET
    @Path("version")
    public Response getVersionOfGeoconnect() {
        return ok("UNKNOWN");
    }

    @GET
    @Path("mapLayerMetadatas")
    public Response getMapLayerMetadatas() {
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        for (MapLayerMetadata mapLayerMetadata : mapLayerMetadataSrv.findAll()) {
            jsonArrayBuilder.add(JsonPrinter.json(mapLayerMetadata));
        }
        return ok(jsonArrayBuilder);
    }

}
