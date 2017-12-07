package edu.harvard.iq.dataverse.api;

import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("files")
public class ProvApi extends AbstractApiBean {

    @POST
    @Path("{id}/prov")
    @Consumes("application/json")
    public Response addProvFile(String body, @PathParam("id") Long idSupplied) {
        // TODO: Permissions.
        // TODO: Save prov file to filesystem/S3.
        StringReader rdr = new StringReader(body);
        try {
            JsonObject jsonObj = Json.createReader(rdr).readObject();
        } catch (JsonException ex) {
            return error(Response.Status.BAD_REQUEST, "A valid JSON object could not be found.");
        }
        JsonObjectBuilder response = Json.createObjectBuilder();
        response.add("message", "A valid JSON object was uploaded.");
        return ok(response);
    }

}
