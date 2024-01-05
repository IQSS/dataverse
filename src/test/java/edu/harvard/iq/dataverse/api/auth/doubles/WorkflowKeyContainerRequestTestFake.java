package edu.harvard.iq.dataverse.api.auth.doubles;

import jakarta.ws.rs.core.UriInfo;

import static edu.harvard.iq.dataverse.api.auth.WorkflowKeyAuthMechanism.DATAVERSE_WORKFLOW_KEY_REQUEST_HEADER_NAME;

public class WorkflowKeyContainerRequestTestFake extends ContainerRequestTestFake {

    private final String workflowKey;
    private final UriInfo uriInfo;

    public WorkflowKeyContainerRequestTestFake(String workflowKey) {
        this.workflowKey = workflowKey;
        this.uriInfo = new WorkflowKeyUriInfoTestFake(workflowKey);
    }

    @Override
    public UriInfo getUriInfo() {
        return uriInfo;
    }

    @Override
    public String getHeaderString(String s) {
        if (s.equals(DATAVERSE_WORKFLOW_KEY_REQUEST_HEADER_NAME)) {
            return this.workflowKey;
        }
        return null;
    }
}
