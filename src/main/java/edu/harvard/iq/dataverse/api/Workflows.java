package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.workflow.PendingWorkflowInvocation;
import edu.harvard.iq.dataverse.workflow.WorkflowServiceBean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 * API Endpoint for external systems to report the results of workflow step
 * execution. Pending workflows (stored as {@link PendingWorkflowInvocation}s) 
 * wait for external systems to post a result on this endpoint.
 * 
 * @author michael
 */
@Path("workflows")
public class Workflows extends AbstractApiBean {
    
    @EJB
    WorkflowServiceBean workflows;
    
    @Path("{invocationId}")
    @POST
    public Response resumeWorkflow( @PathParam("invocationId") String invocationId, String body ) {
        PendingWorkflowInvocation pending = workflows.getPendingWorkflow(invocationId);
        
        // TODO see that the request came from an OK ip address
        Logger.getLogger(Workflows.class.getName()).log(Level.INFO, "Resume request from: {0}", httpRequest.getRemoteAddr());
        
        if ( pending == null ) {
            return notFound("Cannot find workflow invocation with id " + invocationId );
        }
        
        workflows.resume( pending, body );
        
        return Response.accepted("/api/datasets/" + pending.getDataset().getIdentifier() ).build();
    }
}
