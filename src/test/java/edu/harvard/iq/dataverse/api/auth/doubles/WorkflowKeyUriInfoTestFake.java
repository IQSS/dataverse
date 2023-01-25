package edu.harvard.iq.dataverse.api.auth.doubles;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import static edu.harvard.iq.dataverse.api.auth.WorkflowKeyAuthMechanism.DATAVERSE_WORKFLOW_KEY_REQUEST_PARAM_NAME;

public class WorkflowKeyUriInfoTestFake extends UriInfoTestFake {

    private final String workflowKey;

    public WorkflowKeyUriInfoTestFake(String workflowKey) {
        this.workflowKey = workflowKey;
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters() {
        MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();
        queryParameters.add(DATAVERSE_WORKFLOW_KEY_REQUEST_PARAM_NAME, workflowKey);
        return queryParameters;
    }
}
