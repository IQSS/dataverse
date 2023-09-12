package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.provenance.ProvEntityFileData;
import edu.harvard.iq.dataverse.provenance.ProvInvestigator;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteProvJsonCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetProvFreeFormCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetProvJsonCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PersistProvFreeFormCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PersistProvJsonCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.StringReader;
import java.util.HashMap;
import java.util.logging.Logger;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;

@Path("files")
public class Prov extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(Prov.class.getCanonicalName());

    ProvInvestigator provUtil = ProvInvestigator.getInstance();
    
    /** Provenance JSON methods **/
    @POST
    @AuthRequired
    @Path("{id}/prov-json")
    @Consumes("application/json")
    public Response addProvJson(@Context ContainerRequestContext crc, String body, @PathParam("id") String idSupplied, @QueryParam("entityName") String entityName) {
        if(!systemConfig.isProvCollectionEnabled()) {
            return error(FORBIDDEN, BundleUtil.getStringFromBundle("api.prov.error.provDisabled"));
        }
        try {
            DataFile dataFile = findDataFileOrDie(idSupplied);
            if(null == dataFile.getFileMetadata()) { // can happen when a datafile is not fully initialized, though unlikely in our current implementation
                return error(BAD_REQUEST, BundleUtil.getStringFromBundle("api.prov.error.badDataFileId"));
            }
            if(dataFile.isReleased() && dataFile.getProvEntityName() != null){
                return error(FORBIDDEN, BundleUtil.getStringFromBundle("api.prov.error.jsonUpdateNotAllowed"));
            }
            
            if(!provUtil.isProvValid(body)) {
                return error(BAD_REQUEST, BundleUtil.getStringFromBundle("file.editProvenanceDialog.invalidSchemaError"));
            }
            
            /*Add when we actually integrate provCpl*/
            //else if (dataFile.getProvCplId() != 0) {
            //    return error(METHOD_NOT_ALLOWED, "File provenance has already exists in the CPL system and cannot be uploaded.");
            //} 
            HashMap<String,ProvEntityFileData> provJsonParsedEntities = provUtil.startRecurseNames(body);
            if(!provJsonParsedEntities.containsKey(entityName)) {
                //TODO: We should maybe go a step further and provide a way through the api to see the parsed entity names.
                return error(BAD_REQUEST, BundleUtil.getStringFromBundle("api.prov.error.entityMismatch"));
            }
            
            execCommand(new PersistProvJsonCommand(createDataverseRequest(getRequestUser(crc)), dataFile , body, entityName, true));
            JsonObjectBuilder jsonResponse = Json.createObjectBuilder();
            jsonResponse.add("message", BundleUtil.getStringFromBundle("api.prov.provJsonSaved") + " " + dataFile.getDisplayName());
            return ok(jsonResponse);
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
    
    @DELETE
    @AuthRequired
    @Path("{id}/prov-json")
    public Response deleteProvJson(@Context ContainerRequestContext crc, String body, @PathParam("id") String idSupplied) {
        if(!systemConfig.isProvCollectionEnabled()) {
            return error(FORBIDDEN, BundleUtil.getStringFromBundle("api.prov.error.provDisabled"));
        }
        try {
            DataFile dataFile = findDataFileOrDie(idSupplied);
            if(dataFile.isReleased()){
                return error(FORBIDDEN, BundleUtil.getStringFromBundle("api.prov.error.jsonDeleteNotAllowed"));
            }
            execCommand(new DeleteProvJsonCommand(createDataverseRequest(getRequestUser(crc)), dataFile, true));
            return ok(BundleUtil.getStringFromBundle("api.prov.provJsonDeleted"));
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }

    /** Provenance FreeForm methods **/
    @POST
    @AuthRequired
    @Path("{id}/prov-freeform")
    @Consumes("application/json")
    public Response addProvFreeForm(@Context ContainerRequestContext crc, String body, @PathParam("id") String idSupplied) {
        if(!systemConfig.isProvCollectionEnabled()) {
            return error(FORBIDDEN, BundleUtil.getStringFromBundle("api.prov.error.provDisabled"));
        }
        StringReader rdr = new StringReader(body);
        JsonObject jsonObj = null;
        
        try {
            jsonObj = Json.createReader(rdr).readObject();
        } catch (JsonException ex) {
            return error(BAD_REQUEST, BundleUtil.getStringFromBundle("api.prov.error.freeformInvalidJson"));
        }
        String provFreeForm;
        try {
            provFreeForm = jsonObj.getString("text");
        } catch (NullPointerException ex) {
            return error(BAD_REQUEST, BundleUtil.getStringFromBundle("api.prov.error.freeformMissingJsonKey"));
        }
        try {
            DataverseRequest dr= createDataverseRequest(getRequestUser(crc));
            DataFile dataFile = findDataFileOrDie(idSupplied);
            if (dataFile == null) {
                return error(BAD_REQUEST, BundleUtil.getStringFromBundle("api.prov.error.badDataFileId"));
            }
            execCommand(new PersistProvFreeFormCommand(dr, dataFile, provFreeForm));
            execCommand(new UpdateDatasetVersionCommand(dataFile.getOwner(), dr));
            dataFile = findDataFileOrDie(idSupplied);
            JsonObjectBuilder jsonResponse = Json.createObjectBuilder();
            jsonResponse.add("message", "Free-form provenance data saved for Data File : " + dataFile.getFileMetadata().getProvFreeForm());
            return ok(jsonResponse);
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
        
    }
    
    @GET
    @AuthRequired
    @Path("{id}/prov-freeform")
    public Response getProvFreeForm(@Context ContainerRequestContext crc, String body, @PathParam("id") String idSupplied) {
        if(!systemConfig.isProvCollectionEnabled()) {
            return error(FORBIDDEN, BundleUtil.getStringFromBundle("api.prov.error.provDisabled"));
        }
        try {
            String freeFormText = execCommand(new GetProvFreeFormCommand(createDataverseRequest(getRequestUser(crc)), findDataFileOrDie(idSupplied)));
            if(null == freeFormText) {
                return error(BAD_REQUEST, BundleUtil.getStringFromBundle("api.prov.error.freeformNoText"));
            }
            JsonObjectBuilder jsonResponse = Json.createObjectBuilder();
            jsonResponse.add("text", freeFormText);
            return ok(jsonResponse);
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
    
    @GET
    @AuthRequired
    @Path("{id}/prov-json")
    public Response getProvJson(@Context ContainerRequestContext crc, String body, @PathParam("id") String idSupplied) {
        if(!systemConfig.isProvCollectionEnabled()) {
            return error(FORBIDDEN, BundleUtil.getStringFromBundle("api.prov.error.provDisabled"));
        }
        try {
            JsonObject jsonText = execCommand(new GetProvJsonCommand(createDataverseRequest(getRequestUser(crc)), findDataFileOrDie(idSupplied)));
            if(null == jsonText) {
                return error(BAD_REQUEST, BundleUtil.getStringFromBundle("api.prov.error.jsonNoContent"));
            }
            JsonObjectBuilder jsonResponse = Json.createObjectBuilder();
            jsonResponse.add("json", jsonText.toString());
            return ok(jsonResponse);
        } catch (WrappedResponse ex) {
            return ex.getResponse();
        }
    }
    
}
