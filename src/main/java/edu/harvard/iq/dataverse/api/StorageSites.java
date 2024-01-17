package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.locality.StorageSite;
import edu.harvard.iq.dataverse.locality.StorageSiteUtil;
import java.util.List;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path("admin/storageSites")
public class StorageSites extends AbstractApiBean {

    @GET
    public Response listAll() {
        List<StorageSite> storageSites = storageSiteSvc.findAll();
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
        StorageSite storageSite = storageSiteSvc.find(id);
        if (storageSite == null) {
            return error(Response.Status.NOT_FOUND, "Could not find a storage site based on id " + id + ".");
        }
        return ok(storageSite.toJsonObjectBuilder());
    }

    @POST
    public Response addSite(JsonObject jsonObject) {
        StorageSite toPersist = null;
        try {
            toPersist = StorageSiteUtil.parse(jsonObject);
        } catch (Exception ex) {
            return error(Response.Status.BAD_REQUEST, "JSON could not be parsed: " + ex.getLocalizedMessage());
        }
        List<StorageSite> exitingSites = storageSiteSvc.findAll();
        try {
            StorageSiteUtil.ensureOnlyOnePrimary(toPersist, exitingSites);
        } catch (Exception ex) {
            return error(Response.Status.BAD_REQUEST, ex.getLocalizedMessage());
        }
        StorageSite saved = storageSiteSvc.add(toPersist);
        if (saved != null) {
            return ok(saved.toJsonObjectBuilder());
        } else {
            return error(Response.Status.BAD_REQUEST, "Storage site could not be added.");
        }
    }

    @PUT
    @Path("{id}/primaryStorage")
    public Response setPrimary(@PathParam("id") long id, String input) {
        StorageSite toModify = storageSiteSvc.find(id);
        if (toModify == null) {
            return error(Response.Status.NOT_FOUND, "Could not find a storage site based on id " + id + ".");
        }
        // "junk" gets parsed into "false".
        toModify.setPrimaryStorage(Boolean.valueOf(input));
        List<StorageSite> exitingSites = storageSiteSvc.findAll();
        try {
            StorageSiteUtil.ensureOnlyOnePrimary(toModify, exitingSites);
        } catch (Exception ex) {
            return error(Response.Status.BAD_REQUEST, ex.getLocalizedMessage());
        }
        StorageSite updated = storageSiteSvc.save(toModify);
        return ok(updated.toJsonObjectBuilder());
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") long id) {
        boolean deleted = storageSiteSvc.delete(id);
        if (deleted) {
            return ok("Storage site id  " + id + " has been deleted.");
        } else {
            return error(Response.Status.NOT_FOUND, "Could not find a storage site based on id " + id + ".");
        }
    }

}
