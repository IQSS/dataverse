package edu.harvard.iq.dataverse.api;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.MapLayerMetadata;
import static edu.harvard.iq.dataverse.api.AbstractApiBean.error;
import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * @todo consolidate with WorldMapRelatedData (/api/worldmap)?
 */
@Path("admin/geoconnect")
public class Geoconnect extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Geoconnect.class.getCanonicalName());

    @POST
    @Path("mapLayerMetadatas/check")
    public Response checkMapLayerMetadatas() {
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        mapLayerMetadataSrv.findAll().stream().map((unmodified) -> checkStatus(unmodified)).forEach((jsonObjectBuilder) -> {
            jsonArrayBuilder.add(jsonObjectBuilder);
        });
        return ok(jsonArrayBuilder);
    }

    @POST
    @Path("mapLayerMetadatas/check/{id}")
    public Response checkMapLayerMetadatas(@PathParam("id") Long idSupplied) {
        MapLayerMetadata mapLayerMetadata = mapLayerMetadataSrv.find(idSupplied);
        if (mapLayerMetadata == null) {
            return error(NOT_FOUND, "Could not find MapLayerMetadata based on id " + idSupplied);
        }
        return ok(checkStatus(mapLayerMetadata));
    }

    private JsonObjectBuilder checkStatus(MapLayerMetadata unmodified) {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add("fileId", unmodified.getDataFile().getId());
        jsonObjectBuilder.add("mapLayerMetadataId", unmodified.getId());
        jsonObjectBuilder.add("layerLink", unmodified.getLayerLink());
        jsonObjectBuilder.add("fileLandingPage", systemConfig.getDataverseSiteUrl() + "/file.xhtml?fileId=" + unmodified.getDataFile().getId());
        MapLayerMetadata modified = updateLastVerifiedTimeAndStatusCode(unmodified);
        if (modified != null) {
            jsonObjectBuilder.add("lastVerifiedStatus", modified.getLastVerifiedStatus());
        } else {
            jsonObjectBuilder.add("error", "Could not check status of map associate with file id " + unmodified.getDataFile().getId());
        }
        return jsonObjectBuilder;
    }

    private MapLayerMetadata updateLastVerifiedTimeAndStatusCode(MapLayerMetadata mapLayerMetadata) {
        String layerLink = mapLayerMetadata.getLayerLink();
        GetRequest getRequest = Unirest.get(layerLink);
        try {
            int statusCode = getRequest.asBinary().getStatus();
            mapLayerMetadata.setLastVerifiedStatus(statusCode);
            Timestamp now = new Timestamp(new Date().getTime());
            mapLayerMetadata.setLastVerifiedTime(now);
            logger.fine("Setting status code to " + statusCode + " and timestamp to " + now + " for MapLayerMetadata id " + mapLayerMetadata.getId() + ".");
            return mapLayerMetadataSrv.save(mapLayerMetadata);
        } catch (UnirestException ex) {
            logger.info("Couldn't update last verfied status code or timestamp: " + ex.getLocalizedMessage());
            return null;
        }
    }

    // For testing only.
    @Path("addMapLayerMetadata/{identifier}")
    @POST
    public Response addMapLayerMetadata(@PathParam("identifier") String identifier) {
        try {
            Dataset dataset = findDatasetOrDie(identifier);
            MapLayerMetadata mapLayerMetadata = new MapLayerMetadata();

            DataFile dfile = dataset.getLatestVersion().getFileMetadatas().get(0).getDataFile();
            JsonObjectBuilder jab = Json.createObjectBuilder();
            jab.add("layerName", "layerName");
            jab.add("layerLink", "layerLink");
            jab.add("embedMapLink", "embedMapLink");
            jab.add("worldmapUsername", "worldmapUsername");
            jab.add("mapImageLink", "mapImageLink");

            JsonObject jsonInfo = jab.build();
            mapLayerMetadata.setDataFile(dfile);
            mapLayerMetadata.setDataset(dfile.getOwner());
            mapLayerMetadata.setLayerName(jsonInfo.getString("layerName"));
            mapLayerMetadata.setLayerLink(jsonInfo.getString("layerLink"));
            mapLayerMetadata.setEmbedMapLink(jsonInfo.getString("embedMapLink"));
            mapLayerMetadata.setWorldmapUsername(jsonInfo.getString("worldmapUsername"));
            mapLayerMetadata.setMapImageLink(jsonInfo.getString("mapImageLink"));
            MapLayerMetadata savedMapLayerMetadata = mapLayerMetadataSrv.save(mapLayerMetadata);
            return ok("MapLayerMetadata saved with id" + savedMapLayerMetadata.getId());
        } catch (WrappedResponse ex) {
            Logger.getLogger(Admin.class.getName()).log(Level.SEVERE, null, ex);
            return error(Response.Status.NOT_FOUND, "Dataset not found");
        }

    }

}
