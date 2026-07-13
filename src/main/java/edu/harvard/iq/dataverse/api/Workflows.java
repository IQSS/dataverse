package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.IpGroup;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddress;
import edu.harvard.iq.dataverse.authorization.groups.impl.ipaddress.ip.IpAddressRange;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import edu.harvard.iq.dataverse.workflow.PendingWorkflowInvocation;
import edu.harvard.iq.dataverse.workflow.WorkflowServiceBean;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * API Endpoint for external systems to report the results of workflow step
 * execution. Pending workflows (stored as {@link PendingWorkflowInvocation}s) 
 * wait for external systems to post a result on this endpoint.
 * 
 * @author michael
 */
@Path("workflows")
@Tag(name = "Workflows", description = "Workflow callback operations.")
public class Workflows extends AbstractApiBean {
    
    @EJB
    WorkflowServiceBean workflows;
    
    private IpGroup whitelist = new IpGroup();
    private long lastWhitelistUpdate = 0;
    
    @Path("{invocationId}")
    @POST
    @Operation(summary = "Resumes a pending workflow",
            description = "Accepts the result body from an external workflow step and resumes the pending workflow invocation when the caller IP address is allowed.")
    public Response resumeWorkflow(
            @Parameter(description = "Identifier of the pending workflow invocation to resume.", required = true)
            @PathParam("invocationId") String invocationId,
            @RequestBody(description = "Workflow step result body passed to the pending invocation.")
            String body) {
        PendingWorkflowInvocation pending = workflows.getPendingWorkflow(invocationId);
        
        String remoteAddrStr = httpRequest.getRemoteAddr();
        IpAddress remoteAddr = IpAddress.valueOf((remoteAddrStr!=null) ? remoteAddrStr : "0.0.0.0");
        if ( ! isAllowed(remoteAddr) ) {
            return unauthorized("Sorry, your IP address is not authorized to send resume requests. Please contact an admin.");
        }
        Logger.getLogger(Workflows.class.getName()).log(Level.INFO, "Resume request from: {0}", httpRequest.getRemoteAddr());
        
        if ( pending == null ) {
            return notFound("Cannot find workflow invocation with id " + invocationId );
        }
        
        workflows.resume( pending, body );
        
        return Response.accepted("/api/datasets/" + pending.getDataset().getId() ).build();
    }
    
    private boolean isAllowed(IpAddress addr) {
        if ( System.currentTimeMillis()-lastWhitelistUpdate > 60*1000 ) {
            updateWhitelist();
        }
        return whitelist.containsAddress(addr);
    }
    
    private void updateWhitelist() { 
        IpGroup updatedList = new IpGroup();
        String[] ips = settingsSvc.getValueForKey(Key.WorkflowsAdminIpWhitelist, WorkflowsAdmin.DEFAULT_IP_ALLOWLIST).split(WorkflowsAdmin.IP_SEPARATOR);
        Arrays.stream(ips)
                .forEach( str -> updatedList.add(
                                      IpAddressRange.makeSingle(
                                              IpAddress.valueOf(str))));
        whitelist = updatedList;
        lastWhitelistUpdate = System.currentTimeMillis();
    }
}
