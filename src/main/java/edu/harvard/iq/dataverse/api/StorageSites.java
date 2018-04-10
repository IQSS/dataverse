package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.StorageLocation;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("admin/storageSites")
public class StorageSites extends AbstractApiBean {

    @GET
    public Response listAll() {
        List<StorageLocation> storageSites = repositoryStorageAbstractionLayerSvc.findAll();
        if (storageSites != null && !storageSites.isEmpty()) {
            JsonArrayBuilder sites = Json.createArrayBuilder();
            storageSites.forEach((storageSite) -> {
                sites.add(storageSite.toJsonObjectBuilder());
            });
            return ok(sites);
        } else {
            return error(Response.Status.NOT_FOUND, "No storage sites were found.");
        }
    }

    @GET
    @Path("{id}")
    public Response get(@PathParam("id") long id) {
        StorageLocation storageSite = repositoryStorageAbstractionLayerSvc.find(id);
        if (storageSite != null) {
            return ok(storageSite.toJsonObjectBuilder());
        } else {
            return error(Response.Status.NOT_FOUND, "Could not find a storage site based on id " + id + ".");
        }
    }

    @POST
    public Response addSite(JsonObject jsonObject) {
        StorageLocation toPersist = new StorageLocation();
        toPersist.setHostname(jsonObject.getString("hostname"));
        toPersist.setName(jsonObject.getString("name"));
        StorageLocation saved = repositoryStorageAbstractionLayerSvc.add(toPersist);
        if (saved != null) {
            return ok(saved.toJsonObjectBuilder());
        } else {
            return error(Response.Status.BAD_REQUEST, "Storage site could not be added.");
        }
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") long id) {
        boolean deleted = repositoryStorageAbstractionLayerSvc.delete(id);
        if (deleted) {
            return ok("Storage site id  " + id + " has been deleted.");
        } else {
            return error(Response.Status.NOT_FOUND, "Could not find a storage site based on id " + id + ".");
        }
    }

}
