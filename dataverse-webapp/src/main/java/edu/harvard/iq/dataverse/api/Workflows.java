package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.persistence.group.IpAddress;
import edu.harvard.iq.dataverse.persistence.group.IpAddressRange;
import edu.harvard.iq.dataverse.persistence.group.IpGroup;
import edu.harvard.iq.dataverse.workflow.execution.WorkflowExecutionFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.Arrays;

/**
 * API Endpoint for external systems to report the results of workflow step
 * execution. Pending workflows wait for external systems to post a result on this endpoint.
 *
 * @author michael
 */
@Path("workflows")
public class Workflows extends AbstractApiBean {

    private static final Logger log = LoggerFactory.getLogger(Workflows.class);

    @Inject
    private WorkflowExecutionFacade workflowExecutions;

    private IpGroup whitelist = new IpGroup();
    private long lastWhitelistUpdate = 0;

    @POST
    @Path("{invocationId}")
    public Response resumeWorkflow(@PathParam("invocationId") String invocationId, String body) {
        String remoteAddrStr = httpRequest.getRemoteAddr();
        IpAddress remoteAddr = IpAddress.valueOf((remoteAddrStr != null) ? remoteAddrStr : "0.0.0.0");
        if (!isAllowed(remoteAddr)) {
            return unauthorized("Sorry, your IP address is not authorized to send resume requests. Please contact an admin.");
        }
        log.info("Resume request from: {}", httpRequest.getRemoteAddr());

        return workflowExecutions.resume(invocationId, body).map(execution ->
                Response.accepted("/api/datasets/" + execution.getDatasetId()).build()
        ).orElseGet(() ->
                notFound("Cannot find workflow invocation with id " + invocationId)
        );
    }

    private boolean isAllowed(IpAddress addr) {
        if (System.currentTimeMillis() - lastWhitelistUpdate > 60 * 1000) {
            updateWhitelist();
        }
        return whitelist.containsAddress(addr);
    }

    private void updateWhitelist() {
        IpGroup updatedList = new IpGroup();
        String[] ips = settingsSvc.get(WorkflowsAdmin.IP_WHITELIST_KEY).split(";");
        Arrays.stream(ips)
                .forEach(str -> updatedList.add(
                        IpAddressRange.makeSingle(
                                IpAddress.valueOf(str))));
        whitelist = updatedList;
        lastWhitelistUpdate = System.currentTimeMillis();
    }
}
