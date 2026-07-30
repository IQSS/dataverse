package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.WorkflowsAdminIpWhitelist;
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
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * API Endpoint for managing workflows.
 * @author michael
 */
@Path("admin/workflows")
@Tag(name = "Workflows", description = "Workflow administration operations.")
public class WorkflowsAdmin extends AbstractApiBean {
    
    public static final String IP_SEPARATOR = ";";
    public static final String DEFAULT_IP_ALLOWLIST = "127.0.0.1" + IP_SEPARATOR + "::1";
    
    @EJB
    WorkflowServiceBean workflows;
    
    @POST
    @Operation(summary = "Creates a workflow",
            description = "Parses a workflow definition, saves it, and returns the created workflow.")
    public Response addWorkflow(
            @RequestBody(description = "Workflow definition to parse and persist.")
            JsonObject jsonWorkflow) {
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
    @Operation(summary = "Lists workflows",
            description = "Returns brief JSON entries for all configured workflows.")
    public Response listWorkflows() {
        return ok( workflows.listWorkflows().stream()
                            .map(wf->brief.json(wf)).collect(toJsonArray()) );
    }
    
    @Path("default/{triggerType}")
    @PUT
    @Operation(summary = "Sets a default workflow",
            description = "Sets the workflow id used by default for the specified workflow trigger type.")
    public Response setDefault(
            @Parameter(description = "Workflow trigger type that receives the default workflow.", required = true)
            @PathParam("triggerType") String triggerType,
            @RequestBody(description = "Numeric workflow id to use as the default for the trigger type.")
            String identifier) {
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
    @Operation(summary = "Lists default workflows",
            description = "Returns each workflow trigger type with its configured default workflow or null when no default is set.")
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
    @Operation(summary = "Returns a default workflow",
            description = "Returns the default workflow configured for the specified trigger type.")
    public Response getDefault(
            @Parameter(description = "Workflow trigger type whose default workflow is requested.", required = true)
            @PathParam("triggerType") String triggerType) {
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
    @Operation(summary = "Clears a default workflow",
            description = "Removes the default workflow assignment for the specified trigger type.")
    public Response deleteDefault(
            @Parameter(description = "Workflow trigger type whose default assignment is removed.", required = true)
            @PathParam("triggerType") String triggerType) {
        try {
            workflows.setDefaultWorkflowId(TriggerType.valueOf(triggerType), null);
            return ok("default workflow for trigger " + triggerType + " unset.");
        } catch ( IllegalArgumentException iae ) {
            return badRequest("Unknown trigger type '" + triggerType + "'. Available triggers: " + Arrays.toString(TriggerType.values()) );
        }
    }
    
    @Path("/{id}")
    @GET
    @Operation(summary = "Returns a workflow",
            description = "Returns the workflow with the specified numeric id.")
    public Response getWorkflow(
            @Parameter(description = "Numeric id of the workflow to return.", required = true)
            @PathParam("id") String identifier ) {
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
    @Operation(summary = "Deletes a workflow",
            description = "Deletes the workflow with the specified numeric id unless it is configured as a default workflow.")
    public Response deleteWorkflow(
            @Parameter(description = "Numeric id of the workflow to delete.", required = true)
            @PathParam("id") String id ) {
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
    @Operation(summary = "Returns the workflow IP allowlist",
            description = "Returns the semicolon-separated IP addresses allowed to resume external workflow steps.")
    public Response getIpWhitelist() {
        return ok( settingsSvc.getValueForKey(WorkflowsAdminIpWhitelist, DEFAULT_IP_ALLOWLIST) );
    }
    
    @Path("/ip-whitelist")
    @PUT
    @Operation(summary = "Sets the workflow IP allowlist",
            description = "Validates and stores the semicolon-separated IP addresses allowed to resume external workflow steps.")
    public Response setIpWhitelist(
            @RequestBody(description = "Semicolon-separated IP addresses allowed to resume external workflow steps.")
            String body) {
        String ipList = body.trim();
        String[] ips = ipList.split(IP_SEPARATOR);
        boolean allIpsOk = Arrays.stream(ips).allMatch(ip->{
            try {
                IpAddress.valueOf(ip);
                return true;
            } catch ( IllegalArgumentException iae ) {
                return false;
            }
        } );
        if (allIpsOk) {
            settingsSvc.setValueForKey(WorkflowsAdminIpWhitelist, ipList);
            return ok( settingsSvc.getValueForKey(WorkflowsAdminIpWhitelist, DEFAULT_IP_ALLOWLIST) );
        } else {
            return badRequest("Request contains illegal IP addresses.");
        }
    }
    
    @Path("/ip-whitelist")
    @DELETE
    @Operation(summary = "Resets the workflow IP allowlist",
            description = "Deletes the configured workflow IP allowlist so the default localhost allowlist is used.")
    public Response deleteIpWhitelist() {
        settingsSvc.deleteValueForKey(WorkflowsAdminIpWhitelist);
        return ok( "Restored whitelist to default (127.0.0.1;::1)" );
    }
    
}
