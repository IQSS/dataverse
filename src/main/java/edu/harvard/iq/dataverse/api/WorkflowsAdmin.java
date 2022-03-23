package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.brief;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.toJsonArray;
import edu.harvard.iq.dataverse.workflow.Workflow;
import edu.harvard.iq.dataverse.workflow.WorkflowContext.TriggerType;
import edu.harvard.iq.dataverse.workflow.WorkflowServiceBean;
import java.util.Arrays;
import java.util.Optional;
import jakarta.ejb.EJB;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

/**
 * API Endpoint for managing workflows.
 * @author michael
 */
@Path("admin/workflows")
public class WorkflowsAdmin extends AbstractApiBean {
      
    public static final String IP_WHITELIST_KEY="WorkflowsAdmin#IP_WHITELIST_KEY";
    
    @EJB
    WorkflowServiceBean workflows;
    
    @POST
    public Response addWorkflow(JsonObject jsonWorkflow) {
        JsonParser jp = new JsonParser();
        try {
            Workflow wf = jp.parseWorkflow(jsonWorkflow);
            Workflow managedWf = workflows.save(wf);
            
            return created("/admin/workflows/"+managedWf.getId(), json(managedWf));
        } catch (JsonParseException ex) {
            return badRequest("Can't parse Json: " + ex.getMessage());
        }
    }
    
    @GET
    public Response listWorkflows() {
        return ok( workflows.listWorkflows().stream()
                            .map(wf->brief.json(wf)).collect(toJsonArray()) );
    }
    
    @Path("default/{triggerType}")
    @PUT
    public Response setDefault(@PathParam("triggerType") String triggerType, String identifier) {
        try {
            long idtf = Long.parseLong(identifier.trim());
            TriggerType tt = TriggerType.valueOf(triggerType);
            Optional<Workflow> wf = workflows.getWorkflow(idtf);
            if ( wf.isPresent() ) {
                workflows.setDefaultWorkflowId(tt, idtf);
                return ok("Default workflow id for trigger " + tt.name() + " set to " + idtf);
            } else {
                return notFound("Can't find workflow with id " + idtf);
            }
        } catch (NumberFormatException nfe) {
            return badRequest("workflow identifier has to be numeric.");
        } catch ( IllegalArgumentException iae ) {
            return badRequest("Unknown trigger type '" + triggerType + "'. Available triggers: " + Arrays.toString(TriggerType.values()) );
        }
    }
    
    @Path("default/")
    @GET
    public Response listDefaults() {
        JsonObjectBuilder bld = Json.createObjectBuilder();
        for ( TriggerType tp : TriggerType.values() ) {
            bld.add(tp.name(), 
                    workflows.getDefaultWorkflow(tp)
                             .map(wf->(JsonValue)brief.json(wf).build())
                             .orElse(JsonValue.NULL));
        }
        return ok(bld);
    }
    
    @Path("default/{triggerType}")
    @GET
    public Response getDefault(@PathParam("triggerType") String triggerType) {
        try {
            return workflows.getDefaultWorkflow(TriggerType.valueOf(triggerType))
                            .map( wf -> ok(json(wf)) )
                            .orElse( notFound("no default workflow") );
        } catch ( IllegalArgumentException iae ) {
            return badRequest("Unknown trigger type '" + triggerType + "'. Available triggers: " + Arrays.toString(TriggerType.values()) );
        }
    }
    
    @Path("default/{triggerType}")
    @DELETE
    public Response deleteDefault(@PathParam("triggerType") String triggerType) {
        try {
            workflows.setDefaultWorkflowId(TriggerType.valueOf(triggerType), null);
            return ok("default workflow for trigger " + triggerType + " unset.");
        } catch ( IllegalArgumentException iae ) {
            return badRequest("Unknown trigger type '" + triggerType + "'. Available triggers: " + Arrays.toString(TriggerType.values()) );
        }
    }
    
    @Path("/{identifier}")
    @GET
    public Response getWorkflow(@PathParam("identifier") String identifier ) {
        try {
            long idtf = Long.parseLong(identifier);
            return workflows.getWorkflow(idtf)
                            .map(wf->ok(json(wf)))
                            .orElse(notFound("Can't find workflow with id " + identifier));
        } catch (NumberFormatException nfe) {
            return badRequest("workflow identifier has to be numeric.");
        }
    }
    
    @Path("/{id}")
    @DELETE
    public Response deleteWorkflow(@PathParam("id") String id ) {
        try {
            long idtf = Long.parseLong(id);
            return workflows.deleteWorkflow(idtf) ? ok("Workflow " + idtf + " deleted") 
                                 : notFound("workflow with id " + idtf + " not found"); 
        } catch (NumberFormatException nfe) {
            return badRequest("workflow identifier has to be numeric.");
            
        } catch ( IllegalArgumentException e ) {
            return forbidden("Cannot delete the default workflow. Please change the default workflow and try again.");
            
        } catch ( Exception e ) {
            Throwable cc = e;
            while ( cc.getCause() != null ) {
                cc=cc.getCause();
            }
            if ( cc instanceof IllegalArgumentException ) {
                return forbidden("Cannot delete the default workflow. Please change the default workflow and try again.");
            } else {
                throw e;
            }
        }
    }
    
    @Path("/ip-whitelist")
    @GET
    public Response getIpWhitelist() {
        return ok( settingsSvc.get(IP_WHITELIST_KEY, "127.0.0.1;::1") );
    }
    
    @Path("/ip-whitelist")
    @PUT
    public Response setIpWhitelist(String body) {
        String ipList = body.trim();
        String[] ips = ipList.split(";");
        boolean allIpsOk = Arrays.stream(ips).allMatch(ip->{
            try {
                IpAddress.valueOf(ip);
                return true;
            } catch ( IllegalArgumentException iae ) {
                return false;
            }
        } );
        if (allIpsOk) {
            settingsSvc.set(IP_WHITELIST_KEY, ipList);
            return ok( settingsSvc.get(IP_WHITELIST_KEY, "127.0.0.1;::1") );
        } else {
            return badRequest("Request contains illegal IP addresses.");
        }
                
    }
    
    @Path("/ip-whitelist")
    @DELETE
    public Response deleteIpWhitelist() {
        settingsSvc.delete(IP_WHITELIST_KEY);
        return ok( "Restored whitelist to default (127.0.0.1;::1)" );
    }
    
}
