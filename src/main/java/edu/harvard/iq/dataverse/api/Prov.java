package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteProvFreeFormCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteProvJsonProvCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PersistProvFreeFormCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PersistProvJsonProvCommand;
import edu.harvard.iq.dataverse.provenance.ProvenanceRestServiceBean;
import java.io.StringReader;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

@Path("files")
public class Prov extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Prov.class.getCanonicalName());

    @EJB
    ProvenanceRestServiceBean provenanceRestSvc;

    /** Provenance JSON methods **/
    @POST
    @Path("{id}/prov-json")
    @Consumes("application/json")
    public Response addProvJson(String body, @PathParam("id") String idSupplied) {
        try {
            JsonObject jsonObject = execCommand(new PersistProvJsonProvCommand(createDataverseRequest(findUserOrDie()), findDataFileOrDie(idSupplied), body));
            JsonObjectBuilder jsonResponse = Json.createObjectBuilder();
            jsonResponse.add("message", "PROV-JSON provenance data saved: " + jsonObject.toString());
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
            DataFile savedDataFile = execCommand(new PersistProvFreeFormCommand(createDataverseRequest(findUserOrDie()), findDataFileOrDie(idSupplied), provFreeForm));
            JsonObjectBuilder jsonResponse = Json.createObjectBuilder();
            jsonResponse.add("message", "Free-form provenance data saved: " + savedDataFile.getFileMetadata().getProvFreeForm());
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

    @GET
    @Path("{id}/prov")
    public Response exportProv(@PathParam("id") String idSupplied) {
        try {
            // FIXME: Don't hard code this as 94, obviously. Look up the bundleId based on the file id/PID.
            long bundleId = 94;
            JsonObject jsonObject = provenanceRestSvc.getBundleJson(bundleId);
            // FIXME: How should this JSON look?
            return ok(Json.createObjectBuilder().add("exportProv", jsonObject));
        } catch (Exception ex) {
            return error(BAD_REQUEST, "error!");
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
