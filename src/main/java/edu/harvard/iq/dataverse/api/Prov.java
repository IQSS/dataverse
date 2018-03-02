package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteProvFreeFormCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteProvJsonProvCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PersistProvFreeFormCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PersistProvJsonProvCommand;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Path("files")
public class Prov extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Prov.class.getCanonicalName());

    /** Provenance JSON methods **/
    @POST
    @Path("{id}/prov-json")
    @Consumes("application/json")
    public Response addProvJson(String body, @PathParam("id") String idSupplied, @QueryParam("entityName") String entityName) {
        try {
            DataFile dataFile;
            dataFile = findDataFileOrDie(idSupplied);
            if(null == dataFile.getFileMetadata()) { // can happen when a datafile is not fully initialized, though unlikely in our current implementation
                return error(BAD_REQUEST, "Invalid DataFile Id, file not fully initialized");
            } else if (dataFile.getFileMetadata().getCplId() != 0) {
                return error(METHOD_NOT_ALLOWED, "File provenance has already exists in the CPL system and cannot be uploaded.");
            }
            //MAD: I messed with this, need to fix it VVV
            execCommand(new PersistProvJsonProvCommand(createDataverseRequest(findUserOrDie()), dataFile , body, entityName));
            JsonObjectBuilder jsonResponse = Json.createObjectBuilder();
            jsonResponse.add("message", "PROV-JSON provenance data saved for Data File: " + dataFile.getDisplayName());
            return ok(jsonResponse);
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
    
    @DELETE
    @Path("{id}/prov-json")
    public Response removeProvJson(String body, @PathParam("id") String idSupplied) {
        try {
            execCommand(new DeleteProvJsonProvCommand(createDataverseRequest(findUserOrDie()), findDataFileOrDie(idSupplied)));
            return ok("Provenance URL deleted");
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    /** Provenance FreeForm methods **/
    @POST
    @Path("{id}/prov-freeform")
    @Consumes("application/json")
    public Response addProvFreeForm(String body, @PathParam("id") String idSupplied) {
        StringReader rdr = new StringReader(body);
        JsonObject jsonObj = null;
        DataFile dataFile;
        
        try {
            jsonObj = Json.createReader(rdr).readObject();
        } catch (JsonException ex) {
            return error(BAD_REQUEST, "A valid JSON object could not be found.");
        }
        String provFreeForm;
        try {
            provFreeForm = jsonObj.getString("text");
        } catch (NullPointerException ex) {
            return error(BAD_REQUEST, "The JSON object you send must have a key called 'text'.");
        }
        try {
            dataFile = findDataFileOrDie(idSupplied);
            execCommand(new PersistProvFreeFormCommand(createDataverseRequest(findUserOrDie()), dataFile, provFreeForm));
            JsonObjectBuilder jsonResponse = Json.createObjectBuilder();
            jsonResponse.add("message", "Free-form provenance data saved for Data File : " + dataFile.getFileMetadata().getProvFreeForm());
            return ok(jsonResponse);
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
    
    @DELETE
    @Path("{id}/prov-freeform")
    public Response removeProvFreeForm(String body, @PathParam("id") String idSupplied) {
        try {
            return ok(execCommand(new DeleteProvFreeFormCommand(createDataverseRequest(findUserOrDie()), findDataFileOrDie(idSupplied))));
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    /** Helper Methods */
    // FIXME: Delete this and switch to the version in AbstractApiBean.java once this is merged: https://github.com/IQSS/dataverse/pull/4350
    private DataFile findDataFileOrDie(String idSupplied) throws WrappedResponse {
        long idSuppliedAsLong;
        try {
            idSuppliedAsLong = new Long(idSupplied);
        } catch (NumberFormatException ex) {
            throw new WrappedResponse(badRequest("Could not find a number based on " + idSupplied));
        }
        DataFile dataFile = fileSvc.find(idSuppliedAsLong);
        if (dataFile == null) {
            throw new WrappedResponse(badRequest("Could not find a file based on id " + idSupplied));
        }
        return dataFile;
    }

}
