package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.brief;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.json;
import static edu.harvard.iq.dataverse.util.json.JsonPrinter.toJsonArray;
import edu.harvard.iq.dataverse.workflow.Workflow;
import edu.harvard.iq.dataverse.workflow.WorkflowServiceBean;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.JsonObject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 * API Endpoint for managing workflows.
 * @author michael
 */
@Path("admin/workflows")
@Stateless
public class Workflows extends AbstractApiBean {
    
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
    
    @Path("/{identifier}")
    @DELETE
    public Response deleteWorkflow(@PathParam("identifier") String identifier ) {
        try {
            long idtf = Long.parseLong(identifier);
            int effected = workflows.deleteWorkflow(idtf);
            return (effected==0) ? notFound("workflow with id " + idtf + " not found") 
                                 : ok("Workflow " + idtf + " deleted"); 
        } catch (NumberFormatException nfe) {
            return badRequest("workflow identifier has to be numeric.");
        }
    }
    
}
