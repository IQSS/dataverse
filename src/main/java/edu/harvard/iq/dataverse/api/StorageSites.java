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
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("admin/storageSites")
@Tag(name = "Admin", description = "Administrative Dataverse operations.")
public class StorageSites extends AbstractApiBean {

    @GET
    @Operation(summary = "Lists storage sites",
            description = "Returns all configured storage sites as JSON.")
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
    @Operation(summary = "Returns a storage site",
            description = "Returns the configured storage site with the specified numeric id.")
    public Response get(
            @Parameter(description = "Numeric id of the storage site to return.", required = true)
            @PathParam("id") long id) {
        StorageSite storageSite = storageSiteSvc.find(id);
        if (storageSite == null) {
            return error(Response.Status.NOT_FOUND, "Could not find a storage site based on id " + id + ".");
        }
        return ok(storageSite.toJsonObjectBuilder());
    }

    @POST
    @Operation(summary = "Creates a storage site",
            description = "Parses and persists a storage site definition after validating that only one site is primary.")
    public Response addSite(
            @RequestBody(description = "Storage site definition to parse and persist.")
            JsonObject jsonObject) {
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
    @Operation(summary = "Sets primary storage for a site",
            description = "Updates the primary-storage flag for the specified storage site after validating the primary-site constraint.")
    public Response setPrimary(
            @Parameter(description = "Numeric id of the storage site to update.", required = true)
            @PathParam("id") long id,
            @RequestBody(description = "Boolean text indicating whether the storage site is primary.")
            String input) {
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
    @Operation(summary = "Deletes a storage site",
            description = "Deletes the configured storage site with the specified numeric id.")
    public Response delete(
            @Parameter(description = "Numeric id of the storage site to delete.", required = true)
            @PathParam("id") long id) {
        boolean deleted = storageSiteSvc.delete(id);
        if (deleted) {
            return ok("Storage site id  " + id + " has been deleted.");
        } else {
            return error(Response.Status.NOT_FOUND, "Could not find a storage site based on id " + id + ".");
        }
    }

}
