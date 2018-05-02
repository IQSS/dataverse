package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.ProvEntityFileData;
import edu.harvard.iq.dataverse.ProvUtilFragmentBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteProvFreeFormCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteProvJsonCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetProvFreeFormCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetProvJsonCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PersistProvFreeFormCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PersistProvJsonCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
import java.io.StringReader;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.inject.Inject;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;

@Path("files")
public class Prov extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Prov.class.getCanonicalName());

    @Inject
    ProvUtilFragmentBean provUtil;
    
    /** Provenance JSON methods **/
    @POST
    @Path("{id}/prov-json")
    @Consumes("application/json")
//MAD: SHOULD NOT WORK ON PUBLISHED
    public Response addProvJson(String body, @PathParam("id") String idSupplied, @QueryParam("entityName") String entityName) {
        if(!systemConfig.isProvCollectionEnabled()) {
            return error(FORBIDDEN, "This functionality has been administratively disabled.");
        }
        try {
            DataFile dataFile = findDataFileOrDie(idSupplied);
            if(null == dataFile.getFileMetadata()) { // can happen when a datafile is not fully initialized, though unlikely in our current implementation
                return error(BAD_REQUEST, "Invalid DataFile Id, file not fully initialized");
            }
            if(dataFile.isReleased() && dataFile.getProvEntityName() != null){
                return error(FORBIDDEN, "Provenance JSON cannot be updated for a published file that already has Provenance JSON.");
            }
            
            /*Add when we actually integrate provCpl*/
            //else if (dataFile.getProvCplId() != 0) {
            //    return error(METHOD_NOT_ALLOWED, "File provenance has already exists in the CPL system and cannot be uploaded.");
            //} 
            HashMap<String,ProvEntityFileData> provJsonParsedEntities = provUtil.startRecurseNames(body);
            if(!provJsonParsedEntities.containsKey(entityName)) {
                //TODO: We should maybe go a step further and provide a way through the api to see the parsed entity names.
                return error(BAD_REQUEST, "Entity name provided does not match any entities parsed from the uploaded prov json");
            }
            
            execCommand(new PersistProvJsonCommand(createDataverseRequest(findUserOrDie()), dataFile , body, entityName, true));
            JsonObjectBuilder jsonResponse = Json.createObjectBuilder();
            jsonResponse.add("message", "PROV-JSON provenance data saved for Data File: " + dataFile.getDisplayName());
            return ok(jsonResponse);
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
    
    @DELETE
    @Path("{id}/prov-json")
//MAD: SHOULD NOT WORK ON PUBLISHED
    public Response deleteProvJson(String body, @PathParam("id") String idSupplied) {
        if(!systemConfig.isProvCollectionEnabled()) {
            return error(FORBIDDEN, "This functionality has been administratively disabled.");
        }
        try {
            DataFile dataFile = findDataFileOrDie(idSupplied);
            if(dataFile.isReleased()){
                return error(FORBIDDEN, "Provenance JSON cannot be deleted for a published file.");
            }
            execCommand(new DeleteProvJsonCommand(createDataverseRequest(findUserOrDie()), dataFile, true));
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
        if(!systemConfig.isProvCollectionEnabled()) {
            return error(FORBIDDEN, "This functionality has been administratively disabled.");
        }
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
            DataverseRequest dr= createDataverseRequest(findUserOrDie());
            DataFile dataFile = findDataFileOrDie(idSupplied);
            if (dataFile == null) {
                return error(BAD_REQUEST, "Could not find datafile with id " + idSupplied);
            }
            execCommand(new PersistProvFreeFormCommand(dr, dataFile, provFreeForm));
            execCommand(new UpdateDatasetCommand(dataFile.getOwner(), dr));
            dataFile = findDataFileOrDie(idSupplied);
            JsonObjectBuilder jsonResponse = Json.createObjectBuilder();
            jsonResponse.add("message", "Free-form provenance data saved for Data File : " + dataFile.getFileMetadata().getProvFreeForm());
            return ok(jsonResponse);
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
        
    }
    
    @DELETE
    @Path("{id}/prov-freeform")
    public Response deleteProvFreeForm(String body, @PathParam("id") String idSupplied) {
        if(!systemConfig.isProvCollectionEnabled()) {
            return error(FORBIDDEN, "This functionality has been administratively disabled.");
        }
        try {
            DataverseRequest dr= createDataverseRequest(findUserOrDie());
            DataFile dataFile = findDataFileOrDie(idSupplied);
            if (dataFile == null) {
                return error(BAD_REQUEST, "Could not find datafile with id " + idSupplied);
            }
            Boolean result = execCommand(new DeleteProvFreeFormCommand(createDataverseRequest(findUserOrDie()), findDataFileOrDie(idSupplied)));
            if(result) {
                execCommand(new UpdateDatasetCommand(dataFile.getOwner(), dr));
            }
            return ok(result);
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
    
    @GET
    @Path("{id}/prov-freeform")
    public Response getProvFreeForm(String body, @PathParam("id") String idSupplied) {
        if(!systemConfig.isProvCollectionEnabled()) {
            return error(FORBIDDEN, "This functionality has been administratively disabled.");
        }
        try {
            String freeFormText = execCommand(new GetProvFreeFormCommand(createDataverseRequest(findUserOrDie()), findDataFileOrDie(idSupplied)));
            if(null == freeFormText) {
                return error(BAD_REQUEST, "No provenance free form text available for this file.");
            }
            JsonObjectBuilder jsonResponse = Json.createObjectBuilder();
            jsonResponse.add("text", freeFormText);
            return ok(jsonResponse);
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
    
    @GET
    @Path("{id}/prov-json")
    public Response getProvJson(String body, @PathParam("id") String idSupplied) {
        if(!systemConfig.isProvCollectionEnabled()) {
            return error(FORBIDDEN, "This functionality has been administratively disabled.");
        }
        try {
            JsonObject jsonText = execCommand(new GetProvJsonCommand(createDataverseRequest(findUserOrDie()), findDataFileOrDie(idSupplied)));
            if(null == jsonText) {
                return error(BAD_REQUEST, "No provenance json available for this file.");
            }
            JsonObjectBuilder jsonResponse = Json.createObjectBuilder();
            jsonResponse.add("json", jsonText.toString());
            return ok(jsonResponse);
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
