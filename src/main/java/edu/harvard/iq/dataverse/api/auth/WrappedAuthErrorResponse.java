package edu.harvard.iq.dataverse.api.auth;

import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

// TODO: Find common place for this?
import static edu.harvard.iq.dataverse.api.AbstractApiBean.STATUS_ERROR;

public class WrappedAuthErrorResponse extends Exception {

    private final Response response;

    public WrappedAuthErrorResponse(String message) {
        this.response = Response.status(Response.Status.UNAUTHORIZED)
                .entity(NullSafeJsonBuilder.jsonObjectBuilder()
                        .add("status", STATUS_ERROR)
                        .add("message", message).build()
                ).type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    public Response getResponse() {
        return response;
    }
}
